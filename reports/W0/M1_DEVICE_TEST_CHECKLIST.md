# W0 Harness M1 — Founder Device Test Checklist

| Field | Value |
|---|---|
| Prepared by | Lead Orchestrator, 2026-11-28 |
| Harness build | PR #7, commit `d64b371`. CI artifact `w0-harness-d64b371` (expires 2026-12-28) |
| Tester | Founder |
| Evidence type | **Physical device** (not emulator) |
| Scope | Only what M1 supports. P1 full video test, P3, P4, P5, P7 and P8 need M2/M3 |

> This is an interim Lead checklist for M1 only. The full protocol runbook (`reports/W0/TEST_RUNBOOK.md`) will be written by the QA agent.
> **Rule:** write only what you actually saw. If a step could not be done, mark it **BLOCKED** and give the reason. Every number needs a log file or a screenshot.

---

## 0. Setup (once per device)

| # | Step | Done |
|---|---|---|
| 0.1 | On the TV: Settings → System → About → click **Android TV OS build** 7 times to enable Developer options | ☐ |
| 0.2 | Developer options → turn on **USB debugging** / **Network debugging** | ☐ |
| 0.3 | On the computer, install Android platform-tools (`adb`). Then run `adb connect <TV-IP>:5555`, accept the prompt on the TV, and check `adb devices` | ☐ |
| 0.4 | GitHub → Actions → run **46095676799** (or PR #7 → Checks) → download artifact `w0-harness-d64b371` → unzip. It contains 2 APKs (`standard` and `alt`) | ☐ |
| 0.5 | `adb install -r <standard>.apk` and `adb install -r <alt>.apk`. Both appear as "TVBrowse W0 Harness" | ☐ |
| 0.6 | Fill in the device row below | ☐ |

| Device | Model | RAM | Android version | Remote type (BT / IR) | Notes |
|---|---|---|---|---|---|
| D-a | | | | | |
| D-b | | | | | |

**After installing a new CI build:** uninstall first (`adb uninstall dev.tvbrowse.w0harness` and `adb uninstall dev.tvbrowse.w0harness.alt`). The debug signing key changes with every build.

---

## 1. T-01 / T-02 — Device Info (most important; do this first)

| # | Step | Result |
|---|---|---|
| 1.1 | Open the **standard** app → **Device Info** → **Export** | ☐ |
| 1.2 | On the computer:<br>`adb shell getprop ro.config.low_ram`<br>`adb shell getprop ro.product.cpu.abilist`<br>`adb shell getprop ro.build.fingerprint`<br>Save the output in a text file | ☐ |
| 1.3 | Open any web page in the harness. Then run `adb shell ps -A \| grep -i sandboxed` and save the output | ☐ |
| 1.4 | Copy these values from the screen or the JSON: | |
| | `isLowRamDevice` | |
| | `supported64BitAbis` (empty = 32-bit userspace) | |
| | `totalMemMb` | |
| | WebView `packageName` / `versionName` | |
| | **`multiProcessEnabled`** | |
| | Did 1.3 show a `sandboxed_process` line? (yes = separate renderer process) | |

**Read the result like this:**
- `multiProcessEnabled = true` together with a sandboxed process means the device is multi-process. FR-43 can be tested on it in M2.
- `false` means the device is **single-process**. Report it to the Lead immediately; it affects decision F-17 (risk R-02).

---

## 2. T-18 — Cold start

| # | Step | Result |
|---|---|---|
| 2.1 | `adb shell am force-stop dev.tvbrowse.w0harness` | |
| 2.2 | `adb shell am start -W -n dev.tvbrowse.w0harness/.EntryActivity` → write down `TotalTime` | Run 1: ___ ms |
| 2.3 | Repeat 2.1 and 2.2 four more times | 2: ___ 3: ___ 4: ___ 5: ___ |
| 2.4 | In the last run, open Example.com, then **Device Info** → write down `coldStartToFirstFrameMs` and `firstWebViewConstructorMs` | ___ ms / ___ ms |

---

## 3. T-07 — Back key (both flavors)

This matters most on an **Android 16** device. On older Android versions both flavors should behave the same; record the result anyway.

| # | Step | standard | alt |
|---|---|---|---|
| 3.1 | Start the app → open Wikipedia → open any link on it | ☐ | ☐ |
| 3.2 | Press Back once. Did the previous page open? | Y / N | Y / N |
| 3.3 | Press Back again. Did it return to Home? | Y / N | Y / N |
| 3.4 | Open **Device Info** → write down `backKeyEventsSeenInDispatch` and `onBackPressedCallbackInvocations` | ___ / ___ | ___ / ___ |
| 3.5 | Press Back until the app exits. Did it exit cleanly (no crash dialog)? | Y / N | Y / N |

For the alt app use: `adb shell am start -W -n dev.tvbrowse.w0harness.alt/dev.tvbrowse.w0harness.EntryActivity`

---

## 4. Basic browsing + D-pad focus (Compose↔WebView risk R-11)

Use the standard app. Open each bundled Test URL and answer:

| Site | Page loads? | Can you reach links with the D-pad? | Can you get from the page **back to the HUD**? | Menu key toggles the HUD? | Long-press OK toggles the HUD? | Problems (focus stuck, invisible focus, etc.) |
|---|---|---|---|---|---|---|
| Example.com | | | | | | |
| Wikipedia | | | | | | |
| MDN | | | | | | |
| BBC News | | | | | | |
| Local data URL | | | | | | |

| # | Extra check | Result |
|---|---|---|
| 4.1 | In the Wikipedia search box: press OK → does the keyboard open? Type something → Done → do results load? | |
| 4.2 | When the keyboard closes, is focus still usable? | |
| 4.3 | Type a URL on Home (e.g. `example.com`) → Go → does the page open? | |

If something goes wrong, take a screenshot: `adb exec-out screencap -p > shot_<site>.png`

---

## 5. P1 logger — remote sleep / config change (A-01 preliminary)

> The full P1 protocol (fullscreen video, 10 repetitions) needs M2. In M1 we only check **whether the activity is recreated when the remote sleeps or wakes**.

| # | Step | Locked (default) | Unlocked |
|---|---|---|---|
| 5.1 | Settings → Activity variant → select the variant → restart the app | ☐ | ☐ |
| 5.2 | Open a page → Home → **Mark step** "remote idle start" | ☐ | ☐ |
| 5.3 | Leave the remote untouched for **5–10 minutes** until it sleeps. Then press a D-pad key → Mark step "remote woke" | ☐ | ☐ |
| 5.4 | (Optional) Plug in a USB/BT mouse or keyboard → Mark step "mouse in". Unplug it → Mark step "mouse out" | ☐ | ☐ |
| 5.5 | Did the page stay the same (no reload, no jump back to Home)? | Y / N | Y / N |

The Lead will analyse the log: `ACTIVITY_CREATE` with `hasSavedInstanceState=true` means the activity was recreated; `CONFIGURATION_CHANGED` shows what changed.

---

## 6. P2 reference — d3ward (plain WebView baseline)

| Run | Score | Screenshot file |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |

Steps: standard app → Test URLs → **P2 adblock reference (d3ward)** → wait for the score → take a screenshot → Back. Do 3 runs on the same Wi-Fi.
Optional: if TV Bro or BrowseHere is installed on the TV, do the same 3 runs on it and **write down its version**.

---

## 7. Unexpected-exit detector (needed for the M2 crash tests)

| # | Step | Result |
|---|---|---|
| 7.1 | Open a page in the harness | ☐ |
| 7.2 | `adb shell am force-stop dev.tvbrowse.w0harness` | ☐ |
| 7.3 | Restart the app → Logs → open the newest file. Is there a `PREVIOUS_SESSION_DIED` entry? | Y / N |

---

## 8. Not possible in M1 (do not test)

| Item | Reason | Status |
|---|---|---|
| T-17 (WebView missing) | Disabling WebView on your TV is risky for other apps; this is an emulator test | NOT TESTED |
| P1 full (fullscreen video ×10) | Fullscreen container and tabs come in M2 | NOT TESTED |
| P3 / T-04 memory, P4 crash recovery, T-05, T-12 | M2 | NOT TESTED |
| P5, P7, P8, T-06, T-08, T-15 | M3 | NOT TESTED |

---

## 9. Sending the results

1. **Logs** screen → **Export all** (in both apps).
2. `adb pull /sdcard/Android/data/dev.tvbrowse.w0harness/files/w0logs/ ./w0logs-standard/`
3. `adb pull /sdcard/Android/data/dev.tvbrowse.w0harness.alt/files/w0logs/ ./w0logs-alt/`
4. Send the filled-in checklist, the screenshots, the step 1.2/1.3 text files and the two log folders (zip) to the Lead in chat. The Lead will file them in `reports/W0/logs/M1/<device>/`, and the QA agent will verify the evidence and record statuses.
