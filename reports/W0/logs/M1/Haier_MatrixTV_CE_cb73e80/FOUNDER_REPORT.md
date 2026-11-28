# W0 Harness M1 — Founder Device Test Report (Build cb73e80)

**Run date:** 2026-11-28 (Asia/Karachi)  
**Tester:** Codex over ADB  
**Build:** `v0.1.1-m1.1`, commit `cb73e80`, activity variant `locked` at final launch  
**Device:** Haier MatrixTV CE (`HRT56EU`, `deto`)  
**Overall result:** Core M1 checks now run on the new build. Device info, both flavors, cold start, browsing, D-pad web focus, Back navigation, URL/search input, logs export, and the unexpected-exit marker were exercised. Remote sleep/wake could not be verified with a physical remote. The P2 site loaded but is archived and returned no score.

## Build under test

The updated APKs came from `APKs/w0-harness-cb73e80.zip`, extracted to `APKs/cb73e80/`. The APKs in `APKs/standard/debug` and `APKs/alt/debug` remain the earlier build.

| Flavor | Package | Install | SHA-256 |
|---|---|---|---|
| Standard | `dev.tvbrowse.w0harness` | PASS | `6636E07867F56BC7AB6697F234A3824E9CB1E9D7499E98E00FEF476B581EA8C7` |
| Alt | `dev.tvbrowse.w0harness.alt` | PASS | `294DFF1676E5226E792B0DCA7E75779FFEADFBC7D68FD5F2D9F03FE8B10B8EC6` |

The first install attempt was rejected because the installed debug signature differed. Per the checklist, both old packages were uninstalled and the new APKs then installed successfully. The app was left open on the standard Home screen, with the activity variant restored to `locked`.

## Device profile

| Field | Observed value |
|---|---|
| Android | 14 / SDK 34; security patch 2026-02-01 |
| Build fingerprint | `Haier/HRT56EU/deto:14/UKNV.260302.001/14961061:user/release-keys` |
| Low RAM device | `false` |
| Total RAM | 1,414 MiB (`/proc/meminfo`: 1,448,260 kB) |
| ABI list | `armeabi-v7a,armeabi`; `supported64BitAbis` empty (32-bit userspace) |
| WebView | `com.google.android.webview` 157.0.8103.50, version code 810305000 |
| WebView multiprocess | `true` |
| Renderer process | `com.google.android.webview:sandboxed_process0:org.chromium.content.app.Sand…` appeared in the process list while a page was open. |
| Remote/input | Android reports `MStar Smart TV IR Receiver` and `HaierTV-RC Consumer Control`; the physical remote protocol was not independently verified. |

The ADB TLS connection was authorized and active. Developer/network debugging is therefore inferred enabled; the TV Settings screens themselves were not inspected.

## Results by checklist section

### 0. Setup

| Step | Result | Notes |
|---|---|---|
| 0.1 Developer options | PASS (inferred) | ADB authorization succeeded. |
| 0.2 USB/network debugging | PASS (inferred) | Active wireless ADB TLS connection. |
| 0.3 ADB connection | PASS | Device online over `_adb-tls-connect._tcp`. |
| 0.4 APKs | PASS | New `cb73e80` APKs installed after uninstalling the old signature. |
| 0.5 Install both flavors | PASS | Both installs returned `Success`; both apps launched. |
| 0.6 Device row | PASS | One Haier MatrixTV CE tested; IR receiver is present, but remote protocol is unconfirmed. |

### 1. T-01 / T-02 — Device Info

**PASS on standard and alt.** Opened Device Info and used Export in both apps. The JSON files are included in the evidence folders.

| Value | Result |
|---|---|
| `isLowRamDevice` | `false` |
| `supported64BitAbis` | `[]` |
| `totalMemMb` | `1414` |
| WebView package/version | `com.google.android.webview` / `157.0.8103.50` |
| `multiProcessEnabled` | `true` |
| `sandboxed_process` during loaded page | Yes, captured in `processes-webpage-active.txt` |
| Standard Back counters | `backKeyEventsSeenInDispatch=2`; `onBackPressedCallbackInvocations=2` in the Device Info export |
| Alt Back counters | `backKeyEventsSeenInDispatch=2`; `onBackPressedCallbackInvocations=2` in the Device Info export |

The Device Info screen/export reports no renderer PID when it is collected from Home with no live page. Separately, the active-page process snapshot shows the sandboxed renderer process. This device is therefore a multiprocess candidate for FR-43 in M2.

### 2. T-18 — Cold start

Five standard cold starts, each after `am force-stop`:

| Run | `am start -W` TotalTime |
|---|---:|
| 1 | 7,088 ms |
| 2 | 6,532 ms |
| 3 | 6,576 ms |
| 4 | 6,704 ms |
| 5 | 6,908 ms |
| **Mean / median** | **6,762 ms / 6,704 ms** |

The exported Device Info reported `coldStartToFirstFrameMs=5518` and `firstWebViewConstructorMs=742` for standard. Alt reported 5,871 ms and 889 ms, respectively. These values are present in the exported JSON.

### 3. T-07 — Back key

| Check | Standard | Alt |
|---|---|---|
| Start app, open Wikipedia, follow a link | PASS — opened Wikipedia portal, focused/opened the French Wikipedia link with D-pad | PASS — same sequence |
| Back once returns to prior page | PASS — returned to Wikipedia portal | PASS — returned to Wikipedia portal |
| Back again returns Home | PASS — returned to harness Home | PASS — returned to harness Home |
| Device Info counters | `2 / 2` | `2 / 2` |
| Back from Home exits cleanly | PASS — `SESSION_END` says `last activity finished cleanly` | PASS — same; launcher became top activity |

### 4. Basic browsing and D-pad focus

| Site / check | Result | Notes |
|---|---|---|
| Example.com | PASS | Page loaded from bundled URL; Home URL entry also opened `https://example.com/`. |
| Wikipedia | PASS | Portal loaded; D-pad focused the Français link, OK opened it, Back navigated correctly. |
| MDN | PASS | `https://developer.mozilla.org/en-US/` loaded. |
| BBC News | PASS | `https://www.bbc.com/news` loaded. |
| Local data URL | PASS | Rendered “Local data URL” and “Rendered offline, no network request.” |
| D-pad reaches web content | PASS | Wikipedia’s Français link visibly received focus and opened. |
| Return to HUD / Menu key | PASS | Menu key hid and restored the HUD on a web page; harness logs include `HUD_TOGGLED` events. |
| Long-press OK toggles HUD | **FAIL in injected-key attempt** | `adb input keyevent --longpress 66` was logged as a long press but did not emit a HUD toggle; the page moved/scroll state changed. Confirm with the physical remote before treating this as a device-remote defect. |
| Wikipedia search keyboard | PASS | Search box opened the TV keyboard; typed query produced a Wikipedia results page. After keyboard dismissal, another D-pad event moved focus to a page control. |
| URL entry on Home | PASS | Entered `https://example.com` and opened Example.com. |
| Other focus-specific site issues | No issue observed in exercised paths | Link focus was specifically verified on Wikipedia; detailed link-by-link navigation was not repeated on every site. |

### 5. P1 logger — remote sleep / config change

| Check | Result |
|---|---|
| Select `locked` / `unlocked` activity variant and restart | PASS — toggled to `unlocked`, relaunch opened `BrowserActivityUnlocked`; toggled back and confirmed final relaunch opened `BrowserActivityLocked`. |
| Open page and mark “remote idle start” | BLOCKED — requires a real remote input path for a valid remote-sleep run. |
| Leave remote idle 5–10 minutes, wake with D-pad, mark step | BLOCKED — no physical remote sleep/wake event was available to observe. ADB key events use virtual device ID `-1`, so they cannot stand in for a physical remote disconnect/reconnect. |
| Optional mouse in/out | NOT RUN |
| Page remains unchanged after remote wake | BLOCKED — no physical wake event to evaluate. |

### 6. P2 reference — d3ward

Opened the bundled P2 URL three times on the same network and saved `p2-run1.png` through `p2-run3.png`. Each attempt showed the site’s notice that the project is no longer maintained and has been archived. **No score was available** in any run, so the score field is N/A rather than guessed.

### 7. Unexpected-exit detector

| Check | Result |
|---|---|
| Open a page | PASS |
| Force-stop standard and restart | PASS |
| `PREVIOUS_SESSION_DIED` after restart | PASS — marker appears after the explicit force-stop. It confirms detector behavior; it is not evidence that the app crashed unexpectedly. |
| Clean Back exit | PASS on both flavors; see T-07. |

### 8. Explicitly out of scope for M1

Not tested, as directed by the checklist: T-17 (disabling WebView); full P1 fullscreen video ×10; P3/T-04 memory; P4 crash recovery; T-05; T-12; P5; P7; P8; T-06; T-08; T-15.

### 9. Evidence and exports

Evidence folder: `reports/W0/logs/M1/Haier_MatrixTV_CE_cb73e80/`

- Device metadata: `device-properties.txt`, `webview-package.txt`, `processes-webpage-active.txt`
- Cold starts: `cold-start-runs.txt`
- Device Info exports: `w0logs-standard-cb73e80-final/device_info_20261128-141718.json` and `w0logs-alt-cb73e80-final/device_info_20261128-143151.json`
- Log exports: `exports/standard-export-all.zip`, `exports/alt-export-all.zip`
- Raw app logs: `w0logs-standard-cb73e80-final/` and `w0logs-alt-cb73e80-final2/`
- Screenshots: Home, Example.com, Wikipedia link and Back sequence, MDN, BBC, local data URL, search keyboard/results/focus, P2 runs, Device Info, Settings variant, and Logs export.

**Remaining physical check:** repeat the P1 remote idle/sleep/wake sequence using the TV’s actual remote. The ADB key injection covered the app/UI paths but cannot prove a remote sleep or wake configuration change.
