#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WEBAPPS="${TOMCAT_WEBAPPS:-${PROJECT_HOME}/ztomcat/apache-tomcat-10.1.34/webapps}"
MODULE="tcf-gateway"
GRADLE="${GRADLE:-gradle}"

cd "${PROJECT_HOME}"
"${GRADLE}" ":${MODULE}:bootWar"

SRC="${PROJECT_HOME}/${MODULE}/build/libs/gw.war"
if [[ ! -f "${SRC}" ]]; then
  echo "[gw-deploy] WAR not found: ${SRC}" >&2
  exit 1
fi

rm -rf "${WEBAPPS}/gw"
cp -f "${SRC}" "${WEBAPPS}/gw.war"
echo "[gw-deploy] deployed gw.war"
