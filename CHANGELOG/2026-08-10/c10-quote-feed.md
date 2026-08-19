# `c10-quote-feed` — R21 turns out to be two sentences, and the seam is attribution

**Session:** `c10-quote-feed` · **Date:** 2026-08-10 · **Mode:** normal
**Invocation:** `/wayfinder 12 29`, resolving [#29](https://github.com/idomarhaim/Android_Final_Project/issues/29) (`C10`) on the [v0.3 product-model map](https://github.com/idomarhaim/Android_Final_Project/issues/12) · **Planning only; no code.**

## What changed

- **Resolved and closed [#29](https://github.com/idomarhaim/Android_Final_Project/issues/29)** with a full resolution comment.
- **Map body [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)** — one gist line appended to *Decisions so far*, and the `A7` fog patch **narrowed** rather than cleared.
- This changelog, its `CHANGELOG_README.md` row, `kb-candidates/2026-08-10-c10-quote-feed.md`, and the `SESSIONS.md` claim + release.
- **No Kotlin, Gradle, Firestore-rules or Cloud-Functions file was created or modified.**

## The decision

`R21` asks for a daily sentence that is *"practical and inspiring"* **and** *"drawn from a bestselling book or a famous figure"*. Those are **two sentences with two sources**, and one mechanism producing both is what makes the feature either a platitude or a lie.

**The split is by attribution.** Only a **curated corpus shipped in the APK** may name a real human; the model writes the practical line and **names nobody**.

| # | Question | Answer |
|---|---|---|
| 1 | Where do the words come from? | Split by attribution |
| 2 | How is a quote tagged? | `GoalCategory` **+** a theme |
| 3 | Who picks the quote? | Model emits a **theme**; the **app** picks the quote |
| 4 | Where do quotes come from? | Public domain (incl. natively Hebrew) + a hand-added modern tier |
| 5 | What makes it *daily*? | Cached **on the phone**, keyed by date |
| 6 | What is on the home screen? | The quote **+ the 2–3 goals that most need attention** |
| 7 | May the line name a task? | **Yes** — the next unfinished one |

Derived rather than asked, and confirmed at the final gate: a quote is **never machine-translated** (published translation or that language only, per `C15`); today's **four coach cards are absorbed**; short quotations carrying author and source are the licence posture, given the map's audience of one.

**Themes, fixed here:** `STARTING · CONSISTENCY · SETBACK · PATIENCE · FINISHING · DISCIPLINE · PERSPECTIVE · REST` — every one computable **without a measure** except `FINISHING`.

## The correction pass — and what it says about the grilling that preceded it

After the resolution was written, committed and indexed, Ido asked one question: *"is there anything you can improve in the solutions you chose in this session?"* Nine findings came back, and **three of them were wrong answers rather than missing ones** — none caught by seven grilling questions, a schematic summary, and an explicit final confirmation gate.

That is the finding worth keeping. **A confirmation gate tests whether the user understood the proposal; it does not test whether the proposer was right.** The grilling loop is adversarial about *which option to take* and entirely trusting about *the facts inside each option* — and all three errors lived inside the options, not between them.

| | Correction | What was wrong |
|---|---|---|
| **A1** | Hebrew translations are **not** public domain | A translation is a separately copyrighted derivative work. "The original is free" says nothing about the 2010 Hebrew edition. Tier 1 is rebuilt **Hebrew-first from natively-Hebrew sources** (Pirkei Avot, Tanakh) plus lapsed editions verified *per edition, not per author*. |
| **A2** | The task named is the **smallest**, not the earliest | The resolution argued for *"the one thing you could do now"* and then specced *"the earliest unfinished task"*. On a stale goal those are near opposites — the oldest open task is the one that has been avoided. Corrected to smallest `estimatedMinutes`, with ordering winning once `C8` supplies one. |
| **A3** | `C2` already claimed this feature's tone | [#20](https://github.com/idomarhaim/Android_Final_Project/issues/20) lists *"it selects the tone of the daily line (`C10`)"* among its candidate purposes, and this session never opened it. The axes turn out **orthogonal** — theme from the goal's *state*, task type from the task's *content* — but the reconciliation had to be written down or `C2` would re-decide it. |

Six spec lines were added beside them. The largest is **B2: the home feed as first specced is a daily list of the goals you are failing at** — *"the 2–3 goals that most need attention"*, run every morning, on a feature whose purpose is motivation, where a goal you are doing well on can never appear. Fixed by reserving one slot for a goal in a good state, at identical cost. The others: **`Goal.deadlineEpochMillis` existed and was used for nothing** ([`Goal.kt:26`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L26)) and becomes a ninth theme plus an urgency weight; a recently-shown ring per edge, because `hash(today + edgeId)` can repeat a quote inside a week; a logical day starting at **04:00**; a life area resolving to the **union** of its goals' categories at theme `PERSPECTIVE`; and the goal screen showing two blocks where `R21` asked for one sentence, left to `C12`.

Posted as a [correction comment](https://github.com/idomarhaim/Android_Final_Project/issues/29#issuecomment-5242005728) on the closed ticket and folded into the map gist. **The map was re-fetched and hashed a fourth time before that second write** — `1ba3381e…c024`, differing from the copy this session had written only by a trailing newline GitHub adds, so no sibling had touched it.

## The argument that actually decided it was the fallback, not the safety

Four selection mechanisms were on the table, and the two that hand the model a quote id were rejected. The obvious reason is `C11a`'s measured **silent id corruption**, but that is only the second reason and it is the weaker one — a membership check catches it.

The reason that decided it is what each option's **degraded mode** looks like, against the map's standing rule that every AI feature ships with a non-AI fallback:

- **Model emits a theme, app picks the quote** → with no network the app derives the theme by rule and *the corpus, the filter and the pick are identical*. The fallback is **the same feature with one input substituted**, and nothing on screen says anything failed.
- **App shortlists five, model picks one** → with no network the app picks by hash instead. The fallback is a **different mechanism**, so the degraded feature is a substitute.

And the nuance the rejected option buys is small: by the time selection runs, **every candidate is already correct** in subject and moment. It spends a call, five quotes of prompt text per goal per day, and a corruption risk, to choose between seven quotes that are all apt.

The chosen shape is `C7`'s own rule reused unchanged — *the AI judges, the app computes* — so the spec now has **one** rule about the model's role rather than two.

## What `C7` and `C4` did to this ticket while it was open

Both closed mid-session, and neither was cosmetic.

**`C7` made a measure optional with absence the default** — which looks like a problem for a feed keyed off progress. The real situation is worse than silence and it is **a live defect**: `targetValue` defaults to `100.0`, `currentValue` to `0.0` ([`Goal.kt:22-23`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L22-L23)), `progressPercent` derives to `0` ([`Goal.kt:38`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L38)), and the payload ships it unconditionally ([`RecommendationRepositoryImpl.kt:41-50`](../../app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt#L41-L50)). **A goal that deliberately measures nothing reads to the model as "you have done nothing."**

The **theme** axis removes the dependency rather than working around it: days idle, open work, age — signals every goal has. So an unmeasured goal is **well-aimed, not degraded**, and that is the whole reason `category + theme` beat `category` alone.

**`C4` made the sentence attach to an edge.** *"A sentence per goal"* means **per intrinsic edge**, and three things fall out that would otherwise have been bugs: `E19`'s Goal 2 gets **one** sentence and not two; an object that is only a milestone gets **none of its own** and shows its goal's; and the feed stays **bounded** under `C18`'s unbounded depth, at *(intrinsic edges + life areas)* rather than per node.

## `R22` answered as a socket rather than a dependency

`Task` has `isDone` and `createdAtEpochMillis` and **no ordering field at all** ([`Task.kt:7-27`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt#L7-L27)), so `R22`'s *"current numbered task"* does not exist while *"the earliest unfinished task"* does. `C8` ([#24](https://github.com/idomarhaim/Android_Final_Project/issues/24)) is still blocked behind `C16` and `C1`, so deferring would have left `R22` unanswered behind two open tickets.

So the slot opens now against what exists and `C8` fills it later — and **the app chooses the task, the model only phrases it**, which makes naming a nonexistent task impossible rather than unlikely. The UX consequence is why it was worth a yes: once the card knows which task it is about, **the card can be that task**, one tap to complete.

## Ido asked twice for the same thing, and it changed how the session ran

Two question pickers came back with *"I could not fully understand you and what the implications of each option are — explain simply and schematically, and choose the highest-quality answer."* Both times the picker was dense: long option bodies, nested reasoning inside the choices, and jargon carried over from the tickets.

What worked instead was **a plain-language explanation before the picker, and a 2×2 or a small table in place of prose**. Q3 became *"picking a quote is two jobs — what kind, and which one — and each can be done by the model or the app"*, which is the same four options in a shape that fits in one glance. From Q5 on, the pickers were short and were answered directly.

Recorded because it is a method finding, not a one-off: **when a picker has to explain itself, the axis has not been reduced far enough.**

## Facts established, so no later session re-derives them

- **The coach that ships today is not a quote feed and never was.** `getRecommendations` asks for *"at most 4 short, specific, motivating items"* and mentions no book, author or quotation ([`functions/src/index.ts:65-73`](../../functions/src/index.ts#L65-L73)). *"Start Small"* and *"Track Your Wins"* are the model's own unattributed words. `R21` is a **new surface**.
- **"Daily" does not exist anywhere in the code.** The feed is cached by a single in-memory boolean ([`DashboardViewModel.kt:124-128`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt#L124-L128)), so it regenerates on every process start — open the app three times, see three different things.
- **Goal detail has no AI text at all**, and life areas have none. Both halves of `R21`'s placement are greenfield.
- **The quote half needs no storage.** `hash(today + edgeId)` is stable for a whole local day by construction, so only the model-written lines are cached.

## Two defects found — filed as spec lines, not fixed

This ticket ships no code, so both are recorded for the build session:

1. **`progressPercent: 0` for an unmeasured goal** — see above. It must be sent as *absent*.
2. **`Recommendation` carries no `author` and no `source`** — `id`, `title`, `message`, `type`, `relatedGoalId` only ([`RecommendationRepositoryImpl.kt:117-123`](../../app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt#L117-L123)). An attributed quote has nowhere to live in the domain model today.

## What was deliberately not done

- **Filed no new tickets.** Corpus authoring is real manual work but it is *implementation*, not a decision — the map's standing rule is plan, don't do, and the two-tier corpus exists precisely so the feature ships complete on the public-domain tier alone. State→theme thresholds are tuning. Tappable-to-complete is `C12`'s.
- **No comments on [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30), [#35](https://github.com/idomarhaim/Android_Final_Project/issues/35) or [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31).** What each inherits is written in the resolution; the map is the index.
- **Did not touch the stale `c7-what-is-a-unit` / `c4-goal-task-ontology` Active rows.** Both sessions' tickets are closed, so the rows look finished — but clearing another session's row is not this session's call, and it was reported to Ido instead.

## 🧪 Tests

**No suite run, and none applicable.** No Kotlin, Gradle, `firestore.rules` or Cloud-Functions file was created or modified — the deliverable is a GitHub resolution, a map edit and Markdown. The project's layers (JVM unit, instrumented, `firestore-tests/`) all test code that this session did not touch.

Verification was **structural** instead:

- **Blocking re-checked live before claiming** — `#29`'s `blockedBy` was `{#15 CLOSED, #16 CLOSED}`, so it was genuinely on the frontier and not merely unassigned.
- **The map body was hashed three times** — once on first read (`831c71f2…f0330`), once after the map moved and Ido warned of concurrent sessions, and once **immediately before writing**. All three matched, so the append could not have clobbered a sibling. The edit was refused-by-default if they had not.
- **The frontier was re-derived out of GitHub after closing** — 25 children, 7 closed. Open frontier is `#32` (`C13`, **unclaimed**), `#25` (`C9a`) and `#37` (`C16`), the latter two already assigned. `#30` and `#35`, which `#29` was blocking, are **still blocked** — by `{#24, #20, #19}` and `{#24}` respectively — so closing this ticket freed nothing downstream, which is worth knowing before anyone assumes it did.
- **One error caught and fixed in place:** the resolution comment was first posted dated **2026-08-09**; today is 2026-08-10. The comment was edited rather than left standing, and the changelog, KB-candidate and board paths corrected before they were written.

## Singletons

**None taken.** No `#gradle-daemon`, neither AVD, no `adb`, no call to GROQ, and **live `goalpilot-56e30` was never contacted** — every fact about the app came from reading source, not from running it.

The one shared artifact was the map body `#12`, which carries no lease — handled by the hash discipline above.

**`SESSIONS.md` — claimed before the first write, and the known hazard happened again.** The row went in before any GitHub write, but it was **committed by a sibling session rather than by this one** (`bef501b`, `c16-milestone-model`, which also claimed `#37`). That is the same commons-lease hazard `product-model-map` and `kb-ingest-map-method` both recorded; naming it rather than letting the row's provenance look like this session's own commit. Two other sessions are live on this board — `c9a-schedule-a-task` (`#25`) and `c16-milestone-model` (`#37`) — and **neither's paths overlap this one's**: three different tickets, three different changelog and candidate files, and the only shared artifact is the map body, which all three protect by hashing. The row is released in this commit, and **only this session's row was staged** — no blanket staging, per §5 rule 3.

## KB candidates

**6 written, 0 ingested** — normal mode, so the list is a proposal: `kb-candidates/2026-08-10-c10-quote-feed.md`. Candidate 4 is **always-ask by inheritance** — it corroborates a parked `rules/`-shaped entry rather than standing on its own, and says so. Candidates 5 and 6 came out of the correction pass: the translation-copyright fact, and the observation that a confirmation gate tests comprehension rather than correctness.

`kb-candidates/` was listed before the first unit of work, and **what it held changed underneath this session**. At session start it was three files — `2026-08-08-c9d-calendar-scopes`, `2026-08-08-fix-task-completion-feedback`, `2026-08-09-c9f-consent-screen-state` — all owed an ingest and all unowned, which is what was reported to Ido. By the time this session wrote its own file, `kb-ingest-backlog-drain` had drained the backlog and the folder held **two partially-drained files, each down to a single parked always-ask entry** awaiting Ido. Recorded rather than quietly re-stated, because the session-start report is now wrong and someone reading it later would think a backlog still exists.

One of those parked entries is worth naming here: `entity-model-intake`'s *"'It doesn't understand me' usually means every option shares a framing the user doesn't hold"* — which is **exactly** what happened twice in this session's pickers, from a different session that hit it a day earlier. Two independent sessions finding the same failure is the strongest case for that entry, and this session's candidate 3 links to it rather than duplicating it.
