# TVBrowse — PROJECT AUDIT (Consolidated, v2)

**Prepared by:** Lead Orchestrator · **Date:** 2026-11-27 · **Repo state audited:** `main` @ `1660e6a`
**Stage:** W0 validation. **W1 is blocked. W0 execution has NOT started.**

**Source reports.** Each was written by a separate agent on its own branch and PR:

| Short ref | Report | Agent | PR |
|---|---|---|---|
| **PA** | [`PRODUCT_AUDIT.md`](PRODUCT_AUDIT.md) | Product & Documentation | #2 |
| **AA** | [`ANDROID_ARCHITECTURE_AUDIT.md`](ANDROID_ARCHITECTURE_AUDIT.md) | Android Architecture | #3 |
| **QA** | [`QA_VALIDATION_AUDIT.md`](QA_VALIDATION_AUDIT.md) | QA & W0 Validation | #5 |
| **MP** | [`MARKET_POLICY_AUDIT.md`](MARKET_POLICY_AUDIT.md) | Market, Competitor & Policy | #4 |
| **Lead** | this file | Lead Orchestrator: own document checks and spot-verification of agent claims | #1 |

The links resolve once PRs #2–#5 are merged. `ASSIGNMENTS.md` records file ownership.

> **v2 change note.** v1 of this file (commit `065f67d`) was written before the four agent reports existed. It relied on provisional specialist passes that the Lead ran itself (`AGENT1…AGENT4_*.md`). **Those provisional files have been removed** because they duplicated the real reports. They are still in git history. Lead findings that no agent covered are kept below, labelled **Lead**, with file:line evidence.
>
> **Evidence labels:** PASS / FAIL / BLOCKED / NOT TESTED / UNVERIFIED (AGENTS.md §6). Also used:
> - **VERIFIED-DOC** = the Lead confirmed it from an official public source on 2026-11-27.
> - **SRC-VERIFIED (agent)** = the agent cites an official source; the Lead did not re-check it.
> - **STATIC** = reasoning from documents or code; nothing was executed.
>
> **No device, emulator, build or user test was run by anyone. Nothing in this audit is a test result.**

---

## 0. Agent work review (Lead verdict)

| Report | Scope respected | Evidence quality | §9 completion report | Verdict |
|---|---|---|---|---|
| PA (#2) | Yes. Only its own file; no PRD/TECH_SPEC edits | Static document analysis with good references. Two over-statements (errata E-1, E-2) | Partial: ends with "Awaiting Lead", no §9 table | **Accept** |
| AA (#3) | Yes. Only its own file | **High.** V1–V10 are official or AOSP sources. Lead re-verified V1–V3 (AOSP `SystemImpl.java`) | Yes (§14) | **Accept** |
| MP (#4) | Yes. Only its own file | **High.** 31 sources with access dates. Lead re-verified the Amazon browser restriction and the TV PiP ban | Yes | **Accept**, with label errata E-4 |
| QA (#5) | Yes. Only its own file; no tests run | Good static review. Strict about evidence | Yes (§12) | **Accept** |

No agent fabricated a test result. Each agent created exactly one new file and modified nothing else. The four PRs have no file overlap, so they do not conflict.

### 0.1 Errata recorded by the Lead (the reports are not edited; read them with these corrections)

| ID | Report & location | Correction | Basis |
|---|---|---|---|
| E-1 | PA §4.5 / §4.6 | **FR-44 does have an acceptance criterion:** "SW site par blocked count badhe; purane WebView par crash nahi". The "Review §2 (v1.1)" text is in the **Source** column, not in Acceptance. The same applies to the other "Review §N" cells. | TECH_SPEC L72 |
| E-2 | PA §2.3 | The Source codes are only **partly** undefined. `C#n` matches complaint IDs in the TECH_SPEC §6 traceability table (L210–L229). `Review §2/§3` is mapped at L229. `E Q…` is still undefined; it probably refers to `Better_Android_TV_Browser.md` Q1–Q20, but that is not documented. The finding is narrowed to the undefined `E Q…` codes plus the missing review document (C-32, M). | TECH_SPEC §6 L210–L229 |
| E-3 | PA §1 row 12 | `reference/browsehere/README.md` is **empty (1 byte)**, not an "overview". | `wc -c` |
| E-4 | MP §4, completion table | MP uses **PASS/FAIL** for desk verdicts, e.g. "Target API PASS" and "#3 FAIL". Under AGENTS.md, PASS/FAIL apply only to executed tests. Read them as **VERIFIED-DOC / AT RISK (parity)**. No W0 test has been executed. | AGENTS.md §6 |
| E-5 | MP §9.1 | Vega OS 2.0 date "Oct 2026" is marked UNVERIFIED there. **Lead VERIFIED-DOC:** Amazon developer blog, Vega OS 2.0 SDK GA **29 Oct 2026**. | developer.amazon.com/apps-and-games/blogs/2026/10/vega-2-0-sdk-launch |
| E-6 | AA §7 | "Fire OS 7 = Android 9 base (verify)". **Lead VERIFIED-DOC:** the Fire OS overview maps Fire OS 7 to API 28 and Fire OS 8 to API 29–30. Fire OS 7 devices therefore fall in the R-01 window if they are 32-bit and low-RAM; per-device status is still UNVERIFIED. | developer.amazon.com/docs/fire-tv/fire-os-overview |
| E-7 | PA §11 Q3 | "TV Bro v3 already has companion app" is superseded by MP C1. It is the **same APK** with a phone UI plus Send-to-TV. | MP §3.2 (primary: repo code) |
| E-8 | QA §0 E3 vs §1.2 | E3 names 3 D-2-critical gaps (A-03, A-13, A-23). §1.2 correctly finds **4** (adds A-12). This file uses 4. | W0 Plan L75 |
| E-9 | QA §11 | The numbering repeats "2". Cosmetic. | — |
| E-10 | **Lead v1** §9.1 F-11 | v1 proposed *deferring* A-03. **That is withdrawn:** A-03 is in the D-2 gate set (Plan L75), so deferring it would change the gate (see X-08). | QA §1.2 |

---

## 1. Overall project readiness

| Dimension | Rating | Basis |
|---|---|---|
| Product definition | 🟡 **AMBER** | The PRD and TECH_SPEC are detailed: 35 P0 FRs, 12 NFRs, 13 ADRs. But open founder decisions are written as facts (C-01), and the wedge is both "decided" and "to be validated" (C-02). |
| Document consistency | 🟡 **AMBER** | 42 contradictions/defects: **12 H**, 21 M, 9 L (§4). All W0 cross-links are broken (C-20). |
| Architecture | 🟡 **AMBER** | All agents agree the direction is sound, and nobody proposes a re-architecture. However, **six source-verified spec conflicts** (C-05…C-10) must be fixed in v1.3 before W1. |
| W0 design | 🟡 **AMBER** | Gates, raw-log rule and status discipline are strong. But **D-2 cannot be evaluated as designed**: 4 of its 9 assumptions have no protocol (C-11), the P3 memory method under-counts the renderer (C-16), and the P4 premise fails on single-process devices (C-06). |
| W0 evidence | 🔴 **RED (none yet)** | 0/33 assumptions, 0/10 protocols, 0/4 gates. No devices, testers, waitlist or harness (QA §10). |
| Market / distribution | 🔴 **RED** | **0 of 6 differentiators are verified as distinct.** Three are parity with TV Bro v3.1.0 (MP §4). Amazon officially restricts third-party browsers (C-09). Google TV bans media playback in PiP (C-08). |
| W1 readiness | ⛔ **BLOCKED (correct)** | Gates are unsigned and `app/` is empty. This is consistent with AGENTS.md §4. |

**Bottom line:** The project is **well documented but not validated**. It is **ready to start the W0-1 desk/procurement track** as soon as the founder approves the inputs in §9.1. It is **not ready for W0-2** until three things happen: the missing D-2 protocols are added, an isolated harness is authorised, and devices arrive. The dominant risk remains **D-1**: there are not yet ≥3 verifiable reasons to switch from TV Bro. Since v1, a **distribution risk** has been added: Amazon restricts browsers, and the Google TV review bars media PiP and requires browser chrome to be navigable by D-pad without a virtual mouse.

---

## 2. Repository inventory (summary)

19 tracked files on `main`. Docs total about 146 KB, mostly in Roman Urdu and English. The following are **empty (1 byte)**: `README.md`, `prototype/README.md`, `reference/browsehere/README.md` and `.gitignore`. `app/`, `tests/` and `reports/W0/` contain only `.gitkeep`. There is no LICENSE file, no CI, and no Gradle project (AA §1.1, PA §1, QA §10).

---

## 3. Requirements inventory

### 3.1 Counts

| Class | Count | Note |
|---|---|---|
| P0 FR (v1, W1–W16) | **35** | Correct (PA §2.1). The PRD §8 Tabs row says "11" but lists 10 (C-34). |
| P1 FR (v1.1) | 19 | No P1 item appears in the W1–W16 roadmap (PA §3.1). |
| P2 FR (v2) | 4 | FR-20/28/32/40 |
| Deleted / undefined | FR-33b deleted; **FR-42 never defined** | C-37 |
| NFR | 12 | NFR-01…12 |
| ADR | 13 | D-01…D-13 |
| W0 assumptions | 33 | **All NOT TESTED** |
| W0 protocols | 10 | **All NOT TESTED** |
| Gates | 4 | D-1 demand · D-2 technical · D-3 policy · D-4 scope freeze |
| Founder open decisions (PRD §11) | 13 | §9.3 |

### 3.2 P0 requirement register with consolidated audit flags

| FR | Requirement (short) | Phase | W0 dependency | Audit flags (IDs in §4 / §6) |
|---|---|---|---|---|
| FR-01 | Single Activity, `BrowserEngine` interface | W1–2 | — | Contract shape undefined (AA D-gap-01). Enforce via module + import rule (AA §2, F-28) |
| FR-07 | HUD toolbar | W1–2 | — | Play TV-DP: chrome must be D-pad navigable without a virtual mouse (MP G5) |
| FR-08 | Search-first URL entry, FTS suggestions | W1–2 | — | Needs Room, which arrives in W5–7 (C-14). "≤3 interactions" is undefined (C-41) |
| FR-37 | Local homepage, bundled favicons | W1–2 | P5 | Homepage technology affects cold start and first-run calls (AA D-gap-10) |
| FR-43 | Renderer-crash recovery | W1–2 | P4 | **C-06** single-process WebView · **C-14** needs TabState · guard parameters undefined (AA D-gap-04) |
| FR-48 | Site permission prompts | W1–2 | — | Per-site persistence needs Room (C-14). Web-notification support UNVERIFIED (R-28) |
| FR-53 | No hardcoded strings | W1 | — | — |
| FR-56 | Google sign-in honest fallback | W1–2 | A-07 | Policy VERIFIED-DOC. **C-26**: the desktop UA lacks `; wv`, so this guard is the only control |
| FR-02/03/04 | Cursor 60 fps, spatial jump, auto-hide | W3–4 | — | Injection model undecided (R-11) |
| FR-05/06 | Key normalisation, Back priority | W3–4 | — | **C-10** Android 16 + targetSdk 36 Back dispatch · **C-12** `goBack()` vs app history |
| FR-41 | Snippets/shortcuts | W3–4 | A-33, P6 | A-33 is in no gate (C-24). Baseline is a between-app comparison (R-23) |
| FR-46 | Edge auto-scroll | W3–4 | — | Nested scrollers (AA §3) |
| FR-11/12 | ≤2 live, eviction | W5–7 | A-16, P3 | **C-07** popup needs a 3rd WebView · **C-15** `pauseTimers` is global · **C-17** memory metric |
| FR-13 | Disk state + nav-history | W5–7 | A-13, A-23 | **C-12**. Most complex P0 item (AA R-08) |
| FR-14 | `configChanges` lock | W5 | A-01, P1 | P1 conflates the repro with the pass check (C-29). HDMI/CEC triggers missing (AA T-11) |
| FR-15 | Grid switcher | W5–7 | — | Video thumbnails come out black (AA §5.7) |
| FR-29/30/49/50 | Room, bookmarks, clear data, history | W5–7 | — | Bookmark import/export is parity with TV Bro |
| FR-33 | Desktop UA, per-site profiles | W2 + W6 | — | Roadmap phase mismatch (C-27). Profiles are parity (MP #5) |
| FR-17 | Permissive-only hosts ≤20k | W8–9 | A-17 | Permissive union ≈19,134 domains (Lead STATIC). Exclude KADhosts and yoyo.org (MP §7.6) |
| FR-21 | d3ward ≥70% | W8–9 | A-05, A-26, P2 | No baseline. P2 has no decision rule (QA §6) |
| FR-44 | SW interception | W8–9 | P7 | Acceptance is defined (E-1) |
| FR-45 | Sole `window.open` owner | W8–9 | P8 | **C-07**. Test pages missing (QA §2C). FR-19 intersection (C-30) |
| FR-55 | Honest ad-block copy | W13–16 | A-21 | Phase mismatch (C-35) |
| FR-22/23/24/26 | Fullscreen, Media3, subtitles, open-with | W10–12 | — | Native player is **parity** (MP #3). Memory with WebView alive (AA T-22) |
| FR-31 | Downloader (UIDT / WorkManager) | W13–14 | A-31 | **C-05** permission missing. blob:/data:/POST and API 26–28 gaps (AA R-12) |

---

## 4. Contradiction register

Severity:
- **H** = blocks a gate, or would fail the build, CI or policy review.
- **M** = causes wrong work or broken traceability.
- **L** = editorial.

**Nothing has been changed in the documents.** Fixing PRD/TECH_SPEC items needs founder authorisation (v1.3 queue, §9.4).

### 4.1 High (12)

| ID | Contradiction | Exact references | Impact | Proposed resolution (needs approval) | Source |
|---|---|---|---|---|---|
| C-01 | **"Approved" vs open.** AGENTS.md calls the PRD and TECH_SPEC approved. PRD §11 lists 13 open decisions, yet NFR-07 (minSdk 26, targetSdk 36) and NFR-11/§5 (Safe Browsing OFF) state them as facts. | AGENTS.md L24, L74; PRD L3, L20, L215; TECH_SPEC L134, L138 | Agents cannot tell what is binding. v1.3 would have to undo "decided" text. | Founder approval statement: what is binding and what is a proposal (F-02). | Lead; PA §4.1 |
| C-02 | **Wedge decided vs validated.** PRD §0/§3 and Plan §1 say the wedge was already changed. PRD §11 #2 keeps it open, and the D-1 fail path says "wedge revise". | PRD §0, §3, §5.2, §11 #2; Plan §1 | D-1 cannot both test and assume the wedge. | Founder states whether D-1 *confirms* the wedge or *chooses* it (F-21). | PA §4.2 |
| C-03 | **D-1 thresholds not confirmed.** The PRD says "founder confirm karein", and the scorecard pre-fills them. "5–8 interviews" vs "≥5/8". No rubric for a "verified differentiator" and no interview coding rule. | PRD L73, L79; Plan L74; results §D | A false GO or a disputed gate result. | Confirm the numbers, a ratio rule and a differentiator rubric before W0 (F-07). | PA §4.3, §6; Lead |
| C-04 | **W0-4 "optional" vs required.** D-4 and v1.3 sit "at W0-4 end", but W0-4 runs only if D-1 fails. | Plan L18, L77, L115; PRD L21, L159 | No W1 entry slot if D-1 passes. | A fixed v1.3 + D-4 window on both paths (F-08). | Lead |
| C-05 | **FR-31 UIDT requires `RUN_USER_INITIATED_JOBS`** plus a notification, and is API 34+ only. The permission is not in the §4 allow-list, so NFR-10 CI would block it. No Jetpack support exists, and LMK stops the job. | TECH_SPEC L94, L137, L176–198 | Fails the spec's own CI rule. On API <34 (all Fire OS 7/8), the fallback is the only path. | v1.3: add the permission or drop UIDT (F-23). | AA C-01/R-02; Lead **VERIFIED-DOC** |
| C-06 | **FR-43 / NFR-06 / P4 "0 app exits" vs single-process WebView.** On Android 8.1/9/10, a 32-bit device that reports low-RAM runs WebView in single-process mode by default, so a renderer crash kills the app. | TECH_SPEC FR-43 L61, NFR-06; protocols P3/P4; PRD §7 | The gate cannot pass on such devices by design. Likely affected (UNVERIFIED): cheap Android 9/10 boxes and Fire OS 7 (API 28). | T-01 on every device, then founder scope decision (F-17). | AA R-01/C-04; Lead **VERIFIED-DOC** (AOSP `SystemImpl.java` oreo / pie / android11) |
| C-07 | **FR-45 vs FR-11.** `onCreateWindow` requires a *new* WebView, gives no target URL, and has no trustworthy opener. With active + standby live, honouring any popup needs a 3rd instance, which breaks the "hard ceiling 2". | TECH_SPEC FR-11 L55, FR-45 L73 | FR-45 cannot be implemented as written. | Decide: transient 3rd instance / evict standby / new tab without transport (F-24), informed by T-06. | AA C-02/R-04 (SRC-VERIFIED agent) |
| C-08 | **FR-16 PiP and background video vs Google TV policy.** "Any playback of media content [is] not allowed in picture-in-picture mode on TV". Background video must pause (TV-NP). | TECH_SPEC FR-16, D-05, D-11, §4 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` row | TV review failure if shipped as written. TV Bro disables PiP in its Play flavor for this reason. | v1.3: drop media PiP and background video from the TV build; audio-only continuation allowed (F-16). | MP C5/G6/G7; Lead **VERIFIED-DOC** (developer.android.com/training/tv/get-started/multitasking) |
| C-09 | **Fire OS "secondary channel" and differentiator #6 vs Amazon policy.** "Amazon restricts the publication of third-party browser apps on Fire tablets and Fire TV devices. In limited cases, we may permit…" (via Developer Support). | PRD §5.2, §5.1 #6 | An Amazon Appstore listing is unlikely without pre-clearance. #6 is **BLOCKED**. | Open an Amazon Developer Support case in W0-1; otherwise Fire OS is sideload-only (F-15). | MP A1; Lead **VERIFIED-DOC** (Amazon device-filtering page, "Device filtering by web browser apps") |
| C-10 | **NFR-07 targetSdk 36 vs FR-05/06 Back handling.** On Android 16+, `KEYCODE_BACK` is not dispatched and `onBackPressed` is not called unless the app migrates or opts out. | TECH_SPEC NFR-07, FR-05, FR-06 | The Back priority chain can break on Android 16 TVs. | Decide: migrate to predictive-back APIs vs temporary opt-out (F-25). Test with T-07. | AA R-03 (SRC-VERIFIED agent) |
| C-11 | **D-2 gate assumptions without a protocol.** A-03, A-12, A-13 and A-23 (4 of the 9 D-2 assumptions) have no test script. | Plan L75 vs protocols P1–P10 | D-2 cannot be evaluated as planned. | Add the protocols, merging QA §2A sketches with AA T-02/T-12/T-13 (F-11, X-04). | QA E3/§1.2 |
| C-12 | **FR-13 app-owned nav-history (the Bundle is never persisted) vs FR-06 `goBack()`.** After process death, WebView history and app history diverge (POST, SPA `pushState`). | TECH_SPEC L51, L57 | "back/forward wapas" cannot be tested without defined semantics. | Short FR-13 design note before W5; T-12 in W0. | Lead; AA R-08/D-gap-05 |

### 4.2 Medium (21)

| ID | Contradiction | References | Proposed resolution | Source |
|---|---|---|---|---|
| C-13 | F-Droid listed as a PRIMARY channel vs a closed product. Re-rated M (was H in v1) because it does not block W0. | PRD L116; TECH_SPEC L206 | Choose a licence model (F-10) | Lead |
| C-14 | Roadmap dependency inversion: FR-43, FR-08 and FR-48 are in W1–2 but need Room/TabState from W5–7. | PRD §8; TECH_SPEC FR-08/29/43/48 | Minimal data skeleton in W1–2, or scope down the W1–2 acceptance (F-29) | AA C-06/R-09; PA §3.3 |
| C-15 | FR-12 `onPause()+pauseTimers()` cannot target one WebView: `pauseTimers` is global and `onPause` does not stop JS. FR-11 "standby" wording is also ambiguous (blank prewarm vs MRU live page). | TECH_SPEC L55–56, L128 | Define standby (F-26) after T-05 / A-16 | AA C-03/R-05; Lead |
| C-16 | The P3 command `dumpsys meminfo <pkg>` reports the host process only, not the renderer. | protocols P3; NFR-01 | Host + renderer-by-PID method; add PMF | AA C-07/R-06 |
| C-17 | **Memory target definition differs in four places:** NFR-01 PSS ≤250 MB with no device qualifier; the PRD PSS definition omits the filter set; Play **TV-ME: ≤280 MB total on 1 GB low-RAM TVs** (a review criterion); official WebView guidance recommends PMF/RSS. | PRD L144; TECH_SPEC NFR-01 L128 | One metric + device binding (F-19) | PA §5; AA Q-13; MP G11; Lead |
| C-18 | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (FR-16, P1) is in the v1 allow-list. The W13–14 "FGS declaration video" mixes up FR-16 (P1) and FR-31 (P0). | TECH_SPEC L60, L181; PRD §8 W13–14 | Split §4 into v1 / v1.1; tie the video to FR-31 | Lead; AA C-05; PA §3.4 |
| C-19 | Safe Browsing "default OFF" is written as fact while decision 13 is open. WebView's default is **ON**, so OFF needs an explicit manifest flag. WebView `MetricsOptOut` is not addressed despite the zero-telemetry claim. | TECH_SPEC L138, L202; PRD L215 | F-20 after T-15 | PA §4.1; AA C-11, §5.8; Lead |
| C-20 | Every `W0/...` path and `W0/logs/` is missing, and the `../W0_Validation_Plan` links are broken. The CHANGELOG claim "W0/validation-results.md khola gaya" is false. | PA §12 lists all occurrences | Doc-hygiene PR; evidence root `reports/W0/logs/` (F-12, X-07) | PA §12; AA C-10; QA E5; Lead |
| C-21 | Results use ⏳/🔍/✅/❌/⚠️ while AGENTS.md mandates five exact codes. | validation-results L5; AGENTS.md L104–110 | Adopt the mapping in §7.4 | QA §3.1-3; Lead |
| C-22 | TV Bro facts in the PRD/spec are wrong or outdated: "companion app" (it is the same APK); DoH/PIN are v3.1.0 not v3.0.x; context menu is v2.1.4; `onRenderProcessGone` *is* implemented; minAPI is **24** (26 only for the Gecko build). PRD "W0 finding (already)" vs A-30 ⏳. | PRD §5.1 L16, L108; TECH_SPEC D-11 L167; procurement S29 | Correct in v1.3; record A-30 with evidence | MP C1–C4; QA E2; Lead |
| C-23 | The "Play allows ad-block browsers" claim rests on a 2016 secondary source (S7). The current policy text is consistent but has not been re-fetched. | PRD L121; procurement L65 | Replace with the official policy (W0-1) | MP C8/G3; Lead |
| C-24 | A-16 and A-33 are W0-2 items but sit in no gate. A-16 also has no protocol. | Plan L49, L66, L75 | Add both to D-2 | Lead; QA §1.2 |
| C-25 | The JS-bridge rule (own origin only) conflicts with features that need page→app events on arbitrary origins (spatial nav, FR-45 gesture timing). | TECH_SPEC §5; FR-03; FR-45 | Clarify the rule (AA D-gap-15) | AA C-08 |
| C-26 | The default desktop UA has no `; wv` token, which effectively equals the deleted FR-33b. The FR-56 URL guard becomes the only OAuth control. | TECH_SPEC FR-33, FR-33b, FR-56, D-07, D-12 | FR-56 scope decision (F-30); T-16 | AA C-09 |
| C-27 | FR-33 phase: TECH_SPEC says W2 + W6; the roadmap says W5–7 only. | TECH_SPEC L97; PRD L163–165 | Add the FR-33 defaults to W1–2 | Lead |
| C-28 | Waitlist: "static page" vs "server-side counter" + email field. | demand L11–14 | One service + privacy notice (F-09) | Lead |
| C-29 | P1 conflates A-01 (reproduction on BrowseHere) with the FR-14 pass on our app. "Video drop" is undefined. | protocols P1 | Split P1 and define FAIL | QA §6 |
| C-30 | The FR-19 / FR-45 boundary case (a popunder that redirects) has no test page. | TECH_SPEC FR-19, FR-45; P8 | Add a combined P8 page | PA §4.4 |
| C-31 | Reference docs are outdated or unsourced: the RE doc analysed v8.0.770 (target 23; current is 8.2x); the complaints have no URLs; superseded claims remain (Fire TV "huge opportunity", Roku, "95%+"). | `reference/browsehere/*` | "Non-authoritative" banner; sourced review-mining sheet | MP C6/C7; PA §7.H; Lead |
| C-32 | Source-column traceability: `E Q…` is undefined; the "third review" doc and PRD v1.0/v1.1 are missing (E-2). | TECH_SPEC Source column; PRD L5 | Glossary + add the review doc to `docs/history/` | PA §2.3 (corrected); Lead |
| C-33 | The results template asks for a "cold start ___ ms", but no protocol produces it. | validation-results baseline line | Add a mini-protocol (AA T-18) or remove the line | QA §2B; AA T-18 |

### 4.3 Low (9)

| ID | Item | Source |
|---|---|---|
| C-34 | PRD §8 Tabs row says "11"; it lists 10. | PA §3.2; Lead |
| C-35 | FR-55 phase: TECH_SPEC says W13–16; roadmap says W8–9. | Lead |
| C-36 | Safe Browsing is cited as "D-6"; it is decision 13 / ADR D-13 (PRD L207). | Lead |
| C-37 | FR-42 is never defined. Mark it "reserved". | PA §2.1; Lead |
| C-38 | NFR-06/P3 "0 kills" vs the deliberate `chrome://crash` step. Define it as "0 app-process exits". | Lead |
| C-39 | The procurement Fire OS table omits Fire OS 16 (API 35/36). | Lead (VERIFIED-DOC) |
| C-40 | Complaint #21 is only partly fixed in v1 (full fix via FR-25 in v1.1), and this is not stated. | PA §2.2 |
| C-41 | Subjective acceptance wording: FR-33 "readable", FR-08 "interaction", FR-17 cap only in notes, FR-55 "misleading", FR-13 "approximate". | PA §5 |
| C-42 | P10 has no Section B row (it maps to Section E). | QA §1.3 |

---

## 5. Contradictions and tensions **between** agent findings (Lead review)

| ID | Tension | Agents | Lead assessment / ruling |
|---|---|---|---|
| X-01 | **TV Bro "companion app".** PA Q3 says a companion app exists. MP C1 says it is the same APK with a phone UI. | PA vs MP | **MP is correct**: it checked the repo code, a primary source (E-7). |
| X-02 | **minSdk, three-way.** MP: TV Bro reaches API 24, so 26 loses reach. TECH_SPEC: 26. AA Q-01(c): ≥30 avoids the single-process window but drops Fire OS 7 (API 28) and Android 9 boxes. MP G10: Play TV-PS requires minSdk ≤31. | MP vs AA vs spec | A real reach vs reliability trade-off. **Founder decision F-18, after the T-01 data.** No agent is wrong. |
| X-03 | **Memory metric, three-way** (C-17). PA wants device qualifiers. AA says PSS under-counts and recommends PMF. MP says Play TV-ME 280 MB total is a *review gate*, not just a differentiator. | PA / AA / MP | The agents complement each other. Decide **one metric + device binding before** the T-04 baseline is interpreted (F-19). TV-ME makes this a release risk, not only a quality goal. |
| X-04 | **D-2 coverage.** QA lists the missing protocols. AA proposes experiments, but outside the plan. | QA vs AA | They fit together. Merged mapping: **A-03** → QA §2A sketch only (no AA equivalent) · **A-12** → AA T-02 + QA WebView inventory · **A-13** → AA T-12 + QA sketch · **A-23** → AA T-13 + QA sketch · **A-16** → AA T-05. Also **P4 must be preceded by AA T-01/T-03**, and **P3 must use the AA memory method**. |
| X-05 | **Differentiator #2 (resilience).** MP calls it weak, because TV Bro already handles `onRenderProcessGone` and TV-ME binds every app. AA R-01 shows FR-43 *cannot* hold on single-process devices. | MP + AA | They agree: #2 is weaker than the PRD assumes. **Only a measured advantage on the same 1 GB device can save it.** |
| X-06 | **Desk PASS/FAIL labels.** MP labels desk verdicts PASS/FAIL. QA insists that PASS requires an executed test with a raw log. | MP vs QA | **Lead ruling: QA's rule stands** (AGENTS.md §6). MP verdicts are read as VERIFIED-DOC / AT RISK (E-4). |
| X-07 | **Evidence root.** PA: create `W0/` *or* rewrite the paths. QA: `reports/W0/logs/`. AA: notes the mismatch. | PA / QA / AA | **Lead recommendation: `reports/W0/logs/P1…P10/` + `interviews/`, and rewrite the references** (PA option b). This is part of the doc-hygiene PR (F-12). |
| X-08 | **Deferring A-03.** Lead v1 F-11 proposed deferring A-03. QA shows A-03 is in the D-2 gate set. | Lead v1 vs QA | **The Lead withdraws the proposal** (E-10). Only non-gate assumptions (A-04, A-06, A-14, A-15) may be deferred. |
| X-09 | **Value of Fire OS.** The PRD treats Fire OS as a secondary channel. MP: Amazon restricts browsers, and new sticks run Vega. AA/E-6: Fire OS 7 (API 28) is in the single-process window; below API 34, downloads have only the fallback path. | PRD vs MP + AA | The evidence agrees: **Fire OS is worth less than the PRD assumes.** Founder decision F-15 (Amazon pre-clearance, or sideload-only). |
| X-10 | **Safe Browsing default.** PA: an open decision pre-encoded as fact. AA: WebView default is ON, and OFF needs an explicit flag plus `MetricsOptOut`. MP: TV Bro keeps it always on, and Amazon's exception path cites "privacy and security". | PA / AA / MP | They agree. Default OFF improves privacy but **weakens security, and may hurt the Amazon case**. Founder decision F-20 after T-15. |

---

## 6. Technical and project risk register

L = likelihood, I = impact (H/M/L). All risks are **OPEN**. Owners are roles; no named people have been assigned yet (R-15).

| ID | Risk | L | I | Evidence status | Mitigation / closing evidence | Source |
|---|---|---|---|---|---|---|
| R-01 | **D-1 fails:** fewer than 3 verifiable differentiators vs TV Bro v3.1.0. Desk status: 0/6 distinct, 3 parity. | H | H | Desk (MP §4) | Device A/B on the same 1 GB device for #1/#2/#5; prepare a wedge/positioning option (F-07, F-21) | MP; PA |
| R-02 | Single-process WebView on 8.1–10 32-bit low-RAM devices, so FR-43 is impossible there. | M | H | Mechanism **VERIFIED-DOC**; devices UNVERIFIED | T-01 on all devices; F-17 | AA R-01 |
| R-03 | Memory: NFR-01 ≤250 MB and Play TV-ME ≤280 MB total on 1 GB devices may not be reachable with WebView + Media3. | M-H | H | UNVERIFIED | T-04 baseline before W1; F-19 | AA R-07; MP G11 |
| R-04 | **Amazon channel:** third-party browsers are restricted; Downloader was suspended for registering as a browser; new sticks run Vega. | H | M | **VERIFIED-DOC** | Developer Support case; no browser intent filter on Fire (F-15) | MP A1–A3 |
| R-05 | Google TV review: chrome navigable by D-pad without a virtual mouse; no media PiP; video pauses on leave; AAB; 64-bit + 16 KB pages. | M | H | VERIFIED-DOC (rules); compliance NOT TESTED | v1.3 fix for C-08; review checklist W15–16 | MP G5–G7, G10 |
| R-06 | targetSdk 36 → no `KEYCODE_BACK` on Android 16. | M | H | SRC-VERIFIED (agent) | T-07; F-25 | AA R-03 |
| R-07 | FR-45 needs a 3rd WebView. | H | M | SRC-VERIFIED (agent) | T-06; F-24 | AA R-04 |
| R-08 | FR-13 history complexity after process death. | H | M | STATIC | T-12; design note before W5 | AA R-08; Lead |
| R-09 | FR-31: permission gap; blob:/data:/POST; API 26–28 storage; LMK kills downloads. | H | M | VERIFIED-DOC + STATIC | v1.3 spec fix; T-14; F-23 | AA R-02/R-12 |
| R-10 | A live standby keeps running JS (CPU, memory, audio). | M | M | SRC-VERIFIED (agent) | T-05; F-26 | AA R-05 |
| R-11 | Compose↔WebView focus / cursor injection model / WebView owned by composition. | M | H | UNVERIFIED | T-08; one host `FrameLayout`, WebViews swapped imperatively | AA §1.2, R-11; Lead |
| R-12 | d3ward ≥70% with a permissive-only list of ≈19k entries. | M-H | H | UNVERIFIED | P2 baseline + list experiment (F-14) | Lead; MP §7.6 |
| R-13 | **D-2 BLOCKED:** missing protocols plus no harness authorisation. | H | H | Fact | F-03 + F-11 | QA K-1/K-2; Lead |
| R-14 | Procurement lead time / country availability; Vega models must not be bought by mistake. | M | H | UNVERIFIED | Channel on day 1; exact-model list (QA §4.1) | QA K-3; Lead |
| R-15 | No named W0 owners / Android developer. | H | H | Fact (Plan L2) | F-04 | Lead |
| R-16 | Recruiting 12+ TV-owning testers × 14 days, plus 5 P6 users. | M | H | VERIFIED-DOC (rule) | Start in W0-1 | QA K-5; Lead |
| R-17 | Store rejection for piracy-adjacent features (sniffer/downloader copy); the A-21 policy text has not been fetched. | M | H | UNVERIFIED | W0-1 policy fetch; copy guardrails | MP G16; Lead |
| R-18 | WebView provider update or missing provider kills the app or makes construction throw. | M | M | UNVERIFIED | T-17; guarded engine creation | AA R-10 |
| R-19 | Privacy declarations inconsistent (Data safety vs policy vs WebView metrics / Safe Browsing). | M | M | VERIFIED-DOC (rules) | Single source of truth; P5 capture published | MP §7; AA §5.8 |
| R-20 | Android developer verification (2027 global) for the `nogms` / sideload channel. | M | M | VERIFIED-DOC (agent) | Register package names and keys early (F-31) | MP §7.5 |
| R-21 | Evidence fragmentation (broken paths) + empty `.gitignore` (APK/keystore commit risk, AGENTS.md §7). | M | H | Fact | Doc-hygiene PR (F-12) | QA K-4; AA §1.1; Lead |
| R-22 | Non-reproducible tests: unpinned URLs and builds, undefined P9 actions and P6 script. | H | M | Fact | Pinned-URL appendix, build hashes, artifacts (QA §2C, §5) | QA |
| R-23 | FR-41 baseline is a between-app comparison, so the ≥30% claim is weak. | M | M | STATIC | Founder accepts the method or adds a within-app re-baseline | QA R-12; PA §7.C |
| R-24 | No CI, so NFR-10 / FR-01 import rules cannot be enforced. | H | M | Fact | CI decision before W1 (F-28) | Lead; AA §2 |
| R-25 | ACRA needs a self-hosted server, which conflicts with "no backend". | M | L | STATIC | Decide before W13 | AA §8; Lead |
| R-26 | TV Bro licence terms if code is reused; brand closeness "TVBrowse" vs "TV Bro". | M | M | VERIFIED-DOC (licence) | F-13 | MP §3.3, Q7; Lead |
| R-27 | The D7/D30 retention metric may not be available for TV apps (A-29). | M | M | UNVERIFIED | Proxy KPI decision | PA §8.2 |
| R-28 | FR-48 web notifications may be unsupported by WebView. | M | L | UNVERIFIED | Doc check / spike | Lead (not covered by agents) |

---

## 7. W0 execution readiness

### 7.1 By track

| Track | Content | Readiness | Blocking items |
|---|---|---|---|
| **W0-1** Procure / verify / recruit | Official-source checks, device orders, landing page + 14-day clock, tester recruitment, Play account, **Amazon Developer Support case** | 🟡 **READY TO START on approval** | F-01, F-04, F-05, F-06, F-09; procurement channel (M-04) |
| **W0-2** Technical spike | D-2 set + AA gate-critical experiments | 🔴 **BLOCKED** | Devices; **harness (F-03)**; **missing protocols A-03/A-12/A-13/A-23 (F-11)**; P3 method fix; Android 16 TV image (M-15) |
| **W0-3** Demand / teardown / policy | D-1, TV Bro v3.1.0 teardown, policy A-items | 🟡 **PARTIAL.** Policy desk work can proceed | Waitlist from W0-1; D-1 thresholds + rubric (F-07); devices for hands-on A/B |
| **W0-4** Rework / v1.3 | v1.3 + D-4 | 🔴 **UNDEFINED on the D-1-pass path** | F-08 |

### 7.2 By protocol (QA §1.1 + AA §10 prerequisites)

| Protocol | Status now | Before it can run |
|---|---|---|
| P1 BT sleep | BLOCKED | D6 OEM TV + BT remote; split into repro vs FR-14 (C-29); harness; HDMI/CEC triggers (AA T-11) |
| P2 d3ward | BLOCKED (devices) | Pinned builds; A-05 decision rule |
| P3 30-min LMK | BLOCKED (harness) | **AA memory method** (C-16); step markers; pinned URLs |
| P4 chrome://crash | BLOCKED (harness) | **AA T-01 + T-03 first**; memory-pressure method; guard window |
| P5 first-run capture | BLOCKED (harness; BrowseHere step 3 needs only a device) | RE §6 endpoint table; attribution rule |
| P6 form baseline | BLOCKED (users) | Instruction script, form URLs, timing method |
| P7 SW interception | BLOCKED (harness; feature probe needs only a device) | — |
| P8 popups | BLOCKED (harness) | **5 test pages** + the FR-19/45 combined page; AA T-06 instance count |
| P9 D-pad walkthrough | BLOCKED (devices) | Defined "10 core actions" |
| P10 FGS video script | Desk task | Tie it to FR-31 (C-18) |

**Gate-critical experiments to add to W0-2** (AA §10; status: NOT TESTED): T-01 multiprocess, T-02 feature matrix, T-03 crash trigger, T-04 memory baseline, T-05 standby, T-06 popups, T-07 Back key, T-12 process-death restore, T-15 Safe Browsing mechanism, T-17 provider update, T-18 cold start. A-03 still needs a protocol (QA §2A).

### 7.3 Desk evidence gathered by the audit

All items accessed 2026-11-27. They move into `validation-results.md` only after founder approval.

| Item | Result | Verified by |
|---|---|---|
| A-11 target API | New apps/updates target **36** (from 31 Aug 2026); **Android TV ≥34** (existing ≥33) | Lead + MP |
| A-27 closed testing | **12 testers × 14 days** for personal accounts created after 13 Nov 2023. Organisation exemption UNVERIFIED | Lead + MP |
| A-20 Downloader | v2.0.2 (**30 Mar 2026**) stopped registering as a browser on Fire "due to Amazon policy" | Lead + MP |
| Amazon browser policy | "Amazon restricts the publication of third-party browser apps on Fire tablets and Fire TV devices…" | **Lead** + MP |
| A-25 Vega | Vega OS 2.0 GA **29 Oct 2026**; 4K Select `AFTCA002` and 4K 3rd Gen `AFTCR001` run Vega with 1 GB RAM | Lead + MP |
| Fire OS mapping | Fire OS 16 = API 35/36, 14 = API 31–34, 8 = API 29–30, 7 = API 28. Amazon minimum targetSdk per Fire OS version: 36 / 34 / 30 / 28 | Lead |
| TV PiP | Media playback is not allowed in PiP on TV; PiP on TV requires API 34+ and a category meta-data entry | **Lead** + MP |
| WebView multiprocess | AOSP default: 8.0 on; 8.1–10 off for 32-bit low-RAM; 11+ on | **Lead** + AA |
| A-07 Google OAuth | Embedded WebView is not allowed | Lead + MP |
| A-30 TV Bro | v3.1.0 (26 Nov 2026); 5M+ installs, 3.6★; native player, cosmetic ad-block, PIN profiles, DoH, phone UI, Send-to-TV | Lead + MP |
| A-17 filter licences | Permissive set ≈19,134 domains (STATIC count). Exclude MVPS, Dan Pollock, KADhosts (BY-SA) and yoyo.org (no licence) | Lead + MP |
| TV-ME memory | ≤280 MB total on 1 GB low-RAM TVs | MP (Lead did not re-check) |

### 7.4 Status-vocabulary mapping (proposal; resolves C-21)

| Current code | AGENTS.md status |
|---|---|
| ⏳ | NOT TESTED |
| 🔍 | NOT TESTED (with an "in progress" note) |
| ✅ | PASS (executed + raw log), or VERIFIED-DOC for desk items |
| ❌ | FAIL |
| ⚠️ | UNVERIFIED, or BLOCKED (state the blocker) |

A numeric claim without a raw log is downgraded to **UNVERIFIED** (QA §7.3).

---

## 8. Missing inputs

| ID | Input needed | Needed for | From |
|---|---|---|---|
| M-01 | Written approval statement for PRD/TECH_SPEC v1.2 (binding vs proposal) | C-01 | Founder |
| M-02 | Named W0 owners: Android developer, procurement, interviews, recruitment | W0-1 | Founder |
| M-03 | W0 budget (devices ≈$400–600, Play $25, domain) | W0-1 | Founder |
| M-04 | **Procurement country/channel** and lead times | Critical path | Founder |
| M-05 | Play Console account: exists? personal or organisation? | A-27 | Founder |
| M-06 | Landing page: brand, domain, form/counter service, privacy notice, copy | 14-day waitlist | Founder |
| M-07 | D-1 thresholds, differentiator rubric, interview coding rule | D-1 | Founder |
| M-08 | Harness authorisation + scope (`prototype/`, throwaway) | D-2 | Founder |
| M-09 | BrowseHere APK source (licensed) or a TCL Google TV device | P1/P2/P5 | Founder |
| M-10 | Recruitment channel (interviews, P6, closed test) | D-1, P6, W16 | Founder |
| M-11 | Licence model (closed vs OSS) | C-13, R-26 | Founder |
| M-12 | "Third review" source doc (+ optional PRD v1.0/v1.1) | C-32 | Founder |
| M-13 | CI platform; ACRA hosting decision | Before W1 / W13 | Founder |
| M-14 | **Amazon Developer Support response** on browser pre-clearance | C-09, F-15 | Founder (account owner) |
| M-15 | Android 16 (API 36) TV system image or device | T-07 / C-10 | Android developer |

---

## 9. Decisions requiring founder approval

### 9.1 Needed **to start W0** (now)

| ID | Decision | Lead recommendation (not authorised) | Agent refs |
|---|---|---|---|
| **F-01** | Accept this audit and authorise **W0 start**, W0-1 first | Approve | — |
| **F-02** | Binding status of PRD/TECH_SPEC v1.2 (C-01) | Product intent is binding; §11 items, NFR-07 SDK levels and Safe Browsing are **proposals** | PA §4.1 |
| **F-03** | Authorise an **isolated, throwaway harness** under `prototype/` for P1/P3/P4/P5/P7/P8 + AA gate-critical experiments. No code in `app/` | Approve. Without it D-2 cannot pass | QA K-1; AA §10 |
| **F-04** | Name W0 owners and the Android developer | — | — |
| **F-05** | Device budget + procurement channel (PRD decision 12) | ≈$400–600; borrow where possible; **no Vega models** | QA §4.1 |
| **F-07** | D-1 thresholds + differentiator rubric + interview coding (C-03) | ≥50 signups / 14 days; ≥8 interviews with ≥60% explicit intent; ≥3 differentiators each **demonstrated on a device or by a verifiable artifact** | PA Q2/Q3; MP Q1 |
| **F-09** | Landing-page service + email collection + privacy notice (C-28) | One minimal form service | — |
| **F-11** | **W0-2 scope (revised):** add protocols for A-03/A-12/A-13/A-23 and the gate-critical T-experiments; add A-16/A-33 to D-2; defer only A-04/A-06/A-14/A-15 | Approve | QA §2A; AA §10; X-04, X-08 |
| **F-12** | Authorise a **doc-hygiene PR** with no requirement changes: rewrite `W0/` paths → actual paths; evidence root `reports/W0/logs/`; README; `.gitignore`; status mapping §7.4; reference banner; source-code glossary; FR-42 "reserved" | Approve | PA §10.1–2; QA §3.1; AA D-gap-19 |
| **F-15** | **Amazon:** open a Developer Support case for browser pre-clearance. If refused, Fire OS becomes sideload-only and differentiator #6 is dropped | Approve the case now (desk task, W0-1) | MP Q2, §10.1 |

### 9.2 Needed during W0 (before the relevant gate)

| ID | Decision | Recommendation | Agent refs |
|---|---|---|---|
| F-06 | Play account type (PRD decision 7) | Decide before recruiting | PA Q9; MP Q5 |
| F-08 | Fixed v1.3 + D-4 window on both D-1 paths (C-04) | 3–5 working days after W0-3, always | — |
| F-10 | Licence model / F-Droid (C-13) | Decide; if closed, drop F-Droid | MP §10.4 |
| F-13 | TV Bro code policy + brand name | Study only, no reuse. Legal check of the name if it is kept | MP Q7 |
| F-14 | Filter strictness; exclude KADhosts and yoyo.org as well as the non-commercial lists | Exclude now; decide strictness after P2 | MP Q9 |
| F-16 | Remove media PiP + background video from the TV build (C-08) | Approve (policy is VERIFIED-DOC) | MP Q3 |
| F-17 | FR-43/NFR-06 policy for single-process devices (C-06) | Decide after T-01 | AA Q-01 |
| F-18 | minSdk: 24 / 26 / ≥30 (X-02) | Decide after T-01 + A-22 | PA Q4; AA Q-01(c); MP C4 |
| F-19 | Memory metric (PSS vs PMF/RSS) + device binding + TV-ME alignment (C-17) | Decide before the T-04 baseline is read | AA Q-13; MP G11; PA §5 |
| F-20 | Safe Browsing default + mechanism + WebView `MetricsOptOut` (C-19) | Decide after T-15 / A-18 | PA Q6; AA Q-06/Q-07; MP Q4 |
| F-21 | Wedge: confirm now or keep open until D-1 (C-02) | State it explicitly before interviews | PA Q1 |
| F-22 | targetSdk 36 confirmation (PRD decision 6). TV minimum is 34 | Keep 36 if F-25 is handled | PA Q5 |

### 9.3 Existing PRD §11 decisions (still open)

1. Name/brand (→ F-13)
2. Wedge (→ F-21)
3. v1 free
4. 16 weeks vs cut option
5. minSdk (→ F-18)
6. targetSdk (→ F-22)
7. Play account (→ F-06)
8. Vega port
9. Stream-sniffer scope (MP Q8; PA Q10)
10. Filter list (→ F-14)
11. FR-39 as P1 parity (MP confirms it is parity)
12. Device budget (→ F-05)
13. Safe Browsing (→ F-20)

### 9.4 v1.3 design decisions and spec-change queue (not needed to start W0; **nothing changed**)

| ID | Decision / change | Refs |
|---|---|---|
| F-23 | FR-31: add `RUN_USER_INITIATED_JOBS` or drop UIDT; API 26–28 destination; blob:/data: scope | C-05; AA Q-02/Q-08 |
| F-24 | FR-45 popup instance policy | C-07; AA Q-03 |
| F-25 | Back handling under targetSdk 36 | C-10; AA Q-05 |
| F-26 | Standby semantics + JS policy | C-15; AA Q-04 |
| F-27 | First-run privacy disclosure requirement | PA Q11 |
| F-28 | Multi-module layout + import rule; CI platform | AA Q-09; R-24 |
| F-29 | W1–2 vs W5–7 data dependency | C-14; AA Q-10 |
| F-30 | FR-56 scope (third-party OAuth only, or all Google sign-in) | C-26; AA Q-12 |
| F-31 | Register package names and signing keys for developer verification | MP Q6 |
| — | Other spec edits: §4 v1/v1.1 split (C-18) · FR-13 design note (C-12) · TV Bro facts (C-22) · S7 replacement (C-23) · roadmap phases and counts (C-27, C-34, C-35) · Fire OS 16 row (C-39) · complaint #21 limitation (C-40) · acceptance wording (C-41) · P3/P4/P1/P8 protocol fixes (C-16, C-29, C-30) · 16-week cut option and v1 free (PA Q7/Q8) · A-29 proxy KPI (PA Q12) · downloads in a separate process (AA Q-11, default no) | — |

---

## 10. Recommended execution sequence (proposal; nothing started)

| Step | When | What | Exit criterion |
|---|---|---|---|
| 0 | Day 0 | Founder decides §9.1 (F-01…F-05, F-07, F-09, F-11, F-12, F-15) and supplies M-01…M-10 | Signed decisions recorded |
| 1 | W0-1 day 1–2 | Doc-hygiene PR (F-12); status mapping; `reports/W0/logs/`; `.gitignore` | PR merged **by the founder** |
| 2 | W0-1 day 1 | **Start the clocks:** landing page (14-day waitlist), recruitment, device orders, **Amazon Developer Support case** | Page live; orders placed; case ID recorded |
| 3 | W0-1 | Protocol owner adds the A-03/A-12/A-13/A-23 protocols and fixes P1/P3/P4/P8 (QA §2A, §6; AA §10); QA authors the artifacts (test pages, pinned URLs, P9 action list, P6 script) | QA reviews the drafts before execution |
| 4 | W0-1 | Move desk evidence (§7.3) into `validation-results.md`; fetch the remaining policy pages (S3/S7/A-21/A-31/S5/A-28/A-29) | §C and §E filled in with URLs |
| 5 | W0-1 → W0-2 | Build the isolated harness (F-03); emulator smoke test API 26/30/34/36 including T-07 | Harness runs on an emulator (labelled emulator) |
| 6 | W0-2 | On devices: T-01 → P4; T-04 → P3; P1, P5, P7, P8, T-05/06/12/15/17/18; P2 and P9 on competitors; P6 | Raw logs in `reports/W0/logs/`; D-2 review |
| 7 | W0-3 | Interviews; TV Bro v3.1.0 vs BrowseHere 8.25 A/B on the same 1 GB device (#1/#2/#5); review mining with URLs; store-copy pre-review | D-1, D-2 and D-3 verdicts with evidence |
| 8 | v1.3 window (F-08) | PRD/TECH_SPEC v1.3 **from W0 results only**, plus the §9.4 queue | **D-4 founder sign-off → W1 may start** |

**Critical path:** founder decisions → procurement + missing protocols → harness → W0-2. The waitlist clock (14 days) and the Amazon case run in parallel from day 1.

---

## 11. Unresolved decisions and risks — tracking index

| Category | IDs | Count | Owner |
|---|---|---|---|
| Founder decisions (audit) | F-01…F-31 | 31 (10 needed to start W0) | Founder |
| Founder decisions (PRD §11) | 1–13 | 13 (8 overlap with F-) | Founder |
| Contradictions | C-01…C-42 | 42 (12 H / 21 M / 9 L) | Founder / Lead |
| Inter-agent tensions | X-01…X-10 | 10 | Lead → Founder |
| Risks | R-01…R-28 | 28 | Mixed |
| Missing inputs | M-01…M-15 | 15 | Founder |
| Errata on agent reports | E-1…E-10 | 10 | Lead (recorded) |

Each item is closed only with a reference to evidence or to a recorded founder decision.

---

## 12. What this audit did NOT do

- Did **not** write production or prototype code.
- Did **not** modify the PRD, TECH_SPEC, validation plan, protocols, `validation-results.md`, research docs, AGENTS.md, README, or any agent report.
- Did **not** start W0 or W1, and recorded **no** test, device, performance, memory or user result.
- Did **not** authorise scope changes. Every recommendation is a proposal.
- Did **not** merge any pull request.

**Files in this PR (#1):** `reports/project-audit/ASSIGNMENTS.md` and `reports/project-audit/PROJECT_AUDIT.md`.

---

**STOP.** The Lead is waiting for the founder's decisions in §9.1 before any W0 execution starts.
