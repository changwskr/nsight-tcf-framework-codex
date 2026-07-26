#!/usr/bin/env bash
#
# sv-service Kubernetes status
#
# Usage:
#   ./sv-service/scripts/k8s/k8s-status.sh
#

set -euo pipefail

NAMESPACE="${NAMESPACE:-nsight}"
APP="nsight-sv"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || "${1:-}" == "help" ]]; then
  cat <<EOF
Usage: $0

Env:
  NAMESPACE=nsight
EOF
  exit 0
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[sv-k8s-status] kubectl not found." >&2
  exit 1
fi

echo "[sv-k8s-status] namespace=${NAMESPACE} app=${APP}"
echo ""
echo "=== Deploy / Service / Pod ==="
kubectl get deploy,svc,pods -n "${NAMESPACE}" -l "app=${APP}" -o wide || true
echo ""
echo "=== ConfigMap / Secret ==="
kubectl get configmap,secret -n "${NAMESPACE}" -l "app=${APP}" || true
echo ""
echo "=== Recent events ==="
kubectl get events -n "${NAMESPACE}" --field-selector "involvedObject.name=${APP}" --sort-by=.lastTimestamp 2>/dev/null | tail -n 20 || true
echo ""
if kubectl get deployment "${APP}" -n "${NAMESPACE}" >/dev/null 2>&1; then
  echo "=== Rollout ==="
  kubectl rollout status "deployment/${APP}" -n "${NAMESPACE}" --timeout=5s 2>/dev/null || true
  echo ""
  echo "=== Describe (short) ==="
  kubectl describe deployment "${APP}" -n "${NAMESPACE}" | sed -n '1,40p'
fi
