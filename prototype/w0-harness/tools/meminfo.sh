#!/usr/bin/env bash
# W0 harness tools (brief §6): P3 memory sampling of host + renderer PIDs.
# This is the corrected P3 method (AA R-06, C-16): sample BOTH the host app
# process and every WebView renderer (sandboxed_process) with timestamps.
#
# Usage: ./meminfo.sh [interval_s] [count] [application_id]
#   interval_s     seconds between samples (default 5)
#   count          number of samples (default 12)
#   application_id defaults to dev.tvbrowse.w0harness
set -euo pipefail

INTERVAL="${1:-5}"
COUNT="${2:-12}"
APP_ID="${3:-dev.tvbrowse.w0harness}"
OUT="w0h-meminfo-$(date +%Y%m%d-%H%M%S).txt"

echo "Sampling 'dumpsys meminfo' for $APP_ID every ${INTERVAL}s x$count -> $OUT"
echo "(Start the harness page load you want to measure, then let this run.)"

for i in $(seq 1 "$COUNT"); do
  TS="$(date +%Y-%m-%dT%H:%M:%S)"
  HOST_PID="$(adb shell pidof "$APP_ID" | tr -d '\r' | awk '{print $1}')"
  RENDERERS="$(adb shell "ps -A | grep -i sandboxed_process" | tr -d '\r' | awk '{print $2}' || true)"

  echo "===== sample $i @ $TS  host_pid=$HOST_PID  renderers=[$RENDERERS] =====" >> "$OUT"

  if [ -n "${HOST_PID:-}" ]; then
    adb shell dumpsys meminfo "$HOST_PID" >> "$OUT"
  else
    echo "(host process not running)" >> "$OUT"
  fi

  for RPID in $RENDERERS; do
    echo "----- renderer pid $RPID -----" >> "$OUT"
    adb shell dumpsys meminfo "$RPID" >> "$OUT"
  done

  echo "sample $i/$i done" >&2
  if [ "$i" -lt "$COUNT" ]; then
    sleep "$INTERVAL"
  fi
done

echo "Wrote $OUT"
