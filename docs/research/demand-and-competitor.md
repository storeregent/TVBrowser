# W0 — DEMAND VALIDATION & COMPETITOR TEARDOWN
**Use with:** `validation-results.md` Section D · `../W0_Validation_Plan_v1.2.md` W0-3 · Gate **D-1**

> **Wedge pehle hi badal diya gaya hai:** Google TV + AOSP boxes = **primary**, legacy Fire OS = **secondary**. Ye validation usi assumption ko test karegi, D-1 ka intezar nahi.

---

## 1. Landing page + waitlist (W0-1 se 14-day clock)

**Spec (privacy-friendly):**
- Static page (GitHub/Cloudflare Pages), koi analytics cookie / third-party script nahi.
- Sirf ek optional email field + **device type** dropdown: `Google TV / Android TV box` · `AOSP / generic box` · `Fire TV (Fire OS)` · `Other`.
- Copy me honest limits pehle din se: *"Built-in ad blocking (YouTube ads excluded) · No account, no telemetry · Google sign-in not supported."*
- Counter sirf total + channel split (server-side aggregate, koi per-user tracking nahi).

**Record:** start date ____ · end date ____ · signups ____ · split: Google TV ___ / AOSP ___ / Fire OS ___ / Other ___.

---

## 2. Interview guide (5–8 primary + 2–3 Fire OS)

Har interview ~20 min, notes `W0/logs/interviews/`. Sawal:
1. TV par abhi web browse kis app se karte hain? (TV Bro / Silk / Downloader / Puffin / kuch nahi)
2. Aakhri dafa kya karne ki koshish ki aur kya ruka? (exact task)
3. Remote se typing me sabse bara masla kya? (forms vs URL)
4. Ads / popups se kya hota hai? Kya koi blocker use karte hain?
5. Video/stream kaise chalte hain? Subtitles/resolution ka masla?
6. Kya TV Bro (ya current app) chhor kar naya browser aazmayenge? **Kyun / kyun nahi?**
7. Kis ek feature ke liye foran switch kar dein? (forced ranking)
8. Privacy / telemetry / permissions kitni matter karti hai? (0–5)
9. *(Fire OS only)* Device ka exact model/OS? Sideload karte hain?

**Scoring:** switching intent = explicit "haan, agar X ho" (vague interest nahi). Repeated pain = ≥3 interviews me wahi theme.

---

## 3. Review mining (top 10 "missing" themes)

Sources: TV Bro Play reviews (S29b), TV Bro GitHub issues/releases (S29/S29a), Amazon Silk reviews, r/FireTV, AFTVnews comments. Har theme ke saath ≥2 quotes + URL. Table `validation-results.md` Section D me bharein.

---

## 4. Competitor teardown (fill during W0-3)

| Capability | **TV Bro (current)** | TV Bro **v3.0.x** (Nov 2026, verify S29a) | TV Akhi | Puffin TV | Downloader | Silk | Hum (v1 plan) |
|---|---|---|---|---|---|---|---|
| Price / OSS | | | | | | | free, closed |
| Remote cursor / spatial | | | | | | | FR-02/03 |
| URL bar on home | | | | | | | FR-07/08 |
| Tabs / bookmarks / history | | | | | | | FR-11/29/50 |
| Ad-block (network) | | | uBlock lists | none | none | DNS workaround | FR-17 |
| Cosmetic / scriptlets | | | | | | | P1 |
| Popup / new-window guard | | | | | | | FR-45 |
| Phone companion | | **v3 me aa gaya (verify)** | | | | | FR-39 P1 → **parity** |
| Download manager / resume | | v3 resumable | | none (files) | APK fetch | | FR-31 |
| Native video player / subtitles | | embedded optimized player | | | | | FR-23/24 |
| Voice / UA / per-site zoom | | | | | | | FR-36/33 |
| DoH / profiles / PIN | | **v3 (verify)** | | | | | P2 |
| `nogms` / Amazon store presence | | | | | | | `nogms` flavor |
| Privacy positioning (zero telemetry) | | | | | | | core USP |

**⚠️ Already-known W0 input:** TV Bro v3.0.x release notes me phone companion app, custom DNS/DoH, profiles with PIN, contextual menu, direct navigation mode, new TV-optimized ad blocker, resumable downloads, embedded optimized video player, export/import bookmarks hain [S29a]. Isliye **FR-39 ko "killer feature" mat kehna** — parity hai.

---

## 5. 6 differentiator hypotheses — verdict (≥3 chahiye)

| # | Hypothesis | TV Bro me actual kya mila (W0-3) | Verdict ✅/❌ | Evidence |
|---|---|---|---|---|
| 1 | URL/search-first UI + 60fps cursor physics + spatial jump | | | |
| 2 | Renderer-kill + process-death resilience + explicit RAM budget | | | |
| 3 | TV-grade native HLS player (resolution/audio/subtitle UI) | | | |
| 4 | Zero-telemetry + minimal-permission promise in writing | | | |
| 5 | Per-site rendering profiles + honest ad-block reporting | | | |
| 6 | Dono stores par maintained `nogms` build | | | |

**Agar ≥3 ✅ na hon** → product redefine (feature parity se nahi, kisi aur wedge se). Ye **D-1 ka hissa** hai.

---

## 6. "Kyun switch karein?" — final one-liner (W0-3 me likhein)

> __________________________________________________________________
> (Ye line PRD v1.3 ke Section 0 me jayegi — tab hi jab ≥3 differentiators verified hon.)

---

### Sources
S29/S29a/S29b TV Bro official · S29c TV Akhi · S19 Downloader official history · S22 AdGuard · S10/S12 Amazon Vega/Fire OS. Poori list `procurement-and-sources.md`.
