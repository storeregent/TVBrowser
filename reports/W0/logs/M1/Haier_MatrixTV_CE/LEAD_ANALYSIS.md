# M1 Device Run — Lead Analysis (Haier MatrixTV CE)

| Field | Value |
|---|---|
| Analysed by | Lead Orchestrator, 2026-11-28 |
| Source | `FOUNDER_REPORT.md` (run with the founder by an external assistant, "Codex", using **adb key injection**) + 6 screenshots in this folder |
| Build | harness `0.1.0-m1`, git `d64b371` (PR #7), **debug** build |
| Evidence type | **Physical device, adb-driven.** The physical remote was **not** used, so remote-specific behaviour is still untested |

## 1. Evidence inventory

| File | Content | Note |
|---|---|---|
| `FOUNDER_REPORT.md` | The run report | Copied verbatim (CRLF normalised) |
| `screen.png`, `after-dpad.png` | Home with the IME open over it | `after-dpad` shows the IME still open after D-pad input |
| `device-info-result.png` | Home with the IME open | Despite its name, it does **not** show Device Info |
| `tests-screen.png` | Home; field unfocused; "Go" appears focused | Does **not** show the Tests screen |
| `example-load.png`, `focus.png` | **Identical files** (same MD5) | Home; Example.com was **not** loaded |
| **Missing** | `w0logs-*` JSONL folders, `device-properties.txt`, `webview-package.txt`, `processes.txt`, `cold-start-runs.txt`, `alt-launch.txt`, `standard-back-launch.txt` | The report cites them, but they were not sent. **Numbers that depend on them stay UNVERIFIED until they are filed here** |

## 2. Device facts recorded (from the report; raw files pending)

| Item | Value | Implication |
|---|---|---|
| Model | Haier MatrixTV CE (`HRT56EU` / `deto`) | First real target device |
| OS | Android 14 / API 34 | **Cannot test the Android 16 Back change (T-07).** That needs an API 36 image (M-15) |
| Userspace | **32-bit** (`armeabi-v7a,armeabi`; no 64-bit ABIs) | Confirms 32-bit TVs are real targets. Keep the app free of native code |
| RAM | 1,414 MiB; `isLowRamDevice=false` | A "1.5 GB" class device. Play TV-ME 280 MB applies only to low-RAM devices, so **not to this one** |
| WebView | `com.google.android.webview` 157.0.8103.50 | Updatable Google WebView |
| Multiprocess | `true` according to `dumpsys webviewupdate` (adb) | Matches the AOSP default for Android 11+. **R-02 is not expected on this device.** The renderer process has not yet been observed because no page loaded. An Android 8.1–10, 32-bit, low-RAM device is still needed to test R-02 (F-05) |

## 3. Results by test (Lead status)

| Test | Lead status | Basis |
|---|---|---|
| Setup / install (0.3–0.5) | **PASS** | adb `Success` for both APKs; the app shows build `d64b371` |
| T-01 multiprocess | **UNVERIFIED (partial)** | adb `dumpsys` says `true`. The in-app value and the renderer process are not captured (no page could load) |
| T-02 feature matrix | **BLOCKED** | Device Info screen unreachable (HARNESS DEFECT D-1) |
| T-18 cold start | **Measured, but not representative for NFR-03** | `am start -W` TotalTime median **8,222 ms** (5 runs; raw file pending). The app reported first Home frame at 7,399 ms. This is a **debug** build (debuggable, no R8, no baseline profile) going through an EntryActivity trampoline, and logs are written synchronously on the main thread. A release-like build is needed before this can be compared with the <2 s target (fix H-6) |
| T-07 Back key | **BLOCKED** (page history) / alt Home exit **PASS** | No page could be opened. Android 14 cannot show the Android 16 behaviour anyway |
| Basic browsing, D-pad, HUD (R-11) | **FAIL (harness) / BLOCKED (R-11 question)** | A harness defect blocks navigation (D-1). This is itself an R-11-class finding: a Compose text field traps D-pad focus on TV |
| P1 logger | **BLOCKED** | Settings unreachable; physical remote not used |
| P2 d3ward reference | **BLOCKED** | Test URLs unreachable |
| Exit detector | **PASS** | `PREVIOUS_SESSION_DIED` logged after force-stop (induced; not an app crash) |
| Out of scope for M1 | NOT TESTED | As per the checklist |

## 4. Harness defects found (root cause from code review of `d64b371`)

| ID | Severity | Defect | Root cause | Evidence |
|---|---|---|---|---|
| **D-1** | **Blocker** | D-pad cannot leave the URL field, so Device Info, Logs, Settings, Tests and Test URLs are all unreachable | `HomeScreen` uses the **mobile** `androidx.compose.material3.OutlinedTextField`, and it receives initial focus. A focused Compose text field handles Up/Down itself (cursor movement in a single-line field), and OK opens the IME. There is no `onPreviewKeyEvent` to move focus out and no `FocusRequester` on a non-text element | Screenshots: field focused + IME open after D-pad input; once, "Go" received focus via Right, but Down never reached the button row |
| **D-2** | **Blocker (usability)** | Title, URL label and Test-URL titles are near-black on a near-black background, so they are unreadable at 10 ft | `HarnessTheme` calls TV `MaterialTheme {}` **without `darkColorScheme()`**, so the default light scheme's dark `onSurface` text is drawn on the custom dark background. The mobile `OutlinedTextField` gets no material3 theme at all | All screenshots |
| **D-3** | High | The focused element is hard to see (e.g. "Go" turns dark on dark) | Same theme issue; no explicit focus indicator | `tests-screen.png` |
| **D-4** | Medium | Every log line is written synchronously on the main thread (`File.appendText` per event, including every key event) | `SessionLog.log` | Code. Adds startup and input latency and distorts T-18 |
| **D-5** | Medium | Testing is impossible without a working remote path; adb automation has no hook | No intent/extra commands | Run report |
| **D-6** | Low | An empty Go shows "Ignored: not a URL" | `openNormalized` treats empty input as invalid | Screenshots |

## 5. What this run shows for the project (not only the harness)

1. **R-11 is real, and it shows up at the very first screen.** The standard Compose text field is D-pad-hostile on TV. The product (FR-07 HUD / FR-08 URL entry) needs an explicit TV text-entry pattern: a focusable "URL" element that enters edit mode only on OK, with Up/Down always moving focus out. This is input for the v1.3 design notes. **No spec change is made here.**
2. **Low-end target profile:** a 2026 Android 14 TV with 32-bit userspace and 1.4 GB RAM that is not flagged low-RAM. Memory baselines (T-04) on this device will be meaningful for NFR-01.
3. **Cold start needs a release-like measurement.** A debug Compose build at about 8 s says nothing yet about NFR-03.

## 6. Next steps

| # | Action | Owner |
|---|---|---|
| 1 | Send the missing raw files (§1) so they can be filed here | Founder |
| 2 | Fix D-1…D-6 and the H-items in `prototype/HARNESS_M1_1_FIX_BRIEF.md` (M1.1) before M2 | Android agent |
| 3 | Re-run the M1 checklist on this TV **with the physical remote** (and the adb hooks for export), then record the remote model | Founder |
| 4 | QA agent: verify the evidence and record statuses in `validation-results.md` once the re-run is complete | QA agent |
