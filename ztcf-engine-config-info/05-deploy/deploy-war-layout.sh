#!/usr/bin/env bash
set -euo pipefail
CATALINA_BASE=${CATALINA_BASE:-/app/tomcat-nsight}
WAR_SOURCE=${WAR_SOURCE:-/app/artifacts}
mkdir -p "$CATALINA_BASE/webapps"

# Gradle 산출물 별칭 → Tomcat 배포명 (context = 파일명 기준, batch=/batch)
copy_war() {
  local dest="$1"
  shift
  local src
  for src in "$@"; do
    if [[ -f "$WAR_SOURCE/$src" ]]; then
      cp "$WAR_SOURCE/$src" "$CATALINA_BASE/webapps/$dest"
      echo "OK  $src -> $dest"
      return 0
    fi
  done
  echo "WARN: missing sources for $dest (tried: $*)" >&2
  return 1
}

copy_war ic.war    ic.war || true
copy_war pc.war    pc.war || true
copy_war ms.war    ms.war || true
copy_war sv.war    sv.war || true
copy_war pd.war    pd.war || true
copy_war eb.war    eb.war || true
copy_war ep.war    ep.war || true
copy_war ss.war    ss.war || true
copy_war mg.war    mg.war || true
copy_war om.war    om.war tcf-om.war || true
copy_war batch.war batch.war tcf-batch.war || true
copy_war ui.war    ui.war tcf-ui.war || true
copy_war jwt.war   jwt.war tcf-jwt.war || true
