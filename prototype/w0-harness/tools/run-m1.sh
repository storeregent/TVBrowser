#!/usr/bin/env bash
# W0 harness tools (M1.1 D-5): drive the M1 device run non-interactively where
# possible, using the `am start` hooks documented in the README ("adb automation").
#
# Usage: ./run-m1.sh [application_id]
#   application_id defaults to dev.tvbrowse.w0harness (pass .alt for the alt flavor).
# Requires: adb connected to the TV, python3 NOT required.
#
# Sequence: device props -> 5x cold start -> Device Info export -> open each
# bundled test URL -> Export all -> pull logs. Outputs land in ./m1-run-<ts>/.
set -euo pipefail

APP_ID="${1:-dev.tvbrowse.w0harness}"
ACTIVITY="${APP_ID}/.EntryActivity"
OUT="m1-run-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
REMOTE_LOGS="/sdcard/Android/data/$APP_ID/files/w0logs/"

echo "== M1 run for $APP_ID -> $OUT/ =="

# ---------------------------------------------------------------- 1. device props
{
  echo "== getprop ro.config.low_ram =="
  adb shell getprop ro.config.low_ram
  echo "== getprop ro.product.cpu.abilist =="
  adb shell getprop ro.product.cpu.abilist
  echo "== getprop ro.build.fingerprint =="
  adb shell getprop ro.build.fingerprint
  echo "== renderer processes (ps -A | grep -i sandboxed) =="
  adb shell "ps -A | grep -i sandboxed" || echo "(none — no page rendered yet, or single-process WebView)"
} > "$OUT/device-properties.txt" 2>&1 || true
echo "1/6 device properties saved"

# ---------------------------------------------------------------- 2. five cold starts
: > "$OUT/cold-start-runs.txt"
for i in 1 2 3 4 5; do
  adb shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
  sleep 2
  echo "-- run $i --" >> "$OUT/cold-start-runs.txt"
  adb shell am start -W -n "$ACTIVITY" 2>&1 | grep -E "Status|TotalTime|WaitTime" >> "$OUT/cold-start-runs.txt" || true
  sleep 3
done
echo "2/6 five cold starts measured"

# ---------------------------------------------------------------- 3. device info export
adb shell am start -n "$ACTIVITY" -e w0h.action export_deviceinfo >/dev/null 2>&1 || true
sleep 6
echo "3/6 Device Info export requested (see DEVICE_INFO_EXPORTED in the session log)"

# ---------------------------------------------------------------- 4. open each bundled test URL
ASSETS_JSON="$(dirname "$0")/../harness/src/main/assets/test-urls.json"
if [ -f "$ASSETS_JSON" ]; then
  grep -o '"url"[: ]*"[^"]*"' "$ASSETS_JSON" | cut -d'"' -f4 | while IFS= read -r url; do
    [ -n "$url" ] || continue
    case "$url" in *'"'*|*'\'*) continue ;; esac
    echo "   opening $url"
    adb shell am start -n "$ACTIVITY" -e w0h.url "$url" >/dev/null 2>&1 || true
    sleep 8
  done
else
  echo "4/6 SKIPPED (test-urls.json not found next to this script)"
fi
echo "4/6 test URLs opened"

# ---------------------------------------------------------------- 5. export all
adb shell am start -n "$ACTIVITY" -e w0h.action export_all >/dev/null 2>&1 || true
sleep 6
echo "5/6 Export all requested (see EXPORT_ALL in the session log)"

# ---------------------------------------------------------------- 6. pull logs
adb pull "$REMOTE_LOGS" "$OUT/w0logs/" >/dev/null 2>&1 || echo "   (log pull failed — is the app installed?)"
echo "6/6 logs pulled to $OUT/w0logs/ (if present)"

echo ""
echo "Done. Outputs:"
ls -la "$OUT" 2>/dev/null || true
echo ""
echo "Also run tools/logcat.sh in a second terminal during the run for the W0H logcat mirror."
