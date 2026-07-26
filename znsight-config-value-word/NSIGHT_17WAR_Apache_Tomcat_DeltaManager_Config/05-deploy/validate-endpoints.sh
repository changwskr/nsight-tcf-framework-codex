#!/usr/bin/env bash
set -euo pipefail
BASE_URL=${BASE_URL:-https://nh.marketing.com}
for ctx in auth portal customer-service singleview-service campaign-service event-service platform-service ebm-service message-service segment-service report-service stat-service admin-service code-service file-service audit-service batch-admin; do
  echo "== $ctx =="
  curl -k -fsS "$BASE_URL/$ctx/actuator/health" || echo "health check failed: $ctx"
done
