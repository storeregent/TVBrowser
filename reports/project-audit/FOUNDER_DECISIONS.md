# Founder Decisions Log

Maintained by the Lead Orchestrator. Each entry records **what the founder decided, when, and where**. IDs refer to `PROJECT_AUDIT.md` §9. Decisions that are not listed here are still **OPEN**.

| ID | Decision | Date | Recorded from | Notes |
|---|---|---|---|---|
| — | Audit PRs #1–#5 accepted and merged into `main` (`71357ce`) | 2026-11-27 | GitHub merge history | — |
| F-01 | **W0 may start.** The founder will run all physical-device tests himself | 2026-11-27 | Founder chat ("Mai apny physical devices py sab tests perform kru ga") | Scope of W0 is otherwise unchanged |
| F-03 | **Isolated validation harness authorised** in `prototype/` (full W0 harness scope: P1, P3, P4, P5, P7, P8 + device diagnostics) | 2026-11-27 | Founder chat (option "Full W0 harness") | No code in `app/`. Brief: `prototype/HARNESS_BRIEF.md` |
| F-H1 | Harness built by a **separate Android agent** from a Lead-written brief | 2026-11-27 | Founder chat (option "a") | Matches the orchestrator role |
| F-H2 | Harness UI technology: **Compose for TV + System WebView** | 2026-11-27 | Founder chat | Also exercises the Compose↔WebView focus risk (R-11) |
| F-H3 | APKs built by **GitHub Actions**; debug artifacts only | 2026-11-27 | Founder chat | Partially covers F-28 (CI platform) for the harness only; production CI is still open |
| F-H4 | Founder's test devices: **Google TV / Android TV** | 2026-11-27 | Founder chat | Exact models, RAM and Android version still pending (H-IN-1) |
| F-H4a | First test device: **Haier MatrixTV CE (HRT56EU)**, Android 14 / API 34, 32-bit userspace, 1.4 GB RAM (not low-RAM), WebView 157, **Bluetooth remote** | 2026-11-28 | Founder device reports + founder chat (remote type: "Bluetooth / voice remote") | Multiprocess WebView, so R-02 is not reproducible on this device (F-05 still open). Cannot show the Android 16 Back behaviour |
| — | Harness PRs **#7 (M1)** and **#8 (M1 checklist + M1.1 brief)** merged | 2026-11-28 | GitHub merge history | M1.1 = PR #9 |
| F-H5 | **Benchmark build approved**: a non-debuggable, R8-on, profileable `benchmark` build type (suffix `.bench`, CI debug key) for representative cold-start/T-18 numbers. Amends HARNESS_BRIEF §7 "No release builds" for this build type only | 2026-11-28 | Founder chat (option "Haan, approve") | Still no Play upload or publishing. Spec: `prototype/HARNESS_M1_1_FIX_BRIEF.md` H-6 |

## Still open, needed soon

| ID | Decision | Why now |
|---|---|---|
| F-02 | Binding status of PRD/TECH_SPEC v1.2 | Agents need to know what is fixed |
| F-04 | Named owners beyond the founder (Android developer, recruitment) | W0-1 tasks other than device tests |
| F-05 | Device budget / channel | Google TV only may not cover the single-process WebView case (R-02) |
| F-07 | D-1 thresholds + differentiator rubric | Before interviews and the waitlist |
| F-09 | Landing-page service + privacy notice | Starts the 14-day waitlist clock |
| F-11 | W0-2 scope: add missing D-2 protocols (A-03/A-12/A-13/A-23) | The harness provides tooling, but QA must still write the protocols |
| F-12 | Doc-hygiene PR (paths, `.gitignore`, status mapping) | Evidence root `reports/W0/logs/` |
| F-15 | Amazon Developer Support case | Fire OS channel decision |
