# 50-offline-stamps round 2 — the answer was on neither menu: make the mistake impossible, not documented

> **Summary:** Ido handed back two questions about #50's leftover paper trail. Deriving instead of
> re-asking moved the answer off both menus — the deliverable is a **red test**, not a paragraph.
> This session shipped the parts that need no build, and left the test itself in a brief because
> `48-settings-surface` holds the Gradle daemon.

**Session:** `50-offline-stamps` *(round 2)* · **Date:** 2026-08-20 · **Mode:** `AUTO MODE`
**Branch:** `main` · Continues [`CHANGELOG/2026-08-19/50-offline-stamps.md`](../2026-08-19/50-offline-stamps.md)
**Singletons:** **none held.** The Gradle daemon and `Pixel_10_Pro_XL` are `48-settings-surface`'s,
claimed 2026-08-19, and **nothing here builds or touches a device.**

---

## What Ido actually said

He answered the first picker question (*how should the #50 comment reach the issue?* →
**"you paste it"**) and **handed the other two back**, in identical words:

> *"I couldn't fully understand you or what each option means — explain it simply, schematically,
> short and concise. And choose the solution that gives the highest standard and quality of the app
> (and its purpose), UX/UI and the software. And if you think there's a way to improve the solution
> you chose — improve it."*

That is a **delegation**, not a decline. Per the hand-back rule the re-ask is switched off, the
diagnosis is not, and the decision becomes **mine** — recorded here as mine, and his to overturn.

## The diagnosis, since the re-ask is off but the checks are not

Both questions failed the same two checks before they were ever asked:

1. **Ownership.** Q3 (*correct the false premise in the docs?*) was **derivable**. A committed
   document asserting something false at `HEAD`, which nearly caused a regression, does not need a
   product decision — it needs fixing. Asking spent his turn on something the rules already answer.
2. **The fork was not real.** Q2 (*file C20's issue?*) and Q3 (*fix the docs?*) were drawn as two
   independent questions with a *do it / don't* axis each. They are **one problem**: the claim
   *"`C20` removes the transaction"* is live in the record and nothing tracks the work that would
   make it true, so the next session can act on it and re-open closed
   [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3). Split that way, every option
   inherited the false premise that the two halves compete. They don't — they are complements.
3. Consequently both pickers asked him to adjudicate **mechanisms** (*issue vs TODO vs nothing*;
   *dated note vs rewrite vs defer*), which is exactly the shape the ambiguity rule says to re-cast
   as a situation. The comprehension complaint was correct and diagnostic, not a failure to read.

## The decision, and why it is on neither menu

Every option offered was a **document**. The failure mode this is defending against is
*documents get skimmed, and each restatement reads as corroboration* —
`C:\Dev\JARVIS\kb\dev\decision-map-charting.md` §12, written by this very session yesterday. **A
fourth document is the one remedy the diagnosis rules out.**

So, ranked by what actually protects the app:

| | Instrument | Stops the regression? |
|---|---|---|
| 1 | **A test that fails if the pre-check is deleted while `setDone` is a transaction** | **Yes** — the build refuses it |
| 2 | A tracked issue for `C20`'s build half | No, but it *causes* the work that lifts the block |
| 3 | A dated note in the spec | Only if read |
| 4 | Nothing | — |

**Decision: 1 + 2 + 3, in that order of importance.** The test is the deliverable; the issue causes
the fix; the note dates the record. Ido's *"improve it if you can"* is what 1 is — it was on no
menu, and it is the only one of the four that does not depend on somebody choosing to read.

**The UX claim behind it, stated plainly**, since he asked for the app's standard: if
`ConnectivityMonitor` is deleted today, tapping a task offline draws a tick, holds it for a
**measured 7.9 s**, and then takes it back. That is the app lying to the user for eight seconds.
Preventing it is a product guarantee, not housekeeping.

## What shipped here

| File | What |
|---|---|
| `docs/PRODUCT_v0.3.md` §5.3 §5 | **Annotated, not rewritten** — a dated ⚠️ block saying the paragraph describes *intent*, not `HEAD`, and that acting on it re-opens #3. The sentence becomes true the day `C20` ships, so the design record stands; what was missing was the date. |
| `sessions/50b-transaction-guard.md` *(new)* | The brief for instrument 1, **carrying the full test body** so it is not re-derived — plus a three-direction verification (green as-is · red when the guard is removed · **skipped**, not passed, when the premise flips) and the instruction to check the `<skipped/>` element in the results XML rather than trusting the console. |
| this file | Ready-to-paste bodies for both GitHub writes, below. |

**The test is not in this commit, deliberately.** It needs a compile, the Gradle daemon is
`48-settings-surface`'s, and committing an unverified test file into a repo where a sibling is
about to run `testDebugUnitTest` would break *their* build and read as *their* fault. A guard that
cannot be proven to fire is worse than no guard — so it goes in a brief, with the positive control
written down, rather than in an unrun commit.

## The two GitHub writes, ready to paste

Ido chose **"you paste it"**, so neither is posted from here — and `gh` is not installed on this
machine anyway (see [`CLAUDE.md`](../../CLAUDE.md)), so the REST read path is available and the
write path is not.

1. **Comment on #50** — full text at
   `…/scratchpad/issue-50-comment.md`, and reproduced in the reply that accompanies this commit.
2. **New issue, `C20` build half** — full text at `…/scratchpad/issue-c20-build.md`.
   Title: *"C20 build half: the projection function, its two triggers, and setDone reduced to one
   write"*. Scope: the projection function + two triggers, `firestore.rules`' first field-level
   condition, `publicProfiles.level` deleted, `setDone` reduced to a single-document write, and the
   shared `facts → expected numbers` fixture §5.2 requires because the arithmetic will exist in
   both Kotlin and TypeScript.

Both are also findable from `sessions/50b-transaction-guard.md`, which cites this file — so the
scratchpad going away costs nothing.

## 🧪 Tests

**Nothing was built or run, and that is a constraint rather than an omission.**
`48-settings-surface` claimed the **Gradle daemon** and `Pixel_10_Pro_XL` on 2026-08-19, and this
session's whole content is one annotated markdown paragraph and one new brief — no Kotlin, no
resources, no `app/src/**` path at all.

- **JVM unit** — not run. No code changed. Last green: **384 / 0** at `d577dcf`.
- **Instrumented** — not run and not owed; no device touched, so the sign-in on both AVDs is intact.
- **Cloud emulator** — **not triggered**, and checked rather than assumed: the workflow's `push:`
  filter is `['app/**', 'gradle/**', 'build.gradle.kts', 'settings.gradle.kts']`, and this commit
  touches `docs/`, `sessions/`, `CHANGELOG/` and `SESSIONS.md` only.
- **The guard test itself** — **`Untested:` by construction**, which is the point of shipping it as
  a brief. Its three-direction verification is written out in that brief and is the exit criterion
  there; a guard proven only in the green direction is decorative.
