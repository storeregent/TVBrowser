# W0 Harness M1 — Founder Device Test Report

**Run date:** 2026-11-28 (Asia/Karachi)  
**Tester:** Codex, working with the Founder  
**Build:** `d64b371`, harness `0.1.0-m1`  
**Device:** Haier MatrixTV CE (`HRT56EU`, `deto`)  
**Outcome:** Partial run. Setup, installation, device identification, cold-start timing, and unexpected-exit logging were verified. App navigation dependent on TV remote focus and the in-app Device Info/Logs pages remains blocked; no result is inferred for those steps.

## Device and build

| Field | Observed value |
|---|---|
| Android | 14 / SDK 34; security patch 2026-02-01 |
| Build fingerprint | `Haier/HRT56EU/deto:14/UKNV.260302.001/14961061:user/release-keys` |
| Low RAM device | `false` |
| Total memory | 1,414 MiB (procfs: 1,448,260 kB) |
| ABI list | `armeabi-v7a,armeabi` (32-bit userspace) |
| WebView | `com.google.android.webview` 157.0.8103.50; version code 810305000 |
| WebView multiprocess | `true` in `dumpsys webviewupdate` |
| Remote type | Not established |
| Renderer process | No `sandboxed_process` observed in captured process list. No page could be loaded, so this does not establish renderer behavior for an active WebView. |
| Installed packages | `dev.tvbrowse.w0harness` and `dev.tvbrowse.w0harness.alt` |

## Results

### 0. Setup

| Step | Result | Evidence / notes |
|---|---|---|
| 0.1 Developer options | PASS (inferred) | Wireless ADB device was connected and authorized. The menu action itself was not observed. |
| 0.2 USB/network debugging | PASS (inferred) | ADB TLS connection was active. Exact Settings toggles were not inspected. |
| 0.3 ADB connection | PASS | Device listed as `adb-30DDDF9A52-GRTBJO._adb-tls-connect._tcp`, state `device`. |
| 0.4 APK acquisition | PASS | Used the two APKs already present in the requested `APKs` folder. App header reports build SHA `d64b371`. |
| 0.5 APK installation | PASS | Both `adb install -r` commands returned `Success`; both package IDs present for user 0. |
| 0.6 Device row | PASS | One device identified; remote type remains unknown. |

### 1. T-01 / T-02 — Device Info

| Check | Result |
|---|---|
| Open Device Info and Export | BLOCKED — the TV UI opened the text-entry keyboard for the focused URL field; D-pad key events did not move focus to the Compose buttons. Device values below come from the harness session header and Android properties, not an in-app export. |
| `ro.config.low_ram` | `false` |
| `ro.product.cpu.abilist` | `armeabi-v7a,armeabi` |
| `ro.build.fingerprint` | `Haier/HRT56EU/deto:14/UKNV.260302.001/14961061:user/release-keys` |
| `isLowRamDevice` | `false` |
| `supported64BitAbis` | Empty; ABI list confirms 32-bit userspace |
| `totalMemMb` | `1414` |
| WebView package/version | `com.google.android.webview` / `157.0.8103.50` |
| `multiProcessEnabled` | `true` (`dumpsys webviewupdate`) |
| `sandboxed_process` line | No line in captured process snapshot; no page was loaded, so renderer test is inconclusive. |

### 2. T-18 — Cold start

| Run | `am start -W` TotalTime |
|---|---:|
| 1 | 7,988 ms |
| 2 | 8,194 ms |
| 3 | 8,222 ms |
| 4 | 8,466 ms |
| 5 | 8,973 ms |
| **Mean / median** | **8,369 ms / 8,222 ms** |

Each timing followed `am force-stop` and a cold `am start -W`. The latest captured harness `COLD_START_FIRST_FRAME` event was 7,399 ms, measured at first Home composition. The requested `coldStartToFirstFrameMs`/`firstWebViewConstructorMs` values from Device Info are **BLOCKED**; no WebView was constructed because a page could not be opened.

### 3. T-07 — Back key

| Check | Standard | Alt |
|---|---|---|
| Open Wikipedia and follow a link | BLOCKED — could not reach site controls | BLOCKED — not attempted |
| Back returns to previous page | BLOCKED — no page history | BLOCKED — no page history |
| Back returns to Home | BLOCKED — no page history | BLOCKED — no page history |
| Device Info Back counters | BLOCKED — screen inaccessible | BLOCKED — screen inaccessible |
| Back exits without crash dialog | BLOCKED / inconclusive; the standard run did not exit Home with the attempted key event | PASS at Home: log records `BACK_CALLBACK_INVOKED`, `ACTIVITY_DESTROY`, and `SESSION_END` reason `last activity finished cleanly` |

The alt clean-exit result is only a Home exit; it does not establish page-history behavior.

### 4. Basic browsing and D-pad focus

| Site / check | Result |
|---|---|
| Example.com | BLOCKED — URL field could not be submitted through current remote key-event path; screenshot shows harness Home. |
| Wikipedia | BLOCKED — same navigation limitation. |
| MDN | BLOCKED — same navigation limitation. |
| BBC News | BLOCKED — same navigation limitation. |
| Local data URL | BLOCKED — bundled test URLs could not be opened. |
| Reach links with D-pad / return to HUD | BLOCKED — no page loaded. |
| Menu key / long-press OK toggles HUD | BLOCKED — actual remote Menu and long-press OK were not available to this ADB session. |
| Wikipedia search keyboard and focus after close | BLOCKED — Wikipedia page not opened. |
| Type URL on Home and Go | BLOCKED — ADB D-pad Enter invoked the TV text keyboard, but URL submission did not navigate. |

### 5. P1 logger — remote sleep / config change

| Check | Locked | Unlocked |
|---|---|---|
| Activity variant setting | Locked is the logged default | BLOCKED — could not open Settings |
| Mark idle/wake steps and wait 5–10 minutes for remote sleep | BLOCKED — requires actual remote interaction and waiting | BLOCKED |
| Mouse in/out | Not run (optional) | Not run (optional) |
| Page remains without reload | BLOCKED — no page could be opened |

### 6. P2 reference — d3ward

**BLOCKED.** The Test URLs action could not be reached through remote navigation; no scores or screenshots were produced. No score was fabricated.

### 7. Unexpected-exit detector

| Check | Result |
|---|---|
| Open page | BLOCKED — page navigation unavailable |
| Force-stop standard app | PASS — performed with `am force-stop` |
| Restart and inspect newest log | PASS — `PREVIOUS_SESSION_DIED` entries are present after force-stop/restart, matching the requested detector check. These entries were induced by the explicit force-stop step and are not evidence of an application crash. |
| Alt clean exit | PASS — alt log records clean activity/session end after Back at Home. |

### 8. Explicitly out of scope for M1

Not tested per checklist: T-17; full P1 fullscreen video ×10; P3/T-04 memory; P4 crash recovery; T-05; T-12; P5; P7; P8; T-06; T-08; T-15.

### 9. Evidence and files

Evidence is stored under [`reports/W0/logs/M1/Haier_MatrixTV_CE`](./logs/M1/Haier_MatrixTV_CE):

- `device-properties.txt`, `webview-package.txt`, `processes.txt`
- `cold-start-runs.txt`, `alt-launch.txt`, `standard-back-launch.txt`
- `screen.png`, `tests-screen.png`, `after-dpad.png`, `device-info-screen.png`, `device-info-result.png`, `focus.png`, `example-load.png`
- `w0logs-standard/`, `w0logs-standard-final/`, `w0logs-alt/` (pulled app JSONL logs)

The Device Info and other UI screenshots show the app Home, because the tested controls were not reachable. Screenshot filenames are retained as capture points; they do not imply that the named flow passed.

### Follow-up needed to complete blocked items

Use the physical remote (or establish a working TV input/remote bridge) to focus the Home controls, open bundled URLs, visit Device Info → Export and Logs → Export all, and perform the remote sleep/wake sequence. Then repeat the blocked navigation, P2, and Back-history rows on both flavors where required.
