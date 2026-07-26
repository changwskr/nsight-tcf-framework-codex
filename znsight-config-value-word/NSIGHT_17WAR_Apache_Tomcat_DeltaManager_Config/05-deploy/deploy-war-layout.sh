#!/usr/bin/env bash
set -euo pipefail
CATALINA_BASE=${CATALINA_BASE:-/app/tomcat-nsight}
WAR_SOURCE=${WAR_SOURCE:-/app/artifacts}
mkdir -p "$CATALINA_BASE/webapps"
for war in nsight-auth.war nsight-portal.war nsight-customer-service.war nsight-singleview-service.war nsight-campaign-service.war nsight-event-service.war nsight-platform-service.war nsight-ebm-service.war nsight-message-service.war nsight-segment-service.war nsight-report-service.war nsight-stat-service.war nsight-admin-service.war nsight-code-service.war nsight-file-service.war nsight-audit-service.war nsight-batch-admin.war; do
  cp "$WAR_SOURCE/$war" "$CATALINA_BASE/webapps/$war"
done
