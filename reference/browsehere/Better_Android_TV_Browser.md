# BrowseHere se Better Android TV Browser — Expert Answers

Pehle ek **important observation** jo aapki findings se nikalti hai, kyunke yeh baaki sab answers ko shape karti hai:

> **BrowseHere actually koi naya browser nahi — yeh AOSP Stock Browser (Android 4.x ka `com.android.browser`) ka TCL fork hai.** Class names `Controller`, `TabControl`, `Tab`, `PieControl`, `PieMenu`, `BrowserProvider`, `Bookmarks`, `BrowserWebViewFactory` — yeh sab literally AOSP Browser ke class names hain. TCL ne is par TV remote ke liye `MouseControl`, apna homepage aur apni telemetry (`SubmitDataService`, `TCLUserCollection` SOAP services) add ki hai. Target SDK 23 aur Min SDK 14 bhi yahi confirm karta hai.

Is ka matlab:
- AOSP Browser ka source Apache 2.0 license mein open hai (`platform/packages/apps/Browser` — ab deprecated). Aap usay **study** kar sakte hain, lekin 2012-era architecture ko copy karna galti hogi.
- "Best browser" hone ki wajah architecture nahi, balke yeh hai ke TCL TVs par yeh **pre-installed** aata hai aur TV remote ke liye basic cursor deta hai. Is se better banana realistic hai.
- Aik aur open-source reference zyada useful hai: **TV Bro** (GitHub, Kotlin, WebView-based, cursor navigation, GeckoView optional). Usay zaroor study karein.

Ab aapke questions:

---

## A. Architecture

### 1. Native Android + WebView, modern Kotlin mein?

**Haan — yeh sahi decision hai**, lekin AOSP Browser ka structure copy na karein. Recommended stack:

- **Kotlin + Coroutines + Flow**
- **Single-Activity architecture** (TV par multiple activities focus/back-stack ke masle banati hain)
- **MVVM / MVI** — `TabManager`, `NavigationController`, `InputController` alag layers
- UI: **Jetpack Compose for TV** (`androidx.tv.material3`) ya Leanback. Compose for TV ab stable hai aur focus handling modern hai. WebView `AndroidView` ke andar embed hoga.
- DI: **Hilt**
- `androidx.webkit` library — WebView ke modern features backward-compatible tareeqay se use karne ke liye

Sabse important design rule: **WebView ko ek interface (`BrowserEngine`) ke peechay rakhein** — tab UI, tabs, history, ad-block sab us interface se baat karein. Is se Question 2 ka risk kam ho jata hai.

### 2. System WebView sufficient hai ya custom Chromium?

**Shuruat System WebView se karein.** Comparison:

| Option | Pros | Cons |
|---|---|---|
| **System WebView** | APK 5–15 MB, hardware video decode, Widevine DRM automatic, Play Store se auto-update (GMS devices par), zero build complexity | Version aapke control mein nahi; cheap AOSP boxes par purana (Chromium 70–90) aur non-updatable; kuch APIs limited (e.g. request initiator in `shouldInterceptRequest`) |
| **GeckoView (Mozilla)** | Full engine control, extensions (uBlock Origin!) support, consistent version | APK +70–90 MB, TV par officially supported nahi, DRM/hardware decode integration kamzor, memory heavy — 1–2 GB RAM boxes par problem |
| **Custom Chromium build** | Total control | Build infra, 100+ MB APK, maintenance nightmare — sirf bari companies ke liye realistic |

Practical plan: **Phase 1 WebView**, engine abstraction rakhein, aur agar future mein zaroorat ho to GeckoView ko optional "advanced engine" module ke tor par add karein (TV Bro yahi karta hai).

### 3. WebView engine future mein update?

Yeh aapke haath mein nahi, lekin manage ho sakta hai:
- **Google TV / certified devices** — WebView Play Store se update hota rehta hai (Chromium ~monthly). Aapko kuch nahi karna.
- **Fire TV** — Amazon apna WebView ship karta hai, woh bhi update hota hai.
- **AOSP cheap boxes** — stuck. Yahan aap sirf itna kar sakte hain: `WebViewCompat.getCurrentWebViewPackage()` se version detect karein, bohat purana ho to warning + "Update Android System WebView" link dikhayein, aur `WebViewFeature.isFeatureSupported()` se features gracefully degrade karein.

Aap apna WebView bundle **nahi** kar sakte — Android sirf system-approved WebView provider allow karta hai.

---

## B. Android TV Experience

### 4. Remote navigation + mouse pointer ka best architecture

Yahi woh area hai jahan aap BrowseHere se clearly better ho sakte hain. **Do modes + unified `InputController`:**

**Mode 1 — Virtual Cursor (default, BrowseHere jaisa lekin behtar):**
- Overlay `View` jisme cursor draw hota hai, WebView ke upar
- D-pad press par cursor move — **acceleration curve** lazmi (hold karne par speed barhe, warna 1080p screen par cursor ghaseetna torture hai)
- OK button → WebView par synthetic `MotionEvent` dispatch (`ACTION_DOWN` + `ACTION_UP`, `TOOL_TYPE_MOUSE` ya touch)
- Cursor screen edge ke qareeb jaye to **auto-scroll**
- **Long-press OK** → scroll mode ya context menu
- Cursor 3–4 second idle ho to fade out

**Mode 2 — Spatial / Focus Navigation (links ke darmiyan jump):**
- JavaScript inject karke focusable elements (`a`, `button`, `input`, `[tabindex]`) ko D-pad direction ke mutabiq select karna — jaise Smart TV browsers (Tizen/webOS) karte hain
- Text-heavy sites aur forms par cursor se bohat zyada fast hai
- Chromium ka `--enable-spatial-navigation` flag WebView mein available nahi, isliye khud implement karna hoga (TV Bro aur kuch Android TV browsers aisa karte hain)

**Common layer:**
- Sab keys `Activity.dispatchKeyEvent()` par intercept hoon
- Real USB/Bluetooth mouse, air mouse aur gamepad bhi support karein (`InputDevice.getSources()` se detect)
- Media keys (Play/Pause) → page ke `<video>` ko JS ke zariye control
- Back key priority: fullscreen video exit → popup/menu close → WebView `goBack()` → tab close → app exit
- **On-screen keyboard** par khaas tawajjo — TV par text input sab se bara pain point hai. Apna optimized URL/search entry screen banayein jo suggestions dikhaye.

### 5. Custom Pie Menu zaroori hai?

**Nahi.** Pie menu AOSP Browser ka 2012 ka UI pattern hai, TCL ne bas rakha hua tha. Modern alternative:
- **Overlay toolbar** jo Menu key / cursor ko top par le jane / Back-hold par appear ho: URL bar, back/forward, tabs, bookmarks, zoom, desktop-mode toggle, settings
- **Long-press OK** par ek chhota radial/quick-action popup (context actions: open in new tab, copy link, download) — yeh optional hai

Pie menu ka ek faida tha ke kam buttons wale remote par kaam karta tha — woh faida aap toolbar + long-press se bhi le sakte hain.

### 6. Different manufacturers / remotes support

Fragmentation real hai. Rules:
- **Key codes normalize karein:** `KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER`, `KEYCODE_NUMPAD_ENTER`, `KEYCODE_BUTTON_A` → sab "select"
- **Menu key assume na karein** — Google TV reference remote (Chromecast), Fire TV remote mein Menu nahi hota. Har action D-pad + OK + Back se reachable ho.
- Colored buttons (RED/GREEN/YELLOW/BLUE), channel up/down, page up/down → shortcuts (configurable)
- **Key remapping settings** screen dein — user khud set kare "Blue button = new tab"
- Manifest: `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`, `android.software.leanback`
- **No-GMS build variant** (Fire TV + AOSP boxes ke liye) — Play Services dependency optional rakhein
- Test matrix: Chromecast with Google TV (2 GB RAM — memory discipline test), Nvidia Shield, Xiaomi Mi Box/Stick, Fire TV Stick (4K + Lite), Sony/TCL/Hisense built-in Google TV, ek cheap AOSP box, aur Android TV emulator API 30–34

---

## C. Video Playback

### 7. WebView HTML5 video + fullscreen ke liye sufficient?

**Mostly haan**, with caveats:
- HTML5 `<video>` hardware decode (MediaCodec) WebView mein automatic hai
- `onShowCustomView` / `onHideCustomView` properly implement karein: system UI hide, `FLAG_KEEP_SCREEN_ON`, custom view ko full-screen container mein daalna, media keys forward karna
- `mediaPlaybackRequiresUserGesture = false` set karein (autoplay)
- **DRM:** WebView EME + Widevine support karta hai (MediaDrm ke zariye), lekin bohat si sites (Netflix, Prime, Hotstar) **User-Agent/device detection** se browser ko block karti hain ya sirf SD deti hain. Yeh engine ki nahi, service policy ki limitation hai — koi TV browser isay nahi tor sakta.
- **YouTube:** desktop site WebView mein chalti hai; `youtube.com/tv` kuch UAs ke saath kaam karti hai lekin Google isay regularly restrict karta hai — is par depend na karein.
- **Picture-in-Picture** (Android 8+) video ke liye add karein — TV browsers mein yeh differentiator hai

### 8. Media3/ExoPlayer integrate karein?

**Haan, lekin sahi role mein.** ExoPlayer WebView ke andar page ka video replace nahi kar sakta. Uska role:
1. **Direct media URLs** — `.mp4`, `.mkv`, `.m3u8`, `.mpd` click ho ya `onDownloadStart` mein video MIME aaye → native ExoPlayer screen mein open karein (WebView HLS ko achhi tarah handle nahi karta)
2. **"Play in native player" / video sniffing** — page ke `<video>` src detect karke native player mein chalana (Puffin TV, TV Bro style feature; users isay bohat pasand karte hain kyunke native player mein seek/subtitles/audio-track control behtar hota hai)
3. **Local files** (USB) playback

Size impact ~2–4 MB. Isay **dynamic feature module** bhi bana sakte hain.

### 9. Codecs, DRM, streaming formats

- **Codecs:** Device ke MediaCodec par depend karein — HEVC/VP9/AV1 hardware-dependent hain. `MediaCodecList` se capabilities read karke "Unsupported codec" ka clear message dein. Software decoders bundle na karein (TV CPUs weak hain).
- **DRM:** Widevine ke liye `FrameworkMediaDrm` (ExoPlayer) — WebView mein automatic. L1 vs L3 device par depend karta hai. **DRM bypass ki koshish bilkul na karein** — Play policy aur legal issue.
- **Streaming:** HLS/DASH → ExoPlayer native support. Subtitles (WebVTT/SRT) aur audio track selection UI dein.
- **Fallback:** "Open with" intent (VLC, MX Player, Kodi) hamesha available rakhein.

---

## D. Browser Features

### 10. Multiple tabs ka efficient architecture

TV devices mein 1–2 GB RAM hoti hai aur har WebView instance 50–150 MB le sakta hai. Isliye:
- **`TabManager`** mein tabs ka model (URL, title, favicon, thumbnail, saved state) rakhein — WebView instance tab ka *optional* part ho
- **Sirf 2–3 WebViews live** rakhein (current + recently used). Baqi tabs ke liye `webView.saveState(bundle)` + screenshot thumbnail save karein aur WebView `destroy()` kar dein
- Tab switch par lazy restore (`restoreState`) — user ko thumbnail foran dikhe, page background mein load ho
- `onTrimMemory()` par aggressive eviction
- Background tabs par `onPause()` + `pauseTimers()`
- Single `FrameLayout` container jisme WebView swap ho — ViewPager/multiple containers nahi
- Max tabs limit (8–12) aur grid-style tab switcher with D-pad focus

### 11. Bookmarks, history, downloads ka storage

- **Room (SQLite)** — bookmarks (folders ke saath), history (FTS4 full-text search ke saath taake URL bar suggestions fast hon), downloads metadata
- **DataStore** — settings
- Favicons/thumbnails → files in `cacheDir`, DB mein sirf path
- **Downloads:** System `DownloadManager` TV par kaam karta hai lekin uska UI accessible nahi. Behtar: apna downloader (OkHttp + Foreground Service + notification + resume support) ya WorkManager. Files `MediaStore.Downloads` (API 29+) ya app-specific dir mein. Downloads screen apni banayein.
- Bookmarks export/import (HTML Netscape format + JSON) — sync ke baghair bhi users ko backup mile

### 12. Ad blocking — reliable tareeqa

Yeh sabse bara differentiator ho sakta hai. Implementation:
- **Network-level blocking:** `shouldInterceptRequest()` mein har request filter se match karein; blocked ho to empty `WebResourceResponse` return karein
- **Filter lists:** EasyList + EasyPrivacy (ABP syntax) parse karein; shuruat mein **hosts-based list** (e.g. StevenBlack) simpler hai aur 70–80% ads cover karta hai
- **Matching engine performance critical hai:** `shouldInterceptRequest` IO thread par aata hai, har request par <1 ms chahiye. Rules ko compiled/trie structure mein rakhein, startup par load, weekly remote update (lists ko apne CDN/GitHub par mirror karein)
- **Cosmetic filtering** (element hiding): `shouldInterceptRequest` isay nahi kar sakta — CSS rules `evaluateJavascript` se inject karein, ya `WebViewCompat.addDocumentStartJavaScript()` (jahan supported) use karein taake flicker na ho
- **Limitation:** WebView request ka initiator/first-party reliably nahi batata — current page URL ko first-party maan kar `$third-party` rules approximate karein
- Existing options: eyeo ka adblock-android SDK (heavy), ya Brave ka `adblock-rust` via JNI (fast, lekin native build chahiye). Khud lightweight Kotlin engine banana realistic hai.
- Per-site whitelist aur blocked count indicator dein
- Licenses check karein (EasyList CC BY-SA/GPL)

### 13. Incognito / Private browsing

Yahan WebView ki ek bari limitation hai: **`CookieManager` aur storage process-wide global hain.** Solutions:
1. **Best (modern):** `androidx.webkit` **Profile API** (`ProfileStore`, `WebViewFeature.MULTI_PROFILE`) — alag cookie jar/storage ek hi process mein. Naye WebView versions par available.
2. **Fallback (purane devices):** incognito tabs ko **alag Android process** mein chalayein (`android:process`), us process mein `WebView.setDataDirectorySuffix("incognito")` set karein, aur process exit par data directory delete karein
3. Dono cases mein: history/bookmarks mein save nahi, `setSaveFormData(false)`, cache clear on close, tab thumbnails memory-only, optional `FLAG_SECURE`

Normal mode mein bhi "Clear data on exit" option dein.

---

## E. Backend and Costs

### 14. Completely offline/local architecture possible hai?

**Haan, bilkul.** TCL ke saare endpoints (user collection, user logs, favorites web-service, homepage zip, nav config) **telemetry aur remote-config** ke liye the — browser ki core functionality ke liye zaroori nahi. Aapke browser ko sirf in cheezon ke liye network chahiye:
- Web pages (obviously)
- Filter list updates — static files (free hosting)
- Search suggestions — directly search engine ke public suggest endpoints se (Google `suggestqueries.google.com`, DuckDuckGo `duckduckgo.com/ac/`, Bing) — optional aur user-selectable
- App updates — Play Store/Amazon Appstore

Koi account, koi server-side user data nahi. Privacy bhi behtar, compliance burden bhi kam.

### 15. Search, voice, homepage — paid APIs?

**Nahi:**
- **Search:** sirf URL banayein — `https://www.google.com/search?q=...`. Google/Bing/DuckDuckGo/Brave/Startpage options dein. Koi API key nahi.
- **Voice search:** Android `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` — free, GMS devices par Google app handle karta hai (yahi BrowseHere use karta hai). Limitation: Fire TV aur non-GMS boxes par available nahi → fallback: on-screen keyboard, ya **Vosk** (offline, open-source, ~40–50 MB model, optional download) agar aap serious hain.
- **Homepage:** local HTML (WebViewAssetLoader ke zariye) ya pure Compose speed-dial. BrowseHere ka obfuscated/eval-packed JS replicate karne ki zaroorat nahi.

Actual costs: Google Play developer account $25 (one-time), Amazon Appstore free, optional domain/CDN ~$10–15/year.

### 16. Apna lightweight backend banayein?

**Shuru mein nahi.** Jab zaroorat ho to **static/serverless** rakhein:
- Static JSON config (default search engines, featured sites, filter list URLs, announcements) → GitHub Pages / Cloudflare Pages (free)
- Filter list mirror → same
- Crash reporting → Firebase Crashlytics (free) ya Sentry — opt-in
- Future mein bookmark sync chahiye ho to tab server sochein (ya Google Drive app-data folder use karein — koi server nahi)

Principle: **config-driven app, zero user data collection by default.**

### 17. TCL backend integrations ke alternatives

| TCL ka component | Purpose | Aapka alternative |
|---|---|---|
| `TCLUserCollection/*WebService` (SOAP) | User registration, logs, favorites upload | Kuch nahi / opt-in anonymous analytics |
| `SubmitDataService` | Telemetry | Crashlytics (opt-in) |
| `browserZip.zip` / `LocalPageDownloadService` | Remote homepage update | In-app assets + static JSON config |
| `findPageConfig` (my7v) | Search/nav config | Local defaults + static JSON |
| `com.tcl.tvmanager`, `android.tclwidget`, `com.tcl.tvos.addon` | TCL TV hardware hooks (volume, picture mode, etc.) | Standard APIs: `AudioManager`, PiP, `WindowInsets`; in TCL libs ki zaroorat nahi |

---

## F. Security and Compatibility

### 18. Unnecessary permissions

BrowseHere ki list Android 4.x era ki hai. Modern browser ke liye:

**Hata dein (unnecessary / deprecated / Play policy risk):**
`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (TV par GPS nahi; chahein to sirf runtime par jab site mange), `GET_ACCOUNTS`, `USE_CREDENTIALS`, `MANAGE_ACCOUNTS`, `READ_CONTACTS`, `READ_PROFILE`, `NFC`, `WRITE_SETTINGS`, `READ/WRITE_SYNC_SETTINGS`, `SET_WALLPAPER`, `com.android.browser.permission.*` (ab exist hi nahi), `INSTALL_SHORTCUT`, `WRITE_EXTERNAL_STORAGE` (API 29+ scoped storage), `ACCESS_WIFI_STATE` (zaroorat nahi)

**Rakhein:**
`INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` (downloads, Android 14), `POST_NOTIFICATIONS` (Android 13+), `WAKE_LOCK` (ya bas `FLAG_KEEP_SCREEN_ON` — permission nahi chahiye), `RECORD_AUDIO` sirf agar `SpeechRecognizer` directly use karein (RecognizerIntent ke liye nahi chahiye), `READ_MEDIA_*` sirf file upload feature ke liye

Play Store review mein extra permissions rejection ka sabab ban sakti hain.

### 19. HTTPS, certificates, cookies, data security

- **SSL errors:** `onReceivedSslError` mein **default `handler.cancel()`** + interstitial jo user ko detail dikhaye. Blanket `proceed()` kabhi nahi — Play Store isay reject karta hai.
- **HTTPS-first mode:** pehle `https://` try karein, fail ho to `http://` with "Not secure" indicator. Mixed content: `MIXED_CONTENT_COMPATIBILITY_MODE` ya `NEVER_ALLOW`.
- **WebView settings hardening:** `allowFileAccess=false`, `allowFileAccessFromFileURLs=false`, `allowUniversalAccessFromFileURLs=false`, `allowContentAccess=false`, `geolocationEnabled=false` (default). Local homepage ke liye `file://` nahi — **`WebViewAssetLoader`** (`https://appassets.androidplatform.net/`) use karein.
- **JavaScript bridges:** `@JavascriptInterface` sirf apne pages par aur origin check ke saath; sites ke saath communication `WebMessageListener` with allowed-origins list
- **Safe Browsing:** `setSafeBrowsingEnabled(true)` + `onSafeBrowsingHit` handle (GMS devices par)
- **Cookies:** third-party cookies default off (`setAcceptThirdPartyCookies(false)`), `CookieManager.flush()` on pause
- **Intent/URL handling:** `shouldOverrideUrlLoading` mein sirf `http/https` auto-load; `intent://`, `market://` waghera par user confirmation; `Intent.parseUri` ke baad `setComponent(null)`, `setSelector(null)`, aur sirf exported activities — intent-scheme hijacking se bachne ke liye
- **Downloads:** filename sanitize, APK auto-open nahi, warning dikhayein
- **Passwords:** WebView mein password manager nahi hota; agar banayein to Android Keystore + EncryptedSharedPreferences — warna is feature ko skip karein
- **App-level:** `allowBackup=false` (ya WebView data exclude), `debuggable=false`, `usesCleartextTraffic` unavoidable hai browser ke liye lekin apne config endpoints HTTPS-only rakhein
- **Signing:** apni nayi key, Play App Signing use karein (BrowseHere ka SHA1withRSA cert irrelevant hai)

### 20. Modern Android TV / Google TV compatibility

- **targetSdk 34/35** (Play requirement), **minSdk 23** practical hai (Android 6; WebView updatable; is se neeche devices negligible hain)
- Manifest: `LEANBACK_LAUNCHER` category, TV banner (320×180), `touchscreen required=false`, `leanback required=true` (TV-only) ya `false` (agar phone bhi target karna ho)
- **Google TV quality checklist** follow karein: pura app sirf D-pad se navigable, overscan-safe margins (~5%), 10-foot UI text sizes, focus hamesha visible
- **Memory:** Chromecast with Google TV (2 GB) aur Fire TV Stick Lite (1 GB) ko baseline maan kar test karein — tab eviction yahan prove hogi
- **Rendering:** WebView default mobile UA + small viewport deta hai jo TV par ajeeb lagta hai. Default **desktop UA + ~1280px viewport width** rakhein, per-site toggle ke saath. 4K TVs par bhi WebView 1080p density par render karta hai — `textZoom` adjust karne ka option dein
- **Fire TV:** alag build flavor (no GMS), Amazon Appstore submission, Fire OS 7/8 (Android 9/11 based) test
- **Android 13/14 changes:** notification permission, foreground service types, `BroadcastReceiver` export flags
- **PiP** aur **Leanback accessibility (TalkBack)** support
- `WebViewFeature.isFeatureSupported()` har advanced WebView API se pehle — purane WebView par crash nahi, graceful degrade

---

## Summary: Recommended Stack + Priority

**Stack:** Kotlin · Compose for TV · Single Activity · System WebView via `BrowserEngine` interface · `androidx.webkit` · Room + DataStore · Media3 (optional module) · OkHttp (downloads/filter updates) · Hilt · No mandatory backend.

**Jahan aap BrowseHere se clearly better honge:**
1. Cursor acceleration + spatial navigation + proper on-screen URL/search entry (biggest UX win)
2. Built-in ad blocking (BrowseHere mein hai hi nahi)
3. Real tab management with memory discipline
4. Native player for direct streams + PiP
5. Incognito, HTTPS-first, modern security defaults
6. Minimal permissions, zero telemetry — privacy as a selling point
7. Works on Google TV, Fire TV, aur generic boxes — TCL-specific libraries ke baghair

**Suggested build order:** Core shell + WebView + cursor/D-pad input → URL entry/search → tabs with eviction → bookmarks/history → downloads → ad block (hosts list first, EasyList later) → incognito → Media3 player → voice → polish/compat testing.

Agar aap chahein to next step mein main `InputController` (cursor + spatial nav) ya `TabManager` (WebView eviction) ka concrete Kotlin design/skeleton likh sakta hoon — yeh dono woh components hain jo TV browser ko make-or-break karte hain.