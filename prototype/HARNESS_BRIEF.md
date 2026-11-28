# W0 Validation Harness — Assignment Brief

| Field | Value |
|---|---|
| Issued by | Lead Orchestrator |
| Date | 2026-11-27 |
| Assignee | **Android Engineer agent** (separate session and branch) |
| Base | `main` @ `71357ce` (audit PRs #1–#5 merged) |
| Authorisation | Founder decisions F-01, F-03 and F-H1…F-H4: see `reports/project-audit/FOUNDER_DECISIONS.md` |
| Stage | W0 validation. **W1 production development is still BLOCKED.** |

> **What this is:** a **throwaway test app** ("harness"). The founder will install it on his own Google TV device(s) to run the W0 protocols (P1, P3, P4, P5, P7, P8) and the gate-critical experiments from `reports/project-audit/ANDROID_ARCHITECTURE_AUDIT.md` §10.
> **What this is NOT:** the TVBrowse product, an MVP, or the start of W1. No code from the harness may be copied into `app/` without a separate founder decision after gate D-4.

---

## 1. Hard rules (read first)

1. **Write only the files listed in §2.** Do not touch `app/`, `docs/`, `reports/` (except where §2 says so), `AGENTS.md`, `README.md`, the root `.gitignore`, or any other agent's file.
2. **Isolation:** the harness is its own Gradle project under `prototype/w0-harness/`. It is not part of any future production build.
3. **No production features** beyond what a listed test needs. If a feature is not mapped to a test ID in §5, do not build it.
4. **No new third-party SDKs or services.** Allowed: AndroidX (Compose, Compose for TV `androidx.tv:tv-material`, `androidx.webkit`, activity, lifecycle, core), Kotlin stdlib, kotlinx-coroutines, kotlinx-serialization (JSON), JUnit for unit tests. **Not allowed:** Hilt, Room, Media3, OkHttp, Firebase, analytics, crash SDKs, or ad-block libraries. Room/Hilt are approved for the product, not needed here.
5. **Zero app-initiated network traffic.** The harness may only load URLs the tester explicitly opens, plus its own **bundled** local pages. No update checks, favicon fetches, telemetry or remote config. This makes the harness usable for P5.
6. **Never commit** signing keys, keystores, `local.properties`, APK/AAB files or build outputs (AGENTS.md §7).
7. **Evidence honesty:** your sandbox cannot run a real TV device. Mark everything you did not execute as **NOT TESTED**. CI build success is not device testing. Unit tests run on the JVM are labelled "unit (JVM)".
8. Finish with the AGENTS.md §9 completion report and stop. Do not merge your own PR.

---

## 2. File ownership for this assignment

| Path | Owner | Notes |
|---|---|---|
| `prototype/w0-harness/**` | **Android agent** | The whole Gradle project, including its own `.gitignore`, `README.md` and the `tools/` scripts |
| `.github/workflows/w0-harness.yml` | **Android agent** | The only file allowed outside `prototype/` |
| `prototype/HARNESS_BRIEF.md` | Lead | Read-only for the agent |
| `prototype/README.md` | Lead | Read-only |
| `reports/W0/TEST_RUNBOOK.md` | **QA agent (later assignment)** | Protocol-by-protocol steps that use this harness. The Android agent does not write it |
| `reports/W0/logs/**` | Founder / QA | Raw evidence from device runs |

If you believe another file must change, follow AGENTS.md §8: do not change it, explain why, and ask the Lead.

---

## 3. Technical baseline

| Item | Requirement | Why |
|---|---|---|
| Location | `prototype/w0-harness/` (settings.gradle.kts, gradle wrapper, `harness/` module) | Isolation |
| applicationId | `dev.tvbrowse.w0harness` (+ flavor suffixes, §3.1) | Must never collide with a future product ID |
| App label | "TVBrowse W0 Harness" | Distinguishes it on the launcher |
| Language / UI | Kotlin + **Jetpack Compose for TV** | Founder decision F-H2. It also tests the Compose↔WebView focus risk (R-11) |
| WebView hosting | **One** host `FrameLayout` exposed through a single `AndroidView`. WebViews are created, attached, detached and destroyed **imperatively by a `WebViewPool` class**, never inside a composable `factory` keyed per tab | AA §1.2 recommendation |
| Fullscreen video | `onShowCustomView` attaches to a dedicated full-window container **outside** the Compose tree | AA D-gap-09 |
| minSdk | **26** | Same as the TECH_SPEC proposal. Allows later AOSP and Fire OS devices |
| targetSdk / compileSdk | **36** | Needed to observe the Android 16 Back-key behaviour (T-07) |
| ABIs | No native code | Avoids ABI and 16 KB page issues |
| Toolchain | JDK 17; latest **stable** AGP, Kotlin, Compose BOM and androidx.webkit **at the time of work**, pinned in `gradle/libs.versions.toml` and listed in the harness README | Do not guess version numbers. Check the official release pages |
| Manifest | `LEANBACK_LAUNCHER` + 320×180 banner; `android.hardware.touchscreen required=false`; `android.software.leanback required=false`; permissions **only** `INTERNET` and `ACCESS_NETWORK_STATE` | Installs on Google TV; minimal permissions |
| WebView metrics | `<meta-data android:name="android.webkit.WebView.MetricsOptOut" android:value="true"/>` | Keeps P5 captures clean (AA §5.8) |
| Build info | `BuildConfig` fields: git SHA, build time and flavor, injected by CI | Every log must identify the exact build |

### 3.1 Build flavors (one dimension, two flavors)

| Flavor | applicationId suffix | Safe Browsing | Back handling | Used by |
|---|---|---|---|---|
| `standard` | *(none)* | Per-WebView `setSafeBrowsingEnabled(false)` by default, with a runtime toggle in Settings | `OnBackPressedDispatcher` only (no opt-out) | Everything |
| `alt` | `.alt` | Manifest `android.webkit.WebView.EnableSafeBrowsing=false`; runtime toggle still shown, and it logs whether enabling works | Manifest `android:enableOnBackInvokedCallback="false"` (legacy opt-out) | T-15, T-07 comparison only |

Both flavors can be installed side by side. Each test row in §5 says which flavor to use.

### 3.2 Activities (needed for P1)

The `configChanges` attribute cannot change at runtime, so ship two launcher-invisible activities that share the same UI code:

| Activity | `configChanges` | Purpose |
|---|---|---|
| `BrowserActivityLocked` (**default**) | `keyboard|keyboardHidden|navigation|uiMode|screenSize|smallestScreenSize|screenLayout|orientation|density` (FR-14 list) | FR-14-style behaviour |
| `BrowserActivityUnlocked` | none | Reproduces the A-01 failure mode (recreate on BT-remote / mouse config change) |

A launcher `EntryActivity` (or a setting) chooses which one starts. The active choice is written into every session log header.

---

## 4. Screens (all fully usable with a D-pad remote; no touch required)

1. **Home**
   - URL/search field. Plain text is treated as a URL if it parses; otherwise it is ignored (no search engine call, because rule 5 forbids network calls the tester did not request).
   - "Test URLs" list read from a bundled, editable `assets/test-urls.json`.
   - Buttons: **Tests**, **Device Info**, **Logs**, **Settings**, **Mark step**.
2. **Browser**
   - Compose HUD: URL, Back, Forward, Reload, Tabs (`n/10`, live count), Menu.
   - The HUD hides and shows with a dedicated key (Menu key, or long-press OK).
   - D-pad focus inside the page uses WebView's native focus navigation by default.
3. **Tabs**: grid or list of up to **10** tabs. Each shows its live/stored state and whether it is active or standby. Thumbnails are optional; omit them if they cost time.
4. **Device Info** (T-01, T-02): see §5.1. Has an "Export" button.
5. **Tests menu**: one entry per test module in §5.2–§5.9.
6. **Logs**: list of session files, the current file path, and an "Export all" button (§6).
7. **Settings**:
   - Activity variant (§3.2)
   - Standby mode (§5.3)
   - Popup policy (§5.6)
   - Crash-guard window
   - Safe Browsing toggle
   - Desktop/mobile UA toggle
   - Cursor on/off and injection mode (§5.9)
   - Test blocklist on/off (§5.7)

---

## 5. Test modules (each mapped to a protocol or experiment)

Priority: **M1** = first PR (the founder can start testing). **M2** and **M3** follow as later PRs or later commits.

### 5.1 Device Info — T-01, T-02 · **M1** · flavor `standard`

Collect and show, then save as `device_info_<timestamp>.json`:

- `Build.MANUFACTURER/MODEL/DEVICE/FINGERPRINT`, `SDK_INT`, `RELEASE`, `SECURITY_PATCH`
- `SUPPORTED_ABIS`, `SUPPORTED_64_BIT_ABIS`, `SUPPORTED_32_BIT_ABIS`
- `ActivityManager.isLowRamDevice()`, `memoryClass`, `largeMemoryClass`, `MemoryInfo.totalMem`
- WebView provider: `WebViewCompat.getCurrentWebViewPackage()` (package + versionName + versionCode)
- `WebViewFeature.isFeatureSupported(MULTI_PROCESS)` and, if supported, `WebViewCompat.isMultiProcessEnabled()`
- The **feature matrix:** every `WebViewFeature` constant used in this harness, plus: `DOCUMENT_START_SCRIPT`, `SERVICE_WORKER_BASIC_USAGE`, `SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST`, `WEB_MESSAGE_LISTENER`, `SAFE_BROWSING_ENABLE`, `START_SAFE_BROWSING`, `GET_WEB_VIEW_RENDERER`, `WEB_VIEW_RENDERER_TERMINATE`, `WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE`, `MULTI_PROFILE`, `USER_AGENT_METADATA`, `REQUESTED_WITH_HEADER_ALLOW_LIST`, and the saveState-with-size-limit feature if present in the pinned androidx.webkit version
- Display: resolution, density, refresh rate, supported modes, HDR capabilities
- Input devices (`InputManager.getInputDeviceIds()`): name, sources, vendor/product IDs, external or not
- Whether `KEYCODE_BACK` was ever received in `dispatchKeyEvent` during this session. Also count `OnBackPressedCallback` invocations (T-07)
- Cold start: time from `Application.onCreate` to the first Home frame (`reportFullyDrawn()`), and the first `WebView` constructor time (T-18)

### 5.2 Lifecycle and config logger — P1, T-11 · **M1** (logger) / **M2** (fullscreen) · `standard`

Log, with millisecond timestamps:
- `onCreate` (with or without `savedInstanceState`), every lifecycle callback, and `onConfigurationChanged` with a diff of the changed fields
- Activity recreation count
- `InputDeviceListener` added / removed / changed
- Key events (code, action, repeat, source device)
- `onShowCustomView` / `onHideCustomView`
- Display changes (`DisplayListener`)
- `onTrimMemory` levels

The tester presses **Mark step** to insert labelled markers (e.g. "remote asleep", "mouse plugged").

### 5.3 Tab manager (≤2 live, 10 stored) — P3, T-05, FR-11/12-style · **M2** · `standard`

- At most **2 live WebViews** for tabs (active + standby). The popup exception is in §5.6.
- Other tabs are **stored**: URL stack + index + scrollY in JSON. Switching to a stored tab destroys the oldest live non-active WebView using the AA §6.3 sequence (remove from parent → `stopLoading` → `saveState` into memory if needed → `clearHistory` → `destroy`) and then rebuilds from storage.
- **Standby mode** setting, for T-05:
  - (a) blank prewarm WebView
  - (b) MRU live page
  - (c) none

  Log switch latency, from the switch request to the first `postVisualStateCallback` / `onPageFinished`.
- Background (`onStop`): `pauseTimers()` + `CookieManager.flush()`. Foreground: `resumeTimers()`. Never call `pauseTimers` on a single standby, because it is global (C-15).

### 5.4 Renderer crash recovery — P4, T-03, FR-43-style · **M2** · `standard`

- Every WebView comes from one factory, and **every** `WebViewClient` implements `onRenderProcessGone`. That callback must:
  - log `didCrash()` and `rendererPriorityAtExit()`
  - remove and destroy **all** affected instances
  - recreate active and standby from stored tab state
  - return `true`
- Crash-loop guard: max 2 recoveries within a configurable window (default 60 s). On the 3rd crash, show a D-pad-dismissible error page instead of recreating.
- Triggers on the Tests screen:
  1. `loadUrl("chrome://crash")` in the active tab
  2. `WebViewRenderProcess.terminate()` when `WEB_VIEW_RENDERER_TERMINATE` is supported, which exercises the `didCrash()==false` path
- **Unexpected-exit detector:** write a `session_open` marker on start and clear it on clean exit. On the next launch, if the marker is still there, log `PREVIOUS_SESSION_DIED` with the last 50 log lines. This catches the single-process case, where a renderer crash kills the app (C-06).

### 5.5 Session restore after process death — T-12, A-13 · **M2** · `standard`

- Persist tab state (JSON) on every top-level navigation.
- On a cold start with saved state, offer "Restore session".
- After restore, Back/Forward walk the **app-owned** URL stack and log each step, including where it differs from the WebView's own history.
- The tester kills the process with `adb shell am kill dev.tvbrowse.w0harness` while the app is in the background (documented in the README).
- Also log the size of the in-memory `saveState` Bundle and any `TransactionTooLargeException` (A-23 input).

### 5.6 Popup / new-window lab — P8, T-06, FR-45-style · **M3** · `standard`

- `setSupportMultipleWindows(true)`; handle `onCreateWindow`.
- **Popup policy** setting:
  - (1) transient 3rd WebView
  - (2) evict standby to host the popup
  - (3) open as a new tab without `WebViewTransport`

  For every event, log the live-instance count, the URL of the transient WebView's first navigation, gesture age, and the decision.
- Gesture rule: the app tracks its **own** last-OK-press timestamp; the 800 ms rule follows FR-45 (AA §5.4).
- **Bundled local test pages, served from two distinct local origins** with `WebViewAssetLoader` (no network). Pages:
  1. Same-origin `window.open` on a click
  2. Cross-origin `window.open` on a click
  3. Delayed popunder (1200 ms after the click)
  4. `target=_blank`
  5. Redirect chain
  6. **Popunder that also redirects cross-origin** (C-30)
  7. `window.opener` round-trip (payment-style)

### 5.7 Service-worker probe and test blocklist — P7, P2 reference · **M3** · `standard`

- A tiny **test-only** blocklist (≤20 fake/test hostnames in `assets/test-blocklist.txt`) applied in `shouldInterceptRequest`. It is **off by default**, so the harness is the "plain WebView reference" for P2 (d3ward).
- A local SW test page registers a service worker that fetches blocklisted test URLs. Log the blocked count with and without `ServiceWorkerControllerCompat` + `ServiceWorkerClientCompat` interception, guarded by `SERVICE_WORKER_*` features. On unsupported devices: skip, log it, and do not crash.
- This is **not** the FR-17 ad-block engine. Do not import real filter lists.

### 5.8 Network and Safe Browsing — P5, T-15 · **M3** · both flavors

- First run must make **zero** harness-initiated requests: the homepage is local and nothing is fetched.
- The Safe Browsing toggle logs the state before and after, `WebViewCompat.getSafeBrowsingPrivacyPolicyUrl` is **not** called, and in the `alt` flavor the log records whether runtime enabling took effect.
- "Airplane test" button: opens the local Home and test pages and logs that no request left the harness. The `shouldInterceptRequest` counter must show 0 non-local requests.

### 5.9 Cursor mode (basic) — T-08, R-11 · **M3** · `standard`

- A simple overlay cursor: fixed step, accelerates on hold, no physics tuning. It is drawn in a lightweight `View` above the WebView host, not by Compose recomposition.
- **Injection mode** setting:
  - (a) synthetic touch (`ACTION_DOWN/UP`)
  - (b) mouse/hover (`ACTION_HOVER_MOVE` + mouse-source click)
- Log the frame-time distribution while the cursor moves (Choreographer frame callback).
- Toggle cursor ↔ focus mode with a dedicated key. This is the minimum needed to compare injection models; it is **not** the FR-02 implementation.

### 5.10 Robustness — T-17 · **M1** · `standard`

- Wrap WebView construction in a guard. If the provider is missing or disabled, show an error screen and log `WEBVIEW_UNAVAILABLE`; the app must not crash.

---

## 6. Logging and export (all milestones)

- **Format:** one JSONL file per app session in `getExternalFilesDir("w0logs")`, named `w0h_<deviceModel>_<yyyyMMdd-HHmmss>.jsonl`. Every line has `ts` (ms epoch + ISO), `session`, `event` and `data`. Mirror every line to logcat with tag `W0H`.
- **Session header (first line):** the full QA packet (QA §7.1):
  - device model + fingerprint, OS version, RAM class
  - WebView package + version
  - harness version, flavor, git SHA, activity variant
  - all current Settings values
- **Mark step** writes `{"event":"MARK","data":{"label":...}}`. Labels are picked from a preset list (P1/P3 steps) or typed.
- **Export:**
  - The Logs screen shows the absolute path.
  - The README documents `adb pull /sdcard/Android/data/dev.tvbrowse.w0harness/files/w0logs/ <local dir>`.
  - "Export all" also writes a single zip into the same directory.
- **`prototype/w0-harness/tools/` scripts (bash; usable from macOS, Linux or Git-Bash):**
  - `meminfo.sh <interval_s> <count>`: finds the host PID and **renderer PID(s)** (`dumpsys activity processes` / `ps -A | grep sandboxed_process`). Each interval it saves `dumpsys meminfo <hostPID>` + `dumpsys meminfo <rendererPID>` with timestamps. This is the corrected P3 method (AA R-06, C-16).
  - `logcat.sh`: `adb logcat -v time W0H:V ActivityManager:I chromium:W *:S` to a timestamped file.
  - `pull-logs.sh`: pulls `w0logs/`.
  - `device-props.sh`: `getprop ro.config.low_ram`, `ro.product.cpu.abilist`, `ro.build.fingerprint`, and `ps -A | grep -i sandboxed` (T-01 cross-check).

---

## 7. CI — `.github/workflows/w0-harness.yml`

- **Triggers:** `push` and `pull_request` affecting `prototype/w0-harness/**` or the workflow file; also `workflow_dispatch`.
- **Steps:** checkout → `actions/setup-java` (Temurin 17) → `gradle/actions/setup-gradle` → `./gradlew testDebugUnitTest assembleStandardDebug assembleAltDebug` (run in `prototype/w0-harness/`) → upload both APKs as **workflow artifacts** named `w0-harness-<shortSHA>`, kept for 30 days.
- **Signing:** debug builds only.
  - If a repository secret `W0H_DEBUG_KEYSTORE_B64` exists, decode it and use it, so that updates install over each other.
  - If not, use the runner's default debug key and state clearly in the README that **each new CI build must be uninstalled before installing** (signature changes).
  - Never commit a keystore.
- **No release builds, no Play upload, no publishing steps.**

---

## 8. Unit tests (JVM) required

Pure-Kotlin logic must be in classes that can be tested without a device:
- Crash-loop guard: window, count, 3rd-crash behaviour
- Popup decision table: gesture age, same vs cross origin, per policy
- Tab-state JSON round-trip, including the URL stack and index after back/forward
- Live-instance accounting: never more than 2, except for the popup exception under policy (1)
- Log-line serializer: required fields present

---

## 9. Definition of done (per milestone)

| Milestone | Done when | Founder can then run |
|---|---|---|
| **M1** | CI green. Both APKs downloadable from the Actions run. Home + URL entry + single-WebView browsing + HUD + Back handling + Device Info + logging/export + crash-exit detector + WebView guard. README explains install (adb, or a sideload app), D-pad controls, and log export | T-01, T-02, T-07, T-17 (emulator only), T-18; basic browsing on the real device; P2 reference run |
| **M2** | + tab manager, standby modes, renderer recovery + triggers, session restore, lifecycle/config logger with both activities, fullscreen video container, `tools/meminfo.sh` | P1, P3, P4, T-03, T-04, T-05, T-11, T-12 |
| **M3** | + popup lab with local two-origin pages, SW probe, Safe Browsing checks, basic cursor with two injection modes | P5 (harness part), P7, P8, T-06, T-08, T-15 |

**Recommended delivery:** M1 as the first PR so the founder can start device work early; M2 and M3 as follow-up PRs on new assignments. If the agent finishes M1 with time left, it may continue to M2 on the same branch and list exactly what is complete.

---

## 10. Known constraints and inputs

| ID | Item | Status |
|---|---|---|
| H-IN-1 | Founder's exact Google TV model(s), RAM and Android version | **Pending from founder.** Not needed to build; needed to interpret results. Recorded in the device-info JSON automatically |
| H-IN-2 | Optional `W0H_DEBUG_KEYSTORE_B64` secret | Founder may add it later. CI must work without it |
| H-IN-3 | `adb` on the founder's computer + Developer options / network debugging on the TV | Required for P3/T-04 memory scripts and log pull. README must give step-by-step instructions |
| H-C-1 | Google TV devices alone may **not** cover the single-process WebView case (Android 8.1–10, 32-bit, low-RAM) | UNVERIFIED until T-01 runs. AOSP/Fire OS devices may be needed later (PROJECT_AUDIT R-02) |
| H-C-2 | The harness agent's sandbox may lack an Android SDK. **CI is the build of record** | Do not claim local builds you did not run |

---

## 11. Reporting

Open a PR from your branch with:
- What was built, mapped to milestone and test IDs
- CI run link and artifact names
- Pinned versions
- Test status per item: unit (JVM) PASS/FAIL, everything else **NOT TESTED**
- Known gaps
- The AGENTS.md §9 completion report

Then **stop** and wait for the Lead. The QA agent will then write `reports/W0/TEST_RUNBOOK.md` against your README, and the founder will run the device tests.
