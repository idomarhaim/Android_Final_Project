# KB candidates — `51e-sweep-components`, 2026-08-17

Session: `#51`'s literal sweep for `ui/components/`.
Account: [`CHANGELOG/2026-08-17/51e-sweep-components.md`](../CHANGELOG/2026-08-17/51e-sweep-components.md).

**None drained.** Not a mode question — `AUTO MODE` was in effect and would have drained them at the
commit trigger. `/kb-ingest` appends to `kb/log/YYYY-MM-DD.md`, and `C:\Dev\JARVIS`'s board shows
`kb-drain-jarvis-own` **live and owning `kb/log/2026-08-17.md`** (read 2026-08-17). Draining would
have written a claimed path. Every entry below is self-contained, so whoever takes them needs no part
of this session's transcript.

---

## 1 · A guard's false positive can fire *precisely on the remedy*

**Claim.** When a lint-style guard matches on source text, check what it does to the **fixed** form,
not only the broken one. `AnalyticsLiteralSweepTest.isProse` counted alphabetic runs over the raw
Kotlin literal, so the identifiers inside `${…}` counted as words: after the sweep replaced a
concatenated English sentence with a pure-format template, the guard flagged
`"${goal.currentValue.trimNumber()}/${goal.targetValue.trimNumber()}"` — a string containing no word
at all — as user-facing prose. The offender list named two literals that had **just been fixed**.

**Why it matters, and why it is not just a bug.** A false positive on the *defect* is noise. A false
positive on the *remedy* is worse in kind: it fires only for people doing the work correctly, it
appears at the end of the job when the fix is already written, and the cheapest way out is to
contort the code away from the correct shape or to drop the package from the guard. That is how a
guard gets routed around rather than obeyed.

**Rejected fix:** restructuring `GoalCard` to avoid the template (dodges it, and the next sweeper hits
the same wall). **Shipped fix:** strip interpolations, brace-matched, before counting — copy
*between* them survives, so `"Completed ${n} of ${m} tasks"` still fails.

**The second half, which is the part that generalizes.** Loosening a predicate is exactly how a
guard silently stops firing while every other input keeps reporting success, so the loosened rule was
asserted **in both directions** on the hardest inputs it exists for: five copy strings must still be
caught, eight code/punctuation strings must not be flagged. The silent half is the one that cannot be
faked.

**Destination.** `kb/dev/` — extends [`look-at-your-own-output.md`](file:///C:/Dev/JARVIS/kb/dev/look-at-your-own-output.md)
(this is the *instrument-check* clause with a worked instance) and is adjacent to
[`guards-on-absent-input.md`](file:///C:/Dev/JARVIS/kb/dev/guards-on-absent-input.md). Judgement call
between a new page (`a-guard-that-fires-on-the-remedy.md`) and a section on `look-at-your-own-output.md`;
recommend the section, since the existing page already owns "check the instrument on the hardest input
it exists for" and this is that clause's first recorded instance in a guard rather than a renderer.

**Anchors.** `Android_Final_Project`, `app/src/test/java/com/idomarhaim/goalpilot/resources/AnalyticsLiteralSweepTest.kt`
(`isProse`, `withoutInterpolations`, `the prose rule fires on copy and stays silent on code`).

**Supersedes.** Nothing.

**Status.** Not drained — target bundle contended (see header).

---

## 2 · A guard can grow on one side and not the other, and nothing announces it

**Claim.** `AnalyticsLiteralSweepTest` had two tests: one scanning `SWEPT_PACKAGES` for literals, one
asserting the swept package reads its copy from `res/`. The second **hardcoded `feature/analytics`**.
Adding `ui/components` to `SWEPT_PACKAGES` therefore extended the offender scan automatically and left
the complement covering one package — and it reported **green for a package it never read**.

**Why it matters.** The list looks like the single control surface; extending it *feels* like
extending the guard. The failure is invisible because the half that did grow goes red and gets your
whole attention, so the half that did not is never questioned. This is distinct from candidate 1: no
false positive announced it, and nothing was broken — the guard simply covered less than its own
extension mechanism implied.

**Generalizes to:** any guard with a declared scope list plus a second assertion that names one member
of that list literally. The tell is a constant appearing in a test that also reads the list.

**Shipped fix.** The complement loops `SWEPT_PACKAGES` against a `RESOURCE_FLOOR` map, and a third
test fails if a swept package has no floor declared — so the next package added cannot silently skip
the check the way this one would have.

**Destination.** `kb/dev/` — same page as candidate 1 if that lands as a section, otherwise adjacent
to [`jvm-vs-android-locale-codes.md`](file:///C:/Dev/JARVIS/kb/dev/jvm-vs-android-locale-codes.md) §5
(*a sweep is an event, not a state, so each swept unit needs its own guard*), which this refines: the
guard must also **grow on every side** when a unit is added.

**Anchors.** Same file as candidate 1 (`every swept package resolves its words through resources`,
`every swept package declares a resource floor`, `RESOURCE_FLOOR`).

**Supersedes.** Nothing. Refines `jvm-vs-android-locale-codes.md` §5 without contradicting it.

---

## 3 · Whether to strip speech from a domain type is decided by its **consumers**, not by the idiom

**Claim.** `untranslatable-idioms.md` §1's fourth idiom (*speech stored on a domain or core type*)
prescribes one remedy: the enum keeps identity, a UI-side mapper resolves the resource. In practice
one package hit the idiom twice and could only apply that remedy **once**:

| Enum | Other consumers | Answer |
|---|---|---|
| `AppSkin.label`/`.tagline` | none outside the swept package | property **removed**; enum now carries only its persisted `id` |
| `GoalCategory.label` | 3, in **unswept** packages | property **kept**, superseded by `localizedLabel()` with a KDoc pointer |

**Why it matters.** The idiom is identical in both cases, so the idiom cannot be what decides. The
discriminator is **who else reads the property**, which is a fact about the codebase rather than about
the defect — and getting it wrong drags unswept packages into the unit half-done, which is worse than
one deprecated property behind a pointer. The Hebrew is authored once either way, which is the part
that actually mattered; only the English call sites migrate later.

**Corollary worth stating with it:** the replacement must be **named differently** while the old
property survives. Two members one character apart — one language-aware, one not — is how the wrong
one gets picked, so `localizedLabel()` rather than an overload of `label`.

**Destination.** `kb/dev/untranslatable-idioms.md` §1, fourth idiom — a paragraph on staging the
remedy across a partly-swept codebase. Also touches
[`enum-and-label.md`](file:///C:/Dev/JARVIS/kb/dev/enum-and-label.md).

**Anchors.** `Android_Final_Project`, `domain/model/AppSkin.kt`, `domain/model/Goal.kt`
(`GoalCategory.label` KDoc), `ui/components/ComponentStrings.kt`, `AppSkinTest`
(`the enum carries identity only, never display copy`).

**Supersedes.** Nothing; extends §1's fourth idiom.

---

## 4 · A "shared component" package holds far less shared **copy** than the sharing argument implies

**Claim.** The rationale for sweeping `ui/components/` before any feature package was that its
components are used by eight screens, so a literal there would be fixed eight times. **It was half
wrong, and measurably so:** `EmptyState`, `LoadingBox`, `DonutChart`, `StackedColumnChart` and
`SimpleBarChart` hold **no user-facing literals at all** — they take their copy as *parameters*, so
every word belongs to the eight callers and is each caller's own sweep.

What *was* genuinely shared was copy hanging off two **domain enums** the package renders
(10 category labels × 4 packages, 4 skin strings), plus **direction-isolation of caller-supplied
strings** — `SimpleBarChart` isolating `trailing` fixes eight callers at once because `"75%"` has no
strong directional character and an RTL paragraph renders it `%75`.

**Why it matters.** A well-factored presentational component is *defined* by taking its copy from
outside, so "shared UI package" and "shared copy" pull in opposite directions: the better the
component, the less of the eight-times saving is real. The saving lives where a value is
**constructed** (a domain enum's constructor argument) or where a **transformation** is applied
(bidi isolation), never where a string merely passes through as a parameter.

**Actionable form.** Before ordering a localization sweep by "shared-ness", check whether the shared
code *holds* strings or *forwards* them. Forwarding shares nothing.

**Destination.** `kb/dev/untranslatable-idioms.md` — a short section on scoping a sweep, or
[`localization-axes.md`](file:///C:/Dev/JARVIS/kb/dev/localization-axes.md).

**Anchors.** `Android_Final_Project`, `ui/components/Common.kt` (`EmptyState`, `LoadingBox`),
`ui/components/SimpleBarChart.kt` (`BarItem.trailing`), `CHANGELOG/2026-08-17/51e-sweep-components.md` §1.

**Supersedes.** Nothing. It qualifies the reasoning in `#51`'s own sequencing, not a KB claim.
