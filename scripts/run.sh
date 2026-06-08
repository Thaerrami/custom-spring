#!/usr/bin/env bash
# =============================================================================
# run.sh — Execute the main class declared in pom.json
#
# Classpath = compiled classes (out/classes) + dependency JARs (lib/*)
# Learn: docs/build-system.md → "Attach to classpath"
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MAIN_CLASS="$(python3 -c "import json; print(json.load(open('pom.json'))['java']['mainClass'])")"
OUTPUT_DIR="$(python3 -c "import json; print(json.load(open('pom.json'))['java']['outputDir'])")"

if [ ! -d "$OUTPUT_DIR" ]; then
  echo "Output not found. Run ./scripts/build.sh first." >&2
  exit 1
fi

CLASSPATH="$OUTPUT_DIR"
if compgen -G "lib/*.jar" > /dev/null; then
  CLASSPATH="$OUTPUT_DIR:lib/*"
fi

echo "🚀 Running $MAIN_CLASS"
echo "   Classpath: $OUTPUT_DIR + lib/*.jar"
echo
java -cp "$CLASSPATH" "$MAIN_CLASS"
