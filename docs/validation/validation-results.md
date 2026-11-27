# W0 — VALIDATION RESULTS (master fill-in)
**Owner:** ________________ · **Start date:** ____________ · **Last updated:** ____________
**Companion:** `../W0_Validation_Plan_v1.2.md` · **Rule:** har result ke saath evidence (screenshot/log path ya official URL) likhein. **v1.3 sirf is file se banegi.**

> Status codes: ⏳ pending · 🔍 in progress · ✅ confirmed · ❌ disproved (aur kya badla) · ⚠️ partial / needs phase-gate

---

## A. Assumptions register (results)

| ID | Status | Result (1–2 lines) | Evidence (file path / URL) | FR/PRD me kya badla? |
|---|---|---|---|---|
| A-01 | ⏳ | | | |
| A-02 | ⏳ | | | |
| A-03 | ⏳ | | | |
| A-04 | ⏳ | | | |
| A-05 | ⏳ | | | |
| A-06 | ⏳ | | | |
| A-07 (Google OAuth policy) | ⏳ | | | |
| A-08 | ⏳ | | | |
| A-09 | ⏳ | | | |
| A-10 | ⏳ | | | |
| A-11 (target API 36) | ⏳ | | | |
| A-12 | ⏳ | | | |
| A-13 (nav-history) | ⏳ | | | |
| A-14 | ⏳ | | | |
| A-15 | ⏳ | | | |
| A-16 | ⏳ | | | |
| A-17 (filter licenses) | ⏳ | | | |
| A-18 (Safe Browsing) | ⏳ | | | |
| A-19 (vitals/ACRA) | ⏳ | | | |
| A-20 (Amazon browser policy) | ⏳ | | | |
| A-21 (store copy) | ⏳ | | | |
| A-22 (minSdk 26) | ⏳ | | | |
| A-23 (Bundle size) | ⏳ | | | |
| A-24 (device cost) | ⏳ | | | |
| A-25 (Vega) | ⏳ | | | |
| A-26 (d3ward reproducibility) | ⏳ | | | |
| A-27 (Play account type) | ⏳ | | | |
| A-28 (review SLA) | ⏳ | | | |
| A-29 (retained installers metric) | ⏳ | | | |
| A-30 (TV Bro v3) | ⏳ | | | |
| A-31 (FGS declaration) | ⏳ | | | |
| A-32 (2GB baseline device) | ⏳ | | | |
| A-33 (form-time baseline) | ⏳ | | | |

---

## B. Technical test results

| Test | Protocol | Device | Run | Result | Log path |
|---|---|---|---|---|---|
| BT remote sleep x10 (video drop) | `protocols.md` P1 | | | | |
| d3ward baseline — BrowseHere | P2 | | 1/2/3 | | |
| d3ward baseline — TV Bro | P2 | | 1/2/3 | | |
| 30-min LMK session | P3 | | | | |
| `chrome://crash` with **2 WebViews** | P4 | | | | |
| First-run network capture | P5 | | | | |
| Form-time baseline (5 users × 3 forms) | P6 | | | | |
| Service-worker interception | P7 | | | | |
| Cross-origin popup / delayed popunder | P8 | | | | |
| D-pad-only walkthrough (competitor apps) | P9 | | | | |

**Baseline numbers to record:** total PSS (app+renderer+GPU) idle 10 tabs ______ MB · 1080p HLS ______ MB · 1GB device peak ______ MB · cold start ______ ms · d3ward BrowseHere ______% / TV Bro ______% · form median ______ s.

---

## C. Official-source verification (3 unverifiable claims)

| Claim | Official URL check kiya? | Current wording | Verdict |
|---|---|---|---|
| Vega 2.0 (Oct 2026) + VPKG release tracks | ☐ | | |
| Downloader v2.0.2 (Mar 2026) browser de-registration | ☐ | | |
| target API 36 (31 Aug 2026) + TV ≥34 | ☐ | | |

**Duplicate/garbled references cleanup:** ☐ R2 duplicate hata diya · ☐ `hl=en-GB_` fix · ☐ secondary → official mapping `procurement-and-sources.md` me complete.

---

## D. Demand & differentiation (D-1 scorecard)

| Item | Target | Actual | Evidence |
|---|---|---|---|
| Waitlist signups (14 din) | ≥50 | | |
| Channel split | Google TV ___ / AOSP ___ / Fire OS ___ | | |
| Interviews | 5–8 (+2–3 Fire OS) | | |
| Explicit switching intent | ≥5/8 | | |
| Repeated pain points | ≥3 (list karein) | | |
| Verified differentiators vs TV Bro v3 | ≥3 of 6 | | |
| **D-1 verdict** | **GO / NO-GO → W0-4 rework** | | |

**Top 10 "kya missing hai" themes (review mining):** 1. ____ 2. ____ 3. ____ 4. ____ 5. ____ 6. ____ 7. ____ 8. ____ 9. ____ 10. ____

**TV Bro v3 teardown:** `demand-and-competitor.md` me matrix bharein. Kya hamare 6 differentiator hypotheses me se kaunse bache? ______________________

---

## E. Policy / platform / store checklist

| Item | Result | Evidence |
|---|---|---|
| Google OAuth embedded-WebView policy (FR-56 wording) | | |
| Amazon browser registration / default-browser (A-20) | | |
| Store-copy piracy pre-review (A-21) — banned keywords list | | |
| Play account type + closed test requirement (A-27) | | |
| Play TV form-factor opt-in + TV quality checklist (banner 320×180 opaque, 16:9 screenshots, D-pad) | | |
| FGS declaration form requirements + demo video script (A-31) | | |
| Photo/video permissions removed from manifest plan | | |
| Play production / TV review SLA — koi official number mila? (A-28) | | |

---

## F. Procurement log

| Device | Exact model / build | OS | Ordered | Arrived | Cost | Verified (Vega nahi) |
|---|---|---|---|---|---|---|
| 2GB baseline | | | | | | ☐ |
| Google TV Streamer / current-gen | | | | | | ☐ |
| Fire OS (secondary wedge) | | Fire OS 8 / API 30 | | | | ☐ **model check** |
| 1GB AOSP box | | | | | | ☐ |
| Shield / built-in Google TV | | | | | | ☐ |
| *(optional)* Vega unit | | Vega OS 2.0 | | | | ☐ |

**Total spend:** ________ · **Testers recruited (TV-owning):** ____ / 15–20

---

## G. Gates & sign-off

| Gate | Verdict | Date | Notes |
|---|---|---|---|
| **D-1** demand + differentiation | ☐ GO ☐ NO-GO → W0-4 | | |
| **D-2** technical roots | ☐ GO ☐ changes needed | | |
| **D-3** policy/platform | ☐ GO ☐ changes needed | | |
| **D-4** scope freeze → **W1 start** | ☐ GO | | |

**Changes to carry into v1.3 (sirf yahan se):** 
1. ____________________
2. ____________________
3. ____________________
