#!/usr/bin/env bash
# W0 harness tools (brief §6): T-01 cross-check device properties.
# Usage: ./device-props.sh [serial]
set -euo pipefail

SERIAL_ARG=""
if [ "${1:-}" != "" ]; then
  SERIAL_ARG="-s $1"
fi

echo "== getprop ro.config.low_ram =="
adb $SERIAL_ARG shell getprop ro.config.low_ram
echo "== getprop ro.product.cpu.abilist =="
adb $SERIAL_ARG shell getprop ro.product.cpu.abilist
echo "== getprop ro.build.fingerprint =="
adb $SERIAL_ARG shell getprop ro.build.fingerprint
echo "== renderer processes (ps -A | grep -i sandboxed) =="
adb $SERIAL_ARG shell "ps -A | grep -i sandboxed" || echo "(no sandboxed_process found — single-process WebView?)" 
