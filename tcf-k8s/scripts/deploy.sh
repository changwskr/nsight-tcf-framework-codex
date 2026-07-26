#!/usr/bin/env bash
#
# NSIGHT TCF Framework Kubernetes 배포 스크립트
# 사용법:
#   ./deploy.sh <environment> <commit-sha> [module,...]
#
# 예:
#   ./deploy.sh development abc1234
#   ./deploy.sh production abc1234 eb,sv,ui
#   NAMESPACE=nsight-prod IMAGE_PREFIX=ghcr.io/changwskr/nsight-tcf-framework ./deploy.sh staging def5678
#
# environment:
#   development | staging | production
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TCF_K8S_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"

ENVIRONMENT="${1:-}"
COMMIT_SHA="${2:-}"
MODULE_FILTER="${3:-}"

REGISTRY="${REGISTRY:-ghcr.io}"
IMAGE_PREFIX="${IMAGE_PREFIX:-${REGISTRY}/changwskr/nsight-tcf-framework}"
NAMESPACE="${NAMESPACE:-nsight}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-300s}"

# 기본 배포 대상 (비즈니스 서비스 + gateway + ui)
DEFAULT_MODULES=(
  gateway
  ui
  ic
  pc
  ms
  sv
  pd
  eb
  ep
  ss
  mg
  om
)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

usage() {
  cat <<EOF
Usage: $0 <environment> <commit-sha> [module,...]

  environment : development | staging | production
  commit-sha  : image tag (usually git short SHA)
  module,...  : optional subset (comma-separated). Default: all

Examples:
  $0 development abc1234
  $0 production  abc1234 eb,sv,ui

Env overrides:
  NAMESPACE=nsight
  IMAGE_PREFIX=ghcr.io/changwskr/nsight-tcf-framework
  REGISTRY=ghcr.io
  ROLLOUT_TIMEOUT=300s
EOF
}

if [[ -z "${ENVIRONMENT}" || -z "${COMMIT_SHA}" ]]; then
  log_error "사용법: $0 <environment> <commit-sha> [module,...]"
  usage
  exit 1
fi

case "${ENVIRONMENT}" in
  development)
    SPRING_PROFILE="local"
    REPLICAS=1
    REQ_CPU="100m"
    REQ_MEM="256Mi"
    LIM_CPU="200m"
    LIM_MEM="512Mi"
    ;;
  staging)
    SPRING_PROFILE="dev"
    REPLICAS=2
    REQ_CPU="200m"
    REQ_MEM="512Mi"
    LIM_CPU="500m"
    LIM_MEM="1Gi"
    ;;
  production)
    SPRING_PROFILE="prod"
    REPLICAS=3
    REQ_CPU="500m"
    REQ_MEM="1Gi"
    LIM_CPU="1000m"
    LIM_MEM="2Gi"
    ;;
  *)
    log_error "지원하지 않는 환경: ${ENVIRONMENT}"
    usage
    exit 1
    ;;
esac

# module short name helpers (aligned with eb-service/scripts/k8s naming)
deployment_name() {
  echo "nsight-${1}"
}

container_name() {
  case "$1" in
    gateway) echo "tcf-gateway" ;;
    ui)      echo "tcf-ui" ;;
    *)       echo "${1}-service" ;;
  esac
}

image_repo() {
  case "$1" in
    gateway) echo "tcf-gateway" ;;
    ui)      echo "tcf-ui" ;;
    *)       echo "${1}-service" ;;
  esac
}

app_label() {
  echo "nsight-${1}"
}

configmap_name() {
  echo "nsight-${1}-config"
}

secret_file() {
  local m="$1"
  echo "${TCF_K8S_HOME}/configs/${m}-secret.yaml"
}

resolve_modules() {
  if [[ -z "${MODULE_FILTER}" ]]; then
    MODULES=("${DEFAULT_MODULES[@]}")
    return
  fi
  IFS=',' read -r -a MODULES <<< "${MODULE_FILTER}"
  local cleaned=()
  local m
  for m in "${MODULES[@]}"; do
    m="$(echo "${m}" | xargs)"
    [[ -n "${m}" ]] && cleaned+=("${m}")
  done
  MODULES=("${cleaned[@]}")
}

require_kubectl() {
  if ! command -v kubectl >/dev/null 2>&1; then
    log_error "kubectl not found. Install kubectl and configure cluster access."
    exit 1
  fi
}

ensure_namespace() {
  if ! kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1; then
    log_info "네임스페이스 ${NAMESPACE} 생성 중..."
    kubectl create namespace "${NAMESPACE}"
  fi
}

deploy_module() {
  local module="$1"
  local dep
  local ctn
  local image
  local cm
  local secret_path
  local app

  dep="$(deployment_name "${module}")"
  ctn="$(container_name "${module}")"
  image="${IMAGE_PREFIX}/$(image_repo "${module}"):${COMMIT_SHA}"
  cm="$(configmap_name "${module}")"
  secret_path="$(secret_file "${module}")"
  app="$(app_label "${module}")"

  log_info "${module} 모듈 배포 중... (deployment=${dep}, image=${image})"

  if ! kubectl get deployment "${dep}" -n "${NAMESPACE}" >/dev/null 2>&1; then
    log_warn "${module}: Deployment ${dep} 없음 — skip (manifest를 먼저 apply 하세요)"
    return 0
  fi

  # ConfigMap 업데이트 (profile / image tag)
  kubectl create configmap "${cm}" \
    --from-literal=SPRING_PROFILES_ACTIVE="${SPRING_PROFILE}" \
    --from-literal=IMAGE_TAG="${COMMIT_SHA}" \
    --from-literal=ENVIRONMENT="${ENVIRONMENT}" \
    -n "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

  # Secret (optional)
  if [[ -f "${secret_path}" ]]; then
    log_info "${module}: Secret 적용 (${secret_path})"
    kubectl apply -f "${secret_path}" -n "${NAMESPACE}"
  else
    log_warn "${module}: Secret 파일 없음 — skip (${secret_path})"
  fi

  # Image 갱신
  kubectl set image "deployment/${dep}" \
    "${ctn}=${image}" \
    -n "${NAMESPACE}"

  # Replicas + resources
  kubectl patch "deployment/${dep}" -n "${NAMESPACE}" --type strategic -p \
    "{\"spec\":{\"replicas\":${REPLICAS},\"template\":{\"spec\":{\"containers\":[{\"name\":\"${ctn}\",\"resources\":{\"requests\":{\"cpu\":\"${REQ_CPU}\",\"memory\":\"${REQ_MEM}\"},\"limits\":{\"cpu\":\"${LIM_CPU}\",\"memory\":\"${LIM_MEM}\"}}}}]}}}"

  log_info "${module} 배포 상태 확인 중..."
  if kubectl rollout status "deployment/${dep}" -n "${NAMESPACE}" --timeout="${ROLLOUT_TIMEOUT}"; then
    log_info "${module} 배포 완료"
  else
    log_error "${module} 배포 실패"
    exit 1
  fi

  # Pod ready (label이 있는 경우만)
  if kubectl get pods -n "${NAMESPACE}" -l "app=${app}" --no-headers 2>/dev/null | grep -q .; then
    kubectl wait --for=condition=ready "pod" -l "app=${app}" -n "${NAMESPACE}" --timeout="${ROLLOUT_TIMEOUT}"
  fi
}

apply_ingress() {
  local ingress_file="${TCF_K8S_HOME}/templates/ingress.yaml"
  if [[ -f "${ingress_file}" ]]; then
    log_info "Ingress 업데이트 중..."
    kubectl apply -f "${ingress_file}" -n "${NAMESPACE}"
  else
    log_warn "Ingress 템플릿 없음 — skip (${ingress_file})"
  fi
}

print_summary() {
  echo ""
  echo "=== 배포 정보 ==="
  echo "환경         : ${ENVIRONMENT} (spring.profiles=${SPRING_PROFILE})"
  echo "커밋/태그    : ${COMMIT_SHA}"
  echo "네임스페이스 : ${NAMESPACE}"
  echo "이미지 prefix: ${IMAGE_PREFIX}"
  echo "레플리카     : ${REPLICAS}"
  echo "리소스       : req=${REQ_CPU}/${REQ_MEM} lim=${LIM_CPU}/${LIM_MEM}"
  echo "모듈         : ${MODULES[*]}"
  echo ""
  echo "=== 서비스 ==="
  kubectl get services -n "${NAMESPACE}" || true
  echo ""
  echo "=== Pod ==="
  kubectl get pods -n "${NAMESPACE}" || true
  echo ""
}

# ---- main ----
require_kubectl
resolve_modules

log_info "배포 시작: 환경=${ENVIRONMENT}, 커밋=${COMMIT_SHA}, 모듈=${MODULES[*]}"
ensure_namespace

for module in "${MODULES[@]}"; do
  deploy_module "${module}"
done

apply_ingress

log_info "배포 완료!"
print_summary
