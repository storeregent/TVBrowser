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

## P2 — Ad-block baseline (A-05, A-26, FR-21)
**Primary benchmark = d3ward AdBlock Test (%)**; canyoublockit sirf qualitative notes.
1. Same device, same network, desktop UA, JS on, whitelist off, fresh profile.
2. Apps: BrowseHere, TV Bro (current Play/GitHub build), aur ek plain WebView reference.
3. **3 runs each**; score + screenshot + variance log.
4. Note: kis test category me fail hua (network / cosmetic / popup / first-party).
**Record:** BrowseHere ___/___/___ · TV Bro ___/___/___ · variance ___. **Koi "95%" claim nahi.**

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
