# Agent 4 — Market, Competitor & Policy Audit

**Role:** Agent 4 (Market, Competitor & Policy) · **Date / access date of all sources:** 2026-11-27
**Method:** desk research of public official sources only: GitHub API, Play Console Help, Android Developers, Google Identity, Amazon Developer, the Google Play listing, and AFTVnews (the official Downloader history).
**Labels:** **VERIFIED-DOC** = read directly from the cited official page or repo on the access date · **STATIC-CODE** = read from a competitor's public source tree; not run · **UNVERIFIED** = not confirmed.
**Not done:** no app was installed or run. No user was contacted. **No D-1 verdict was recorded.** The differentiator view in §2 is a desk risk assessment, not the W0-3 result. No W0 artifact was edited.

---

## 1. Competitor: TV Bro (truefedex/tv-bro)

### 1.1 Facts

| Fact | Value | Source | Label |
|---|---|---|---|
| Latest release | **v3.1.0, 2026-11-26.** Releases v3.0.5 (11-13), 3.0.6 (11-18), 3.0.7 (11-20). | GitHub API `repos/truefedex/tv-bro/releases` | VERIFIED-DOC |
| v3.0.5 notes | Major redesign; GeckoView split into a separate variant; **new TV-optimised ad blocker**; **resumable downloads, custom downloads folder**; bookmark import/export + folders; **embedded optimised video player**; HTTP proxy; Mobile/Desktop view per page; targets latest Android | Release v3.0.5 body | VERIFIED-DOC |
| v3.1.0 notes | **Kids profile behind PIN**; **DoH custom DNS**; per-profile clear data; cursor "mouse or finger" mode; incognito via new WebView APIs (**WebView ≥119** needed for incognito/kids profile) | Release v3.1.0 body / `RELEASE_NOTES.md` | VERIFIED-DOC |
| Play listing | **5M+ downloads, 3.6★ (9.74K reviews)**, updated Nov 20 2026. "**a version for phones, with support for tabs and bookmarks exchange**"; "ad and popup blocking"; "built-in optimized video player" | play.google.com/store/apps/details?id=com.phlox.tvwebbrowser | VERIFIED-DOC |
| Play Data safety | "No data shared with third parties"; "**This app may collect** App activity and App info and performance" | same listing | VERIFIED-DOC |
| Privacy policy (repo) | "does **not** collect personal data and does not transmit analytics" (updated Nov 10 2026). Search suggestions sent to the chosen engine; this can be turned off. | `PRIVACY.md` | VERIFIED-DOC |
| minAPI | **24** (WebView "geckoExcluded" build); **26** (geckoIncluded) | `latest_version.json` | VERIFIED-DOC |
| Licence | **Custom BSD-style, not MIT.** Source redistribution must keep the notice. Modified binaries must not use the name "TV Bro", its icon or its app ID, and must show an About window crediting TV Bro with a link. GitHub detects it as `NOASSERTION`. | `LICENSE.md` | VERIFIED-DOC |
| Code (master tree) | `player/` with `NativePlayer`, `PlayerTracks`, `PictureInPicturePlayback`, `PlayerMediaSession`, `FrameRateSwitcher`; `adblock/` module with `FilterParser`, `CosmeticIndex`, `PublicSuffixList`, `SiteFilter` (ABP-style network + cosmetic engine); `send/` with `LocalSendPage`, `QrBitmap`, `CloudInbox` | GitHub tree API | STATIC-CODE |
| Manifest permissions | POST_NOTIFICATIONS, RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, WAKE_LOCK, **ACCESS_COARSE_LOCATION, CAMERA, READ/WRITE_EXTERNAL_STORAGE, REQUEST_INSTALL_PACKAGES, QUERY_ALL_PACKAGES**, FGS + DATA_SYNC, WRITE_EPG_DATA | `app/src/main/AndroidManifest.xml` | STATIC-CODE |
| Recent reviews (2 shown) | One user unhappy with the redesign; one complaining about popups. Developer replied Nov 17 / Nov 24. | Play listing | VERIFIED-DOC (**anecdotal, n = 2, not demand evidence**) |

### 1.2 Corrections to project documents

| Doc claim | Finding | Ref |
|---|---|---|
| "TV Bro v3.0.x (Nov 2026): phone companion, DoH, profiles with PIN…" | Mostly correct, but **DoH and PIN profiles arrived in v3.1.0 (Nov 26)**, not 3.0.x. The "phone companion" is a phone version that exchanges tabs and bookmarks. The repo also has a local "send" page with a QR code. | PRD L16, L108; Plan A-30 |
| "TV Bro `minAPI 26` [S29]" used as the D-11 rationale | **Partly wrong.** The WebView build is **minAPI 24**. Only the Gecko build is 26. | TECH_SPEC L167; procurement L77 |
| "TV Bro = code reference" | The licence permits reuse **only with attribution and naming conditions**. Copying code into a closed product needs founder/legal sign-off. | TECH_SPEC L153 |
| S7 (Play allows ad-block browsers) | The source is a **2016 Android Police article** (secondary, 10 years old). It is not official policy. | procurement L65; PRD L121 |

---

## 2. Differentiator hypotheses — desk risk view (not a W0-3 result)

| # | Hypothesis (PRD §5.1) | Desk evidence | Desk risk |
|---|---|---|---|
| 1 | URL/search-first UI + 60fps cursor + spatial jump | TV Bro redesigned its UI in 3.0.5 and added cursor modes in 3.1.0. Needs hands-on testing. | **UNVERIFIED** |
| 2 | Renderer-kill / process-death resilience + RAM budget | Not visible in public notes. Hard for users to notice or for marketing to claim. | **UNVERIFIED** (weak as a *switching* reason) |
| 3 | TV-grade native HLS player (resolution/audio/subtitle UI) | TV Bro already ships a native player with track selection, PiP, MediaSession and frame-rate switching. | **AT RISK** |
| 4 | Zero-telemetry + minimal permissions in writing | **Minimal permissions:** likely a real difference (TV Bro's manifest is broad). **Zero telemetry:** TV Bro's privacy policy also says no collection. Its Play data-safety label says "may collect App activity / performance", an internal inconsistency in the competitor's own disclosures. | **PARTIAL** |
| 5 | Per-site rendering profiles + honest ad-block reporting | TV Bro has per-page mobile/desktop view, site filters, and an ABP-style engine with cosmetic filtering, which is **stronger than our P0 hosts-only plan**. | **AT RISK** |
| 6 | Maintained `nogms` on both stores | TV Bro ships a generic APK on GitHub. Its Amazon Appstore presence was not checked. | **UNVERIFIED** |

**Desk conclusion:** from public evidence alone, **0 of 6 differentiators are confirmed**, 2 are at risk and 1 is partial. Getting to the D-1 threshold of "≥3 verified" will need W0-3 hands-on testing **and probably a sharper wedge**. Possible directions (for the founder only, not authorised scope): a privacy-plus-minimal-permission claim, 1 GB device reliability, or Fire OS / AOSP focus.

---

## 3. Platform and store policy

| Claim (doc) | Finding | Source | Label |
|---|---|---|---|
| Target API: 36 from Aug 31 2026; TV ≥34 (A-11, Plan §4) | **Confirmed.** New apps and updates must target API 36, **except Android TV apps, which must target ≥34**. The extension to Nov 1 2026 has expired. TECH_SPEC's targetSdk 36 is above the TV minimum, which is allowed. | support.google.com/googleplay/android-developer/answer/11926878 | VERIFIED-DOC |
| Closed testing 12 testers × 14 days (A-27, S2) | **Confirmed** for personal accounts created after Nov 13 2023. Production access is applied for after the test. | …/answer/14151465 | VERIFIED-DOC |
| Downloader v2.0.2 (Mar 2026) browser de-registration (A-20, Plan §4) | **Confirmed:** v2.0.2, **March 30 2026**, Fire TV & Fire tablets: "Due to Amazon policy, Downloader no longer registers itself as a web browser…" | aftvnews.com/downloader/history/ | VERIFIED-DOC |
| Vega 2.0 (Oct 2026) (A-25, Plan §4) | **Confirmed:** GA announced **Oct 29 2026**; "Vega OS 2.0 has now rolled out to all Vega-powered Fire TV devices". Amazon promotes AI-assisted migration of Fire OS Java/Kotlin apps to React Native for Vega. | developer.amazon.com/apps-and-games/blogs/2026/10/vega-2-0-sdk-launch | VERIFIED-DOC |
| Fire TV Stick 4K Select (2025) = Vega | **Confirmed:** build `AFTCA002`, Vega OS 2.0, 1 GB RAM, 32-bit ABI | Amazon device specs (`?v=ftvstick4kselect`) | VERIFIED-DOC |
| Fire TV Stick 4K Plus (2025) `AFTMA08C15` = Fire OS 8 | The spec page did not render for the query tried. | — | **UNVERIFIED** |
| Fire OS versions | Fire OS 16 (API 35/36), 14 (31–34), 8 (29/30), 7 (28), 6 (25), 5 (22). **The procurement table omits Fire OS 16.** minSdk 26 excludes Fire OS 5/6 devices. | developer.amazon.com/docs/fire-tv/ja-fire-os-overview.html | VERIFIED-DOC |
| Google OAuth blocks embedded WebView (A-07, FR-56) | **Confirmed:** "Do not redirect the request through embedded browsing environments, including webviews on mobile platforms, such as WebView on Android." | developers.google.com/identity/protocols/oauth2/resources/best-practices | VERIFIED-DOC |
| UIDT needs `RUN_USER_INITIATED_JOBS` + a notification | Confirmed (handed to Agent 2, A2-01). | developer.android.com/develop/background-work/background-tasks/uidt | VERIFIED-DOC |
| TV quality: 320×180 banner, 16:9 screenshots (S5) | The S5 URL now serves "Android **multi-platform** app quality guidelines". The TV section was not reached in this pass. | …/answer/18058043 | **UNVERIFIED** |
| TV form-factor opt-in (S4), FGS declaration (S3b, A-31), photo/video (S30), review SLA (A-28), retained-installer metric (A-29), Amazon browser/default policy beyond Downloader (A-20) | Not checked in this pass. | — | **UNVERIFIED** |
| Google TV Streamer 4 GB / $149.99 (S20, A-32) | Not checked. | — | **UNVERIFIED** |
| Play policy on ad-block browsers, and on a "donation link" (PRD §9) | Not checked against current official policy. | — | **UNVERIFIED** |

---

## 4. Licensing: filter lists (A-17, FR-17, D-03)

Source: StevenBlack/hosts official `readme.md` "Sources of hosts data" table, repo commit `1cdc93b` (2026-11-26). Domain counts are unique hosts lines I extracted from `data/<source>/hosts` via the GitHub API on 2026-11-27 (**STATIC measurement**).

| Source | Licence (README) | Unique domains | FR-17 permissive (MIT / CC BY / CC0)? |
|---|---|---|---|
| Steven Black ad-hoc | MIT | 3,126 | ✅ |
| AdAway | CC BY 3.0 | 7,326 | ✅ (attribution) |
| add.2o7Net / Dead / Risk / Spam | MIT | 2,030 / 14 / 2,189 / 57 | ✅ |
| Badd Boyz Hosts | MIT | 1,385 | ✅ |
| hostsVN | MIT | 1,757 | ✅ (Vietnam-specific) |
| Tiuxo ads | CC BY 4.0 | 1,729 | ✅ (attribution) |
| UncheckyAds | MIT | 9 | ✅ |
| minecraft-hosts | CC0 | (bare list) | ✅ |
| KADhosts | **CC BY-SA 4.0** | 50,460 | ❌ share-alike (excluded by the FR-17 definition) |
| MVPS | **CC BY-NC-SA 4.0** | 8,726 | ❌ non-commercial |
| Dan Pollock (someonewhocares) | **non-commercial with attribution** | 13,121 | ❌ |
| yoyo.org (Peter Lowe) | **blank in the README** | 3,563 | ❌ until cleared separately |
| **Permissive union (deduplicated)** | — | **≈19,134** | Fits the FR-17 cap of ≤20k |
| Full unified list (README) | — | 81,676 | — |

**Implications:** (1) The ≤20k cap and the permissive-only rule fit together. (2) The permissive subset **drops the three largest classic ad-server lists (MVPS, Pollock, yoyo)**, so the effect on d3ward is **UNVERIFIED and probably negative**. (3) CC BY sources need attribution in `NOTICE`. (4) This is not legal advice. A-17 still needs founder sign-off.

---

## 5. Other policy and business risks found

| # | Risk | Ref |
|---|---|---|
| A4-01 | **F-Droid needs FOSS**, but the product is "free, closed". The channel is not viable unless the licence model changes. | PRD L116; demand L47 |
| A4-02 | **The waitlist collects emails.** It needs a privacy notice. The "server-side counter" needs a form/backend service, which is a new service and needs approval. | demand L11–14 |
| A4-03 | **Persona A ("local media / sideload", `.m3u8`, APK sideload)** is the profile that store reviewers watch for piracy. A-21 copy pre-review is essential. TV Bro holds `REQUEST_INSTALL_PACKAGES` on Play. Whether that is allowed for us on the Play build is an open question (A-09). | PRD L69; TECH_SPEC FR-25/31/32 |
| A4-04 | **Amazon is a shrinking channel:** Vega is on all new sticks and browser registration is restricted (A-20). The PRD already treats Fire OS as secondary, which matches the evidence. | PRD §5.2 |
| A4-05 | **There is no quantitative market-size data in the repo.** TV Bro's 5M+ installs is the only category proxy found. | — |

---

## 6. Task Completion Report (AGENTS.md §9)

- **Assigned task:** Market, Competitor & Policy audit.
- **Work completed:** TV Bro v3.x verification, licence and manifest review; differentiator desk-risk view; 3 "unverifiable" claims checked against official sources; filter-licence table with domain counts; 5 business/policy risks.
- **Files created:** `reports/project-audit/AGENT4_MARKET_COMPETITOR_POLICY_AUDIT.md` only.
- **Evidence:** URLs above, access date 2026-11-27; GitHub API reads (repo commit dates noted).
- **Tests performed:** none (desk research). **Test status:** N/A. Competitor behaviour is **NOT TESTED** (no app was installed).
- **Unresolved issues:** UNVERIFIED rows in §3; the D-1 differentiators need hands-on work; licence decisions (TV Bro reuse, A-17).
- **Dependencies:** devices (for hands-on teardown); founder decisions on wedge, F-Droid and the waitlist service.
- **Recommended next action:** once W0 is approved, move the VERIFIED-DOC rows into `validation-results.md` §C/§E **with the founder's approval**, then finish the UNVERIFIED policy checks in W0-1.
