# W0 — TEST PROTOCOLS
**Use with:** `validation-results.md` Section B · `../W0_Validation_Plan_v1.2.md`
**Rule:** har protocol ka raw log `W0/logs/` me save karein aur path results table me likhein. Koi bhi number "measure kiya" tab hi maana jayega jab raw log ho.

---

## P1 — BT remote sleep / config-change (A-01, C#1)
**Device:** Google TV + TCL/Hisense built-in, BT remote.
1. Fullscreen HTML5 video play karein (1080p, 10 min).
2. Remote ko 2–5 min idle chhor kar sleep/disconnect hone dein; `adb logcat -v time | grep -E 'ActivityManager|BrowserActivity|onHideCustomView|config'` capture.
3. **10 repetitions**; har baar note karein: Activity recreate? `onHideCustomView`? video drop?
4. Repeat with USB/Bluetooth mouse connect/disconnect.
**Pass for FR-14:** 0/10 video drops. **Agar recreate na ho** → A-01 ❌ aur FR-14 ko "defensive only" karke W5 se hatayein / re-scope.

## P2 — Ad-block baseline (A-05, A-26, FR-21) — **PROPOSAL · PENDING FOUNDER APPROVAL**
> **STATUS (current): BLOCKED.** d3ward AdBlock Test (`d3ward.github.io/toolz/adblock.html`) is archived and returns no score — confirmed across 3 M1 runs on Haier MatrixTV CE (see `reports/W0/logs/M1/Haier_MatrixTV_CE_cb73e80/FOUNDER_REPORT.md` §6, issue **I-2**). **No ad-block % claims are permitted until the founder approves the replacement below.**
>
> **Founder conflict to resolve (do NOT edit PRD/TECH_SPEC here):** `PRD_v1.2.md` and `TECH_SPEC_v1.2.md` FR-21 mandate *"ek hi benchmark = d3ward AdBlock Test (%), KPI ≥70%"*. d3ward is dead, so that KPI is currently unmeasurable. Approving a replacement below implies the founder also re-baselines the KPI source. PRD/TECH_SPEC edits are owned by product/tech lead, not QA.

**Proposed PRIMARY benchmark — `https://adblock.turtlecute.org` (Turtlecute33/adblocktest)**
- **Live?** ✅ Yes — repo active (latest commit 13 Nov 2026; README updated Aug 2026). Served from `adblock.turtlecute.org` (GitHub Pages / Cloudflare).
- **Numeric score?** ✅ Yes — one-click test returns a **percentage** ("percentage of connections blocked") with a liquid score ball + color bands (red/amber/green). Three categories: Cosmetic Filter, Script Loading, Host — same shape as d3ward.
- **Android TV WebView compatibility?** ⚠️ **UNVERIFIED.** We have **not** executed this page in an Android TV WebView, so we cannot assert it renders and scores correctly on a TV WebView (WebView 157 / low-RAM device). It is a pure client-side static page whose score is derived from whether probe connections / script loads are blocked (d3ward's exact mechanism) and requires JS enabled; this *should* work, but TV-WebView rendering of the score UI is **UNVERIFIED** until a real run is performed. Requires JS on and connection-failure detection (no aggressive safe-browsing interference). Same plain-WebView reference flow already used in M1.
- **License / terms:** Source `github.com/Turtlecute33/adblocktest` is a **fork of `d3ward/toolz`** (177 commits ahead of `d3ward:toolz:master`). **Licensed CC BY-NC-SA 4.0** (same as upstream d3ward). Free + open-source. No ToS page imposes usage restrictions beyond the CC license; results are computed locally, nothing is uploaded. Minor monetization: a static "sponsorship" box only (run-time ad creatives removed in the Nov 2026 cleanup).
- **Usage scope (important):** this benchmark is a **test/validation target only**. **Do NOT bundle Turtlecute/d3ward lists or code in the TVBrowser product.** We measure against the live test page; we do not vendor its filter lists or scripts. (CC BY-NC-SA is non-commercial — bundling into a commercial product is out of scope regardless.)
- **Why primary:** it *is* d3ward's test, kept online — lowest-friction, highest-faithfulness replacement; preserves the "one numeric % per run" format the plan/PRD expect.

**Qualitative only (NOT a score source):** `canyoublockit.com` — live Simple / eXtreme / Advanced suites, but qualitative pass-fail, no single numeric %. Retained only as a complementary qualitative check per FR-21; no % is claimed from it.

**Rejected / not proposed (for the record):**
- `adblocktester.pages.dev` (KristopherKubicki/adblock-tester) — **DROPPED as secondary: unverified provenance and licence.** An earlier draft proposed it; removed pending founder/lead confirmation of its MIT claim and independent origin. Not used.
- `adblock-tester.com` (the /100 site, "v3.1.1", 11 services/22 checks) — live & numeric, **but affiliate-monetized (Total Adblock referral links)** → undisclosed commercial bias; not suitable as a neutral QA benchmark. (Its ToS at adblock-testers.com states results are "informational only, not a measure of effectiveness.")
- `blockads.fivefilters.org` — live but **boolean** (blocking enabled / partial / none), no numeric %.
- `d3ward.github.io` — **archived / dead**, no score. (The reason for this proposal.)

**Proposed protocol (mechanics unchanged from prior P2; swap the URL to `https://adblock.turtlecute.org`):**
1. Same device, same network, desktop UA, JS on, whitelist off, fresh profile.
2. **Network control (plain-WebView reference):** on the device, set **Private DNS = OFF** so the measurement isolates *browser-level* blocking and is not skewed by device/network DNS filtering. Document the Wi-Fi / DNS provider used each run.
3. Apps: BrowseHere, TV Bro (pinned Play/GitHub build versions), + one plain WebView reference (harness blocklist **OFF** = plain-WebView reference, per `prototype/HARNESS_BRIEF.md` §5.7).
4. **3 runs each** on `https://adblock.turtlecute.org`; record score + screenshot + variance log.
5. **Per run, record the upstream commit SHA** the benchmark is built from (the `d3ward/toolz` commit the fork is currently based on) so each result is tied to a known, reproducible benchmark version.
6. Note failing categories (network / cosmetic / popup / first-party / per-service) from the site's own breakdown.
7. Raw logs + screenshots → `W0/logs/` (evidence root, F-12); path in `validation-results.md` Section B.

**Open validity question (flag for founder/lead — do NOT resolve in this PR):**
- **`shouldInterceptRequest` / empty-200 caveat.** TVBrowser (and some TV browsers) block via `WebViewClient.shouldInterceptRequest`, which can return an *empty 200 response* instead of letting the request fail. The benchmark detects "Blocked" only when the connection actually fails; with an empty-200 shim the request *succeeds*, so the page may report those items as **Not Blocked** even though the ad was suppressed. This could *understate* the true block rate and make cross-app comparison invalid. Decision needed: does the benchmark's failure-detection work against our interception method, or should we add a probe that distinguishes an empty-200 shim from a real block? **UNRESOLVED.**

**Record:** BrowseHere ___/___/___ · TV Bro ___/___/___ · plain-WebView ___/___/___ · variance ___ · upstream-SHA ___. **Koi "95%" claim nahi** until founder approves and a real measured baseline exists.
**Founder decision required:** (a) approve PRIMARY `https://adblock.turtlecute.org` (CC BY-NC-SA, non-commercial — confirm product use is limited to external testing only, no bundling); (b) re-baseline the ≥70% KPI source in PRD/TECH_SPEC (out of QA scope); (c) confirm whether the plain-WebView reference or the app builds is the headline number; (d) resolve the `shouldInterceptRequest`/empty-200 validity question.

## P3 — 30-min scripted LMK session (NFR-01, NFR-06, FR-12)
**Devices:** 1GB device + 2GB baseline. Capture `dumpsys meminfo <pkg>` (ALL PIDs incl. `:webview`/renderer) + `gfxinfo` + `logcat ActivityManager`.
1. 10 tabs open (mix: news, SPA, video page).
2. 3 heavy site loads → 10 min 1080p HLS → Home/restore x3 → BT sleep x10 → `chrome://crash` (P4).
3. Har 5 min par total PSS = app + renderer + GPU + **filter-list in-memory delta** likhein.
**Pass:** 0 process kills, 0 app exits, ≤250MB idle/10 tabs on 2GB, ≤300MB peak on 1GB.

## P4 — Renderer crash with 2 WebViews (FR-43)
1. **2 live WebViews** (active + standby) same renderer process me confirm karein (`adb shell ps -A | grep webview`).
2. Active par `loadUrl("chrome://crash")`.
3. Verify: callback **dono** instances ke liye aaya? Dono remove+destroy+recreate hue? App zinda? `return true`?
4. Crash-loop guard: 3rd crash par error page (max 2 recreate).
5. `didCrash()==false` (LMK) path bhi force memory pressure se test karein.
**Pass:** 0 app exits; dono tabs restored with `TabState` + nav-history.

## P5 — First-run network capture (NFR-11, FR-37, A-02, A-18)
1. Fresh install, airplane mode ON → homepage/favicons dikhne chahiye (**0 calls**).
2. Network ON, mitmproxy/Charles ke saath first-run: har app-initiated request log karein (URL, trigger).
3. BrowseHere ka first-run/new-tab/voice bhi capture karein (A-02 endpoints live?).
4. `gms` me Safe Browsing OFF (default) vs ON dono capture; `nogms` me disabled confirm + page-load timing comparison.
**Pass:** 0 app-initiated calls at first run; OS/WebView calls alag note.

## P6 — Form-time usability baseline (FR-41, A-33)
**Protocol:** **5 users × 3 forms** (search box, login-ish text field, long comment field) = 15 timed tasks.
- Koi app nahi hai to **proxy** = TV Bro / BrowseHere par same forms, same remote, same instructions. W0 me baseline, v1 me same protocol se compare.
- Har task: start = field focus, end = submit/enter. Median + range likhein. Screen recording optional.
- **Target (v1, not W0):** median ≥30% kam is baseline se.
**W0 deliverable:** baseline median ___ s + per-user sheet + protocol notes (kaunse field me zyada dard).

## P7 — Service-worker interception (FR-44)
1. `WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST` check on each device.
2. SW-backed news/PWA site par blocked-count with vs without `ServiceWorkerClientCompat`.
3. Unsupported WebView par crash/flicker? Coverage gap Settings me disclose ho?
**Pass:** supported devices par blocked count badhe; unsupported par 0 crashes.

## P8 — Cross-origin popup / delayed popunder (FR-45, FR-19)
1. Test pages: same-origin `window.open` (gesture), cross-origin `window.open` (gesture), delayed 1200ms popunder (non-gesture), `target=_blank`, redirect chain.
2. Expected: same-origin gesture → new tab; **cross-origin gesture → confirm prompt**; delayed/non-gesture → block + toast; redirect → FR-19 toast.
3. Confirm: FR-19 aur FR-45 me koi duplicate logic nahi; per-site allow-list kaam kare.
**Pass:** 0 unwanted tabs/redirects; 0 false blocks on legitimate same-origin links.

## P9 — D-pad-only & TV quality walkthrough (Play TV review prep)
- Har competitor app aur (jab available ho) apne prototype par: **sirf D-pad + OK + Back** se 10 core actions.
- Note: focus visibility, overscan, 10-foot text, banner/screenshot observations.
- Play TV quality checklist ke items [S4][S5][S6] yahin se evidence jayenge.

## P10 — FGS declaration video script (A-31, FR-16/FR-31)
Play Console FGS declaration me demo video maanga jata hai [S3b]. Script ready rakhein:
1. User **initiates** download → in-app progress dikhein → Home → progress resume.
2. User starts video → playback background/PiP (P1) continue.
3. `dataSync` fallback 6h timeout par graceful stop dikhayein (adb override: `adb shell device_config put activity_manager data_sync_fgs_timeout_duration <ms>`).

---

### Logs index
| Protocol | Log path | Result in `validation-results.md` |
|---|---|---|
| P1…P10 | `W0/logs/...` | Section B |
