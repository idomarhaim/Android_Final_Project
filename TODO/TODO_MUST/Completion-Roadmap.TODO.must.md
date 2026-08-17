# Completion roadmap — functionality first, Hebrew last

**Decision, Ido, 2026-08-17:** time constraints mean **all functionality must work
before Hebrew**. #51 is parked as OPEN, not reverted. The app ships English-only
until #51 resumes; publishing without Hebrew is acceptable.

Submission itself is **not** blocked — both items in
[Submission.TODO.must.md](Submission.TODO.must.md) closed on 2026-08-06. Everything
below is v0.3 product work.

---

## Why #51 was parked, in one paragraph

`SWEPT_PACKAGES` is at 2 of 10. The eight remaining are `auth challenges dashboard
goals health lifeareas profile social` — one sweep session plus a render pass each.
`51e-sweep-components` was the one package that promised leverage (sweep the shared UI
once, eight callers benefit) and it found the opposite: well-factored presentational
components hold no literals, because they take their copy as parameters. **So the
remaining work is per-package and irreducible** — there is no shortcut left to find.
The Hebrew *mechanisms* are all solved and pinned by tests; only the grind remains,
which is exactly the part that can wait.

---

## The order

### Wave 1 — runs **alone**, nothing else may run beside it

| | Brief | Why it must be first |
|---|---|---|
| 1 | `/kickoff hebrew-defer-freeze` | It withholds Hebrew at the picker and **suspends §0.8 in writing**. Until that block is in AGENTS.md, every feature session reads *"a design is not finished until it has been seen in Hebrew"* and re-blocks itself. It also touches `AGENTS.md`, `docs/`, `domain/model/`, `ui/locale/` and `ui/components/` — too wide to share. |

### Wave 2 — three lanes, genuinely disjoint

| Lane | Brief | Touches | Needs a build? |
|---|---|---|---|
| A | `/kickoff 50-offline-stamps` | `data/firestore/`, `feature/social/`, `feature/challenges/`, `functions/`, deletes `core/net/` | yes |
| B | `/kickoff 48-settings-surface` | `feature/profile/`, new settings screen, `ui/` | yes + device |
| C | `/kickoff docs-hygiene-backfill` **or** `/kickoff kb-drain-51e-backfill` | `scripts/`, `CHANGELOG/`, `kb-candidates/`, `C:\Dev\JARVIS\kb\` | **no** |

**Do A and B in either order or together; do C alongside whichever.** Lane C is the
free one — it compiles nothing, so it never contends.

### Wave 3 — the add-flow cluster, **strictly sequential**

`#6 → #7 → #9 → #11`, in that order. They all edit the same add-flow and the same
ViewModel, so they are **one working set** and cannot be parallelised however many
emulators are free. #6 (silent filing) changes the flow the other three sit inside, so
it goes first.

Alongside it, in the second lane: whichever of Wave 2's Lane C you did not run.

### Wave 4 — close out

| | Work | Why here |
|---|---|---|
| 1 | `#8` — notify when the sorter invents a goal | Depends on #6's silent-filing behaviour existing |
| 2 | Final verification pass — full suite, both accounts, `assembleDebug`, release-candidate check | Last |

**Wave 3 and Wave 4 briefs are deliberately not written yet.** Both recent sessions
reported their brief's premise was wrong — 51e's *"half wrong"*, `changelog-index-backfill`'s
*"wrong about the tool, and its numbers were off in the flattering direction"*. Briefs
written now against a codebase three sessions from now would repeat that. Write them at
the end of Wave 2, against HEAD.

---

## 🚥 Every session ends by telling Ido whether he may proceed — this is mandatory

**Ido's instruction, 2026-08-17.** He should never have to work out for himself whether
the next `/kickoff` is safe. So **the last thing in every session's final reply** — below
the three file lists, below `## ❓ Questions ready`, below any `## ⏳ WAITING` banner — is
**exactly one** of these two headings. Not a sentence in the body, not an inference from
the status block. A heading.

```
## 🚥 GO — NEXT: /kickoff <slug>
```
```
## 🚥 STOP — DO NOT KICKOFF YET — <what must happen first, and whose move it is>
```

**`GO` requires all six. Any one missing makes it `STOP`.**

1. **Your commit landed** — not "ready to commit", not "awaiting approval". Held on Ido's
   approval is a `STOP`, and the next line says so: *needs your OK to commit first*.
2. **Your board row is released** on `SESSIONS.md`, and your brief is closed to
   `sessions/done/` with `status: done` in the same commit.
3. **Your singletons are released** — Gradle daemon, and the emulator by name. A session
   that still holds the daemon cannot hand over.
4. **Nothing you did is knowingly broken** — tests green at every layer the project has,
   or explicitly stated as not applicable.
5. **The next brief's preconditions are actually met** — read them, don't assume. If your
   work was supposed to supply something (`hebrew-defer-freeze` supplies
   `AppLanguage.OFFERED`; the AGENTS.md suspension block), confirm it exists at HEAD.
6. **No `## ⏳ WAITING` or `## 📣 UNPUBLISHED` banner is open** in your own reply. If you
   are waiting on a sibling, that is a `STOP` even though the wait clears itself — Ido
   starting the next session on top of a contended file is the thing this prevents.

**Name the exact slug, never a wave.** *"proceed to wave 2"* is not an answer; `/kickoff
50-offline-stamps` is. Where two are legitimately available in parallel, name **both** and
say which to start first and why:

> `## 🚥 GO — NEXT: /kickoff 50-offline-stamps` (start here — no device needed) **or**
> `/kickoff 48-settings-surface`. One of `/kickoff docs-hygiene-backfill` /
> `/kickoff kb-drain-51e-backfill` may run alongside either — it compiles nothing.

**Where the next brief does not exist yet, that is a `STOP` with a specific next.** Waves
3 and 4 are deliberately unwritten (see below), so the **last wave-2 session to finish**
ends with:

> `## 🚥 STOP — DO NOT KICKOFF YET` — waves 3–4 have no briefs. **next:** one short
> session to write them against HEAD, then `/kickoff` the first.

### The per-session answers, so no session has to derive them

| Session | On success, `GO` to |
|---|---|
| `hebrew-defer-freeze` | `/kickoff 50-offline-stamps` **or** `/kickoff 48-settings-surface`; plus one Lane C alongside |
| `50-offline-stamps` | `/kickoff 48-settings-surface` if it has not run; if it has, `STOP` — the wave-3 briefs are owed |
| `48-settings-surface` | `/kickoff 50-offline-stamps` if it has not run; if it has, `STOP` — the wave-3 briefs are owed |
| `docs-hygiene-backfill` | `/kickoff kb-drain-51e-backfill`, and whatever build lane is free |
| `kb-drain-51e-backfill` | `/kickoff docs-hygiene-backfill`, and whatever build lane is free |

---

## What can and cannot run in parallel — the actual constraints

**The bottleneck is not the emulators.** Two AVDs exist (`Pixel_10_Pro_XL`,
`Pixel_10_Pro_XL_B`) and `scripts/run-goalpilot.ps1` supports one each. But:

1. **The Gradle daemon is a singleton** and `SESSIONS.md` carries it as one.
   `gradle.properties` sets `org.gradle.parallel=true`, which parallelises *inside one
   build*, not two. Two concurrent invocations contend on `app/build/` and KSP output —
   the *"Could not delete/move …"* Windows lock failure already documented in
   [CLAUDE.md](../../CLAUDE.md). **Claim it, build, release, hand over.**
2. **RAM.** `-Xmx2560m` per daemon × 2 plus two AVDs; the submission TODO already notes
   one ANR from host RAM with just the emulators up.
3. **`connectedDebugAndroidTest` uninstalls the app**, taking the Google sign-in with it.
   So: **dedicate `Pixel_10_Pro_XL_B` to instrumented runs** and keep `Pixel_10_Pro_XL`
   for manual verification. That is the one thing the second emulator genuinely buys.
   Every session that runs it must announce `## 📱 DO NOT SIGN IN` **before** the device
   command, not afterwards.
4. **Two files any two lanes will both touch** — `SESSIONS.md` (contested by
   construction; a pathspec commit cannot protect it) and, if both sweep,
   `AnalyticsLiteralSweepTest.kt`. Read `git diff -- SESSIONS.md` in its own tool call
   before committing it.
5. **The pre-commit hook** (`a1aa041`) refuses any commit whose changelog file is not in
   the generated index. Run `scripts/New-ChangelogIndex.ps1`. It was installed
   **mid-flight** on 2026-08-17 and changed a live session's behaviour with no warning
   artifact — expect it, don't rediscover it.

**Ceiling: two build lanes, and only if their packages are disjoint.** A third lane is
safe only if it compiles nothing (Lane C above).

---

## Why this is not one session driving subagents

Asked and answered on 2026-08-17. Subagents share **one** filesystem, **one** git index
and **one** Gradle daemon, and `agent-topology-and-model-routing.md` §6 excludes them for
sequential edits for exactly that reason. The evidence is already on the record at the
*session* level, where the protections exist: a hook installed mid-flight changed a live
session's behaviour, 51e had to fold a sibling's file back into its changelog, and one
push carried three foreign commits. Sessions have a board, claims, release notes and
separate transcripts. **Subagents have none of that.**

And each of these sessions' real value came from things a subagent cannot deliver:
rendering on the emulator and *looking*, deciding to keep `GoalCategory.label` rather
than delete it, catching its own miscount before committing. Those are mid-flight
judgment calls with approval gates attached.

**The copy-paste problem is solved by a different mechanism:** `sessions/<slug>.md` plus
`/kickoff <slug>`. One line typed per session, no paste. That is what the briefs beside
this file are.
