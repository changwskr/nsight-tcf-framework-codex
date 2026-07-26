#!/usr/bin/env bash
#
# sv-service Kubernetes delete
#
# Usage:
#   ./sv-service/scripts/k8s/k8s-delete.sh
#   ./sv-service/scripts/k8s/k8s-delete.sh --keep-ns
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-nsight}"
KEEP_NS=0
APP="nsight-sv"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[sv-k8s-delete]${NC} $1"; }
log_error() { echo -e "${RED}[sv-k8s-delete]${NC} $1" >&2; }

for arg in "$@"; do
  case "${arg}" in
    -h|--help|help)
      cat <<EOF
Usage: $0 [--keep-ns]

  Deletes sv-service k8s resources from namespace.

  --keep-ns   do not delete the namespace itself

Env:
  NAMESPACE=nsight
EOF
      exit 0
      ;;
    --keep-ns)
      KEEP_NS=1
      ;;
    *)
      log_error "unknown arg: ${arg}"
      exit 1
      ;;
  esac
done

if ! command -v kubectl >/dev/null 2>&1; then
  log_error "kubectl not found."
  exit 1
fi

log_info "delete resources in namespace=${NAMESPACE}"
kubectl delete -f "${SCRIPT_DIR}" -n "${NAMESPACE}" --ignore-not-found=true

if [[ "${KEEP_NS}" -eq 0 ]]; then
  log_info "namespace ${NAMESPACE} is kept (shared). Use kubectl delete ns ${NAMESPACE} manually if needed."
fi

kubectl get deploy,svc,pods -n "${NAMESPACE}" -l "app=${APP}" 2>/dev/null || true
log_info "done"
