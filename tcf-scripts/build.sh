#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

SERVICE_CODES=(ic pc ms sv pd eb ep ss mg)

usage() {
  cat <<'EOF'

Usage: build.sh <target> [target2 ...]

Targets:
  all       clean + buildBusinessWars (10 WAR)
  wars      buildBusinessWars only
  ztomcat   buildZtomcatWars (12 WAR: + batch + ui)
  tcf       tcf-util, tcf-core, tcf-web
  common    (removed — use tcf-om)
  ui        tcf-ui bootJar
  batch     tcf-batch bootWar
  services  all *-service modules + tcf-om bootWar
  sv ic     service code (ex: sv -> sv-service)
  tcf-om    tcf-om bootWar

Examples:
  ./build.sh sv
  ./build.sh tcf sv
  ./build.sh ztomcat
  ./build.sh all

Gradle: GRADLE_HOME_OVERRIDE > GRADLE_HOME > PATH

EOF
  exit 1
}

is_service_code() {
  local code="$1"
  for c in "${SERVICE_CODES[@]}"; do
    [[ "$c" == "$code" ]] && return 0
  done
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
  echo "[build] gradle not found. Set GRADLE_HOME or GRADLE_HOME_OVERRIDE." >&2
  exit 1
}

resolve_gradle
"${GRADLE}" --stop >/dev/null 2>&1 || true

TASKS=()
append() { TASKS+=("$1"); }

resolve_target() {
  local target
  target="$(echo "$1" | tr '[:upper:]' '[:lower:]')"

  case "$target" in
    all)
      echo "[build] ${GRADLE} clean buildBusinessWars"
      "${GRADLE}" clean buildBusinessWars
      exit 0
      ;;
    wars) append buildBusinessWars ;;
    ztomcat) append buildZtomcatWars ;;
    tcf)
      append :tcf-util:build
      append :tcf-core:build
      append :tcf-web:build
      ;;
    common)
      echo "[build] common-etc module was removed. Use tcf-om for shared features." >&2
      exit 1
      ;;
    ui|tcf-ui) append :tcf-ui:bootJar ;;
    batch|tcf-batch) append :tcf-batch:bootWar ;;
    tcf-om|om) append :tcf-om:bootWar ;;
    tcf-util) append :tcf-util:build ;;
    tcf-core) append :tcf-core:build ;;
    tcf-web) append :tcf-web:build ;;
    tcf-cache) append :tcf-cache:build ;;
    services)
      for code in "${SERVICE_CODES[@]}"; do
        append ":${code}-service:build"
      done
      append :tcf-om:bootWar
      ;;
    *-service)
      append ":${target}:build"
      ;;
    *)
      if is_service_code "$target"; then
        append ":${target}-service:build"
      else
        echo "[build] Unknown target: $target" >&2
        usage
      fi
      ;;
  esac
}

[[ $# -eq 0 ]] && usage

for arg in "$@"; do
  resolve_target "$arg"
done

[[ ${#TASKS[@]} -eq 0 ]] && usage

echo "[build] ${GRADLE} ${TASKS[*]}"
"${GRADLE}" "${TASKS[@]}"
