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

### Wave 1 — ✅ **DONE 2026-08-17, `7baf120`** (ran alone, as required)

| | Brief | Status |
|---|---|---|
| 1 | `/kickoff hebrew-defer-freeze` | **Steps 1–4 shipped.** `AppLanguage.OFFERED` is the single switch and it closed **three** doors, not the two the brief named — the third was **persistence**, where `AppPreferencesRepositoryImpl` read `AppLanguage.fromId(stored)` and `fromId` faithfully returns `HEBREW` for a stored `"he"` whatever the picker offers. So the freeze would have held everywhere except devices that had actually used Hebrew — including Ido's. Closed by `offeredFromId` on the preferences read path only; `fromId` stays whole because #51 needs the full id round-trip. §0.8's suspension block is in `AGENTS.md` at HEAD. JVM unit **364/0** (+6). |
| 2 | `/kickoff 51-freeze-verify` | **Owed — steps 5 and 6 never ran**, both because the harness classifier denied `adb` and `gh` writes. The render pass proving *"the app is uniformly English"* has not happened, and three #51 writes are unposted. **Ido granted both permissions on 2026-08-17**, so it is runnable. Short session. |

**Run `51-freeze-verify` BEFORE `48-settings-surface`** — revised 2026-08-17 once the
permissions were granted. It does not *gate* wave 2 logically (nothing in wave 2 reads the
Hebrew path, and the freeze is proven by six JVM tests), but **the evidence it needs expires**:
if `Pixel_10_Pro_XL` still holds a stored `"he"` from 51e's Hebrew render, that device is the
only place the pre-freeze state can be observed — and `48-settings-surface` is the session that
would boot and reinstall over it. Ordering costs nothing; losing the observation is permanent.

`50-offline-stamps` needs no device, so it may run **alongside** `51-freeze-verify` — the two
contend only on the Gradle daemon, which the board's singleton column already serialises.

### Wave 2 — three lanes, genuinely disjoint

| Lane | Brief | Touches | Needs a build? |
|---|---|---|---|
| A | `/kickoff 50-offline-stamps` | `data/firestore/`, `feature/social/`, `feature/challenges/`, `functions/`, deletes `core/net/` | yes |
| B | `/kickoff 48-settings-surface` | `feature/profile/`, new settings screen, `ui/` | yes + device |
| C | `/kickoff docs-hygiene-backfill` **or** `/kickoff kb-drain-51e-backfill` | `scripts/`, `CHANGELOG/`, `kb-candidates/`, `C:\Dev\JARVIS\kb\` | **no** |

**Order: `51-freeze-verify` → A → B, with C alongside any of them.** A and B are disjoint and
could run together, but B must not precede `51-freeze-verify` (see the expiring-evidence note
above). Lane C is the free one — it compiles nothing, so it never contends with anything.

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
the next `/kickoff` is safe. So **the last thing in the reply that reports the unit
finished or abandoned** — below the three file lists, below `## ❓ Questions ready`, below
any `## ⏳ WAITING` banner — is **exactly one** of these two headings. Not a sentence in
the body, not an inference from the status block. A heading.

**Which reply, exactly — because "final reply" was too loose.** It goes on the reply that
**closes the unit of work**, and on no other. **Not** on a mid-session progress turn, and
**not** on a turn that only answered a question or ran a read-only check. A session that
did no work has nothing to hand over, and a 🚥 heading on such a turn is noise that
teaches Ido to skim past the one place it matters. If you are unsure whether your turn
closes the unit, it does not.

```
## 🚥 GO — NEXT: /kickoff <slug>
```
```
## 🚥 STOP — DO NOT KICKOFF YET — <what must happen first, and whose move it is>
```

**`GO` requires all seven. Any one missing makes it `STOP`.**

1. **Your commit landed** — not "ready to commit", not "awaiting approval". Held on Ido's
   approval is a `STOP`, and the next line says so: *needs your OK to commit first, then
   GO to `<slug>`*. **In normal mode this is the ordinary case, not the exception**, so
   most sessions will end on a `STOP` naming the commit approval — and the same line
   already names the slug that follows, so Ido's *"yes, commit"* is the whole hand-off.
2. **Your commit is *pushed*, or you say plainly that it is not.** Added 2026-08-17 by the
   fallback check below, which found this missing against a real session: `51e-sweep-components`
   had a landed commit, a released row, released singletons and a green suite — it would
   have scored `GO` on the original six conditions **while its push was held on Ido's
   decision.** The next session would then build on unpublished work. An unpushed commit
   is not automatically a `STOP` — a held push is often correct — but it must be **stated
   on the 🚥 line**, dated, with what is holding it: `GO — NEXT: /kickoff <slug>; note: 1
   commit unpushed, held on <reason>, still unpublished as of <check>`.
3. **Your board row is released** on `SESSIONS.md`, and your brief is closed to
   `sessions/done/` with `status: done` in the same commit.
4. **Your singletons are released** — Gradle daemon, and the emulator by name. A session
   that still holds the daemon cannot hand over.
5. **Nothing you did is knowingly broken** — tests green at every layer the project has,
   or explicitly stated as not applicable.
6. **The next brief's preconditions are actually met** — read them, don't assume. If your
   work was supposed to supply something (`hebrew-defer-freeze` supplies
   `AppLanguage.OFFERED`; the AGENTS.md suspension block), confirm it exists at HEAD.
7. **No `## ⏳ WAITING` or `## 📣 UNPUBLISHED` banner is open** in your own reply. If you
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

**The slug each session names.** Whether the heading is `GO` or `STOP` depends on the seven
conditions — in normal mode it is usually `STOP` on the commit approval — but **the slug is
the same either way**, so it is never left for Ido to look up.

| Session | Names this slug |
|---|---|
| `hebrew-defer-freeze` | `/kickoff 50-offline-stamps` **or** `/kickoff 48-settings-surface`; plus one Lane C alongside |
| `50-offline-stamps` | `/kickoff 48-settings-surface` if it has not run; if it has, `STOP` — the wave-3 briefs are owed |
| `48-settings-surface` | `/kickoff 50-offline-stamps` if it has not run; if it has, `STOP` — the wave-3 briefs are owed |
| `docs-hygiene-backfill` | `/kickoff kb-drain-51e-backfill`, and whatever build lane is free |
| `kb-drain-51e-backfill` | `/kickoff docs-hygiene-backfill`, and whatever build lane is free |
| `51-freeze-verify` | whichever wave-2 slug is still unrun; if wave 2 is finished, `STOP` — the wave-3 briefs are owed |

---

## 🔀 Every brief in this plan runs in AUTO MODE — Ido's standing instruction, 2026-08-17

> *"I want all the sessions to be in AUTO MODE as long as they verify they are not harming
> other sessions' work."*

Each brief's front matter carries `mode: auto`. That is the **only** sanctioned way the mode
crosses a session boundary, and it crosses as **Ido's stated intent**, not as a leftover — so a
session reading `mode: auto` in its brief is in auto mode from its first turn and does not need
to be told again.

### What it grants

**Commit** when a unit is done, **push** when it is finished, and **drain KB candidates** at the
commit trigger — all three without asking. Say in the reply whenever the mode acted; silent
autonomy is how a stale marker goes unnoticed.

### What it never grants — read this before assuming it covers you

Auto mode is the **dev** half of autonomy. It is repo-bounded, and none of the following moves:

- **Deletions stay always-ask.** The one carve-out is a `kb-candidates/` file whose *every*
  entry was promoted, deleted in the same commit as the promotion. **`#50`'s deletion of
  `ConnectivityMonitor` is authorised by its own ticket**, not by this mode — name it in the
  reply and do not extend it by one file.
- **Outward-facing actions stay draft-then-ask** — email, calendar, publishing, sharing, third
  party. **`51-freeze-verify`'s three `gh` writes are outward**: Ido granted them for *that
  task* on 2026-08-17, and that grant does not widen to any other session or any other write.
- **Destructive git stays always-ask** in both modes: `--force`, `--force-with-lease`,
  `--delete`, moving a published tag, pushing a rebase of published commits, `git reset --hard`.
- **Not pushes, and still always-ask:** opening or merging a PR, creating or deleting a remote
  branch or tag, publishing a release, changing repo visibility or settings.
- **A `rules/`-destined KB candidate**, and anything that supersedes or contradicts a standing
  KB claim. Both stay parked and named, never dropped.
- **Rewriting anything that predates JARVIS** (2026-07).

### The condition Ido attached — verify you are not harming a sibling

Before the commit, and again before the push:

1. **Read the whole Active-claims section** of `SESSIONS.md` to its next `## ` heading — never
   the first N lines. Count rows mechanically:
   `awk '/^## 🔒 Active claims/{f=1;next} /^## /{f=0} f' SESSIONS.md | grep -c '^| '`.
2. **Claim your paths before your first write**, in **every** repo you write into — a
   `/kb-ingest` into `C:\Dev\JARVIS\kb` owes a row on that board too.
3. **Never blanket-stage.** No `git add -A`, `git add .`, `git commit -a`. Explicit paths only,
   on the days you are alone as well.
4. **Commit explicit paths too** — `git commit -F <msg> -- <path> …`. A bare `git commit`
   commits the *index*, so it publishes whatever a sibling has staged.
5. **Read a shared file's own diff in its own tool call** before committing it —
   `git diff -- SESSIONS.md`. A pathspec commit cannot subtract a sibling's hunk from a file you
   both write, so if one is there, **name it in the commit message**. Never `--amend` a shared
   file.
6. **Before pushing:** `git fetch`, then `git log @{u}..HEAD` and `git diff --stat @{u}..HEAD`.
   A foreign commit whose paths sit under a **live** board row → **stop and ask**. An absent row
   is not proof a session finished; a recent commit of theirs, or their paths dirty in the tree,
   means live.
7. **A check that decides whether an action is safe gets its own tool call.** A gate chained
   into the same shell command as the thing it gates reports after the fact and is not a gate.

**Its honest limit, so nobody trusts it further than it goes.** The window on a sibling's file
opens when *you write it*, not when you stage it — their `git add <path>` reads the **working
tree** and never your index. So none of the above stops a sibling publishing your work under
their commit message. The remedy is **naming what rode along**, not preventing it. Auto mode
does not change this either way; it just means nobody is asked first.

### Two environment facts every session below should stop working around

- **GitHub's API is healthy again, 2026-08-17.** A several-hour partial outage 503'd every
  **GraphQL** call — `gh issue view`, `gh pr view`, Projects — while REST kept working, which is
  why briefs written that afternoon told sessions to use `gh api repos/:owner/:repo/...`. Both
  halves work now; use plain `gh issue view` and stop reaching for the REST form as a
  workaround. **This does NOT unblock the three owed #51 writes** — those were denied by the
  **harness classifier**, not by GitHub, and they need Ido's permission. Two different blockers
  that happened to look alike.
- **`adb` and `gh` writes may be denied by the harness classifier.** Wave 1 hit both. That is an
  outward-action gate and **not** something to route around: report the exact command as owed,
  on the 🚥 line, and let Ido decide. A step attempted and denied is never reported as done.

### How this wording was checked — Ido waived the walkthrough, so the mechanical half ran

Ido answered the 🎬 offer with **`waive`** on 2026-08-17: the gate was considered and
refused, which closes the *judgment* question (is this the behaviour he wants — yes, he
asked for it) and leaves the *mechanical* half owed. Run against the two recorded instances
of the failure it addresses, which are the only two sessions on the board that ended with a
hand-off decision in play:

| Instance | Should it fire? | Did it? |
|---|---|---|
| `changelog-index-backfill`, 2026-08-17 — all done, committed **and pushed**, row released, one out-of-scope defect left open | `GO` | **Yes, correctly.** The one open defect is another subsystem's and blocks no kickoff. |
| `51e-sweep-components`, 2026-08-17 — commit landed, row released, singletons released, 358/0 and 70/0 green, **but push held** and the #51 comment owed | `GO` **with the held push stated** | **No — it scored a bare `GO`.** The original six conditions never mentioned publication, so a session would have handed over while its work sat unpushed. **This is what condition 2 was added for.** |

**And the half that cannot be faked — where it must stay silent.** The original wording said
*"every session's final reply"*, which fires on a mid-session progress turn and on a turn
that only answered a question. Three replies in the very conversation that produced this
document were pure question-answering with no work done; a 🚥 heading on each would have
been noise, and noise is what teaches Ido to skim past the heading on the one turn it
matters. Hence the *reply that closes the unit of work* narrowing above.

**Necessity, since a waived gate still owes it:** Ido asked for this directly, and the
transcript shows him deriving the answer by hand — *"is development stuck"*, *"can #51 run
in parallel"*, *"which order and what can I run at once"*. The requirement removes a
derivation he was already performing, so it is not speculative ceremony.

**What this check could NOT test, stated rather than glossed:** the corpus is two instances,
both from the same day, and **both the wording and the corpus were authored by the same
session** — so it cannot detect a failure mode neither instance exhibits. A fresh-context
agent reading only the briefs would be the right instrument, and it is deliberately not
used: that is a subagent, `waive` does not grant the 🧩 gate, and Ido has not been asked.
`Untested:` no session has yet run under this requirement; the first real evidence arrives
with wave 1.

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
