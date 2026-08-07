<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# 🧭 Session claim board — GoalPilot

Who is working on what, **right now**. Read this before your first edit; claim
before your first write. Normative rule:
`C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.

> Running more than one session at a time is **opt-in and not the default**. It
> is legitimate only when the working sets are disjoint and the user has
> assigned them. If two claims would overlap, it is one session's work — run it
> sequentially.

## 🔒 Active claims

| Session | Task | Owns (paths) | Singletons | Claimed |
|---|---|---|---|---|
| `product-model-map` | `/kickoff product-model-map` — chart the GoalPilot v0.3 product-model `/wayfinder` map over the 13 decisions `C1`–`C13`. **HITL, normal mode**: it grills Ido and cannot be run AFK. Charting only — no ticket is resolved | `TODO/TODO_FUTURE/ProductModel.TODO.future.md`, `sessions/product-model-map.md`, `CHANGELOG/2026-08-06/product-model-map.md`, `kb-candidates/2026-08-06-product-model-map.md`, plus GitHub issues and the `wayfinder:*` labels on `idomarhaim/Android_Final_Project` | **none** — no build, no device, no Firebase. Not `#gradle-daemon`, neither AVD | 2026-08-06 |

> **Issue-tracker partition, left here because `product-model-map` is still live.**
> The tracker is shared and partitioned by *content*, not by turn-taking:
> `product-device-pass` filed the reproduced `D` defects and the `U1`–`U6` UX items
> from `TODO_OPTIONAL/` as **[#2](https://github.com/idomarhaim/Android_Final_Project/issues/2)–[#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)**;
> `product-model-map` files `wayfinder:*` decision tickets from `TODO_FUTURE/`, and
> should number from there. Neither files from the other's list — a `C` item filed
> twice is how a map ends up with two sources of truth.
>
> ⚠️ **`product-model-map`: there is a 14th decision waiting for you.** Ido's call,
> 2026-08-07 — `D1` resolved to *not a defect but an undecided model question*
> (**"what should a challenge score from?"**) and belongs in your map beside `C7`,
> not bolted on afterwards. Because `TODO/TODO_FUTURE/` is yours, this session did
> **not** write there. Instead there is a **fenced, liftable `C14` block** you can
> paste verbatim, at the end of the `D1` entry in
> `TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md` — decision stated, four
> candidate answers enumerated, the `C7` dependency and the anti-cheat coupling
> named. Evidence: `CHANGELOG/2026-08-06/product-device-pass.md`.

## 📏 Rules

0. **This file existing is the trigger.** Read it before your first edit — not
   "if someone else might be here". Whether they are is what this file tells you,
   so skipping it means you have no evidence you were allowed to skip it.
1. **Claim before writing.** Add your row, commit it, then work. If a row was
   written *for* you by another session from files it saw change, that is a
   report, not a claim — confirm and correct its path list before you continue.
2. **Never write outside your paths.** If you need a path another session owns,
   say so and let the user re-assign — do not "just quickly" edit it.
3. **Never blanket-stage** — `git add -A`, `git add .`, `git add --all`,
   `git commit -a`. Not "while another session is live": you cannot know that
   until you have read this board, and by then you have already staged. Explicit
   paths always; it costs nothing on the days you are alone.
4. **Singletons are exclusive.** Builds and device work serialise. Claim, use,
   release — and check the table below before your first **build or device
   command**, not only before your first edit.
5. **Release when done** — clear your row on `/handoff`, on finish, or when
   abandoning. A stale claim blocks work nobody is doing.
6. **The agent recommends, the user assigns.** A session that sees unclaimed work
   fitting its context emits one line —
   `🧭 **Claim:** <candidate> → owns <paths>; conflicts: <none|paths>` — and waits.
   It never self-assigns.

### Singletons in this repo

| Singleton | Why it matters here |
|---|---|
| Gradle daemon / `.gradle` locks | Two `gradlew` runs contend; one blocks or dies mid-write. **This, not the emulator, is what actually serialises two sessions** — see below |
| The git index | Never `git add -A` — stage explicit paths |
| Emulator `Pixel_10_Pro_XL` (`adb`) | One screen, one driver. Installing/driving the app is exclusive |
| Emulator `Pixel_10_Pro_XL_B` (`adb`) | Second device, added 2026-08-05 for the two-account demo. Same rule, claimed separately: `run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B` |
| Firebase project `goalpilot-56e30` | Live Firestore/Storage/Functions — concurrent writes are attributable to nobody |

**Two emulators do not buy two parallel verifications.** Both AVDs exist so that
*one* session can drive two signed-in accounts at once (the spec §7 sharing demo),
not so that two sessions can each run `:app:connectedDebugAndroidTest`. Those two
runs would still queue at the Gradle daemon above, and worse, each would build an
APK from the *other* session's uncommitted edits — one working tree, one
`app/build/`. Real parallel instrumented testing needs a second checkout, which
this repo has deliberately not adopted.

## 🗂️ Unclaimed work

Where to look, in order: [`TODO/TODO.md`](TODO/TODO.md) (MUST → OPTIONAL →
FUTURE), then open issues. `/claim` reads both and proposes a fit.

Currently unclaimed and ready:
- **Two written briefs, one session each** — `/kickoff product-device-pass` and
  `/kickoff product-model-map`, both from the 2026-08-06 product/UX intake. They
  are **disjoint and can run in either order or concurrently**: the device pass
  owns `TODO/TODO_OPTIONAL/`, GitHub issues, and both singletons (emulator +
  Gradle daemon); the map owns `TODO/TODO_FUTURE/` and the issue tracker's
  `wayfinder:*` labels, and takes no device at all. The map session is **HITL** —
  it grills Ido and cannot be run AFK.
- ~~**Two-account demo + spec title page**~~ — **effectively closed 2026-08-06.**
  The sharing demo was done on 05/08 (`CHANGELOG/2026-08-05/submission.md`,
  rules deployed and a non-owner join proven), and the title page turns out to be
  filled in already: `GoalPilot_spec_EN.docx` is modified in the working tree and
  reads `Submitted by: Ido · [Ido Mar-Chaim 209497072] · [10208]`. **Confirmed
  and ticked 2026-08-06** — the template's square brackets survive around both
  values and Ido ruled them cosmetic. **Every MUST item is now closed; nothing
  blocks submission.** The docx stays *Frozen / off-limits* in `AGENTS.md`, was
  not touched by any agent, and is Ido's to commit.
- **Health Connect on a physical phone** — small follow-up to the shipped feature,
  and **bigger since 2026-08-05**. The emulator carries the provider but its store
  is empty, so the reading → Firestore write path has never run against real step
  data — and that path now runs unattended, with no review sheet to catch it. The
  top-up arithmetic (today logged at 09:00, walked, opened again at 18:00) has only
  ever been exercised against fakes; a phone is the only place it can be watched.
- ~~One written brief, its own session: `/kickoff challenges-ui`~~ — **done
  2026-08-05.** All three briefs are now in `sessions/done/`; there is no
  unworked brief left. What challenges left behind is folded into the two-account
  item above: **deploy `firestore.rules`**, then verify a *non-owner* join
  against the live backend. Both want the second account, so both belong to that
  sitting rather than to a session of their own.
- ~~One `time-insights` verification is still open~~ — **done 2026-08-04.** Both
  blocked checks ran once the AVD came free: `:app:connectedDebugAndroidTest` 20/20
  green, and a live re-estimation run that returned 105 minutes for a five-word
  title, which the client heuristic (ceiling 60 for five words) and the Cloud
  Function's flat 30 both cannot produce. One of eight candidates matched a
  fallback and was unticked automatically. See
  `CHANGELOG/2026-08-04/time-insights.md` → "Verified against the live model".

**Disjointness**, checked 2026-08-04 and left here so it need not be re-derived:
- The three briefs' **paths** were disjoint — `feature/challenges/`,
  `feature/lifeareas/` + `feature/goals/`, and `feature/analytics/` +
  `functions/src/index.ts`. Two ran concurrently and never collided.
- Their **verification was not**, and that is the part that actually bit. All three
  touch composables, so all three want `:app:connectedDebugAndroidTest`, and the
  emulator is one exclusive singleton — `time-insights` released with its
  instrumented layer unrun because `lifearea-polish` held the AVD. **Disjoint paths
  do not make sessions independent; the device does.** When two sessions both end
  at a composable, expect one of them to hand its device check to the other.
- The **Gradle daemon** turned out to be shareable by queueing, not by claiming: a
  build during a sibling's mid-edit fails on *their* half-written file, which reads
  as your own compile error until you look at the path.
- `challenges-ui` stays clear of `functions/src/index.ts` only because standings
  are computed client-side. A later session moving them server-side collides with
  what `time-insights` already landed.

## 📓 Recently released

| Session | Task | Released | Landed in |
|---|---|---|---|
| `product-device-pass` *(2nd sitting)* | Re-claimed to close the one item the first sitting left `unverified` — the **first-run empty states** — with Ido's approval to `pm clear`. It **did not** reach the zero-data states (signing back in restores everything from Firestore, as predicted) and says so; a throwaway account is the only remaining route. It **did** find `A10`: a cold, cacheless first load is a **blank page and a single ~8 px dot** for ~10 s — what every user gets on a new phone. Also recorded an environment trap: `pm clear` wedges Play Services on this emulator (`SignInActivity` focused, rendering nothing, through two retries), and `am force-stop com.google.android.gms` clears it — the discriminator being that `dumpsys window \| grep mCurrentFocus` names GMS, not the app. `D1` handed to `product-model-map` as a **liftable `C14` block** rather than written into their file. Wrote `sessions/fix-task-completion-feedback.md` for issue #3. **No suite run, none applicable**; app restored to exactly as found (Aurora, 70 pts, 7 goals) and verified; `goalpilot-56e30` read-only; `#emulator` **released** | 2026-08-07 | this commit |
| `product-device-pass` | **Read-only against the code; Markdown and issues only.** Drove a real debug build on `Pixel_10_Pro_XL` as Ido to turn the `product-review` backlog from static claims into verdicts, then filed **the repo's first GitHub issues, [#2](https://github.com/idomarhaim/Android_Final_Project/issues/2)–[#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)**. `D2`/`D4`/`D5` confirmed (the accessibility tree names exactly which nodes are clickable — the life-area row and goal count are not, and the whole social feed card has **zero** interactive nodes); `D3` **measured at 2.24 s and 1.94 s** from frame-timed recordings, with the cause found: `setDone` is a **server-only Firestore transaction** whose `Resource` `toggleTask` discards — which is also why the identical tap is a **silent no-op offline** (`A5`), so both were filed as one issue. `D1` **reclassified, not filed**: a challenge's score has exactly one writer (`reportScore`) and `ChallengeType` is decorative, so "what should a challenge score from?" is an undecided model question — it belongs in `TODO_FUTURE/`, which the live `product-model-map` session owns, and was deliberately left unmoved (see the ⚠️ above). Device half of the UX pass added as `A5`–`A9`, plus a *checked-and-not-a-defect* section (both skins in dark are fine; **GROQ is live, not falling back**; the FAB clears the last card). **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; verification was behavioural (`uiautomator` dumps, VFR `screenrecord`, PSNR, logcat). **Not verified, and said so:** first-run empty states, which need `pm clear` or a throwaway account. `#emulator` `Pixel_10_Pro_XL` and `#gradle-daemon` leased and **released**. Live `goalpilot-56e30` **was** touched — one task toggled done and back to time `D3`, **restored and verified** (`2 / 100 %`, `70 pts`); theme switched to Blossom for the dark check and **restored to Aurora** | 2026-08-06 | this commit |
| `product-review` | **Markdown only.** Intake of Ido's 2026-08-06 pre-sleep product/UX brief: faithful English transcription (`R1`–`R28`) beside the `.docx`, the actionable half (`D1`–`D5`, `U1`–`U6`, `A1`–`A4`) split from the 13 product-model decisions (`C1`–`C13`) bound for a `/wayfinder` map, plus two session briefs for the halves. Deliberately **no device pass, no GitHub issues, no map** — every repro note is static, and this repo has been burned once by a stale backlog premise, so nothing graduates until it is reproduced. Headline finding: the reported task-score/goal-percentage "bug" is **not a defect** — `Task.points` and `currentValue/targetValue` are independent by construction, joined only by `progressContribution` (default `1.0`, invisible in the UI) — so it was reclassified to decision `C3` rather than filed. Also recorded: no `values-he`, so the app has no Hebrew and no RTL, which is not in the brief. No `app/`, `functions/`, `firestore.rules` or `scripts/` file touched; **no suite run and none applicable**; neither AVD nor `#gradle-daemon` taken; live `goalpilot-56e30` untouched | 2026-08-06 | this commit |
| `release-distribution` | Signed release key, Firebase App Distribution on both ends (upload plugin + in-app update prompt), and a tag-triggered release workflow — so an APK reaches other people's phones and updates itself afterwards | 2026-08-06 | `5316782`, `1f41b50`, `40cfc12`, `356613d`, `7e21ab1`, `964d6e9`; see `CHANGELOG/2026-08-05/release-distribution.md`. **Proven end to end on a physical phone:** install → Google Sign-In under the release key → in-app update `v0.2.1` → `v0.2.2` in one tap. 197 JVM green; instrumented and rules suites not run (no UI, no rules file touched). `#gradle-daemon` leased twice and **released**; neither AVD taken. Live `goalpilot-56e30` **was** touched — release SHA-1, `testers` group, service account — all additive, all listed in `docs/RELEASING.md`. Note for the next session: **`v0.2.2` was built and uploaded from the developer machine**, because GitHub could not allocate a hosted runner on two attempts (15-min acquisition timeout, zero steps). CI itself is fine — `v0.2.1` went green on the same file |
| `kb-audit` | **Ingest only, Markdown only** — this repo's share of a cross-repo KB-candidate sweep run from `C:\Dev\GenAI-Driven-Dev-Self-Improvement`. **New** [`knowledge/release-distribution.md`](knowledge/release-distribution.md) from `CHANGELOG/2026-08-05/release-distribution.md` (off-Play means Android supplies no update mechanism; the signing key is unrecoverable so it precedes the first hand-out; tag-triggered because `versionCode` is manual), plus the bundle index and journal, and the missing `CHANGELOG_README.md` row for that 08-05 session file. Three claims from `second-avd`, `submission` and `release-distribution` generalise past GoalPilot and were ingested **centrally** instead (PowerShell 5.1 encoding traps · second-AVD mechanics · an authorization rule needs a second real identity). No `app/`, `firestore.rules`, `functions/` or `scripts/` file touched; neither AVD nor `#gradle-daemon` taken; no suite run. **Recorded rather than papered over:** the row went in *with* the commit rather than before the first write — the tree was clean and the only Active row owns disjoint paths, but the ordering rule says claim first and this session did not. **Also noted, not touched:** the `release-distribution` Active row is stale — its work is committed and pushed (`5316782`), so it is a release somebody owes | 2026-08-06 | this commit |
| `template-sync-v16` | 🔁 **Mechanical template sweep**, driven from JARVIS by `Update-TemplateConsumers.ps1`: `general.instructions.md` **v14 → v16**, `new-changelog-entry.prompt.md` v3 → v4, `AGENTS.md` v12 → v14. Clears the v14 → v15 gap the 2026-08-04 sweep **correctly** refused while `challenges` held a dirty tree — that tree is clean now, so both versions landed in one pass. Verbatim projections only; no decision was taken in this repo, no Kotlin/Gradle/Firestore file touched, and neither `#emulator` nor `#gradle-daemon` was taken. **No Active row was claimed:** a single-commit mechanical sync into a clean, unclaimed tree, so a claim created and cleared in the same breath protects nothing (`C:\Dev\JARVIS\rules\scale-adaptive-ceremony.md`, *mechanical sync* row) | 2026-08-05 | this commit |
| `backend` | Live Firebase backend, E2E verification, Google Tasks import, JARVIS §5 governance | 2026-07-31 | `6e4a184`, `79ce624`, `1ebb178`, `53c2afb`, `64802e5`, PR #1 |
| `launchers` | One-click run scripts; made the emulator singleton self-enforcing | 2026-08-01 | `dc1c06e` + follow-up (pending) |
| `health` | Health Connect integration — steps & sleep, read-only, review-before-write | 2026-08-02 | see `CHANGELOG/2026-08-02.md`; emulator released |
| `theming` | Selectable app skins (Aurora/Blossom) + UI/UX pass | 2026-08-02 | `e31ac9d`, `a413485`, `c30709e`; emulator released |
| `scaffold` | Template-library upgrade — `AGENTS.md` v8→v10, `general.instructions.md` v10→v12, this file v1→v2 | 2026-08-03 | see `CHANGELOG/2026-08-03.md` |
| `lifeareas` | Life areas (user-defined + synced from Google Tasks list names), LLM task durations, interactive time-allocation analytics at day/week/month/quarter/year | 2026-08-03 | `fe9f61d`; see `CHANGELOG/2026-08-03/lifeareas.md`. Emulator released. |
| `challenges` | Competitive challenges: the `participants` security rule that makes joining possible, `firestore-tests/` (the repo's first rules test layer), and the domain + data + DI layers | 2026-08-04 | `1e56ee3`, `8117368`; see `CHANGELOG/2026-08-04/challenges.md`. **Rules written and tested but NOT deployed.** UI continues in `sessions/challenges-ui.md`. Emulator never claimed in practice; Gradle daemon released. |
| `second-avd` | Second emulator `Pixel_10_Pro_XL_B` for the two-account demo; `-Avd` made a demand so it never adopts the other session's screen | 2026-08-05 | see `CHANGELOG/2026-08-05/second-avd.md`. No app code touched, so no suite was run — verification was behavioural against both live emulators. Both AVDs left running; neither claim held. |
| `time-insights` | A stacked-column trend beside the time-allocation donut, and an AI re-estimation pass for tasks that never had a duration | 2026-08-04 | `342af48` + verification pass; see `CHANGELOG/2026-08-04/time-insights.md`. **All layers green and fully verified**: 150 JVM, 20 instrumented, and a live re-estimation run against GROQ that wrote 7 durations to `goalpilot-56e30`. Ran in two sittings — released once with the device checks blocked, re-claimed when the AVD came free. Emulator and Firebase project **released**. |
| `lifearea-polish` | Drag-to-reorder life areas (minimal `sortOrder` writes) and the goals list banded by life area | 2026-08-04 | `6f4a749`; see `CHANGELOG/2026-08-04/lifearea-polish.md`. Both layers green — 144 JVM, 20 instrumented. Emulator `Pixel_10_Pro_XL` recovered from a wedge and **released**; Gradle daemon released. |
| `submission` | Deployed `firestore.rules` to live `goalpilot-56e30` and proved the **non-owner challenge join** end-to-end with two real accounts on both AVDs; captured the spec §7 sharing evidence | 2026-08-05 | see `CHANGELOG/2026-08-05/submission.md`. **16/16 rules tests** re-run against the deployed file; JVM and instrumented suites **not run** — no Kotlin/Gradle file changed. Both emulators and the live project **released**. Found the MUST item's premise stale: two profiles, both friend edges and a share had existed since 02/08, so §7 needed capturing rather than building. Health Connect re-checked on API 37 by request — permissions and the empty read are fine; the physical-phone follow-up stands |
| `health-autosync` | Health Connect made automatic: syncs on every app foreground, throttled to 15 min by a per-uid SharedPreferences stamp, and writes every unsynced reading with no review sheet. The dedupe became a *value* comparison so today is topped up rather than frozen at its first reading | 2026-08-05 | see `CHANGELOG/2026-08-05/health-autosync.md`. **197 JVM and 29 instrumented green**; rules untouched, so `firestore-tests/` not run. Throttle proven in both directions against the on-device stamp, not the UI. **Still unproven: the write path against real step data** — the emulator's store is empty, so the physical-phone follow-up below stands and now covers the top-up path too. Emulator `Pixel_10_Pro_XL` and the Gradle daemon **released** |
| `challenges-ui` | Competitive challenges as a real screen: ViewModel, live standings, discover/join/leave, score reporting, create flow | 2026-08-05 | see `CHANGELOG/2026-08-05/challenges-ui.md`. All three layers green — 175 JVM, 29 instrumented, 16 rules. Ran against an empty board, uncontended. Emulator `Pixel_10_Pro_XL` and the Gradle daemon **released**. The live project `goalpilot-56e30` was **never touched** — the rules deploy is held for the two-account session on Ido's call, so a non-owner join is still proven by `firestore-tests` only. |

> **Post-mortem, recorded because the next session should not repeat it.** The
> `theming` session ran for two days without ever reading this board — it did not
> exist when that work started, and the `AGENTS.md` it read (template v4) had no
> pointer to it. Consequences: it used `git add -A` (the one thing rule 3 forbids
> by name), wrote to `feature/dashboard/DashboardScreen.kt` and
> `di/RepositoryModule.kt` while `health` owned them, and used the Gradle daemon
> and the AVD while `health` held both. Nothing was actually lost — the two
> sessions happened to edit different regions of `DashboardScreen.kt`, and the
> `add -A` landed in a window where the tree held no sibling work — but only by
> luck. The rule text and enforcement were tightened in JARVIS §5 as a result.
