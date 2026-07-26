#!/usr/bin/env bash
set -euo pipefail

ZTOMCAT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOMCAT_VERSION="10.1.34"
TOMCAT_DIR="apache-tomcat-${TOMCAT_VERSION}"
ARCHIVE="${TOMCAT_DIR}.tar.gz"
DOWNLOAD_URL="https://archive.apache.org/dist/tomcat/tomcat-10/v${TOMCAT_VERSION}/bin/${ARCHIVE}"
TARGET="${ZTOMCAT_HOME}/${TOMCAT_DIR}"

if [[ -f "${TARGET}/bin/catalina.sh" ]]; then
  echo "[ztomcat] Already installed: ${TOMCAT_DIR}"
  exit 0
fi

tmp="${ZTOMCAT_HOME}/${ARCHIVE}"
echo "[ztomcat] Downloading ${TOMCAT_DIR} ..."
if command -v curl >/dev/null 2>&1; then
  curl -fL "${DOWNLOAD_URL}" -o "${tmp}"
elif command -v wget >/dev/null 2>&1; then
  wget -O "${tmp}" "${DOWNLOAD_URL}"
else
  echo "[ztomcat] curl or wget is required."
  exit 1
fi

echo "[ztomcat] Extracting ..."
tar -xzf "${tmp}" -C "${ZTOMCAT_HOME}"
rm -f "${tmp}"
chmod +x "${TARGET}/bin/"*.sh

echo "[ztomcat] Installed to ${TARGET}"
