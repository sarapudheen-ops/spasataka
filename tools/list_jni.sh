#!/usr/bin/env bash
set -euo pipefail

OUT=tools/jni_symbols.txt
: > "$OUT"

# Adjust path if your module isn't "app"
for so in app/src/main/jniLibs/*/*.so; do
  echo "### $(basename "$so")" >> "$OUT"
  # Use llvm-objdump if nm isn't available on your OS
  nm -D --defined-only "$so" 2>/dev/null | awk '{print $3}' \
    | grep -E '^Java_' || true >> "$OUT"
  echo >> "$OUT"
done

echo "Wrote $OUT"
