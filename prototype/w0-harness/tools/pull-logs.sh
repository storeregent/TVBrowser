#!/usr/bin/env bash
# W0 harness tools (brief §6): pull the JSONL session logs from the device.
# Usage: ./pull-logs.sh [application_id]
#   application_id defaults to dev.tvbrowse.w0harness; pass
#   dev.tvbrowse.w0harness.alt for the alt flavor.
set -euo pipefail

APP_ID="${1:-dev.tvbrowse.w0harness}"
REMOTE="/sdcard/Android/data/$APP_ID/files/w0logs/"
OUT="w0logs-pulled-$(date +%Y%m%d-%H%M%S)"

echo "Pulling $REMOTE -> $OUT/"
adb pull "$REMOTE" "$OUT"
echo "Done. Session files are in $OUT/"
