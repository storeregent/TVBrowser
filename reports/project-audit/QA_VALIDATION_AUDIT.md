# QA_VALIDATION_AUDIT — W0 Test Strategy & Validation Review
**Author:** QA & Validation Engineer (TVBrowse agent team) · **Date:** 2026-11-27
**Branch:** `arena/e66a9a89-tvbrowser` (from `1660e6a8c44d3677249aecb07e5f2c79f4ca15ef`)
**Type:** Desk audit only — **no tests were executed** during this task (per assignment restriction).
**Status of every W0 test referenced below:** `NOT TESTED` (nothing executed by me; nothing executed by anyone per available evidence).

**Documents audited:**
- `docs/validation/W0_Validation_Plan_v1.2.md` (plan, 33-row assumptions register, gates D-1…D-4, device matrix)
- `docs/validation/protocols.md` (P1…P10 scripts)
- `docs/validation/validation-results.md` (master fill-in results)
- `docs/research/procurement-and-sources.md` (device/order + official-source list)
- `docs/research/demand-and-competitor.md` (D-1 demand protocol, TV Bro teardown)
- `docs/technical/TECH_SPEC_v1.2.md` (35 P0 FRs, NFR-01…12, permissions allow-list)
- `docs/product/PRD_v1.2.md` (success metrics, roadmap, risks)
- `AGENTS.md` (evidence rules, role isolation, status vocabulary)
- Supporting: `docs/history/CHANGELOG_v1.2.md`, `reference/browsehere/*` (static-analysis inputs)

**Restrictions observed:** no production code written, no tests executed, no results invented, no W0 plan/protocol/results file modified, no other agent's report touched, no competitor research performed (existing research docs were only read for traceability).

---

## 0. Executive summary

| # | Finding | Severity |
|---|---|---|
| E1 | **All W0 tests are `NOT TESTED`.** `validation-results.md` is 100 % empty (all 33 A-rows ⏳, all Section B rows blank, Sections C–G blank, owner/date blank). No `logs/` directory, no screenshots, no raw artifacts exist anywhere in the repo (`reports/W0/` and `tests/` contain only `.gitkeep`). | Expected at W0 start — but gates cannot be evaluated until evidence exists |
| E2 | **No false PASS claims detected.** No file asserts a passed/executed test. One labeling inconsistency: `PRD_v1.2.md` §5.1 calls the TV Bro v3 feature list a *"W0 finding (already)"* while its assumption row **A-30 is ⏳ pending** and Section D is empty. Evidence links [S29a]/[S29b] are cited but unverified-into-results. | Medium — reconcile label vs A-30 status (Lead/owner action; PRD is not this agent's file) |
| E3 | **9 of 15 W0-2-track assumptions have no test protocol**, including three **D-2 gate-critical** assumptions: **A-03, A-13, A-23**. D-2 cannot be honestly evaluated as planned. | Critical |
| E4 | **Six protocols (P1, P3, P4, P5-ours, P7, P8) test *our* app's behavior, but no app or authorized prototype exists** (`app/` and `prototype/` are empty). Execution prerequisite is undefined: spike harness vs defer-to-W1. Until the Lead resolves this, those protocols are `BLOCKED`. | Critical — blocks D-2 |
| E5 | **Evidence root path is inconsistent:** plan/protocols demand raw logs in `W0/logs/`, but no `W0/` directory exists; actual W0 files live under `docs/validation/` and `docs/research/`; an empty `reports/W0/` exists. Evidence would fragment without a ratified canonical path. | High |
| E6 | Multiple reproducibility defects: unpinned URLs (10-tab mix, heavy sites, video source, forms), unpinned competitor builds, undefined P6 timing method/participant criteria, undefined P9 "10 core actions", P8's five test pages don't exist, P3 composite script muddles failure attribution. | High |
| E7 | Acceptance-criteria gaps: P1 "video drop" undefined, P2/A-05 has no decision rule, P4 recreate timing/memory-pressure method undefined, results template requests a **cold-start baseline with no protocol**, and two status vocabularies coexist (`⏳/🔍/✅/❌/⚠️` in results vs the five exact codes mandated by AGENTS.md). | High |

**Overall verdict:** W0's validation *design* is strong for a desk plan (clear gates, raw-log rule, 3-run variance discipline, honest status codes), but **coverage inside W0's own declared scope is incomplete (E3), the execution environment is undefined (E4), and zero evidence exists yet (E1)**. Nothing may be marked PASS until executed with the evidence packets defined in §8.

---

## 1. W0 test coverage review

### 1.1 Protocol inventory vs requirement traceability

| Protocol | Purpose | Assumption(s) | FR/NFR acceptance touched | Gate | Executable in W0 as-is? |
|---|---|---|---|---|---|
| P1 | BT remote sleep / config-change, 10 reps | A-01 | FR-14 (0/10 video drops), NFR-06 step | D-2 | **No** — pass criterion is FR-14 (our app); phenomenon-repro on BrowseHere not separated |
| P2 | d3ward ad-block baseline, 3 runs × 3 apps | A-05, A-26 | FR-21 baseline (≥70% is v1 target) | D-2 | **Yes** (competitor apps + plain WebView reference) |
| P3 | 30-min scripted LMK session | (A-16 adjacent) | NFR-01, NFR-06, FR-11/FR-12 | D-2 | **No** — runs "10 tabs" of an app that doesn't exist |
| P4 | `chrome://crash` with 2 WebViews | — | FR-43 acceptance | D-2 (FR-43) | **No** — requires our 2-WebView build/spike |
| P5 | First-run network capture | A-02, A-18 | NFR-11, FR-37 | D-2 | **Split** — BrowseHere capture (step 3) executable now; steps 1/2/4 need our build |
| P6 | Form-time baseline 5 users × 3 forms | A-33 | FR-41 baseline (≥30% later) | D-3 (FR-41 metric) | **Yes** (proxy apps TV Bro/BrowseHere) — artifacts missing (§6) |
| P7 | Service-worker interception | (A-12 adjacent) | FR-44, NFR-12 | — | **Split** — WebViewFeature probe per device executable now; blocked-count with/without `ServiceWorkerClientCompat` needs our build |
| P8 | Cross-origin popup / delayed popunder | — | FR-45, FR-19 | — | **No** — tests our popup logic; the 5 required test pages don't exist |
| P9 | D-pad-only walkthrough (10 actions) | — | NFR-09 (partial), Play TV prep [S4][S5][S6] | D-3 prep | **Partial** — competitor apps yes; "10 core actions" list undefined |
| P10 | FGS declaration video *script* | A-31 | FR-16/FR-31 (documentation deliverable) | D-3 | Desk task — script drafted when assigned; not a test |

### 1.2 Assumption-register coverage (the decisive check)

**W0-2 track (device-dependent technical spike) — 15 assumptions:**

| Assumption | Protocol | Coverage verdict |
|---|---|---|
| A-01 BT sleep → video drop | P1 | Covered (app-target ambiguous, §6) |
| A-02 TCL endpoints live/telemetry | P5 step 3 | Partial — RE §6 endpoint list exists (`reference/browsehere/BrowseHere_Reverse_Engineering.md` §6: `browser.tcloudfamily.com`, `cedock.com`, `my7v.com`, SOAP paths) but P5 does not enumerate them for pass/fail comparison |
| A-03 stream headers via `shouldInterceptRequest` | — | **MISSING — D-2 critical** |
| A-04 Auto-PiP saves process on Home | — | **MISSING** (register says W0-2; §1 W0-2 work row omits it) |
| A-05 "95% / <30-of-300" baseline | P2 | Covered (decision rule missing, §7) |
| A-06 Netflix deep-link exact title | — | **MISSING** |
| A-12 old non-updatable WebView on cheap boxes | — | **MISSING** (P7 probes feature support only; no WebView-version inventory protocol) |
| A-13 saveState scroll/form + nav-history need | — | **MISSING — D-2 critical** |
| A-14 `MULTI_PROFILE` availability | — | **MISSING** (trivial capability probe, still no script) |
| A-15 local WebSocket foreground-only feasibility | — | **MISSING** |
| A-16 1 active+1 standby vs 2–3 live | — | **MISSING** (P3 touches memory but not switch-latency comparison) |
| A-18 Safe Browsing `nogms` call/slowdown | P5 step 4 | Covered (needs both flavors → our builds; sample size undefined) |
| A-23 Bundle size / `TransactionTooLargeException` / 256KB cap | — | **MISSING — D-2 critical** |
| A-26 d3ward reproducible, 3-run variance | P2 | Covered |
| A-33 form baseline without our app | P6 | Covered (proxy methodology, §7) |

**D-2 gate set** (`W0_Validation_Plan` §3: A-01/02/03/05/12/13/18/23/26): covered = A-01, A-05, A-26 (and partial A-02, A-18); **not covered = A-03, A-12, A-13, A-23 (4 of 9)**. → **D-2 is unevaluable as planned.**

**W0-1 track (A-11, A-24, A-27, A-32):** procurement/official-document verification tasks — checklist coverage exists (plan §4, results Sections C/F). Not tests; status = process items. No gap beyond execution discipline.

**W0-3 track (A-07, A-08, A-09, A-10, A-17, A-19, A-20, A-21, A-22, A-25, A-28, A-29, A-30, A-31):** desk/policy verification — covered by `demand-and-competitor.md` + results Section E + official-source checklist. A-30 has preliminary desk citations but ⏳ status (see E2).

### 1.3 Results-template ↔ protocol mapping

- Section B lists 10 rows for P1–P9 (d3ward split into BrowseHere/TV Bro rows). **P10 has no Section B row** — it maps to Section E ("FGS declaration… demo video script"); logs index claims "P1…P10 → W0/logs/…". Minor documentation inconsistency; acceptable if P10 evidence is filed under Section E.
- **Baseline-numbers line requests "cold start ______ ms" but no protocol measures cold start** (NFR-03 / `reportFullyDrawn()`). Either a protocol is missing or the template line is aspirational — owner's call (§3B).
- FR/NFR correctly *out of W0 scope* (deferred, not gaps): NFR-02 cursor fps, NFR-04 ad-block p95 (W8–9), NFR-05 crash rate (W17+), NFR-07/08 (build metrics), NFR-09 TalkBack audit (W15–16), NFR-10 manifest CI (W1), FR-level acceptances for FR-02…FR-08, FR-15, FR-22…FR-26, FR-29/30, FR-33, FR-48/49/50, FR-53, FR-55/56 etc. W0 is baselines + assumptions, **not** FR certification — this audit does not demand W0 execution of those.

---

## 2. Missing test cases

### A. Assumption tests with no protocol (must be added by the plan owner — I do not modify W0 plans)

| ID | Missing test (sketch) | Priority |
|---|---|---|
| **A-03** | Load an HLS/DASH page with `shouldInterceptRequest` logging (spike) → verify exact `Referer`/`Origin`/`Cookie`/`User-Agent` headers captured per `#21` complaint; explicitly out-of-scope note for `#25` | **P0 — D-2** |
| **A-13** | Home → browse 10 min (scroll, fill form, zoom) → return/kill → restore: page + scroll + zoom + back/forward present; necessity verdict for Room nav-history | **P0 — D-2** |
| **A-23** | Build TabState/nav-history payload near and above 256 KB → confirm skip rule; assert no `TransactionTooLargeException` under `onSaveInstanceState`-free design | **P0 — D-2** |
| A-12 | WebView-version inventory across cheap AOSP/Fire devices (`dumpsys webviewupdate` / package version) → "non-updatable old WebView" reality check | P1 — feeds P7/NFR-12 |
| A-04 | Auto-PiP/Home press during playback → process alive? (needs spike + device) | P1 |
| A-06 | Netflix/Prime deep-link `ACTION_VIEW` → exact title opens (needs accounts; note DRM-black-screen is out of scope per FR-27) | P1 |
| A-14 | `WebViewFeature.MULTI_PROFILE` probe per device/WebView version (one-liner capability check) | P1 |
| A-15 | Local WebSocket server bound to foreground Activity only: start/stop with Activity lifecycle, no FGS, Doze behavior noted (FR-39 feasibility) | P2 (FR-39 is P1 feature — probe only in W0) |
| A-16 | Switch-latency measurement: 1 active + 1 standby vs hypothetical 2–3 live (spike instrumentation; LMK correlation) | P2 (decision feeds FR-11/12) |

### B. Baseline/measurement gaps

1. **Cold-start baseline (NFR-03)** — requested by results template, no protocol (add: `reportFullyDrawn()` × 5 cold runs per device, or strike the line).
2. **Emulator compatibility sweep** — plan §5 assigns "Emulator API 26/30/34/36 → Compatibility sweep (free)" but no protocol defines what runs (install/launch/smoke/`chrome://crash`/WebView version presence?). Undefined = untestable as written.
3. **A-02 endpoint enumeration** — P5 should carry the RE §6 expected-endpoint table for live/dead verdicts; currently open-ended.
4. **gms/nogms dual-flavor runs** (A-18, P5, P7, NFR-11) require *two builds of our app* — no build/flavor plan for W0 spike exists.
5. **P4 "forced memory pressure" path** (`didCrash()==false` / LMK-killed renderer) has no method (candidates: `adb shell am send-trim-memory`, memory hog activity, `dumpsys` + kill of renderer PID — each has different fidelity; must be chosen and documented).

### C. Missing test *artifacts* required to execute existing protocols

| Artifact | Needed by | Status |
|---|---|---|
| 5 popup/popunder test pages (same-origin gesture, cross-origin gesture, delayed 1200 ms popunder, `target=_blank`, redirect chain) | P8 | **Do not exist** — must be authored (local HTML or pinned URLs) |
| "10 core actions" enumeration for D-pad walkthrough | P9 | Not defined |
| Form URLs + verbatim participant instructions (search box, login-ish field, long comment) | P6 | Not defined |
| Video source for P1 (local 1080p file vs HLS URL; 10-min asset) | P1 | Not defined |
| Pinned competitor builds: BrowseHere version (PRD cites v8.0.770), TV Bro exact build/date (v3.0.x moving target) | P1/P2/P5/P6/P9 | Not pinned in any protocol |
| logcat grep/filter patterns for P3/P4/P5 (only P1 specifies one) | P3/P4/P5 | Missing |
| Kill-detection rule for P3 (`logcat` ActivityManager "Killing"/"Force stopping" patterns, which PIDs count) | P3 | Implied only |

### D. Explicitly *not* W0 gaps (tracked for clarity)
NFR-02/03*/04/05/07/08/09/10, FR-53 pseudolocale, security-baseline checks (SSL interstitial, mixed content, cookie policy), FR-48 permission-prompt behavior, FR-49/50 data UI — all belong to W1–W16 gates. *(\*cold start is listed above only because the W0 results template asks for it.)*

---

## 3. Test execution requirements

### 3.1 Decisions required BEFORE any execution (Lead/founder)

1. **Canonical evidence root.** Plan says `W0/logs/`; reality has `docs/validation/`, `docs/research/`, empty `reports/W0/`. Recommend: `reports/W0/logs/P1…P10/` + `reports/W0/logs/interviews/` (reports/ already exists for outputs; AGENTS.md points validation work to `reports/`), with a one-line mapping note in the results file's header — **owner edits, not QA**.
2. **Spike authorization.** P1/P3/P4/P5(ours)/P7/P8 need an isolated test harness (e.g., minimal single-Activity + 2-WebView spike under `prototype/`, explicitly authorized per AGENTS.md §4). Alternative: formally defer A-03/04/13/16/23 etc. out of W0 — that changes the plan and needs founder/Lead approval. **Until decided: status = `BLOCKED`.**
3. **Status vocabulary mapping.** Results file uses ⏳/🔍/✅/❌/⚠️; AGENTS.md mandates exactly PASS/FAIL/BLOCKED/NOT TESTED/UNVERIFIED. Define the mapping (e.g., ⏳ = NOT TESTED, ✅ = PASS, ❌ = FAIL, ⚠️ = UNVERIFIED, plus BLOCKED for dependency failures) so gate sign-offs use one language.
4. **Build pinning policy** for competitor apps and any spike (version + date + source APK hash recorded per run).

### 3.2 Tooling & environment (per protocol)

| Tool | Used by | Notes |
|---|---|---|
| `adb` (platform-tools): `logcat -v time`, `dumpsys meminfo`, `dumpsys gfxinfo`, `ps -A`, `shell input`, `am` | P1, P3, P4, P7 | TV boxes: verify `adb` over network enabled; uncertified boxes may restrict |
| mitmproxy/Charles + CA cert install on device + proxy config (TV Wi-Fi advanced or router-level) | P5 | TV proxy UI friction — document exact method per device; flows must be exportable (`.flows`) |
| Screen recording (device `screenrecord` or HDMI capture) | P1, P6, P9, P10 | Mandatory for video-drop and usability evidence |
| d3ward AdBlock Test site access, desktop UA, JS on, whitelist off, fresh profile | P2 | Same network conditions recorded; canyoublockit = qualitative only |
| WebView feature probes (`WebViewFeature.*`) | P7 | One probe log per device |
| Emulators API 26/30/34/36 (AVD images with Google Play / AOSP variants) | sweep (undefined) | Complement, never substitute, for LMK/BT/usability |
| Human participants (5 for P6; 15–20 recruited pool for later closed test) | P6 | TV-owning, remote in hand, anonymized IDs |

### 3.3 Executor discipline (from `protocols.md` + AGENTS.md — enforce, don't reinvent)
- Raw log per protocol → results-table path. **"Measure kiya" only with a raw log.**
- Numbers without raw logs = `UNVERIFIED`.
- Distinguish static analysis / emulator / physical-device evidence in every row.
- Record for every run: date, executor, device model + build fingerprint, OS version, **WebView/Chrome version**, app under test + version, parameter set (UA, network, profile state).

---

## 4. Device requirements

### 4.1 Device inventory (from plan §5 + `procurement-and-sources.md`)

| ID | Device | Why | Vega guard |
|---|---|---|---|
| D1 | 2GB Google TV baseline — used Chromecast w/ Google TV (2GB) or 1–2GB AOSP box | NFR-01/06 low-RAM | Streamer is 4GB — **not** a low-RAM substitute |
| D2 | Google TV Streamer 4GB ($149.99) | current-gen + TV quality review | — |
| D3 | Fire OS: 4K Plus 2025 `AFTMA08C15` (Fire OS 8/API 30) or 4K Max 2nd gen (or used Lite 1GB) | secondary wedge, `nogms`, low-RAM | **Do NOT order** 4K Select 2025 `AFTCA002`, 4K 3rd gen 2026 `AFTCR001`, HD 2026 `AFTCL001` (Vega OS 2.0) |
| D4 | 1GB device (used Fire Lite / 1GB AOSP) | ≤300 MB peak criterion | A-32 open (Chromecast discontinued) |
| D5 | Cheap uncertified AOSP box | old non-updatable WebView (A-12), sideload | — |
| D6 | OEM built-in Google TV (Nvidia Shield / Sony/TCL/Hisense) + **physical BT remote** | P1 config-change/OEM behavior, OEM remotes | Borrow/retail demo |
| D7 | Emulators API 26/30/34/36 | compatibility sweep (scope undefined — §2B) | $0 |
| D8 | *(optional)* Vega Stick 4K Select | A-25 feasibility only | separate SDK/.vpkg |

Procurement log (results Section F) is empty — **no device confirmed on hand**; all physical protocols are currently `BLOCKED` on procurement.

### 4.2 Physical hardware vs emulator — per protocol

| Protocol | Physical hardware required? | Emulator role |
|---|---|---|
| P1 BT sleep/config-change | **Yes** — real BT remote sleeping/disconnecting + OEM TV (D6); emulator cannot reproduce remote sleep | None meaningful |
| P2 d3ward baseline | Physical preferred (score parity with future our-app runs on same device class) | Acceptable for smoke only; variance vs device must not mix |
| P3 30-min LMK | **Yes** — true LMK needs 1GB/2GB physical (D1/D4); emulator RAM is host-backed | None |
| P4 chrome://crash | Functional part emulator-OK; **shared-renderer confirmation + memory-pressure path physical preferred** | Partial |
| P5 first-run capture | Either; proxy/cert friction favors D1/D2 + documented method; needs `gms` and `nogms` builds/devices (D2 + D5/D3) | OK for controlled runs |
| P6 form baseline | **Yes** — real remote, real 10-foot UI, human users | None |
| P7 SW interception | **Yes for old-WebView case** (D5); current-WebView probe any device | Partial |
| P8 popup pages | Either | Fine |
| P9 D-pad walkthrough | **Yes** — real remote + TV display (overscan/10-foot) | None |
| P10 FGS video script | Physical device (+ Play Console later) | None |

**Bluetooth remote & D-pad specifics:** record exact remote model & sleep interval per run (2–5 min idle window varies by OEM); P1 additionally covers USB/BT mouse connect/disconnect but its pass criteria ignore the mouse/double-cursor aspect (FR-04 is W3–4 — out of W0 scope, note only). P9's D-pad list must be defined before execution; NFR-09 TalkBack audit is W15–16, not W0.

**WebView renderer crash specifics (P4):** good coverage of callback-per-instance, `return true`, crash-loop guard (max 2), and `didCrash()==false` path — but needs: chosen memory-pressure method, wait-for-recreate timing, logcat filter, and a fallback rule if the 2 WebViews are *not* in one renderer process (test premise failure handling).

**Memory & tab lifecycle specifics (P3):** composite script (loads → HLS → Home/restore → BT sleep → crash) means a kill's root cause is ambiguous — require step markers/timestamps in the log; define "idle" settle time before each 5-min PSS sample; specify the 10-tab URL mix and 3 heavy sites (pinned domains); filter-list delta is N/A until our build exists.

**Network & privacy specifics (P5):** define app-vs-OS/WebView call attribution rule; airplane-mode step proves FR-37 only for *our* build; Safe Browsing on/off timing needs ≥3 runs/config; A-02 needs the RE §6 endpoint table; optional hardening: 30-min idle capture to catch background telemetry beyond "first-run" (NFR-11 is first-run only — label any extra check as scope-enhancement).

---

## 5. Reproducibility issues

| # | Issue | Protocols | Fix (proposal only) |
|---|---|---|---|
| R-1 | 10-tab mix, "3 heavy sites", 1080p video source, 5 popup pages, 3 forms — no pinned URLs | P3, P1, P8, P6 | Maintain a pinned-URL appendix (date-stamped; local HTML for popup pages) |
| R-2 | Competitor builds unpinned (BrowseHere/TV Bro update continuously) | P1, P2, P5, P6, P9 | Record version + source + SHA-256 per run |
| R-3 | P6: no timing instrument (stopwatch vs recording timestamps), no participant criteria, no instruction script | P6 | Verbatim script + screen recording mandatory + eligibility (TV owner, weekly remote typing) |
| R-4 | P3 composite script → failure attribution ambiguous; no kill-detection grep; no idle-settle definition | P3 | Step markers in logcat; explicit `Killing` patterns; N-s settle before samples |
| R-5 | Only P1 has a logcat filter; P4/P5 lack capture commands | P4, P5 | Standard capture snippets per protocol |
| R-6 | WebView/OS versions not mandated in records → cross-device variance unexplained | all | Mandatory metadata block (§3.3) |
| R-7 | d3ward scores depend on network/JS/site changes; 3-run variance recorded but site version/date not | P2 | Record test date, network type, screenshot of score + site header |
| R-8 | P5 proxy method on TVs unspecified; cert install steps vary | P5 | Per-device proxy playbook appendix |
| R-9 | "10 core actions" (P9) and "10-min video" (P1) undefined → different executors run different tests | P9, P1 | Enumerate before execution |
| R-10 | Executor variance: no inter-rater rule (single executor assumed) | P6, P9 | Same executor for baseline & v1 re-test, or dual-score + reconcile |
| R-11 | Evidence path ambiguity (E5) → artifacts "lost" between `docs/`, `reports/`, missing `W0/` | all | Ratify canonical root (§3.1-1) |
| R-12 | FR-41 baseline is a *between-app* comparison (proxy TV Bro/BrowseHere vs future our app) — methodologically weaker than within-app baseline | P6 | Founder accepts methodology now, or add within-app re-baseline at v1 (document either way) |

---

## 6. Acceptance criteria gaps

| Protocol / gate | Current criterion | Gap |
|---|---|---|
| P1 | "Pass for FR-14: 0/10 video drops" | **"Video drop" undefined** — define FAIL as any of: activity recreate, `onHideCustomView` invoked unexpectedly, playback stopped/exited fullscreen. Also split: (a) reproduce-on-BrowseHere observation for A-01, (b) FR-14 pass on *our* build — currently conflated |
| P2 | Baseline recorded; "koi 95% claim nahi" | **A-05 decision rule missing** — what result confirms/disproves the "<30/300" and "95%" claims (e.g., BrowseHere score vs 300-point scale → ❌ claim plausible?) |
| P3 | 0 kills, 0 exits, ≤250 MB idle/10 tabs (2GB), ≤300 MB peak (1GB) | ≤400 MB 1080p-HLS threshold (NFR-01) not in pass list (record-only in W0 — acceptable, but mark it "baseline, threshold final after W0"); kill-detection method undefined |
| P4 | 0 app exits; both tabs restored with TabState + nav-history | No recreate **timing SLA**; memory-pressure method undefined; guard-sequence (3rd crash → error page) needs ordered steps; different-process fallback undefined |
| P5 | 0 app-initiated calls first run | **App-vs-OS/WebView attribution rule** undefined; SB timing comparison lacks run count |
| P6 | W0 = baseline median + per-user sheet | No pass/fail (measurement task) — results vocabulary has no "RECORDED" state; v1 target ≥30% is between-app (R-12) |
| P7 | Supported devices: blocked count increases; unsupported: 0 crashes | "Increases" has no magnitude — fine for a W0 capability check if verdict is recorded as boolean SUPPORTED/UNSUPPORTED + counts |
| P8 | 0 unwanted tabs/redirects; 0 false blocks | Criteria good — **artifact missing** (test pages) |
| P9 | 10 core actions, note focus/overscan/text | Action list undefined → not reproducible |
| P10 | Script ready for [S3b] | Define "done" = script reviewed against official FGS-declaration requirements; actual video is a later deliverable |
| **D-2 gate** | "Koi bhi root cause galat nikla → FR change" | Only P1 states its disproof branch. A-03/12/13/23 have **no protocol → no disproof path → D-2 partially unevaluable** (E3) |
| **Results template** | "cold start ____ ms" | No protocol produces it (§2B-1) |
| **Status vocabularies** | ⏳/🔍/✅/❌/⚠️ vs AGENTS.md's exact five codes | Map before first fill-in (§3.1-3) |

---

## 7. Evidence requirements (what a PASS/FAIL packet must contain)

### 7.1 Universal packet (every Section B / Section A row)
1. **Status** from the ratified vocabulary (§3.1-3) — never blank at gate time.
2. **Device identity:** model, build fingerprint (`adb shell getprop`), OS version, RAM class.
3. **WebView identity:** package + version (chrome major) — WebView behavior is the SUT for P4/P7.
4. **App under test:** package + version + flavor (`gms`/`nogms`) + build hash (ours or competitor pin).
5. **Date/time + executor.**
6. **Raw log path** under the canonical root (mandatory — `protocols.md` rule: no log ⇒ number not "measured").
7. **Screenshots/recordings** where visual behavior is the claim (video drop, focus visibility, score screens, restored tabs).
8. **Parameter set:** UA, JS on/off, whitelist state, profile freshness, network (same Wi-Fi / cellular), proxy in path Y/N.

### 7.2 Per-protocol evidence matrix

| Protocol | PASS/FAIL evidence packet |
|---|---|
| P1 | logcat (`ActivityManager|BrowserActivity|onHideCustomView|config`), 10-row repetition sheet (recreate?/onHideCustomView?/drop?), screen recording of any drop, remote model + sleep window, video source |
| P2 | Per run: score screenshot ×3 apps ×3 runs, variance table, UA/JS/whitelist screenshots, app versions, network note, failing-category notes |
| P3 | 6× `dumpsys meminfo` (all PIDs) + `gfxinfo` + logcat-kill grep per device, step-timestamped timeline, pinned tab URL list, settle-time rule applied |
| P4 | `ps -A` proof of shared renderer, `onRenderProcessGone` logcat, before/after screenshots of **both** tabs, guard-run log (3rd crash → error page), memory-pressure method + result |
| P5 | mitmproxy `.flows` + redacted summary table (URL, trigger, initiator, app-vs-OS tag), airplane-mode screenshot (homepage+favicons, 0 calls), SB on/off timing tables ≥3 runs, endpoint verdict table vs RE §6 |
| P6 | 5 anonymized participant sheets, verbatim instructions, form URLs, timing method output, median + range calc, recording links |
| P7 | Per-device `WebViewFeature` probe output, blocked-count with/without `ServiceWorkerClientCompat`, crash observation log |
| P8 | The 5 test pages (pinned), per-case screenshot/toast + tab-count log, allow-list spot-check |
| P9 | 10-action checklist with per-action pass/fail, focus-visibility screenshots, overscan/10-foot notes |
| P10 | Script vs [S3b] requirement checklist (desk evidence) |
| Sections C/E (policy) | Official URL + access date + quoted wording (screenshot/PDF) |
| Section D (D-1) | Waitlist export, interview notes (`W0/logs/interviews/`), review-mining quotes + URLs, teardown matrix |
| Section F | Receipts, exact model numbers, `getprop` fingerprints, Vega-check confirmed |

### 7.3 Gate-time verification procedure (QA spot-check — to run only when authorized)
1. Every row: status ≠ blank; packet fields 1–8 complete.
2. Raw-log existence check + spot open (numbers in results must appear in logs).
3. Cross-check: results claims ↔ log timestamps ↔ device metadata consistency.
4. Sampling re-execution of 1–2 protocols (if authorized) to detect executor drift.
5. **Any row lacking a raw log for a numeric claim → downgrade to UNVERIFIED, regardless of what the table says.**

---

## 8. Proposed test matrix

### 8.1 Devices × protocols (prerequisite legend: ✅ executable now ·🧩 needs spike/build authorization ·📦 needs device delivered ·👤 needs humans · n/a)

| | D1 2GB GTV | D2 Streamer 4GB | D3 Fire OS | D4 1GB | D5 cheap AOSP (old WV) | D6 OEM TV+BT remote | D7 emulators 26/30/34/36 |
|---|---|---|---|---|---|---|---|
| P1 BT sleep | 📦🧩 | 📦🧩 | 📦 | — | — | 📦🧩 **primary** | n/a |
| P2 d3ward ×3 runs | ✅📦 | ✅📦 | ✅📦 | — | ✅📦 | — | smoke only |
| P3 30-min LMK | 📦🧩 **primary** | 📦🧩 | — | 📦🧩 **primary** | — | — | n/a |
| P4 chrome://crash | 📦🧩 | 📦🧩 | 📦🧩 | — | 📦🧩 (old WV) | — | functional only |
| P5 first-run capture | 📦🧩 (`gms`) | 📦🧩 (`gms`) | 📦🧩 (`nogms`) | — | 📦🧩 (`nogms`, A-12) | — | controlled alt. |
| P6 form baseline 5×3 | 👤📦 | — | 👤📦 (2–3 users) | — | — | 👤📦 | n/a |
| P7 SW interception | 📦🧩 | 📦🧩 | 📦🧩 | — | 📦🧩 **old-WV case** | — | probe alt. |
| P8 popup pages | 📦🧩 | ✅🧩 | ✅🧩 | — | ✅🧩 | — | ✅🧩 fine |
| P9 D-pad 10 actions | 👤📦 | 👤📦 | 👤📦 | — | — | 👤📦 **primary** | n/a |
| P10 FGS video script | 📦 (later, with real feature) | 📦 | — | — | — | — | n/a |

*Wave order:* **W0-Wave-1 (now, no devices):** P2 on available emulator/desktop-adjacent setups is *not* recommended — prefer wait for D1/D3 to keep baselines device-faithful; desk items (Section C/E verification, P10 script, test-page authoring, endpoint tables, action lists) are executable immediately. **Wave-2:** procurement arrives → P2, P6-prep, P9-prep, P5-step-3 (BrowseHere), P7 probes. **Wave-3:** spike authorized → P1, P3, P4, P5-full, P7-full, P8. **Wave-4:** participants → P6, P9.

### 8.2 Assumption × evidence-source matrix (gate readiness)

| Gate | Assumptions | Ready now? |
|---|---|---|
| D-1 | A-30 (preliminary only), Section D items | Partially — desk work yes; waitlist/interviews not started |
| D-2 | A-01✅prot · A-02~ · **A-03✗** · A-05✅prot · **A-12✗** · **A-13✗** · A-18~ · **A-23✗** · A-26✅prot | **No — 4 missing protocols + spike dependency** |
| D-3 | A-07/20/21/27/28/31 + Section E | Desk-yes; all ⏳ |
| D-4 | Aggregates v1.3 docs from results | Blocked on D-1…D-3 |

---

## 9. Risks and dependencies

| # | Risk / dependency | Impact | Mitigation (proposal) | Owner |
|---|---|---|---|---|
| K-1 | **No spike/app** for P1/P3/P4/P5/P7/P8 | D-2 blocked; W1 spec stays tentative | Lead authorizes isolated `prototype/` harness **or** formally defers assumptions (plan change → founder) | Lead/Founder |
| K-2 | **Missing protocols** A-03/13/23 (+A-04/06/12/14/15/16) | D-2 unevaluable | Plan owner adds protocols (QA supplied sketches §2A) | Plan owner |
| K-3 | **Procurement not placed** (Section F empty; shipping lead time) | All physical tests blocked; 3-week W0 slips | Place orders from exact-model list; avoid Vega models; A-24/A-32 resolve | Founder |
| K-4 | Evidence-root ambiguity (E5) | Evidence fragmentation; gate audits fail | Ratify canonical log path now | Lead |
| K-5 | Tester recruitment (15–20 TV-owning; 5 for P6) | P6 blocked; W17 closed test at risk | Start recruitment W0-1 as planned | Founder |
| K-6 | d3ward/canyoublockit site drift or outage | Baseline invalid | Pin date, screenshots, 3-run variance; qualitative fallback (canyoublockit) | QA on execution |
| K-7 | WebView fragmentation across cheap boxes | Results non-comparable | Mandatory WebView-version metadata (§3.3) | QA on execution |
| K-8 | TV proxy/cert friction for mitmproxy | P5 stalls | Per-device proxy playbook; router-level capture fallback | Executor |
| K-9 | TV Bro v3 builds move during W0 | Teardown/baseline inconsistency | Pin build+date+hash per run | Executor |
| K-10 | OEM TV borrow falls through (A-01 device) | P1 coverage reduced to Google TV remote only | Prioritize D6; document reduced-coverage as UNVERIFIED for OEM-specific behavior | Founder |
| K-11 | FR-41 between-app baseline (R-12) | Later ≥30% claim attackable | Founder accepts methodology or adds within-app re-baseline | Founder |
| K-12 | P3 composite script attribution | False root-cause at D-2 | Step markers + isolate failures into follow-up runs | QA on execution |
| K-13 | Status-vocabulary mismatch | Gate sign-off confusion | Publish mapping (§3.1-3) before first fill-in | Lead |
| K-14 | PRD "W0 finding" label vs A-30 ⏳ (E2) | Evidence-integrity optics | Reconcile label or complete A-30 → Section D | PRD owner |

---

## 10. Evidence-status verification (assignment item 14): *are W0 results supported by evidence?*

**Answer: There are no W0 results yet — and no unsupported results were found.**

| Check | Finding |
|---|---|
| `validation-results.md` Section A (33 rows) | All ⏳, empty result/evidence columns — **`NOT TESTED`** |
| Section B (10 test rows) | All blank (no device/run/result/log path) — **`NOT TESTED`** |
| Sections C/D/E/F/G | All unchecked/blank; gates unsigned; owner/start-date blank — **`NOT TESTED`/pending** |
| Raw logs (`W0/logs/` per protocol rule) | **Directory does not exist**; repo-wide search found 0 log/screenshot/flow artifacts |
| `reports/W0/`, `tests/`, `app/`, `prototype/` | Empty (`.gitkeep`/stub README only) |
| Day-1 checklist (plan §1) | All items unchecked |
| Fabricated/pass-without-execution claims | **None detected** in any audited document |
| One label inconsistency | `PRD_v1.2.md` §5.1 "⚠️ W0 finding (already): TV Bro v3.0.x…" vs A-30 ⏳ + empty Section D — cite exists [S29a/S29b] but verification not recorded. Treat as **UNVERIFIED** until A-30 is filled with evidence (E2/K-14) |

**Status of every W0 protocol P1–P10 as of this audit: `NOT TESTED`. No test was executed by QA during this audit.**

---

## 11. Recommendations to the Lead (prioritized; no plan files were modified by QA)

1. **Ratify the canonical evidence root** (suggest `reports/W0/logs/`) and add the status-vocabulary mapping — both are one-line owner edits.
2. **Resolve the spike question (K-1):** authorize an isolated 2-WebView test harness for W0-2, or defer A-03/04/13/16/23 (+P1/P3/P4/P5/P7/P8 app-side steps) out of W0 with founder approval. This is the single biggest schedule/gate risk.
2. **Add the four missing D-2 protocols (A-03, A-13, A-23, plus A-12)** — sketches in §2A; QA will review drafts before execution.
3. **Publish the missing artifacts** before Wave-1: pinned-URL appendix, 5 popup test pages, P9 10-action list, P6 instruction script + form URLs, P1 video source, P4 memory-pressure method, logcat capture snippets.
4. **Pin competitor builds** (BrowseHere, TV Bro) with version+hash in every protocol's run sheet.
5. **Reconcile the PRD "W0 finding" label with A-30** (E2/K-14).
6. **Decide the cold-start baseline line** in the results template: add a mini-protocol or remove the line.
7. When execution is authorized, QA will: pre-verify setup checklists, then perform gate-time evidence audits per §7.3 — **no PASS will be recorded without an executed test and a raw log.**

---

## 12. Task completion report (per AGENTS.md §9)

- **Assigned task:** QA & Validation Engineer — W0 validation plan/protocol audit (audit only, no test execution).
- **Work completed:** Full desk audit of W0 plan, P1–P10 protocols, results template, procurement/demand companions, PRD/TECH_SPEC traceability; coverage matrix, 14 missing-test/artifact findings, reproducibility review (12 issues), acceptance-criteria gap review, evidence-packet spec, proposed device×protocol test matrix, 14 risks/dependencies, evidence-status verification.
- **Files created:** `reports/project-audit/QA_VALIDATION_AUDIT.md` (this file). **Files modified:** none.
- **Evidence and references:** documents listed in the header; repo-state checks (`find` for logs/artifacts: none), empty `validation-results.md` Sections A–G, empty `reports/W0/`/`tests/`/`app/`.
- **Tests performed:** **none** (audit-only authorization). **Test status:** all W0 protocols = `NOT TESTED`.
- **Unresolved issues:** K-1 spike authorization, K-2 missing protocols, K-3 procurement, K-4 evidence root, K-13 status mapping, K-14 PRD label.
- **Dependencies:** Lead/founder decisions above; device delivery; tester recruitment.
- **Recommended next action:** Lead reviews this audit and rules on items 1–3 of §11. **QA stops here and waits for the Lead's next assignment.**
