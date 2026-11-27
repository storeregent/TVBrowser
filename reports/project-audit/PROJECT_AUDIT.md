# TVBrowse — PROJECT AUDIT (Consolidated)

**Prepared by:** Lead Orchestrator · **Date:** 2026-11-27 · **Repo state audited:** `main` @ `1660e6a` (branch `arena/55acd7be-tvbrowser`)
**Stage:** W0 validation. **W1 is blocked. W0 execution has NOT started.**
**Specialist reports (source of every finding below):**
- Agent 1 — Product & Docs → [`AGENT1_PRODUCT_DOCS_AUDIT.md`](AGENT1_PRODUCT_DOCS_AUDIT.md)
- Agent 2 — Android Architecture → [`AGENT2_ANDROID_ARCHITECTURE_AUDIT.md`](AGENT2_ANDROID_ARCHITECTURE_AUDIT.md)
- Agent 3 — QA & W0 Validation → [`AGENT3_QA_W0_VALIDATION_AUDIT.md`](AGENT3_QA_W0_VALIDATION_AUDIT.md)
- Agent 4 — Market, Competitor & Policy → [`AGENT4_MARKET_COMPETITOR_POLICY_AUDIT.md`](AGENT4_MARKET_COMPETITOR_POLICY_AUDIT.md)
- Assignment record and file ownership → [`ASSIGNMENTS.md`](ASSIGNMENTS.md)

> **How this audit was produced.** No separate agent runtime was available. The four specialist audits were run as **separate, scope-limited passes in the Lead session**, and each one wrote only its own file (see `ASSIGNMENTS.md` §1). Because of this, the reports did not cross-check each other independently. The founder may re-run the same briefs with independent agents.
>
> **Evidence labels used:** PASS / FAIL / BLOCKED / NOT TESTED / UNVERIFIED (AGENTS.md §6). In addition: **VERIFIED-DOC** = confirmed from an official public source on 2026-11-27 · **STATIC** = reasoning from documents or code, not executed. **No device, emulator, build or user test was run. No result in this audit is a test result.**

---

## 1. Overall project readiness

| Dimension | Rating | Basis |
|---|---|---|
| Product definition | 🟡 **AMBER** | PRD/TECH_SPEC are detailed and self-critical (35 P0 FRs, 12 NFRs, 13 ADRs). But the approval status is ambiguous and 13 founder decisions are still open (C-01, C-02). |
| Document consistency | 🟡 **AMBER** | 25 contradictions/defects: **6 High**, 11 Medium, 8 Low (§4). |
| Architecture | 🟢/🟡 **GREEN-AMBER** | The direction is sound and no change is recommended. There are 2 spec defects that would fail the project's own CI rule (C-05) and 4 high technical unknowns (§5). |
| W0 evidence | 🔴 **RED (none yet)** | 0/33 assumptions, 0/10 protocols, 0/4 gates. No devices, testers, waitlist or harness (Agent 3 §1). |
| W0 executability | 🟡 desk track / 🔴 device track | Desk and procurement work can start as soon as the founder approves. Device tracks are **BLOCKED** by procurement and by harness authorisation (§6). |
| Market / differentiation | 🔴 **RED-AMBER** | Desk evidence: TV Bro v3.1.0 (5M+ installs) already ships a native player, a cosmetic ad-blocker, profiles/DoH and a phone version. **0 of the 6 differentiators are confirmed; 2 are at risk** (Agent 4 §2). |
| W1 readiness | ⛔ **BLOCKED (correct)** | Gates D-1…D-4 are not signed. `app/` is empty. That is consistent with AGENTS.md §4. |

**Bottom line:** The project is **well documented but not yet validated**. It is **ready to start the W0-1 desk/procurement track** once the founder approves the inputs in §9. It is **not ready** for the W0-2 technical spike until devices arrive and an isolated validation harness is authorised. The biggest risk is **not technical**. It is whether there are **≥3 verifiable reasons to switch from TV Bro** (gate D-1).

---

## 2. Repository inventory (summary)

19 tracked files. Docs total about 146 KB and are mostly Roman Urdu/English. `README.md`, `prototype/README.md`, `reference/browsehere/README.md` and `.gitignore` are each **1 byte (empty)**. `app/`, `tests/` and `reports/W0/` contain only `.gitkeep`. No LICENSE file, no CI, no PRs or issues. Details: Agent 1 §1.

---

## 3. Requirements inventory

### 3.1 Counts (verified by recount)

| Class | Count | Note |
|---|---|---|
| P0 FR (v1, W1–W16) | **35** | Count is correct. Roadmap rows add up to 36 because FR-13 is counted twice (C-09). |
| P1 FR (v1.1) | 19 | FR-09/10/16/18/19/25/27/31b/34/35/36/38/39/41b/47/51/52/53b/54 |
| P2 FR (v2) | 4 | FR-20/28/32/40 (+ GeckoView gate, Vega port) |
| Deleted / undefined | FR-33b deleted · **FR-42 never defined** | Record FR-42 as "reserved". |
| NFR | 12 | NFR-01…12 |
| ADR | 13 | D-01…D-13 |
| W0 assumptions | 33 | **All NOT TESTED** |
| W0 protocols | 10 | **All NOT TESTED** |
| Gates | 4 | D-1 demand · D-2 technical · D-3 policy · D-4 scope freeze |
| Founder open decisions (PRD §11) | 13 | See §9 |

### 3.2 P0 requirement register with audit flags

| FR | Requirement (short) | Phase (TECH_SPEC) | W0 dependency | Audit flag |
|---|---|---|---|---|
| FR-01 | Single-Activity shell, `BrowserEngine` interface | W1–2 | — | — |
| FR-07 | HUD toolbar (no Pie Menu) | W1–2 | — | R-04 focus interop |
| FR-08 | Search-first URL entry, FTS suggestions | W1–2 | — | Whether remote suggestions exist is not specified (NFR-11 interplay) |
| FR-37 | Local homepage, bundled favicons | W1–2 | P5 | — |
| FR-43 | Renderer-crash recovery (all instances) | W1–2 | P4 | **R-02** single-process WebView on low RAM |
| FR-48 | Site permission prompts; no geolocation | W1–2 | — | **R-09** web notifications may be unsupported by WebView (UNVERIFIED) |
| FR-53 | No hardcoded strings | W1 | — | — |
| FR-56 | Google sign-in honest fallback | W1–2 | A-07 | Policy **VERIFIED-DOC**. Guard must also cover popups (Agent 2, A2-11) |
| FR-02 | 60fps virtual cursor | W3–4 | — | R-04 |
| FR-03 | Spatial/focus jump | W3–4 | — | R-04 |
| FR-04 | Auto-hide; never 2 cursors | W3–4 | — | — |
| FR-05 | Key normalisation | W3–4 | — | — |
| FR-06 | Back priority + context menu | W3–4 | — | **C-06** `goBack()` vs FR-13 custom history |
| FR-41 | Snippets/shortcuts (no IME) | W3–4 | A-33, P6 | A-33 is not in any gate (C-21) |
| FR-46 | Edge auto-scroll | W3–4 | — | — |
| FR-11 | TabManager, ≤2 live | W5–7 | A-16 | **C-07** standby wording |
| FR-12 | 1 active renders; eviction | W5–7 | P3, A-16 | **R-03** ≤250 MB unproven |
| FR-13 | Disk state + nav-history | W5–7 | A-13, A-23 | **C-06 / R-07** |
| FR-14 | `configChanges` lock | W5 | A-01, P1 | Root cause unverified |
| FR-15 | Grid tab switcher | W5–7 | — | — |
| FR-29 | Room/DataStore | W5–7 | — | — |
| FR-30 | Bookmark import/export | W5–7 | — | TV Bro already has it (parity) |
| FR-33 | Desktop UA, per-site profiles | W2 + W6 | — | **C-11** roadmap places it in W5–7 only |
| FR-49 | Clear browsing data | W5–7 | — | — |
| FR-50 | History UI | W5–7 | — | — |
| FR-17 | Permissive-only bundled hosts (≤20k) | W8–9 | A-17 | Permissive union measured at **≈19,134** domains (STATIC). **R-05** |
| FR-21 | d3ward ≥70% | W0 / W8–9 | A-05, A-26, P2 | **R-05** no baseline |
| FR-44 | Service-worker interception | W8–9 | P7 | — |
| FR-45 | Sole `window.open` owner | W8–9 | P8 | 800 ms window must be tracked by the app |
| FR-55 | Honest ad-block copy | W13–16 | A-21 | **C-10** roadmap says W8–9 |
| FR-22 | Fullscreen custom view | W10–12 | — | — |
| FR-23 | Media3 player, both flavors | W10–12 | — | Differentiator #3 **at risk** |
| FR-24 | Subtitles | W10–12 | — | — |
| FR-26 | "Open with" | W10–12 | — | — |
| FR-31 | Downloader: UIDT/WorkManager, in-app progress | W13–14 | A-31 | **C-05** `RUN_USER_INITIATED_JOBS` missing from allow-list |

---

## 4. Contradiction register

Severity: **H** = blocks a gate or would fail build/CI/policy · **M** = causes wrong work or broken traceability · **L** = editorial.
**Nothing in this register has been changed in the documents.** Fixing PRD/TECH_SPEC items needs founder authorisation (planned for v1.3).

| ID | Contradiction | Exact references | Impact | Sev | Proposed resolution (needs approval) | Source |
|---|---|---|---|---|---|---|
| C-01 | **"Approved" vs open.** AGENTS.md says PRD/TECH_SPEC are "approved". The PRD lists 13 open decisions, and NFR-07/NFR-11 state open decisions as fact. There is no sign-off record. | AGENTS.md L24, L74; PRD L3, L20, L215; TECH_SPEC L134, L138 | Agents cannot tell what is binding. | **H** | Founder records an approval statement: which sections are binding and which are proposals. | A1-06/07 |
| C-02 | **W0-4 "optional" vs required.** D-4 (scope freeze → W1) and v1.3 are "at W0-4 end", but W0-4 runs only if D-1 fails. | Plan L18, L77, L115; PRD L21, L159 | No W1 entry slot if D-1 passes. | **H** | Define a fixed v1.3 + D-4 window on both paths. | A1-08 |
| C-03 | **D-1 "5–8 interviews" vs "≥5/8".** | PRD L73, L79; Plan L74 | The gate result could be disputed. | **H** | Use a ratio rule (for example ≥60% of ≥8) and confirm the thresholds. | A1-09 |
| C-04 | **F-Droid as a PRIMARY channel vs a "closed" product.** | PRD L116; TECH_SPEC L206; demand L47 | One of the two cannot hold. | **H** | Choose the licence model, or drop F-Droid. | A1-10, A4-01 |
| C-05 | **FR-31 UIDT needs `RUN_USER_INITIATED_JOBS` + a notification, which §4 / NFR-10 would block.** It is also API 34+ only. | TECH_SPEC L94, L176–198, L137 | Fails the spec's own CI rule. On API <34 devices the fallback becomes the main path. | **H** | v1.3: add the permission + `JobService`, or redesign FR-31. | A2-01 (VERIFIED-DOC) |
| C-06 | **FR-13 own nav-history (Bundle never persisted) vs FR-06 `goBack()`.** | TECH_SPEC L51, L57 | Back/forward after process death needs a custom history engine that is not specified. | **H** | Short FR-13 design note before W5. | A2-04 |
| C-07 | FR-11 "live standby" vs FR-12 "rest `destroy()`" vs NFR-01 "1 active + 1 standby". | TECH_SPEC L55–56, L128 | Ambiguous memory design. | M | Resolve through A-16 and add A-16 to D-2. | A1-15, A2-05 |
| C-08 | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (FR-16, **P1**) is in the v1 allow-list. | TECH_SPEC L60, L181 | Unneeded FGS declaration and review risk in v1. | M | Split §4 into v1 / v1.1. | A2-02 |
| C-09 | Roadmap P0 load = 36 (W5–7 says 11, actual 10). | PRD L165, L175 | Planning arithmetic. | L | Correct to 10. | A1-01 |
| C-10 | FR-55 phase: TECH_SPEC W13–16 vs roadmap W8–9. | TECH_SPEC L74; PRD L166 | Ownership. | L | Pick one. | A1-02 |
| C-11 | FR-33 phase: TECH_SPEC W2 + W6 vs roadmap W5–7 only. | TECH_SPEC L97; PRD L163–165 | Desktop UA missing during W1–4. | M | Add FR-33 defaults to W1–2. | A1-03 |
| C-12 | `W0/...` paths and `W0/logs/` don't exist. `../W0_Validation_Plan` links are broken. | PRD L222–227; Plan L3, L111; protocols L2–3, L80; demand L22 | Evidence filed inconsistently. | M | Doc-hygiene PR; evidence in `reports/W0/logs/`. | A1-04 |
| C-13 | The "third review" source and PRD v1.0/v1.1 are cited but missing. | TECH_SPEC Source column; PRD L5 | About 15 FRs cannot be traced to a source. | M | Add the review doc to `docs/history/`. | A1-05 |
| C-14 | Waitlist: "static page" vs "server-side counter" + email field. | demand L11–14 | Needs a new service and a privacy notice. | M | Founder picks a service. | A1-13, A4-02 |
| C-15 | PSS definition: PRD omits the filter set; NFR-01 includes it. | PRD L144; TECH_SPEC L128 | Measurements not comparable. | L | Use the NFR-01 definition. | A1-14 |
| C-16 | Safe Browsing cited as "D-6"; it is decision 13 / ADR D-13. | PRD L207 | Cross-reference error. | L | Fix the reference. | A1-11 |
| C-17 | Status codes ⏳/✅/❌ vs AGENTS.md PASS/FAIL/BLOCKED/NOT TESTED/UNVERIFIED. | validation-results L5; AGENTS.md L104–110 | Evidence integrity. | M | Adopt the mapping in Agent 3 §2. | A1-17, A3 §2 |
| C-18 | "TV Bro `minAPI 26`" used as the D-11 rationale. The actual WebView build is **minAPI 24**. | TECH_SPEC L167; procurement L77 | Weakens the minSdk 26 argument. | M | Correct it in v1.3; feed into decision 5. | A4 §1.2 (VERIFIED-DOC) |
| C-19 | DoH and PIN profiles dated to "v3.0.x". They actually shipped in **v3.1.0 (2026-11-26)**. | PRD L16, L108; Plan A-30 | Minor factual error. | L | Correct it in v1.3. | A4 §1.2 |
| C-20 | The Play "ad-block browsers allowed" claim rests on a 2016 secondary article (S7). | PRD L121; procurement L65 | Policy claim has no official source. | M | Replace with current official policy (W0-1). | A4 §1.2 |
| C-21 | A-16 and A-33 are W0-2 items but not in any gate. | Plan L49, L66, L75 | FR-11/12/41 could enter W1 unvalidated. | M | Add both to D-2. | A1-12 |
| C-22 | Safe Browsing "default OFF" is written as fact while decision 13 is open. WebView turns Safe Browsing on by default on GMS devices unless the app disables it. | TECH_SPEC L138, L202; PRD L215 | NFR-11 could fail without an explicit manifest flag. | M | Founder decides; v1.3 makes the per-flavor setting explicit. | A1-07, A2-09 |
| C-23 | Reference docs contain superseded claims ("95%+", `; wv` removal, OpenSubtitles, Google sync, APK installer). | `reference/browsehere/Users_Complaints…md` | Agents could treat them as requirements. | L | Add a "non-authoritative" banner to the reference README. | A1 §4 |
| C-24 | NFR-06/P3 "0 kills" vs the deliberate `chrome://crash` step in the same session. | TECH_SPEC L133; protocols P3 | Ambiguous pass rule. | L | Define it as "0 app-process kills/exits". | A3 §3 |
| C-25 | The procurement Fire OS table omits **Fire OS 16 (API 35/36)**. | procurement §1 | Incomplete device matrix. | L | Add it in W0-1. | A4 §3 |

---

## 5. Contradictions **between** specialist findings (Lead review)

| ID | Tension | Agents | Lead assessment |
|---|---|---|---|
| X-01 | **minSdk:** Agent 2 favours 26 (FR-43 `onRenderProcessGone` is API 26+). Agent 4 shows the competitor's WebView build reaches API 24, and minSdk 26 drops Fire OS 5/6. | A2 vs A4 | A genuine trade-off between reach and complexity. **Founder decision 5**, informed by A-22. Neither agent is wrong. |
| X-02 | **Ad-block:** Agent 4 measured the permissive-only set at ≈19.1k, which fits the FR-17 ≤20k cap and Agent 2's memory view. The same set drops the three largest ad-server lists, and Agent 2 says d3ward ≥70% has no evidence behind it. | A2 vs A4 | FR-17 licence purity and the FR-21 target pull in opposite directions. **Founder decision 10 / A-17**, decided only after the P2 baseline. |
| X-03 | **Differentiators:** the PRD lists 6. Agent 4's desk view confirms 0 (2 at risk). Agent 2 says #2 (resilience) is real engineering but invisible to users. | A1/A2/A4 | The agents agree rather than contradict each other: **D-1 is the dominant project risk.** The founder should expect W0-3 to sharpen the wedge. |
| X-04 | **Recording evidence:** Agent 4 has VERIFIED-DOC answers for Validation Plan §C (3 claims) and part of §E. Agent 3 says no W0 artifact may be filled in before approval. | A3 vs A4 | **Lead ruling:** the evidence stays in the audit report. It is moved into `validation-results.md` only after the founder approves W0 start. |
| X-05 | **W0-2 scope:** Agent 2 wants to *add* spikes (Compose↔WebView focus, single-process WebView check, A-16). Agent 3 wants to *defer* P1-only assumptions (A-03/04/06/14/15) so the 1-week spike fits. | A2 vs A3 | These fit together as a **scope swap**. Founder approval is needed because it changes the W0 plan (F-11). |
| X-06 | **Competitor privacy claim:** TV Bro's `PRIVACY.md` says no collection; its Play Data-safety label says "may collect App activity / performance". | A4 (internal to the evidence) | Differentiator #4 stays **UNVERIFIED** until a hands-on network capture (P5) is done. |

---

## 6. Technical and project risk register

L = likelihood, I = impact (H/M/L). Status: all **OPEN**.

| ID | Risk | L | I | Evidence status | Mitigation / closing evidence | Owner |
|---|---|---|---|---|---|---|
| R-01 | **D-1 fails: fewer than 3 verifiable differentiators vs TV Bro v3.1** | H | H | Desk: 0/6 confirmed (A4) | W0-3 hands-on teardown + interviews; prepare a wedge-revision option | Founder / Agent 4 |
| R-02 | WebView runs single-process on low-RAM devices, so FR-43 cannot help | M | H | UNVERIFIED | P4 step 1 on 1 GB device | Agent 2 / 3 |
| R-03 | NFR-01 ≤250 MB (10 tabs, renderer + GPU + filter) not reachable | M-H | H | UNVERIFIED | P3 with harness → v1.3 target | Agent 2 / 3 |
| R-04 | Compose-for-TV ↔ WebView D-pad focus / cursor-overlay interop | M | H | UNVERIFIED; **no spike in plan** | Add a W0-2 spike (F-11) | Agent 2 |
| R-05 | d3ward ≥70% not reachable with a permissive-only ≈19k hosts list | M-H | H | UNVERIFIED | P2 baseline + list experiment | Agent 2 / 4 |
| R-06 | FR-31 spec gap (permission / notification / API 34) | H | M | VERIFIED-DOC | v1.3 spec fix | Agent 2 |
| R-07 | FR-13 custom history complexity (SPA, Back semantics) | H | M | STATIC | Design note before W5; A-13 | Agent 2 |
| R-08 | Safe Browsing / WebView makes first-run calls (NFR-11) | M | M | STATIC | Explicit manifest flag; P5 | Agent 2 / 3 |
| R-09 | FR-48 web notifications not supported by WebView | M | L | UNVERIFIED | Doc check / spike | Agent 2 |
| R-10 | APK >20 MB (NFR-08) | L | L | UNVERIFIED | First build | Agent 2 |
| R-11 | Store rejection for Persona A (sideload / `.m3u8` / downloader) | M | H | UNVERIFIED (A-21) | Copy pre-review; FR-25/31 guardrails | Agent 4 |
| R-12 | Play TV quality review requirements not confirmed (S5 page changed) | M | M | UNVERIFIED | W0-1 official check | Agent 4 |
| R-13 | Licence: TV Bro code reuse conditions; CC BY attribution; NC lists | M | M | VERIFIED-DOC | Founder policy (F-13, F-14) | Founder |
| R-14 | Procurement lead time / country availability of named devices | M | H | UNVERIFIED | Pick channel day 1; borrow fallbacks | Founder / Agent 3 |
| R-15 | Recruiting 12+ TV-owning testers kept opted in for 14 days | M | H | VERIFIED-DOC (requirement) | Start recruitment in W0-1 | Founder |
| R-16 | No named W0 owners / Android developer | H | H | Fact (Plan L2) | F-04 | Founder |
| R-17 | Harness not authorised, so D-2 is BLOCKED | H | H | Fact (AGENTS.md §4) | F-03 | Founder |
| R-18 | Amazon channel shrinking (Vega on all new sticks; browser registration restricted) | H | M | VERIFIED-DOC | Fire OS stays secondary (already in PRD) | — |
| R-19 | ACRA self-hosting needs a server, owner and privacy notice ($0 assumed) | M | L | STATIC | Decide before W13 | Founder |
| R-20 | No CI, so NFR-10 (manifest allow-list) and FR-21 CI runs cannot be enforced | H | M | Fact | CI decision before W1 | Lead |
| R-21 | Empty `.gitignore`: risk of committing an APK, keystore or `local.properties` (forbidden by AGENTS.md §7) | M | H | Fact | Doc-hygiene PR (F-12) | Lead |

---

## 7. W0 execution readiness

### 7.1 By track

| Track | Content | Readiness | Blocking items |
|---|---|---|---|
| **W0-1** Procure / verify / recruit | Official-source checks, device orders, landing page + 14-day clock, tester recruitment, Play account | 🟡 **READY TO START on approval** | F-01, F-04, F-05, F-06, F-09; procurement channel (M-04) |
| **W0-2** Technical spike | A-01/02/03/05/12/13/18/23/26 + P1–P5, P7, P8 | 🔴 **BLOCKED** | Devices (none held); **harness authorisation (F-03)**; scope swap (F-11) |
| **W0-3** Demand / teardown / policy | D-1 (waitlist, interviews, review mining), TV Bro teardown, policy A-items | 🟡 **PARTIAL.** Policy desk work can proceed; D-1 depends on W0-1. | Waitlist must start in W0-1; interviewees; devices for hands-on teardown; D-1 thresholds (F-07) |
| **W0-4** Rework / v1.3 | v1.3 + D-4 | 🔴 **UNDEFINED on the D-1-pass path** | F-08 |

### 7.2 By protocol (from Agent 3 §3)

| P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 | P9 | P10 |
|---|---|---|---|---|---|---|---|---|---|
| BLOCKED (devices + harness) | BLOCKED (devices; BrowseHere source) | BLOCKED (harness) | BLOCKED (harness) | BLOCKED (harness; HTTPS-capture limits) | BLOCKED (users) | BLOCKED (harness) | BLOCKED (harness + 2-origin hosting) | BLOCKED (devices) | N/A in W0 |

**Executable without our own code:** P2, P6, P9 and the BrowseHere part of P5, once devices and users exist.

### 7.3 Desk evidence already gathered by the audit (VERIFIED-DOC, 2026-11-27; to be moved to `validation-results.md` only after approval)

| Item | Result |
|---|---|
| A-11 / Plan §4: target API | New apps must target **36** from Aug 31 2026; **Android TV apps ≥34**. Extension window ended Nov 1 2026. |
| A-27 / S2: closed testing | **12 testers × 14 days** for personal accounts created after Nov 13 2023. |
| A-20 / Plan §4: Downloader v2.0.2 | **Mar 30 2026**, "Due to Amazon policy… no longer registers itself as a web browser" on Fire TV/tablets. |
| A-25 / Plan §4: Vega 2.0 | GA **Oct 29 2026**; rolled out to all Vega Fire TV devices. 4K Select (2025) = `AFTCA002`, Vega OS 2.0, 1 GB. |
| A-07: Google OAuth | Official: do not use embedded webviews "such as WebView on Android". |
| A-30: TV Bro | v3.1.0 (2026-11-26); feature list in Agent 4 §1. |
| A-17: filter licences | Permissive set ≈19,134 domains; MVPS / Pollock / yoyo / KADhosts excluded. |

---

## 8. Missing inputs

| ID | Input needed | Needed for | From |
|---|---|---|---|
| M-01 | Written approval statement for PRD/TECH_SPEC v1.2 (binding vs proposal) | Everything (C-01) | Founder |
| M-02 | Named W0 owners: Android developer, procurement, interviews, recruitment | W0-1 start | Founder |
| M-03 | W0 budget approval (devices ≈$400–600, Play $25, domain) | W0-1 | Founder |
| M-04 | **Procurement country/channel** and shipping lead times | W0-1 → W0-2 critical path | Founder |
| M-05 | Play Console account: does it exist, and is it personal or organisation (decision 7) | A-27, closed-test plan | Founder |
| M-06 | Landing page: name/brand, domain, hosting + form/counter service, privacy notice, approved copy | 14-day waitlist clock | Founder |
| M-07 | Confirmed D-1 thresholds and interview-count rule | D-1 | Founder |
| M-08 | Harness authorisation + scope (`prototype/`, throwaway) | D-2 | Founder |
| M-09 | BrowseHere APK source (licensed) or a TCL Google TV device | P1/P2/P5 | Founder |
| M-10 | Recruitment channel for interviewees, P6 users and closed-test testers | D-1, P6, W16 | Founder |
| M-11 | Licence model (closed vs OSS): decides F-Droid, TV Bro reuse | C-04, R-13 | Founder |
| M-12 | "Third review" source document (+ optional PRD v1.0/v1.1) | Traceability (C-13) | Founder |
| M-13 | CI platform choice and ACRA hosting decision | Before W1 / W13 | Founder |

---

## 9. Decisions requiring founder approval

### 9.1 New decisions raised by this audit (needed to start W0)

| ID | Decision | Recommendation (Lead; not authorised) | Urgency |
|---|---|---|---|
| **F-01** | Accept this audit and authorise **W0 start** (W0-1 first) | Approve | **Now** |
| **F-02** | State the binding status of PRD/TECH_SPEC v1.2 (C-01) | Product intent binding; open items in §11 = proposals | **Now** |
| **F-03** | Authorise an **isolated, throwaway validation harness** under `prototype/` limited to P1/P3/P4/P5/P7/P8 + focus spike. No production code in `app/`. | Approve (otherwise D-2 cannot pass) | Now |
| **F-04** | Name W0 owners and the Android developer | — | Now |
| **F-05** | Device budget + procurement channel (merges PRD decision 12) | Approve ≈$400–600; borrow where possible | Now |
| **F-06** | Play account type (PRD decision 7) | Decide before recruiting | W0-1 |
| **F-07** | D-1 thresholds + interview rule (C-03) | ≥50 signups; ≥8 interviews with ≥60% explicit intent; ≥3 verified differentiators | W0-1 |
| **F-08** | Fixed v1.3 + D-4 window on both D-1 paths (C-02) | 3–5 working days after W0-3, always | W0-1 |
| **F-09** | Landing-page service + email collection + privacy notice (C-14) | Approve one minimal form service, or a mailto/issue-based list | W0-1 day 1 |
| **F-10** | Licence model / F-Droid channel (C-04) | Decide; if closed, drop F-Droid and use GitHub releases | Before v1.3 |
| **F-11** | W0-2 scope swap: add focus/single-process/A-16 spikes; defer A-03/04/06/14/15 (X-05) | Approve | Before W0-2 |
| **F-12** | Authorise a **doc-hygiene PR** with no requirement changes: paths, README, reference banner, `.gitignore`, status mapping, evidence folder | Approve | W0-1 |
| **F-13** | Policy on TV Bro code (study only vs attributed reuse) | Study only unless legal review | Before W1 |
| **F-14** | Filter strictness vs effectiveness (X-02; PRD decision 10) | Defer until the P2 baseline | W0-3 |

### 9.2 Existing PRD §11 decisions (still open)

1 Name/brand · 2 Wedge confirm (Google TV + AOSP primary; evidence supports Fire OS as secondary) · 3 v1 free · 4 16 weeks vs cut option · 5 minSdk 23/24/26 (X-01) · 6 targetSdk 36 (TV minimum is 34, VERIFIED-DOC) · 7 Play account type (=F-06) · 8 Vega port · 9 Stream-sniffer scope · 10 Filter list (=F-14) · 11 FR-39 as P1 parity · 12 Device budget (=F-05) · 13 Safe Browsing default (C-22).

### 9.3 Spec changes to queue for v1.3 (to be authorised later; **not made**)

FR-31 permission/design (C-05) · §4 split into v1/v1.1 (C-08) · FR-48 notifications (R-09) · FR-13 history design (C-06) · Safe Browsing manifest per flavor (C-22) · FR-11/12 standby wording (C-07) · roadmap count / FR-33 / FR-55 phases (C-09/10/11) · TV Bro facts (C-18/19) · S7 replacement (C-20) · A-16/A-33 into D-2 (C-21) · Fire OS 16 row (C-25).

---

## 10. Recommended execution sequence (proposal; nothing started)

| Step | When | What | Exit criterion |
|---|---|---|---|
| 0 | Day 0 | Founder decides F-01…F-09 (§9.1) and supplies M-01…M-10 | Signed decisions recorded |
| 1 | W0-1 day 1–2 | Doc-hygiene PR (F-12); adopt status mapping; create `reports/W0/logs/` | PR reviewed and merged **by the founder** |
| 2 | W0-1 day 1 | **Start the clocks:** landing page live (14-day waitlist), tester + interviewee recruitment, device orders | Page URL live; orders placed |
| 3 | W0-1 | Move VERIFIED-DOC audit evidence (§7.3) into `validation-results.md`; close the remaining desk items (S4/S5/S3b/S30/A-28/A-29/A-20/A-21/S7) | §C and most of §E filled with URLs |
| 4 | W0-1 → W0-2 | Build the isolated harness (if F-03), smoke-test it on emulators API 26/30/34/36 | Harness runs P4/P8 pages on an emulator (labelled emulator) |
| 5 | W0-2 | On devices: P1, P3, P4, P5, P7, P8 + focus spike + single-process check; P2 and P9 on competitors; P6 baseline | Raw logs in `reports/W0/logs/`; D-2 review |
| 6 | W0-3 | Interviews, TV Bro v3.1 hands-on teardown, review mining, store-copy pre-review | D-1, D-2, D-3 verdicts with evidence |
| 7 | v1.3 window (F-08) | PRD/TECH_SPEC v1.3 **from W0 results only** + §9.3 queue | **D-4 founder sign-off → W1 may start** |

**Critical path:** founder decisions → procurement → harness → W0-2. The waitlist clock (14 days) runs in parallel and must start in week 1.

---

## 11. Unresolved decisions and risks — tracking index

| Category | IDs | Count | Owner |
|---|---|---|---|
| Founder decisions (new) | F-01…F-14 | 14 | Founder |
| Founder decisions (PRD §11) | 1–13 | 13 (4 overlap with F-) | Founder |
| Contradictions | C-01…C-25 | 25 (6 H) | Founder / Lead |
| Inter-agent tensions | X-01…X-06 | 6 | Lead → Founder |
| Risks | R-01…R-21 | 21 | Mixed |
| Missing inputs | M-01…M-13 | 13 | Founder |

The Lead will keep this index current in later rounds. Each item is closed only with a reference to evidence or to a recorded founder decision.

---

## 12. What this audit did NOT do

- Did **not** write production or prototype code, or create `app/` content.
- Did **not** modify PRD, TECH_SPEC, the validation plan, protocols, `validation-results.md`, research docs, AGENTS.md or README.
- Did **not** start W0 or W1, and recorded **no** test, device, performance, memory or user result.
- Did **not** authorise scope changes. All recommendations above are proposals.
- Did **not** open or merge any pull request.

**Files created by this audit:** `reports/project-audit/{ASSIGNMENTS.md, AGENT1_PRODUCT_DOCS_AUDIT.md, AGENT2_ANDROID_ARCHITECTURE_AUDIT.md, AGENT3_QA_W0_VALIDATION_AUDIT.md, AGENT4_MARKET_COMPETITOR_POLICY_AUDIT.md, PROJECT_AUDIT.md}`.

---

**STOP.** The audit is delivered. The Lead is waiting for the founder's decisions (§9.1, at minimum F-01…F-05) before any W0 execution starts.
