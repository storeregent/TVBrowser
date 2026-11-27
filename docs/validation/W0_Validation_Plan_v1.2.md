# W0 VALIDATION PLAN — v1.2 (W0-start edition)
**Date:** 2026-11-27 · **Owner:** founder + 1 Android dev · **Status:** **W0 shuru** · **Blocks:** W1 (coding)
**Companions:** `PRD_v1.2.md` · `TECH_SPEC_v1.2.md` · `W0/validation-results.md` (master fill-in) · `W0/protocols.md` · `W0/procurement-and-sources.md` · `W0/demand-and-competitor.md`

> **Rule:** v1.3 sirf `W0/validation-results.md` ke asli results se banegi. Mazeed desk-review nahi. Jo assumption yahan resolve na ho, wo apne `Gate` phase se pehle resolve hogi — warna us phase ka code spec tentative hai.

---

## 1. Kyun W0 = 3 hafte (1 ya 2 nahi)

v1.1 me W0b me 15 assumptions + demand + teardown ek hafte me the — wahi overloading dobara ho rahi thi. Ab **3 alag tracks** hain, aur device-dependent kaam procurement ke baad hi:

| Track | Hafte | Kaam | Dependency |
|---|---|---|---|
| **W0-1 — Procure + verify + recruit** | 1 | Exact device models/OS order karein, shipping lead time; **official-source checklist** (`W0/procurement-and-sources.md`) complete karein; **15–20 TV-owning closed-test testers** recruit karein; Play account type confirm; landing page live | Koi device nahi chahiye — **aaj se shuru** |
| **W0-2 — Technical spike** | 2 | Device-dependent assumptions A-01/02/03/05/12/13/18/23/26 + `W0/protocols.md` ke scripts: BT sleep, d3ward x3, 30-min LMK, `chrome://crash` 2 WebViews, first-run mitmproxy, 5×3 form baseline | Devices haath me |
| **W0-3 — Demand + teardown + policy** | 3 | D-1 (waitlist + 5–8 interviews + review mining), TV Bro v3 teardown, A-07 Google OAuth policy, A-20/A-21/A-22/A-24/A-25/A-27/A-28/A-29/A-31/A-33 | Landing page 14-day clock **W0-1 se start** |
| **W0-4 — optional rework buffer** | +1 | **D-1 fail path**: wedge/requirements revise, PRD v1.3 draft | Sirf agar D-1 fail ho; pass ho to W1 jaldi |

**Aaj ka Day-1 checklist (W0-1):**
- [ ] `W0/validation-results.md` kholein, owner/date likhein
- [ ] `W0/procurement-and-sources.md` ke exact model numbers se order place karein (**Vega model na aa jaye**)
- [ ] Official source checklist me 3 "unverifiable" claims verify karein (Section 4)
- [ ] Waitlist landing page live + 14-day clock start
- [ ] 15–20 TV-device-wale testers ko invite (closed test ke liye; paid services se parhez)
- [ ] Play Console: account type (personal/organization), target API page, TV form-factor page, FGS declaration form dekhein

---

## 2. Assumptions register (33 rows; `W0/validation-results.md` me status fill karein)

| ID | Claim / sawal | Gate | Track | Status |
|---|---|---|---|---|
| A-01 | BT remote sleep → config change → fullscreen video drop (Complaints #1) | W5 | W0-2 | ⏳ |
| A-02 | TCL endpoints live / telemetry (RE §6) | W0 | W0-2 | ⏳ |
| A-03 | `shouldInterceptRequest` se exact stream headers milte hain (#21 / Pillar 2, **#25 nahi**) | v1.1 | W0-2 | ⏳ |
| A-04 | Auto-PiP Home par process bachata hai (#4) | v1.1 | W0-2 | ⏳ |
| A-05 | "95%" aur <30/300 baseline (#28) — d3ward/canyoublockit dono apps par | W0 | W0-2 | ⏳ |
| A-06 | Netflix deep-link exact title kholta hai (#12) | v1.1 | W0-2 | ⏳ |
| **A-07 (badla)** | ~~`; wv` hata kar OAuth theek karna~~ → **POLICY ITEM**: Google embedded WebView me sign-in block karta hai (`disallowed_useragent`); UA trick policy ke khilaf. Verify official docs + FR-56 message ka wording | W1 | W0-3 | ⏳ [S16][S17][S18] |
| A-08 | OpenSubtitles free? (quota/ToS) | v2 | W0-3 | ⏳ |
| A-09 | `REQUEST_INSTALL_PACKAGES` Play par | v2 | W0-3 | ⏳ |
| A-10 | Google Sign-In/Drive Fire TV par | v2 | W0-3 | ⏳ |
| A-11 | targetSdk 36 / TV ≥34 official schedule | W0 | W0-1 | ⏳ [S1] |
| A-12 | Cheap boxes par purana non-updatable WebView | W0 | W0-2 | ⏳ |
| A-13 | `saveState` scroll/form restore + **nav-history list** ki zarurat | W5 | W0-2 | ⏳ |
| A-14 | `MULTI_PROFILE` availability | v1.1 | W0-2 | ⏳ |
| A-15 | Local WebSocket server foreground-only chal sakta hai (koi FGS nahi) | v1.1 | W0-2 | ⏳ |
| A-16 | 1 active + 1 standby vs 2–3 live (switch latency vs LMK) | W5 | W0-2 | ⏳ |
| **A-17 (badla)** | **StevenBlack repo MIT hai, lekin aggregated sources mixed**: CC BY-NC-SA 4.0 (MVPS), "non-commercial with attribution" (Dan Pollock), CC BY-SA 4.0, CC0. Permissive-only subset kaunsa? Pro(v2) me non-commercial terms ka kya? Official README + `data/` licenses se | W8 | W0-3 | ⏳ [S21] |
| A-18 | Safe Browsing `nogms` par call/slowdown; GMS dependency | W0 | W0-2 | ⏳ [S31] |
| A-19 | Play vitals threshold + ACRA self-hosted/F-Droid | W1 | W0-3 | ⏳ [S3a] |
| A-20 | Amazon ab browser registration/default allow karta hai? (Downloader v2.0.2) | W0 | W0-3 | ⏳ [S19] |
| A-21 | Stream sniffer + downloader + `.m3u8` persona → Play/Amazon copy reject? | W13 | W0-3 | ⏳ [S3] |
| A-22 | minSdk 26 par kitna install-base chhootta hai | W0 | W0-3 | ⏳ [S29] |
| A-23 | `saveState` Bundle size / `TransactionTooLargeException` + 256KB cap | W5 | W0-2 | ⏳ |
| A-24 | Device matrix ki asli qeemat (~$400–600) | W0 | W0-1 | ⏳ |
| A-25 | Vega installed base / port kab worth; **Fire OS abhi bhi launch hota hai** (Amazon blog) | W0 / v2 | W0-3 | ⏳ [S10][S12] |
| A-26 | d3ward TV WebView par reproducible (3 runs variance) | W0 | W0-2 | ⏳ |
| A-27 | Play account personal (12×14 din) ya organization (exempt) | W0 | W0-1 | ⏳ [S2] |
| **A-28 (new)** | Play production/TV-quality review ka **koi official SLA number hai ya nahi** — "17–21 din" claim hata diya gaya; W19–20 buffer rakha | W17 | W0-3 | ⏳ |
| **A-29 (new)** | Play Console me TV form factor ke liye **"retained installers" / D7 metric** available hai? (label = install-retention, usage nahi) | W15 | W0-3 | ⏳ |
| **A-30 (new)** | TV Bro **v3.0.x (Nov 2026)** features — phone companion, DoH, profiles, contextual menu, player, resumable downloads — Play listing + GitHub releases se confirm. **≥3 differentiators bachte hain?** | W0 (D-1) | W0-3 | ⏳ [S29a][S29b] |
| **A-31 (new)** | FGS declaration form me `mediaPlayback` + fallback `dataSync` ke liye video + description kya maanga jayega | W13 | W0-3 | ⏳ [S3b] |
| **A-32 (new)** | Chromecast (2GB) discontinued → used unit ya Google TV Streamer (4GB, $149.99) kaunsa low-RAM baseline dega | W0 | W0-1 | ⏳ [S20] |
| **A-33 (new)** | Form-time baseline protocol (5 users × 3 forms) W0 me measure ho sakta hai bina app ke? (paper/competitor-app proxy) | W3 | W0-2 | ⏳ |

---

## 3. Go / No-Go gates

| Gate | Kab | Faisla |
|---|---|---|
| **D-1 (demand + differentiation)** | W0-3 end | ≥50 waitlist / 14 din **aur** ≥5/8 interviews switching intent + 3 repeated pains **aur** ≥3 verified differentiators vs TV Bro → **GO**. Fail → **W0-4 rework buffer**, wedge/scope revise |
| **D-2 (technical)** | W0-2 end | A-01/02/03/05/12/13/18/23/26 me se koi bhi root cause galat nikla to us FR ko `TECH_SPEC` me badlein, phir W1 |
| **D-3 (policy/platform)** | W0-3 end | A-07/A-20/A-21/A-27/A-28/A-31 clear; store copy aur permissions finalize |
| **D-4 (scope freeze)** | W0-4 end | PRD v1.3 + Tech Spec v1.3 publish → **W1 start** |

---

## 4. Official-source checklist (3 claims jo reviewer khud verify nahi kar sake)

| Claim | Official source | Kya confirm karein | Status |
|---|---|---|---|
| **Vega 2.0 (Oct 2026) + VPKG / release tracks** | Amazon Dev blog `vega-2-0-sdk-launch` (Oct 2026) + App Submission release notes (Nov 16 2026) + Vega run-apps docs | Current wording, kaunse devices Vega, kya Android APK chal sakta hai | ⏳ W0-1 |
| **Downloader v2.0.2 (Mar 2026) browser de-registration** | AFTVnews official version history | Exact changelog wording + Amazon policy reason | ⏳ W0-1 |
| **target API 36 schedule (31 Aug 2026) + TV ≥34** | Play Console Help `answer/11926878` | Current wording, TV exception, extension deadline guzar chuka hai | ⏳ W0-1 |

Baaki secondary/SEO references (v1.1 ke R6/R7/R10/R18/R24/R27–R29) ko official se replace kar diya gaya hai — poori mapped list `W0/procurement-and-sources.md` me. **R2 duplicate aur `hl=en-GB_` garbled URL hata di gayi.**

---

## 5. Device matrix (exact models `W0/procurement-and-sources.md`)

| Device | Kyun | Note |
|---|---|---|
| **2GB Google TV baseline** — used Chromecast w/ Google TV (2GB) ya 1GB/2GB AOSP box | NFR-01/06 low-RAM | Chromecast discontinued [S20]; **Google TV Streamer 4GB hai, low-RAM test ke liye nahi** |
| Google TV Streamer (4GB, $149.99) | Current-gen Google TV + TV quality review | Official Google Store [S20] |
| **Fire OS device — exact model verify**: 4K Plus 2025 `AFTMA08C15` (Fire OS 8 / API 30) ya 4K Max 2nd gen | Secondary wedge, `nogms`, 1GB-ish | **4K Select 2025 `AFTCA002`, 4K 3rd gen 2026 `AFTCR001`, HD 2026 `AFTCL001` = Vega OS 2.0 → order na karein** [S10][S11] |
| Nvidia Shield / Sony-TCL-Hisense built-in Google TV | OEM remote + config-change | Borrow / retail demo |
| Sasta AOSP box (no Play) | Purana WebView + sideload | — |
| Emulator API 26/30/34/36 | Compatibility sweep | free |
| *(optional)* Vega Stick 4K Select | A-25 port feasibility | Alag SDK/.vpkg |

---

## 6. W0 deliverables

| File | Output |
|---|---|
| `W0/validation-results.md` | Har A-row ka result + evidence link/screenshot path; D-1..D-4 sign-off |
| `W0/protocols.md` | Scripts ke actual logs: BT sleep x10, d3ward x3, 30-min LMK, `chrome://crash` 2 WebViews, mitmproxy first-run, 5×3 form baseline |
| `W0/procurement-and-sources.md` | Ordered models + invoices + official-source verification status |
| `W0/demand-and-competitor.md` | Interview notes, waitlist numbers, TV Bro v3 feature matrix, 6 differentiators ka verdict |
| **→ `PRD_v1.3.md` / `TECH_SPEC_v1.3.md`** | Sirf in results se, W0-4 me |

---

### Key official sources (poori list `W0/procurement-and-sources.md`)
[S1] https://support.google.com/googleplay/android-developer/answer/11926878?hl=en · [S2] https://support.google.com/googleplay/android-developer/answer/14151465?hl=en · [S3] https://support.google.com/googleplay/android-developer/answer/17517561 · [S3a] https://support.google.com/googleplay/android-developer/answer/9844486?hl=en · [S3b] https://support.google.com/googleplay/android-developer/answer/13392821?hl=en · [S16] https://developers.google.com/identity/openid-connect/reference · [S17] https://developers.google.com/identity/siwg/best-practices · [S18] https://developers.google.com/identity/protocols/oauth2/resources/best-practices · [S19] https://www.aftvnews.com/downloader/history/ · [S20] https://store.google.com/product/google_tv_streamer?hl=en-US · [S21] https://github.com/StevenBlack/hosts/blob/master/readme.md · [S29a] https://github.com/truefedex/tv-bro/releases · [S29b] https://github.com/truefedex/tv-bro/blob/master/README.md · [S31] https://chromium.googlesource.com/chromium/src.git/+/master/android_webview/browser/safe_browsing/README.md
