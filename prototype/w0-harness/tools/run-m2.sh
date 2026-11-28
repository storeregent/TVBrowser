#!/usr/bin/env bash
# W0 harness tools (M2): drive the M2 scenarios non-interactively where possible,
# using the `am start` hooks documented in the README ("adb automation").
#
# Usage: ./run-m2.sh [application_id] [out_dir]
#   application_id defaults to dev.tvbrowse.w0harness (pass ...w0harness.alt for the alt flavor).
# Requires: adb connected to the TV/emulator.
#
# Sequence: device props -> tab open/switch hooks -> standby modes -> renderer
# crash + terminate triggers -> crash-loop guard -> session restore (am kill) ->
# fullscreen local page -> meminfo sampling -> pull logs.
# Outputs land in ./<out_dir>/ (default m2-run-<ts>/).
set -uo pipefail

APP_ID="${1:-dev.tvbrowse.w0harness}"
OUT="${2:-m2-run-$(date +%Y%m%d-%H%M%S)}"
ACTIVITY="${APP_ID}/.EntryActivity"
STATE="/sdcard/Android/data/$APP_ID/files/tabs-state.json"
mkdir -p "$OUT"

log_dump() { adb logcat -d -v time | grep -E "W0H|StrictMode" > "$OUT/$1" || true; }

echo "== M2 run for $APP_ID -> $OUT/ =="

# ---------------------------------------------------------------- 1. device props
{
  echo "== getprop ro.product.cpu.abilist =="
  adb shell getprop ro.product.cpu.abilist
  echo "== renderer processes (ps -A | grep -i sandboxed) =="
  adb shell "ps -A | grep -i sandboxed" || echo "(none yet)"
} > "$OUT/device-props.txt" 2>&1

# ---------------------------------------------------------------- 2. tabs: open two, switch back
adb shell am force-stop "$APP_ID" >/dev/null 2>&1 || true; sleep 2
adb shell rm -f "$STATE" >/dev/null 2>&1 || true
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY" -e w0h.action open_tab -e w0h.url "https://example.com/" >/dev/null 2>&1
sleep 8
adb shell am start -n "$ACTIVITY" -e w0h.action open_tab -e w0h.url "https://www.wikipedia.org/" >/dev/null 2>&1
sleep 8
adb shell am start -n "$ACTIVITY" -e w0h.action switch_tab -e w0h.value 1 >/dev/null 2>&1
sleep 8
log_dump "m2-tabs.txt"

# ---------------------------------------------------------------- 3. standby modes
adb shell am start -n "$ACTIVITY" -e w0h.action set_standby -e w0h.value prewarm >/dev/null 2>&1; sleep 4
adb shell am start -n "$ACTIVITY" -e w0h.action set_standby -e w0h.value mru >/dev/null 2>&1; sleep 4
adb shell am start -n "$ACTIVITY" -e w0h.action set_standby -e w0h.value none >/dev/null 2>&1; sleep 4
log_dump "m2-standby.txt"

# ---------------------------------------------------------------- 4. renderer triggers + crash-loop guard
adb shell am start -n "$ACTIVITY" -e w0h.action crash_active >/dev/null 2>&1; sleep 10
log_dump "m2-crash-1.txt"
adb shell am start -n "$ACTIVITY" -e w0h.action terminate_renderer >/dev/null 2>&1; sleep 10
log_dump "m2-terminate.txt"
adb shell am start -n "$ACTIVITY" -e w0h.action crash_active >/dev/null 2>&1; sleep 10
log_dump "m2-crash-guard.txt"

# ---------------------------------------------------------------- 5. session restore (am kill)
adb shell am force-stop "$APP_ID" >/dev/null 2>&1 || true; sleep 2
adb shell rm -f "$STATE" >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY" -e w0h.action open_tab -e w0h.url "https://example.com/" >/dev/null 2>&1
sleep 8
adb shell input keyevent 3 >/dev/null 2>&1   # HOME -> background
sleep 3
adb shell am kill "$APP_ID" >/dev/null 2>&1 || true
sleep 3
adb shell am start -n "$ACTIVITY" >/dev/null 2>&1   # cold start -> Restore session?
sleep 8
adb exec-out screencap -p > "$OUT/restore-offer.png" 2>/dev/null || true
adb shell input keyevent 23 >/dev/null 2>&1   # OK -> Restore
sleep 10
log_dump "m2-restore.txt"

# ---------------------------------------------------------------- 6. fullscreen (local page)
adb shell am start -n "$ACTIVITY" -e w0h.url "https://appassets.androidplatform.net/assets/m2-fullscreen.html" >/dev/null 2>&1
sleep 9
adb shell input keyevent 82 >/dev/null 2>&1   # MENU -> page focus
sleep 2
adb shell input keyevent 20 >/dev/null 2>&1   # DOWN -> button
sleep 1
adb shell input keyevent 23 >/dev/null 2>&1   # OK -> requestFullscreen()
sleep 4
adb exec-out screencap -p > "$OUT/fullscreen.png" 2>/dev/null || true
adb shell input keyevent 4 >/dev/null 2>&1    # BACK -> exit fullscreen
sleep 3
log_dump "m2-fullscreen.txt"

# ---------------------------------------------------------------- 7. memory sampling (P3 method, brief §6)
adb shell am start -n "$ACTIVITY" -e w0h.url "https://www.wikipedia.org/" >/dev/null 2>&1
sleep 6
./meminfo.sh 5 6 || echo "(meminfo.sh failed; sampling skipped)" | tee -a "$OUT/meminfo-error.txt"
mv w0h-meminfo-*.txt "$OUT/" 2>/dev/null || true

# ---------------------------------------------------------------- 8. pull logs
adb pull "/sdcard/Android/data/$APP_ID/files/w0logs/" "$OUT/w0logs/" >/dev/null 2>&1 || true

echo ""
echo "== M2 run complete. Artifacts in $OUT/ =="
echo "   Key log markers to check manually:"
echo "   - m2-tabs.txt: TAB_OPENED x2, TAB_SWITCH, TAB_SWITCH_COMPLETE (latencyMs)"
echo "   - m2-standby.txt: STANDBY_MODE_SET x3"
echo "   - m2-crash-guard.txt: RENDER_PROCESS_GONE, CRASH_GUARD_TRIPPED"
echo "   - m2-restore.txt: RESTORE_OFFERED, RESTORE_APPLIED"
echo "   - m2-fullscreen.txt: FULLSCREEN_SHOW, FULLSCREEN_HIDE"
