#!/usr/bin/env bash
# W0 harness tools (M1.1): A1-A8 acceptance checks on a booted API 34 TV emulator.
#
# Preconditions:
#   - an Android TV emulator (system-images;android-34;android-tv;x86 or newer)
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

dumpui() { # dumpui <name> — uiautomator dump can fail while an IME is up; rm the old file first and retry so we never pull a STALE dump
  adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1 || true
  for _ in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && break
    sleep 1
  done
  adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1 || true
}

# Prints "label|focusedClass". `label` is the focused node's own text (or
# content-desc); uiautomator dumps of Compose TV surfaces often carry the text
# on a CHILD node, so fall back to the first labeled descendant.
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

# "yes" if the focused node or any of its descendants is an EditText.
focused_edittext() {
  python3 - "$1" <<'PY' 2>/dev/null || echo no
import sys
import xml.etree.ElementTree as ET
try:
    tree = ET.parse(sys.argv[1])
except Exception:
    print("no"); raise SystemExit
target = None
for node in tree.iter('node'):
    if node.get('focused') == 'true':
        target = node
        break
if target is None:
    print("no"); raise SystemExit
for e in target.iter('node'):
    if (e.get('class') or '').endswith('EditText'):
        print("yes"); raise SystemExit
print("no")
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

navigate_to_url_entry() { # from initial focus: UP -> button row, LEFT x4 -> Device Info, UP -> top row (URL entry or Go); fix left if we landed on Go
  adb shell input keyevent 19; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 21; sleep 1
  adb shell input keyevent 19; sleep 1.5
  dumpui nav_probe
  local L
  L="$(focused_node "$OUT/nav_probe.xml")"; L="${L%%|*}"
  if [ "$L" = "Go" ]; then adb shell input keyevent 21; sleep 1.5; fi
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

  # A2: record the pre-keypress initial focus as step0 (Example.com coverage).
  dumpui a2_step0
# ================================================================= A2: D-pad traversal coverage (dump-driven adaptive walk)
# Fixed key sequences are fragile: which row-1 control UP lands on (URL entry
# vs Go) depends on tv-material focus scale. Walk with dump feedback instead.
a2_label() { # dump now, print focused label
  dumpui a2_probe
  local F
  F="$(focused_node "$OUT/a2_probe.xml")"
  echo "${F%%|*}"
}
a2_press() { adb shell input keyevent "$1" >/dev/null 2>&1 || true; sleep 1; }
a2_walk() { # a2_walk <keyevent> <max>: press key until the focused label repeats (edge reached)
  local key="$1" max="$2" prev="" cur=""
  for _ in $(seq 1 "$max"); do
    a2_press "$key"
    cur="$(a2_label)"
    VISITED+=("$cur")
    if [ "$cur" = "$prev" ]; then break; fi
    prev="$cur"
  done
}
dumpui a2_step0
VISITED=("$(a2_label)") # step0 = initial focus (first test URL)
a2_press 19; VISITED+=("$(a2_label)")  # UP -> button row
a2_walk 21 8                           # LEFT: sweep the button row leftwards
CUR="$(a2_label)"
case "$CUR" in
  URL|Go) a2_press 20; VISITED+=("$(a2_label)") ;;  # LEFT jumped up a row: re-enter the button row
esac
a2_walk 22 8                           # RIGHT: sweep rightwards (covers the rest of the row)
CUR="$(a2_label)"
case "$CUR" in
  URL|Go) ;;                                      # RIGHT jumped up to the top row already
  *) a2_press 19; VISITED+=("$(a2_label)") ;;     # UP -> top row (URL entry or Go)
esac
CUR="$(a2_label)"
if [ "$CUR" = "URL" ]; then a2_press 22; else a2_press 21; fi
VISITED+=("$(a2_label)")                # the other top-row control
a2_press 20; VISITED+=("$(a2_label)")   # DOWN -> button row
a2_press 20; VISITED+=("$(a2_label)")   # DOWN -> test URL list
{ echo "A2 traversal (adaptive walk, focusedText|focusedClass):"
  for i in "${!VISITED[@]}"; do echo "  step$i: ${VISITED[$i]}"; done
} > "$OUT/a2-focus-traversal.txt"
COVERAGE_BAD=0
for want in "Go" "Device Info" "Logs" "Settings" "Tests" "Mark step" "Example.com" "URL"; do
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
ET="$(focused_edittext "$OUT/a3_edit_mode.xml")"
{ [ "$FC" = "android.widget.EditText" ] || [ "$ET" = "yes" ]; }
check "A3" "OK on URL entry switches to edit mode (focused: $F)" $?
adb shell input text "example.com" >/dev/null 2>&1 || true
sleep 1
adb shell input keyevent 66 >/dev/null 2>&1 || true   # ENTER = IME Done -> commit
sleep 8
shot a3_after_done
logcat_dump "$OUT/a3-logcat.txt"
grep -q "LOAD_URL" "$OUT/a3-logcat.txt" && grep -qi "example.com" "$OUT/a3-logcat.txt"
check "A3" "IME Done commits and loads the URL (LOAD_URL in log)" $?

# ... then edit mode exits WITHOUT loading. On a TV the open IME window may
# consume D-pad AND Back itself (founder's R-11 trap). Both exit paths log
# url_edit_exit, so: BACK first (system closes the IME, or the app's
# BackHandler exits editing); if the field is still focused, the IME ate the
# key and D-pad Down now reaches the app's interceptor.
adb logcat -c >/dev/null 2>&1 || true
fresh_launch || true
sleep 2
navigate_to_url_entry
adb shell input keyevent 23 >/dev/null 2>&1 || true   # OK -> edit mode + IME
sleep 2
adb shell input keyevent 4 >/dev/null 2>&1 || true    # BACK: closes IME (system) or exits edit (app)
sleep 2
dumpui a3_down_probe
PD="$(focused_node "$OUT/a3_down_probe.xml")"; PD="${PD##*|}"
if [ "$PD" = "android.widget.EditText" ]; then
  adb shell input keyevent 20 >/dev/null 2>&1 || true # IME ate BACK -> D-pad Down reaches the app interceptor now
  sleep 2
fi
shot a3_down_exit
dumpui a3_down_exit
F="$(focused_node "$OUT/a3_down_exit.xml")"; FC="${F##*|}"
logcat_dump "$OUT/a3-down-logcat.txt"
EXIT_LOGGED=0
grep -q "url_edit_exit" "$OUT/a3-down-logcat.txt" && EXIT_LOGGED=1
[ "$FC" != "android.widget.EditText" ]; check "A3" "edit mode exits without committing (focused class=$FC, exit-logged=$EXIT_LOGGED)" $?
grep -q "LOAD_URL" "$OUT/a3-down-logcat.txt"; [ $? -ne 0 ]; check "A3" "Down-exit does NOT load a page" $?
grep -q "url_edit_exit" "$OUT/a3-down-logcat.txt"; check "A3" "BACK_DISPATCH/exit logged (handledBy=url_edit_exit path)" $?

# ================================================================= A5: adb hooks
# Each hook is delivered via a cold start: `am start` on the live task would
# just bring it to the front without delivering a new intent.
adb shell am force-stop "$PKG" >/dev/null 2>&1 || true; sleep 2
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY" -e w0h.url "https://example.com/" >/dev/null 2>&1 || true
sleep 8
logcat_dump "$OUT/a5-hook-url.txt"
grep -q "NAV_REQUEST" "$OUT/a5-hook-url.txt" && grep -q '"source":"adb"' "$OUT/a5-hook-url.txt"
check "A5" "am start -e w0h.url -> NAV_REQUEST (source=adb)" $?

adb shell am force-stop "$PKG" >/dev/null 2>&1 || true; sleep 2
adb shell am start -n "$ACTIVITY" -e w0h.screen tests >/dev/null 2>&1 || true
sleep 3
dumpui a5_screen_tests
logcat_dump "$OUT/a5-screen-logcat.txt"
grep -q "ADB_SCREEN_OPENED" "$OUT/a5-screen-logcat.txt" && grep -q '"screen":"Tests"' "$OUT/a5-screen-logcat.txt"
check "A5" "am start -e w0h.screen tests -> Tests screen (ADB_SCREEN_OPENED)" $?

adb shell am force-stop "$PKG" >/dev/null 2>&1 || true; sleep 2
adb shell am start -n "$ACTIVITY" -e w0h.action export_deviceinfo >/dev/null 2>&1 || true
sleep 8
logcat_dump "$OUT/a5-hook-export-info.txt"
grep -q "DEVICE_INFO_EXPORTED" "$OUT/a5-hook-export-info.txt"
check "A5" "am start -e w0h.action export_deviceinfo -> DEVICE_INFO_EXPORTED" $?

adb shell am force-stop "$PKG" >/dev/null 2>&1 || true; sleep 2
adb shell am start -n "$ACTIVITY" -e w0h.action export_all >/dev/null 2>&1 || true
sleep 8
logcat_dump "$OUT/a5-hook-export-all.txt"
grep -q "EXPORT_ALL" "$OUT/a5-hook-export-all.txt"
check "A5" "am start -e w0h.action export_all -> EXPORT_ALL" $?

adb shell am force-stop "$PKG" >/dev/null 2>&1 || true; sleep 2
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
grep -q '"variant":"unlocked"' "$OUT/a5-hook-unlocked.txt"
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
PID_GONE=0
PID_AFTER="(polling)"
for _ in $(seq 1 12); do
  sleep 2
  PID_AFTER="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
  if [ -z "$PID_AFTER" ]; then PID_GONE=1; break; fi
done
logcat_dump "$OUT/a7-back.txt"
grep -q "BACK_DISPATCH" "$OUT/a7-back.txt" && grep -q "activity_finish" "$OUT/a7-back.txt"
check "A7" "Back at Home logged (BACK_DISPATCH handledBy=activity_finish)" $?
{ [ "$PID_GONE" = "1" ] || grep -q '"isFinishing":true' "$OUT/a7-back.txt"; }
check "A7" "Back at Home falls through to the system: process exited (pid after poll='$PID_AFTER', gone=$PID_GONE; isFinishing:true accepted)" $?

# ================================================================= evidence bundle: pull session logs
adb pull "/sdcard/Android/data/$PKG/files/w0logs/" "$OUT/w0logs/" >/dev/null 2>&1 || true
adb logcat -d -b crash > "$OUT/crash-buffer.txt" 2>/dev/null || true

note ""
note "Checks passed: $PASS, failed: $FAIL"
note ""
note "Evidence is emulator-only. No W0 test (T-xx/P-x) status is claimed."

[ "$FAIL" = "0" ] && exit 0 || exit 1
