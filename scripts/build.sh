#!/usr/bin/env bash
# =============================================================================
# build.sh — Compile the project using settings from pom.json
#
# Steps (mirrors what Maven does):
#   1. Read pom.json for source/output paths and Java version
#   2. resolve-deps.py → download JARs to lib/
#   3. javac with lib/* on classpath
#   4. Copy src/main/resources → out/classes (classpath root)
#
# Learn: docs/build-system.md
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

eval "$(python3 - <<'PY'
import json
with open("pom.json") as f:
    p = json.load(f)
j = p["java"]
version = j.get("version", "17")
print('SOURCE_DIR="' + j["sourceDir"] + '"')
print('RESOURCES_DIR="' + j["resourcesDir"] + '"')
print('OUTPUT_DIR="' + j["outputDir"] + '"')
print('MAIN_CLASS="' + j["mainClass"] + '"')
print('JAVA_VERSION="' + version + '"')
PY
)"

echo "🔧 Custom Spring Build (pom.json driven)"
echo "   Source:    $SOURCE_DIR"
echo "   Resources: $RESOURCES_DIR"
echo "   Output:    $OUTPUT_DIR"
echo "   Main:      $MAIN_CLASS"
echo

python3 scripts/resolve-deps.py

CLASSPATH=""
if ls lib/*.jar >/dev/null 2>&1; then
  CLASSPATH="lib/*"
fi

mkdir -p "$OUTPUT_DIR"

JAVA_FILES=()
while IFS= read -r file; do
  JAVA_FILES+=("$file")
done < <(find "$SOURCE_DIR" -name "*.java" | sort)

if [ ${#JAVA_FILES[@]} -eq 0 ]; then
  echo "ERROR: No .java files found in $SOURCE_DIR" >&2
  exit 1
fi

echo "⚙️  Compiling ${#JAVA_FILES[@]} source file(s)..."
if [ -n "$CLASSPATH" ]; then
  javac --release "$JAVA_VERSION" -d "$OUTPUT_DIR" -cp "$CLASSPATH" "${JAVA_FILES[@]}"
else
  javac --release "$JAVA_VERSION" -d "$OUTPUT_DIR" "${JAVA_FILES[@]}"
fi

if [ -d "$RESOURCES_DIR" ]; then
  echo "📁 Copying resources..."
  cp -R "$RESOURCES_DIR/." "$OUTPUT_DIR/"
fi

echo
echo "✅ Build complete."
echo "   Run: ./scripts/run.sh"
