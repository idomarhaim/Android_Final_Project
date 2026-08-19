---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 9
created: 2026-08-20
---

# `#9` — the duration box: AI estimate by default, typed value wins forever

**Independent of `C20` — can run at any time, in parallel with #11, #8 or #6.** Verified
2026-08-20: no file touching `TaskEstimate` also touches `Goal.unit`, so this and `11-fill-buttons`
share nothing and will not collide.

Needs the **Gradle daemon**; a device or the cloud emulator for the render pass.

> ⚠️ **Runs BEFORE `7-quickadd-complete`. Verified 2026-08-20.** Both land in
> `GoalDetailScreen.kt:319` `AddTaskRow`, which already holds `aiMinutes` at `:332` — *"the AI's
> answer while the title still matches, and a function of the points the moment the user types
> something new"*. That reconstruction **is** what this brief replaces with stored provenance, so
> #7 built first would be built on a value about to be redefined.
>
> Independent of `11-fill-buttons` — different documents, and **no file mentions both**
> (`grep -rl unit … | xargs grep -l looksLikeFallback` returns empty).

## Why it exists

`R8`: *"A person may optionally type the work duration into a box, but the default is that the AI
estimates it; there should be an icon inside the box for as long as the person has not entered a
number."*

The data is already there — `Task.estimatedMinutes` is nullable-with-fallback and the LLM fills it.
What is missing is the affordance and, more importantly, **the provenance**.

## The precedence rule — decided, unconditional, and not yours to soften (§1.4)

> **A hand-typed duration is sticky.** The typed number **is** the duration, points recompute from
> it, and **no re-estimation ever overwrites it** — unconditionally, with **no threshold**.

§0.6: a **fact about Ido's life** is his. Any threshold makes the app judge when he is wrong about
his own day. If you find yourself designing a "only re-estimate if the typed value is wildly off"
rule, that is the rule this decision already rejected.

**And it is structural, not a prompt instruction (§3.3 A):** the `estimate` call *"never re-prices a
completed task or a hand-typed duration — those tasks are not in `tasks[]` at all."* So the box is
enforced by **what is sent**, not by a check on what comes back. Build it that way.

## The placeholder icon becomes stored provenance — this is the real change (§1.4)

`domain/model/TaskEstimate.kt` today reconstructs provenance by guessing:

- `:100` `looksLikeFallback(taskTitle, estimate)` — *"evidence, not proof"* by its own KDoc, and
  §0.3's fifth instance of *a second number that quietly disagrees*
- `:110` `private val SERVER_FALLBACK = TaskEstimate(points = 10, minutes = DEFAULT_MINUTES)` — the
  sentinel it compares against

**Both are deleted.** A stored provenance value replaces them, and the icon reads it instead of
recomputing a guess. Check `domain/usecase/BackfillDurationsUseCase.kt`, the other caller, before
you delete.

## What the box shows when there is no estimate (§3.4)

An absent `estimatedMinutes` means **ask the user *how long?***, `DEFAULT_MINUTES` if skipped.
**The app never guesses a duration from a word count.** §0.3's house rule: when a value is unknown
it is **absent** — never a default that looks like an answer.

## The trap

The migration. Existing tasks have no stored provenance, and the honest value for them is
**absent**, not a guessed `SERVER_FALLBACK`. Backfilling them by running `looksLikeFallback` one
last time re-imports the exact guess this ticket deletes. Decide it explicitly and write the
decision down.

## Exit

- JVM unit for the precedence rule **in both directions**: a typed value survives a re-estimate,
  **and** an untyped one is still re-estimated. The direction you did not think to write is the one
  the edit broke.
- Instrumented for the icon appearing and disappearing.
- **Seen** — the icon is the deliverable; a green test is not a look.
- `CHANGELOG/<today>/9-duration-box.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading. Name whichever of `/kickoff 11-fill-buttons`, `/kickoff
8-notifications`, `/kickoff 6-silent-filing` is still unrun.
