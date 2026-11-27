# W0 — PROCUREMENT & OFFICIAL SOURCES
**Use with:** `validation-results.md` Sections C/F · `../W0_Validation_Plan_v1.2.md` W0-1

---

## 1. ⚠️ Order dene se pehle: exact model / OS check

Amazon official Fire OS overview aur device specs ke mutabiq [S10][S11]:

| Model (build) | OS | Hamare liye |
|---|---|---|
| Fire TV Stick 4K Select (2025) `AFTCA002` | **Vega OS 2.0** | ❌ **Mat kharidein** (Android APK nahi chalega) |
| Fire TV Stick 4K 3rd Gen (2026) `AFTCR001` | **Vega OS 2.0** | ❌ |
| Fire TV Stick HD (2026) `AFTCL001` | **Vega OS 2.0** | ❌ |
| Fire TV Stick 4K Plus (2025) `AFTMA08C15` | **Fire OS 8 / API 30** | ✅ Secondary wedge test device |
| Fire TV Stick 4K Max 2nd Gen (2023) | Fire OS | ✅ (verify current listing) |
| Fire TV Stick Lite (1GB, 2020) | Fire OS | ✅ low-RAM, used market |
| Fire OS 14 TVs (2025/26, API 34) | Fire OS 14 | ✅ optional |

**Checklist:** ☐ Seller se exact build model poochein · ☐ Box/settings me OS version confirm · ☐ "Vega" / "4K Select" / "2026 HD" keyword se bachein · ☐ Receipt me model likhein.

**Google side:** Chromecast with Google TV (2GB) **discontinue** ho chuka hai (Google Store ab Google TV Streamer 4K, **4GB RAM, $149.99** bechta hai [S20]). 2GB low-RAM baseline ke liye: ☐ used Chromecast (2GB) ya ☐ 1–2GB AOSP box. **Streamer 4GB hai — isay low-RAM test ka substitute na banayein**, current-gen compat ke liye alag rakhein.

---

## 2. Cost (W0-1 me quotes confirm → A-24)

| Item | Estimate | Note |
|---|---|---|
| Used Chromecast w/ Google TV 2GB / 1–2GB AOSP box | ~$40–80 | Low-RAM baseline |
| Google TV Streamer 4K | $149.99 | Official Google Store [S20] |
| Fire OS 4K Plus / 4K Max / Lite (used) | ~$30–70 | Exact model check upar |
| Nvidia Shield (used) ya built-in Google TV borrow | ~$100–180 / $0 | Borrow preferred |
| Cheap uncertified AOSP box | ~$30–50 | Purana WebView test |
| *(optional)* Vega Stick 4K Select | ~$40 | Sirf A-25 port feasibility |
| Emulators | $0 | — |
| **Total** | **≈$400–600 one-time** | + Play $25 + domain ~$10–15/yr |

**Testers:** 12 required, **15–20 recruit karein** aur sab ke paas **TV device** hona chahiye (closed test TV-only app hai). Paid tester services risky hain — community/friends/TV forums use karein. ☐ Recruitment W0-1 se start.

---

## 3. Official source master list (secondary/SEO references ki jagah)

| ID | Source | URL |
|---|---|---|
| S1 | Play Console Help — Target API level | https://support.google.com/googleplay/android-developer/answer/11926878?hl=en |
| S2 | Play Console Help — Closed testing 12×14 din | https://support.google.com/googleplay/android-developer/answer/14151465?hl=en |
| S3 | Play Developer Programme Policy (copyright / IP) | https://support.google.com/googleplay/android-developer/answer/17517561 |
| S3a | Play Console Help — Android vitals thresholds (1.09% / 0.47% / 8%) | https://support.google.com/googleplay/android-developer/answer/9844486?hl=en |
| S3b | Play Console Help — FGS declaration (description + video) | https://support.google.com/googleplay/android-developer/answer/13392821?hl=en |
| S4 | Play Console Help — TV form-factor opt-in / dedicated tracks | https://support.google.com/googleplay/android-developer/answer/13295490?hl=en |
| S5 | Play Console Help — Android TV quality guidelines (banner/screenshots) | https://support.google.com/googleplay/android-developer/answer/18058043 |
| S6 | Play Console Help — Preview assets (TV banner + 16:9 screenshot) | https://support.google.com/googleplay/android-developer/answer/9866151?hl=en |
| S7 | Play policy — in-app ad-block browsers allowed (secondary corroboration) | https://www.androidpolice.com/2016/03/01/google-explicitly-bans-ad-blockers-from-the-play-store-except-all-those-ad-blocking-web-browsers-apparently/ |
| S8 | Amazon Vega Docs — run apps / `.vpkg` / Developer Mode | https://developer.amazon.com/docs/vega/0.23/run-apps |
| S9 | Amazon Dev Blog — Vega 2.0 GA (Oct 2026), Fire OS→Vega migration | https://developer.amazon.com/apps-and-games/blogs/2026/10/vega-2-0-sdk-launch |
| S10 | Amazon — Fire OS overview / device OS table (**Vega vs Fire OS models**) | https://developer.amazon.com/docs/fire-tv/ja-fire-os-overview.html |
| S11 | Amazon — Fire TV device specifications (4K Select = 1GB Vega OS) | https://developer.amazon.com/docs/device-specs/device-specifications-fire-tv-streaming-media-player.html?v=ftvstick4kselect |
| S12 | Amazon Dev Blog — "Introducing Vega OS" (multi-OS, Fire OS launch jari) | https://developer.amazon.com/apps-and-games/blogs/2025/09/announcing-vega-os |
| S13 | Amazon Dev Blog — 2025 year in review (Vega = Linux + React Native) | https://developer.amazon.com/apps-and-games/blogs/2025/12/2025-year-in-review |
| S14 | Amazon App Submission — release notes (VPKG submissions 2025, release tracks Nov 2026) | https://developer.amazon.com/docs/app-submission/release-notes.html |
| S15 | Amazon — Cloud App Streaming details (local network/USB/PiP unsupported) | https://developer.amazon.com/docs/cloud-app-program/amazon-cap-solution-details.html |
| S16 | Google Identity — OIDC reference (`disallowed_useragent`) | https://developers.google.com/identity/openid-connect/reference |
| S17 | Google Identity — Sign in with Google best practices (embedded WebView blocked) | https://developers.google.com/identity/siwg/best-practices |
| S18 | Google Identity — OAuth best practices (use secure browsers, no webviews) | https://developers.google.com/identity/protocols/oauth2/resources/best-practices |
| S19 | AFTVnews — Downloader official version history (v2.0.2 Amazon policy) | https://www.aftvnews.com/downloader/history/ |
| S20 | Google Store — Google TV Streamer (4GB, $149.99) | https://store.google.com/product/google_tv_streamer?hl=en-US |
| S21 | StevenBlack/hosts — **official README** (source license table: MIT, CC BY-NC-SA, non-commercial, CC0…) | https://github.com/StevenBlack/hosts/blob/master/readme.md |
| S22 | AdGuard official — DNS/hosts cannot block YouTube in-video ads | https://adguard.com/en/article/how-to-block-ads-on-youtube.html · https://adguard.com/en/blog/dns-content-blocking-at-scale/amp.html |
| S23 | Android Developers — `WebViewClient.onRenderProcessGone` (API 26, return true) | https://developer.android.com/reference/android/webkit/WebViewClient |
| S24 | Android Developers — `ServiceWorkerController` | https://developer.android.com/reference/android/webkit/ServiceWorkerController |
| S25 | Android Developers — `ServiceWorkerClientCompat` | https://developer.android.com/reference/kotlin/androidx/webkit/ServiceWorkerClientCompat |
| S26 | Android Developers — FGS changes (API 34 types, API 35 timeouts, API 36 user-initiated jobs) | https://developer.android.com/develop/background-work/services/fgs/changes |
| S27 | Android Developers — FGS timeouts + data transfer options (6h/24h, WorkManager, user-initiated transfer) | https://developer.android.com/develop/background-work/services/fgs/timeout · https://developer.android.com/about/versions/15/changes/datasync-migration |
| S28 | Android Developers — Play Feature Delivery (DFM/SplitCompat — **kyun avoid kiya**) | https://developer.android.com/guide/playcore/feature-delivery |
| S29 | TV Bro official GitHub README + `latest_version.json` (minAPI 26) | https://github.com/truefedex/tv-bro/blob/master/README.md · https://github.com/truefedex/tv-bro/blob/master/latest_version.json |
| S29a | TV Bro official GitHub Releases (**v3.0.x Nov 2026 features**) | https://github.com/truefedex/tv-bro/releases |
| S29b | TV Bro Google Play listing | https://play.google.com/store/apps/details?id=com.phlox.tvwebbrowser |
| S29c | TV Akhi (TV Bro fork, uBlock Origin) GitHub | https://github.com/halgurdh/tv-bro-no-ads |
| S30 | Play Console Help — Photo/Video permissions policy (SAF/photo picker) | https://support.google.com/googleplay/android-developer/answer/14115180?hl=en · https://support.google.com/googleplay/android-developer/answer/16558241?hl=en |
| S31 | Chromium source — WebView Safe Browsing README (GMS, M126+ real-time) | https://chromium.googlesource.com/chromium/src.git/+/master/android_webview/browser/safe_browsing/README.md |
| S32 | Android Developers — `Manifest.permission` FGS type constants | https://developer.android.com/reference/android/Manifest.permission |
| S33 | Android Police (secondary) — Google TV PiP category restrictions | https://www.androidpolice.com/google-tv-finally-support-picture-in-picture-mode-not-way-you-want/ |

### v1.1 ke secondary references → official replacement map

| v1.1 ref (hata diya) | Ab kya use hua |
|---|---|
| R2 (R1 ka duplicate, garbled `hl=en-GB_`) | **Deleted** — sirf S1 |
| R6 / R7 (Vega SEO/news blogs) | **S10/S11/S12/S13/S14** (Amazon official) |
| R10 (TV Bro stats blog) | **S29/S29a/S29b** (GitHub + Play) |
| R18 (AAB/DFM glossary blog) | **S28** (Android Developers) |
| R24 (third-party license tables) | **S21** (StevenBlack official README) |
| R27/R28 (hosts/YouTube blogs) | **S22** (AdGuard official) |
| R29 (vitals blog) | **S3a** (Play Console Help) |
| R15 (free-codecs Downloader) | **S19** (AFTVnews official) |
| R5 (PCWorld Vega, secondary) | **S8/S10/S12**; PCWorld sirf corroboration |
| R23 (LogRocket renderer explainer) | **S23** (Android Developers) |

**3 claims jo reviewer khud verify nahi kar sake** → `validation-results.md` Section C me official URL se confirm karein: Vega 2.0 Oct 2026 (S9/S14), Downloader v2.0.2 (S19), target API 36 (S1).
