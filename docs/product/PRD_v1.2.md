# PRD — Next-Gen Android TV Browser (Product)
**Working title:** TVBrowse · **Version:** 1.2 (W0-start edition) · **Date:** 2026-11-27
**Status:** Review fixes done → **W0 shuru; W1 (coding) is document ke W0 results se update hone tak blocked**
**Document set:** `PRD_v1.2.md` (ye file) · `TECH_SPEC_v1.2.md` · `W0_Validation_Plan_v1.2.md` · `W0/` working files · `CHANGELOG_v1.2.md`
**History:** `TV_Browser_PRD_v1.0.md`, `TV_Browser_PRD_v1.1.md` (same-day iterations — is liye date same hai; revision ID alag hai. **v1.3 sirf W0 ke asli results se banegi, desk-review se nahi.**)

---

## 0. Ek page ka khulasa

| | |
|---|---|
| **Problem** | BrowseHere v8.0.770 = Android 4.x AOSP Stock Browser ka TCL fork (Java, minSdk 14/target 23, WebView, telemetry, legacy permissions; koi ad-block engine / native player / downloader UI nahi). 28 complaints → 7 clusters. |
| **Insight** | "Best" isliye nahi ke acha hai — **TCL par pre-installed** hai. 28 complaints TCL users ki hain; hamara wedge doosra hai → **demand abhi unverified** (Section 3, D-1 gate). |
| **Primary wedge (v1.2 me badla)** | **Google TV / Android TV boxes + generic AOSP boxes = PRIMARY.** **Legacy Fire OS = SECONDARY** (Vega OS aur Amazon browser policy is base ko ghatate ja rahe hain). Ye faisla **D-1 se pehle** liya gaya hai. Roku excluded. |
| **Asli competitor** | **TV Bro** — free, OSS, remote-first, tabs/bookmarks/history, ad-block + popup blocking, download manager, voice, UA switch, embedded video player, WebView+GeckoView. Nov 2026 me **v3.0.x** me phone companion app, DoH, profiles, contextual menu, resumable downloads bhi aa gaye hain → W0 teardown lazmi. |
| **Hamara bet** | Kotlin + Compose for TV + System WebView (engine abstraction ke peechay) + D-pad-first input + 1 active WebView (+1 optional standby, hard ceiling 2) + renderer-crash resilience + bundled ad-block + native player + **zero telemetry, zero mandatory backend, v1 bilkul free**. |
| **Honest limits** | YouTube/first-party ads block nahi honge · **Google sign-in support nahi** (embedded WebView Google OAuth policy se block hai — UA se bypass nahi karenge) · DRM/Netflix-in-WebView nahi · koi "95%" claim nahi. |
| **Scope** | **P0 = v1 (W1–W16) = 35 FR** · **P1 = v1.1 (RC ke baad)** · **P2 = v2/explore**. Poora FR index `TECH_SPEC_v1.2.md` me. |
| **Config** | minSdk 26 (proposal) · targetSdk 36 · flavors `gms` + `nogms` · Media3 **dono flavors me bundled** (koi DFM/SplitCompat nahi). |
| **Timeline** | **W0 = 3 hafte** (+ optional W0-4 rework buffer) → **W1–16 build** → W17–18 closed test + production application → **W19–20 external review buffer**. Review ka koi fixed SLA claim nahi. |
| **Cost** | Play $25 · Amazon free · domain ~$10–15/yr · **devices ≈ $400–600** (Chromecast discontinued → Google TV Streamer $149.99 ya used 2GB unit) · koi paid API nahi. |
| **Pehla gate** | **W0** = technical assumptions + demand (D-1) + competitor teardown + official-source/policy check + procurement. Iske baghair code freeze nahi. |

---

## 1. Problem & evidence

Android TV par browsing phone/PC wali assumptions ke saath design hoti hai: sirf D-pad + OK + Back, 1–2 GB RAM, 10-foot screen, Bluetooth remote 2–5 min me sleep. BrowseHere in sab ko 2012 ke code se handle karta hai → fullscreen video drop, LMK process kill, jerky/double cursor, kamzor ad-block, typing ka dard, downloads na hona, Facebook/Google jaise sites ka tootna.

**Evidence caveat:** 28 complaints **TCL/BrowseHere** users ki hain. Target devices par BrowseHere pre-installed nahi — pain points transferable hain, **demand proved nahi**.

| Source | Confidence |
|---|---|
| `BrowseHere_Reverse_Engineering.md` (static APK analysis) | Medium — APK execute nahi hua |
| `Better_Android_TV_Browser.md` (20 architecture answers) | High — engineering baseline |
| `Users_Complaints_...md` (28 → 7 pillars) | Medium-Low — root causes infer kiye gaye |
| Official Play / Amazon / Android / Google Identity docs (full list `W0/procurement-and-sources.md`) | High for platform/policy |

**JTBD:** *"TV par bina phone uthaye, remote se, baghair ads ke, apne (legal/local) video aur links comfortably use karun — aur mera data kisi server na jaye."*

---

## 2. Goals & Non-goals

**Goals (v1)**
1. D-pad se 100% navigable, 60fps cursor + spatial jump + edge auto-scroll.
2. 1–2 GB RAM par stable: max 2 live WebViews, disk-based tab + **nav-history** restore, renderer kill par app crash na ho.
3. Bundled ad-block + popup/new-window guard with **ek hi benchmark (d3ward)** aur honest coverage.
4. User-provided/direct streams ke liye native player (resolution/subtitles/seek).
5. Zero telemetry by default, minimal permissions, modern security defaults, **v1 free**.
6. Ek codebase se Google TV + legacy Fire OS + AOSP sideload.

**Non-goals (v1)**
- Apna *system WebView provider* ship karna (Android allow nahi karta). **Lekin** app apna engine (GeckoView) bundle kar sakta hai → P2 gate, `TECH_SPEC` D-01.
- GeckoView / YouTube ad-block / DRM bypass / Netflix-in-WebView / **Google sign-in**.
- **UA manipulation to bypass Google OAuth** (`; wv` removal experiment **hata diya gaya** — FR-33b delete).
- **Vega OS port** (alag OS/SDK/`.vpkg`) · Roku/webOS/Tizen · TCL libs · Pie Menu.
- Cloud accounts, sync, IAP/Pro, ad networks, paid APIs, `READ_MEDIA_*`, `REQUEST_INSTALL_PACKAGES` on Play build.
- Fire TV par "default browser" registration par bharosa.
- **Custom predictive IME in v1** (FR-41b → P1).

---

## 3. Users & Demand validation (D-1 gate)

| Persona | Device | Need | v1 me |
|---|---|---|---|
| **A. Local-media / sideload utility** | AOSP box, Google TV box, legacy Fire OS | Apne/self-hosted `.mp4`/`.m3u8`, USB files, APK sideload, subtitles. *Store copy me "free movies/IPTV" jaise keywords banned.* | FR-22…28, FR-31 |
| **B. Couch browser** | Google TV / Android TV | Facebook, news, YouTube, search, forms | FR-33, FR-41, FR-50, FR-56 |
| **C. Privacy / ad-block user** | Google TV box, Mi Box, legacy Fire OS | Ads/popunders band, koi tracking na ho | FR-17…21, zero telemetry |

**W0 deliverables** (`W0/demand-and-competitor.md` me protocol):
1. Competitor review mining: TV Bro Play/GitHub reviews, Amazon Silk reviews, r/FireTV, AFTVnews comments → top 10 missing themes. Sawal: *"TV Bro chhor kar ye kyun lein?"*
2. **5–8 interviews** with Google TV + AOSP box users (primary wedge), 2–3 legacy Fire OS (secondary).
3. Landing page + waitlist 14 din, channel-wise signups (Google TV / AOSP / Fire OS).
4. Store keyword check (Play + Amazon) for "TV browser adblock" etc.

**Go/No-Go D-1 (thresholds founder confirm karein):** ≥50 waitlist signups / 14 din **aur** ≥5/8 interviews me explicit switching intent + 3 repeated pain points **aur** ≥3 verified differentiators vs TV Bro (Section 5). **Fail path:** wedge revise → **W0-4 (1 hafta rework buffer)**; tab hi W1 shuru.

---

## 4. Product principles

1. **D-pad first** — har action Back + OK + D-pad se.
2. **Memory discipline** — WebView tab ka optional part; max 2 live; **filter-list ka in-memory set bhi RAM budget me count hoga**.
3. **Privacy as feature** — zero user data collection, opt-in analytics, **koi feature first-run par network call na kare** (default favicons bundled).
4. **Graceful degradation** — `WebViewFeature` guards; `nogms` me GMS features absent.
5. **Engine abstraction** — `BrowserEngine` interface.
6. **Verify before claim** — koi root cause / "95%" / review-SLA tab tak marketing me na jaye jab tak measure/official na ho.
7. **Honest coverage** — YouTube ads aur Google sign-in ki kami likhna lazmi.
8. **Store-policy-safe** — koi copyright-download messaging, koi DRM circumvention, koi hidden installer.

---

## 5. Competitive landscape & platform strategy

### 5.1 "Kyun switch karein?" (differentiator hypotheses — W0 me verify)

| # | Differentiator | TV Bro aaj kya karta hai |
|---|---|---|
| 1 | URL/search-first UI + 60fps cursor physics/spatial jump | Home par URL bar/search nahi, Back se menu khulta hai (Play review complaint); cursor improvements v3 me hain lekin 60fps spec nahi |
| 2 | Renderer-kill + process-death resilience + explicit RAM budget | koi documented `onRenderProcessGone` policy / 30-min LMK budget nahi |
| 3 | TV-grade native HLS player: resolution/audio/subtitle UI | "embedded optimized video player" hai — **W0 me feature-parity check zaroori** |
| 4 | Zero-telemetry + minimal-permission promise **in writing** | OSS/transparent hai, privacy positioning alag |
| 5 | Per-site rendering profiles + honest ad-block reporting | UA switch + configurable ad-block list hai → parity risk |
| 6 | Dono stores par maintained `nogms` build | Play + sideload; Amazon presence W0 verify |

**⚠️ W0 finding (already):** TV Bro **v3.0.x (Nov 2026)** me *phone companion app*, custom DNS/DoH, profiles with PIN, contextual menu, direct navigation mode, new TV-optimized ad blocker, resumable downloads, embedded video player hain. Isliye hamara phone-remote (FR-39) **killer feature nahi, parity feature** hai — isse marketing claim na banayein. **Agar ≥3 differentiators verify na hon → product redefine.**

### 5.2 Platform reality

| Channel | Flavor | Status |
|---|---|---|
| Google Play / Google TV / Android TV | `gms` | **PRIMARY.** Play TV form-factor **opt-in + alag TV quality review** (320×180 opaque banner, 16:9 actual-UI screenshots, D-pad/leanback) — W15–16 me [S4][S5][S6]. Naye personal accounts: 12 testers/14 din closed test [S2]. |
| Generic AOSP boxes / GitHub / F-Droid | `nogms` | **PRIMARY.** APK pipeline, Firebase-free build, user-triggered update check. |
| Amazon Appstore — **legacy Fire OS only** (e.g. 4K Plus 2025 `AFTMA08C15` Fire OS 8, 4K Max 2nd gen) | `nogms` | **SECONDARY.** Amazon official device specs ke mutabiq 4K Select 2025, 4K 3rd gen 2026, HD 2026 = **Vega OS 2.0**, in par Android APK nahi [S10][S11]. Amazon ne kaha hai ke wo Fire OS devices launch karta rahega, lekin naye sticks Vega par hain [S12]. Browser policy sakht: Downloader v2.0.2 (Mar 2026) ne Fire TV par browser registration hata di [S19]. |
| Amazon — **Vega OS** | — | **v1 excluded.** Linux/React Native, `.vpkg`, alag SDK, developer-mode sideload only [S8][S9][S13]. Amazon Appstore ab Fire OS (APK/AAB) aur Vega (VPKG) ke liye alag release tracks deta hai [S14]. Cloud app streaming select apps ke liye hai, lekin local network/USB/PiP unsupported [S15]. |
| ~~Roku~~ | — | Android nahi, browser allow nahi. |

**Policy:** Play in-app ad-block browsers allow karta hai (sirf apne andar) [S7], lekin **bina authorization ke copyrighted content download karwane wali streaming apps** prohibit [S3]. Google **embedded WebView me sign-in block** karta hai (`disallowed_useragent`) aur isay OAuth policy ke khilaf maanta hai [S16][S17][S18] — isliye UA se bypass nahi, honest message (FR-56).

---

## 6. Scope & priorities (canonical legend)

| Pri | Kab | Count | Kahan likha hai |
|---|---|---|---|
| **P0** | **v1 = W1–W16** | **35 FR** | `TECH_SPEC_v1.2.md` Section 1 |
| **P1** | **v1.1 = RC ke baad +6–8 hafte** | FR-09/10/16/18/19/25/27/31b/34/35/36/38/39/41b/47/51/52/53b/54 | wahi |
| **P2** | v2 / explore | FR-20/28/32/40, GeckoView gate, Vega port | wahi |

> **Legend fix:** koi bhi P1 item W1–W16 roadmap me **nahi** hai (FR-47/51/52/53b/54 ab sirf v1.1 me). Har FR ka `Phase` column `TECH_SPEC` me hai.

**Suggested cut-option (agar 16 hafte manzoor nahi):** native player (FR-23/24) + full downloader (FR-31) → v1.1; P0 me sirf `onShowCustomView` + "Open with" + basic `setDownloadListener` → ~11–12 hafte, lekin differentiator kam.

---

## 7. Success metrics (unmeasurable wali hata di gain)

| Metric | Target | Measurement — koi server-side event nahi |
|---|---|---|
| d3ward ad-block score | ≥70% (W0 baseline ke baad final) | CI + 3 manual runs |
| Total PSS (app + renderer + GPU) | ≤250MB idle **with 10 tabs** · ≤400MB 1080p HLS · ≤300MB peak on 1GB | `dumpsys meminfo` all PIDs + `gfxinfo` |
| 30-min scripted session | **0 LMK kills, 0 renderer-crash exits** | `W0/protocols.md` |
| User-perceived crash rate | **internal target ≤0.3%** | Play Console Android vitals (`gms`) |
| Penalty floor (**target nahi**) | 1.09% crash / 0.47% ANR / 8% per-device | official [S3a] |
| `nogms` crash signal | opt-in ACRA sample — **population claim nahi** | self-hosted ACRA, clearly labeled |
| First-run network calls | 0 (app code) | mitmproxy |
| **D7/D30 "retained installers"** | ≥25% at D7 | **Play Console statistics — label: app install- retention hai, USAGE nahi** |
| Usage signal (**KPI nahi**) | qualitative | opt-in in-app survey + voluntary CSV export of on-device counters; **"median sessions/week ≥3" metric HATA diya** (data device se bahar jata hi nahi) |
| Form-time (FR-41) | W0 baseline se ≥30% kam | **5 users × 3 forms usability protocol** (`W0/protocols.md`) |
| Play rating / store mix | ≥4.2 after 500 installs | Store consoles |

---

## 8. Roadmap (rebalanced; W0 ke baad final)

**W0 = 3 hafte** (W0-1 procurement + official sources · W0-2 technical spike · W0-3 demand + teardown + policy) + **optional W0-4 = D-1 fail rework buffer**.

| Phase | Hafte | P0 load | Deliverable / gate |
|---|---|---|---|
| Core shell | W1–2 | 8 | FR-01/07/08/37/43/48/53-structure/56 · D-pad page load + renderer-crash survive |
| Input | W3–4 | 7 | FR-02/03/04/05/06/41/46 · 60fps + double-cursor zero |
| Tabs / data / compat | W5–7 | 11 | FR-11…15, 13 nav-history, 29/30/33/49/50 · 10 tabs @ ≤250MB, 0 kills/30min |
| Ad-block | W8–9 | 5 | FR-17/21/44/45/55 · d3ward ≥70%, license NOTICE |
| Media | W10–12 | 4 | FR-22/23/24/26 · HLS + subtitles on dono flavors |
| Downloads + policy copy | W13–14 | 1 + reviews | FR-31, in-app progress, A-21 store-copy pre-review, FGS declaration video |
| Compat / flavors / stores | W15–16 | — | `nogms` build, Play TV form-factor opt-in + TV assets, Amazon submission, **closed test launch (12 TV-owning testers)**, D-pad/TalkBack audit, buffer |
| Closed test → apply | W17–18 | — | 14-day clock complete → production access application |
| **External review buffer** | **W19–20** | — | Play production + TV quality review, Amazon review. **Koi fixed "17–21 din" SLA claim nahi** — official number verify (A-28). |
| v1.1 | +6–8 | P1 | FR-09/10/16/18/19/25/27/31b/34/35/36/38/39/41b/47/51/52/53b/54 |
| v2 | explore | P2 | FR-20/28/32/40, GeckoView gate, Vega decision |

> **Load imbalance fix:** W1–3 ke 15 items ab W1–2 (8) + W3–4 (7) me; tab/renderer/persistence ka heavy kaam W5–7 me. **W0-4 buffer** D-1 fail path ke liye rakha gaya hai.

---

## 9. Cost & monetization

| Item | Cost |
|---|---|
| Play account $25 · Amazon free · domain/static ~$10–15/yr | ~$35–40/yr first year |
| **Test devices ≈$400–600 one-time** | Google TV Streamer (4GB, $149.99 [S20]) ya used Chromecast 2GB; Fire OS 4K Plus/4K Max exact model verify; 1GB AOSP box; Shield/borrowed TVs; optional Vega unit. `W0/procurement-and-sources.md` |
| Play closed testing | $0 community recruitment (15–20 TV-owning testers); paid services risky |
| Paid APIs / crash reporting | $0 (Play vitals + self-hosted ACRA) |

**v1 = 100% free, koi IAP/Pro/ads/data-sale nahi.** Pro (v2) ka matlab 3 alag billing raaste + network verification + privacy conflict → alag go/no-go decision (Section 11). Donation link sirf About screen.

**Maintenance:** bundled filter refresh har release, WebView fragmentation, per-site quirks, Play/Amazon policy, target-API annual bump, Vega installed-base monitoring.

---

## 10. Risks (top)

| Risk | Mitigation |
|---|---|
| **Demand ≠ evidence** | D-1 + W0-4 buffer |
| **TV Bro parity (v3 me phone companion, DoH, profiles, player)** | 6 differentiators W0 me verify; ≥3 na niklein to redefine |
| **Fire wedge shrink (Vega + Amazon policy)** | **Primary wedge pehle hi Google TV + AOSP kar diya**; Fire OS secondary |
| **Google OAuth policy / `disallowed_useragent`** | FR-56 honest message; koi UA bypass nahi; default desktop UA sirf layout ke liye |
| **Play/Amazon piracy-adjacent rejection** | FR-25/31 guardrails, persona reframe, W13–14 copy pre-review, A-21 |
| **Play TV quality review rejection** (banner/D-pad/leanback/screenshots) | W15–16 dedicated checklist [S4][S5][S6] |
| **Ad-block license (aggregated sources mixed, kuch non-commercial)** | A-17: permissive-only curated subset + official StevenBlack README [S21]; v2 Pro se pehle re-check |
| **12 TV testers recruit karna** | W0-1 se community recruitment, 15–20 buffer; paid services se parhez |
| **YouTube ad expectation** | FR-55 honest copy; DNS/hosts isay rok nahi sakta [S22] |
| Safe Browsing default OFF = privacy↔security tradeoff | **Conscious decision → Section 11 (D-6)** |
| Typing cluster v1 me partial | FR-41 snippets/shortcuts P0; custom IME + phone remote P1 |
| Scope / 35 P0 / unverified roots | W0 freeze + `W0_Validation_Plan` assumptions register |

---

## 11. Open decisions (founder)

1. Naam/brand · 2. **Wedge confirm: Google TV + AOSP primary (recommended)** · 3. v1 free (recommended) · 4. 16 hafte vs cut-option · 5. minSdk 23→26 · 6. targetSdk 36 Play Console me confirm · 7. Play account personal vs organization (closed-test exemption) · 8. Vega port v2 me ya skip · 9. Stream sniffer/download-video scope (policy) · 10. Filter list = permissive-only curated subset + d3ward benchmark · 11. FR-39 phone remote P1 (parity feature) · 12. Device budget ~$400–600 · 13. **Safe Browsing default OFF (privacy) vs ON-with-disclosure (security)** — conscious tradeoff, W0-2 ke A-18 result ke baad final.

---

## 12. Document map & next step

| File | Kya hai |
|---|---|
| `PRD_v1.2.md` | Product: problem, goals, demand, competitors, wedge, metrics, roadmap, cost, risks, decisions |
| `TECH_SPEC_v1.2.md` | 35 P0 + P1/P2 FR tables, NFR, architecture/ADR, **permissions allow-list**, security, traceability |
| `W0_Validation_Plan_v1.2.md` | W0 3-week schedule, assumptions register, device matrix, official-source checklist, D-1 |
| `W0/validation-results.md` | **W0 ka master fill-in artifact** — isi me results likhe jayenge |
| `W0/protocols.md` | BT sleep, d3ward, 30-min LMK, renderer crash, first-run capture, 5×3 form usability |
| `W0/procurement-and-sources.md` | Exact device models/OS + official source verification checklist |
| `W0/demand-and-competitor.md` | Interview guide, waitlist/D-1 scorecard, TV Bro v3 teardown template |
| `CHANGELOG_v1.2.md` | Is review ka item-by-item fix map |

**Ab agla qadam:** `W0/validation-results.md` kholein aur W0-1 (procurement + official sources) shuru karein. **v1.3 W0 results se banegi — mazeed desk-review nahi.**

---

### Key official sources (poori list `W0/procurement-and-sources.md`)

[S1] Play target API: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en · [S2] Play closed testing: https://support.google.com/googleplay/android-developer/answer/14151465?hl=en · [S3] Play policy (IP/FGS/etc.): https://support.google.com/googleplay/android-developer/answer/17517561 · [S3a] Android vitals: https://support.google.com/googleplay/android-developer/answer/9844486?hl=en · [S4] TV form-factor opt-in: https://support.google.com/googleplay/android-developer/answer/13295490?hl=en · [S5] TV quality guidelines: https://support.google.com/googleplay/android-developer/answer/18058043 · [S8]–[S15] Amazon Vega/Fire OS/app-submission docs · [S16]–[S18] Google Identity OAuth/embedded-WebView policy · [S21] StevenBlack README · [S22] AdGuard official (YouTube ads) · [S20] Google Store Streamer.
