#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/build/offline-test"
rm -rf "$OUT"
mkdir -p "$OUT"
find "$ROOT/src/main/java" -name '*.java' ! -name 'HarnessApplication.java' -print0 \
  | xargs -0 javac -encoding UTF-8 -d "$OUT"
javac -encoding UTF-8 -cp "$OUT" -d "$OUT" \
  "$ROOT/src/offlineTest/java/com/nh/nsight/harness/OfflineHarnessSmokeTest.java"
java -cp "$OUT:$ROOT/src/main/resources" com.nh.nsight.harness.OfflineHarnessSmokeTest
java -cp "$OUT:$ROOT/src/main/resources" com.nh.nsight.harness.cli.OfflineHarnessMain help >/dev/null
