# Agent 2 — Android Architecture Audit

**Role:** Agent 2 (Android Architecture) · **Date:** 2026-11-27
**Method:** static review of `TECH_SPEC_v1.2.md` (FR/NFR/ADR/§4/§5) against Android platform behaviour. Official Android docs were checked where stated (accessed 2026-11-27).
**Not done:** no code, no prototype, no emulator or device run. This sandbox has no Android SDK, `adb` or Java (`which` returned nothing). **No finding below is a test result.** Labels: **VERIFIED-DOC** = confirmed in official documentation · **STATIC** = engineering reasoning, not executed · **UNVERIFIED** = needs a spike or device evidence.

---

## 1. Overall assessment

The chosen direction is sound for a TV browser on 1–2 GB devices: Kotlin, single Activity, Compose for TV, System WebView behind a `BrowserEngine` interface, at most 2 live WebViews, disk-based tab state, renderer-crash recovery, bundled hosts ad-block, Media3 for direct streams, `gms`/`nogms` flavors, and no backend. It matches the expert baseline in `reference/browsehere/Better_Android_TV_Browser.md`. **I do not recommend any change to the architecture.**

The spec does contain **several gaps in feasibility and internal consistency**. They are listed below. Most can be closed in W0-2 or in a v1.3 edit. Two of them (A2-01, A2-03) would cause the build or CI to fail as currently written.

---

## 2. Findings

| # | Finding | Ref | Label | Severity |
|---|---|---|---|---|
| A2-01 | **FR-31 needs a permission that is not on the allow-list.** User-initiated data transfer (UIDT) jobs need a `JobService` and the **`RUN_USER_INITIATED_JOBS`** permission, and they **require a notification**. TECH_SPEC §4 does not list this permission, and NFR-10 says CI blocks anything not on the allow-list, so the primary FR-31 design would fail the project's own CI rule. UIDT also exists only on **API 34+**. On Fire OS 8 (API 29/30) and many Android TV 11–13 devices, the `dataSync` FGS "fallback" is actually the **main** path. | TECH_SPEC L94, §4 L176–198, NFR-10 L137 | **VERIFIED-DOC** (developer.android.com/develop/background-work/background-tasks/uidt) | **High** |
| A2-02 | **The permission allow-list does not separate v1 from v1.1.** `FOREGROUND_SERVICE_MEDIA_PLAYBACK` is for FR-16, which is **P1**, but it sits in the v1 manifest list. Declaring an FGS type with no service using it creates a Play FGS declaration obligation and a review risk in v1. P10 step 2 (playback/PiP video) is also P1 work. | TECH_SPEC L60, L181; protocols P10 | STATIC | Medium |
| A2-03 | **FR-48 "Notifications" site permission probably cannot be implemented.** Android WebView is not known to implement the Web Notifications / Push API for embedding apps. There is no `WebChromeClient` callback for notification permission. As written, the FR-48 acceptance criterion may be impossible. | TECH_SPEC L111 | **UNVERIFIED** (needs an official-doc check or a spike) | Medium |
| A2-04 | **FR-13 back/forward after process death needs a custom history design that is not specified.** A WebView back/forward list can only be rebuilt with `restoreState(Bundle)`. FR-13 forbids persisting that Bundle (it is in-memory only). So after process death, "back/forward restored" means the **app must run its own history** (intercept navigations, `doUpdateVisitedHistory` for SPA `pushState`, and `loadUrl` on Back). This conflicts with FR-06's Back chain, which calls `goBack()`. Forms and scroll will also be lost or approximate after process death. | TECH_SPEC L51 (FR-06), L57 (FR-13) | STATIC | **High** (W5–7 core) |
| A2-05 | **FR-11 / FR-12 / NFR-01 disagree about the standby WebView.** FR-11 allows a live prewarmed standby. FR-12 says "rest `destroy()`". NFR-01 budgets "1 active + 1 standby". A-16 (standby vs 2–3 live) is not in any W0 gate (see Agent 1, A1-12). | L55–56, L128 | STATIC | Medium |
| A2-06 | **FR-43 assumes the WebView renderer runs in its own process.** On Android O+ the WebView renderer is normally out-of-process, and `onRenderProcessGone` (API 26) lets the app survive. **On low-RAM device configurations, WebView may run single-process.** If so, a renderer crash takes down the app and FR-43 cannot help. This has to be confirmed on the 1 GB devices (P4 step 1 already checks `ps -A | grep webview`, which is good). | L62; protocols P4 | **UNVERIFIED** | **High** (1 GB target) |
| A2-07 | **The NFR-01 memory budget (≤250 MB total PSS, 10 tabs, renderer + GPU + filter set) is ambitious.** A single renderer on a heavy site can by itself be in the 100–300 MB range. With Compose + Media3 + Room in the app process, the margin is thin. No baseline exists. | L128 | **UNVERIFIED** (no measurement exists) | High |
| A2-08 | **Focus between Compose for TV and the embedded WebView is the highest UX-engineering risk, and W0 has no spike for it.** D-pad focus has to move between the Compose focus system, an `AndroidView(WebView)`, a virtual-cursor overlay, and HUD reveal. Key dispatch must be centralised at the Activity level (FR-05). P9 tests only competitor apps. **Note:** the status of the Compose-for-TV libraries (`androidx.tv` foundation vs material) should be re-checked against current AndroidX release notes before W1. | FR-01–07; TECH_SPEC L153 | UNVERIFIED | High |
| A2-09 | **Safe Browsing default-OFF has to be explicit.** WebView Safe Browsing is on by default on devices with GMS, whatever the app flavor. A `nogms` APK sideloaded onto a Google TV device would still use it unless it is disabled (manifest meta-data `android.webkit.WebView.EnableSafeBrowsing=false` or `setSafeBrowsingEnabled(false)`). TECH_SPEC §5 does not say this. If it is missed, NFR-11 (zero first-run calls) and P5 could fail. | L138, L202 | STATIC (well-known platform behaviour; check against current docs) | Medium |
| A2-10 | **FR-45 "gesture age >800 ms" is not a WebView API.** `onCreateWindow(isUserGesture)` gives only a boolean. The 800 ms window needs the app's own OK/click timestamps, which is feasible. Delayed popunders that WebView reports as user gestures will need heuristics. | L73 | STATIC | Low |
| A2-11 | **FR-56 guard coverage.** Google OAuth can start through `shouldOverrideUrlLoading` **and** through `window.open` popups ("Sign in with Google" on third-party sites), so both FR-56 and the FR-45 path must catch `accounts.google.com`. **Product impact:** Google sign-in breaks on third-party sites too. That is acceptable under the policy, but it has to appear in the honest-limits copy. | L45, L73, L97 | STATIC + Google OAuth doc (Agent 4) | Medium |
| A2-12 | **The hosts-only ad-block design has a ceiling.** Hosts matching in `shouldInterceptRequest` cannot block first-party paths or cosmetic elements, and it does not get a reliable request initiator. Together with the permissive-only list (Agent 4 measured about 19.1k domains), the **d3ward ≥70%** target is not supported by any evidence. The <1 ms p95 trie lookup is very plausible. | FR-17, FR-21, NFR-04 | UNVERIFIED (no baseline) | High (differentiator) |
| A2-13 | **minSdk choice and FR-43.** `onRenderProcessGone` needs API 26. If minSdk 23 or 24 were chosen (decision 5; Agent 4 reports TV Bro's WebView build uses minAPI 24), FR-43 would need an API guard and could not work on 23–25. minSdk 26 drops Fire OS 6 (API 25) devices. | D-11 L167, NFR-07 | STATIC | Medium (founder trade-off) |
| A2-14 | **NFR-08 ≤20 MB APK with Compose + Media3 (HLS/UI) + Room + Hilt + OkHttp + about 19k-domain list + favicons.** Plausible with R8, since there are no native libraries, but no build exists. | L136 | UNVERIFIED | Low |
| A2-15 | **ACRA "self-hosted" needs a server (for example Acrarium).** The PRD lists $0, and D-04 says "no backend". Hosting, cost, owner and privacy notice are not specified. | PRD L186; TECH_SPEC L150, L206 | STATIC | Medium |
| A2-16 | **FR-14 depends on the A-01 root cause.** The `configChanges` lock is low-cost and harmless as a defensive measure, but whether a BT remote's sleep/wake actually fires `keyboard|navigation` changes on target devices is unproven. | L58; protocols P1 | UNVERIFIED | Low |
| A2-17 | **Engineering spec items needed before W1 (not before W0).** Module/package structure; DI choice (Hilt) ADR; threading model for `shouldInterceptRequest` and the filter trie; Room schema and migrations; flavor/signing config; CI (NFR-10 manifest diff, lint, unit tests); test strategy (unit, instrumented, macrobenchmark for NFR-02/03); WebView-version support policy (NFR-12 minimum WebView version). None of these exist in the repo. | — | STATIC | Medium |
| A2-18 | **Security baseline §5 is solid.** `cancel()` on SSL errors, no file access, `WebViewAssetLoader`, intent hardening, `allowBackup=false`. **Product trade-off to note:** "third-party cookies off" breaks some embedded logins and comment widgets. | L200 | STATIC | Info |
| A2-19 | **Competitor feasibility signal (from Agent 4's static repo read):** TV Bro v3.1.0 uses the WebView multi-profile API for incognito (it needs WebView 119+). This supports FR-35 (P1) feasibility and A-14. | FR-35 | Desk evidence (release notes) | Info |

---

## 3. Technical risks for the register (input to the Lead)

| ID | Risk | Likelihood | Impact | Closing evidence |
|---|---|---|---|---|
| T-01 | WebView runs in-process on low-RAM devices, so FR-43 cannot help (A2-06) | Med | High | P4 on 1 GB device |
| T-02 | NFR-01 ≤250 MB not reachable (A2-07) | Med-High | High | P3 baseline with harness |
| T-03 | Compose↔WebView D-pad focus/cursor interop costs W3–4 (A2-08) | Med | High | **New** W0-2 spike item |
| T-04 | FR-31 permission/notification/API-level gaps (A2-01) | High (certain as written) | Med | Spec fix in v1.3 |
| T-05 | FR-13 custom history complexity (A2-04) | High | Med | Design note + A-13 |
| T-06 | d3ward ≥70% not reachable with permissive hosts only (A2-12) | Med-High | High | P2 baseline + list experiment |
| T-07 | Safe Browsing makes first-run calls (A2-09) | Med | Med | P5 |
| T-08 | FR-48 web notifications impossible (A2-03) | Med | Low | Doc check / spike |
| T-09 | APK >20 MB (A2-14) | Low | Low | First build |

---

## 4. Recommendations (for founder/Lead decision; nothing has been changed)

1. In v1.3: add `RUN_USER_INITIATED_JOBS` and `JobService` to §4, or redesign FR-31. Split §4 into **v1 vs v1.1** columns. Re-check whether FR-48 Notifications is feasible.
2. Add to the W0-2 spike: (a) Compose-for-TV ↔ WebView focus + cursor overlay, (b) the single-process WebView check on the 1 GB device, (c) A-16 standby latency vs memory, plus a gate entry for it.
3. Write a short FR-13 history design (custom stack, SPA handling, Back semantics) before W5.
4. Make Safe Browsing state explicit per flavor (manifest meta-data) in §5.
5. The architecture spec items in A2-17 are W1 entry criteria, not W0 work.

---

## 5. Task Completion Report (AGENTS.md §9)

- **Assigned task:** Android Architecture audit.
- **Work completed:** feasibility and consistency review of the FRs, NFRs, ADRs, permissions and security baseline; 19 findings; 9 technical risks.
- **Files created:** `reports/project-audit/AGENT2_ANDROID_ARCHITECTURE_AUDIT.md` only.
- **Evidence:** TECH_SPEC line refs; official UIDT doc (VERIFIED-DOC); all other items STATIC or UNVERIFIED as labelled.
- **Tests performed:** **none.** No emulator, device or build. **Test status:** NOT TESTED for every technical claim.
- **Unresolved issues:** T-01, T-02, T-03 and T-06 need W0-2 device evidence. A2-01 and A2-02 need a spec edit (founder authorisation).
- **Dependencies:** device procurement (Agent 3); permission to build an **isolated validation prototype** (founder).
- **Recommended next action:** founder authorises a W0-2 validation harness with a strictly limited scope. Spec edits are deferred to v1.3.
