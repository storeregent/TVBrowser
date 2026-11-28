# TVBrowse W0 Harness (M1)

A **throwaway validation harness** for the TVBrowse W0 protocols. It is **not** the
TVBrowse product, not an MVP, and not the start of W1. Nothing in this folder may
be copied into `app/` without a separate founder decision after gate D-4
(`prototype/HARNESS_BRIEF.md`).

**This is the M1 build.** M1 exists so the founder can start device work early:
basic single-WebView browsing with a D-pad HUD, device diagnostics, session
logging/export, the crash-exit detector and the WebView guard. The tab manager,
renderer-recovery triggers, popup lab, service-worker probe and cursor mode are
**M2/M3** and are shown as disabled placeholders in the UI.

---

## 1. What the founder can test with M1

| Test | What the harness provides |
|---|---|
| T-01 / T-02 | **Device Info** screen: full device, OS, memory, WebView provider (package/version), WebView feature matrix, display, input devices; **Export** writes `device_info_<ts>.json` into the log directory |
| T-07 | Back-key counters in Device Info: `KEYCODE_BACK` events seen in `dispatchKeyEvent` + `OnBackPressedCallback` invocations, per session |
| T-17 | **WebView guard**: if the WebView provider is missing/disabled, a D-pad-navigable error screen appears and `WEBVIEW_UNAVAILABLE` is logged; the app must not crash |
| T-18 | Cold start (Application.onCreate → first Home frame via `reportFullyDrawn`) and the first WebView constructor duration, shown in Device Info and logged |
| Basic browsing | Home → URL entry (or bundled test URLs) → single WebView with HUD (Back / Fwd / Reload / Tabs 1/10 / Menu), HUD hides with the Menu key or long-press OK |
| P1 (logger part) | Every lifecycle callback, key event, configuration-change diff, input/display device events, `onTrimMemory`, **Mark step** markers; the two activity variants (`locked` default, `unlocked` for the A-01 reproduction) are switchable in Settings |
| P2 (reference) | The harness makes **zero network requests on its own**; open the d3ward P2 page from the bundled test list. It is the plain-WebView baseline (test blocklist is an M3 feature and is off) |

---

## 2. Install

### 2.1 Via adb (recommended)

1. On the Google TV device: **Settings → System → About →** click *Android TV OS build* 7× to enable Developer options.
2. **Settings → Device preferences → Developer options →** enable *Network debugging* (or USB debugging) and confirm the on-screen prompt.
3. Find the TV's IP (Settings → Network) and connect from your computer:

   ```bash
   adb connect 192.168.x.x:5555
   adb devices          # must list the TV
   ```

4. Download the two APKs from the GitHub Actions run of this repository
   (workflow **w0-harness**, artifact `w0-harness-<shortSHA>`), then:

   ```bash
   adb install -r harness-standard-debug.apk    # applicationId dev.tvbrowse.w0harness
   adb install -r harness-alt-debug.apk         # applicationId dev.tvbrowse.w0harness.alt
   ```

   Both flavors install side by side as **“TVBrowse W0 Harness”**.

> **Signature note:** CI builds are signed with the runner's default debug key
> **unless** the repository secret `W0H_DEBUG_KEYSTORE_B64` exists (then each
> build reuses that key and `-r` upgrades work). Without the secret every new
> CI build must be installed after uninstalling the previous one:
>
> ```bash
> adb uninstall dev.tvbrowse.w0harness && adb uninstall dev.tvbrowse.w0harness.alt
> ```

### 2.2 Via a sideload app (no computer)

Install any TV-capable sideloader (e.g. *Send Files to TV* on both phone and
TV, or *Downloader* on the TV), copy the APK from the Actions artifact to the TV,
open it with a file manager and confirm the unknown-sources prompt.

### 2.3 Launch

Find **TVBrowse W0 Harness** on the TV launcher (banner 320×180 included). The
launcher activity routes to the activity variant selected in **Settings →
Activity variant** (default: *locked*).

### 2.4 adb automation (M1.1, D-5)

Every M1 test is also drivable over adb via intent extras on the launcher
activity. The only exported component is the launcher; the browser activities
are `exported="false"`, so these hooks work through `am start` (or the app UI).

| Hook | Effect |
|---|---|
| `-e w0h.url <url>` | Open the URL (same path as Go: normalised, logged as `NAV_REQUEST` with `source=adb`) |
| `-e w0h.screen home\|deviceinfo\|logs\|settings\|tests` | Open that screen |
| `-e w0h.action export_deviceinfo\|export_all\|mark_step` | Perform the action, write to the normal `w0logs/` folder, log the output path and show the result on screen. `mark_step` also takes `-e w0h.label <text>` |
| `--ez w0h.locked true\|false` | Pick the Locked or Unlocked activity variant for this launch only (logged) |

Copy-paste commands (package `dev.tvbrowse.w0harness`; add `.alt` for the alt flavor):

```bash
# Open a URL directly
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.url "https://example.com/"

# Open a screen
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.screen deviceinfo
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.screen tests
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.screen logs
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.screen settings

# Exports and marks (results also appear on screen and in the session log)
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.action export_deviceinfo
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.action export_all
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity -e w0h.action mark_step -e w0h.label "P1: remote asleep"

# Pick the activity variant for this launch
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity --ez w0h.locked false   # Unlocked (A-01)
adb shell am start -n dev.tvbrowse.w0harness/.EntryActivity --ez w0h.locked true    # Locked (default)
```

**`tools/run-m1.sh`** runs the full M1 sequence non-interactively where
possible (device props → 5× cold start → Device Info export → open each bundled
test URL → Export all → pull logs):

```bash
tools/run-m1.sh                          # standard flavor
tools/run-m1.sh dev.tvbrowse.w0harness.alt   # alt flavor
```

Outputs land in `./m1-run-<timestamp>/`: `device-properties.txt`,
`cold-start-runs.txt`, and the pulled `w0logs/` JSONL session files. Run
`tools/logcat.sh` in a second terminal to capture the `W0H` logcat mirror.

---

## 3. D-pad controls

| Control | Action |
|---|---|
| D-pad OK | Click focused item; **long-press OK inside a page** toggles the HUD (≈0.6 s) |
| **Menu key** | Toggles the HUD anywhere (some remotes map this to a gear/… key) |
| D-pad Up/Down | Move focus (HUD buttons / page links, per WebView native focus navigation) |
| **Back** | Inside a page: WebView history back. Otherwise returns to the previous screen. On Home: exits cleanly |
| Text field | Press OK to open the system IME (voice/keyboard); IME *Done* acts as “Go” |
| Mark step | Home button; pick a preset label or type a custom one |

Notes:

* Plain text that does **not** parse as a URL is ignored — the harness never
  calls a search engine (zero-initiated-traffic rule). `example.com` becomes
  `https://example.com`.
* Long-press OK on a focused link may still open the page's native long-press
  menu (the press reaches the WebView); the UP event is consumed so no click
  fires. Use the Menu key if that is ambiguous.
* Typing is easiest with a USB/Bluetooth keyboard, or from your computer:
  `adb shell input text 'example.com'`.

---

## 4. Logging and export (brief §6)

* One **JSONL** file per app session in
  `/sdcard/Android/data/dev.tvbrowse.w0harness/files/w0logs/`
  (alt flavor: `dev.tvbrowse.w0harness.alt`), named
  `w0h_<deviceModel>_<yyyyMMdd-HHmmss>.jsonl`.
* Every line has `ts` (ms epoch), `iso`, `session`, `event`, `data`, and is
  mirrored to logcat under tag **`W0H`**.
* The first line is the **SESSION_HEADER** QA packet: device model + fingerprint,
  OS version, RAM class, WebView provider + version, harness version/flavor/
  git SHA/build time/activity variant, and all current Settings values.
* **Unexpected-exit detector (C-06):** a `session_open` marker is written at
  start and cleared on clean exit. If the process dies (e.g. a single-process
  renderer crash), the next launch logs `PREVIOUS_SESSION_DIED` including the
  last 50 log lines of the dead session.
* **Logs** screen: shows the absolute directory path, all files, and **Export
  all** (writes a single zip into the same directory).

Pull the logs:

```bash
adb pull /sdcard/Android/data/dev.tvbrowse.w0harness/files/w0logs/ ./w0logs/
```

Or capture the live logcat:

```bash
tools/logcat.sh
```

### 4.1 `tools/` scripts (bash; macOS, Linux, Git-Bash)

| Script | Purpose |
|---|---|
| `logcat.sh` | `adb logcat -v time W0H:V ActivityManager:I chromium:W *:S` into a timestamped file |
| `pull-logs.sh [appId]` | Pulls the `w0logs/` directory (pass `dev.tvbrowse.w0harness.alt` for the alt flavor) |
| `device-props.sh` | `getprop ro.config.low_ram`, `ro.product.cpu.abilist`, `ro.build.fingerprint`, `ps -A | grep -i sandboxed` (T-01 cross-check) |
| `meminfo.sh <interval_s> <count>` | Samples `dumpsys meminfo` for the **host PID and every renderer PID** with timestamps (corrected P3 method; used from M2 on) |

---

## 5. Flavors

| | `standard` | `alt` |
|---|---|---|
| applicationId | `dev.tvbrowse.w0harness` | `dev.tvbrowse.w0harness.alt` |
| Safe Browsing | Per-WebView `setSafeBrowsingEnabled(false)` default + runtime toggle (Settings) | Manifest `android.webkit.WebView.EnableSafeBrowsing=false`; the toggle is still shown and the log records whether runtime enabling took effect |
| Back handling | `OnBackPressedDispatcher` only | Manifest `android:enableOnBackInvokedCallback="false"` (legacy opt-out) for the T-07 comparison |
| Activities | `BrowserActivityLocked` (default, configChanges = FR-14 list) and `BrowserActivityUnlocked` (no configChanges, A-01 reproduction) — both flavors ship both | same |

---

## 5.1 Back key at Home (M1.1, D-7)

On the Haier device the **standard** flavor did not exit at Home on adb BACK,
while the alt flavor did. Root cause: the D-1 focus trap — the (then-focused)
Compose text field/IME swallowed the key, so the dispatcher never saw it.

M1.1 behaviour (platform default; HARNESS_BRIEF does not override it):

* If the URL edit mode is open: the first BACK closes edit mode (does **not**
  load), the second BACK closes the IME if the system opened it.
* Otherwise at Home with no web history and no open dialog, BACK falls through
  to the system: the activity finishes (clean exit, marker cleared).
* Every Back decision is logged as `BACK_DISPATCH`
  `{screen, canGoBack, handledBy}` with `handledBy` one of
  `url_edit_exit`, `webview_history`, `screen_navigation`, `activity_finish`.
  When the IME consumes a Back to hide itself, no app-side
  `BACK_DISPATCH` line appears for that press (the `KEY_EVENT` line still
  shows the key), which is the observable difference between the flavors.

## 6. Session log events (M1 / M1.1)

`SESSION_HEADER`, `APP_START`, `ENTRY_ROUTE`, `ACTIVITY_CREATE/DESTROY`,
`ACTIVITY_STOP` (log flush), `LIFECYCLE`, `CONFIGURATION_CHANGED` (field diff),
`WINDOW_FOCUS_CHANGED`, `KEY_EVENT` (code/action/repeat/device),
`BACK_CALLBACK_INVOKED`, `BACK_DISPATCH` (D-7 decision log), `HUD_TOGGLED`,
`URL_ENTRY_ACCEPTED/IGNORED`, `TEST_URL_SELECTED`, `LOAD_URL`,
`PAGE_STARTED/PAGE_FINISHED`, `RECEIVED_ERROR`, `WEB_GO_BACK/FORWARD`,
`WEB_RELOAD`, `WEBVIEW_PROVIDER_NULL`, `WEBVIEW_UNAVAILABLE` (T-17),
`WEBVIEW_CREATED` (constructor ms, T-18), `WEBVIEW_DESTROY`,
`RENDER_PROCESS_GONE` (didCrash, rendererPriorityAtExit), `CONSOLE`,
`COLD_START_FIRST_FRAME` (T-18), `INPUT_DEVICE_ADDED/REMOVED/CHANGED`,
`DISPLAY_ADDED/REMOVED/CHANGED`, `TRIM_MEMORY`, `SETTING_CHANGED`,
`SAFE_BROWSING_APPLIED/TOGGLED`, `MARK`, `DEVICE_INFO_EXPORTED`, `EXPORT_ALL`,
`SESSION_END`, `PREVIOUS_SESSION_DIED`, `STORAGE_FALLBACK`,
`ADB_HOOKS` (D-5, parsed extras), `ADB_SCREEN_OPENED`, `NAV_REQUEST`
(`source=adb`), `NAV_REQUEST_IGNORED`, `ADB_ACTION_RESULT`, `APP_STOP`,
`UNCAUGHT_EXCEPTION` (crash handler, with a bounded log flush).

---

## 7. Build info and pinned versions

Builds inject the git short SHA, build time and flavor into `BuildConfig`; every
session header records them.

Pinned in `gradle/libs.versions.toml` (checked against official release pages on
**2026-11-28**):

| Component | Version | Source |
|---|---|---|
| Android Gradle Plugin | **9.5.1** | developer.android.com/build/releases (latest stable; needs Gradle ≥ 9.6.0, JDK 17) |
| Gradle wrapper | **9.6.0** | AGP 9.5 minimum/default |
| Kotlin | **2.4.21** | kotlinlang.org/releases (latest stable; AGP 9 built-in Kotlin, KGP pinned via buildscript classpath) |
| Compose BOM | **2026.06.01** (Compose 1.11.4) | latest stable BOM **compatible with compileSdk 36** — BOM 2026.08.00+ (Compose 1.12) requires compileSdk 37 and 2026.11.01 requires 37.1 per AAR metadata. The brief pins compileSdk 36 (T-07), so the newest satisfying BOM was chosen; flagged to the Lead for confirmation |
| Compose for TV (`androidx.tv:tv-material`) | **1.1.0** | latest stable |
| androidx.webkit | **1.18.0** | latest stable (adds `WebViewFeature.SAVE_STATE`) |
| androidx.activity-compose | **1.12.4** | latest activity line that compiles against compileSdk 36 (1.13.x requires 37) |
| androidx.core-ktx | 1.17.0 | |
| kotlinx-coroutines | 1.11.0, kotlinx-serialization-json | 1.12.0 | latest stable |
| JUnit | 4.13.2 (JVM unit tests only) | |

**Toolchain:** JDK 17 · minSdk 26 · targetSdk/compileSdk **36** · no native code
(no ABI/16 KB-page issues) · debug builds only (CI builds no release).

---

## 8. M1 / M1.1 scope, known gaps and honesty notes

* **M1.1 (this build, v0.1.1-m1.1)** fixes the Haier-device run findings
  (`reports/W0/logs/M1/Haier_MatrixTV_CE/LEAD_ANALYSIS.md`):
  D-1 TV-safe URL entry + focus order (initial focus on the first test URL),
  D-2 dark tv-material theme with WCAG-AA-checked pairs, D-3 visible focus
  (bright container + 2 dp light border + 1.05 scale), D-4 async log writer
  (single thread, timestamps at call time, bounded flush on stop/export/crash),
  D-5 adb hooks + `tools/run-m1.sh`, D-6 empty-Go no-op, D-7 `BACK_DISPATCH`
  logging (see §5.1), H-7 README APK names + in-app multiprocess/renderer-PID.
  H-6 (benchmark build) is **skipped** — founder decision F-H5 pending.
* Emulator evidence for the D-fixes lives in `docs/m1.1-emulator/` (API 34 TV
  image, CI job `emulator-acceptance`). It is **emulator evidence only** — no
  W0 test (T-xx/P-x) is marked PASS by it; device statuses come only from the
  founder's runs.
* **M2/M3 features are absent by design:** 2-live tab pool + standby modes,
  renderer-recovery triggers (`chrome://crash`), session restore, fullscreen
  video container, popup lab, service-worker probe/blocklist, Safe Browsing
  lab, cursor mode. The Tests screen lists them as disabled placeholders.
* `onRenderProcessGone` does **minimal** cleanup in M1 (log, destroy, error
  screen); recreating from stored tab state + crash-loop guard is M2.
* The HUD "Tabs 1/10" is a live-count display for the single M1 tab; the tab
  manager (M2) replaces it.
* All device behaviour above is **NOT TESTED** until the founder runs it on
  real hardware; CI success is not device testing (brief §1.7).
* Zero-initiated-traffic guarantee: the Home screen is a Compose screen (not a
  web page), no analytics/telemetry exists, `WebView.MetricsOptOut` is set, and
  pages load only when the tester opens them.
