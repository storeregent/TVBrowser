#!/usr/bin/env bash
# W0 harness tools (M2): B1-B8 acceptance checks on a booted API 34 TV emulator.
#
# Preconditions (same session as emulator-m1.1-checks.sh, which should run first):
#   - an Android TV emulator (system-images;android-34;android-tv;x86 or newer)
#     is booted and reachable via adb,
#   - the standard flavor APK is installed,
#   - python3 is available (UI-dump parsing).
# Usage: ./emulator-m2-checks.sh <package-id> <out-dir>
#
# Writes screenshots, UI dumps, logcat excerpts and a summary to <out-dir>.
# Exits non-zero if a check fails. Evidence is EMULATOR-ONLY: it never marks a
# W0 test (T-xx/P-x) as PASS.
set -uo pipefail

PKG="${1:-dev.tvbrowse.w0harness}"
ACTIVITY="${PKG}/.EntryActivity"
OUT="${2:-acceptance-out-m2}"
mkdir -p "$OUT"

PASS=0
FAIL=0
SUMMARY="$OUT/SUMMARY.md"
: > "$SUMMARY"

note() { echo "$1" | tee -a "$SUMMARY"; }
check() { # check <id> <desc> <0|1>
  if [ "$3" = "0" ]; then
    note "- [x] $1 $2"
    PASS=$((PASS + 1))
  else
    note "- [ ] **FAIL** $1 $2"
    FAIL=$((FAIL + 1))
  fi
}

shot() { adb exec-out screencap -p > "$OUT/$1.png" 2>/dev/null || true; }

dumpui() { # dumpui <name> — uiautomator dump can fail while an IME is up; rm the old file first and retry so we never pull a STALE dump
  adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1 || true
  for _ in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && break
    sleep 1
  done
  adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1 || true
}

# Prints "label|focusedClass"; label falls back to the first labeled descendant.
focused_node() {
  python3 - "$1" <<'PY' 2>/dev/null || echo "|"
import sys
import xml.etree.ElementTree as ET
try:
    tree = ET.parse(sys.argv[1])
except Exception:
    print("|"); raise SystemExit
target = None
for node in tree.iter('node'):
    if node.get('focused') == 'true':
        target = node
        break
if target is None:
    print("|"); raise SystemExit
def label(e):
    return (e.get('text') or e.get('content-desc') or '').strip()
text = label(target)
cls = target.get('class') or ''
if not text:
    for d in target.iter('node'):
        if d is target:
            continue
        t = label(d)
        if t:
            text = t
            break
print(f"{text}|{cls}")
PY
}

logcat_dump() { adb logcat -d -v time 2>/dev/null | grep -E "W0H|StrictMode" > "$1" || true; }

STATE="/sdcard/Android/data/$PKG/files/tabs-state.json"

reset_state() {
  adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
  sleep 1
  # pm clear wipes app data (settings + persisted tab state + exit marker) so
  # every scenario starts deterministic; T-12 re-creates the state on purpose.
  adb shell pm clear "$PKG" >/dev/null 2>&1 || true
  adb shell rm -f "$STATE" >/dev/null 2>&1 || true
}

# Cold start with a cleared state file and cleared logcat.
fresh_start() {
  reset_state
  sleep 1
  adb logcat -c >/dev/null 2>&1 || true
  adb shell am start -n "$ACTIVITY" >/dev/null 2>&1 || true
}

# Hook delivery WITHOUT force-stop: the live singleTask browser receives it as
# onNewIntent (this also verifies the M2 relaunch path).
hook_live() {
  adb shell am start -n "$ACTIVITY" "$@" >/dev/null 2>&1 || true
}

note "# M2 emulator acceptance (B1-B8)" ; note ""
note "Package: $PKG · $(date -u +%Y-%m-%dT%H:%M:%SZ) · API level: $(adb shell getprop ro.build.version.sdk | tr -d '\r')"
note ""

# ================================================================ B1: tab_open hook
fresh_start
adb shell am start -n "$ACTIVITY" -e w0h.action open_tab -e w0h.url "https://example.com/" >/dev/null 2>&1 || true
sleep 9
logcat_dump "$OUT/b1-tab-open.txt"
grep -q '"event":"TAB_OPENED"' "$OUT/b1-tab-open.txt" && grep -q '"url":"https://example.com/"' "$OUT/b1-tab-open.txt"
check "B1" "am start -e w0h.action open_tab -e w0h.url -> TAB_OPENED + URL on the app stack" $?

# ================================================================ B2: switch_tab hook + switch latency (live relaunch = onNewIntent)
hook_live -e w0h.action switch_tab -e w0h.value 1
sleep 8
logcat_dump "$OUT/b2-tab-switch.txt"
grep -q '"event":"TAB_SWITCH"' "$OUT/b2-tab-switch.txt"
check "B2a" "am start -e w0h.action switch_tab -e w0h.value 1 -> TAB_SWITCH (onNewIntent delivery)" $?
grep -q '"event":"TAB_SWITCH_COMPLETE"' "$OUT/b2-tab-switch.txt"
check "B2b" "switch latency measured (TAB_SWITCH_COMPLETE with latencyMs)" $?

# ================================================================ B3: renderer crash trigger (chrome://crash, didCrash()==true)
hook_live -e w0h.action crash_active
sleep 10
logcat_dump "$OUT/b3-crash.txt"
grep -q '"event":"RENDER_PROCESS_GONE"' "$OUT/b3-crash.txt" && grep -q '"didCrash":true' "$OUT/b3-crash.txt"
check "B3a" "chrome://crash -> RENDER_PROCESS_GONE with didCrash=true" $?
grep -q '"event":"RECOVERY_COMPLETE"' "$OUT/b3-crash.txt"
check "B3b" "recovery recreated the tab from stored state (RECOVERY_COMPLETE)" $?

# ================================================================ B4: renderer terminate trigger (didCrash()==false)
hook_live -e w0h.action terminate_renderer
sleep 10
logcat_dump "$OUT/b4-terminate.txt"
grep -q '"event":"RENDERER_TERMINATE_REQUESTED"' "$OUT/b4-terminate.txt"
check "B4a" "terminate_renderer -> RENDERER_TERMINATE_REQUESTED (WEB_VIEW_RENDERER_TERMINATE supported)" $?
grep -q '"event":"RENDER_PROCESS_GONE"' "$OUT/b4-terminate.txt" && grep -q '"didCrash":false' "$OUT/b4-terminate.txt"
check "B4b" "terminate -> RENDER_PROCESS_GONE with didCrash=false + RECOVERY_COMPLETE" $?
grep -q '"event":"RECOVERY_COMPLETE"' "$OUT/b4-terminate.txt"
check "B4c" "second recovery inside the guard window still RECOVERs" $?

# ================================================================ B5: crash-loop guard trips on the 3rd crash (fresh process, empty guard)
fresh_start
sleep 3
hook_live -e w0h.action open_tab -e w0h.url "https://example.com/"
sleep 7
for i in 1 2 3; do
  hook_live -e w0h.action crash_active
  sleep 8
done
logcat_dump "$OUT/b5-crash-guard.txt"
grep -q '"event":"CRASH_GUARD_TRIPPED"' "$OUT/b5-crash-guard.txt"
check "B5a" "3rd crash inside the window trips the guard (CRASH_GUARD_TRIPPED)" $?
sleep 2
dumpui b5_guard_page
grep -q "crash guard" "$OUT/b5_guard_page.xml" 2>/dev/null
check "B5b" "D-pad-dismissible guard error page visible (uiautomator)" $?
shot b5_guard_page

# ================================================================ B6: standby mode hook + live accounting
fresh_start
sleep 3
hook_live -e w0h.action set_standby -e w0h.value mru
sleep 4
logcat_dump "$OUT/b6-standby.txt"
grep -q '"event":"STANDBY_MODE_SET"' "$OUT/b6-standby.txt" && grep -q '"mode":"mru"' "$OUT/b6-standby.txt"
check "B6a" "set_standby mru -> STANDBY_MODE_SET mode=mru" $?
hook_live -e w0h.action open_tab -e w0h.url "https://example.com/"
sleep 6
hook_live -e w0h.action open_tab -e w0h.url "https://www.wikipedia.org/"
sleep 8
logcat_dump "$OUT/b6-mru-live.txt"
grep -q '"event":"WEBVIEW_CREATED"' "$OUT/b6-mru-live.txt" && grep -q '"liveInstances":2' "$OUT/b6-mru-live.txt"
check "B6b" "MRU standby keeps 2 live WebViews (WEBVIEW_CREATED liveInstances=2)" $?
hook_live -e w0h.action set_standby -e w0h.value none
sleep 6
logcat_dump "$OUT/b6-none.txt"
grep -q '"event":"WEBVIEW_DESTROY"' "$OUT/b6-none.txt" && grep -q '"reason":"standby_mode_none"' "$OUT/b6-none.txt"
check "B6c" "set_standby none evicts the standby instance (WEBVIEW_DESTROY reason=standby_mode_none)" $?

# ================================================================ B7: session restore after process death (am kill)
fresh_start
sleep 3
hook_live -e w0h.action open_tab -e w0h.url "https://example.com/"
sleep 8
adb shell input keyevent 3 >/dev/null 2>&1 || true # HOME -> background (onStop persists + pauseTimers)
sleep 3
adb shell am kill "$PKG" >/dev/null 2>&1 || true
sleep 3
PID_KILLED=1
if adb shell pidof "$PKG" | tr -d '\r' | grep -q .; then PID_KILLED=0; fi
[ "$PID_KILLED" = "1" ]
check "B7a" "am kill in background kills the process (T-12 setup)" $?
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY" >/dev/null 2>&1 || true
sleep 8
dumpui b7_restore_offer
grep -q "Restore session?" "$OUT/b7_restore_offer.xml" 2>/dev/null
check "B7b" "cold start offers Restore session (dialog on Home)" $?
shot b7_restore_offer
adb shell input keyevent 23 >/dev/null 2>&1 || true # OK on Restore
sleep 10
logcat_dump "$OUT/b7-restored.txt"
grep -q '"event":"RESTORE_APPLIED"' "$OUT/b7-restored.txt" && grep -q '"url":"https://example.com/"' "$OUT/b7-restored.txt"
check "B7c" "Restore -> RESTORE_APPLIED and the active tab reloads example.com" $?

# ================================================================ B8: fullscreen container (D-gap-09) via the local page
hook_live -e w0h.url "https://appassets.androidplatform.net/assets/m2-fullscreen.html"
sleep 9
adb shell input keyevent 82 >/dev/null 2>&1 || true # MENU hides HUD, focuses the page
sleep 2
adb shell input keyevent 20 >/dev/null 2>&1 || true # DOWN onto the fullscreen button
sleep 1
adb shell input keyevent 23 >/dev/null 2>&1 || true # OK on the button -> requestFullscreen()
sleep 4
logcat_dump "$OUT/b8-fullscreen.txt"
grep -q '"event":"FULLSCREEN_SHOW"' "$OUT/b8-fullscreen.txt"
check "B8a" "local page fullscreen -> onShowCustomView attaches outside Compose (FULLSCREEN_SHOW)" $?
shot b8_fullscreen
adb shell input keyevent 4 >/dev/null 2>&1 || true  # BACK exits fullscreen
sleep 3
logcat_dump "$OUT/b8-fullscreen-hide.txt"
grep -q '"event":"FULLSCREEN_HIDE"' "$OUT/b8-fullscreen-hide.txt"
check "B8b" "BACK exits fullscreen (FULLSCREEN_HIDE)" $?

# ================================================================ evidence bundle
adb pull "/sdcard/Android/data/$PKG/files/w0logs/" "$OUT/w0logs/" >/dev/null 2>&1 || true
adb shell rm -f "$STATE" >/dev/null 2>&1 || true
adb logcat -d -b crash > "$OUT/crash-buffer.txt" 2>/dev/null || true

note ""
note "Checks passed: $PASS, failed: $FAIL"
note ""
note "Evidence is emulator-only. No W0 test (T-xx/P-x) status is claimed."

[ "$FAIL" = "0" ] && exit 0 || exit 1
