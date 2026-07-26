#!/usr/bin/env bash
#
# sv-service Kubernetes apply
#
# Usage (from repo root or this directory):
#   ./sv-service/scripts/k8s/k8s-apply.sh
#   NAMESPACE=nsight ./sv-service/scripts/k8s/k8s-apply.sh
#
# Env:
#   NAMESPACE   default: nsight
#   WAIT=0      skip rollout wait (default waits)
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-nsight}"
WAIT="${WAIT:-1}"
APP="nsight-sv"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[sv-k8s-apply]${NC} $1"; }
log_error() { echo -e "${RED}[sv-k8s-apply]${NC} $1" >&2; }

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || "${1:-}" == "help" ]]; then
  cat <<EOF
Usage: $0

  Creates namespace (if needed) and applies sv-service/scripts/k8s manifests.

Env:
  NAMESPACE=nsight
  WAIT=1          set WAIT=0 to skip rollout wait
EOF
  exit 0
fi

if ! command -v kubectl >/dev/null 2>&1; then
  log_error "kubectl not found. Install kubectl and configure cluster access."
  exit 1
fi

log_info "checking cluster connectivity..."
if ! kubectl cluster-info >/dev/null 2>&1; then
  log_error "cannot reach Kubernetes API (usually 127.0.0.1:6443)."
  echo ""
  echo "  Current context: $(kubectl config current-context 2>/dev/null || echo '(none)')"
  echo ""
  echo "  Fix for Docker Desktop:"
  echo "    1) Open Docker Desktop"
  echo "    2) Settings -> Kubernetes -> Enable Kubernetes"
  echo "    3) Apply & Restart, wait until Kubernetes is running"
  echo "    4) Verify: kubectl get nodes"
  echo "    5) Re-run this script"
  echo ""
  echo "  Or switch to a reachable context:"
  echo "    kubectl config get-contexts"
  echo "    kubectl config use-context <name>"
  exit 1
fi

log_info "namespace=${NAMESPACE}"
kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

log_info "apply ${SCRIPT_DIR}"
kubectl apply -f "${SCRIPT_DIR}" -n "${NAMESPACE}"

if [[ "${WAIT}" != "0" ]]; then
  log_info "waiting for deployment/${APP} rollout..."
  kubectl rollout status "deployment/${APP}" -n "${NAMESPACE}" --timeout=300s
fi

echo ""
kubectl get deploy,svc,pods -n "${NAMESPACE}" -l "app=${APP}"
log_info "done"
