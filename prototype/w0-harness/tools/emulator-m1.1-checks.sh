#!/usr/bin/env bash
# W0 harness tools (M1.1): A1-A8 acceptance checks on a booted API 34 TV emulator.
#
# Preconditions:
#   - an Android TV emulator (system-images;android-34;android-tv;x86_64 or newer)
#     is booted and reachable via adb,
#   - the standard flavor APK is installed,
#   - python3 is available (UI-dump parsing).
# Usage: ./emulator-m1.1-checks.sh <package-id> <out-dir>
#
# Writes screenshots, UI dumps, logcat excerpts and a summary to <out-dir>.
# Exits non-zero if a check fails. Evidence is EMULATOR-ONLY: it never marks a
# W0 test (T-xx/P-x) as PASS.
set -uo pipefail

PKG="${1:-dev.tvbrowse.w0harness}"
ACTIVITY="${PKG}/.EntryActivity"
OUT="${2:-acceptance-out}"
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

dumpui() { # dumpui <name>
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1 || true
}

# Prints "focusedText|focusedClass" (empty if no focused node found).
focused_node() {
  python3 - "$1" <<'PY' 2>/dev/null || echo "|"
import sys
import xml.etree.ElementTree as ET
try:
    tree = ET.parse(sys.argv[1])
except Exception:
    print("|"); raise SystemExit
for node in tree.iter('node'):
    if node.get('focused') == 'true':
        text = (node.get('text') or node.get('content-desc') or '').strip()
        print(f"{text}|{node.get('class') or ''}")
        raise SystemExit
print("|")
PY
}

logcat_dump() { adb logcat -d -v time 2>/dev/null | grep -E "W0H|StrictMode" > "$1" || true; }

wait_boot_or_app() { # wait until our UI text appears
  for _ in $(seq 1 30); do
    dumpui probe
    if grep -q "TVBrowse W0 Harness" "$OUT/probe.xml" 2>/dev/null; then return 0; fi
    sleep 2
  done
  return 1
}

navigate_to_url_entry() { # from initial focus: UP -> button row, LEFT x4 -> Device Info, UP -> URL entry
  adb shell input keyevent 19; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 19; sleep 1.5
}

fresh_launch() {
  adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
  sleep 2
  adb shell am start -n "$ACTIVITY" >/dev/null 2>&1 || true
  wait_boot_or_app
}

# ================================================================= A1: initial focus
note "# M1.1 emulator acceptance (A1-A8)" ; note ""
note "Package: $PKG · $(date -u +%Y-%m-%dT%H:%M:%SZ) · API level: $(adb shell getprop ro.build.version.sdk | tr -d '\r')"
note ""
adb logcat -c >/dev/null 2>&1 || true
fresh_launch || true
sleep 3
shot a1_home_initial
dumpui a1_home_initial
FOCUSED="$(focused_node "$OUT/a1_home_initial.xml")"
FOCUSED_TEXT="${FOCUSED%%|*}"; FOCUSED_CLASS="${FOCUSED##*|}"
IME_SHOWN="$(adb shell dumpsys input_method 2>/dev/null | grep -c 'mInputShown=true' | tr -d '\r')"
[ "$IME_SHOWN" = "0" ]; check "A1" "fresh launch: IME not open (mInputShown=$IME_SHOWN)" $?
[ "$FOCUSED_CLASS" != "android.widget.EditText" ] && [ "$FOCUSED_CLASS" != "androidx.compose.ui.platform.ComposeView" ]; check "A1" "initial focus is a non-text element (focused: '$FOCUSED_TEXT' class=$FOCUSED_CLASS)" $?

# ================================================================= A2: D-pad traversal coverage
declare -a VISITED=()
KEYS=(19 21 21 21 21 19 22 20 20) # UP, LEFT x4, UP, RIGHT, DOWN, DOWN
STEP=0
for key in "${KEYS[@]}"; do
  adb shell input keyevent "$key" >/dev/null 2>&1 || true
  sleep 1.2
  STEP=$((STEP + 1))
  dumpui "a2_step$STEP"
  F="$(focused_node "$OUT/a2_step$STEP.xml")"
  VISITED+=("$F")
  shot "a2_step$STEP"
done
{ echo "A2 traversal (key -> focusedText|focusedClass):"
  for i in "${!VISITED[@]}"; do echo "  step$((i+1)): ${VISITED[$i]}"; done
} > "$OUT/a2-focus-traversal.txt"
COVERAGE_BAD=0
for want in "Go" "Device Info" "Logs" "Settings" "Tests" "Mark step" "Example.com"; do
  grep -q "$want" "$OUT/a2-focus-traversal.txt" || { COVERAGE_BAD=1; note "   (A2: '$want' never focused)"; }
done
grep -q "EditText" "$OUT/a2-focus-traversal.txt"; [ $? -ne 0 ]; check "A2" "D-pad reaches every Home element, never traps in a text field" $?
[ "$COVERAGE_BAD" = "0" ]; check "A2" "coverage: URL/Go/buttons/first list item all reached" $?

# ================================================================= A3: URL entry edit modes
adb logcat -c >/dev/null 2>&1 || true
fresh_launch || true
sleep 2
navigate_to_url_entry
dumpui a3_before_ok
adb shell input keyevent 23 >/dev/null 2>&1 || true   # OK -> edit mode
sleep 2
shot a3_edit_mode
dumpui a3_edit_mode
F="$(focused_node "$OUT/a3_edit_mode.xml")"; FC="${F##*|}"
[ "$FC" = "android.widget.EditText" ]; check "A3" "OK on URL entry switches to edit mode (focused class=$FC)" $?
adb shell input text "example.com" >/dev/null 2>&1 || true
sleep 1
adb shell input keyevent 66 >/dev/null 2>&1 || true   # ENTER = IME Done -> commit
sleep 8
shot a3_after_done
logcat_dump "$OUT/a3-logcat.txt"
grep -q "LOAD_URL" "$OUT/a3-logcat.txt" && grep -qi "example.com" "$OUT/a3-logcat.txt"
check "A3" "IME Done commits and loads the URL (LOAD_URL in log)" $?

# ... then edit mode + DPAD_DOWN leaves WITHOUT loading
adb logcat -c >/dev/null 2>&1 || true
fresh_launch || true
sleep 2
navigate_to_url_entry
adb shell input keyevent 23 >/dev/null 2>&1 || true   # OK -> edit mode
sleep 2
adb shell input keyevent 20 >/dev/null 2>&1 || true   # DPAD_DOWN -> exit edit, no load
sleep 2
shot a3_down_exit
dumpui a3_down_exit
F="$(focused_node "$OUT/a3_down_exit.xml")"; FC="${F##*|}"
logcat_dump "$OUT/a3-down-logcat.txt"
[ "$FC" != "android.widget.EditText" ]; check "A3" "D-pad Down leaves edit mode (focused class=$FC)" $?
grep -q "LOAD_URL" "$OUT/a3-down-logcat.txt"; [ $? -ne 0 ]; check "A3" "Down-exit does NOT load a page" $?
grep -q "url_edit_exit" "$OUT/a3-down-logcat.txt"; check "A3" "BACK_DISPATCH/exit logged (handledBy=url_edit_exit path)" $?

# ================================================================= A5: adb hooks
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY" -e w0h.url "https://example.com/" >/dev/null 2>&1 || true
sleep 8
logcat_dump "$OUT/a5-hook-url.txt"
grep -q "NAV_REQUEST" "$OUT/a5-hook-url.txt" && grep -q "adb" "$OUT/a5-hook-url.txt"
check "A5" "am start -e w0h.url -> NAV_REQUEST (source=adb)" $?

adb shell am start -n "$ACTIVITY" -e w0h.screen tests >/dev/null 2>&1 || true
sleep 3
dumpui a5_screen_tests
grep -q "Tests" "$OUT/a5_screen_tests.xml" 2>/dev/null
check "A5" "am start -e w0h.screen tests -> Tests screen" $?

adb shell am start -n "$ACTIVITY" -e w0h.action export_deviceinfo >/dev/null 2>&1 || true
sleep 8
logcat_dump "$OUT/a5-hook-export-info.txt"
grep -q "DEVICE_INFO_EXPORTED" "$OUT/a5-hook-export-info.txt"
check "A5" "am start -e w0h.action export_deviceinfo -> DEVICE_INFO_EXPORTED" $?

adb shell am start -n "$ACTIVITY" -e w0h.action export_all >/dev/null 2>&1 || true
sleep 8
logcat_dump "$OUT/a5-hook-export-all.txt"
grep -q "EXPORT_ALL" "$OUT/a5-hook-export-all.txt"
check "A5" "am start -e w0h.action export_all -> EXPORT_ALL" $?

adb shell am start -n "$ACTIVITY" -e w0h.action mark_step -e w0h.label ADB_MARK_TEST >/dev/null 2>&1 || true
sleep 4
logcat_dump "$OUT/a5-hook-mark.txt"
grep -q "ADB_MARK_TEST" "$OUT/a5-hook-mark.txt"
check "A5" "am start -e w0h.action mark_step -e w0h.label -> MARK line" $?

adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
sleep 2
adb shell am start -n "$ACTIVITY" --ez w0h.locked false >/dev/null 2>&1 || true
sleep 8
logcat_dump "$OUT/a5-hook-unlocked.txt"
grep -q '"variant": "unlocked"' "$OUT/a5-hook-unlocked.txt"
check "A5" "am start --ez w0h.locked false -> Unlocked variant used" $?

# ================================================================= A6: StrictMode / no main-thread file I/O in SessionLog
adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
sleep 2
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY" >/dev/null 2>&1 || true
sleep 10
adb shell input keyevent 20 >/dev/null 2>&1 || true
sleep 2
adb shell input keyevent 20 >/dev/null 2>&1 || true
sleep 2
logcat_dump "$OUT/a6-strictmode.txt"
VIOLATIONS="$(grep -A 25 "StrictModeDiskWriteViolation" "$OUT/a6-strictmode.txt" 2>/dev/null | grep -c "dev.tvbrowse.w0harness" || true)"
[ "${VIOLATIONS:-0}" = "0" ]
check "A6" "no StrictMode disk-write violations from harness classes ($VIOLATIONS found)" $?
note "  - full StrictMode excerpt: a6-strictmode.txt (harness-related violations only fail this check)"

# ================================================================= A7: standard-flavor Back at Home
adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
sleep 2
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY" >/dev/null 2>&1 || true
sleep 10
adb shell input keyevent 4 >/dev/null 2>&1 || true   # BACK at Home
sleep 3
logcat_dump "$OUT/a7-back.txt"
grep -q "BACK_DISPATCH" "$OUT/a7-back.txt" && grep -q "activity_finish" "$OUT/a7-back.txt"
check "A7" "Back at Home logged (BACK_DISPATCH handledBy=activity_finish)" $?
PID_AFTER="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
[ -z "$PID_AFTER" ]
check "A7" "Back at Home falls through to the system: process exited (pid='$PID_AFTER')" $?

# ================================================================= evidence bundle: pull session logs
adb pull "/sdcard/Android/data/$PKG/files/w0logs/" "$OUT/w0logs/" >/dev/null 2>&1 || true

note ""
note "Checks passed: $PASS, failed: $FAIL"
note ""
note "Evidence is emulator-only. No W0 test (T-xx/P-x) status is claimed."

[ "$FAIL" = "0" ] && exit 0 || exit 1
