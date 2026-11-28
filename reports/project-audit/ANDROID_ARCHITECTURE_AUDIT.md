# ANDROID ARCHITECTURE AUDIT — TVBrowse (W0)

| | |
|---|---|
| **Role** | Android TV Technical Architect (audit only) |
| **Date** | 2026-11-27 |
| **Branch / base** | `arena/88e7e745-tvbrowser` from `main@1660e6a` |
| **Inputs reviewed** | `AGENTS.md`, `README.md`, `docs/technical/TECH_SPEC_v1.2.md`, `docs/product/PRD_v1.2.md`, `docs/validation/W0_Validation_Plan_v1.2.md`, `docs/validation/protocols.md`, `docs/validation/validation-results.md`, `docs/research/procurement-and-sources.md`, `docs/history/CHANGELOG_v1.2.md`, `reference/browsehere/*.md`, repo tree (`app/`, `prototype/`, `tests/`, `reports/`) |
| **Not reviewed (out of scope)** | `docs/research/demand-and-competitor.md` (market/competitor; not my role) |
| **Files modified** | **Only this file.** No source, spec or validation document was changed. |

### Evidence labels used in this report

| Label | Meaning |
|---|---|
| **[DOC]** | Static reading of the repo documents (exact section cited) |
| **[SRC-VERIFIED]** | Checked today against an official primary source (Android Developers docs or AOSP source); reference IDs `V1…V10` in §13 |
| **[ENG-JUDGMENT / UNVERIFIED]** | Engineering expectation from platform knowledge. **Not measured, not device-tested.** Must be confirmed in W0 |

**Test status for everything in this report: NOT TESTED.** No emulator or physical-device work was done. No memory, performance or compatibility figures below are measurements.

---

## 0. Executive summary

1. **There is no existing Android implementation to assess.** The repository has documents only. `app/`, `prototype/` and `tests/` contain a 1-byte `.gitkeep`, and there is no Gradle project, manifest or Kotlin source [DOC]. This report therefore assesses the **architecture as specified** in TECH_SPEC v1.2 §1–§5.
2. **The approved direction is technically feasible overall.** That direction is Kotlin, Compose for TV, a single Activity, System WebView behind `BrowserEngine`, at most 2 live WebViews, disk-based tab state and renderer recovery. Nothing in it needs a non-standard platform capability.
3. **Seven findings need Lead or founder attention before W1.** Four were confirmed against primary sources today:
   - **F-1 (critical, SRC-VERIFIED).** On **Android 8.1, 9 and 10, a 32-bit device that reports low-RAM runs WebView in single-process mode by default**. A renderer crash then kills the app, so FR-43, NFR-06 and P4 ("0 app exits on `chrome://crash`") **cannot pass on such devices by design**. Android 8.0 and Android 11+ default to multiprocess [V1][V2][V3][V8].
   - **F-2 (high, SRC-VERIFIED).** FR-31's primary download path (user-initiated data transfer job) **requires `RUN_USER_INITIATED_JOBS`**. That permission **is not in the TECH_SPEC §4 allow-list**, so NFR-10 CI would block it. No Jetpack library supports UIDT, and a UIDT job is stopped if the process is killed for low memory [V5].
   - **F-3 (high, SRC-VERIFIED).** With **targetSdk 36 on Android 16+ devices, `KEYCODE_BACK` is no longer dispatched and `onBackPressed` is not called** unless the app migrates or opts out. FR-05/FR-06 key handling depends on this [V4].
   - **F-4 (high, SRC-VERIFIED).** `onCreateWindow` requires the app to **create a new WebView instance**, and it does not give the target URL or a trustworthy opener [V10]. FR-45 therefore conflicts with the FR-11 "hard ceiling 2 live" whenever active and standby both exist.
   - **F-5 (medium, SRC-VERIFIED).** FR-12 lists `onPause()+pauseTimers()`, but **`pauseTimers()` is global to all WebViews** and **`onPause()` does not pause JavaScript** [V9]. A live standby WebView will keep running page JS.
   - **F-6 (medium, SRC-VERIFIED).** Protocol P3 measures memory with `dumpsys meminfo <pkg>`. Per official docs that **reports only the host process, not the isolated renderer** [V8], so NFR-01 measurements would under-count.
   - **F-7 (medium, DOC).** **Roadmap dependency inversion:** FR-43 (W1–2) must restore tabs "from `TabState`", but `TabState`/Room/nav-history (FR-13, FR-29) and the 2-live policy (FR-11) are scheduled for W5–7.
4. **The W0 plan covers lifecycle and policy well but misses several technical spikes:** multiprocess detection, the WebView feature matrix, back-key behaviour under targetSdk 36, cursor event-injection strategy, the 3rd-WebView problem in popup handling, download edge cases, and WebView-update kills. See §10.

---

## 1. Existing architecture assessment

### 1.1 Repository state [DOC]

| Path | Content | Assessment |
|---|---|---|
| `app/` | `.gitkeep` (1 byte) | No Android project exists. Nothing to inspect. |
| `prototype/` | `README.md` (1 byte, empty) | No validation prototype yet. |
| `tests/` | `.gitkeep` | No tests. |
| `reports/W0/` | `.gitkeep` | No W0 results yet. |
| `.gitignore` | empty (1 byte) | **Risk:** no ignore rules for `build/`, `.gradle/`, `local.properties`, `*.jks`/`*.keystore`, `*.apk`/`*.aab`. AGENTS.md §7 forbids committing signing keys and APKs. A rule set is needed **before** any prototype work starts. (I have not modified it; this is outside my file scope.) |
| `README.md` | empty | No build/run instructions yet. |
| `docs/validation/validation-results.md` | Template; every A-row ⏳ | No W0 evidence exists yet. All device-dependent assumptions remain open. |

**Conclusion:** the only architecture available is the documented one. Every claim about the implementation below is about **the specification**, not about code.

### 1.2 Specified architecture (TECH_SPEC §3) — assessment

```
UI (Compose for TV, Single Activity) → InputController → Domain
  TabManager(Room: TabState + nav-history) · AdBlockEngine · ServiceWorkerGuard
  RendererRecovery (per WebView instance) · MediaSniffer(P1)
→ BrowserEngine (interface) → SystemWebViewEngine [P2: GeckoViewEngine]
→ Room+FTS4 · DataStore · OkHttp · Media3 · ACRA(nogms)
```

| Aspect | Assessment | Label |
|---|---|---|
| Single Activity | Correct for TV. It avoids focus and back-stack problems across Activities and simplifies `configChanges` locking (FR-14). | ENG-JUDGMENT |
| Compose for TV + `AndroidView(WebView)` | Feasible, but **WebView should not be owned by composition**. If a Composable's `factory` creates the WebView keyed per tab, recomposition and key changes will detach, recreate or leak WebViews and can exit fullscreen. The spec should require **one host `FrameLayout` exposed through `AndroidView`, with WebViews swapped imperatively by the engine layer**. This matches `Better_Android_TV_Browser.md` Q10 ("Single FrameLayout container"). | ENG-JUDGMENT |
| `BrowserEngine` interface (FR-01, D-02) | Right decision, and the FR-01 acceptance ("no `android.webkit` import in UI") is good. It is **not enforceable without module boundaries or a lint rule** (§2). The interface contract (session-oriented vs View-oriented) is undefined; see §9 D-gap-01. | DOC + ENG-JUDGMENT |
| MVVM/MVI + Hilt + Coroutines/Flow | Standard and low-risk. Hilt adds small startup cost; NFR-03 (<2 s) needs a baseline measurement (§10 T-18). | ENG-JUDGMENT |
| Room (FTS4) + DataStore | Appropriate. FTS4 under 100 ms for about 1,000 history rows is realistic but unmeasured. | ENG-JUDGMENT |
| OkHttp (downloads) | Feasible. **It is a second network stack** with no shared cookies, HTTP cache, HSTS, client certificates or proxy behaviour with Chromium. Downloads must bridge cookies explicitly (§5.6). | ENG-JUDGMENT |
| Media3 bundled in both flavors (D-10 revised) | Simpler than DFM, so fine. Memory co-residence with a live WebView during native playback is unspecified (§6.6). | DOC + ENG-JUDGMENT |
| `gms` / `nogms` flavors from day 1 (D-08) | Correct. `nogms` here means the build has no GMS dependency. It does **not** mean the device lacks Google WebView; AOSP boxes may still run Google's WebView provider (relevant to Safe Browsing, §5.8). | ENG-JUDGMENT |
| AOSP Browser code not copied | Correct. BrowseHere = AOSP 4.x `Controller`/`TabControl` fork with minSdk 14 / target 23 (`Better_Android_TV_Browser.md` intro; RE doc). | DOC |

---

## 2. Recommended module boundaries

> **Scope note:** this is **not a new architecture**. It maps the layers already approved in TECH_SPEC §3 onto Gradle modules so that FR-01 and D-02 can be enforced at build time. Adopting a multi-module layout is an architecture-level decision and needs **founder approval** (§12 Q-09).

| Module (proposed) | Contains (from TECH_SPEC) | May depend on | Must NOT depend on |
|---|---|---|---|
| `:app` | Single Activity, Hilt graph, flavor wiring, manifest (NFR-10 allow-list), fullscreen host container | all feature modules | — |
| `:core:model` | `TabState`, nav-history entry, site profile, permission decision (pure Kotlin) | — | Android framework |
| `:core:engine-api` | `BrowserEngine`, engine session/events, engine capability flags (FR-01) | `:core:model` | `android.webkit`, `androidx.webkit` |
| `:engine:systemwebview` | `SystemWebViewEngine`, **RendererRecovery hooks (FR-43)**, `WebViewFeature` guards (NFR-12), `ServiceWorkerGuard` (FR-44), custom-view bridge (FR-22), download-listener adapter (FR-31), UA/viewport application (FR-33) | `:core:engine-api`, `:core:adblock`, `:core:policy` | UI modules |
| `:core:tabs` (domain) | `TabManager`, live-instance policy (≤2, FR-11), eviction and `onTrimMemory` (FR-12), crash-loop guard state machine (FR-43), restore orchestration (FR-13) | `:core:engine-api`, `:core:data` | `android.webkit` |
| `:core:input` | `InputController`, key normalization (FR-05), back-priority dispatcher (FR-06), cursor physics (FR-02), auto-hide (FR-04), edge-scroll policy (FR-46) | `:core:model` | `android.webkit` |
| `:core:adblock` | hosts parser, trie, per-site whitelist, counters (FR-17, NFR-04) — pure JVM, microbenchmarkable | — | Android UI |
| `:core:policy` | FR-45 popup decision table, FR-56 Google-OAuth URL classifier, intent sanitizer (§5), per-site permission decisions (FR-48) — pure Kotlin, table-driven tests | `:core:model` | `android.webkit` |
| `:core:data` | Room (bookmarks, history+FTS4, downloads, `TabState`, nav-history), DataStore, thumbnail file store (FR-29) | `:core:model` | UI |
| `:feature:browser` | Compose HUD (FR-07), URL entry (FR-08), tab grid (FR-15), TV dialogs (JS alert/confirm, permissions) | `:core:*` (not engine impl) | `android.webkit` |
| `:feature:home` | Speed-dial and bundled favicons (FR-37) | `:core:data` | — |
| `:feature:player` | Media3 screen, subtitles, "Open with" (FR-23/24/26) | `:core:model` | `android.webkit` |
| `:feature:downloads` | UIDT / WorkManager / FGS fallback, OkHttp, in-app progress (FR-31) | `:core:data` | `android.webkit` |
| `:feature:settings` | Clear data (FR-49), history UI (FR-50), site settings (FR-33) | `:core:*` | — |
| flavor source sets (`gms`/`nogms`) | Safe Browsing toggle availability (§5), ACRA (`nogms` only, NFR-05) | — | — |

**Enforcement (recommended):** a Gradle dependency rule plus a lint/static-analysis rule: "`android.webkit.*` / `androidx.webkit.*` imports allowed only in `:engine:systemwebview`". This turns FR-01's acceptance into a CI check instead of a code-review convention.

**Benefits for testing:** `:core:input` (physics with a virtual clock), `:core:adblock` (p95 lookup), `:core:policy` (FR-45/FR-56 tables) and `:core:tabs` (fake engine, FR-01) can all be unit-tested on the JVM without devices.

---

## 3. Technical feasibility (per area)

Complexity is relative (**L / M / H / VH**). These are judgments, not time estimates.

| Area | FRs | Feasible? | Complexity | Main uncertainty | Label |
|---|---|---|---|---|---|
| Shell + engine abstraction | FR-01, FR-07 | Yes | M | Interface shape (§9) | ENG-JUDGMENT |
| Virtual cursor at 60 fps | FR-02, FR-04 | Yes, with caveats | H | Event-injection strategy (touch vs mouse/hover), renderer hit-test cost per frame on weak SoCs | UNVERIFIED |
| Spatial / focus mode (JS) | FR-03 | Partially | H | Cross-origin iframes, shadow DOM, page JS interference; old WebViews lack `DOCUMENT_START_SCRIPT` | UNVERIFIED |
| Key normalization / back priority | FR-05, FR-06 | Yes | M | **Android 16 + targetSdk 36 back-key change [V4]** | SRC-VERIFIED risk |
| Edge auto-scroll | FR-46 | Yes | M | Nested scrollers (SPAs) need a synthetic `ACTION_SCROLL` at the cursor, not only `WebView.scrollBy` | UNVERIFIED |
| Tab model ≤2 live | FR-11, FR-12, FR-15 | Yes | H | Standby semantics (§6.2), global `pauseTimers` [V9], FR-45 3rd instance [V10] | SRC-VERIFIED risk |
| Disk tab state + nav-history | FR-13, FR-29 | Yes, "approximate" | VH | WebView back/forward list cannot be rebuilt from a URL stack, so app-owned history must drive navigation after process death (§6.4) | ENG-JUDGMENT |
| Config-change lock | FR-14 | Yes | L–M | Coverage of HDMI/display-mode/CEC changes not in the list (§7) | UNVERIFIED |
| Renderer crash recovery | FR-43 | **Yes on multiprocess devices only** | H | **Single-process devices (API 27–29, 32-bit, low-RAM) [V2]** | SRC-VERIFIED risk |
| Hosts ad-block | FR-17, NFR-04 | Yes | M | ≤20k-entry trie memory delta; interception gaps (WebSocket, redirects, SW on old WebViews) | UNVERIFIED |
| Service-worker interception | FR-44 | Yes (guarded) | L–M | Feature availability per device | UNVERIFIED |
| Popup / new-window ownership | FR-45 | Yes, with design change | H | Needs a transient WebView; target URL not provided; no trustworthy opener [V10] | SRC-VERIFIED risk |
| Fullscreen HTML5 video | FR-22 | Yes | M | Custom-view host placement relative to Compose; Back routing during fullscreen | ENG-JUDGMENT |
| Native player (Media3) | FR-23, FR-24, FR-26 | Yes | M | Memory with WebView alive (NFR-01 ≤400 MB HLS) | UNVERIFIED |
| Downloads | FR-31 | Yes, with gaps | H | Permission gap [V5], blob:/data:/POST downloads, API 26–28 storage, LMK during download | SRC-VERIFIED + UNVERIFIED |
| Per-site rendering / UA | FR-33, FR-56 | Yes | M | UA-string vs UA-Client-Hints consistency; desktop UA inherently lacks the `; wv` token (§5.9) | UNVERIFIED |
| Permissions prompts | FR-48 | Yes | M | Per-site persistence needs Room (W5–7) but is scheduled for W1–2 | DOC |
| Local homepage | FR-37 | Yes | L–M | Compose vs `WebViewAssetLoader` choice affects cold start (§9) | ENG-JUDGMENT |
| Security baseline | §5 | Yes | M | JS-bridge rule vs features that need scripts in arbitrary origins (§11 C-08) | DOC |

**Overall:** feasible. The highest technical risks are concentrated in **FR-13, FR-43, FR-45, FR-02/03 and FR-31**.

---

## 4. Critical technical risks

| ID | Risk | Severity | Evidence | Impact | Mitigation direction (for Lead/founder; no spec change made) |
|---|---|---|---|---|---|
| **R-01** | **Single-process WebView on Android 8.1/9/10 32-bit low-RAM devices.** AOSP `SystemImpl.isMultiProcessDefaultEnabled()` returns `SUPPORTED_64_BIT_ABIS.length > 0 \|\| !isLowRamDeviceStatic()` on `oreo-mr1`, `pie` and `android10` [V2]. It returns `true` unconditionally on `oreo` (8.0) [V1] and on Android 11–14 [V3]. Official docs: "on low-RAM devices, it might fall back to a single process" [V8]. | **Critical** | SRC-VERIFIED (AOSP source); per-device status UNVERIFIED | A renderer crash in single-process mode takes the app process down, so FR-43/NFR-06/P4 "0 app exits" cannot pass. Likely affected (**UNVERIFIED**): cheap Android 9/10 AOSP boxes and Fire OS 7 devices (Android 9 base) if their userspace is 32-bit and flagged low-RAM. OEMs and Amazon may also change the default. | W0 test T-01 (`WebViewCompat.isMultiProcessEnabled()`, `ro.config.low_ram`, ABI list) on every matrix device. Founder decision on acceptance scope for such devices (Q-01). |
| **R-02** | **`RUN_USER_INITIATED_JOBS` missing from §4 allow-list** while FR-31 makes UIDT primary. UIDT also requires a notification, must be scheduled while the app is visible, and **is stopped when the process is killed for low memory** [V5]. No Jetpack library supports UIDT; the official fallback is the WorkManager foreground-service worker [V5]. | **High** | SRC-VERIFIED | NFR-10 CI blocks the build, or the spec is silently violated. On every device below API 34 (all Fire OS 7/8, most AOSP boxes), the **fallback path is the only path**: an FGS (dataSync on API 34+ with the 6 h cap). Downloads share the browser's process and die with it under LMK. | Spec change via Lead (Q-02). W0 T-14. |
| **R-03** | **Back key under targetSdk 36.** On Android 16+ devices, "`onBackPressed` is not called and `KeyEvent.KEYCODE_BACK` is not dispatched anymore" unless the app migrates or sets `android:enableOnBackInvokedCallback="false"` [V4]. | **High** | SRC-VERIFIED (Android TV 16 device availability UNVERIFIED) | FR-06 back priority (fullscreen → popup → `goBack` → tab close → exit) and any long-press or raw key handling of Back may stop working on Android 16 TV devices. | Decision Q-05 (migrate to `OnBackPressedDispatcher`/`OnBackInvokedCallback` vs temporary opt-out). W0 T-07 on an API 36 TV emulator. |
| **R-04** | **FR-45 vs FR-11.** `onCreateWindow` only works if the app "create[s] a new WebView to host the window" and passes it via `WebViewTransport`. It provides no target URL, and "there is no trustworthy way to tell which page requested the new window" [V10]. | **High** | SRC-VERIFIED | With active + standby live, honouring any popup means a 3rd instance, which conflicts with the "hard ceiling 2". FR-45's same-origin vs cross-origin decision cannot be made inside `onCreateWindow`; it needs the first navigation of the transient WebView. Opening the URL in a fresh tab (without transport) breaks `window.opener` flows (payments, some logins). | Decision Q-03. W0 T-06. |
| **R-05** | **Standby WebView keeps running JS.** `pauseTimers()` "pauses all layout, parsing, and JavaScript timers for **all WebViews** … global"; `onPause()` "does not pause JavaScript" [V9]. | Medium | SRC-VERIFIED | FR-12's "`onPause()`+`pauseTimers()`" cannot be applied to the standby alone. A live standby page (ads, trackers, possibly audio) consumes CPU, memory and network in the background, which works against NFR-01/NFR-06. | Define standby semantics (Q-04). Use `pauseTimers` only on app background. Consider renderer priority policy [V6]. W0 T-05. |
| **R-06** | **Memory measurement under-counts the renderer.** "Running `dumpsys meminfo` with your app's package name only outputs memory for the main host process"; the renderer must be measured by PID. In-app memory APIs can't see the renderer [V8]. | Medium | SRC-VERIFIED | P3/NFR-01 numbers would be wrong if the protocol is followed literally. P3 says "ALL PIDs incl. `:webview`/renderer", but the command shown does not capture them. | Fix the measurement method in W0 (T-04). Lead to route the protocol update to its owner. |
| **R-07** | **NFR-01 ≤250 MB (10 tabs) feasibility is unknown.** The renderer process is shared, so destroying WebViews frees DOM memory, but "physical memory … doesn't immediately drop" after `destroy()` [V8]. The filter-list set and Compose/Media3 also count. | Medium–High | Mechanism SRC-VERIFIED; numbers UNVERIFIED | The FR-12 / W5–7 gate may fail regardless of implementation quality, depending on the page mix. | Early baseline (T-04) before W1, so the target can be confirmed or renegotiated with data. |
| **R-08** | **App-owned nav-history after process death.** WebView's back/forward list can only be restored from a `restoreState(Bundle)`, and TECH_SPEC forbids persisting the Bundle to disk (FR-13). After process death, the app must replay its own URL stack, so the WebView history and the app history diverge (`loadUrl(previous)` adds new entries; POST results and SPA `pushState` entries can't be replayed faithfully). | High (complexity) | ENG-JUDGMENT | FR-13 acceptance "back/forward wapas" needs a precise definition (semantic restore, not byte-identical). It is the most complex P0 item. | Define acceptance semantics (§9 D-gap-05). W0 T-12. |
| **R-09** | **Roadmap dependency inversion** (FR-43, FR-08, FR-48 in W1–2 need Room/`TabState`, scheduled W5–7). | Medium | DOC (PRD §8; TECH_SPEC FR-08/29/43/48) | W1–2 acceptance for FR-43 ("dono tabs wapas from `TabState`") can't be met as written. | Lead decision: pull a minimal data skeleton into W1–2, or scope the W1–2 acceptance (Q-10). |
| **R-10** | **WebView provider update or absence during a session.** When the WebView provider package is updated, dependent app processes may be killed. Cheap boxes may have a disabled or broken provider, which makes `WebView(...)` throw at construction. | Medium | UNVERIFIED (engineering knowledge; AOSP shows `killPackageDependents` when the multiprocess setting changes [V2-file]) | Adds an "app exit" source that NFR-06 doesn't model. Missing-provider crashes at startup violate NFR-12. | W0 T-17. Guarded engine creation with an error screen. |
| **R-11** | **Cursor injection model.** Synthetic touch events vs mouse/hover events produce different page behaviour: `:hover` menus, pointer type, click synthesis, scrollbars. Hover dispatch every frame triggers renderer hit-testing. | Medium | UNVERIFIED | FR-02 (≥55 fps, no frame >32 ms) and site compatibility depend on this choice. | W0 T-08 (decide before W3). |
| **R-12** | **Downloads gaps:** `DownloadListener` gives URL/UA/content-disposition/MIME/length but **no cookies, Referer or POST body**. `blob:` and `data:` downloads can't be fetched by OkHttp. On API 26–28, `MediaStore.Downloads` (API 29+) is unavailable and `WRITE_EXTERNAL_STORAGE` is banned by §4. | Medium | ENG-JUDGMENT / UNVERIFIED | Some downloads fail. On Android 8/9 devices (incl. Fire OS 7), files land in app-specific storage (removed on uninstall, poorly discoverable). | Decisions Q-08. W0 T-14. |

---

## 5. WebView limitations (relevant to the approved scope)

### 5.1 Process model and renderer
- All WebViews in an app share **one renderer process** (TECH_SPEC FR-43 already states this). One crash affects **every** instance, including any transient popup WebView (§5.4) and the fullscreen custom-view state [DOC + ENG-JUDGMENT].
- Multiprocess is **not guaranteed** (R-01) [V1][V2][V3][V8].
- If `onRenderProcessGone` is missing from **any** `WebViewClient` (popup WebView, help/licence screen), the app crashes. Every WebView must come from the engine factory [ENG-JUDGMENT].
- Re-loading the URL that crashed the renderer can crash again ("attempting to load that same page again can cause a new WebView object to exhibit the same rendering crash behavior") [V6]. FR-43's guard (max 2) is necessary. The **time window** and **scope** (per tab or per session) are undefined (§9).
- `didCrash()==false` means the system killed the renderer for memory [V6]. Immediately recreating **both** active and standby (as FR-43 requires) under the same memory pressure may re-trigger the kill [ENG-JUDGMENT]. Flagged for W0 P4 step 5, not changed.
- Renderer **hang** (unresponsive, not crashed) is not covered by FR-43. `WebViewRenderProcessClient` (API 29+, androidx feature-gated) exists for this; not specified (§9).
- `setRendererPriorityPolicy(…, waivedWhenNotVisible)` exists from API 26 [V6]. With a shared renderer, its effect on a hidden standby is unclear [UNVERIFIED].

### 5.2 Lifecycle APIs
- `pauseTimers()` is global; `onPause()` does not stop JS [V9] (R-05).
- `saveState()` / `restoreState()` no longer store display data [V9]. The `savedInstanceState` budget is **1 MB shared across the whole process** [V7].
- **New since TECH_SPEC v1.2 was written:** `WebViewCompat.saveState(webView, bundle, maxSizeBytes, includeForwardState)` drops older entries until the payload fits, without touching live history [V7]. FR-13 currently says "> 256 KB skip"; truncation is a possible alternative to skipping. This is for Lead awareness only; I am not changing the spec. It needs a feature check and an androidx.webkit version check.

### 5.3 Request interception (ad-block)
- `shouldInterceptRequest` runs off the UI thread and must not block [ENG-JUDGMENT]. Request initiator/first-party is not reliably exposed (`Better_Android_TV_Browser.md` Q12) [DOC].
- Coverage gaps to confirm: service-worker requests (FR-44, feature-gated), WebSocket, redirect hops, `blob:`/`data:` [UNVERIFIED].

### 5.4 New windows
- A WebView instance must be created per honoured popup. There is no URL and no trustworthy opener [V10] (R-04).
- `setSupportMultipleWindows(true)` is a prerequisite for `onCreateWindow`. Without it, `target=_blank` loads in place. That choice is not stated in FR-45 [ENG-JUDGMENT].
- Chromium's own user-activation window is longer than FR-45's 800 ms rule, so the app must track its **own** last-OK-press timestamp. The `isUserGesture` flag alone is insufficient for the 800 ms rule [UNVERIFIED; design note].

### 5.5 Script injection
- `evaluateJavascript` runs in the page's main world, so page scripts can observe or tamper with injected code. WebView exposes no isolated worlds [ENG-JUDGMENT].
- `evaluateJavascript` reaches the main frame only. Injecting into iframes needs `addDocumentStartJavaScript` (feature-gated, absent on older providers) [UNVERIFIED per device].

### 5.6 Downloads
- See R-12. In addition, OkHttp is a separate stack (§1.2): cookies must be copied from `CookieManager`, and HSTS, client certificates and proxy behaviour differ [ENG-JUDGMENT].

### 5.7 Fullscreen video
- `onShowCustomView` hands over a View that must be attached to a full-window container. `onHideCustomView` must remove it and `CustomViewCallback.onCustomViewHidden()` must be called on Back [V10 page; ENG-JUDGMENT].
- If the custom view is attached inside the Compose tree, recomposition can detach it. Attaching it to a dedicated container outside composition is safer [ENG-JUDGMENT].
- The video tag is excluded from `postVisualStateCallback` guarantees. Thumbnails of video pages will usually be black (relevant to FR-15 thumbnails) [V9 page; ENG-JUDGMENT].

### 5.8 Safe Browsing and WebView metrics (privacy defaults)
- "The default value of `EnableSafeBrowsing` is **true**." Global opt-out uses manifest `<meta-data android:name="android.webkit.WebView.EnableSafeBrowsing" android:value="false"/>`; per-WebView toggling is available on API 26+ [V6]. D-13's "default OFF" therefore **requires explicit implementation**. Whether runtime opt-in still works when the manifest flag is `false` is **UNVERIFIED** (T-15).
- WebView can upload diagnostic data when the user has consented at OS level, unless the app sets `android.webkit.WebView.MetricsOptOut=true` [V6]. TECH_SPEC §5 ("zero telemetry") doesn't mention this. It needs a decision (Q-07).

### 5.9 User-agent
- A default desktop Chrome UA string (FR-33) **by construction lacks the `; wv` token**. FR-33b ("remove `; wv`") was deleted for policy reasons, but the default desktop UA reaches the same token state. The **FR-56 URL guard is therefore the only control** preventing an effective OAuth bypass, and it must cover all Google sign-in entry points (popups, iframes, redirects) [DOC + ENG-JUDGMENT]. Flagged as C-09; policy interpretation belongs to the Lead/founder.
- UA Client Hints (`Sec-CH-UA*`) may not match an overridden UA string, which can produce inconsistent layouts. androidx.webkit exposes UA metadata APIs on newer providers [UNVERIFIED] (T-16).

---

## 6. Memory and lifecycle concerns

### 6.1 What counts toward NFR-01
- NFR-01 = "app + WebView renderer + GPU + filter-list set" [DOC]. WebView has no separate GPU process; GPU work is attributed to the **app** process. The renderer is an **isolated `SandboxedProcessService`** under the WebView provider package [V8; GPU attribution ENG-JUDGMENT, confirm in T-04].
- Official guidance recommends **RSS / Anon RSS / PMF**, and **browser PMF vs renderer PMF** [V8]. NFR-01 specifies **PSS**. The metric definition should be fixed before baselining (§9 D-gap-08).

### 6.2 Two-live design (FR-11)
- "1 active + 1 optional prewarmed standby" has two possible meanings: (a) a blank pre-initialised WebView for fast new tabs, or (b) the most-recently-used tab kept alive for fast switch-back. A-16 explores latency vs LMK but does **not** define which meaning is meant [DOC].
- (a) After Chromium is loaded, creating an extra blank WebView is expected to be relatively cheap, so the benefit of (a) may be small. (b) avoids reloads but keeps a live page running JS (R-05) [UNVERIFIED].
- Prewarming before an Activity exists requires a `MutableContextWrapper`-style approach. Some WebView features (select popups, dialogs, autofill) need an Activity context [ENG-JUDGMENT].

### 6.3 Eviction
- `destroy()` sequence per official guidance: remove from parent, `stopLoading()`, `clearHistory()`, `destroy()`, null the reference [V8]. Note that `clearHistory()` must come **after** `saveState` if the Bundle is kept for in-memory restore [ENG-JUDGMENT].
- Memory may not drop immediately after `destroy()` [V8]. Eviction triggered by `onTrimMemory` will show delayed effect in measurements.
- Leak detection: watch the `WebViews:` / `Activities:` counts in `dumpsys meminfo -a` across tab switches [V8]. Recommended as a CI/W0 check.

### 6.4 Process death and restore (FR-13)
- Bundle in memory: helps only for in-process eviction, not process death [DOC + V7].
- After process death, restore = `TabState` row + app nav-history, followed by `loadUrl(current)` and app-driven back/forward. Form state, POST results, iframe scroll and SPA internal state are lost; FR-13 already accepts "approximate" scroll [DOC]. **Back/forward after restore has different semantics from live WebView history** (R-08).
- Activity `onSaveInstanceState` must stay minimal. The 1 MB process-wide limit includes Compose `rememberSaveable` [V7].

### 6.5 Background and foreground
- Home/background: `pauseTimers()` and `CookieManager.flush()` on `onPause`/`onStop`, then `resumeTimers()` on return [ENG-JUDGMENT; `flush` per `Better_Android_TV_Browser.md` Q19].
- Downloads and the browser share one process. LMK of the host kills a UIDT job [V5] or a WorkManager FGS. Process isolation for downloads would be an architecture change and is only asked (Q-11).

### 6.6 Media
- With the native Media3 player active, the WebView (renderer) can stay alive behind it. Whether to pause or destroy the WebView during native playback is unspecified, and it affects NFR-01 ≤400 MB HLS [UNVERIFIED] (T-22).
- `FLAG_KEEP_SCREEN_ON` only while fullscreen or playing (FR-22) [DOC].

---

## 7. Compatibility risks (Android TV / Google TV, AOSP boxes, Fire OS)

| Platform | Risk | Label |
|---|---|---|
| **All, targetSdk 36** | Back key not dispatched on Android 16+ without migration or opt-out (R-03) [V4]. Edge-to-edge opt-out disabled on Android 16 for targetSdk 36 [V4]; on TV this matters only for overscan-safe layout. Low impact, but verify on an API 36 TV image. | SRC-VERIFIED |
| **Android 8.1/9/10, 32-bit, low-RAM** (cheap AOSP boxes; possibly Fire OS 7 devices) | Single-process WebView → FR-43 impossible (R-01) [V2]. | SRC-VERIFIED mechanism; device list UNVERIFIED |
| **Android 8.0 and 11+** | Multiprocess on by default per AOSP [V1][V3]; OEM changes possible. | SRC-VERIFIED (AOSP only) |
| **API 26–28** | No `MediaStore.Downloads`; `WRITE_EXTERNAL_STORAGE` banned by §4, so downloads go to app-specific storage (R-12). | ENG-JUDGMENT |
| **API 26–33** | No UIDT, so the WorkManager FGS fallback is the only download path [V5]. Covers all Fire OS 7/8 and most AOSP boxes. | SRC-VERIFIED |
| **AOSP boxes (no Play)** | Old, non-updatable WebView (A-12). Missing androidx.webkit features (`DOCUMENT_START_SCRIPT`, `SERVICE_WORKER_*`, `MULTI_PROFILE`, `WEB_MESSAGE_LISTENER`, `SAFE_BROWSING_*`). Missing or broken provider → construction failure. Modern sites may break on old Chromium. Non-standard key codes from cheap remotes and air-mice. | UNVERIFIED |
| **Fire OS 7/8** | Amazon-supplied WebView with update cadence and feature set outside our control. No GMS (Safe Browsing, voice P1 hidden). FR-31 fallback path only. Fire OS 7 = Android 9 base (engineering knowledge; verify), so R-01 applies if 32-bit + low-RAM. Vega devices excluded (procurement doc) [DOC]. | UNVERIFIED |
| **Google TV** | 2 GB baseline device is the critical memory target (W0 plan §5) [DOC]. WebView auto-updates via Play, so a provider update can kill a running session (R-10). | UNVERIFIED |
| **Config changes** | FR-14 list (`keyboard|keyboardHidden|navigation|uiMode|screenSize|smallestScreenSize|screenLayout|orientation|density`, per the complaints doc) doesn't cover all TV-specific triggers: HDMI resolution/mode switch, `fontScale`, `locale`, `layoutDirection`, `colorMode`. Display refresh-rate switching during video may or may not cause config changes. P1 tests only BT sleep and mouse connect. | UNVERIFIED |
| **Input** | TV IMEs (Gboard for TV, Fire keyboard) capture D-pad while open; routing on dismiss is unspecified. TalkBack takes over D-pad, which conflicts with cursor mode (NFR-09). | UNVERIFIED |
| **ABIs** | Many TV devices run 32-bit userspace. No native libraries are planned in P0 (adblock-rust is P2), so an all-Java/Kotlin APK avoids ABI and 16 KB page-size issues. Keep it that way in v1. | ENG-JUDGMENT |
| **Manifest / Play** | §4 declares P1 permissions (`FOREGROUND_SERVICE_MEDIA_PLAYBACK` for FR-16) in the canonical list. If declared in the v1 manifest without v1 use, Play may require an FGS declaration for an unused type. NFR-10 is an allow-list, but whether "allowed" means "must declare" is ambiguous (C-05). | DOC + UNVERIFIED |

---

## 8. Technical dependencies

| Dependency | Status in spec | Notes / risk |
|---|---|---|
| Kotlin, Coroutines/Flow | Approved | — |
| Jetpack Compose + Compose for TV (`androidx.tv` material) | Approved | Pin versions in W0. Use standard Compose foundation lazy layouts where TV-specific ones are deprecated [UNVERIFIED; check current release notes]. |
| `androidx.webkit` | Approved | **Version floor determines available features** (e.g. `WebViewCompat.saveState(…maxSizeBytes…)` [V7], `isMultiProcessEnabled`, renderer client). All usage behind `WebViewFeature` (NFR-12). |
| Hilt (KSP) | Approved | Startup cost minor; measure under NFR-03. |
| Room (FTS4), DataStore | Approved | Migrations policy needed from first schema. |
| OkHttp | Approved | Second network stack (§1.2). |
| Media3 (ExoPlayer, HLS, UI) | Approved, bundled | APK ≤20 MB target (NFR-08) plausible but unmeasured. |
| WorkManager | Implied by FR-31 | **Needed** for the below-API-34 fallback [V5]; not listed in the §3 stack line. |
| JobScheduler UIDT (platform) | FR-31 | Needs `RUN_USER_INITIATED_JOBS` (R-02). |
| ACRA (`nogms`, self-hosted) | Approved | Needs a self-hosted endpoint, which conflicts with "no backend" (D-04) unless opt-in endpoint config is user-supplied. Clarify (A-19 exists). |
| `WebViewAssetLoader` | Approved | Only if the homepage is web-based (FR-37 choice). |
| Build tooling (AGP/Gradle/JDK versions, baseline profiles, lint rules) | **Not specified** | Decide at W1 start. |

No new framework, paid API or SDK is recommended in this report.

---

## 9. Missing technical decisions

| ID | Decision needed | Why it matters | Related |
|---|---|---|---|
| D-gap-01 | **`BrowserEngine` contract shape**: View-oriented (the engine returns an Android `View`) vs session-oriented (engine session + separate view host); and which WebView-specific callbacks (custom view, file chooser, permission request, renderer gone) are abstracted vs passed through. | FR-01/D-02 enforceability; P2 Gecko path should not force a rewrite. Avoid over-designing for P2. | FR-01, D-02 |
| D-gap-02 | **Standby definition** (blank prewarm vs MRU live page) and **its JS policy** (R-05). | Memory, CPU, background audio, switch latency. | FR-11/12, A-16 |
| D-gap-03 | **Popup instance policy** under the 2-live ceiling (R-04): transient 3rd instance allowed? evict standby? open in new tab without transport? | FR-45 cannot be implemented as written without one of these. | FR-11, FR-45 |
| D-gap-04 | **Crash-loop guard parameters**: window (time), scope (per tab / per session), the culprit tab's behaviour (auto-reload vs "tap to reload"), and recreate both vs active-first under `didCrash()==false`. | FR-43 determinism; avoid crash loops under memory pressure. | FR-43, P4 |
| D-gap-05 | **FR-13 "back/forward restored" semantics after process death** (app-driven replay; POST/SPA entries excluded?). | Acceptance testability. | FR-13, A-13 |
| D-gap-06 | **Back handling under targetSdk 36** (migrate vs opt-out) and how FR-06 priority maps to `OnBackPressedDispatcher` / `OnBackInvokedCallback`. | R-03. | FR-05/06, NFR-07 |
| D-gap-07 | **Cursor event-injection model** (touch vs mouse/hover; click synthesis; scroll via `ACTION_SCROLL`), and the **cursor rendering surface** (dedicated lightweight View or Compose draw-phase only; avoid per-frame recomposition). | FR-02 fps target and site compatibility. | FR-02/04/46 |
| D-gap-08 | **Memory metric definition** (PSS vs PMF/RSS-anon [V8]), **how the renderer is attributed**, and **which device each NFR-01 figure applies to**. | Pass/fail of the W5–7 gate. | NFR-01, P3 |
| D-gap-09 | **Fullscreen custom-view host** location (outside the Compose tree) and fullscreen behaviour on renderer death. | FR-22/14 stability. | FR-22, FR-14 |
| D-gap-10 | **Homepage technology** (Compose speed-dial vs `WebViewAssetLoader`), which determines whether a WebView/renderer is created at cold start. | NFR-03 cold start, NFR-11 first-run calls, memory at idle. | FR-37 |
| D-gap-11 | **Download destination per API level** (26–28 app-specific vs 29+ `MediaStore.Downloads`), handling of `blob:`/`data:`/POST, cookie/Referer bridging, notification-denied behaviour. | R-12. | FR-31 |
| D-gap-12 | **Renderer hang policy** (`WebViewRenderProcessClient`, API 29+, feature-gated). | ANR-like UX on heavy pages. | FR-43 (adjacent) |
| D-gap-13 | **Safe Browsing implementation mechanism** for "default OFF" (manifest flag vs per-instance) and its effect on `gms` opt-in [V6]. | D-13 correctness; NFR-11. | §5, D-13 |
| D-gap-14 | **WebView `MetricsOptOut`** [V6]. | Zero-telemetry promise (PRD §4 principle 3). | §5 |
| D-gap-15 | **JS-bridge rule vs features needing page→app messages** in arbitrary origins (spatial-nav focus events, FR-45 gesture timing, P1 media keys). `evaluateJavascript` return values need no bridge; event streams do. | TECH_SPEC §5 says "JS bridge sirf apne origin + `WebMessageListener` allow-list". | §5, FR-03, FR-45 |
| D-gap-16 | **Basic browser dialogs not enumerated in FRs:** JS `alert/confirm/prompt/beforeunload`, HTTP auth, client-cert, `<select>` popups on TV, SSL interstitial UI. Not new scope; these are engineering defaults that must be D-pad-navigable (NFR-09). | Without TV-styled handling, defaults may be unusable with D-pad. | NFR-09, §5 |
| D-gap-17 | **Thumbnail capture method** (PixelCopy vs `draw()`; video areas black) and bitmap memory policy. | FR-15 grid and memory. | FR-13/15 |
| D-gap-18 | **TalkBack mode**: auto-switch to focus mode when accessibility services are on. | NFR-09. | FR-02/03 |
| D-gap-19 | **Repository hygiene before prototype**: `.gitignore` rules, signing-key handling, module/package naming, build-tool versions. | AGENTS.md §7. | — |

---

## 10. Recommended W0 technical experiments (missing from W0 plan / protocols)

Existing protocols P1–P10 cover BT-sleep, d3ward, 30-min LMK, 2-WebView crash, first-run capture, form-time, SW interception, popups, D-pad walkthrough and FGS video. The experiments below are **additional technical test cases** I recommend. They would be run in the isolated `prototype/` area only if the Lead assigns that work (AGENTS.md §4). **Status of all: NOT TESTED.**

**Priority: G = gate-critical (before W1) · P = before the owning phase.**

| ID | Experiment | Pri | Method (summary) | Pass / output | Closes |
|---|---|---|---|---|---|
| **T-01** | **WebView multiprocess status per device** | **G** | On each matrix device + emulator API 26/28/29/30/34/36 TV: log `WebViewCompat.isMultiProcessEnabled()` (feature-gated), `getprop ro.config.low_ram`, `Build.SUPPORTED_64_BIT_ABIS`, presence of `sandboxed_process` in `ps -A`. | Table per device. Any single-process device → founder decision Q-01. | R-01 |
| **T-02** | **WebView provider + androidx.webkit feature matrix** | **G** | Log `WebViewCompat.getCurrentWebViewPackage()` version and `isFeatureSupported` for every feature used by P0 (document-start script, SW intercept, web-message listener, multi-process, safe-browsing enable/start, renderer get/terminate/client, `saveState` with limits, UA metadata, requested-with header control, multi-profile). | Matrix in `validation-results` (Lead to route). Defines graceful-degradation rules. | A-12 (extends), NFR-12 |
| **T-03** | **Renderer-kill trigger viability** | **G** | Check that `loadUrl("chrome://crash")` actually crashes the renderer on each provider in a release-like build. Alternatives: `WebViewRenderProcess.terminate()` (API 29+/compat) for the `didCrash()==false` path; system memory pressure. | P4 has a working trigger on every device, or an alternative is documented. | P4 validity |
| **T-04** | **Memory baseline with correct method** | **G** | Host `dumpsys meminfo <pkg>` + renderer `dumpsys meminfo <RENDERER_PID>` (PID from `dumpsys activity processes <pkg>`) [V8]. States: empty Compose shell; +1 blank WebView; +heavy page; +2nd WebView; after `destroy()`; +20k-entry trie; +Media3 HLS. 2 GB and 1 GB devices. Record PSS **and** PMF/RSS-anon. | Real baseline for NFR-01/FR-12; confirms or challenges the ≤250 MB target before W1. | R-06, R-07 |
| **T-05** | **Standby semantics A/B** | **G** | (a) blank prewarm vs (b) MRU live page. Measure switch latency, memory delta, background CPU (JS keeps running [V9]), background audio, effect of `setRendererPriorityPolicy(BOUND, true)` [V6]. | Data for D-gap-02 / A-16. | R-05, A-16 |
| **T-06** | **`onCreateWindow` with 2 live WebViews** | **G** | Test pages: `window.open` same/cross-origin, `target=_blank`, delayed popunder, `window.opener` round-trip (payment-style). Count live instances, capture the target URL from the transient WebView's first navigation, measure memory. | Evidence for D-gap-03. P8 checks policy outcomes only, not instance count. | R-04 |
| **T-07** | **Back key under targetSdk 36** | **G** | API 36 TV emulator (and an Android 16 TV device if one exists): with and without `enableOnBackInvokedCallback="false"`. Check Back during fullscreen custom view, HUD, long-press, `goBack`, exit. | Evidence for D-gap-06. | R-03 |
| **T-08** | **Cursor injection + frame pacing** | P (W3) — recommend W0 | Touch vs mouse/hover synthetic events on 10 sites (hover menus, small `[X]`, `<select>`, cross-origin iframe buttons, video controls, canvas). Perfetto on the 2 GB device while moving with hover dispatch every frame. | Choice for D-gap-07; ≥55 fps feasibility signal (FR-02/NFR-02). | R-11 |
| **T-09** | **Edge scroll in nested scrollers** | P (W3) | `WebView.scrollBy` vs synthetic `ACTION_SCROLL` at cursor position on SPA layouts. | Method for FR-46. | FR-46 |
| **T-10** | **Spatial-nav JS prototype** | P (W3) | Script via `evaluateJavascript` / document-start script; measure latency and coverage on 10 sites incl. iframes and shadow DOM; old-provider fallback. | Coverage %; FR-03 feasibility. | FR-03 |
| **T-11** | **Fullscreen host + extended config changes** | P (W5) — extend P1 | Custom view attached outside Compose; trigger HDMI resolution switch, display-mode/refresh change, CEC/AVR power, locale/font scale; renderer crash while fullscreen. | FR-14/22 stability evidence beyond BT sleep. | FR-14/22 |
| **T-12** | **Process-death restore** | **G** (A-13 related) | `adb shell am kill <pkg>` while backgrounded; restore active tab from a minimal Room nav-history; test back/forward after restore incl. SPA `pushState`, POST result page, hash navigation. | Defines FR-13 acceptance semantics (D-gap-05). | R-08, A-13 |
| **T-13** | **`saveState` size behaviour** | P (W5) — extend A-23 | Heavy sessions; compare `saveState` Bundle size vs `WebViewCompat.saveState(…maxSizeBytes, includeForwardState)` [V7] where supported. | Input for FR-13 cap policy. | A-23 |
| **T-14** | **Download edge cases + background execution** | P (W13) — recommend W0 for the permission question | `blob:`, `data:`, POST, `Content-Disposition`, cookie-gated and Referer-gated URLs. UIDT on API 34+ with `RUN_USER_INITIATED_JOBS`; WorkManager FGS on API 26–33; LMK mid-download; notification permission denied; API 26–28 destination. | Evidence for R-02, R-12, D-gap-11. | FR-31 |
| **T-15** | **Safe Browsing default-OFF mechanism** | **G** (P5-adjacent) | Manifest `EnableSafeBrowsing=false` vs per-instance `setSafeBrowsingEnabled(false)`; then runtime opt-in. Capture network with P5 setup. | Mechanism for D-13 / D-gap-13. | A-18 |
| **T-16** | **UA string vs UA-CH consistency + FR-56 guard coverage** | P (W1–2) | Desktop UA on 5 layout sites; inspect `Sec-CH-UA-Mobile/Platform`. Exercise Google sign-in entry points (redirect, popup, iframe) and confirm the guard fires on each. | FR-33/FR-56 evidence. | C-09 |
| **T-17** | **WebView provider update / missing provider** | **G** | Emulator: update the WebView provider during an active session (`adb install -r` of a provider build); disable the provider and launch. | Is the app killed? Does construction throw? Graceful screen needed (NFR-12). | R-10 |
| **T-18** | **Cold-start baseline** | **G** | Empty Compose+Hilt shell `reportFullyDrawn()`; first `WebView` construction time; native homepage vs asset-loader homepage on 1 GB/2 GB devices. | NFR-03 feasibility; input to D-gap-10. | NFR-03 |
| **T-19** | **Ad-block hot-path micro-benchmark** | P (W8) — memory part G | 20k-domain trie: p95 lookup on the 1 GB device from the interception thread; heap delta (counts in NFR-01). Check interception of redirects, WebSocket, SW. | NFR-04 and NFR-01 inputs. | FR-17 |
| **T-20** | **IME interplay** | P (W3) | Gboard for TV / Fire keyboard with WebView inputs: key routing in cursor mode, dismiss behaviour, password fields. | Input routing rules. | FR-05, FR-41 |
| **T-21** | **TalkBack with cursor/focus modes** | P (W15) | Enable TalkBack; verify the D-pad-only path. | NFR-09 feasibility; D-gap-18. | NFR-09 |
| **T-22** | **Native playback with WebView alive vs destroyed** | P (W10) | Media3 HLS 1080p with the renderer alive vs torn down; memory per T-04 method. | NFR-01 ≤400 MB input. | FR-23 |
| **T-23** | **Thumbnail capture** | P (W5) | PixelCopy vs `draw()` on a hardware-accelerated WebView, incl. video pages; bitmap memory at 10 tabs. | D-gap-17. | FR-15 |

**Fixes recommended to existing protocols** (for the protocol owner via the Lead; I did not edit them):
- **P3:** replace "`dumpsys meminfo <pkg>` (ALL PIDs…)" with the host + renderer-by-PID method [V8]. Add PMF alongside PSS.
- **P4:** precede with T-01 (multiprocess) and T-03 (trigger viability). Record whether the device is single-process. Define the guard window.
- **P1:** add HDMI/display-mode/CEC triggers (T-11).
- **All protocols:** log paths reference `W0/logs/…`, but no `W0/` directory exists. The repo uses `docs/validation/` and `reports/W0/` (C-10).

---

## 11. Document conflicts identified (per AGENTS.md §2: identify → reference → impact → ask Lead)

| ID | Conflict | Exact references | Impact | Ask |
|---|---|---|---|---|
| C-01 | UIDT primary path needs `RUN_USER_INITIATED_JOBS`, which is not in the allow-list | TECH_SPEC FR-31; §4; NFR-10; [V5] | CI block or a spec violation | Lead → founder (Q-02) |
| C-02 | "Hard ceiling 2 live" vs mandatory new WebView for honoured popups | TECH_SPEC FR-11; FR-45; [V10] | FR-45 unimplementable as written | Q-03 |
| C-03 | `onPause()+pauseTimers()` can't be applied per WebView | TECH_SPEC FR-12; [V9] | Standby behaviour undefined | Q-04 |
| C-04 | "0 app exits on `chrome://crash`" vs single-process WebView default on some devices | TECH_SPEC FR-43, NFR-06; protocols P3/P4; PRD §7; [V2] | Gate cannot pass on such devices | Q-01 |
| C-05 | §4 lists P1 permissions (FR-16 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) as "declare". Unclear whether the v1 manifest must include P1-only permissions | TECH_SPEC §4; FR-16 (P1); NFR-10 | Possible Play FGS declaration for an unused type in v1 | Lead clarification |
| C-06 | W1–2 items depend on W5–7 data layer | PRD §8 (W1–2: FR-08/43/48); TECH_SPEC FR-08 (Room FTS), FR-29 (W5–7), FR-43 acceptance ("TabState se"), FR-48 ("per-site … persisted") | W1–2 acceptance not achievable as written | Q-10 |
| C-07 | Memory measurement method in protocol under-counts the renderer | protocols.md P3; TECH_SPEC NFR-01; [V8] | Invalid NFR-01 evidence | Lead → protocol owner |
| C-08 | JS bridge restricted to own origin vs features needing page→app events on arbitrary origins | TECH_SPEC §5; FR-03; FR-45 | Design ambiguity | Lead clarification (D-gap-15) |
| C-09 | Default desktop UA (FR-33) has no `; wv` token, so it is effectively equivalent to the deleted FR-33b. FR-56 guard becomes the sole OAuth control | TECH_SPEC FR-33, FR-33b (deleted), FR-56, D-07, D-12; PRD §2 non-goals | Policy-compliance reliance on guard completeness | Lead / policy owner (Q-12) |
| C-10 | Doc paths reference `W0/…` files that live elsewhere in the repo | W0_Validation_Plan header + §6; protocols.md header + "Logs index"; PRD §12 vs actual `docs/validation/`, `docs/research/`, `reports/W0/` | Evidence may be stored in non-existent locations | Lead (doc owner) |
| C-11 | Safe Browsing "default OFF" relies on WebView default being overridden; WebView default is ON | TECH_SPEC §5, D-13; PRD §11 decision 13; [V6] | If not implemented explicitly, `gms` builds ship with Safe Browsing ON | Lead (D-gap-13) |

---

## 12. Questions requiring founder approval

| # | Question | Options (not recommendations unless stated) | Blocking |
|---|---|---|---|
| **Q-01** | How should FR-43/NFR-06 apply to devices where WebView is **single-process** (R-01)? | (a) Exclude such devices from the supported matrix; (b) accept "best effort": app restarts with session restore and acceptance is scoped to multiprocess devices; (c) raise minSdk (large install-base impact, A-22). **Decide after T-01 data.** | D-2 gate |
| **Q-02** | Add `RUN_USER_INITIATED_JOBS` to the §4 allow-list for FR-31, or drop UIDT and use the WorkManager FGS (dataSync, 6 h cap) on all API levels? | Spec change either way (TECH_SPEC is approved; I have not modified it). | W13 (decide by D-3) |
| **Q-03** | Popups vs the 2-live ceiling: allow a **transient 3rd WebView**, **evict standby** to host the popup, or **open in a new tab without transport** (breaks `window.opener` flows)? | Decide after T-06. | FR-45 design (W8) |
| **Q-04** | Standby = blank prewarm or MRU live page? Is a live background page allowed to keep running JS? | Decide after T-05 (A-16). | W5 |
| **Q-05** | targetSdk 36 back handling: migrate to the predictive-back APIs now, or use the temporary manifest opt-out [V4]? | Opt-out is described as temporary by Google. | W3 |
| **Q-06** | Safe Browsing "default OFF" mechanism: manifest flag (may prevent runtime opt-in; UNVERIFIED) vs per-instance disable. | Tied to PRD §11 decision 13; decide after T-15. | D-3 |
| **Q-07** | Set WebView `MetricsOptOut=true` to match the zero-telemetry promise? [V6] | Privacy positioning decision. | W1 |
| **Q-08** | Downloads on API 26–28: accept app-specific storage (no new permission), or another approach? Also: are `blob:`/`data:` downloads in v1 scope? | `WRITE_EXTERNAL_STORAGE` is banned by §4. | W13 |
| **Q-09** | Approve a **multi-module layout + build-time import rule** (§2) as the implementation of the approved layers? | Architecture-level change, so founder authority. | W1 |
| **Q-10** | Resolve the W1–2 / W5–7 dependency (C-06): pull a minimal Room/`TabState` skeleton into W1–2, or scope W1–2 acceptance for FR-43/FR-08/FR-48? | Roadmap change. | W1 |
| **Q-11** | Should downloads run in a separate process to survive browser-process LMK? | Default assumption: **no** for v1 (simplicity), unless T-14 shows frequent kills. | W13 |
| **Q-12** | FR-56 scope: block only third-party OAuth flows, or all Google sign-in on `accounts.google.com` (incl. YouTube/Gmail first-party login)? Is the URL guard accepted as the sole compliance control given the desktop UA (C-09)? | Policy decision. | W1–2 |
| **Q-13** | NFR-01 metric: keep PSS, or adopt PMF / RSS-anon as officially recommended [V8]; which device each figure binds to? | Decide before the T-04 baseline is interpreted. | D-2 |

---

## 13. Evidence references (verified 2026-11-27)

| ID | Source | What it supports |
|---|---|---|
| V1 | AOSP `frameworks/base` `services/core/java/com/android/server/webkit/SystemImpl.java` @ `oreo-release` — `isMultiProcessDefaultEnabled()` returns `true`. https://github.com/aosp-mirror/platform_frameworks_base/blob/oreo-release/services/core/java/com/android/server/webkit/SystemImpl.java | Android 8.0 multiprocess default |
| V2 | Same file @ `oreo-mr1-release`, `pie-release`, `android10-release` — `return Build.SUPPORTED_64_BIT_ABIS.length > 0 \|\| !ActivityManager.isLowRamDeviceStatic();` with comment "Multiprocess is also enabled for 32-bit devices unless they report they are 'low ram'." e.g. https://github.com/aosp-mirror/platform_frameworks_base/blob/pie-release/services/core/java/com/android/server/webkit/SystemImpl.java · [V2-file] `WebViewUpdateServiceImpl.java` @ `android10-release` (`enableMultiProcess` → `killPackageDependents`) | R-01, R-10 |
| V3 | Same file @ `android11-release` … `android14-release` — "Multiprocess is enabled by default for all devices." | Android 11+ default |
| V4 | Android Developers — *Behavior changes: Apps targeting Android 16* (predictive back: "`onBackPressed` is not called and `KeyEvent.KEYCODE_BACK` is not dispatched anymore"; opt-out `android:enableOnBackInvokedCallback="false"`; edge-to-edge opt-out disabled). https://developer.android.com/about/versions/16/behavior-changes-16 | R-03 |
| V5 | Android Developers — *User-initiated data transfer* (`RUN_USER_INITIATED_JOBS`; notification required; schedule while visible; "no Jetpack library that supports UIDT jobs"; WorkManager FGS fallback; stopped when "app process is killed due to low device memory"). https://developer.android.com/develop/background-work/background-tasks/uidt | R-02 |
| V6 | Android Developers — *Manage WebView objects* (Safe Browsing default true + manifest opt-out; `MetricsOptOut`; Termination Handling; Renderer Importance API; 1 MB `savedInstanceState`). https://developer.android.com/develop/ui/views/layout/webapps/managing-webview | §5.1, §5.8 |
| V7 | Android Developers — *Manage WebView state efficiently* (`WebViewCompat.saveState(webView, outState, maxSizeBytes)` + `includeForwardState`; 1 MB budget shared across process). https://developer.android.com/develop/ui/views/layout/webapps/manage-state | §5.2, §6.4 |
| V8 | Android Developers — *Manage and diagnose WebView memory* (low-RAM single-process fallback; `dumpsys meminfo <pkg>` = host only, renderer by PID; post-`destroy()` memory; PMF metrics; in-app APIs can't see renderer). https://developer.android.com/develop/ui/views/layout/webapps/manage-webview-memory | R-06, R-07, §6 |
| V9 | Android Developers — `android.webkit.WebView` reference (`pauseTimers()` global; `onPause()` "does not pause JavaScript"; `saveState`/`restoreState` no display data). https://developer.android.com/reference/android/webkit/WebView | R-05 |
| V10 | Android Developers — `android.webkit.WebChromeClient` reference (`onCreateWindow`: must create new WebView + `WebViewTransport`; "no trustworthy way to tell which page requested the new window"; `onHideCustomView`). https://developer.android.com/reference/android/webkit/WebChromeClient | R-04, §5.4, §5.7 |

Repository documents cited by section: TECH_SPEC v1.2 §1–§5; PRD v1.2 §0/§2/§4/§7/§8/§11; W0 Validation Plan §2/§3/§5/§6; protocols.md P1–P10; validation-results.md (all ⏳); `reference/browsehere/Better_Android_TV_Browser.md` Q1–Q20; complaints doc (Pillar 1/4).

---

## 14. Task completion report (AGENTS.md §9)

| Item | Detail |
|---|---|
| **Assigned task** | Android TV technical architecture & feasibility audit; deliver `reports/project-audit/ANDROID_ARCHITECTURE_AUDIT.md` |
| **Work completed** | Repository inspection (no Android code present). Review of TECH_SPEC/PRD/W0 docs and engineering references. Verification of 10 platform facts against official Android docs and AOSP source. Risk, limitation, compatibility, decision-gap and W0-experiment analysis. Conflict register. Founder question list. |
| **Files created / modified** | Created: `reports/project-audit/ANDROID_ARCHITECTURE_AUDIT.md`. **No other file modified.** |
| **Evidence and references** | §13 (V1–V10) + document section citations throughout |
| **Tests performed** | None. Static analysis + official-source verification only. |
| **Test status** | All technical items: **NOT TESTED**. Device/OEM-specific claims: **UNVERIFIED**. |
| **Unresolved issues** | C-01…C-11 (§11); Q-01…Q-13 (§12) |
| **Dependencies** | Devices from W0-1 procurement for T-01…T-23; Lead assignment for any isolated prototype work in `prototype/` |
| **Recommended next action** | Lead to (1) route C-01…C-11 to their document owners; (2) schedule the gate-critical experiments **T-01, T-02, T-03, T-04, T-05, T-06, T-07, T-12, T-15, T-17, T-18** in W0-2; (3) take Q-01, Q-02, Q-03, Q-09, Q-10 to the founder before D-2/D-4. |

*Audit ends here. Waiting for the Lead Agent.*
