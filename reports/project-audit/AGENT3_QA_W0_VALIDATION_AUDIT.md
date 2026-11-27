# Agent 3 — QA & W0 Validation Audit

**Role:** Agent 3 (QA & W0 Validation) · **Date:** 2026-11-27
**Method:** static review of `W0_Validation_Plan_v1.2.md`, `protocols.md`, `validation-results.md` and `procurement-and-sources.md`, plus a check of the evidence folders (`prototype/`, `tests/`, `reports/W0/`).
**No W0 work was executed and no result was recorded.** `validation-results.md` was not touched.

---

## 1. Current evidence state (fact)

| Item | State | Evidence |
|---|---|---|
| Assumptions A-01…A-33 | **33 / 33 NOT TESTED** | validation-results L13–45 (all ⏳) |
| Protocols P1…P10 | **10 / 10 NOT TESTED**. No logs exist. | `reports/W0/` contains only `.gitkeep`; `W0/logs/` does not exist |
| Gates D-1…D-4 | **None signed** | validation-results L128–137 |
| Official-source checks (§C) | **Not done** in the W0 artifact | validation-results L71–75 (Agent 4 found desk evidence; see its report) |
| Procurement (§F) | **No device ordered or held** | validation-results L113–124 |
| Testers recruited | **0 / 15–20** | validation-results L124 |
| Waitlist / interviews | **Not started** | validation-results L82–90 |
| Validation prototype / harness | **Does not exist** (`prototype/README.md` is 1 byte) | — |
| Toolchain in this sandbox | No Android SDK, `adb`, Java or emulator | `which` returned nothing |

**Conclusion: W0 has not started. Nothing has a PASS or FAIL.**

---

## 2. Status vocabulary mapping (proposed; needs Lead/founder approval)

`validation-results.md` uses emoji codes. AGENTS.md §6 requires five fixed labels. Proposed mapping:

| validation-results | AGENTS.md |
|---|---|
| ⏳ pending | NOT TESTED |
| 🔍 in progress | NOT TESTED (with a note "in progress") |
| ✅ confirmed | PASS (executed) / or, for desk claims, "VERIFIED (official source)" |
| ❌ disproved | FAIL |
| ⚠️ partial | UNVERIFIED (or BLOCKED if a dependency stops it) |

Desk/policy rows (A-07, A-11, A-17, …) are not "tests". They should carry `VERIFIED-DOC + URL + access date`, never PASS.

---

## 3. Protocol executability (what each one needs)

| Protocol | Needs | Can run without our own app? | Readiness | Blockers / defects |
|---|---|---|---|---|
| **P1** BT sleep / config change (A-01) | Google TV + TCL/Hisense built-in TV, BT remote, adb | **Partly.** Watching a competitor app is possible, but proving the root cause and the FR-14 fix needs a **harness with and without the `configChanges` lock**. | **BLOCKED** (devices, harness) | Mixes up the hypothesis test (A-01) with FR-14 acceptance. No TCL/Hisense device in procurement (only "borrow"). The app under test is not named. |
| **P2** d3ward baseline (A-05, A-26) | Device, BrowseHere, TV Bro, plain-WebView reference | Mostly. The "plain WebView reference" needs a harness. | **BLOCKED** (devices) | **BrowseHere is preinstalled on TCL only. There is no TCL device and no approved APK source** (an APK with a SHA is referenced in RE; licence/ToS of sideloading it is unknown). No pass criterion (baseline only, which is acceptable). |
| **P3** 30-min LMK (NFR-01/06) | 1 GB + 2 GB devices, **our tab manager**, filter set, HLS | **No.** It measures our architecture. | **BLOCKED** (devices + prototype authorisation) | Thresholds are "final after W0 baseline", which is circular. The pass criterion "0 kills" conflicts with the deliberate `chrome://crash` step (does a renderer kill count?). |
| **P4** renderer crash, 2 WebViews (FR-43) | Device, 2-WebView harness | **No** | **BLOCKED** (prototype) | Good protocol; step 1 also covers the single-process risk (Agent 2, T-01). |
| **P5** first-run capture (NFR-11, A-02, A-18) | mitmproxy, devices, our build | Partly (the BrowseHere part) | **BLOCKED** | **On Android 7+ apps don't trust user-installed CAs**, so TV Bro and other modern apps' HTTPS cannot be decrypted without root. Only DNS/SNI-level capture is possible. Our debug build can opt in with `debug-overrides`. "OS/WebView calls separate" has no attribution method. |
| **P6** form-time baseline (A-33) | 5 users, TV, competitor app | **Yes (proxy)** | **BLOCKED** (users, device) | n = 5 is small for a "≥30%" claim, so report medians and ranges, not significance. User recruitment is not planned anywhere. |
| **P7** service-worker interception (FR-44) | Harness with `ServiceWorkerClientCompat` | **No** | **BLOCKED** (prototype) | No SW test site has been chosen. |
| **P8** popup / popunder (FR-45/19) | Harness + **test pages on 2 origins** | **No** | **BLOCKED** (prototype + hosting) | Test pages not written; cross-origin hosting not specified (needs approval if it is a new service). |
| **P9** D-pad walkthrough | Devices, competitor apps | **Yes** | **BLOCKED** (devices) | "10 core actions" not listed. |
| **P10** FGS video script | Our app with downloads | **No** (W13–14 deliverable) | N/A for W0 | Correctly a script only. Step 2 is P1 scope (see Agent 2, A2-02). |

**Summary:** only **P2, P6 and P9** (and the BrowseHere part of P5) can run against competitor apps alone. **P1 (root cause), P3, P4, P5 (own app), P7 and P8 need an isolated validation harness.** AGENTS.md §4 allows such a harness "only when assigned", so **founder authorisation is a hard blocker for D-2.**

---

## 4. Assumption triage (who can close what, and how)

| Bucket | Assumptions | Closure method | Current status |
|---|---|---|---|
| Desk / official source (no device) | A-07, A-08, A-09, A-10, A-11, A-17, A-19, A-20, A-22, A-25, A-27, A-28, A-29, A-30, A-31 | Official URL + access date (Agent 4 territory) | NOT TESTED in W0 artifact. Agent 4 has desk evidence for A-07, A-11, A-17, A-20 (partial), A-25 (partial), A-30 (partial). |
| Procurement / admin | A-24, A-32 | Quotes, invoices | NOT TESTED |
| Device, competitor apps only | A-05, A-26, A-12, A-02 | P2, P5 | BLOCKED (devices) |
| Device + harness | A-01, A-03, A-04, A-06, A-13, A-14, A-15, A-16, A-18, A-23 | P1, P3, P4, P5, P7 | BLOCKED (devices + authorisation) |
| Users | A-33 | P6 | BLOCKED (recruitment) |

**Scope note:** A-03, A-04, A-06, A-14 and A-15 gate **P1 / v1.1** features but are scheduled in W0-2. Recommend deferring them so the 1-week W0-2 spike can focus on the D-2 items.

---

## 5. Gate readiness

| Gate | Inputs | Ready? | Notes |
|---|---|---|---|
| D-1 demand + differentiation | 14-day waitlist, 5–8 interviews, review mining, TV Bro teardown | **No** | Thresholds unconfirmed. 5/8 ratio problem (Agent 1, A1-09). Waitlist page needs a hosting/form service and a privacy notice. Agent 4 desk evidence suggests the differentiators are at risk. |
| D-2 technical | A-01/02/03/05/12/13/18/23/26 | **No** | Needs devices and a harness. Missing A-16, A-33, and the Compose↔WebView focus spike (Agent 2, T-03). |
| D-3 policy/platform | A-07/20/21/27/28/31 | **Partly feasible now** | Desk work. A-27 needs a founder account decision. |
| D-4 scope freeze | v1.3 docs | **No** | No slot on the D-1-pass path (A1-08). |

---

## 6. Timeline feasibility

- **W0-2 (1 week)** has 9 assumptions plus 6 protocols, starting when devices arrive. Shipping lead time is not estimated anywhere. **The procurement country/channel is not specified.** Some named devices (Google Store, Amazon US models) may not ship to every country. **UNVERIFIED** for the founder's location.
- **The 14-day waitlist** has to start on W0-1 day 1 to finish by the end of W0-2/W0-3. It cannot start until the landing-page service, domain and copy are approved.
- **Tester recruitment (15–20 TV owners)** has to start in W0-1 even though the closed test is in W16–18. That is reasonable, but nobody owns it.
- **Realistic estimate:** if the inputs are approved quickly, W0 is still 3 weeks. If devices or the harness slip, it becomes 4–5 weeks. **UNVERIFIED** estimate.

---

## 7. QA recommendations

1. Founder authorises an **isolated W0 validation harness** under `prototype/` (not `app/`), with its scope limited to P1/P3/P4/P5/P7/P8 instrumentation. It is thrown away after W0.
2. Fix the evidence path to `reports/W0/logs/<protocol>/<date>-<device>/` (needs a doc-hygiene approval) and adopt the §2 status mapping.
3. Add a pass/fail line to P1 (split A-01 and FR-14), P3 (renderer-kill vs app-kill), P5 (attribution method) and P9 (list the 10 actions).
4. Plan to buy or borrow one TCL Google TV (for P1 and P2 BrowseHere coverage) or drop BrowseHere from P2.
5. Recruit P6 users together with closed-test testers.
6. Use the W0 device matrix to also set up emulator API 26/30/34/36 smoke checks for the harness (emulators cannot validate BT, LMK realism or hardware decode).

---

## 8. Task Completion Report (AGENTS.md §9)

- **Assigned task:** QA & W0 Validation audit.
- **Work completed:** evidence census, status mapping, executability review of 10 protocols, assumption triage, gate readiness, timeline review.
- **Files created:** `reports/project-audit/AGENT3_QA_W0_VALIDATION_AUDIT.md` only.
- **Evidence:** repo state at `1660e6a`; doc line references.
- **Tests performed:** **none.** **Test status:** every W0 item is **NOT TESTED**. Device- and harness-dependent items are **BLOCKED**.
- **Unresolved issues:** harness authorisation; owners; procurement channel; BrowseHere source; landing-page service.
- **Dependencies:** founder decisions (Lead's list); Agent 4 for desk closure of policy items.
- **Recommended next action:** founder approves W0 inputs (owners, budget, harness, landing page). The W0-1 desk and procurement track can then start immediately.
