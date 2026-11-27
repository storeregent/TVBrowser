# TVBrowse — Market, Competitor & Platform Policy Audit

| Field | Value |
|---|---|
| Role | Market, Competitor & Platform Policy Research Analyst |
| Stage | W0 (validation; audit only, no code) |
| Branch | `arena/f98e882b-tvbrowser` (base `1660e6a`) |
| Research date / access date for all sources | **2026-11-27** (UTC) |
| Inputs reviewed (read-only) | `AGENTS.md`, `docs/product/PRD_v1.2.md`, `docs/technical/TECH_SPEC_v1.2.md`, `docs/validation/W0_Validation_Plan_v1.2.md`, `docs/validation/validation-results.md`, `docs/research/demand-and-competitor.md`, `docs/research/procurement-and-sources.md`, `reference/browsehere/*.md`, `docs/history/CHANGELOG_v1.2.md` |
| Method | Desk research only. Primary sources = official policy/developer docs, official store listings, the competitor's own repository/website. Secondary = third-party sites (used only for corroboration and labelled **(S)**). No device testing was performed; anything that needs a device is **NOT TESTED**. |

**Status vocabulary (AGENTS.md):** PASS · FAIL · BLOCKED · NOT TESTED · UNVERIFIED. In addition, facts are labelled **VERIFIED (P)** = confirmed from a primary source, **VERIFIED (S)** = only secondary sources, **UNKNOWN** = no reliable information found, **OPINION** = user reviews / analyst judgement (never treated as technical fact).

---

## 0. Executive summary

1. **Differentiation gate is currently not met at desk level.** Of the 6 PRD §5.1 differentiators, **0 are verified as distinct today**. Three (#3 native player, #5 per-site/ad-block, FR-39 phone remote) are **parity** with TV Bro based on its own source code and release notes. Three (#1, #2, #4) are only distinguishable through measurement or verifiable privacy proof (**NOT TESTED**). #6 (Amazon distribution) is **BLOCKED** by Amazon's published browser restriction. PRD rule "≥3 differentiators or redefine" therefore cannot pass on desk evidence alone → founder decision after W0-2 device tests.
2. **TV Bro moved fast in Nov 2026.** v3.0.5 (13 Nov), v3.0.6 (18 Nov), v3.1.0 (26 Nov) added: redesign, separate GeckoView app, TV ad blocker, resumable downloads, embedded Media3 player with HLS/DASH download, HTTP proxy, "Send to this TV" (LAN + cloud inbox), DoH, PIN-protected kids profile, cursor modes. TV Bro already handles `onRenderProcessGone` in code, which contradicts the PRD §5.1 #2 wording.
3. **Amazon Fire TV is the highest policy risk.** Amazon officially "restricts the publication of third-party browser apps on Fire tablets and Fire TV devices" (case-by-case via Developer Support). Downloader was suspended in March 2026 after registering as a browser and restored only after removing browser registration (v2.0.2). New 2026 Fire TV sticks run **Vega OS 2.0** (cannot run APKs). TV Bro is not on the Amazon Appstore (secondary sources only); BrowseHere was not found in an amazon.com Appstore search.
4. **Google Play TV review has requirements that directly hit a browser:** full 5-way D-pad navigation "without requiring a virtual mouse", **no media playback in PiP on TV**, video must pause when leaving the app, AAB mandatory, 64-bit + 16 KB page size from 1 Aug 2026, and a hard memory ceiling of **280 MB total** on 1 GB low-RAM TVs. Two TECH_SPEC items conflict with these (FR-16 PiP/background video; see §5 and §9).
5. **Target API claim (validation-results §C) is VERIFIED:** mobile apps must target API 36 from 31 Aug 2026; **TV apps must target ≥34** (new) / ≥33 (existing). TECH_SPEC NFR-07 `targetSdk 36` satisfies both.
6. **Android developer verification** (enforced from 30 Sep 2026 in select regions, global 2027) applies to apps installed from *participating stores* and, for non-Play distribution, is currently enforced **only on mobile/tablet form factors**. Sideloaded TV APKs are not yet affected, but the `nogms` channel and FR-52 sideload updates must plan for 2027 registration of package names and signing keys.
7. **Evidence quality of existing market inputs is low:** the demand/teardown template is empty, the complaints document has no source URLs, the BrowseHere reverse-engineering report analysed a ~2014-era build (v8.0.770, target 23) that is not representative of the current Play build (8.25.x).

---

## 1. Competitor comparison

All figures from the official Google Play listings on 2026-11-27 unless noted. Store figures are bucketed and include all form factors; they show category demand, **not** TVBrowse demand.

| Attribute | **TV Bro** (`com.phlox.tvwebbrowser`) | **BrowseHere** (`com.tcl.browser`) | **Puffin TV** (`com.cloudmosa.puffinTV`) | **Downloader** (AFTVnews) | **TV Akhi** (S29c) | **Amazon Silk** |
|---|---|---|---|---|---|---|
| Developer | Fedir Tsapana / phlox.dev (Zhytomyr, UA) — VERIFIED (P) | TCL; legal entity 深圳市雷鸟网络传媒有限公司 — VERIFIED (P) | CloudMosa Inc — VERIFIED (P) | Elias Saba (AFTVnews) — VERIFIED (P) | GitHub fork `halgurdh/tv-bro-no-ads` | Amazon |
| Play installs | 5M+ — VERIFIED (P) | 50M+ — VERIFIED (P) | 10M+ — VERIFIED (P) | UNKNOWN (not checked on Play this session) | Not on Play — UNKNOWN | N/A (preinstalled on Fire TV) |
| Play rating (all / TV) | 3.6★ (9.74K) / TV 3.7★ (2.39K) | 4.6★ (313K) / TV 4.4★ (31.1K) | 1.9★ (5.93K) / TV 3.1★ (5.8K) | UNKNOWN | — | — |
| Last updated (Play) | 20 Nov 2026 (GitHub latest v3.1.0, 26 Nov 2026) | 27 Nov 2026 | 24 Nov 2026 | Fire v2.0.2 (Mar 2026) per AFTVnews | Repo pushed 28 Oct 2026, 0 stars | UNKNOWN |
| Price model | Free; Qwant partner search in some regions (P, repo) | Free, no IAP; promoted "free movies & anime" content, personalised recommendations (P, listing) | **Subscription required** ($1/mo, $0.25/wk, $0.05/day) (P) | Free | Free | Free |
| Engine | System WebView; separate GeckoView app `.gecko` (minSdk 26) (P) | System WebView (RE doc, old build; current build UNVERIFIED) | Cloud-rendered (servers US + Singapore) (P) | WebView-based built-in browser (S) | WebView (fork) | Chromium-based (UNVERIFIED) |
| Native video player | Yes — Media3/ExoPlayer, HLS/DASH, stream download, PiP on generic flavor (P, code) | Yes — "web video player" with subtitles, IPTV playlist player (P, listing) | "Optimized video playback", playback speed (P) | UNKNOWN | Inherited from TV Bro (UNVERIFIED) | UNKNOWN |
| Ad blocking | Own ABP-syntax engine + cosmetic filtering, popup blocking (P, code + notes) | "Ad blocker (pop-up, video, banner)" (P, listing); effectiveness UNVERIFIED | Not advertised | No | Claimed uBlock Origin (S29c description; **UNVERIFIED**) | UNKNOWN |
| Phone / remote input | Same APK has phone UI; "Send to this TV" via LAN HTTP server + Firebase cloud inbox (P, code) | QR web-page phone input, "only specific TV models" (P, listing) | Separate "Puffin TV Remote" app + send link (P) | — | — | — |
| Privacy declaration (Play Data safety) | Collects crash logs + app interactions; shares none; deletion not available (P) | "No data collected"; deletion available (P) | "No data collected", encrypted in transit (P) | UNKNOWN | — | — |
| Amazon Appstore | Not listed (S only); sideloaded via Downloader code 627360 (S) | Not found in amazon.com Appstore search (P, weak: search result only); Play listing says Fire TV is supported (sideload) | UNKNOWN (not checked) | Listed (restored after v2.0.2) (P) | No | Preinstalled |
| Licence / openness | Source-available, custom BSD-like licence with naming/branding clauses (P) | Proprietary | Proprietary | Proprietary | Fork of TV Bro (licence inherited; NOASSERTION in GitHub API) | Proprietary |

**Strengths and weaknesses (verifiable only).**

| Competitor | Verifiable strengths | Verifiable weaknesses / openings |
|---|---|---|
| TV Bro | Very active (3 releases in Nov 2026); feature-rich; open source; three build flavors (generic/google/foss); targetSdk 37; already handles renderer crashes; D-pad + cursor; GeckoView option | Privacy inconsistency: `PRIVACY.md` (10 Nov 2026) says no personal data/analytics, but Play Data safety declares crash logs + app interactions, and the new cloud inbox writes TV name + LAN IP:port + key to Firebase Realtime DB without mention in `PRIVACY.md`. Broad manifest: `QUERY_ALL_PACKAGES`, `REQUEST_INSTALL_PACKAGES`, `ACCESS_COARSE_LOCATION`, `allowBackup=true`, `usesCleartextTraffic=true` (whether Play build strips some = UNVERIFIED). LAN send server is plain HTTP. Not on Amazon Appstore. Mid ratings (3.6★). OPINION: recent reviews complain about redesign/popups. |
| BrowseHere | Scale (50M+, 4.6★, 313K reviews); TCL pre-install relationship (UNVERIFIED); IPTV + subtitles; free | Content-promotion / recommendation surface ("free movies & anime", "You May Interest") vs "No data collected" declaration is internally inconsistent (UNVERIFIED which is accurate); phone input limited to specific TCL models; legacy permission set in old builds (RE doc); proprietary, China-based entity (some users may care — OPINION). |
| Puffin TV | Cloud rendering works on weak hardware (claim by vendor) | Paid subscription mandatory; 1.9★; geo-restrictions from US/SG servers; blocked in some regions (vendor-stated). |
| Downloader | Fire TV distribution + brand trust | Not a full browser on Fire after v2.0.2 (cannot register as browser). |

---

## 2. BrowseHere findings

| # | Finding | Status | Source |
|---|---|---|---|
| B1 | Package `com.tcl.browser`, publisher TCL; legal entity 深圳市雷鸟网络传媒有限公司 | VERIFIED (P) | Play listing |
| B2 | 50M+ installs, 4.6★ (313K), TV rating 4.4★ (31.1K), updated 27 Nov 2026, free, no IAP | VERIFIED (P) | Play listing |
| B3 | Advertised: web video player + subtitles, voice search, IPTV player (user playlists), ad blocker (pop-up/video/banner), downloads incl. APK + short codes, "free movies & anime" home content, trending searches, personalised "You May Interest", phone input via QR web page (specific TV models only), zoom, scroll mode, Fire TV listed as supported | VERIFIED (P) as *advertised claims*; actual behaviour NOT TESTED | Play listing |
| B4 | Data safety: "No data collected", deletion request available | VERIFIED (P) as declaration; accuracy UNVERIFIED | Play Data safety page |
| B5 | Personalised recommendations vs "no data collected" | UNVERIFIED (possible inconsistency; could be on-device). Flag only. | Analyst inference |
| B6 | Current versions 8.25.012 (30 Oct 2026; Android 7.0+/10+/12L+ variants) and 8.20.060 (~22 Nov 2026) | VERIFIED (S) | APKMirror, updatestar, soft112 |
| B7 | RE doc analysed v8.0.770 (minSdk 14 / target 23, SHA1withRSA cert 2014, http endpoints, legacy permissions, no ad-block lib/ExoPlayer found) | Out of date. Findings **must not** be generalised to current builds (8.2x has Android 7.0+ minimum and advertises an ad blocker). | `reference/browsehere/BrowseHere_Reverse_Engineering.md` vs B6 |
| B8 | Amazon Appstore presence | Not found in amazon.com "Appstore for Android" search for "browsehere" on 2026-11-27 → likely not listed in US; other marketplaces UNKNOWN | amazon.com search |
| B9 | Pre-installed on TCL TVs (explains install base) | UNVERIFIED | — |
| B10 | Complaint pillars (28 complaints) in `Users_Complaints_...md` | Low evidence quality: no URLs, no dates, no counts → treat as hypotheses (OPINION) until re-mined with links | Repo doc |
| B11 | Privacy policy `tcl.com/us/en/terms-privacy`, support `browse.here@tcl.com`, Telegram channel | VERIFIED (P) | Play listing |

**Implication:** BrowseHere is the volume leader and is free with an ad blocker and native player, so "ad-block + native player" is table stakes, not a differentiator. BrowseHere's visible weakness is the content-promotion surface and opaque data practices — an opening only if TVBrowse can *prove* its privacy claims (see #4).

---

## 3. TV Bro findings

### 3.1 Release timeline (GitHub releases, VERIFIED (P))

| Version | Date | Key items |
|---|---|---|
| v2.0.1 | Feb 2024 | — then ~2-year gap |
| v2.1.4 | Apr 2026 | Context menu, direct navigation, configurable cursor speed, dark mode, ad-block list, remote debugging |
| v3.0.5 | 13 Nov 2026 | Redesign; separate GeckoView app (`.gecko`, minSdk 26); TV ad blocker; resumable downloads; bookmark import/export + folders; embedded player; HTTP proxy; Qwant default in some regions |
| v3.0.6 | 18 Nov 2026 | Default UA update, cursor behaviour, ad-block popup/window blocking; `send/` package (Send to this TV) first appears in tree |
| v3.1.0 | 26 Nov 2026 | Kids profile with PIN; DoH (Cloudflare, Google, AdGuard, Quad9 via DoT, DNS.SB, custom); per-profile clear data; mouse/finger cursor. Incognito + kids profile need WebView ≥119 |

`latest_version.json`: v3.1.0, versionCode 86, **minAPI 24** (Gecko-included variant minAPI 26). `gradle/libs.versions.toml`: compileSdk 37, **targetSdk 37**, minSdk 24.

### 3.2 Code-level facts (repo `truefedex/tv-bro`, VERIFIED (P))

- Media3/ExoPlayer native player; HLS/DASH stream downloads; Android TV home channels (`WRITE_EPG_DATA`).
- `WebViewClient.onRenderProcessGone` is implemented (returns `true`); `onTrimMemory` present.
- Own ABP-syntax ad blocker with cosmetic filtering; site filter; UA toggle.
- Safe Browsing always on.
- Phone touch UI (PhoneShell) shipped in the **same** APK.
- Flavors: `generic` (built-in auto-update, PiP on), `google` (auto-update off with code comment "violates Google Play policies"; PiP off with comment that TV quality guidelines disallow media playback in PiP), `foss` (`.foss` suffix; F-Droid listing UNKNOWN).
- Main manifest permissions: POST_NOTIFICATIONS, RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, WAKE_LOCK, ACCESS_COARSE_LOCATION, CAMERA, READ_EXTERNAL_STORAGE (max 32), WRITE_EXTERNAL_STORAGE (max 29), REQUEST_INSTALL_PACKAGES, FOREGROUND_SERVICE + _DATA_SYNC, QUERY_ALL_PACKAGES, WRITE_EPG_DATA, INTERNET, ACCESS_NETWORK_STATE, ACCESS_LOCAL_NETWORK. Flags: `allowBackup=true`, `usesCleartextTraffic=true`, LEANBACK_LAUNCHER + banner. No flavor-specific manifest found → whether the Play (`google`) build removes REQUEST_INSTALL_PACKAGES / QUERY_ALL_PACKAGES is **UNVERIFIED** (needs Play APK).
- "Send to this TV": `LocalSendServer` = plain-HTTP LAN server (key in URL fragment) accepting text/files; `CloudInbox` = Firebase Realtime DB REST (`tv-bro-3546c.firebaseio.com`), TV publishes name, LAN ip:port, key; anyone with the inbox code can write.
- Search: Qwant partner (client ID kept out of repo); region detected via SIM/timezone/locale; suggestions from Google by default.

### 3.3 Privacy, licence, distribution

| Item | Finding | Status |
|---|---|---|
| `PRIVACY.md` (updated 2026-11-10) | Claims no personal data, no analytics (notes older builds had Crashlytics); **does not mention the Firebase cloud inbox** | VERIFIED (P) |
| Play Data safety | Collects crash logs + app interactions (analytics); no sharing; deletion not available; privacy link points to a Google Doc | VERIFIED (P) — **conflicts with PRIVACY.md** |
| Licence | Custom BSD-like © 2019 Fedir Tsapana. Modified binaries must not contain "TV Bro" in name (any case), must not reuse icon or app ID, must show About window crediting TV Bro with link | VERIFIED (P). Relevance to TVBrowse only if any TV Bro code is reused → legal/founder question |
| Amazon Appstore | Not present | VERIFIED (S) only (downloadercodes.com, simturax.com, daily.dev) — not officially confirmed |
| Play listing | "Version for phones, with tabs and bookmarks exchange"; Everyone / Unrestricted Internet | VERIFIED (P) |

### 3.4 PRD claims about TV Bro — check

| PRD statement (§5.1 / W0 finding) | Result |
|---|---|
| "v3.0.x (Nov 2026) has phone companion app" | **PARTIALLY VERIFIED.** No separate companion app; same APK with phone UI + Send-to-TV (QR/link, LAN + cloud) from v3.0.6. |
| "custom DNS/DoH, profiles with PIN" in v3.0.x | **Attribution incorrect**: first documented in **v3.1.0** release notes (26 Nov). |
| "contextual menu, direct navigation mode" in v3.0.x | **Attribution incorrect**: documented in **v2.1.4** (Apr 2026). |
| "new TV-optimised ad blocker, resumable downloads, embedded video player" | VERIFIED (v3.0.5). |
| #2 "no documented `onRenderProcessGone` policy" | **Contradicted at code level** — handler exists. A *documented* user-facing policy/RAM budget: none found (UNVERIFIED either way). |
| #1 "Home has no URL bar/search; Back opens menu (Play review complaint)" | Source is a user review (OPINION) and pre-dates the v3.0.5 redesign → **UNVERIFIED** for current version; needs device check. |
| S29 "TV Bro minAPI 26" | **Partially incorrect**: minAPI 24 (WebView build), 26 only for Gecko variant. |

---

## 4. Differentiator validation (PRD §5.1)

Criteria: *distinct* = TVBrowse would offer something that the three main competitors verifiably do not, and that can be demonstrated to a user/reviewer.

| # | Differentiator | Competitor evidence | Distinct? | Status | What would make it PASS |
|---|---|---|---|---|---|
| 1 | URL/search-first UI + 60 fps cursor physics/spatial jump | TV Bro has cursor speed config (v2.1.4), cursor modes (v3.1.0), redesign (v3.0.5). 60 fps / spatial jump not measured for anyone. | Unproven | **NOT TESTED** | Side-by-side device test (frame timing, task-time to URL entry) vs TV Bro v3.1.0 and BrowseHere 8.25 on same device |
| 2 | Renderer-kill + process-death resilience + explicit RAM budget | TV Bro implements `onRenderProcessGone` + `onTrimMemory`. Google sets a public memory ceiling (TV-ME: 280 MB total on 1 GB low-RAM devices) that every Play TV app must meet anyway. | Weak — only measured outcome (state survives, lower PSS) can differ | **NOT TESTED** | W0-2 protocols: renderer-crash recovery, 30-min LMK, PSS on 1 GB device vs TV Bro |
| 3 | TV-grade native HLS player (resolution/audio/subtitle UI) | TV Bro: Media3 with HLS/DASH + downloads + PiP. BrowseHere: player with subtitles + IPTV. Track-selection UI quality UNVERIFIED for both. | **No (parity)** | **FAIL** (as a differentiator) | Only if device test shows clearly superior track UI — would still be weak marketing |
| 4 | Zero-telemetry + minimal-permission promise in writing | "No data collected" is already declared by BrowseHere and Puffin; TV Bro claims it in `PRIVACY.md`. But TV Bro's manifest is verifiably broad (QUERY_ALL_PACKAGES, REQUEST_INSTALL_PACKAGES, location, cleartext, backup) and its Data safety declares analytics. | Partially — the *written promise* is not distinct; a **verifiable** minimal-permission manifest + reproducible/F-Droid build + published traffic capture would be | **UNVERIFIED** | Publish manifest diff, mitmproxy first-run capture (NFR-11), F-Droid reproducible build; keep Data safety and policy consistent |
| 5 | Per-site rendering profiles + honest ad-block reporting | TV Bro: UA toggle, site filter, own ABP+cosmetic blocker, per-profile data; BrowseHere: ad blocker. "Honest reporting" (d3ward score) not measured for anyone. | **No (parity risk)** for profiles; reporting unproven | **FAIL** (profiles) / **NOT TESTED** (reporting) | d3ward benchmark + in-app transparent counter; still easy to copy |
| 6 | Maintained `nogms` build on both stores | TV Bro: generic APK + `foss` flavor (F-Droid UNKNOWN), not on Amazon. Amazon restricts third-party browsers (official). | Would be distinct **only if** Amazon approves | **BLOCKED** | Written pre-clearance from Amazon Developer Support (see §6) |
| (FR-39) | Phone web-remote | TV Bro Send-to-TV + phone UI; BrowseHere QR phone input; Puffin remote app | No (parity) — PRD already says so | **FAIL** as differentiator (consistent with PRD) | — |

**Result:** verified distinct = **0/6**; potential with evidence = #2, #4, #6 (and #1 if measurable). The PRD threshold (≥3) is **not met on desk evidence** → depends on W0-2 device tests and an Amazon response. This is reported, not a scope change; decision belongs to the founder (§11).

---

## 5. Google Play policy risks

| ID | Risk | Official requirement (quoted/paraphrased) | Impact on TVBrowse docs | Severity | Status |
|---|---|---|---|---|---|
| G1 | Self-update outside Play | Device & Network Abuse: an app distributed via Play "may not modify, replace, or update itself using any method other than Google Play's update mechanism" | FR-52 must stay `nogms`-only (TECH_SPEC already says so). TV Bro disables auto-update in `google` flavor for this reason. | High if mixed | Aligned (PASS on paper) |
| G2 | `REQUEST_INSTALL_PACKAGES` (A-09) | Restricted permission. Core functionality must include sending/receiving packages **and** user-initiated installation. **"Web browsing or search" is a listed permitted use**; requires Permissions Declaration Form and Prominent Disclosure; invalid if a less intrusive method exists | TECH_SPEC prohibits it on `gms` — conservative and compliant. If FR-32 ever moves to `gms`, declaration + disclosure required. | Medium | A-09: answer = *permitted category exists but declaration-gated*; current plan PASS |
| G3 | Ad blocking | Device & Network Abuse lists "Apps that block or interfere with another app displaying ads" as a violation | Blocking inside TVBrowse's own WebView is not "another app" — PRD claim [S7] is consistent with the text but **S7 itself was not re-fetched** → inference only. Never market system-wide blocking. | Medium | UNVERIFIED (inference) |
| G4 | JavaScript interface + untrusted content | Violation example: "webview with added JavaScript Interface that loads untrusted web content (for example, http:// URL)" | A browser loads untrusted content by definition; any `addJavascriptInterface` must be scoped to own assets (TECH_SPEC §security says "JS bridge only own…"). Must be enforced and tested. | High | NOT TESTED |
| G5 | TV D-pad navigation | Play multi-platform guidelines (TV-DP): all menus/buttons/settings "must be fully navigable … using standard 5-way D-pad … **without requiring a virtual mouse** or touchscreen" | Browser chrome must be D-pad navigable; cursor mode only for page content. TV Bro and BrowseHere pass review, so achievable. | High | NOT TESTED |
| G6 | **PiP media playback on TV** | Multitasking on TV: "Usage types that don't fall into [communication, smartHome, health, ticker], **in particular any playback of media content, are not allowed in picture-in-picture mode on TV**." TV-IC/IE/IX criteria | **Conflict** with TECH_SPEC FR-16 ("PiP bonus"), D-05, D-11 and permissions table row for FGS_MEDIA_PLAYBACK ("MediaSession/PiP"). TV Bro disables PiP in its Play flavor for this reason. | High | FAIL (doc conflict) |
| G7 | Background video | TV-NP: background audio needs Now Playing controls; "**Video apps must not use these media controls, and video must be paused when the user switches out of the app**" | FR-16 MediaSession + FGS mediaPlayback is valid for audio-only continuation; background *video* would fail TV review. | High | UNVERIFIED (depends on FR-16 intent) |
| G8 | Foreground service declarations (A-31) | Apps targeting 14+ must declare FGS types in manifest **and** Play Console with description, user impact and **demo video**; UIDT recommended for user-initiated transfers | FR-31 (UIDT primary, dataSync fallback) aligns. A-31 exact form questions: not visible without Console → UNVERIFIED. | Medium | Partially VERIFIED (P) |
| G9 | Target API (A-11) | New apps/updates: target **API 36** from 31 Aug 2026 (extension to 1 Nov 2026) for mobile; **Android TV: target ≥34** (existing TV apps ≥33) | NFR-07 targetSdk 36 → compliant. | Low | **PASS** (claim verified) |
| G10 | TV quality technical bar | TV-G1 AAB mandatory; **TV-G6 from 1 Aug 2026: 32- and 64-bit + 16 KB page size**; TV-PS minSdk ≤31; TV-ML leanback launcher; TV-MT touchscreen etc. `required=false` (also camera, microphone, GPS, telephony, NFC); TV-LB 320×180 opaque banner with app name + ≥512 px icon; TV-G4 ≥1 unaltered 16:9 screenshot of actual TV UI | minSdk 26 OK. Any bundled native libs (e.g., Media3 extensions, GeckoView if ever used) must be 16 KB aligned. Microphone/camera `required=false` is already in TECH_SPEC. | Medium | VERIFIED (P); compliance NOT TESTED |
| G11 | Memory ceiling on low-RAM TVs | TV-ME: on `isLowRamDevice()` TVs, total (Anon+Swap + Graphics + File) **must not exceed 280 MB** for 1 GB devices; strongly recommended ≤200 MB Anon+Swap+Graphics; media buffers 40–60 MB @1 GB/1080p | Very demanding for a WebView browser + Media3. Becomes a review criterion, not just a differentiator (#2). | High | NOT TESTED |
| G12 | Store-listing accuracy | TV-G3 / expected performance: every advertised feature must work on TV; listing screenshots must match | Store copy must not promise features gated on P1/P2 FRs. | Medium | — |
| G13 | Closed testing (A-27) | Personal accounts created after 13 Nov 2023: closed test with **≥12 testers opted in continuously for 14 days** before production access | Verified for personal accounts. That organisation accounts are exempt is implied by scope ("personal developer accounts") but not explicitly stated on the page fetched → UNVERIFIED. | Medium | Partially VERIFIED (P) |
| G14 | Data safety accuracy | All collection incl. SDKs must be declared; Google may enforce on discrepancy. **Open-web navigation in a WebView need not be declared**; on-device-only and E2E-encrypted data are out of scope | `gms` opt-in Crashlytics (TECH_SPEC §data) must be declared as *optional* crash-log collection. TV Bro's PRIVACY.md vs Data safety mismatch is a cautionary example. | Medium | VERIFIED (P) |
| G15 | Google sign-in in WebView (A-07) | Google Identity best practices: "Do not redirect the request through embedded browsing environments, including webviews on mobile platforms, such as WebView on Android" | FR-56 honest message and "no UA bypass" are aligned. Exact `disallowed_useragent` error page [S16–S18] not re-fetched. | Medium | Partially VERIFIED (P) |
| G16 | Piracy-adjacent features (A-21) | IP / streaming-download policy [S3] **not fetched this session** | FR-25 stream sniffer and FR-31 downloader copy remain a review risk. | High | UNVERIFIED |
| G17 | Review SLA (A-28) | No official review-time SLA found | Keep buffer (PRD already removed "17–21 days"). | Low | UNKNOWN |
| G18 | TV install-retention metric (A-29) | Not checked (requires Console) | — | Low | UNKNOWN |

---

## 6. Amazon / Fire TV policy risks

| ID | Risk | Evidence | Severity | Status |
|---|---|---|---|---|
| A1 | **Third-party browser restriction** | Amazon Device Filtering doc: "For privacy and security reasons, Amazon restricts the publication of third-party browser apps on Fire tablets and Fire TV devices. In limited cases, we may permit third-party browser apps if they meet applicable privacy and security requirements. Contact Developer Support." | **Critical** for PRD §5.2 Fire OS secondary channel and differentiator #6 | VERIFIED (P) → #6 **BLOCKED** |
| A2 | Browser intent registration | Downloader v2.0 (5 Mar 2026) registered as a browser → suspended 28 Mar; v2.0.2 (30 Mar) removed browser registration on Fire and was restored. A built-in browser was tolerated; *declaring as a browser* was not (as reported by the developer). (A-20) | High | VERIFIED (P — AFTVnews is the Downloader developer's own site); Amazon's internal reasoning UNVERIFIED |
| A3 | Vega OS on new devices (A-25) | Official spec: **Fire TV Stick 4K – 3rd Gen (2026), `AFTCR001`, Vega OS 2.0, 1 GB RAM, 32-bit ABI**. Vega cannot run APKs; Appstore has separate Fire OS (APK/AAB) and Vega (VPKG) release tracks. | High (addressable Fire base shrinking) | VERIFIED (P) for 4K 3rd Gen; HD (2026) and 4K Select (2025) on Vega per PRD [S11] — not re-checked this session |
| A4 | Fire OS still developed | Fire OS overview lists **Fire OS 16** (Android 16/15, API 36/35) and Fire OS 14 (API 31–34) | Mitigating | VERIFIED (P). Which current devices run Fire OS 16: UNKNOWN |
| A5 | Fire TV test criteria | Official test criteria: 1080p design, readable at ~10 ft, simple D-pad UI, **limited text entry**, Back must not force-close, app should fill ≥90 % (≥80 % centred) of screen, ≤4 GB install | Medium — a browser is text-entry heavy by nature | VERIFIED (P via search excerpt of official page; full page not fetched) |
| A6 | Manifest / device targeting | Must declare `android.hardware.touchscreen required=false`; devices must still be selected manually; multiple APKs per listing allowed (same package name, unique versionCodes); remove `uses-amzn-sdk` | Low | VERIFIED (P) |
| A7 | No Google services | Fire OS replaces Google services (FCM → A3L/ADM, Sign-In with Google → A3L/Login with Amazon, etc.) | Safe Browsing (GMS) unavailable on Fire; Google OAuth in WebView blocked anyway | VERIFIED (P) |
| A8 | Fire-specific developer verification | Not applicable to Amazon Appstore as far as found; which stores are "participating" is defined in a Google blog post not fetched | Low | UNKNOWN |

**Net:** without written Amazon pre-clearance, an Amazon Appstore listing for a general-purpose browser should be treated as **unlikely**; a sideload-only presence on Fire OS (like TV Bro) is possible but is not "on the store".

---

## 7. Privacy & distribution concerns

1. **Declaration consistency.** Play Data safety, in-app disclosure and the written privacy policy must match (Google can enforce discrepancies). TV Bro is a live example of a mismatch. TVBrowse's "zero telemetry" claim (#4) is only credible if verifiable (NFR-11 traffic capture published, reproducible builds).
2. **Crash reporting.** `gms`: Play vitals + opt-in Crashlytics → declare crash logs as optional. `nogms`: opt-in self-hosted ACRA → not on Play; F-Droid anti-features policy not checked (UNVERIFIED).
3. **Safe Browsing default OFF (D-13).** Competitor baseline: TV Bro has it always on. Default OFF is a privacy-positive but security-negative stance and could be questioned by Amazon's "privacy and security requirements" (A1). Founder decision already listed (PRD §11 #13).
4. **Phone remote (FR-39).** TV Bro's equivalent uses a plain-HTTP LAN server and a public Firebase inbox — a privacy/security weakness TVBrowse should not replicate. TV Bro declares `ACCESS_LOCAL_NETWORK`; TVBrowse's allow-list does not. Whether a local-network permission will be required for FR-39 on newer Android versions is **UNVERIFIED** (not researched; hand to Technical Lead).
5. **Developer verification (sideload channel).** Enforcement from 30 Sep 2026 applies only to installs from participating stores in select regions and, for non-Play distribution, **only mobile/tablet form factors**; ADB installs exempt; applies to Android 8+ certified devices; global rollout 2027; advanced flow (one-time) for unverified apps; limited-distribution accounts ≤20 devices. Play App Signing apps are auto-registered. Lost signing key = package cannot be registered; an already-used package name needs extra review. → FR-52 and the `nogms` APK need a signing-key and package-name registration plan before 2027.
6. **Filter-list licensing (A-17).** StevenBlack README source table (VERIFIED (P)): MIT (StevenBlack, FadeMind add.*, Badd Boyz, hostsVN, UncheckyAds), CC BY 3.0 (AdAway), CC BY 4.0 (Tiuxo), CC0 (minecraft-hosts), **CC BY-SA 4.0 (KADhosts — share-alike, outside FR-17's MIT/CC BY/CC0 set)**, **CC BY-NC-SA 4.0 (MVPS)**, **"non-commercial with attribution" (Dan Pollock)**, **yoyo.org — licence blank (UNKNOWN)**. FR-17's permissive-only subset is feasible; yoyo.org and KADhosts must be excluded or cleared.
7. **TV Bro code/brand.** If any TV Bro code is reused, its licence forbids "TV Bro" in the app name, the same icon/app ID, and requires an About credit. "TVBrowse" does not contain "TV Bro" literally, but similarity is a legal judgement → founder/legal.
8. **Store content rating.** Both competitors carry "Unrestricted Internet" interactive element — expected for TVBrowse too.
9. **Piracy adjacency.** Stream sniffer + downloader + `.m3u8` persona are the most likely rejection triggers on both stores (A-21). Policy text not re-fetched this session → UNVERIFIED.

---

## 8. Evidence & source list

All accessed **2026-11-27**. (P) primary, (S) secondary. S-numbers refer to `docs/research/procurement-and-sources.md` where they match.

**Competitors**
1. (P) TV Bro repository — https://github.com/truefedex/tv-bro (README, LICENSE.md, PRIVACY.md, RELEASE_NOTES.md, `latest_version.json`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, AndroidManifests, `send/LocalSendServer.kt`, `send/CloudInbox.kt`, `webengine/webview/WebViewEx.kt`, `metadata/en-US/*`) [S29]
2. (P) TV Bro releases — https://github.com/truefedex/tv-bro/releases (v2.1.4, v2.1.5, v2.1.6, v3.0.5, v3.0.6, v3.0.7, v3.1.0) [S29a]
3. (P) TV Bro website — https://tvbro.phlox.dev
4. (P) TV Bro Play listing — https://play.google.com/store/apps/details?id=com.phlox.tvwebbrowser [S29b]; Data safety — https://play.google.com/store/apps/datasafety?id=com.phlox.tvwebbrowser
5. (P) BrowseHere Play listing — https://play.google.com/store/apps/details?id=com.tcl.browser ; Data safety — https://play.google.com/store/apps/datasafety?id=com.tcl.browser
6. (S) BrowseHere version history — https://www.apkmirror.com/apk/tcl-group/browsehere-tv-web-browser/ ; updatestar ; soft112
7. (P, weak) amazon.com Appstore search "browsehere" — https://www.amazon.com/s?k=browsehere&i=mobile-apps (no results)
8. (P) Puffin TV Play listing — https://play.google.com/store/apps/details?id=com.cloudmosa.puffinTV
9. (P) TV Akhi fork metadata — https://github.com/halgurdh/tv-bro-no-ads (via GitHub API) [S29c]
10. (P) Downloader history — https://www.aftvnews.com/downloader/history/ [S19]; https://www.aftvnews.com/downloader-is-back-in-the-amazon-appstore-for-fire-tvs-and-fire-tablets/
11. (S) https://www.cordbusters.co.uk/amazon-pulls-fire-tv-downloader-app/
12. (S) TV Bro not on Amazon: https://downloadercodes.com/tv-bro/ ; https://simturax.com/tv-bro-atv-browser/ ; daily.dev post

**Google**
13. (P) Target API requirements — https://support.google.com/googleplay/android-developer/answer/11926878 [S1]
14. (P) TV app quality — https://developer.android.com/docs/quality-guidelines/tv-app-quality [S5]
15. (P) Android multi-platform app quality guidelines (TV review failures) — https://support.google.com/googleplay/android-developer/answer/18058043
16. (P) Multitasking on TV (PiP usage types) — https://developer.android.com/training/tv/get-started/multitasking
17. (P) Optimize memory usage (TV) — https://developer.android.com/training/tv/playback/memory
18. (P) Device and Network Abuse + FGS policy — https://support.google.com/googleplay/android-developer/answer/16559646 (redirected from /answer/9888379)
19. (P) REQUEST_INSTALL_PACKAGES policy — https://support.google.com/googleplay/android-developer/answer/12085295
20. (P) Testing requirements for new personal accounts — https://support.google.com/googleplay/android-developer/answer/14151465 [S2]
21. (P) Data safety section guidance — https://support.google.com/googleplay/android-developer/answer/10787469
22. (P) Developer Program Policy (effective 29 Oct 2026; header only reviewed) — https://support.google.com/googleplay/android-developer/answer/18265346
23. (P) Android developer verification — https://developer.android.com/developer-verification ; FAQ — https://developer.android.com/developer-verification/guides/faq
24. (S) https://tuneline.app/blog/sideloading-on-android-tv-in-2026-what-still-works
25. (P) Google Identity OAuth best practices — https://developers.google.com/identity/protocols/oauth2/resources/best-practices

**Amazon**
26. (P) Device filtering & compatibility (browser restriction) — https://developer.amazon.com/docs/app-submission/device-filtering-and-compatibility
27. (P) Fire OS overview (Fire OS versions, service mapping) — https://developer.amazon.com/docs/fire-tv/fire-os-overview [S10, English page]
28. (P) Fire TV device specifications — https://developer.amazon.com/docs/device-specs/device-specifications-fire-tv-streaming-media-player [S11]
29. (P, search excerpt) Test criteria for Amazon Appstore apps — https://developer.amazon.com/docs/app-testing/test-criteria.html
30. (P, search excerpt) Submit your app (Fire OS / Vega release tracks) — https://developer.amazon.com/docs/app-submission/submitting-apps-to-amazon-appstore [S14]

**Licensing**
31. (P) StevenBlack hosts README source/licence table — https://github.com/StevenBlack/hosts (readme.md via GitHub API) [S21]

---

## 9. Unverified claims & document conflicts

### 9.1 validation-results.md §C — verdicts

| Claim | Verdict | Basis |
|---|---|---|
| Vega 2.0 (Oct 2026) + VPKG release tracks | **Partially VERIFIED.** Vega OS 2.0 on Fire TV Stick 4K 3rd Gen (2026) and separate Fire OS / Vega release tracks: VERIFIED (P). "Oct 2026" date: **UNVERIFIED**. | Sources 28, 30 |
| Downloader v2.0.2 (Mar 2026) browser de-registration | **VERIFIED (P)** | Source 10 |
| target API 36 (31 Aug 2026) + TV ≥34 | **VERIFIED (P)** (mobile 36, TV ≥34 new / ≥33 existing; extension to 1 Nov 2026) | Source 13 |

### 9.2 Conflicts to report to the Lead (exact references)

| # | Document & location | Conflict |
|---|---|---|
| C1 | `PRD_v1.2.md` §5.1 "W0 finding" | "phone companion app" — not a separate app; same APK phone UI + Send-to-TV (from v3.0.6) |
| C2 | `PRD_v1.2.md` §5.1 "W0 finding"; `W0_Validation_Plan` A-30 | DoH and PIN profiles attributed to v3.0.x — first documented in v3.1.0; contextual menu / direct navigation are v2.1.4 |
| C3 | `PRD_v1.2.md` §5.1 row #2 | "no documented `onRenderProcessGone` policy" — TV Bro implements the handler in `WebViewEx.kt` |
| C4 | `procurement-and-sources.md` S29; `TECH_SPEC` D-11 | "TV Bro minAPI 26" — `latest_version.json` minAPI 24 (26 only for Gecko variant); targetSdk 37 |
| C5 | `TECH_SPEC_v1.2.md` FR-16, D-05, D-11, permissions row `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | PiP for media playback is **not allowed on TV** (Google Multitasking on TV); background video must pause (TV-NP) |
| C6 | `reference/browsehere/BrowseHere_Reverse_Engineering.md` | Analyses v8.0.770 (target 23); current Play builds are 8.2x (Android 7.0+). Not representative |
| C7 | `reference/browsehere/Users_Complaints_...md` | No source URLs/dates; contains unverifiable numbers ("95 %+ ad-block", RAM 600→150 MB, Netflix error code, OpenSubtitles "killer feature") |
| C8 | `PRD_v1.2.md` §5.2 policy line [S7] | "Play allows in-app ad-block browsers" — consistent with policy text (prohibition is on blocking *other apps'* ads) but S7 not re-verified |

### 9.3 Other unverified / unknown items

- TV Bro Play (`google`) build permission set (does it strip REQUEST_INSTALL_PACKAGES / QUERY_ALL_PACKAGES?) — UNVERIFIED.
- TV Bro on Amazon Appstore — secondary only. TV Bro on F-Droid — UNKNOWN.
- BrowseHere current engine, ad-block effectiveness, data collection vs recommendations, pre-install deals — UNVERIFIED.
- BrowseHere on non-US Amazon marketplaces — UNKNOWN.
- TV Akhi "uBlock Origin" claim and maintenance status — UNVERIFIED (repo is a fork, 0 stars, pushed 28 Oct 2026).
- Amazon Silk on Fire TV current status/features — UNKNOWN (not researched).
- Puffin TV Amazon availability — UNKNOWN.
- Which Fire TV devices run Fire OS 16 — UNKNOWN.
- Google Play IP/streaming-download policy text (A-21) and FGS declaration form contents (A-31) — UNVERIFIED.
- Organisation Play accounts exempt from 12×14 closed testing — UNVERIFIED (implied).
- Developer-verification "participating stores" list — UNKNOWN (in Google blog, not fetched).
- Google Play review SLA (A-28), TV install-retention metric (A-29) — UNKNOWN.
- Local-network permission requirement for FR-39 — UNVERIFIED (technical, out of scope).
- All demand data (waitlist, interviews, D-1 scorecard) — NOT TESTED (template empty).

---

## 10. Recommendations

Research-level recommendations only; none changes scope, PRD or TECH_SPEC.

1. **Ask Amazon before building for Fire.** Open an Amazon Developer Support case (category per A1) describing TVBrowse as a third-party browser and ask for written pre-clearance criteria. Until answered, treat #6 as BLOCKED and Fire OS as sideload-only.
2. **Fix the TV-policy conflicts in TECH_SPEC through the owning agent/Lead:** C5 (PiP media playback on TV not allowed; background video must pause). Re-scope "PiP bonus" to non-TV devices or drop it — owner decision.
3. **Run the differentiation tests before D-1 sign-off:** #1 (cursor/URL-entry timing), #2 (renderer crash, LMK, PSS vs the 280 MB TV-ME ceiling), #5 (d3ward score) against **TV Bro v3.1.0** and **BrowseHere 8.25.x** on the same 1 GB device.
4. **Reframe #4 as verifiable proof, not a promise:** manifest-diff vs TV Bro, published first-run traffic capture, reproducible/F-Droid build, Data safety identical to privacy policy.
5. **Update competitor references** to current versions (TV Bro v3.1.0; BrowseHere 8.25.012/8.20.060) and replace the complaints doc with a sourced review-mining sheet (URL, date, rating, version).
6. **Plan developer verification now:** decide package names and signing keys for `gms` and `nogms`, keep keys in managed storage, register before the 2027 global rollout; keep FR-52 `nogms`-only.
7. **TV review readiness checklist** (W15–16): AAB, 64-bit + 16 KB alignment for any native libs, 320×180 opaque banner with name, ≥512 px icon, 16:9 actual-UI screenshot, D-pad-only walkthrough of all app chrome, `required=false` for touchscreen/camera/microphone/GPS/telephony/NFC.
8. **Filter list:** exclude MVPS, Dan Pollock, KADhosts (BY-SA) and yoyo.org (licence unknown) from the v1 bundle unless cleared.
9. **Store copy:** avoid "download any video", streaming-site keywords, and system-wide ad-block claims; keep every listed feature working in the TV build (TV-G3).
10. **Do not register a browser intent filter on any Fire build** (Downloader precedent) unless Amazon explicitly approves.

---

## 11. Questions requiring founder approval

1. **Differentiation gate:** With 0/6 differentiators verified at desk level and three already parity, do we (a) wait for W0-2 device results, (b) redefine the product positioning now, or (c) accept parity and compete on a narrower claim (e.g., verifiable privacy + low-RAM stability)?
2. **Amazon channel:** Approve contacting Amazon Developer Support for browser pre-clearance? If refused, drop Fire OS from v1 (sideload only) and remove #6?
3. **PiP / background playback (C5):** Accept removing media PiP and background video from the TV build to meet Google TV policy?
4. **Safe Browsing default OFF (PRD §11 #13):** Confirm, knowing TV Bro is always-on and Amazon's exception path cites "privacy and security requirements".
5. **Play account type (A-27):** Personal (12 testers × 14 days) or organisation (D-U-N-S, up to 28 days to obtain; also needed for full developer-verification account)?
6. **Developer verification:** Approve registering `nogms` package names and signing keys through Play Console / Android Developer Console ahead of 2027.
7. **Name/brand:** Is "TVBrowse" acceptable given its closeness to "TV Bro"? (Legal review needed only if any TV Bro code is reused, but market confusion is also a brand question.)
8. **Stream sniffer / downloader scope (PRD §11 #9, A-21):** Accept the store-rejection risk, or limit to "play in native player" with no download UI on Play/Amazon builds?
9. **Filter-list licence:** Approve excluding KADhosts (CC BY-SA) and yoyo.org (unknown licence) in addition to the non-commercial lists?

---

## Completion report (AGENTS.md)

| Field | Value |
|---|---|
| Assigned task | Market, competitor & platform policy research (10-point scope) → `reports/project-audit/MARKET_POLICY_AUDIT.md` |
| Work completed | Reviewed repo demand/competitor/validation docs; researched BrowseHere, TV Bro (source + releases + listings), Puffin TV, Downloader, TV Akhi; validated 6 differentiators; reviewed Google Play policies (Device & Network Abuse, REQUEST_INSTALL_PACKAGES, FGS, Data safety, target API, closed testing, developer verification), Android TV quality/PiP/memory guidelines, Amazon browser restriction, Fire OS/Vega device specs and test criteria; evaluated evidence quality |
| Files | Created `reports/project-audit/MARKET_POLICY_AUDIT.md` only |
| Evidence | 31 sources listed in §8 with URLs and access date 2026-11-27 |
| Tests | No device/app tests (out of scope for this role) |
| Test status | Differentiators: #3 FAIL, #5 FAIL/NOT TESTED, #6 BLOCKED, #1/#2 NOT TESTED, #4 UNVERIFIED. §C claims: Downloader PASS, target API PASS, Vega partially verified |
| Unresolved issues | Conflicts C1–C8 (§9.2); unknowns in §9.3 |
| Dependencies | W0-2 device tests (QA), Amazon Developer Support response, founder decisions (§11), Technical Lead for C5 and FR-39 local-network question |
| Next action | STOP — waiting for Lead Agent |
