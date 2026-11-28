#!/usr/bin/env bash
# W0 harness tools (brief §6): logcat capture filtered to the harness tags.
# Usage: ./logcat.sh [serial]
# Works from macOS, Linux or Git-Bash. Stop with Ctrl-C; output file is printed.
set -euo pipefail

SERIAL_ARG=""
if [ "${1:-}" != "" ]; then
  SERIAL_ARG="-s $1"
fi

OUT="w0h-logcat-$(date +%Y%m%d-%H%M%S).log"
echo "Capturing logcat to: $OUT (Ctrl-C to stop)"
adb $SERIAL_ARG logcat -v time W0H:V ActivityManager:I chromium:W '*:S' | tee "$OUT"
