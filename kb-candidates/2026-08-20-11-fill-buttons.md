# KB candidates — `11-fill-buttons`, 2026-08-20

Session: `11-fill-buttons` · issue [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11) ·
mode `AUTO MODE` (brief front matter, Ido's standing instruction of 2026-08-17).
Account: [`CHANGELOG/2026-08-20/11-fill-buttons.md`](../CHANGELOG/2026-08-20/11-fill-buttons.md).

---

## 1 · A write that resolves on *server ack* must not gate a control whose premise is repetition

**Claim.** When a UI control exists to be operated **repeatedly** — a fill button, a `+`/`−` stepper,
a quantity nudge — the reflex of disabling it while the write is in flight is not merely
conservative, it **deletes the feature offline**, and it does so silently: every test passes, because
a test harness never has a slow radio.

The mechanism is a mismatch between two clocks that a caller cannot see from the call site:

| What | When it happens |
|---|---|
| Firestore applies the write to the **offline cache** | synchronously, at the call |
| The snapshot listener re-emits, so the UI updates | next frame, radio on or off |
| The `Task` returned by `set()` / `update()` **resolves** | on **server ack** — never offline |

So `isSubmitting = true; repo.write().await(); isSubmitting = false`, feeding `enabled =
!isSubmitting`, disables the control from the first tap until the network answers — while the number
it drives has *already* moved. The user sees their tap land and the button die.

**Observed** 2026-08-20 in GoalPilot, during a pre-commit self-review rather than by a failing test:
`ProgressRepositoryImpl.logProgress` ends in `ref.set(dto).await()`, and `FillButtonRow` had been
written with `enabled = !action.isSubmitting`. Both suites (JVM 459/0, instrumented 91/0) were green
with the defect in place. The fix was to **delete the parameter**, not to default it — a flag whose
only correct value is `true` is a hazard left lying about — and to invert the test that asserted the
disable into one asserting it can never happen.

**Why the gate is right in the neighbouring case, which is what makes this subtle.** The *dialog* on
the same screen keeps `isSubmitting`, and correctly: it uploads an image first, so a double-submit
costs a second upload and a second entry. The discriminator is not *"is this a write"* but **is the
control's own premise repetition**. A submit button pressed twice is a mistake; a fill button pressed
twice is the feature.

**Why (rejected alternatives).**
- *Debounce instead of disable* — rejected: it also throttles the deliberate fourth tap, and it picks
  a timeout nobody can justify.
- *Gate on a cache-write callback rather than server ack* — rejected: Firestore exposes no such
  callback, and the snapshot listener already **is** that signal, which is what makes the gate
  redundant rather than merely mistimed.
- *Keep `enabled` with a `true` default* — rejected above.

**Destination.** `kb/dev/firestore-write-semantics.md`. It is the page that already holds the write
half of exactly this clock mismatch, and this is the **UI-affordance** consequence of it — a sibling
of `50-finish`'s finding that `runTransaction` never reaches the cache, and of `C20`'s move that
fixed it. **Merged into that page, not appended blind**: §8 there is a read-side entry
(`MetadataChanges.EXCLUDE`), so this wants its own section with a pointer from the `await()`
discussion, and the existing text must be read before writing.

**Anchors.** `kb/dev/firestore-write-semantics.md` (destination) ·
`kb/dev/look-at-your-own-output.md` (this was found by a review question, not a test — same family
as its §4c *green over a component that never ran*).

**Supersedes.** Nothing. It **narrows** nothing either; it is additive.

**Status.** `pending` — not drained by this session. The destination page must be read and the entry
placed against its existing structure, which is more than a commit-trigger drain should do blind.
Neither always-ask gate applies: the destination is `kb/dev/`, not `rules/`, and it contradicts no
standing claim.

---

## 2 · Sub-unit prefixes are unreachable when the unit word is user content — and that is a design consequence, not a gap

**Claim.** A product decision that a unit label is **user content** (free text, any language,
untranslated) makes SI-prefix rendering — `0.25 L` → `250 ml` — **structurally unreachable**, not
merely unimplemented. The two mechanisms that would bridge it are the two the same decision rules
out: a lookup table keyed on the user's text (a string match on user content), or a dimensional
model (which buys conversions the app never performs).

The trap is that a spec can *state the goal in prefixed form* while its own model forbids producing
it. GoalPilot's §1.3 writes the fill-button example as `[250 ml] [500 ml] [750 ml] [1 L]` **and**
declares the unit word user content, three paragraphs apart. The implementable reading is the
amounts, in the goal's own word: `0.25 L · 0.5 L · 0.75 L · 1 L`. Numerically identical,
presentationally one step short — and the right move is to **ship it and flag the divergence**,
rather than to quietly satisfy the picture with a millilitre table that is correct in one language
for one unit.

**Why it generalises.** The shape recurs wherever a spec illustrates output with a *rendering* while
constraining the *model* elsewhere: currency symbols and minor units, date formats, pluralisation.
The rendering in an example is evidence of intent, never of feasibility, and the model clause wins.

**Why (rejected).** *Ask the model to classify the word* — rejected on the same ground the whole
ticket rests on: §3.1 measured free numbers swinging 2× run-to-run and 1.8× between languages, and a
classification that is right 90% of the time silently mislabels one goal in ten with no way for the
user to see it happened.

**Destination.** `kb/dev/untranslatable-idioms.md`. That page already carries *a language switch
cannot reach a constructor argument*; this is the same boundary read from the other side — what the
app **may not compute** once a string is declared to belong to the user.

**Anchors.** `kb/dev/untranslatable-idioms.md` §1 · `kb/dev/enum-and-label.md`.

**Supersedes.** Nothing.

**Status.** `pending`. Lower value than entry 1 and may reasonably be judged too narrow to promote —
if so, record that judgement in this file rather than deleting the entry.
