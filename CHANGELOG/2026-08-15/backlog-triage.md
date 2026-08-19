# `backlog-triage` — the pre-map backlog reconciled against `docs/PRODUCT_v0.3.md`

**Session:** `backlog-triage`, from brief [`sessions/backlog-triage.md`](../../sessions/backlog-triage.md).
**Branch:** `feat/goalpilot-implementation`. **Mode:** `AUTO MODE` (Ido's session message; the brief
said `mode: normal` and this session's message wins — that unlocked the brief's own carve-out,
*"do not close unilaterally unless he has said `AUTO MODE`"*).

**In one line: nine open issues, nine reconciled, one closed as superseded, four new issues filed —
and `/implement #N` is now a sufficient first message for all twelve survivors.**

---

## Why this ran

The backlog issues `#2`–`#11` were written on 2026-08-06 from Ido's brief. The map then spent
**31 tickets** deciding the product model, and **nothing had reconciled the two**. A build session
opening `#34` today would have implemented a design `C20` explicitly rejected. This session was the
gate between a finished spec and someone building from a ticket the spec contradicts.

Precondition (`C22` #44, `C23` #45, `C24` #46 all closed) was **met** before it started; `#12` itself
was closed by Ido during the session.

---

## Verdicts — every open issue

| Issue | Verdict | Spec sections it now builds against | What actually changed |
|---|---|---|---|
| [`#2`](https://github.com/idomarhaim/Android_Final_Project/issues/2) Life areas: no route into goals | **reworded** | §4.7, §4.2, §1.2, §4.8 | **Destination replaced.** §4.7 requires a **life-area screen hosting its own goal list**; the old plan (scroll/filter the Goals tab) is retired, and §1.2's plural `lifeAreaIds` breaks one-goal-one-band. Entry point moved by §4.2/§4.9 |
| [`#6`](https://github.com/idomarhaim/Android_Final_Project/issues/6) Smart add: silent filing | **reworded** | §0.7, §3.3 D, §3.4 | **Promoted above a setting.** §0.7 makes silent filing a *rule*, so the requested preference and settings row are **deleted, not implemented**; the surviving exception is §3.4's new-goal branch, which *tells* |
| [`#7`](https://github.com/idomarhaim/Android_Final_Project/issues/7) Quick add: complete in-flow | **reworded** | §1.4, §1.5, §4.6, §5.3 | **All three deferred questions answered, none the way the issue guessed** — the AI awards no points at all; `progressContribution`'s `1.0` was *a silence, not a value*; and its `#3` blocker is **deleted** by §5.3 rather than fixed |
| [`#8`](https://github.com/idomarhaim/Android_Final_Project/issues/8) Notify on invented goal | **reworded** | §2.5, §3.4, §0.4, §4.9 | **Widened, in the spec's own sentence** — §2.5: *"This widens #8 to scheduled, not only immediate, notifications."* Now carries the whole local-scheduling substrate; §2.7 closes the escape route (no credential for a background sync, and cannot be one) |
| [`#9`](https://github.com/idomarhaim/Android_Final_Project/issues/9) Duration box | **reworded — UNBLOCKED** | §1.4, §3.3 A, §3.4 | `C1` answered the blocked part **unconditionally, with no threshold**. The rule is **structural** (a hand-typed task is not in `tasks[]` at all), and the placeholder icon becomes **stored provenance**, deleting `looksLikeFallback` |
| [`#10`](https://github.com/idomarhaim/Android_Final_Project/issues/10) Widget pack | **reworded — UNBLOCKED** | §4.5, §4.1, §4.4, §4.9 | `C12` designed it: **seven cards × four sizes**, disclosure shrunk to the smallest true sentence. Its `A7` feeder note is **retired** — §4.4 settled the dashboard order and §9 records that patch as *stale rather than unsolved* |
| [`#11`](https://github.com/idomarhaim/Android_Final_Project/issues/11) Fill buttons | **reworded — UNBLOCKED** | §1.3, §4.6, §7.1 | `C7` decided what a unit is, and the ladder is **arithmetic** (`target / 16` at `1× 2× 3× 4×`), not the hard-coded millilitre table the issue warned against. The live water goal arrives **unmeasured** under §7.1's migration |
| [`#34`](https://github.com/idomarhaim/Android_Final_Project/issues/34) Cloud Function owns derived state | **SUPERSEDED — CLOSED** | §5.2, §5.3 | See below |
| [`#36`](https://github.com/idomarhaim/Android_Final_Project/issues/36) Tasks consent unchecked | **reworded — narrowed** | §2.6, §2.7, §4.9 | **Promoted to a spec premise** (§2.6 adopts it verbatim) **and narrowed**: the relocation half needs `AuthorizationClient`, which §8 puts **out of scope for v0.3**. Only the legibility half ships |

`#4` and `#5` were on the brief's list and were already **closed by `social-share-bugs`** (`4b138ce`),
exactly as the brief predicted.

### The one close, and why it is safe

**`#34` is superseded by `C20` [#42](https://github.com/idomarhaim/Android_Final_Project/issues/42)**, which adjudicated its proposal
**on `#34`'s own stated risk** and chose the other shape: *project from facts* is idempotent
**structurally**, where *recompute-and-store* makes double-crediting something the function must be
*careful* about. Four consequences, all in the spec:

1. **The problem is solved** — §5.3: completing offline works for real, the donut moves immediately,
   and `#3`'s overlay/undo/pre-check are **deleted rather than kept**.
2. **The cost `#34` accepted is not paid** — §5.3 quotes its *"a second or two before the donut
   moves"* and states *"that cost is not paid at all."*
3. **Both of its objections dissolved** — *"`increment` cannot clamp"*: §1.5 **deletes four clamps**.
   *"`increment` cannot derive"*: §5.2 **deletes `publicProfiles.level` outright**.
4. **Its pattern survives and is credited.** The brief required reading all three `#34` references in
   `#12`'s body before closing. Done — `C9a`'s *"honest counter-precedent"*, `C14`'s *"#34's pattern
   for the third time"*, `C1`'s *"fourth site of one pattern"*. **All three cite it as precedent about
   a closed ticket's reasoning; none is a live instruction**, so none is a reason to keep it open in a
   narrower form. Closed as `not planned`, with the full account on the issue.

---

## New issues filed — four

| # | Title | Why it is a ticket |
|---|---|---|
| [`#48`](https://github.com/idomarhaim/Android_Final_Project/issues/48) | Settings surface (§4.9) | The one screen `C24` produced that **no issue carried**. Fixes the live `ProfileScreen.kt:114` defect (the app's only per-device setting sits on the **account** screen). Precondition of `#8`'s scheduled half |
| [`#49`](https://github.com/idomarhaim/Android_Final_Project/issues/49) | `logProgress` is non-atomic | **Live data corruption.** §10 defect 1, and the brief said it *"should not be left implicit"* |
| [`#50`](https://github.com/idomarhaim/Android_Final_Project/issues/50) | Offline as-of stamps (§5.3) | `C21`'s **four spec lines and one deletion**, touching `feature/social` and `feature/challenges`. One coherent unit, filed as one issue |
| [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) | Hebrew locale and RTL | §8 puts it **in scope**, §0.8 makes it a precondition of *every* screen ticket, and no issue carried it |

### `#49` — the defect got worse on inspection

The spec records the crash window. Re-reading `ProgressRepositoryImpl.logProgress` at `HEAD` found a
**second path to the same corruption that needs no crash at all**: the entry is committed at `:83`,
the counter moves at `:87`, and the `catch` at `:90` reports failure *after* the entry has landed. The
user is told the log failed, logs it again, and now **two entries exist for one event** while the
counter moved once. Under §4.6's *sum over entries* that second entry is the durable record, which is
why the fix has to make the entry list the truth rather than make the two writes atomic.

`Observed:` the code at `HEAD` 2026-08-15. `Untested:` neither path reproduced on a device; **the
retry path is the cheaper repro** and needs only step 3 to fail once.

### One spec citation corrected while filing

§7.2 cites the two cross-boundary DTOs as `Dtos.kt:77` / `:118`. At `HEAD` the file is
`data/firestore/dto/Dtos.kt` and they are at **`:83`** (`PublicProfileDto`, no timestamp of any kind)
and **`:124`** (`ChallengeParticipantDto`, `joinedAt` at `:129`). The DTOs and the finding are
unchanged; only the path and lines drifted. `#50` carries the verified numbers. Also verified at
`HEAD`: `snapshot.metadata.isFromCache` has **0 usages**, `core/net/ConnectivityMonitor.kt` exists
with **one** consumer, and `res/` holds **`values` and `values-night` only**.

---

## Deliberately **not** filed — recorded, per the brief's Exit

1. **The other twelve `§10` defects.** All are listed with their sites in §7.2 and land inside the
   build sessions that touch those files. Filing twelve tickets would duplicate a table that is
   already committed and already read. `#49` is the exception because it corrupts stored user data
   **today** and its symptom is **invisible**, so it must not wait for whichever session happens to
   open that file.
2. **`RecommendationRepositoryImpl.kt:175`'s `< 0.34f` *needs attention* filter** — the brief left
   this one to the session to decide. **Not filed.** It is meaningless for a goal with no measure
   (`C7`'s default), but it cannot survive the work it sits inside regardless: §1.6 forbids sending an
   unmeasured goal as `progressPercent: 0`, and §3.3 C sends `progressPercent` **absent, never `0`**.
   Whoever builds the `daily` feature must rewrite this line to read §3.4 at all. A ticket would
   describe a line that is already condemned twice over.
3. **`ThemePaletteTest`'s owed update** and the **dark tone per `GoalCategory`** — named inside
   `#10`, which is the ticket that changes the palette. Filing separately would split one change.
4. **The `GoogleSignIn` → Credential Manager / `AuthorizationClient` migration** — §8 puts it
   **out of scope for v0.3** and §9 keeps it as fog. Filing it would contradict the spec. Recorded on
   `#36` as the boundary that narrows it.
5. **`C20`'s projection function itself** (§5.2's *one projection function, two trigger
   registrations*) and the `C14` challenges rework. **These are not missing issues.** §8 states that
   `#2`–`#11`/`#34`/`#36` are *"not part of v0.3's product model — the UX/defect backlog is a separate
   track, not this map"*, so **the v0.3 build's work order is `docs/PRODUCT_v0.3.md` itself**, not the
   tracker. `#50` names its dependency on that function rather than pretending a ticket exists.

---

## Recommended build order

Dependencies first; **this is a recommendation, not an assignment** — Ido assigns.

1. **`#49`** — live corruption, and it lands `currentValue` as a sum over entries, which `#7` and
   `#11` both build on.
2. **`#51`** — precondition of every screen ticket (§0.8). Independent of `#49`; can run beside it.
3. **`#48`** — precondition of `#8`'s scheduled half; also moves `ProfileScreen.kt:114`.
4. **`#9`**, **`#36`**, **`#2`** — small, unblocked, mutually independent.
5. **`#11`** — after `#49`.
6. **`#8` then `#6`**, or both together — `#6` is unsafe alone, by §3.4 and by both issues.
7. **`#7`** — after `#49` (needs the completion-fact collection).
8. **`#10`** — largest; the whole material contract lands with it.
9. **`#50`** — after `C20`'s projection function exists (unticketed spec work, see above).

---

## 🧪 Tests

**No test layer applies.** This session edited GitHub issue bodies, wrote issue comments, closed one
issue and filed four; it touched **no code** — the brief's *Out of scope* forbids it (*"If it starts
editing `app/src/`, it has become a build session and should stop and split"*). There is no test
layer for tracker hygiene and Markdown, and none was skipped silently.

What was verified instead, mechanically rather than asserted:

- `gh issue list --state open` before and after — **9 open → 12 open, 1 closed**;
- the four `HEAD` code facts behind `#49`/`#50`/`#51` listed above, each read rather than copied from
  the spec — which is how the `Dtos.kt` line drift was caught;
- the three `#34` references in `#12`'s body, read in full before closing it, as the brief required.

---

## Session hygiene

- **Claim.** Row taken on `SESSIONS.md` before the first write. **It did not ride my own commit.** A
  sibling (`c24-settings-surface`) ran a pathspec commit on `SESSIONS.md` seconds earlier, and
  because a pathspec commit takes the **working tree**, `0bba71a` carried my claim row under *their*
  message; my `ed71060` took only the brief's `status: active`. Nothing was lost and the row is live —
  recorded here because `ed71060`'s message asserts it carries a foreign hunk, and in the event the
  traffic went the other way.
- **`kb-candidates/`** listed before the first unit of work, as required. Four files at session start
  belonging to **other** sessions (`c15b-stored-ai-text`, `c2-task-type`, `c23-goal-category`,
  `c24-settings-surface`); a fifth (`c21-offline-story`) was drained by a sibling mid-session
  (`4d67ad4`). **Not drained here** — `AUTO MODE` drains *"every pending candidate that unit
  produced"*, and none of these was produced by this unit. Reported, not adopted.
- **No subagents.** No fan-out; the 🧩 gate was never reached.

---

## Files

- **Written:** this changelog; `kb-candidates/2026-08-15-backlog-triage.md`.
- **Edited:** `SESSIONS.md` (claim, then release); `sessions/backlog-triage.md` (`status`, then close).
- **Tracker:** `#2`, `#6`, `#7`, `#8`, `#9`, `#10`, `#11`, `#36` bodies rewritten + one comment each;
  `#34` commented and closed; `#48`–`#51` created; one cross-reference comment on `#8`.
