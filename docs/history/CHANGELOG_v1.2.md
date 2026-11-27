# CHANGELOG v1.1 → v1.2 (third review ka fix map)
**Date:** 2026-11-27 · **Verdict accepted:** "W0 shuru karne ke liye PRD kaafi hai" · **v1.3 sirf W0 results se**
**Note:** v1.0/v1.1/v1.2 same din ki iterations hain — is liye **date same hai, revision ID alag**. v1.1 ke changelog me "FR-11/12 fix ke saath D-02" wala unrelated reference **hata diya gaya**.

---

## 1. Fixes ke dauran nayi contradictions

| Review item | Fix | Kahan |
|---|---|---|
| Legend vs Roadmap: P1 = v1.1 lekin FR-51 W14, `ur` W14–15, W12–13 me "FR-47/51/52/54 jahan P0" (koi P0 nahi), v1.1 me repeat | **Canonical legend:** P0 = v1/W1–W16, P1 = v1.1, P2 = v2. FR-47/51/52/54 aur `ur` ab **sirf v1.1**; W12–13/W14–15 se hata diye; `W0` roadmap rebalanced | `PRD` §6/§8 · `TECH_SPEC` §0/§1.5/§1.6 |
| FR-45 vs FR-19: gesture popunders + duplicate | **FR-19 = sirf cross-origin redirect guard.** FR-45 = `window.open`/`target=_blank` ka akela owner: gesture+same-origin → tab; **gesture+cross-origin → confirm**; delayed(>800ms)/non-gesture → block+toast; per-site allow | `TECH_SPEC` §1.3 |
| Section 10 vs FR-48: runtime location bina manifest, camera/mic permissions, `uses-feature camera required=false`, NFR-10 CI block | **Naya canonical permissions Section 4:** `CAMERA` + `RECORD_AUDIO` declare + dono `uses-feature required=false`; **geolocation v1 me unsupported** (koi location permission nahi, honest message); `READ_MEDIA_*` hata diya (SAF needs none); NFR-10 allow-list in sab ko cover karti hai | `TECH_SPEC` §4, FR-48, NFR-10 |
| NFR-05 vs Section 14: ≥99.5% sessions vs <1.09% users alag metrics; 1.09% penalty floor hai target nahi | **Internal target = user-perceived crash rate ≤0.3%** (Play vitals); `nogms` = opt-in ACRA **sample**; 1.09/0.47/8% = penalty floors, target nahi [S3a] | `TECH_SPEC` NFR-05 · `PRD` §7 |
| NFR-01 "≤250MB idle/3 tabs" vs gate "10 tabs @ ≤250MB" | Dono ab **≤250MB idle with 10 tabs**; + filter-set memory bhi count | `TECH_SPEC` NFR-01/FR-12 · `PRD` §7/§8 |
| W0b: 15 assumptions + demand + teardown 1 hafte me | **W0 = 3 hafte**: W0-1 procure/verify/recruit · W0-2 technical spike · W0-3 demand+teardown+policy · **W0-4 optional D-1 rework buffer** | `W0_Validation_Plan` §1/§3 |
| W17–18: "17–21 din" slot me fit nahi | Fixed-SLA claim **hata diya**; W17–18 = closed test + application, **W19–20 external review buffer**; A-28 me official SLA verify | `PRD` §8 · `W0_Validation_Plan` A-28 |
| P0 "≈30" asal 34; changelog D-02 unrelated; version date | **P0 = 35** (v1.1 ke 34 + naya FR-56; FR-33b P1 tha, delete hone se P0 count par asar nahi) explicitly likha; D-02 reference hataya; same-day date note + v1.3 W0-se rule | `PRD` §0/§6/§12 · is file ka header |

---

## 2. Technical / policy gaps

| Review item | Fix |
|---|---|
| `READ_MEDIA_*` hata dein; SAF ko permission nahi chahiye; Play photo/video declaration | **Removed** from allow-list; FR-47 SAF-only; [S30] | 
| FR-16 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, FR-39 FGS type missing | `mediaPlayback` add; **FR-39 ke liye koi FGS nahi** — server sirf foreground Activity se bound (specialUse declaration se bach gaye) |
| Play Console FGS declaration form + video | W13–14 deliverable + `W0/protocols.md` P10 script [S3b] |
| Android 15 `dataSync` 6h/24h limit; user-initiated data transfer jobs (API 34+) | **FR-31 redesign:** primary = user-initiated data transfer job / WorkManager + **in-app progress**; `dataSync` FGS sirf fallback with `onTimeout()` [S26][S27] |
| TV par notification shade nahi | Download progress **in-app** (FR-31 acceptance) |
| Google sign-in embedded WebView = `disallowed_useragent`, UA bypass policy ke khilaf; A-07 compat → policy; FR-33 default desktop UA par bhi risk | **A-07 = policy item** [S16][S17][S18]; **FR-33b (`; wv` experiment) delete**; **FR-56 = honest "not supported" message**; FR-33 UA = **layout only**, `accounts.google.com` par guard; `PRD` non-goals me |
| FR-41 predictive keyboard W2–3 me unrealistic; baseline/protocol nahi | **FR-41 = snippets + URL shortcuts + suggestions only (P0)**; **FR-41b custom IME = P1**; "≥30%" ab **5 users × 3 forms** protocol ke saath (`W0/protocols.md` P6, A-33) |
| FR-43: WebViews aam tor par ek renderer share karte hain | FR-43 acceptance: callback **har affected instance** par, active + standby dono recreate, `chrome://crash` **2 WebViews** ke saath test (P4) |
| FR-13: in-memory Bundle → process death me back/forward history chali jayegi; SPA scroll approximate | **Apni nav-history list (URL stack + index) Room me**; acceptance me back/forward restore; SPA/infinite-scroll approximate explicitly likha |
| FR-17 license: repo MIT lekin sources mixed (Peter Lowe/MVPS non-commercial); Pro me masla; A-17 tak "MIT" pakka na; official README; 100k domains RAM | **"MIT" pakka claim hata diya**; **permissive-only curated subset** (MIT/CC BY/CC0), source = official StevenBlack README [S21]; v2 Pro se pehle re-check (A-17); **filter-set ka RAM NFR-01 me count**, subset cap ≤20k |
| D-10: 2–4MB ke liye DFM/SplitCompat complexity zyada | **D-10 revised: Media3 dono flavors me bundled**; NFR-08 = **ek hi target APK ≤20MB**; S28 sirf "kyun avoid kiya" ke liye |
| Safe Browsing default OFF = conscious tradeoff | `PRD` §11 decision 13 (founder) + A-18 |
| NFR-11: default favicons bundled warna pehla homepage network call | **FR-37 = bundled default speed-dial favicons**; airplane-mode first-run acceptance |

---

## 3. Metrics jo measure nahi ho sakte

| Review item | Fix |
|---|---|
| "Sessions/week median ≥3" — data device se bahar jata hi nahi | **Metric HATA diya**; jagah = qualitative opt-in survey + voluntary CSV export (KPI nahi) |
| "D7 retention via Play cohorts" = "retained installers", usage nahi | Label badla: **D7/D30 retained installers (install-retention, NOT usage)**; availability A-29 me verify |
| FR-41 form-time protocol | **5 users × 3 forms**, W0 baseline → v1 same protocol, `W0/protocols.md` P6 |

---

## 4. Plan / store realism

| Review item | Fix |
|---|---|
| W1–3 me 15 FR, W4–6 halka | **Rebalanced:** W1–2 = 8 P0, W3–4 = 7 P0, W5–7 = 11 P0, W8–9 = 5, W10–12 = 4, W13–14 = 1+reviews, W15–16 = stores |
| Play TV form-factor opt-in + alag TV quality review (banner/D-pad/leanback rejection) | W15–16 checklist + [S4][S5][S6]; `PRD` §10 risk |
| 12 testers ke paas TV device chahiye; paid services risky | W0-1 se **15–20 TV-owning testers** community recruitment; `W0/procurement-and-sources.md` |
| Chromecast discontinued; naye Fire Sticks Vega; exact model confirm | `W0/procurement-and-sources.md` §1 me official model/OS table + "Vega mat kharidein" checklist; Google TV Streamer 4GB/$149.99 [S20]; A-32 |
| D-1 fail path ka rework buffer nahi | **W0-4 = 1 hafta optional rework buffer** |

---

## 5. Sources

| Review item | Fix |
|---|---|
| Core decisions abhi bhi secondary/SEO blogs par (R6/R7/R10/R18/R24/R27–R29) | Official replacements: Amazon docs (S8–S15), TV Bro GitHub/Play (S29–S29c), Play vitals (S3a), Play Feature Delivery (S28), StevenBlack README (S21), AdGuard official (S22). Poora replacement map `W0/procurement-and-sources.md` §3 |
| R2 = R1 duplicate, `hl=en-GB_` garbled | **R2 deleted**, sirf S1 (`?hl=en`) |
| 3 claims reviewer verify nahi kar sake (Vega 2.0 Oct 2026, Downloader v2.0.2, target API 36) | `W0_Validation_Plan` §4 + `W0/validation-results.md` Section C me **W0-1 official-URL verification tasks** (S9/S14, S19, S1) |

---

## 6. Strategic note

| Review item | Fix |
|---|---|
| Fire wedge kamzor: Google TV + AOSP primary, Fire OS secondary **D-1 se pehle** | **`PRD` §0/§3/§5.2/§10/§11** me primary wedge ab Google TV + AOSP; legacy Fire OS secondary; interviews bhi primary-weighted |
| 19-section PRD bhaari; split karo; mazeed review rounds ka faida kam | **3 docs me split:** `PRD_v1.2.md` (product), `TECH_SPEC_v1.2.md` (engineering), `W0_Validation_Plan_v1.2.md` (validation) + 4 `W0/` working files. **v1.3 sirf `W0/validation-results.md` se** |

---

## 7. W0 shuru (aaj)

1. `W0/validation-results.md` khola gaya — master fill-in artifact.
2. `W0/procurement-and-sources.md` — exact models + official source checklist (W0-1).
3. `W0/protocols.md` — P1…P10 scripts (W0-2).
4. `W0/demand-and-competitor.md` — D-1 + TV Bro v3 teardown (W0-3).
5. **W1 blocked** jab tak D-1…D-4 sign-off na ho.
