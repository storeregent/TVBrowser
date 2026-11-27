# TECH SPEC — Next-Gen Android TV Browser (Engineering)
**Version:** 1.2 (W0-start edition) · **Date:** 2026-11-27 · **Companion:** `PRD_v1.2.md`, `W0_Validation_Plan_v1.2.md`
**Status:** W1 (coding) W0 gate ke baad. **Legend (canonical):** `P0 = v1 (W1–W16)` · `P1 = v1.1 (RC ke baad)` · `P2 = v2/explore`. Har FR ka `Phase` column hai. **P0 = 35 FR** (v1.1 me 34 the; +FR-56 = 35; FR-33b P1 tha aur delete hua, is liye P0 count par asar nahi).

---

## 0. Fix summary (is version me kya badla)

| Review item | Fix location |
|---|---|
| Legend vs Roadmap / P1 items W12–16 me | Section 1 legend + sab P1 sirf v1.1; `PRD` Section 6/8 |
| FR-45 vs FR-19 duplicate + gesture popunder | FR-19 = sirf cross-origin **redirect** guard; FR-45 = new-window ka akela owner, **gesture ke saath bhi cross-origin par confirm** |
| Permissions contradictions | **Section 4** (poori allow-list), FR-48, NFR-10 |
| NFR-05 vs metrics | NFR-05 + `PRD` Section 7 |
| NFR-01 vs W4–6 gate | NFR-01 = **10 tabs @ ≤250MB**; dono jagah same |
| W0b load / W17–18 SLA / count / changelog / date | `PRD` 8/12, `W0_Validation_Plan`, `CHANGELOG_v1.2.md` |
| `READ_MEDIA_*`, FGS types, Android 15 dataSync, TV notifications | Section 4, FR-16/31/39 |
| Google sign-in policy | **FR-56**, FR-33 guardrail, A-07 ab policy item, `PRD` 2/5 |
| FR-41 predictive keyboard | FR-41 = snippets/shortcuts only (P0); **FR-41b custom IME = P1** |
| FR-43 shared renderer | FR-43 acceptance |
| FR-13 back/forward history | FR-13 me apni nav-history list |
| Filter license / 100k domains | FR-17 + A-17 + NFR-01 |
| D-10 DFM complexity | **D-10 revised: Media3 dono flavors me bundled**; NFR-08 ek target |
| Safe Browsing tradeoff | `PRD` Section 11 decision 13 |
| NFR-11 favicons | FR-37 |
| Unmeasurable metrics / form-time | `PRD` 7 + `W0/protocols.md` |

---

## 1. Functional requirements

### 1.1 Core / Input

| ID | Requirement | Pri | Phase | Acceptance | Source |
|---|---|---|---|---|---|
| FR-01 | Single-Activity shell + WebView `AndroidView`, `BrowserEngine` interface ke peechay | P0 | W1–2 | Fake-engine unit test; UI me direct `android.webkit` import na ho | E Q1 |
| FR-02 | Virtual cursor: `Choreographer` 60fps velocity, 150ms micro-precision → cubic accel → friction | P0 | W3–4 | ≥55fps, koi >32ms frame; chhota `[X]` click-able | C#15A |
| FR-03 | Spatial/focus mode: JS se `a`,`button`,`input`,`[tabindex]` par directional jump | P0 | W3–4 | 5 links D-pad se selectable | E Q4 |
| FR-04 | Auto-hide 3.5s idle (video 1.5s); `SOURCE_MOUSE` par foran hide, D-pad par wapas | P0 | W3–4 | Hardware mouse ke saath **kabhi 2 cursor na ho** | C#14 |
| FR-05 | Key normalization (`DPAD_CENTER`/`ENTER`/`NUMPAD_ENTER`/`BUTTON_A`→select); Menu key par dependency nahi | P0 | W3–4 | Google TV + Fire remote dono | E Q6 |
| FR-06 | Back priority: fullscreen → popup/menu → `goBack()` → tab close → exit; long-press OK = context menu. **"Open in Incognito" item v1 me HIDDEN (FR-35/P1 ke saath)** | P0 | W3–4 | v1 menu: new/background tab, download, copy URL | E Q4, C#6 |
| FR-07 | Top auto-reveal HUD toolbar (cursor top edge ya Back). **Pie Menu nahi** | P0 | W1–2 | D-pad se fully navigable | E Q5, C#6 |
| FR-08 | Search-first URL entry with Room FTS suggestions + engine picker | P0 | W1–2 | URL/search ≤3 interactions; suggestions <100ms | E Q4, C#2 |
| FR-09 | Configurable key remapping | P1 | v1.1 | "Blue = new tab" | E Q6 |
| FR-10 | Media keys → `<video>` JS; adaptive scrub (10s→30s→1min hold) | P1 | v1.1 | YouTube-style scrub bar | C#15B |
| FR-41 | **Typing mitigation (scope reduced): saved text snippets + URL/search shortcuts + suggestions ONLY.** Koi custom IME / JS text-injection nahi | P0 | W3–4 | **5 users × 3 forms** usability test me median completion time W0 baseline se ≥30% kam (`W0/protocols.md`) | C#2,#16,#27 |
| FR-41b | Custom predictive on-screen IME (WebView input par system IME aata hai; selection, password fields, autofill, JS injection sab handle karne parenga) | P1 | v1.1 | Separate spec + usability test; v1 me bilkul nahi | Review §2 |
| FR-46 | Edge auto-scroll (cursor edge par smooth scroll) + scroll-only mode | P0 | W3–4 | 1080p par page end tak bina 20+ clicks | E Q4 |
| FR-56 | **Google sign-in honest fallback:** `accounts.google.com` OAuth URL detect → clear message *"Google sign-in is not supported in this browser. Use the native app or another device."* Koi UA-based bypass nahi | P0 | W1–2 | Google OAuth flow par koi `disallowed_useragent` workaround na ho; message D-pad se dismiss-able [S16][S17][S18] | Review §2 |

### 1.2 Tabs / memory / lifecycle

| ID | Requirement | Pri | Phase | Acceptance | Source |
|---|---|---|---|---|---|
| FR-11 | `TabManager`: tab model WebView se alag; **hard ceiling 2 live = 1 active + 1 optional prewarmed standby** | P0 | W5–7 | 10 tabs par ≤2 live, dono NFR-01 budget me | E Q10, C#4 |
| FR-12 | Sirf 1 active WebView render kare; baqi `destroy()`; `onTrimMemory` eviction; `onPause()`+`pauseTimers()` | P0 | W5–7 | **10 tabs @ ≤250MB total PSS idle**; 30-min scripted session me 0 LMK kills | C#4,#7,#18,#20,#23 |
| FR-13 | **Disk-based state**: `TabState` Room row + `tab_state.json` + thumbnail PNG + **apni nav-history list (URL stack + index) Room me**. `saveState()` Bundle sirf in-memory, `onSaveInstanceState`/Parcel me kabhi nahi, >256KB skip. SPA/infinite scroll par scroll restore **approximate** (acceptance me likha hai) | P0 | W5–7 | Home→10min→restore: page + scroll + zoom + **back/forward** wapas; bara Bundle test pass; `TransactionTooLargeException` nahi | C#4, Review §2 |
| FR-14 | `configChanges` lock + `onConfigurationChanged` me WebView/CustomView ko touch na karein | P0 | W5 | BT remote 10x sleep me video drop na ho (A-01 par) | C#1 |
| FR-15 | Grid tab switcher, max 8–12 tabs, focus visible | P0 | W5–7 | 10-foot par focus dikhe | E Q10 |
| FR-16 | MediaSession + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` FGS (+ Play Console FGS declaration with demo video [S3b]); **PiP bonus only** | P1 | v1.1 | PiP na mile to bhi state survive | C#4 |
| FR-43 | **`onRenderProcessGone` P0:** ek app ke multiple WebViews aam taur par **ek hi renderer process share** karte hain → callback **har affected instance** ke liye chalega. Har dead WebView ko remove+destroy karein, references saf karein, active **aur** standby dono recreate karein `TabState` se; `return true`; crash-loop guard (max 2). API 26+ | P0 | W1–2 | `chrome://crash` **2 WebViews ke saath** + forced memory pressure: app crash na ho, dono tabs wapas [S23] | Review §2 |

### 1.3 Ad-block / popups / windows

| ID | Requirement | Pri | Phase | Acceptance | Source |
|---|---|---|---|---|---|
| FR-17 | **v1 filter list:** APK me bundled curated `hosts.txt` — **permissive-only subset** (sirf MIT / CC BY / CC0 sources; official StevenBlack README ki source-license table se [S21]). Repo/script MIT hai lekin aggregated data me CC BY-NC-SA / "non-commercial with attribution" bhi hain → **"MIT" pakka tab tak na likhein jab tak A-17 resolve na ho**. In-app `NOTICE` + attribution + version/date. `shouldInterceptRequest` trie. **Update v1 me sirf app release ke saath; koi network call nahi. 100k+ domains ka in-memory set NFR-01 me count hoga** → isliye subset cap (target ≤20k entries) aur memory measure | P0 | W8–9 | <1ms/request p95; per-site whitelist; blocked-count; list size + RAM delta report; `NOTICE` pass | E Q12, C#28 |
| FR-18 | Cosmetic filtering (`addDocumentStartJavaScript` + fallback) | P1 | v1.1 | Flicker na ho | C#9 |
| FR-19 | **Sirf cross-origin REDIRECT guard** (gesture timestamp + 800ms + toast "Press Back to allow"). `onCreateWindow` / new-window logic **FR-45 me hi** — yahan duplicate nahi | P1 | v1.1 | Streaming site par ad redirect ruke; FR-45 se koi overlap nahi | C#17 |
| FR-20 | EasyList/EasyPrivacy + signed weekly mirror (user-triggered/opt-in, kabhi first-run par nahi); `adblock-rust` JNI tab hi jab benchmark ya p95 bottleneck ho; **v2 Pro se pehle non-commercial source terms re-check** | P2 | v2 | License review pass | C#28 |
| FR-21 | **Ek hi benchmark = d3ward AdBlock Test (%)**; canyoublockit qualitative secondary | P0 | W0/W8–9 | v1 ≥70% (W0 baseline ke baad final); 3 runs, desktop UA + JS on + whitelist off; **"95%" claim nahi** | C#28 |
| FR-44 | Service-worker coverage: `ServiceWorkerControllerCompat` + `SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST` guard, pehle WebView se pehle set; unsupported par skip + coverage disclose | P0 | W8–9 | SW site par blocked count badhe; purane WebView par crash nahi [S24][S25] | Review §2 (v1.1) |
| FR-45 | **`window.open`/`target=_blank` ka akela owner (P0):** gesture + **same-origin** → naya tab; gesture + **cross-origin** → **confirmation prompt** ("Site wants to open [domain] — Allow/Block") *chahe gesture ho*; gesture age >800ms (delayed popunder) ya non-gesture → block + toast; per-site allow-list. `onCreateWindow` yahin handle | P0 | W8–9 | 5 test pages incl. delayed popunder: koi ad tab/redirect chain nahi; FR-19 se duplicate nahi | Review §1 |
| FR-55 | Honest ad-block copy: in-app + listing me **YouTube / first-party ads block nahi hote** (same content domains; DNS/hosts playback todta hai [S22]). GeckoView+uBlock P2 explore | P0 | W13–14 + W15–16 | Store copy review pass; koi misleading claim | Review §2 (v1.1) |

### 1.4 Media

| ID | Requirement | Pri | Phase | Acceptance | Source |
|---|---|---|---|---|---|
| FR-22 | `onShowCustomView`/`onHideCustomView`, system UI hide, `FLAG_KEEP_SCREEN_ON`, autoplay gesture off | P0 | W10–12 | Fullscreen in/out stable | E Q7 |
| FR-23 | Direct/**user-provided** media URLs → **Media3/ExoPlayer har flavor me bundled APK** (koi dynamic feature module / SplitCompat / do packaging paths nahi — D-10 revised) | P0 | W10–12 | HLS resolution (Auto/360/720/1080), audio track, seek; `gms` + `nogms` dono me kaam kare | C#8,#13 |
| FR-24 | Subtitles: in-stream VTT/SRT + local file pick; track UI | P0 | W10–12 | 1 click on/off | C#13 |
| FR-25 | Stream sniffer sirf **"Play in native player"** ke liye; headers jo milen; fallback. **Guardrails:** koi auth/DRM bypass, koi "download this stream" button streaming sites par, koi piracy keyword store copy me | P1 | v1.1 | A-03 + A-21 pass | C#21 |
| FR-26 | "Open with" intent (VLC/MX/Kodi) hamesha | P0 | W10–12 | Unsupported codec par clear message | E Q9 |
| FR-27 | Netflix/Prime URL → `ACTION_VIEW` deep-link; warna clean message | P1 | v1.1 | Koi black screen nahi | C#12 |
| FR-28 | OpenSubtitles API (key/quota/ToS → "no paid APIs" se conflict) | P2 | v2 | A-08 | C#13 |

### 1.5 Data / downloads / compat / privacy

| ID | Requirement | Pri | Phase | Acceptance | Source |
|---|---|---|---|---|---|
| FR-29 | Room (bookmarks, history+FTS4, downloads, `TabState`, nav-history), DataStore, thumbnails `cacheDir` | P0 | W5–7 | Suggestions <100ms | E Q11 |
| FR-30 | Bookmarks import/export (Netscape HTML + JSON) | P0 | W5–7 | Chrome export → TV import | C#5 |
| FR-31 | **Downloader redesigned:** `setDownloadListener` → **user-initiated data transfer job (API 34+) / WorkManager** + **in-app progress UI** (TV par notification shade practically nahi). FGS `dataSync` sirf fallback with **Android 15 6h/24h cap** + `Service.onTimeout()` handling; Play Console FGS declaration + demo video [S3b][S26][S27]. Guardrails: sirf user-initiated, filename sanitize, koi auto "download stream" flow | P0 | W13–14 | Browser band hone par download ruke na; progress in-app dikhe; 6h timeout par graceful stop | C#11, Review §2 |
| FR-31b | Multi-thread + SAF `ACTION_OPEN_DOCUMENT_TREE` (USB/SD) + custom folder | P1 | v1.1 | USB par file likhi jaye | C#11 |
| FR-32 | APK "Install" helper — **sirf `nogms`/sideload**, Play build me `REQUEST_INSTALL_PACKAGES` nahi | P2 | v2 | A-09 | C#11 |
| FR-33 | Per-site rendering: **default desktop UA + ~1280px viewport + zoom 50–300% + `textZoom`**, per-domain profile Room me, 1-click site-settings. **UA sirf LAYOUT ke liye — Google OAuth bypass ke liye nahi (FR-56)**. `; wv` removal experiment (FR-33b) **delete** | P0 | W2 (defaults) + W6 (persistence) | Facebook readable; profile revisit par yaad; `accounts.google.com` par FR-56 chale | C#3,#10,#22, Review §2 |
| FR-34 | 10-foot Reader Mode (`Readability.js`) | P1 | v1.1 | 28sp dark view | C#27 |
| FR-35 | Incognito: `MULTI_PROFILE` → fallback `setDataDirectorySuffix` alag process; FR-06 ka menu item isi ke saath | P1 | v1.1 | Cookies leak na ho | E Q13 |
| FR-36 | Voice search `RecognizerIntent` — **P1, sirf `gms`**; `nogms` me hidden | P1 | v1.1 | Fire TV par crash na ho | E Q15 |
| FR-37 | Local homepage: Compose speed-dial ya `WebViewAssetLoader`; **default speed-dial favicons APK me bundled** (warna pehla homepage network call karega → NFR-11 tootega); koi remote `browserZip.zip` nahi | P0 | W1–2 | **Airplane mode + first-run**: 0 app network calls, homepage + favicons dikhein | E Q14/17, Review §2 |
| FR-38 | Customization: engine picker, OLED theme, cursor size/color/speed, wallpaper, speed-dial grid | P1 | v1.1 | Settings D-pad se | C#19 |
| FR-39 | Phone web-remote (local WebSocket + QR + PWA): live keyboard/trackpad/send link/media. **Koi FGS nahi — server sirf app foreground me bound**; opt-in, koi cloud nahi. **Ab parity feature hai (TV Bro v3 me companion app hai), killer claim nahi** | P1 | v1.1 | 30-min Doze test; form text phone se TV | C#2,#16,#26 |
| FR-40 | Cloud sync — sirf `gms`, opt-in | P2 | v2 | Privacy disclose | C#26 |

### 1.6 Browser basics

| ID | Requirement | Pri | Phase | Acceptance | Source |
|---|---|---|---|---|---|
| FR-47 | File upload `onShowFileChooser` + SAF `ACTION_OPEN_DOCUMENT` — **koi `READ_MEDIA_*` nahi** | P1 | v1.1 | 3 forms par file select | Review §2 |
| FR-48 | Site permission prompts: **geolocation v1 me unsupported** (koi location permission declare nahi; site mange to clear "not supported" message — runtime request manifest ke baghair mumkin hi nahi). Notifications, `onPermissionRequest` (camera/mic) → user prompt + per-site allow/deny persisted. `CAMERA`/`RECORD_AUDIO` declare + `<uses-feature ... required=false>` (Section 4) | P0 | W1–2 | Koi silent grant; deny/revoke kaam; Play TV devices filter na hon | Review §1/§2 |
| FR-49 | Clear browsing data UI (cookies/cache/history/form/site settings) | P0 | W5–7 | Clear ke baad residual cookie na ho | E Q19 |
| FR-50 | History UI: FTS search, delete individual/range | P0 | W5–7 | 1,000 entries par <100ms | E Q11 |
| FR-51 | Default-browser intent filter (`VIEW` http/https + `BROWSABLE`) — **sirf Google TV par rely**; Fire OS par Amazon policy ke chalte assumption nahi | P1 | v1.1 only | Android TV par link open; Fire TV graceful | Review §1 |
| FR-52 | Sideload updates: user-triggered "Check for updates" → static `version.json` + signed SHA-256 → install intent; koi auto-download/first-run call nahi | P1 | v1.1 only | Tampered APK reject | Review §3 |
| FR-53 | **W1 se koi hardcoded string nahi**, sab user-facing strings resources me (structure only) | P0 | W1 | Pseudolocale test pass | Review §3 |
| FR-53b | `ur` (RTL) translation | P1 | v1.1 | RTL layout + 10-foot sizes | Review §3 |
| FR-54 | Find-in-page (D-pad friendly) | P1 | v1.1 only | Highlight + next/prev | E Q4 |

> **Legend check:** FR-47/51/52/54 aur `ur` **W1–W16 roadmap me kahin nahi** — sab v1.1 me. FR-41b bhi P1.

---

## 2. Non-functional requirements

| ID | Metric | Target | Measurement |
|---|---|---|---|
| NFR-01 | **Total PSS = app + WebView renderer + GPU + filter-list in-memory set** | **≤250MB idle with 10 tabs** (1 active + 1 standby), ≤400MB 1080p HLS (2GB device), ≤300MB peak (1GB device). W0 baseline ke baad final | `dumpsys meminfo` all PIDs + `gfxinfo` + filter-set delta |
| NFR-02 | Cursor pacing | ≥55fps, koi >32ms frame | Choreographer/Perfetto |
| NFR-03 | Cold start | <2s | `reportFullyDrawn()` |
| NFR-04 | Ad-block overhead | <1ms/request p95 | instrumented callback |
| NFR-05 | **Do alag metrics:** (a) **internal quality target = user-perceived crash rate ≤0.3%** (`gms`, Play vitals); (b) `nogms` = opt-in ACRA **sample** crash-free sessions (population claim nahi). **1.09% / 0.47% / 8% = Play penalty floors, TARGET nahi** [S3a] | ≤0.3% · ACRA opt-in labeled | Play Console + self-hosted ACRA |
| NFR-06 | 30-min scripted LMK session: 10 tabs → 3 loads → 10min 1080p → Home/restore x3 → BT sleep x10 + `chrome://crash` 2 WebViews | **0 kills, 0 app exits** | `W0/protocols.md` |
| NFR-07 | minSdk **26** (A-22) · targetSdk **36** [S1] | Gradle + Play Console | — |
| NFR-08 | **Ek hi size target, dono flavors:** APK ≤20MB (Media3 bundled) — koi DFM/split target nahi | `apkanalyzer` | — |
| NFR-09 | D-pad-only coverage 100% | TalkBack + audit | — |
| NFR-10 | Permissions sirf **Section 4** ki allow-list (including `CAMERA`, `RECORD_AUDIO`, FGS types, `uses-feature`) | Manifest diff in CI | — |
| NFR-11 | **Zero first-run network calls from app code** — default favicons bundled (FR-37), filter list bundled (FR-17), Safe Browsing opt-in/`gms`/default-OFF. OS/WebView components exclude, A-18 me document | 0 | mitmproxy |
| NFR-12 | Purane WebView par koi crash nahi | `WebViewFeature` guards | — |

---

## 3. Architecture & decisions

```
UI (Compose for TV, Single Activity) → InputController → Domain
  TabManager(Room: TabState + nav-history) · AdBlockEngine · ServiceWorkerGuard
  RendererRecovery (har WebView instance) · MediaSniffer(P1)
→ BrowserEngine (interface) → SystemWebViewEngine [P2: GeckoViewEngine]
→ Room+FTS4 · DataStore · OkHttp · Media3 (bundled, dono flavors) · ACRA(nogms)
```

**Stack:** Kotlin · Coroutines/Flow · Compose for TV · Single Activity · MVVM/MVI · Hilt · `androidx.webkit` · Room/DataStore · OkHttp · Media3 (bundled) · `WebViewAssetLoader` · ACRA (opt-in, self-hosted). AOSP `Controller`/`TabControl`/`PieMenu` copy nahi; **TV Bro = code reference + competitor**.

| # | Decision | Wajah / revisit |
|---|---|---|
| D-01 | v1 = System WebView. Android apna *system WebView provider* ship nahi karne deta — **lekin app apna engine (GeckoView) bundle kar sakta hai** (TV Bro/TV Akhi) | P2 gate: uBlock-grade demand ya ≥3 top sites toottein |
| D-02 | Engine abstraction lazmi | D-01 ka P2 path |
| D-03 | Permissive-only curated hosts subset pehle; EasyList/Rust tab hi jab benchmark/p95 bottleneck | A-17 license result; v2 Pro se pehle re-check |
| D-04 | Koi backend/account/telemetry/IAP in v1 | v2 alag decision |
| D-05 | PiP bonus; state-restore + renderer-recovery asli fix | Device matrix |
| D-06 | Pie Menu nahi, HUD + long-press | — |
| D-07 | DRM bypass / Netflix-in-WebView / **Google-OAuth UA bypass** / YouTube-ad-block nahi | Policy [S3][S16][S17] |
| D-08 | `nogms` pehle din se; **Google TV + AOSP primary, legacy Fire OS secondary** | Vega + Amazon policy |
| D-09 | `configChanges` lock workaround; durability FR-13 + FR-43 se | A-01 |
| **D-10 (revised)** | **Media3 dono flavors me bundled.** DFM/SplitCompat/do packaging paths sirf 2–4MB bachate hain aur complexity zyada hai [S28] | — |
| D-11 | minSdk 26 proposal (`onRenderProcessGone` API 26+, ServiceWorker, PiP; TV Bro `minAPI 26` [S29]) | A-22 |
| **D-12 (new)** | Google sign-in unsupported + honest message; UA sirf layout ke liye | [S16][S17][S18] |
| **D-13 (new)** | Safe Browsing default OFF = conscious privacy↔security tradeoff → founder decision (`PRD` 11) | A-18 |

---

## 4. ⚠️ Permissions allow-list (canonical — poora fix)

**Declare karein (manifest):**

| Permission / declaration | Kis liye | Note |
|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | pages, filter mirror (P1) | — |
| `FOREGROUND_SERVICE` | base | — |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | **FR-16 MediaSession/PiP (P1)** | Play Console FGS declaration form + demo video [S3b] |
| `FOREGROUND_SERVICE_DATA_SYNC` | **FR-31 fallback only** | Android 15: 6h/24h cap, `onTimeout()` [S26][S27]; primary = user-initiated data transfer job |
| `POST_NOTIFICATIONS` (API 33+) | download/playback notification | TV par shade nahi → **progress in-app** (FR-31) |
| `CAMERA` | FR-48 `onPermissionRequest` video capture | **`<uses-feature android:name="android.hardware.camera" android:required="false"/>` lazmi** warna Play TV devices filter ho jayenge |
| `RECORD_AUDIO` | FR-48 audio capture + (P2) direct `SpeechRecognizer` | `uses-feature microphone required=false`; `RecognizerIntent` ke liye zarurat nahi |
| `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`, leanback launcher, 320×180 opaque banner | TV | [S4][S5][S6] |

**Bilkul NAHI:**
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` — **hata diya**. File upload SAF `ACTION_OPEN_DOCUMENT` se hota hai, koi permission nahi chahiye; Play photo/video policy me declaration + justification maangi jati hai [S30].
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — **v1 me geolocation unsupported**; "site mange to runtime" wali baat manifest declaration ke baghair mumkin nahi thi → contradiction khatam. (P1 me geolocation add karna ho to dono declare + runtime prompt + FGS location type alag se.)
- `GET_ACCOUNTS`/`USE_CREDENTIALS`/`MANAGE_ACCOUNTS`, contacts/profile, NFC, `WRITE_SETTINGS`, sync settings, wallpaper, launcher shortcut, `com.android.browser.permission.*`, `WRITE_EXTERNAL_STORAGE`, `ACCESS_WIFI_STATE`, `WAKE_LOCK` (`FLAG_KEEP_SCREEN_ON` kaafi), `REQUEST_INSTALL_PACKAGES` on `gms`.
- **`FOREGROUND_SERVICE_SPECIAL_USE` / koi FGS FR-39 ke liye nahi** — phone-remote server sirf foreground Activity ke saath bound (FR-39).

**NFR-10 CI rule:** upar wali allow-list ke ilawa koi permission/uses-feature merge block ho. Play Console me FGS declaration form (description + user-impact + **video**) W13–14 me tayyar, [S3b].

---

## 5. Security / privacy baseline

`onReceivedSslError` → `cancel()` + interstitial · HTTPS-first + "Not secure" · mixed content `NEVER_ALLOW` · `allowFileAccess/ContentAccess/UniversalAccessFromFileURLs=false` · `geolocationEnabled=false` (FR-48 honest message) · JS bridge sirf apne origin + `WebMessageListener` allow-list · third-party cookies off · `WebViewAssetLoader` · `intent://`/`market://` par confirm + `setComponent(null)` · filename sanitize, APK auto-open nahi · `allowBackup=false`, `debuggable=false`, nayi signing key.

**Safe Browsing (NFR-11 conflict fix):** GMS-based implementation ko up-to-date Play Services chahiye; M126+ real-time server checks karta hai [S31]. `gms` = **opt-in, default OFF** with `WebViewFeature.SAFE_BROWSING_ENABLE` guard + disclosure; `nogms` = bilkul disabled + "unavailable" note (HTTPS-first + SSL interstitial iski jagah). **Tradeoff = founder decision 13.** Actual behavior A-18.

**Google sign-in:** embedded WebView Google OAuth 2.0 policies se blocked hai (`disallowed_useragent`) [S16][S17][S18]. **Koi UA trick nahi** — FR-56 honest message. Default desktop UA (FR-33) sirf layout ke liye hai aur isay OAuth bypass ke liye use nahi kiya jayega; `accounts.google.com` par guard.

**Data:** sab on-device. Koi first-run network call, device ID, analytics by default. Crash: Play vitals (`gms`) + ACRA (`nogms`, opt-in, self-hosted, F-Droid-friendly); Crashlytics sirf `gms` opt-in, F-Droid build me nahi.

---

## 6. Traceability (complaint → FR)

| Complaints | FR |
|---|---|
| #1 | FR-14, A-01 |
| #2/#16/#26 | FR-41(P0), FR-39(P1 parity), FR-40(P2), FR-08 |
| #3/#10/#22 | FR-33(P0 layout-only), **FR-56 (OAuth policy)**, A-07 |
| #4/#7/#18/#20/#23 | FR-11…15, FR-43, NFR-01/05/06 |
| #5 | FR-30 |
| #6 | FR-06/07/15 |
| #8/#13/#21/#24 | FR-22…28, A-03 |
| #9/#17/#28 | FR-17/19/21/44/**45**/55, A-05 |
| #11 | FR-31/31b/32, A-09, A-21 |
| #12 | FR-27 |
| #14 | FR-04 |
| #15 | FR-02/10/46 |
| #19 | FR-38 |
| #25 | `PRD` 5.2, A-20/A-25 |
| #27 | FR-34/39/41 |
| Review §2/§3 | FR-41b, FR-43, FR-13 nav-history, FR-17 license, FR-45, FR-47…54, FR-56, Section 4 |

---

### Sources (official; poori list `W0/procurement-and-sources.md`)
[S1] Play target API · [S2] closed testing · [S3] Play policy / [S3a] Android vitals / [S3b] FGS declaration · [S4–S6] TV form factor + quality + assets · [S16–S18] Google Identity embedded-WebView policy · [S21] StevenBlack README · [S22] AdGuard (YouTube ads) · [S23] `WebViewClient.onRenderProcessGone` · [S24][S25] ServiceWorkerController/Compat · [S26][S27] FGS changes/timeouts, user-initiated data transfer · [S28] Play Feature Delivery (kyun avoid kiya) · [S29] TV Bro GitHub · [S30] Play photo/video permissions · [S31] Chromium Safe Browsing README.
