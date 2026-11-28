# W0 Harness — M1.1 Fix Brief (Lead → Android agent)

| Field | Value |
|---|---|
| Owner of this brief | Lead Orchestrator |
| Implementer | Android agent (the same role that built M1, PR #7) |
| Write scope | `prototype/w0-harness/**`, `.github/workflows/w0-harness.yml`. **Nothing else** |
| Base | PR #7 head `d64b371`. If PR #7 is merged by the time work starts, branch from `main` |
| Trigger | First physical-device run: `reports/W0/logs/M1/Haier_MatrixTV_CE/LEAD_ANALYSIS.md` |
| Priority | **Before M2.** M2 work starts only after the M1.1 re-run passes the §3 acceptance checks |
| Unchanged | `prototype/HARNESS_BRIEF.md` stays authoritative for everything not changed here. The PRD and TECH_SPEC are not touched |

## 1. Context (read first)

Founder's first device: **Haier MatrixTV CE (HRT56EU), Android 14 / API 34, 32-bit userspace only (`armeabi-v7a`), 1.4 GB RAM, WebView 157.** The run was driven via adb key events.

Result: the harness **installs and launches, but cannot be operated with D-pad.**
- Initial focus lands in the URL field and never leaves it.
- Text on Home is unreadable (dark on dark).
- As a result, Device Info, Logs, Settings, Tests and Test URLs were all unreachable, and nearly every M1 test is BLOCKED.

Read `LEAD_ANALYSIS.md` §4 for root causes and look at the screenshots in the same folder.

## 2. Required fixes

### D-1 — D-pad focus trap on Home (BLOCKER)
- **Replace** `androidx.compose.material3.OutlinedTextField` on Home with a **TV-safe URL entry**:
  - A focusable TV element (e.g. a tv-material `Surface`/`Button`-styled row) shows the current URL text.
  - **OK** switches it into edit mode: an internal `BasicTextField` gets focus and the IME opens.
  - In edit mode, **IME Done / Back / D-pad Down** leave edit mode and move focus to the next element.
  - Outside edit mode the field must never consume Up/Down/Left/Right.
  - A thin `onPreviewKeyEvent` wrapper is acceptable if it achieves this.
- **Initial focus** on Home goes to the **first Test URL item** (or, if that list is empty, the "Device Info" button). Use a `FocusRequester` requested once after first composition. It is **never** the URL field.
- The whole Home must be traversable with D-pad only, in a predictable order: URL entry → Go → button row (Device Info, Logs, Settings, Tests, Mark step, …) → Test URL list. Up must return in reverse order.
- Apply the same rule to **any other screen** that has a text input (Settings, Mark step dialog): text input is never the initial focus and never traps D-pad.
- Empty Go (D-6): do nothing and show no error. "Ignored: not a URL" is only for non-empty invalid input.

### D-2 — Unreadable text (BLOCKER)
- `HarnessTheme` must use tv-material `MaterialTheme(colorScheme = darkColorScheme(...))` with explicit `background`, `surface`, `onBackground`, `onSurface`, `primary`, `onPrimary`, etc. that match `HarnessColors`.
- Wrap the root in a tv-material **`Surface`** (or provide `LocalContentColor`) so every `Text` gets a light content colour by default.
- `ListItem` headlines, the title, and labels must all be readable. Target **WCAG AA contrast ≥ 4.5:1** for body text on the background colour.
- Remove all remaining `androidx.compose.material3` usage from the UI, or theme it explicitly, so that no default light-theme colours appear.

### D-3 — Focus visibility (HIGH)
- Every focusable element shows an **obvious focus state** from 3 m away: e.g. a scale of about 1.05 plus a 2–3 dp light border or a high-contrast container colour.
- The focused state must contrast with both the background and the unfocused state.

### D-4 — Main-thread log I/O (MEDIUM)
- `SessionLog.log()` must not do file I/O on the calling thread. Use a single-thread writer (a `HandlerThread` or a single-thread `Executor` with a queue) that buffers and appends.
- Event order and monotonic timestamps are preserved. Timestamps are taken **at call time**, not at write time.
- `flush()` is called on `onStop`, on export, and in the uncaught-exception handler. The writer must flush synchronously there, with a bounded wait (≤ 500 ms) so a crash does not lose the last lines.
- The logcat (`W0H`) mirror can stay synchronous.

### D-5 — adb automation hooks (MEDIUM)
The founder tests with an assistant over adb, so every M1 test must also be drivable by `am start`. Add intent extras to the **launcher `EntryActivity`**, forwarded to the browser activity:

| Extra | Effect |
|---|---|
| `-e w0h.url <url>` | Open the URL directly (same path as Go, normalised, logged as `NAV_REQUEST` with `source=adb`) |
| `-e w0h.screen home\|deviceinfo\|logs\|settings\|tests` | Open that screen |
| `-e w0h.action export_deviceinfo\|export_all\|mark_step` | Perform the action, write to the normal `w0logs/` folder, log the output path, then show the result on screen. `mark_step` also takes `-e w0h.label <text>` |
| `--ez w0h.locked true\|false` | Pick the Locked or Unlocked activity variant (equivalent to the existing setting, for this launch only; logged) |

- These hooks are **debug-only** (the harness is debug-only anyway) and must not be exported to other apps beyond the launcher intent.
- Document every hook with copy-paste commands in `prototype/w0-harness/README.md` under a new "adb automation" section, together with a `tools/run-m1.sh` script that runs the M1 sequence non-interactively where possible. That covers: device props → 5× cold start → Device Info export → open each Test URL → Export all → pull logs.

### D-7 — Standard-flavor Back at Home (INVESTIGATE)
- On the device, adb `KEYCODE_BACK` at Home did not exit the **standard** flavor, while the alt flavor did exit.
- Check which `OnBackPressedCallback`s are enabled at Home in the standard flavor, and log every Back dispatch decision: `BACK_DISPATCH` with `{screen, canGoBack, handledBy}`.
- Expected behaviour (platform default; HARNESS_BRIEF does not override it): Back at Home with no web history and no open dialog falls through to the system, i.e. the activity finishes.
- If the behaviour is intentional, say so in the README.

### H-6 — Representative cold-start build (NEEDS FOUNDER APPROVAL: decision F-H5)
- This item applies **only if the founder approves F-H5** (it changes HARNESS_BRIEF §7 "No release builds").
- Add a `benchmark` build type: `initWith(release)`, `isDebuggable=false`, `<profileable android:shell="true"/>`, R8 **on** (keep rules as needed), signed with the same CI debug key. Use the applicationId suffix `.bench` so it can be installed side by side.
- CI uploads it as a separate artifact.
- Without approval, skip H-6 and say so in the completion report.

### H-7 — Small items carried from the Lead's PR #7 review
- Fix the README APK file names so they match the actual CI artifact names.
- Show the **in-app** `multiProcessEnabled` value and the WebView renderer PID (if obtainable) on Device Info and in the export.

## 3. Acceptance (agent must self-verify in an emulator; the founder verifies on the device)

| # | Check | Agent evidence |
|---|---|---|
| A1 | Fresh launch: initial focus is on a non-text element; IME **not** open | Emulator screenshot (API 34 TV image) |
| A2 | From Home, D-pad alone reaches **every** button and list item and returns via Up | Screen recording or an ordered list of screenshots |
| A3 | URL entry: OK → IME; Done → page loads; Back/Down → leaves edit mode without loading | Screenshots / log lines |
| A4 | All Home text readable; contrast numbers listed for the text/background pairs | Table in the PR |
| A5 | Each D-5 hook works via `adb shell am start …` | Command + resulting log line for each |
| A6 | No file I/O on main thread: StrictMode `detectDiskWrites()` enabled in debug shows no `SessionLog` violations | Logcat excerpt |
| A7 | Standard flavor Back at Home: behaviour explained + `BACK_DISPATCH` lines | Log excerpt |
| A8 | CI green; existing unit tests pass; new unit tests for intent-extra parsing | CI link |

Emulator results are **emulator evidence only**. Do not mark any W0 test (T-xx/P-x) as PASS, because those statuses come only from the founder's device runs.

## 4. Process
- Keep PR #7's structure. Either push to a **new branch/PR "W0 harness M1.1"** based on PR #7 head (preferred if #7 is still open), or push from `main` if #7 has been merged.
- Bump `versionName` to `0.1.1-m1.1` and increment `versionCode`.
- Finish with the AGENTS.md §9 completion report, which must include the A1–A8 table, and list anything not done.
- Do not start M2 in this PR.
