# `widget-hebrew-terminology` — the widget pack said מטרה where the entity is יעד

**Session:** `widget-hebrew-terminology` · **Issue:** [#51](https://github.com/idomarhaim/Android_Final_Project/issues/51)
· **Brief:** [`sessions/done/widget-hebrew-terminology.md`](../../sessions/done/widget-hebrew-terminology.md)
· **Mode:** `AUTO MODE`

## 🎯 What this was

`app/src/main/res/values-iw/widget_strings.xml` carried two independent defects,
both of which `values-iw/analytics_strings.xml` had already solved and written
down in its header — and the widget pack, written afterwards in the same `res/`
tree, violated both anyway.

**Eight of its resources were wrong: six on rule 1, three on rule 2, one on both.**
The brief enumerated nine defective *lines* and missed `gp_widget_effort_lead`,
which carries rule 2's defect twice in one string. That miss is the whole
argument for the guard added below.

## 🔤 Rule 1 — the `Goal` entity is יעד, never מטרה

Spec §5.1 / `E1`, ratified by Ido on 2026-08-16. Six resources said מטרה, and
`gp_widget_goals_ring_meaning` used **both** words in one sentence **with the
meanings swapped**.

יעד is masculine where מטרה is feminine, so the verbs moved with the noun —
`יכולה` → `יכול`, `נמצאת` → `עומד`. The agreement had been wrong for exactly as
long as the noun was, which is the part a find-and-replace would have left
behind.

### The blocking question, and the answer

**Once יעד is taken by `Goal`, what is the Hebrew for a measure's numeric
target?** The brief made this the reason for a session rather than a ten-minute
edit, and it was right to: the answer is that **there isn't one, and inventing
one would have been the mistake.**

`Observed:` researched against shipped, professionally localized Hebrew:

- **Google Fit's Hebrew help** — *"מדידה אוטומטית של ההתקרבות ליעדים והשגתם"*.
  יעד is the thing you approach; there is no separate word for the number.
  ([support.google.com/fit/answer/6075067?hl=iw](https://support.google.com/fit/answer/6075067?hl=iw))
- **Microsoft Dynamics 365, Hebrew** — one page uses **both** `מדדי יעד` and
  `מדדי מטרה` for one concept, sentence to sentence. That is the collision
  surfacing inside a product with a paid localization budget, not a translator
  having a bad day.
  ([learn.microsoft.com/he-il/…/create-edit-goal-metric](https://learn.microsoft.com/he-il/dynamics365/sales/create-edit-goal-metric))

So the file **does not name the target at all**. The ring is described by the
measure the goal already carries:

> `כל טבעת מראה את ההתקדמות ליעד אחד, לפי המדד שהגדרתם לו.`

That is exactly true of the code — `BuildWidgetSnapshotUseCase.hasMeasure` is
`targetValue > 0 && unit is real`, so **a measure *is* the unit plus the target**
— and it is how Hebrew actually says it. `לפי … שהגדרתם לו` carries the English's
*"that goal's **own** target"* without a second noun.

**Rejected:** `היעד המספרי`, the brief's pre-approved proposal. It reintroduces
the exact collision this session existed to remove, one adjective weaker: the
same file would then use יעד for the entity in `gp_widget_goal` and for a number
two strings later.

## ↔️ Rule 2 — no Hebrew prefix bonded to a Latin or digit run (§4.8)

`ל־%1$d`, `ב־%2$s`: the bidi algorithm resolves the run's direction from the
paragraph, so an RTL prefix bonded to the front of a Latin or digit run lands
against the run's **last** word — the `מ‑Health Connect` → `Health Connect‑מ`
class. Three resources, five lines.

| Resource | Was | Now | Why |
|---|---|---|---|
| `gp_widget_as_of` | `נכון ל־%1$s` | `נכון לשעה %1$s` | see below — the brief's own proposal was factually wrong |
| `gp_widget_goals_without_measure` | `— ל־%1$d מהמטרות שלכם אין.` | `— עבור %1$d מהיעדים שלכם אין מדד.` | `עבור` is space-separated; same construction `analytics_estimates_all` already uses |
| `gp_widget_effort_lead` | `ו־%1$s נמצאת ב־%2$s.` | `והיעד %1$s עומד על %2$s.` | **not in the brief.** `ו` now attaches to `היעד`; `עומד על` is a space-separated preposition |

### 🔴 The brief's proposed fix for `gp_widget_as_of` was wrong, and not about bidi

It proposed `נכון לתאריך %1$s` — *"as of the **date** %1$s"*. The argument is
`AndroidWidgetStrings.asOfShort`, which is `DateFormat.getTimeFormat(context)`:
**a clock reading (`14:32`), never a date.** The proposal fixed the bidi defect
and introduced a factual one in the same string. It is `נכון לשעה %1$s`.

Reading the string against the code that fills it is what caught this; reading
it as Hebrew would not have, because it parses perfectly.

### 🔴 `gp_widget_effort_lead` — the defect the brief's enumeration missed

Not in the list of nine, and it carries §4.8's defect **twice**:

- `%1$s` is `lead.title` — a **user-authored goal title**, which §8 keeps exactly
  as the user typed it, so it may well be Latin. `ו־Learn Spanish` is precisely
  `מ‑Health Connect`.
- `%2$s` is `strings.percent(lead.percent)` — **digits**.

It is squarely inside the brief's own stated defect class, so fixing it completes
the task rather than widening it. `עומד` also replaces `נמצאת`, which agreed with
nothing determinate: `היעד` is the only word in the sentence whose gender is
knowable, since the user's title's is not.

### The contradiction in this file's own header, now resolved in it

The header already said §4.8 *"is fixed in code … not here — a translation cannot
fix it"*. That is true of the defect it was describing — a **value** the tile
computes (`3.2 / 4 ק״מ`), which `Bidi.kt` isolates. It is false of this one: the
prefix lives in the **template** and `Inferred:` from what an isolate is, must sit
outside it, so isolating the argument can never reach it. The two paragraphs read
as contradictory until separated, so the header now separates them explicitly.

## 🛡️ The guard — `HebrewTerminologyTest` *(new)*

`app/src/test/java/com/idomarhaim/goalpilot/resources/HebrewTerminologyTest.kt`,
three tests over **every** `values-iw/*.xml`, not just the widget pack:

1. no `מטרה`/`מטרות` in any string value (XML comments stripped, because three
   headers legitimately contain the word in order to forbid it);
2. no Hebrew letter bonded to a format specifier — `Regex("[א-ת]־?%\\d\\$")`;
3. the widget pack still contains `יעד` — the complement, since zero occurrences
   of מטרה is also what a deleted file looks like.

**Why a test.** Neither defect is reachable from any other layer: a wrong noun
compiles, packs and renders; `WidgetHebrewResourceTest` compares resource *names*
and is blind to values by construction; and rule 2's failure happens in the bidi
algorithm at draw time, which no JVM test can exercise. What *is* checkable is the
string that causes it. And a header did not work — `analytics_strings.xml` stated
both rules and the next file written in the same tree broke both.

`Untested:` the guard has no false positives **in this codebase today**; whether
it stays quiet as `values-iw/` grows is not something one repo can prove.

## 🧪 Tests

Run with `$env:JAVA_HOME` = JDK 21, per [AGENTS.md](../../AGENTS.md).

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **354 / 0** — 351 before, +3 new |
| **Build** (`:app:assembleDebug`) | green |
| Instrumented (`connectedDebugAndroidTest`) | **not run** — resource-value change only, and running it uninstalls the app and takes Ido's sign-in with it (`51c`'s trap) |
| `firestore-tests/` | **not run** — no rules, no backend surface touched |

**No `--rerun-tasks`.** `resource-guard-inputs` (`c477557`) declared `res/` and
`src/` as inputs to every `Test` task earlier the same day, and this unit is the
first that gets an honest green off it.

### The guard was proved to fire, not assumed to

The four-state protocol from that session, because a resource-only **value** edit
is exactly the shape the guards used to be blind to (`R.jar` is keyed on resource
*names*, not values):

| State | Run | Result |
|---|---|---|
| 1 · defect present, no guard | historical — 351 green with all eight wrong resources in the tree | **green** (the fault) |
| 2 · defect present, guard added | `--tests '*HebrewTerminologyTest*'` | **2 of 3 FAILED** — one per rule |
| 3 · reverted | full suite | **green** |
| 4 · defect re-applied from that green state | **full suite, unfiltered** | **1 FAILED, 354 completed** |

State 4 is the one that matters: an unfiltered run from a warm, cached-green state
caught a change to a resource **value** with no key added. Before `c477557` that
run would have reported green.

## 📌 Out of scope, unchanged

Per the brief: every other package (`ui/components/` is the next sweep unit), the
`LocalContext`-into-Dialog defect, `formatMinutes` hardcoding `h`/`m`,
`"Unassigned"`, and the English strings. All filed on `#51`.

## ⚠️ Still owed

**A Hebrew render of the widgets on a device.** §0.8 — *every screen is designed,
and is not finished until seen in Hebrew* — is satisfied in intent and not in
fact. The file header has said so since it was written and still says so. This
session did not claim an emulator: the render and an instrumented run are mutually
exclusive on one device (`51c`), and nothing here needed the instrumented run.

## Files

- `app/src/main/res/values-iw/widget_strings.xml` — eight resources, plus a header
  recording both rules and the research behind rule 1's answer
- `app/src/test/java/com/idomarhaim/goalpilot/resources/HebrewTerminologyTest.kt` *(new)*
