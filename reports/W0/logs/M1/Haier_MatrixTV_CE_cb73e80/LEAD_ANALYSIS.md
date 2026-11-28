# M1.1 Device Run — Lead Analysis (Haier MatrixTV CE, build `cb73e80`)

| Field | Value |
|---|---|
| Analysed by | Lead Orchestrator, 2026-11-28 |
| Build | `v0.1.1-m1.1`, commit `cb73e80` (PR #9 head; CI run 46124705442 green, including emulator A1–A8) |
| Sources | (1) `FOUNDER_REPORT.md`: a run by the founder's assistant ("Codex") over **adb key injection**. (2) **Founder manual run with the physical Bluetooth remote**, reported in chat on 2026-11-28: "Mene manual bhi test kiye hain physical remote ke saath, sab theek hai". Scope confirmed by the founder: browsing, link focus, Back to Home, exit from Home, and long-press OK toggling the HUD |
| Evidence filed | Only `FOUNDER_REPORT.md`. The founder chose **not** to upload the evidence folder (JSON exports, JSONL logs, screenshots, `cold-start-runs.txt`). It stays on the founder's machine |
| Previous run | `../Haier_MatrixTV_CE/LEAD_ANALYSIS.md` (build `d64b371`, blocked by D-1/D-2) |

> **Evidence level.** Every status below is a **physical-device result reported by the founder**. None of the raw files are in the repo, so the Lead and the QA agent cannot re-check them. In `validation-results.md`, QA should record these as founder-reported, with raw evidence not filed. They are not fabricated, but they are not independently verifiable either.

## 1. Status by test

| Test | Status | Basis / note |
|---|---|---|
| Setup / install (both flavors) | **PASS** | Old packages had to be uninstalled first, because of a signature change. This is expected without the `W0H_DEBUG_KEYSTORE_B64` secret |
| **T-01** WebView multiprocess | **PASS** | In-app `multiProcessEnabled=true`, and the process `com.google.android.webview:sandboxed_process0` was seen while a page was open |
| **T-02** Device feature matrix | **PASS** | Device Info opened and exported on both flavors: `isLowRamDevice=false`, `supported64BitAbis=[]`, `totalMemMb=1414`, WebView 157.0.8103.50 |
| **T-18** Cold start | **MEASURED, not representative** | `am start -W` median **6,704 ms** (mean 6,762 ms; 5 runs). In-app first frame: 5,518 ms (standard) and 5,871 ms (alt). First WebView constructor: 742 ms and 889 ms. This is a **debug** build. M1 (`d64b371`) had a median of 8,222 ms with the same method (**−18 %**), consistent with the async-logging fix D-4. A comparison with NFR-03 needs the **benchmark build (F-H5 approved)** |
| **T-07** Back key (Android 14) | **PASS** | Both flavors: page → previous page → Home → clean exit (`SESSION_END … finished cleanly`), with counters at 2/2. Confirmed by adb **and** by the founder's physical remote. The **Android 16 predictive-Back behaviour is NOT TESTED**: it needs an API 36 device or emulator (M-15) |
| Basic browsing (Example, Wikipedia, MDN, BBC, data URL) | **PASS** | adb run + founder remote |
| D-pad into web content / Compose↔WebView focus (R-11) | **PASS (exercised paths)** | Focus on Wikipedia links, the TV keyboard for search, and the Menu-key HUD toggle all work. The founder's remote confirms. Not every site was walked link by link |
| Long-press OK toggles HUD | **PASS (physical remote)** | It **failed with adb** `input keyevent --longpress 66`. Injected long-presses do not produce the same key-repeat sequence as a real remote, so this is a **test-method limitation, not a defect**. Note for automation: use the Menu key in scripts |
| URL entry on Home (D-1 fix) | **PASS** | |
| **P1 logger** — variant switch Locked/Unlocked | **PASS** | |
| **P1** — remote idle 5–10 min → wake → page unchanged | **NOT TESTED** | It was not part of the founder's manual run. **The device has a Bluetooth remote, so the A-01 preliminary check *can* be run on this TV** (see §3) |
| **P2** d3ward reference ×3 | **BLOCKED (external)** | All 3 runs showed d3ward's notice that the project is archived and no longer maintained, so **no score** was given. The primary P2 benchmark in `protocols.md` is no longer usable (§2, issue I-2) |
| Unexpected-exit detector | **PASS** | `PREVIOUS_SESSION_DIED` after a deliberate force-stop. This proves the detector works; it is not a crash |
| Out of scope for M1 (T-17 device, P3, P4, T-05, T-12, P5, P7, P8, T-06, T-08, T-15) | NOT TESTED | As directed |

**M1 gate (HARNESS_BRIEF §9, M1 row):**
- T-01, T-02, T-07 (Android 14), T-18 (measured) and basic browsing are done on the real device.
- The P2 reference run was **attempted but blocked** by an external site change.
- **Lead recommendation:** M1 is functionally **ACCEPTED**, with exceptions I-1, I-2 and I-3 below. The founder decides on merging PR #9 and on starting M2.

## 2. Open issues

| ID | Issue | Owner | Proposed action |
|---|---|---|---|
| I-1 | Cold start is measured only on a debug build | Android agent | Build the `benchmark` type (H-6, now approved under F-H5) in the next harness PR. The founder then repeats the 5× cold start on `.bench` |
| I-2 | **d3ward AdBlock Test is archived** and gives no score, so P2's primary benchmark is gone | QA agent → founder | QA proposes a replacement benchmark. It must still be live, give a numeric score, and work in a TV WebView. QA then updates `protocols.md` P2 (QA-owned file). Founder approves the choice. **Until then, P2 = BLOCKED and no ad-block % claims are allowed** |
| I-3 | P1 remote-idle check not yet run | Founder | Run checklist §5 with the Bluetooth remote now (about 15 min, see §3). Full P1 (fullscreen video ×10) still needs M2 |
| I-4 | Raw evidence is not filed | Founder (optional) | Not required now. Before any W0 Go/No-Go decision, the QA agent will ask for at least the Device Info JSON exports and `cold-start-runs.txt` |
| I-5 | Each CI build needs an uninstall first (signature changes) | Founder (optional) | Optionally add the repo secret `W0H_DEBUG_KEYSTORE_B64`, so updates install over the old build and app logs and settings survive. This needs a repo admin; the Lead cannot set secrets |

## 3. P1 quick check the founder can run now (Bluetooth remote, M1.1 build)

1. Open the app (standard), then open a page, e.g. Wikipedia.
2. Press **Mark step** and enter "remote idle start".
3. Put the remote down for **10 minutes** without touching it.
4. Press **one D-pad key** and check whether the page is unchanged: same page, same scroll position, no reload, and the app was not restarted.
5. Press **Mark step** and enter "remote wake".
6. Logs → Export all.

Report for each run: whether the page stayed the same (yes/no), and whether the log shows `CONFIGURATION_CHANGED` or a new `LIFECYCLE` `onCreate` between the two `MARK` lines. Do 3 runs, and if possible repeat with the Unlocked variant (Settings). This result is the **preliminary A-01 signal** only. FR-14's pass criterion (0/10 video drops) needs M2's fullscreen video.

## 4. Implications for the product (record only; no spec change)

- **R-11 (Compose↔WebView focus) is workable.** With a TV-specific text-entry pattern (the D-1 fix), D-pad navigation between Compose controls and web content works on a real 2026 Google TV device.
- **The low-end target is confirmed:** 32-bit userspace and 1.4 GB RAM. Keep the APK free of native code and keep `armeabi-v7a` compatible. T-04/NFR-01 memory baselines on this TV will matter (M2, P3).
- **Ad-block claims:** losing d3ward removes the one benchmark the PRD/W0 plan relied on (A-05/A-26). This must be resolved before any ad-block KPI is set.
