#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ -n "${TOMCAT_WEBAPPS:-}" ]]; then
  WEBAPPS="${TOMCAT_WEBAPPS}"
elif [[ -d "${PROJECT_HOME}/ztomcat/apache-tomcat-10.1.34/webapps" ]]; then
  WEBAPPS="$(cd "${PROJECT_HOME}/ztomcat/apache-tomcat-10.1.34/webapps" && pwd)"
else
  WEBAPPS="/opt/tomcat/webapps"
fi

BATCH_WARS="${PROJECT_HOME}/ztomcat/wars"
BATCH_XML="${PROJECT_HOME}/ztomcat/conf/Catalina/localhost/batch.xml"

ALL_MODULES=(
  ic-service:ic.war:ic.war:ic
  pc-service:pc.war:pc.war:pc
  ms-service:ms.war:ms.war:ms
  sv-service:sv.war:sv.war:sv
  pd-service:pd.war:pd.war:pd
  eb-service:eb.war:eb.war:eb
  ep-service:ep.war:ep.war:ep
  ss-service:ss.war:ss.war:ss
  mg-service:mg.war:mg.war:mg
  tcf-om:tcf-om.war:om.war:om
)

usage() {
  cat <<EOF
Usage:
  deploy.sh              Build and deploy business WARs + om (10)
  deploy.sh all          Same as above
  deploy.sh sv           Build and deploy one code
  deploy.sh sv cc om     Build and deploy multiple codes
  deploy.sh batch ui     tcf-batch (wars/zz-batch.war) + tcf-ui

Target webapps:
  ${WEBAPPS}
  (override: export TOMCAT_WEBAPPS=/path/to/webapps)

Gradle:
  export GRADLE_HOME=/path/to/gradle-8.10.1
  or export GRADLE_HOME_OVERRIDE=...

Codes: ic pc ms sv pd eb ep ss mg om batch ui
Full 12 WAR: ztomcat/deploy-wars.sh all
EOF
}

resolve_entry() {
  local code
  code="$(echo "$1" | tr '[:upper:]' '[:lower:]')"
  local entry module _src _dest ctx
  case "${code}" in
    tcf-om|om|ud) echo "tcf-om:tcf-om.war:om.war:om"; return 0 ;;
    ui|tcf-ui) echo "tcf-ui:tcf-ui.war:ui.war:ui"; return 0 ;;
    batch|tcf-batch) echo "__batch__"; return 0 ;;
  esac
  for entry in "${ALL_MODULES[@]}"; do
    IFS=':' read -r module _src _dest ctx <<< "${entry}"
    if [[ "${ctx}" == "${code}" ]]; then
      echo "${entry}"
      return 0
    fi
  done
  echo "[deploy] Unknown code: ${1}" >&2
  echo "[deploy] Codes: ic pc ms sv pd eb ep ss mg om batch ui" >&2
  return 1
}

resolve_gradle() {
  local home="${GRADLE_HOME_OVERRIDE:-${GRADLE_HOME:-}}"

  if [[ -n "${home}" && -x "${home}/bin/gradle" ]]; then
    GRADLE="${home}/bin/gradle"
    return 0
  fi
  if command -v gradle >/dev/null 2>&1; then
    GRADLE="$(command -v gradle)"
    return 0
  fi
  echo "[deploy] gradle not found. Set GRADLE_HOME or GRADLE_HOME_OVERRIDE." >&2
  exit 1
}

sync_batch_xml() {
  mkdir -p "$(dirname "${BATCH_XML}")"
  local war_path
  war_path="$(echo "${BATCH_WARS}/zz-batch.war" | sed 's|\\|/|g')"
  cat >"${BATCH_XML}" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<Context docBase="${war_path}" />
EOF
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" || "${1:-}" == "/?" ]]; then
  usage
  exit 0
fi

if [[ ! -d "${WEBAPPS}" ]]; then
  echo "[deploy] webapps directory not found: ${WEBAPPS}" >&2
  exit 1
fi

resolve_gradle

selected=()
batch_deploy=0
deploy_all=0

if [[ $# -eq 0 ]]; then
  deploy_all=1
else
  for arg in "$@"; do
    if [[ "$(echo "${arg}" | tr '[:upper:]' '[:lower:]')" == "all" ]]; then
      deploy_all=1
      break
    fi
  done
fi

if [[ "${deploy_all}" -eq 1 ]]; then
  selected=("${ALL_MODULES[@]}")
  gradle_tasks=(buildBusinessWars)
  echo "[deploy] Building all WAR files (buildBusinessWars) ..."
else
  gradle_tasks=(
    :tcf-util:build
    :tcf-core:build
    :tcf-web:build
  )
  for local_code in "$@"; do
    if [[ "$(echo "${local_code}" | tr '[:upper:]' '[:lower:]')" == "all" ]]; then
      continue
    fi
    resolved="$(resolve_entry "${local_code}")"
    if [[ "${resolved}" == "__batch__" ]]; then
      batch_deploy=1
      gradle_tasks+=(":tcf-batch:bootWar")
      continue
    fi
    selected+=("${resolved}")
    module="${resolved%%:*}"
    gradle_tasks+=(":${module}:bootWar")
  done
  echo "[deploy] Building selected WAR(s): ${gradle_tasks[*]}"
fi

(
  cd "${PROJECT_HOME}"
  "${GRADLE}" "${gradle_tasks[@]}"
)

echo "[deploy] Removing stale exploded directories ..."
for entry in "${selected[@]}"; do
  IFS=':' read -r _module _src _dest ctx <<< "${entry}"
  rm -rf "${WEBAPPS}/${ctx}"
done
if [[ "${batch_deploy}" -eq 1 ]]; then
  rm -rf "${WEBAPPS}/batch"
fi

if [[ "${batch_deploy}" -eq 1 ]]; then
  sync_batch_xml
fi

echo "[deploy] Copying WAR files ..."
for entry in "${selected[@]}"; do
  IFS=':' read -r module src dest _ctx <<< "${entry}"
  from="${PROJECT_HOME}/${module}/build/libs/${src}"
  if [[ -f "${from}" ]]; then
    cp -f "${from}" "${WEBAPPS}/${dest}"
    echo "  deployed ${dest} (from ${src})"
  else
    echo "  missing ${src} in ${module}"
  fi
done

if [[ "${batch_deploy}" -eq 1 ]]; then
  mkdir -p "${BATCH_WARS}"
  from="${PROJECT_HOME}/tcf-batch/build/libs/tcf-batch.war"
  if [[ -f "${from}" ]]; then
    cp -f "${from}" "${BATCH_WARS}/zz-batch.war"
    echo "  deployed wars/zz-batch.war (from tcf-batch.war)"
  else
    echo "  missing tcf-batch.war"
  fi
fi

echo
echo "[deploy] Verifying deployed WAR files ..."
verify_count=0
verify_failed=0
for entry in "${selected[@]}"; do
  IFS=':' read -r _module _src dest _ctx <<< "${entry}"
  target="${WEBAPPS}/${dest}"
  if [[ -f "${target}" ]]; then
    verify_count=$((verify_count + 1))
    echo "  [OK] ${dest}"
    ls -lh "${target}"
  else
    verify_failed=$((verify_failed + 1))
    echo "  [MISSING] ${dest} - not found in ${WEBAPPS}"
  fi
done

if [[ "${batch_deploy}" -eq 1 ]]; then
  if [[ -f "${BATCH_WARS}/zz-batch.war" ]]; then
    verify_count=$((verify_count + 1))
    echo "  [OK] wars/zz-batch.war"
    ls -lh "${BATCH_WARS}/zz-batch.war"
  else
    verify_failed=$((verify_failed + 1))
    echo "  [MISSING] wars/zz-batch.war"
  fi
fi
echo

if [[ "${verify_failed}" -gt 0 ]]; then
  echo "[deploy] Verification failed: ${verify_failed} WAR(s) missing" >&2
  exit 1
fi

echo "[deploy] All ${verify_count} WAR(s) verified"

if [[ "${deploy_all}" -eq 1 ]]; then
  echo "[deploy] Done. For batch/ui use: deploy.sh batch ui  or  ztomcat/deploy-wars.sh all"
  echo "[deploy] Restart Tomcat if it is already running."
else
  echo "[deploy] Done. Tomcat running: context redeploys automatically (~15s)."
fi
