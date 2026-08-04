#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
./gradlew wrapper --gradle-version 8.10.2 --distribution-type bin
printf '%s\n' 'Official Gradle Wrapper files generated. Review gradle-wrapper.jar checksum before commit.'
