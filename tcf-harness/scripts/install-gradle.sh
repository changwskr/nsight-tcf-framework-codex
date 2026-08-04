#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
VERSION=8.10.2
TARGET="$ROOT/.gradle-bootstrap"
ZIP="$TARGET/gradle-$VERSION-bin.zip"
mkdir -p "$TARGET"
if [ ! -f "$ZIP" ]; then
  curl --fail --location --retry 3 \
    "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" \
    --output "$ZIP"
fi
rm -rf "$TARGET/gradle-$VERSION"
unzip -q "$ZIP" -d "$TARGET"
printf 'Installed Gradle %s at %s\n' "$VERSION" "$TARGET/gradle-$VERSION"
