---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 6
created: 2026-08-20
---

# `#6` — silent filing, and the one branch that is allowed to speak

**Independent of `C20`.** Touches `functions/` — so **check the board**: `c20-build-half` owns
`functions/src/index.ts` while it runs. Needs the **Gradle daemon** and the **Firebase emulator**.

> ⚠️ **Runs AFTER `7-quickadd-complete`. Verified 2026-08-20.** Both edit
> `feature/dashboard/DashboardScreen.kt`: this one removes `SmartAddDialog` (`:275`, `:659`) from
> the existing-goal branch, #7 adds a done-affordance to `SmartAddCard` (`:616`). Adjacent
> functions in one file. Also conflicts with `c20-build-half` on `functions/src/index.ts`.

## ⚠️ The ask was promoted, and that deletes the thing it asked for

`R3`: *"It asks for approval on where to file every task you enter. The configuration default
should be that it does not ask and just does it."*

The triage confirmed the ask **and promoted it out of settings**. §0.7:

> *The app may act silently on instrumental structure, but must ask before asserting an intrinsic
> edge.* So: the agent may file, schedule, link and break down freely. It may **never** invent a
> goal.

So filing under an **existing** goal or life area is silent **always** — no dialog, no toggle, no
default to configure. §4.9's settings surface has **no such control, and adding one would
contradict §0.7.** If you build a preference here you have built the wrong thing.

## Today

`feature/dashboard/DashboardScreen.kt:616` `SmartAddCard` → `:275` always opens `SmartAddDialog`
(`:659`) for confirmation. There is no way to turn it off. That dialog does not become optional —
**for the existing-goal branch it stops existing.**

## The one branch that speaks — and it *tells*, it does not ask

- **§3.3 D** — `classify` returns `suggestedGoalId` (a member of the list sent with it, else
  **absent**), `suggestedLifeAreaId`, `suggestedNewGoalTitle`, `suggestedCategory`, `confidence`,
  `rationale`, plus the whole `estimate` group validated independently.
- **§3.4** — an **absent `suggestedGoalId` takes the new-goal branch, and only this one speaks.**
  Every other fallback is silent: an absent `suggestedLifeAreaId` means the Google Tasks list wins
  on import, and *unfiled* on quick-add. §0.4: *speak about a failure the user can act on; stay
  silent about one they cannot* — an absent goal id does not degrade the outcome, it **changes** it.
- **§3.5** — the sorter **must never invent a goal**; low confidence leaves `goalId` null.
  `suggestedNewGoalTitle` is **content** the moment it lands in Ido's list, so it is **never
  translated and never re-rendered** (`C15b`).
- **§1.1** — an `AI_SUGGESTED` goal can sit **pending** rather than silently appear, and offers a
  **lossless demotion**: drop `declaredBy`, and the object and all its edges survive. **That is the
  witness in the data that §0.7 requires** — build it, it is not decoration.

## Validation is not the client's job — §3.4

It lives in the **Cloud Function, singly.** `classify` is the highest-volume call; a client-side
validator is a second implementation that will disagree. `functions/src/index.ts:96`
`classifyTask` is the site.

## The relationship to #8

The new-goal branch is what #8 notifies about. **They are separately briefed on purpose:** #8 owns
the substrate (channel, permission, scheduling), this owns the sorter. If #8 has not landed, build
the **in-app** half here and leave a clean seam — do not grow a notification channel inside this
unit.

## The trap

"Silent" is not "invisible". §0.7 permits acting without asking; it does not permit acting without
**a witness**. Every silent filing must be visible after the fact and undoable — otherwise the first
time the sorter is wrong, Ido has no way to find what it did.

## Exit

- JVM unit for the branch table: present `suggestedGoalId` → silent; absent → speaks; low
  confidence → `goalId` null and **no invented goal**.
- Functions tests for the validation, against the emulator.
- Instrumented for the pending-goal surface and the lossless demotion.
- **Seen** — a task filed with no dialog, and a pending goal visible afterwards.
- `CHANGELOG/<today>/6-silent-filing.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading.
