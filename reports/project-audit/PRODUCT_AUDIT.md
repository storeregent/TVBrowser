# PRODUCT & DOCUMENTATION AUDIT — TVBrowse (TVBrowser)

**Document:** `PRODUCT_AUDIT.md`
**Prepared by:** Product & Documentation Analyst (Agent role: product/requirements audit only)
**Date of audit:** 2026-11-27
**Stage:** W0 validation (per `AGENTS.md` §4 — W1 coding blocked until W0 gates D-1…D-4 signed off)
**Documents reviewed:**
- `docs/product/PRD_v1.2.md`
- `docs/technical/TECH_SPEC_v1.2.md`
- `docs/validation/W0_Validation_Plan_v1.2.md`
- `docs/validation/validation-results.md`
- `docs/validation/protocols.md`
- `docs/research/procurement-and-sources.md`
- `docs/research/demand-and-competitor.md`
- `docs/history/CHANGELOG_v1.2.md`
- `reference/browsehere/*` (evidence base for the problem statement)
- `AGENTS.md`, `README.md`, `prototype/README.md` (empty)

> Scope note: This report audits **product requirements and project documentation only**. It does not write code, design a new product, add features, modify the approved PRD/TECH_SPEC, perform Android architecture analysis, or conduct market research belonging to another agent. Findings are problems/observations, not decisions. Where a decision is required, it is escalated to the Lead Agent / founder in §11.

---

## 0. Executive summary

The v1.2 document set is unusually disciplined for a pre-build stage: it separates PRD / TECH_SPEC / W0 plan, carries an explicit FR index with priority/phase/acceptance/source columns, a 28-complaint→FR traceability map, an assumptions register (33 rows), and a W0 protocol set. The P0=35 / P1=19 / P2=4 FR split is internally consistent and the legend/roadmap contradiction from v1.1 has been largely resolved.

However, the audit found **defects that should be corrected before W0 results are trusted and before v1.3 is produced**. The most material are:

1. **Open founder decisions have been pre-encoded as fixed requirements** in the TECH_SPEC (minSdk 26, targetSdk 36, Safe-Browsing default OFF), contradicting the PRD's own "Open decisions" list and the W0 gates that are meant to confirm them.
2. **The primary wedge is both "already decided" and "to be validated by D-1"** — a logical inconsistency that undermines the most important W0 gate. The D-1 GO/NO-GO thresholds are also still marked "founder confirm karein" and are therefore not yet defined.
3. **Cross-references are broken**: every `W0/...` path and every `../W0_...` relative link points to a `W0/` directory that does **not exist** in the repo; the working files actually live under `docs/validation/` and `docs/research/`. The CHANGELOG even claims "W0/validation-results.md khola gaya" but the file is at `docs/validation/validation-results.md`.
4. **The FR "Source" column uses an undefined code scheme** (`C#1`, `C#15A`, `E Q4`, `Review §2`, …) that cannot be traced to any artifact in the doc set.
5. **Several headline acceptance targets are provisional** ("W0 baseline ke baad final") — ad-block ≥70%, memory ≤250 MB/10 tabs, form-time ≥30% — so the product's core promises are currently unverifiable and dependent on baselines that may not be captured in W0 (esp. A-33 form-time baseline).

Severity legend used below: 🔴 Critical · 🟠 High · 🟡 Medium · 🟢 Low.

---

## 1. Document inventory

| # | File | Status | Role | Notes |
|---|------|--------|------|-------|
| 1 | `docs/product/PRD_v1.2.md` | Approved (W0-start) | Product requirements, goals, wedge, metrics, roadmap, risks, open decisions | Authoritative for product scope |
| 2 | `docs/technical/TECH_SPEC_v1.2.md` | Approved (W0-start) | 35 P0 + 19 P1 + 4 P2 FR tables, NFRs, architecture/ADRs, permissions allow-list, traceability | Authoritative for engineering requirements |
| 3 | `docs/validation/W0_Validation_Plan_v1.2.md` | W0-start | 3-week schedule, 33-row assumptions register, device matrix, official-source checklist, gates D-1…D-4 | Blocks W1 |
| 4 | `docs/validation/validation-results.md` | **Empty template** (all rows ⏳) | Master fill-in for W0 results | Not started |
| 5 | `docs/validation/protocols.md` | Draft protocol set | P1…P10 test scripts | References `W0/logs/` (non-existent) |
| 6 | `docs/research/procurement-and-sources.md` | Draft | Exact device models, official source URL master list | Solid; references `W0/...` paths |
| 7 | `docs/research/demand-and-competitor.md` | Draft | Interview guide, D-1 scorecard, TV Bro v3 teardown template | Good structure; depends on W0 execution |
| 8 | `docs/history/CHANGELOG_v1.2.md` | Approved | Item-by-item fix map v1.1→v1.2 | Useful; contains the "W0/..." claim |
| 9 | `reference/browsehere/BrowseHere_Reverse_Engineering.md` | Evidence | Static APK analysis (confidence "Medium — APK execute nahi hua") | Basis for problem evidence |
| 10 | `reference/browsehere/Users_Complaints_Regarding_BrowserHere_and_there_Solution.md` | Evidence | 28 complaints → 7 pillars | Inferential root causes ("Medium-Low") |
| 11 | `reference/browsehere/Better_Android_TV_Browser.md` | Evidence | 20 architecture answers (confidence "High") | Engineering baseline |
| 12 | `reference/browsehere/README.md` | Evidence | Reference app overview | — |
| 13 | `AGENTS.md` | Approved | Global agent instructions / golden rule | Correctly constrains scope |
| 14 | `README.md`, `prototype/README.md` | Empty | — | No content |
| 15 | `reports/W0/.gitkeep` | Empty | W0 report dir placeholder | — |

**Inventory gaps:** v1.0/v1.1 PRDs are referenced in the CHANGELOG but are **not present** in the repo (only summarized). The "W0/" working files referenced throughout are not present at the referenced paths. `app/`, `tests/`, `prototype/` are empty placeholders — expected at W0.

---

## 2. Requirement completeness

### 2.1 FR index vs. complaint coverage
- **FR count is internally consistent**: TECH_SPEC contains 58 requirement rows = **35 P0 + 19 P1 + 4 P2**. This matches the PRD §6 legend exactly. ✅
- **All 28 source complaints are traced** to at least one FR in TECH_SPEC §6. ✅
- **FR numbering has a gap at FR-42** (IDs run 01–56 with b-variants but 42 is absent). Not harmful, but it can confuse traceability and CHANGELOG references. 🟢
- **Not every FR traces to a user complaint.** Several P0 FRs (FR-01 single-activity shell, FR-02 cursor physics, FR-37 homepage, FR-48 permission prompts, FR-53 i18n, FR-56 honest sign-in message) originate from engineering/competitor analysis, not from the 28 complaints. This is legitimate, but see §2.3 — their "Source" column is **not resolvable**.

### 2.2 Partial coverage of a complaint in P0
- **Complaint #21** ("videos load na hona" due to missing stream headers) is only fully solved by **FR-25 (stream sniffer, P1)** and FR-26 (Open-with, P0). In P0 the path is user-provided URLs (FR-23). So #21 is **partially addressed in v1** and fully only in v1.1. This is an acceptable scope decision but should be **explicitly stated as a v1 limitation** in the PRD (currently only implicit via priority). 🟡

### 2.3 Traceability defect — undefined Source codes
The TECH_SPEC "Source" column uses tokens such as `E Q1`, `E Q4`, `C#15A`, `C#15B`, `C#28`, `Review §2`, `Review §1`, `C#17`, `C#6`, `A-01`, `S23`, etc.
- The **`S#`** tokens map to the official-source list in `procurement-and-sources.md` ✅.
- The **`A-##`** tokens map to the W0 assumptions register ✅.
- The **`C#…`** and **`E Q…`** tokens are **nowhere defined** in any document. There is no glossary mapping "C#15A" or "E Q4" to a complaint, research note, or design rationale.
- **`Review §2` / `Review §1`** point to prior review comments that are not part of the current v1.2 doc set.

**Impact:** A reviewer cannot verify *why* an FR exists or *what* evidence justifies it. This defeats the purpose of the Source column and weakens the auditability the doc set is otherwise good at. 🟠

---

## 3. P0 / P1 / P2 scope review

### 3.1 Separation is now correct
The v1.1 legend/roadmap contradiction is resolved: **no P1 item appears in the W1–W16 roadmap**, and all 19 P1 FRs are explicitly assigned to v1.1 (PRD §6, TECH_SPEC §1.5/§1.6 notes). The W1–W16 roadmap contains only the 35 P0 FRs. ✅

### 3.2 Roadmap load column off-by-one (minor)
PRD §8 "Tabs / data / compat" row lists **"P0 load = 11"** but enumerates only 10 distinct P0 items (FR-11…15 = 5, plus FR-29/30/33/49/50 = 5; "13 nav-history" is part of FR-13). Sum of all phases = 8+7+10+5+4+1 = 35, which is correct, so the **11** label is an arithmetic error, not a scope error. 🟢

### 3.3 P0 load vs. priority realism (observation, not a defect)
The roadmap front-loads Input (W3–4, 7 FRs) and Tabs/memory (W5–7, 10 FRs) — the riskiest, most device-dependent work — which is reasonable. However:
- **FR-43 (renderer recovery)** is placed in **W1–2 (Core shell)**, but its acceptance requires a 2-WebView crash test (P4) that depends on devices + the tab system from W5–7. Scheduling FR-43's *full* acceptance in W1–2 is optimistic; the *implementation* may start in W1–2 but verification belongs later. 🟡

### 3.4 FR-16 (P1) FGS declaration prepared during P0 window
PRD §8 W13–14 row lists "FGS declaration video" as a P0-phase deliverable. FR-16 (MediaSession/`mediaPlayback` FGS) is **P1**, while FR-31 (dataSync FGS fallback) is **P0**. The FGS declaration video is therefore presumably for FR-31, but the PRD text conflates it with FR-16's mediaPlayback type. The W13–14 window should explicitly separate the **P0 (FR-31/dataSync)** vs **P1 (FR-16/mediaPlayback)** FGS declarations so the P0 gate does not depend on a P1 feature. 🟡

---

## 4. Requirement contradictions

### 4.1 🔴 Open decisions encoded as fixed requirements (highest impact)
Three items are listed as **open founder decisions** in PRD §11 yet are written as **locked facts** in TECH_SPEC:

| Item | PRD position | TECH_SPEC position | W0 gate |
|------|--------------|--------------------|---------|
| **minSdk 26** | §11 #5 "minSdk 23→26" (open); §0 "minSdk 26 (proposal)" | NFR-07 "minSdk **26** (A-22)" stated as fact | A-22 (W0) "minSdk 26 par kitna install-base chhootta hai" |
| **targetSdk 36** | §11 #6 "targetSdk 36 Play Console me confirm" (open) | NFR-07 "targetSdk **36** [S1]" stated as fact | A-11 (W0) "targetSdk 36 / TV ≥34 official schedule" |
| **Safe Browsing default OFF** | §11 #13 explicit **open tradeoff** ("default OFF vs ON-with-disclosure … final after A-18") | NFR-11 "Safe Browsing opt-in/`gms`/default-**OFF**" stated as fact; Security §5 repeats "default OFF" | A-18 (W0) |

**Why this is a contradiction:** The PRD and W0 plan treat these as *pending confirmation*, but the TECH_SPEC — which is "authoritative for technical requirements" (AGENTS.md §2) — already bakes in the outcome. If the founder chooses targetSdk 34 (or 35), or turns Safe Browsing ON-with-disclosure, NFR-07 / NFR-11 / the Security §5 baseline and FR-37 acceptance would all need revision, and v1.3 would have to undo "decided" text. This is exactly the kind of silent pre-decision AGENTS.md §2 warns against ("Never assume that a suggestion is an approved requirement").

**Recommended handling:** Until the founder confirms, the TECH_SPEC should mark these as **provisional** (e.g., "minSdk 26 — *proposed, pending A-22/founder*") rather than canonical facts.

### 4.2 🔴 The wedge is both "decided" and "to be validated by D-1"
- PRD §0/§3/§5.2 state **Google TV + AOSP = PRIMARY, legacy Fire OS = SECONDARY** as settled fact.
- PRD §11 #2 lists "**Wedge confirm: Google TV + AOSP primary (recommended)**" as an *open* decision.
- `W0_Validation_Plan` §1 states "**Primary wedge pehle hi badal diya gaya hai**" (wedge already changed *before* D-1).
- Yet PRD §3 D-1 fail-path is "**wedge revise → W0-4 rework buffer**".

**Contradiction:** If the wedge was already changed before D-1, then D-1 cannot meaningfully *test* the wedge choice — it can only confirm demand for an already-selected wedge. The "wedge revise" branch of D-1 is therefore partially moot, and the PRD's framing of D-1 as the gate that could overturn the wedge is inconsistent with the plan's "already decided" stance. This should be reconciled: either (a) D-1 is explicitly *confirmation* of a pre-set wedge (and the "revise wedge" branch is reframed as "revise scope/positioning"), or (b) the wedge remains genuinely open until D-1. 🔴

### 4.3 🔴 D-1 GO/NO-GO thresholds are not yet defined
PRD §3: "**Go/No-Go D-1 (thresholds founder confirm karein):** ≥50 waitlist … **aur** ≥5/8 interviews … **aur** ≥3 verified differentiators …". The thresholds are explicitly flagged for founder confirmation but are **not** in the confirmed open-decisions list as *confirmed*. The W0 plan repeats the same numbers without founder sign-off.

**Impact:** The single most important gate in the project has a pass/fail line that is still provisional. W0 could "complete" and still lack an authorized GO/NO-GO criterion. The D-1 scorecard in `validation-results.md` §D has the thresholds pre-filled as if fixed. 🟠

### 4.4 🟠 FR-19 vs FR-45 boundary (apparently resolved, but untested edge)
The v1.2 split assigns FR-19 = cross-origin **redirect** guard (via `shouldOverrideUrlLoading`) and FR-45 = `window.open`/`target=_blank` **new-window** owner (via `onCreateWindow`), with both acceptance criteria asserting "no overlap / no duplicate". The boundary case — a **popunder that is also a cross-origin redirect**, or a new window that immediately redirects — is exactly where the two could both fire or both miss. The docs *assert* separation but provide **no test page for the combined case** in P8 (P8 lists same-origin open, cross-origin open, delayed popunder, `target=_blank`, redirect chain — separately, not the intersection). Residual ambiguity/risk remains. 🟠

### 4.5 🟡 FR-44 acceptance deferred to "Review §2 (v1.1)"
FR-44 (ServiceWorker interception, P0) has acceptance text "Review §2 (v1.1)" — i.e., the acceptance detail for a **P0** requirement is itself deferred to a v1.1 review that does not yet exist as a document. A P0 FR should have a defined, testable acceptance at approval time. 🟡

### 4.6 🟡 "Review §N" references in acceptance columns
Multiple FR acceptance cells cite `Review §1` / `Review §2` (FR-19, FR-45, FR-55, FR-47…54, FR-56, Section 4). These refer to prior review commentary not present in the v1.2 doc set, so the acceptance criteria are only partially self-contained. 🟡

---

## 5. Missing / weak acceptance criteria

| FR / NFR | Issue | Severity |
|----------|-------|----------|
| **FR-21** ad-block ≥70% | Acceptance "v1 ≥70% (**W0 baseline ke baad final**)" — target provisional; no fixed bar at approval | 🟠 |
| **NFR-01** memory ≤250 MB/10 tabs | "≤250MB idle with 10 tabs" has **no device qualifier**, while ≤400 MB is bound to "2GB device" and ≤300 MB peak to "1GB device". Ambiguous whether 250 MB applies to a 1GB device too (where it would be ~25% of RAM). Target also "W0 baseline ke baad final" | 🟠 |
| **FR-41** form-time ≥30% | Acceptance depends on a **W0 baseline (A-33)** that may be uncapturable without the app (see §7). If A-33 fails, this P0 acceptance is unverifiable | 🟠 |
| **FR-33** "Facebook readable" | "readable" is subjective; no objective metric (e.g., layout renders without horizontal scroll at 10-ft) | 🟡 |
| **FR-08** "URL/search ≤3 interactions" | "interaction" undefined (keypress? navigation step?) | 🟡 |
| **FR-37** "default speed-dial favicons APK me bundled" | "default" set size undefined; only the airplane-mode/0-call outcome is testable | 🟡 |
| **FR-17** "permissive-only subset (target ≤20k entries)" | The ≤20k cap is in the *notes*, not the acceptance column; acceptance lists "list size + RAM delta report" but no pass/fail threshold | 🟡 |
| **FR-55** "koi misleading claim" | "Misleading" is subjective; only store-copy review is named | 🟡 |
| **FR-48** geolocation "honest message" | Acceptance "Play TV devices filter na hon" is a side-constraint, not a direct test of the message behaviour | 🟢 |
| **NFR-05** crash rate ≤0.3% | Explicitly an *internal target* requiring post-launch Play vitals — not a launch-gate acceptance; acceptable but should be labelled as such everywhere (it is, mostly) | 🟢 |
| **FR-13** SPA scroll restore "approximate" | Acceptable as scoped, but "approximate" has no tolerance stated | 🟢 |

**Pattern:** The most important *differentiating* promises (ad-block score, memory budget, typing improvement) are the ones with the **weakest / provisional acceptance**, because they defer to W0 baselines. This is defensible *only if* W0 actually captures those baselines (see §7–§8).

---

## 6. Requirements that cannot be objectively tested

- **Differentiator hypotheses (PRD §5.1, demand-and-competitor §5):** "≥3 verified differentiators vs TV Bro" is the D-1 GO criterion, but "verified differentiator" has **no rubric** for what counts as a differentiator vs parity. Six hypotheses are listed, but the verdict column is free-text. This is inherently qualitative and currently unmeasured. 🟠
- **"Explicit switching intent" (D-1):** Defined only as "explicit 'haan, agar X ho' (vague interest nahi)". No inter-rater protocol for coding interviews → risk of confirmation bias in the GO call. 🟠
- **"Zero telemetry by default" / privacy promise:** NFR-11 tests *first-run* network calls (0). But "zero telemetry over the product lifetime" (opt-in analytics, ACRA sample, crash reporting) is **not** objectively testable at the requirement level — it depends on code paths that aren't specified as rejects. The promise is stronger than what NFR-11 measures. 🟡
- **FR-55 "honest ad-block copy" / FR-07 "D-pad se fully navigable" / FR-09 "Blue = new tab":** partially subjective; the first two at least name a test (store-copy review / TalkBack audit), the third ("Blue = new tab") is an example, not a criterion. 🟢
- **JTBD statement (PRD §1):** "TV par bina phone … baghair ads … mera data kisi server na jaye" — a vision statement, not a testable requirement. Fine as framing, but it should not be cited as a requirement. 🟢

---

## 7. Undocumented assumptions & dependencies

| # | Assumption / dependency | Where it lives | Risk if wrong |
|---|--------------------------|----------------|---------------|
| A | **Demand is unverified** — PRD §3 explicitly says pain points are "transferable" but "demand proved nahi". The entire 35-FR scope rests on D-1. | PRD §1/§3 | If D-1 fails, v1.3 redefines everything (acknowledged via W0-4) |
| B | **Platform strategy (Fire OS secondary, Vega excluded, Downloader de-registration) rests on 3 unverified official-source claims** (Vega 2.0 Oct 2026; Downloader v2.0.2 Mar 2026; target API 36). | W0 Plan §4, A-07/A-20/A-25/A-28 | If any official claim is mis-stated, §5.2 channel strategy is invalid |
| C | **Form-time baseline (A-33)** may be uncapturable in W0 without the app (proxy = competitor apps). | W0 Plan A-33 | FR-41's ≥30% acceptance becomes unverifiable → P0 acceptance gap |
| D | **Memory budget ≤250 MB/10 tabs** depends on A-16 (1 active+1 standby vs 2–3 live) and a W0 baseline not yet taken. | NFR-01, A-16 | Headline differentiator ("explicit RAM budget") may be infeasible on 1GB |
| E | **Ad-block ≥70%** depends on W0 d3ward baselines (A-05/A-26) not yet measured. | FR-21, A-05/A-26 | Differentiator claim unverifiable |
| F | **Device procurement lead time** — W0-2 technical spike needs physical devices; Chromecast discontinued, 2GB baseline must be sourced used. | W0 Plan §1/§5, A-32 | W0-2 slips if devices don't arrive in week 1–2 |
| G | **D-1 thresholds not founder-confirmed** (see §4.3). | PRD §3 | Gate pass/fail line undefined |
| H | **Reference doc drift:** `reference/browsehere/...Solution.md` (Pillar 7) still calls Fire TV a "Huge Opportunity" and primary-like, and lists Roku as targetable — contradicting PRD §5.2 (Fire OS secondary, Roku excluded). | reference doc vs PRD | Misleads anyone reading the evidence base; should be annotated as superseded |
| I | **"1–2 GB RAM" target** (PRD §2 goal 2) vs device matrix where the *low-RAM baseline* is 2GB and 1GB is a stress device. NFR-01's 250 MB idle target lacks a device qualifier (see §5). | PRD §2 / NFR-01 / procurement | Potential mismatch between stated RAM target and the memory budget's applicability |

---

## 8. W0 validation coverage of important product assumptions

**What W0 covers well:** D-1 (demand + differentiation), D-2 (technical root causes A-01/02/03/05/12/13/18/23/26), D-3 (policy/platform A-07/A-20/A-21/A-27/A-28/A-31), D-4 (scope freeze). The protocol set (P1–P10) is concrete and evidence-aware (raw logs required). The 33-assumption register is broad.

**Gaps / weaknesses in coverage of *important product assumptions*:**

1. 🟠 **The 35-FR P0 scope is not independently validated against demand.** W0 validates *demand existence* and *differentiators*, but there is no gate step that confirms the specific 35 P0 FRs are the right scope for the validated wedge. Traceability exists, but it is not part of any D-gate. If D-1 passes on a different wedge emphasis, the FR mix may not match.
2. 🟠 **Key success metrics may be unmeasurable (A-29).** PRD §7's "D7/D30 retained installers ≥25%" depends on a Play Console metric whose availability for TV apps is **unverified** (gate W15, A-29). A headline success metric could turn out to be uncollectable — yet it is presented as a target now.
3. 🟠 **Core promises have provisional bars (FR-21, NFR-01, FR-41).** W0 is supposed to *set* these baselines, but the form-time baseline (A-33) in particular may not be capturable, and the memory/ad-block baselines depend on devices arriving in time.
4. 🟡 **Root causes for complaints #4/#7/#18/#20/#23 (memory/LMK)** are addressed by *design* (FR-11…15) but the underlying assumption "BrowseHere fails because it keeps all WebViews alive" is inferential (complaints doc = "Medium-Low" confidence) and has **no dedicated W0 assumption ID** validating it. D-2 covers A-16 (standby count) but not the core LMK root cause directly.
5. 🟡 **First-run privacy promise (NFR-11) IS covered** by P5 (mitmproxy capture) — good. But there is **no requirement for a first-run privacy/consent disclosure**; "zero calls" is tested, yet some stores expect an explicit privacy notice. This is a potential store-policy gap not covered by W0 (only A-21 covers *piracy-adjacent* copy, not a general privacy notice).
6. 🟡 **Safe-Browsing tradeoff (A-18/D-13)** is in W0-2, but the *decision* is deferred to the founder (PRD §11 #13). W0 produces the evidence; the *product requirement* stays open. Acceptable, but the TECH_SPEC pre-decides it (see §4.1).
7. 🟢 **W0 schedule vs. scope is aggressive but acknowledged.** 33 assumptions + 4 gates + 5–8 interviews + 14-day waitlist + device procurement + TV Bro teardown in 3 weeks, with an optional W0-4 buffer. The plan explicitly notes shipping lead time for devices; the risk is W0-2 slipping if devices are late. This is a scheduling risk, not a documentation defect, but it should be flagged to the Lead Agent.

---

## 9. Product risks (from a requirements standpoint)

| Risk | Linked finding | Severity |
|------|----------------|----------|
| Founder signs off W0/D-1 using provisional thresholds → false GO | §4.3 | 🔴 |
| v1.3 has to undo "decided" minSdk/targetSdk/Safe-Browsing text | §4.1 | 🔴 |
| Wedge reframing inconsistency invalidates D-1 meaning | §4.2 | 🔴 |
| Core differentiators (ad-block %, memory, typing) ship with unverifiable acceptance | §5, §8.3 | 🟠 |
| FR-41 P0 acceptance unverifiable if A-33 baseline not captured | §7.C | 🟠 |
| Confused readers trust the `W0/` cross-references that don't resolve | §10 | 🟠 |
| "Privacy promise" stronger than what NFR-11 measures | §6 | 🟡 |
| Reference evidence doc contradicts current wedge (Fire OS/Roku) | §7.H | 🟡 |
| Store rejection risk from missing privacy disclosure (not in W0) | §8.5 | 🟡 |
| Roadmap FR-43 verification scheduled before its dependency exists | §3.3 | 🟡 |

---

## 10. Recommended corrections (documentation only — no PRD/TECH_SPEC edits without authorization)

These are *recommended* changes for the Lead Agent / founder to authorize; per AGENTS.md §8 I do not modify the approved docs myself.

1. **Fix cross-references (mechanical, low-risk).** Either (a) create the `W0/` directory and move `validation-results.md`, `protocols.md`, `procurement-and-sources.md`, `demand-and-competitor.md` into it and keep `W0_Validation_Plan_v1.2.md` referencing them, **or** (b) rewrite all `W0/...` and `../W0_...` references to the actual `docs/validation/` and `docs/research/` paths. Currently **every** `W0/...` link is broken and the CHANGELOG's "W0/validation-results.md khola gaya" claim is false. 🔴
2. **Add a Source-code glossary** defining `C#…` and `E Q…` tokens (map to complaint IDs / research notes / design rationale), or replace them with resolvable references (e.g., complaint `#14`, `research: Better_Android_TV_Browser.md §X`). 🟠
3. **Mark provisional decisions as provisional in TECH_SPEC** (minSdk 26, targetSdk 36, Safe-Browsing default OFF) until the founder confirms via A-22/A-11/A-18. Do not present them as canonical facts while PRD §11 keeps them open. 🔴
4. **Reconcile the wedge narrative** (§4.2): state explicitly whether D-1 *confirms* a pre-set wedge or *chooses* it; reframe the "wedge revise" branch accordingly. 🔴
5. **Get founder sign-off on D-1 thresholds** (§4.3) *before* W0 execution, and record the confirmed numbers in `validation-results.md` §D rather than pre-filling them as fixed. 🟠
6. **Define "verified differentiator" rubric** and an interview coding protocol for "explicit switching intent" so D-1 is objectively scoreable. 🟠
7. **Add device qualifiers to NFR-01** (state whether ≤250 MB/10 tabs applies to 1GB, 2GB, or both) and set the FR-21 / NFR-01 / FR-41 bars as *explicitly provisional-until-W0* with the exact W0 artifact that will finalize them. 🟠
8. **Annotate the `reference/browsehere/...Solution.md`** as superseded by PRD §5.2 (Fire OS secondary, Roku excluded) to prevent evidence drift. 🟡
9. **Add a first-run privacy-disclosure requirement** (or explicitly decide none is needed and note the store-policy rationale) so the "zero telemetry" promise is backed by a requirement, not just NFR-11's 0-call test. 🟡
10. **Close the FR-19/FR-45 intersection gap** with one combined test page in P8 (popunder-that-redirects / new-window-then-redirect). 🟡
11. **Fix the roadmap "11" → "10" label** in PRD §8 Tabs/data row. 🟢
12. **State complaint #21 as an explicit v1 limitation** (full fix in v1.1 via FR-25). 🟢

---

## 11. Questions requiring founder approval

1. **Wedge (PRD §11 #2):** Is Google TV + AOSP PRIMARY / legacy Fire OS SECONDARY *confirmed now*, or is it genuinely still open until D-1? (Drives the §4.2 reframing.)
2. **D-1 thresholds (PRD §3):** Please confirm the exact GO/NO-GO numbers — ≥50 waitlist/14d, ≥5/8 interviews with switching intent, ≥3 repeated pains, ≥3 verified differentiators. Should any be weighted or substituted (e.g., channel split on waitlist)?
3. **"Verified differentiator" definition:** What standard proves a differentiator vs parity? (TV Bro v3 already has companion app, DoH, profiles, player — several of our 6 hypotheses may fall to parity.)
4. **minSdk (§11 #5):** Confirm 26 (vs 23/24). What is the acceptable install-base loss from dropping <26?
5. **targetSdk (§11 #6):** Confirm 36 (vs 34/35) given the official schedule.
6. **Safe Browsing (§11 #13 / D-13):** Default OFF (privacy) or ON-with-disclosure (security)? This changes NFR-11, Security §5, and FR-37 acceptance.
7. **16 weeks vs cut-option (§11 #4):** Approve the full 16-week P0 scope, or invoke the cut-option (defer native player + full downloader to v1.1)?
8. **v1 free / no IAP (§11 #3):** Confirm v1 stays 100% free (Pro deferred to v2 with its own go/no-go).
9. **Play account type (§11 #7 / A-27):** Personal vs organization (affects the 12×14-day closed-test exemption).
10. **Stream sniffer / "download video" scope (§11 #9 / A-21):** How aggressively can FR-25/FR-31 be scoped before store-copy rejection risk? Confirm the banned-keyword list and the "Play in native player only, no download-stream button" guardrail.
11. **First-run privacy disclosure (new):** Required by policy, or rely on zero-network-calls + listing copy? (Not currently in any FR.)
12. **Success-metric measurability (A-29):** If the D7/D30 "retained installers" metric is unavailable for TV apps in Play Console, which proxy metric is acceptable as the retention KPI?

---

## 12. Appendix — exact broken references found

- `docs/product/PRD_v1.2.md` references `W0/demand-and-competitor.md`, `W0/procurement-and-sources.md`, `W0/protocols.md`, `W0/validation-results.md` (2–4 occurrences each).
- `docs/technical/TECH_SPEC_v1.2.md` references `W0/procurement-and-sources.md`, `W0/protocols.md`.
- `docs/validation/W0_Validation_Plan_v1.2.md` references `W0/validation-results.md` (5×), `W0/protocols.md`, `W0/procurement-and-sources.md`, `W0/demand-and-competitor.md`.
- `docs/validation/protocols.md` and `docs/validation/validation-results.md` use relative `../W0_Validation_Plan_v1.2.md` (would resolve to `docs/W0_Validation_Plan_v1.2.md`, which does not exist — the plan is in the *same* folder).
- `docs/research/demand-and-competitor.md` and `docs/research/procurement-and-sources.md` use `../W0_Validation_Plan_v1.2.md` (same broken relative path).
- `docs/history/CHANGELOG_v1.2.md` §7 claims "W0/validation-results.md khola gaya" — false; file is `docs/validation/validation-results.md`.
- **No `W0/` directory exists** in the repository.

**Net:** All inter-document navigation for the W0 working set is currently broken; a reader following the links lands on non-existent paths. This must be fixed before W0 execution relies on these documents.

---

*End of PRODUCT_AUDIT.md — Product & Documentation Analyst. No code written, no PRD/TECH_SPEC modified, no product decisions made. Awaiting Lead Agent direction.*
