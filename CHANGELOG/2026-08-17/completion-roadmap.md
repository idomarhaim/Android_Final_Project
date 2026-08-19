# completion-roadmap — 2026-08-17

> **Summary:** Ido deferred #51 (Hebrew/RTL) to after functionality; this session wrote the five briefs and the wave-ordered roadmap that carry the project to completion, and answered why it is not one session driving subagents.

## Why

Ido reported time constraints and asked whether development was stuck on #51, and
whether the remainder could run as one session driving subagents.

**It was stuck.** 8 of the last 10 session labels were #51 work (`51`, `51b`, `51c`,
`resource-guard-inputs`, `51d`, `51e`, plus `widget-hebrew-terminology` and its drain),
while #6, #7, #8, #9, #11, #48 and #50 had not moved since 2026-08-15.

## The decision, and the evidence that it is the right moment

`SWEPT_PACKAGES` is at **2 of 10**. `51e-sweep-components` was the package that promised
leverage and disproved it: well-factored presentational components hold no literals
because they take copy as parameters, so **the remaining eight packages are irreducible**,
one session plus a render pass each.

`Observed:` this is a clean freeze point, not an amputation:

- [`Goal.kt:111`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L111)
  keeps `GoalCategory.label` beside `localizedLabel()` deliberately, so the three unswept
  packages that read it still compile and render English.
- `AnalyticsLiteralSweepTest`'s `SWEPT_PACKAGES` is **opt-in** — an unswept package is
  *unswept, not failing* — so feature work can use plain English literals with a green build.
- `bc5ef69` fixed the guard's two holes, so whoever resumes #51 inherits a working
  instrument rather than `isProse`'s false-positive-on-the-remedy.

## What the freeze must actually do — found while planning, not assumed

`grep` for `AppLanguage` turned up **`ui/components/LanguagePicker.kt`, live in
`ProfileScreen`**. So עברית is selectable today, and `AppLanguage.DEFAULT = SYSTEM` with
`AppLocale` reading the device locale live means **a Hebrew-locale phone gets the
half-Hebrew app without touching the picker.** That second half is the one a picker-only
fix misses, and `sessions/hebrew-defer-freeze.md` step 2 addresses it by clamping the
SYSTEM branch rather than by changing `DEFAULT`.

## Why not subagents

`agent-topology-and-model-routing.md` §6 excludes them for sequential edits: subagents
share one filesystem, one git index and one Gradle daemon. The failure is already on the
record at the *session* layer, where a board, claims and release notes exist — a hook
installed mid-flight changed a live session's behaviour, 51e folded a sibling's file back
into its changelog, and one push carried three foreign commits. Subagents have none of
those protections.

On model routing: [`agent-topology-and-model-routing.md:18`](file:///C:/Dev/JARVIS/rules/agent-topology-and-model-routing.md#L18)
puts **Fable and Opus in the same Frontier tier** with no discrimination, so the committed
rule gives no basis to prefer Fable as a manager. `Untested:` no measurement of either
model on this repo's work exists; the claim is that the rule is silent, not that the
models are equivalent.

The copy-paste problem Ido wanted solved is solved by `sessions/<slug>.md` + `/kickoff
<slug>` — one typed line per session.

## The 🚥 hand-off line — Ido's mid-session addition

Asked for while this commit was being staged: **every session must end by saying whether he
may proceed to the next `/kickoff`, and to which one exactly.** Defined once in the roadmap
(§🚥) with six `GO` conditions and a per-session answer table, and pointed at from all five
briefs so no session derives it.

Two design choices worth recording:

- **It is a heading, not a sentence in the body** — same reason as the `📱` and `⏳` banners.
  A turn that ends *finished* and a turn that ends *do not proceed* otherwise look identical
  from outside, and Ido is the one person who cannot tell them apart.
- **`GO` names a slug, never a wave.** *"Proceed to wave 2"* pushes the derivation back onto
  him, which is the whole thing being removed.

The pending-brief case is handled explicitly rather than left to improvise: waves 3–4 have
no briefs by design, so the last wave-2 session ends in `STOP` with *write them against
HEAD* as its next — otherwise a session with nothing to hand to would have to invent an
answer.

`Untested:` no session has run under this requirement yet; whether the two headings are
enough shapes is unproven and will show on wave 1.

## 🧪 Tests

**No test layer applies.** This session wrote briefs and a roadmap; it changed no
`app/src/` file, ran no build and touched no device. Stated explicitly rather than
skipped.

## Files

New: five briefs under `sessions/`, `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md`,
this file. Modified: `SESSIONS.md` (own claim row).

---

## Addendum — the waived walkthrough's fallback found two real defects in the 🚥 wording

Appended after `9c19b69`. Ido answered the 🎬 offer with **`waive`**, which closes the
judgment half and leaves the mechanical half owed: run the drafted wording against every
recorded instance of the failure it addresses, then check necessity. Two instances existed —
the only two sessions on the board that ended with a hand-off decision in play — and the
run was not a formality.

**Defect 1 — publication was missing from the conditions, and a real session proves it.**
`51e-sweep-components` had a **landed commit, a released row, released singletons and a
green suite (358/0, 70/0)**, so it scored a bare `GO` on the original six conditions —
**while its push was held on Ido's decision.** The next session would have started on
unpublished work. Fixed as condition 2: an unpushed commit is not automatically a `STOP`,
but it must be stated on the line, dated, with what holds it. `changelog-index-backfill`,
which *was* pushed, correctly scored `GO` — so the check discriminates rather than merely
firing.

**Defect 2 — the silence half, which is the one that cannot be faked.** The wording said
*"every session's final reply"*, which fires on a mid-session progress turn and on a turn
that only answered a question. **Three replies in the very conversation that produced this
document** were pure question-answering with no work done. A 🚥 heading on each is noise,
and noise is what teaches Ido to skim past the heading on the turn it matters. Narrowed to
*the reply that closes the unit of work*, with an explicit *if unsure, it does not*.

**A third, smaller one, found by re-reading rather than by the corpus:** every brief said
*"On success: `GO`"*, but in normal mode the commit needs Ido's approval, so **`STOP` is the
ordinary case, not the exception** — and five briefs plus a table header were quietly
teaching the wrong default. All six sites now say the slug is named either way; only the
heading changes.

**What the fallback could not test.** The corpus is two instances from one day, and the
wording and the corpus share an author, so it cannot see a failure mode neither instance
exhibits. A fresh-context agent reading only the briefs is the right instrument and was
deliberately not used: that is a subagent, `waive` does not grant the 🧩 gate, and Ido was
not asked. `Untested:` no session has run under this requirement; first real evidence
arrives with wave 1.

---

## Addendum 2 — wave 1's evidence, the GitHub correction, and a session for its residue

Appended after `da20225`, once `hebrew-defer-freeze` released (`7baf120`, board clear, tree
clean).

**The 🚥 requirement earned its keep on its first run, and in the shape it was hardest to
predict.** Wave 1 ended `STOP`, correctly, for a reason neither addendum above anticipated:
the harness classifier denied `adb` and `gh` writes, so two of six steps could not run. A
session that reported *"all done"* on the four it managed would have handed Ido an unverified
freeze; the heading made the incompleteness the last thing he read. `Observed:` first real
evidence, and it argues the two-shape design was enough — no third heading was wanted.

**Wave 1 found a third door the brief did not name, which is worth recording as a pattern.**
The brief named two ways into a half-Hebrew app (the picker, and `SYSTEM` resolving the device
locale) and there were three: **persistence**. `AppPreferencesRepositoryImpl` read
`AppLanguage.fromId(stored)`, and `fromId` is faithful — it returns `HEBREW` for a stored
`"he"` however the picker is filtered. So the freeze would have held on every device **except
the ones that had actually used the feature**, which is precisely Ido's, and precisely where
the render pass was going to happen. The pattern: enumerating *entry points to a state* and
missing the one that is **already-stored state** rather than a live input.

**The GitHub correction Ido asked for.** The API recovered on 2026-08-17. The two live briefs
(`50-offline-stamps`, `48-settings-surface`) now say so and point at plain `gh issue view`;
`sessions/done/hebrew-defer-freeze.md` keeps the old wording deliberately, because a closed
brief is an archived record of what a session was given, not a live instruction.

**The correction that matters more than the fix:** *GitHub is healthy* and *the three #51
writes can now be posted* are **different claims**, and only the first is true. Those writes
were denied by the **harness classifier** — an outward-action gate — not by the outage. The two
blockers were indistinguishable from the outside for several hours, and recording them as one
thing would have left a session believing a permission problem had cured itself. Stated
explicitly on the board and in the roadmap for that reason.

**New brief: `sessions/51-freeze-verify.md`** carries wave 1's residue — the Hebrew-device
render pass and the three writes — as its own short session, because both need permissions Ido
must grant and neither gates wave 2. It flags the one thing that expires: if
`Pixel_10_Pro_XL` still holds a stored `"he"` from 51e's Hebrew render, that device is the only
place the pre-freeze state can be observed, so door 3 should be checked **before** anything
wipes it.

## Addendum 3 — every brief moved to AUTO MODE, and what that deliberately does not cover

Ido, 2026-08-17: *"I want all the sessions to be in AUTO MODE as long as they verify they are not
harming other sessions' work."*

**Implemented through the mechanism that already exists, not a new one.** `memory-promotion.md`
and the mode-signal rule both state that the brief's `Mode` line is the *only* way auto mode
crosses a session boundary, and that it crosses as Ido's stated intent rather than as a leftover
marker. He stated the intent, so all five live briefs now carry `mode: auto`. No new rule was
written and no protocol was altered — a value was set in the field designed to hold it. The 🎬
offer was made anyway, because the *effect* is a change to when sessions stop and ask.

**The instruction has two halves and the second is the load-bearing one.** *All sessions in auto
mode* is one line to apply. *As long as they verify they are not harming other sessions' work* is
seven checks, and they were already scattered across the parallel-sessions and push-precondition
rules — read the whole Active-claims section (count rows mechanically), claim before first write
in every repo, never blanket-stage, commit explicit paths because `git commit` commits the index,
read a shared file's own diff in its own tool call, fetch and read `@{u}..HEAD` before pushing,
and give a safety check its own tool call. Collected into the roadmap's §🔀 so a session reads
them as one list rather than deriving them from five places.

**And its honest limit is stated rather than implied**, because a checklist that looks total is
worse than one that admits its edge: the window on a sibling's file opens when *you write it*,
not when you stage it — their `git add <path>` reads the working tree, never your index. So none
of the seven prevents a sibling publishing your work under their commit message. The remedy is
**naming what rode along**. Auto mode neither worsens nor fixes this; it only means nobody is
asked first.

**What auto mode explicitly does not grant**, written into both the board note and §🔀 because
this is where a standing autonomy marker over-reaches: deletions (`#50`'s `ConnectivityMonitor`
is authorised by its **ticket**, not by the mode) · outward actions (`51-freeze-verify`'s three
`gh` writes were granted for **that task** and do not widen) · destructive git · PR open/merge,
remote branch or tag creation and deletion, releases, repo settings · a `rules/`-destined KB
candidate or one superseding a standing claim.

**One consequence worth noting:** the 🚥 hand-off line's ordinary case inverts. Under normal mode
condition 1 made `STOP`-on-commit-approval the common outcome; under auto mode a session commits
itself, so `GO` becomes ordinary and condition 2 — *say it if you held the push* — is now the one
doing the work. All five briefs were updated to say so; the wording that taught the old default
would otherwise have survived as a stale instruction, which is the same failure the fallback
check caught in addendum 1.

## 🧪 Tests (addendum 2)

**No test layer applies** — briefs, roadmap and board only; no `app/src/` change, no build, no
device. Wave 1's own numbers (JVM unit 364/0, `assembleDebug` green) are recorded in
`CHANGELOG/2026-08-17/hebrew-defer-freeze.md`, not claimed here.
