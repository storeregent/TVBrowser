# Agent 1 — Product & Documentation Audit

**Role:** Agent 1 (Product & Documentation) · **Date:** 2026-11-27 · **Method:** static reading of the repository at commit `1660e6a`. No code was run. No external sources were used (those belong to Agent 4).
**Evidence labels:** findings are **static document analysis**. Each finding has a `file:line` reference.

---

## 1. Repository inventory

| Path | Size | State |
|---|---|---|
| `AGENTS.md` | 4.7 KB | Agent rules. Says the PRD/TECH_SPEC are "approved" (L24, L74). |
| `README.md` | **1 byte (empty)** | AGENTS.md L19 lists it as a source of truth, but it has no content. |
| `docs/product/PRD_v1.2.md` | 19.8 KB | Product doc, Roman-Urdu/English. Status: "Review fixes done → W0 shuru". |
| `docs/technical/TECH_SPEC_v1.2.md` | 24.8 KB | 35 P0 / 19 P1 / 4 P2 FRs, 12 NFRs, ADR D-01…D-13, permission allow-list. |
| `docs/validation/W0_Validation_Plan_v1.2.md` | 10.9 KB | 3-week W0, 33 assumptions, gates D-1…D-4. |
| `docs/validation/protocols.md` | 5.5 KB | Protocols P1…P10. |
| `docs/validation/validation-results.md` | 5.4 KB | Master fill-in sheet. **Every row ⏳, owner and date blank.** |
| `docs/research/procurement-and-sources.md` | 9.6 KB | Device list and source list S1…S33. |
| `docs/research/demand-and-competitor.md` | 4.8 KB | Interview guide, teardown template (empty). |
| `docs/history/CHANGELOG_v1.2.md` | 8.4 KB | v1.1→v1.2 fix map. |
| `reference/browsehere/*.md` | 3 files + empty README | BrowseHere static reverse engineering, expert Q&A, 28-complaint analysis. |
| `app/`, `tests/`, `reports/W0/` | `.gitkeep` only | Empty. |
| `prototype/README.md` | 1 byte | Empty. |
| `.gitignore` | 1 byte | Empty. Nothing excludes `*.apk`, `*.jks`, `local.properties`, `build/`. AGENTS.md §7 forbids committing APKs and keys. |

No LICENSE file exists. No PRs or issues exist on the remote (`gh pr list`, `gh issue list` returned nothing).

---

## 2. Requirements inventory (counted from TECH_SPEC §1–§2)

| Class | Count | IDs |
|---|---|---|
| P0 (v1, W1–W16) | **35** ✔ matches the PRD/TECH_SPEC claim | Core/Input 11: FR-01…08, 41, 46, 56 · Tabs 6: FR-11…15, 43 · Ad-block 5: FR-17, 21, 44, 45, 55 · Media 4: FR-22, 23, 24, 26 · Data/compat 5: FR-29, 30, 31, 33, 37 · Basics 4: FR-48, 49, 50, 53 |
| P1 (v1.1) | **19** ✔ matches PRD §6 | FR-09, 10, 16, 18, 19, 25, 27, 31b, 34, 35, 36, 38, 39, 41b, 47, 51, 52, 53b, 54 |
| P2 (v2) | 4 | FR-20, 28, 32, 40 (+ GeckoView gate, Vega port) |
| Deleted | 1 | FR-33b |
| Undefined ID | 1 | **FR-42 is not defined anywhere** (`grep` found nothing). This is a gap in the numbering, not a defect. It should be recorded as "reserved/retired". |
| NFR | 12 | NFR-01…12 |
| ADR | 13 | D-01…D-13 |
| Assumptions | 33 ✔ | A-01…A-33 |
| Gates | 4 | D-1…D-4 |
| Founder open decisions | 13 | PRD §11 L215 |
| Protocols | 10 | P1…P10 |

---

## 3. Document contradictions and defects

| # | Finding | References | Impact | Severity |
|---|---|---|---|---|
| A1-01 | **The roadmap P0 load adds up to 36, not 35.** The W5–7 row says "11" but lists FR-11…15 + FR-29/30/33/49/50 = **10**. FR-13 is counted twice ("13 nav-history"). 8+7+11+5+4+1 = 36. | PRD L165, L175; TECH_SPEC L3 | Capacity planning is off by one. A reader may think there is a 36th FR. | Low |
| A1-02 | **FR-55 phase conflict.** TECH_SPEC puts FR-55 in "W13–14 + W15–16". The PRD roadmap lists it in the W8–9 Ad-block row. | TECH_SPEC L74; PRD L166 | Unclear sprint ownership. | Low |
| A1-03 | **FR-33 phase conflict.** TECH_SPEC says "W2 (defaults) + W6 (persistence)". The PRD roadmap has FR-33 only in W5–7, and the W1–2 list does not include it. | TECH_SPEC L97; PRD L163–165 | The desktop-UA default would be missing during W1–4 page-load and compatibility work. | Medium |
| A1-04 | **Broken path references across the document set.** The docs refer to `W0/validation-results.md`, `W0/protocols.md`, `W0/procurement-and-sources.md`, `W0/demand-and-competitor.md`, and `W0/logs/`. The real paths are `docs/validation/…` and `docs/research/…`. `W0/logs/` does not exist; the repo has `reports/W0/`. Relative links `../W0_Validation_Plan_v1.2.md` in `protocols.md` L2 and `procurement-and-sources.md` L2 resolve to `docs/W0_Validation_Plan_v1.2.md`, which does not exist. | PRD L4, L222–227; Plan L3, L111–114; protocols L2–3, L80; demand L22 | Evidence could be filed in different places by different people. Hurts traceability. | Medium |
| A1-05 | **Referenced documents are missing.** `TV_Browser_PRD_v1.0.md` and `v1.1.md` (PRD L5), and the "third review" document that TECH_SPEC cites as source "Review §1/§2/§3" for about 15 FRs. | PRD L5; TECH_SPEC §1 Source column; CHANGELOG title | Those FRs cannot be traced back to their source. | Medium |
| A1-06 | **"Approved" status is ambiguous.** AGENTS.md calls the PRD and TECH_SPEC "approved" and authoritative. The PRD calls itself "W0-start edition", lists **13 open founder decisions** (including wedge, timeline, minSdk and targetSdk), and says v1.3 will replace it. There is no sign-off record (name, date, version) anywhere. | AGENTS.md L24, L74–76; PRD L3, L215; TECH_SPEC L3 | Agents cannot tell which statements are binding and which are proposals. | **High** |
| A1-07 | **Requirements written as fact while the decision is still open.** NFR-07 fixes minSdk 26 and targetSdk 36, but decisions 5 and 6 are open and PRD L20 calls minSdk 26 a "proposal". NFR-11 and TECH_SPEC §5 say Safe Browsing is default-OFF, while decision 13 is open "after A-18". The PRD §0 wedge is "decided before D-1", yet decision 2 is "Wedge confirm". | TECH_SPEC L134, L138, L202; PRD L12, L20, L215 | Same as A1-06. | High |
| A1-08 | **W0-4 is described as both optional and required.** The Plan calls W0-4 "optional… only if D-1 fails; if it passes, W1 starts earlier" (L18). But gate **D-4 (scope freeze → W1) happens "at W0-4 end"** and v1.3 is produced "in W0-4" (L77, L115). If D-1 passes there is no slot for v1.3 or D-4. | Plan L18, L77, L115; PRD L21, L159 | The W1 entry criterion is undefined on the success path. | **High** |
| A1-09 | **The D-1 interview threshold is badly defined.** "5–8 interviews" with a pass rule of "≥5/8 switching intent". If only 5–7 interviews are held, the ratio cannot be evaluated. The thresholds are also marked "founder confirm karein" (still unconfirmed). | PRD L73, L79; Plan L74 | The go/no-go result could be disputed. | High |
| A1-10 | **Distribution contradiction: F-Droid vs closed source.** PRD §5.2 lists "GitHub / F-Droid" as a PRIMARY channel and TECH_SPEC §5 says ACRA is "F-Droid-friendly", but the teardown template describes our product as "free, closed". F-Droid's main repository only distributes FOSS. | PRD L116; TECH_SPEC L206; demand L47 | Either the channel or the licence model has to change. | **High** |
| A1-11 | **Wrong cross-reference.** PRD §10 says the Safe Browsing decision is "Section 11 (D-6)". It is decision **13**, and TECH_SPEC ADR **D-13**. TECH_SPEC D-06 is "no Pie Menu". | PRD L207; TECH_SPEC L164, L169 | Minor confusion. | Low |
| A1-12 | **Gate coverage gaps.** Gate D-2 lists A-01/02/03/05/12/13/18/23/26. **A-16** (1 active + 1 standby vs 2–3 live, gate W5, track W0-2) and **A-33** (form baseline, gate W3, track W0-2) are W0-2 items but are not in any gate. | Plan L49, L66, L75 | The FR-11/12 and FR-41 designs could go into W1 without being validated. | Medium |
| A1-13 | **Waitlist page spec contradicts itself.** "Static page (GitHub/Cloudflare Pages)" but also "server-side aggregate counter" and an email field. Static hosting cannot collect submissions without a form/backend service. AGENTS.md §5 says no new service without approval. | demand L11–14; AGENTS.md L95 | A service decision is needed. Collecting emails also needs a privacy notice. | Medium |
| A1-14 | **PRD §7 PSS definition differs from NFR-01.** PRD: "app + renderer + GPU". NFR-01: "+ filter-list in-memory set". | PRD L144; TECH_SPEC L128 | Measurements may not be comparable. | Low |
| A1-15 | **FR-11 vs FR-12 wording.** FR-11 allows "1 active + 1 prewarmed standby" (live). FR-12 says "only 1 active renders; **the rest `destroy()`**". | TECH_SPEC L55–56 | Unclear whether a standby counts as "the rest". A-16 is still unresolved. | Low (technical detail is Agent 2's) |
| A1-16 | **Who does W0 is unclear.** The Plan owner is "founder + 1 Android dev" (L2). No Android developer is named. AGENTS.md describes a multi-agent model. Nobody is assigned to procurement, interviews or recruitment. | Plan L2 | W0 cannot start without owners. | High |
| A1-17 | **Status vocabulary conflict.** `validation-results.md` uses ⏳/🔍/✅/❌/⚠️. AGENTS.md §6 requires PASS/FAIL/BLOCKED/NOT TESTED/UNVERIFIED. | validation-results L5; AGENTS.md L104–110 | Needs a mapping (Agent 3 has the details). | Medium |
| A1-18 | **Mixed language.** The core docs are in Roman Urdu with English mixed in. Fine for the founder, but a risk for outside reviewers, store-policy copy and agents. | all docs | Interpretation errors. | Low |

---

## 4. Product-level observations (static, no external facts)

1. **The problem evidence is about the TCL/BrowseHere installed base, but the primary target is Google TV + AOSP.** The PRD says this itself (L14, L31). Demand is still a hypothesis. D-1 is the right gate.
2. **The reference document `Users_Complaints…md` contains claims the PRD has since dropped:** "95%+ score", OpenSubtitles "killer feature", removing `; wv` to fix Google login, Google Sign-In cloud sync, APK installer. The PRD/TECH_SPEC correctly mark these as removed or deferred (FR-28 P2, FR-33b deleted, FR-32 P2, FR-40 P2). **Risk:** agents reading `reference/` could treat these as requirements. The reference folder needs a "non-authoritative" banner (its README is empty).
3. **There is no single statement of value.** Demand §6 "Kyun switch karein?" is blank on purpose until D-1 passes.
4. **Success metrics** are measurable after the v1.2 fixes. D7 "retained installers" depends on A-29.

---

## 5. Task Completion Report (AGENTS.md §9)

- **Assigned task:** Product & Documentation audit.
- **Work completed:** inventory, requirements count, 18 document findings, product observations.
- **Files created:** `reports/project-audit/AGENT1_PRODUCT_DOCS_AUDIT.md` (this file). **No other file was modified.**
- **Evidence:** `file:line` references above (commit `1660e6a`).
- **Tests performed:** none (documentation audit). **Test status:** N/A.
- **Unresolved issues:** A1-06, A1-07, A1-08, A1-09 and A1-10 need founder decisions. A1-04 and A1-05 need an authorised documentation fix.
- **Dependencies:** Agent 4 for whether external claims are correct; Agent 3 for the status-vocabulary mapping.
- **Recommended next action:** founder records the approval state and resolves A1-08/A1-09/A1-10. Lead authorises a separate doc-hygiene PR (paths, README, reference banner, .gitignore) that does not change any requirement.
