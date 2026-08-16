# `51b-sweep-analytics` — 2026-08-16

`/implement` [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) — the literal
sweep, **one package: `feature/analytics/`** · branch `feat/goalpilot-implementation` · mode
`AUTO MODE`

Ido's constraint: not `dashboard/` or `goals/`. *"The first pass is about proving the mechanism — the
parity test, the resource layout, the bidi call sites — not about volume."*

## 1 · Why analytics

Measured rather than guessed. Prose literals per package: `lifeareas` 93, `dashboard` 91, `goals` 73,
`challenges` 71, **`analytics` 52**, `social` 39, `health` 14, `profile` 11, `auth` 4. *(Worth
recording: by this measure `lifeareas` is **larger** than `goals`, so "the two biggest" is
`lifeareas` and `dashboard`.)*

`analytics` is the only mid-size package that contains **all four** bidi categories at once —
percentage, duration, count **and** range — plus a `→` between two durations, which is §4.8's named
defect in miniature. It also carries five "goal" strings for the יעד terminology, and it *consumes*
`AnalyticsRange.windowLabel`, whose producer was isolated last session, so the sweep tests that seam
end to end.

## 2 · The parity guard was checked before anything was written

Ido: *"If the parity test does NOT fail on a deliberately missing translation, that finding is worth
more than the strings."* It fails, in both directions:

| Injected fault | Result |
|---|---|
| English key `zz_instrument_check` with no Hebrew | **FAILED** — `[strings.xml:zz_instrument_check]` |
| Removed the Hebrew for an existing key | **FAILED** |

The message names the offending key rather than just the count. Both faults reverted, suite green
again, before the sweep began.

## 3 · What shipped

**68 keys**, `res/values/analytics_strings.xml` and `res/values-iw/analytics_strings.xml` — a sibling
file per package, following `widget_strings.xml`, so two sessions can sweep two packages without
contending on one file. The parity test pairs files **by name**, which is what makes that layout safe.

Three authoring rules, each bought by a defect:

1. **Whole sentences, never fragments.** The backfill sheet built its opening line with a
   `buildString { append(…) }` of six pieces. Fragment concatenation is untranslatable — word order
   is a property of the language. Replaced by **four** complete sentences, one per situation
   (partial × unanswered).
2. **`<plurals>`, not `if (n == 1) "" else "s"`.** Seven sites had an English plural rule baked into
   Kotlin. Hebrew has one/two/many/other, which no `-s` test can express.
3. **Numbers arrive pre-isolated** (`%1$s`, not `%1$d`). Every count, percentage, duration and range
   goes through `core/util/Bidi.kt` — **no second helper**: `d2-life-area-route` wrote one and
   deleted it in favour of that file, and a third would repeat the mistake.

**Terminology (§5.1 / `E1`).** `Progress by goal` → **התקדמות לפי יעד**. יעד throughout, never מטרה.

**A §4.8 wording trap avoided.** "The AI" cannot be `ה‑AI` in Hebrew: a Hebrew prefix attached to a
Latin run lays out on the far side of it, rendering `AI‑ה`. It is **הבינה המלאכותית** everywhere —
pure Hebrew, and the natural term anyway. The same rule is why no sentence attaches a prefix to a
`%1$s` that may hold digits: where English says "in this week", the Hebrew says
`בטווח הנוכחי (%1$s)`.

**The arrow flips.** `analytics_backfill_change` is `%1$s → %2$s` in English and `%1$s ← %2$s` in
Hebrew — the same before/after claim reads the other way. A translation, not a typo.

**Two seams moved, because English was living where a language switch cannot reach it:**

- `AnalyticsRange` carried `label = "Day"` and `bucketNoun = "4 hours"` as constructor arguments.
  Speech frozen into a `core/util` enum is the same defect as the process-scoped date formatters,
  one layer up. The enum is now bare; `feature/analytics/AnalyticsStrings.kt` maps each constant to
  its resource, the way `iconForKey` maps a `GoalCategory` to its icon.
- `AnalyticsViewModel` held `"Updated $saved task durations"`. It now emits a typed
  `AnalyticsMessage` and the screen resolves it — which is also what lets the count be isolated and
  the plural rule live per language.

**`labelInline()` is a separate resource, not `label().lowercase()`.** Case is a property of English;
Hebrew has none, and a locale where case-mapping is not per-character (Turkish dotted/dotless i)
turns the transformation into a bug. The two forms are identical in Hebrew, which is the clearest
possible statement that the difference belongs to the translator.

## 4 · A new guard, and it was checked against the fault it exists for

`AnalyticsLiteralSweepTest` fails if user-facing prose returns to a swept package. It is needed
because **a sweep is an event, not a state**: the package is clean the day it lands and drifts the
first time somebody adds `Text("Try again")`. Nothing else notices — the build is fine, the English
render is perfect, and `HebrewLocaleResourceTest` is blind to it, because a literal that never
reached `res/` has no key to be missing a translation for.

Verified by injecting `Text("Asking the AI now")`: **FAILED** with
`[AnalyticsScreen.kt: "Asking the AI now"]`. Restored, green.

`SWEPT_PACKAGES` is a record of progress — a package absent from it is *unswept*, not exempt.

## 5 · The on-device check is now reflective, and it found one thing

`AppLocaleInstrumentedTest` enumerated a hand-kept list of keys. It now walks `R.string` by
reflection, so a package swept next week is covered the day it lands. Run against the device it
reported exactly one key resolving identically in both languages: **`gp_widget_percent`** = `%1$d%%`,
genuinely language-independent, now named with its reason. **Every analytics string resolved
differently in Hebrew on the device.**

## 6 · Two findings this pass produced, neither fixed here

1. 🔴 **The widget's Hebrew contradicts §5.1's terminology.** `values-iw/widget_strings.xml` uses
   **מטרה** for `Goal` in six strings (`gp_widget_goal`, `gp_widget_goals`, `gp_widget_no_goals`,
   `gp_widget_goals_ring_meaning`, the `gp_widget_goals_without_measure` plural,
   `gp_widget_pick_goals_desc`) — and one of them uses מטרה and יעד in the same sentence with the
   meanings swapped. §5.1/`E1` says the entity is **יעד**. Not fixed: it is a different package, and
   Ido's instruction was one.
2. ⚠️ **`ל־%1$d` in that same file is the §4.8 prefix defect**, attaching a Hebrew prefix directly to
   a digit run. Same file, same ticket, same reason for leaving it.

Both are now on #51's remaining list.

## 7 · The rendered check, and what it could and could not reach

Installed, set to Hebrew by writing the same per-device preference the picker writes, and launched.
**No fatal exception, process alive, RTL genuinely active** (the sign-in arrow is mirrored).

It reached the **auth** screen, which is unswept — and that screen is showing a live §4.8 artifact:
the tagline's full stop has jumped to the **left** end of the wrapped line
(`.stay motivated with friends`), because a neutral character takes the RTL paragraph's direction.
That is the defect class rendered on a real device, on a screen nobody has swept yet — useful
evidence that the remaining sweep is not cosmetic.

**`unverified`: the analytics screen itself was not seen by eye.** Reaching it needs a real Google
sign-in, which is not something to do on the user's behalf. Its Hebrew is proven instead by the
reflective on-device test in §5 — every string, resolved through the real resource table on the real
device — which is stronger than a screenshot for *coverage* and weaker for *layout*. A Hebrew render
of the donut is still owed (#51 item 3, the caption overrun).

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`--rerun-tasks`) | **350 pass, 0 fail**, 37 classes — was 348 |
| **Instrumented / UI E2E** | **51 pass, 0 fail** |
| **Build** (`:app:assembleDebug`) | green |
| **Real install + launch, app set to Hebrew** | no fatal exception, process alive, RTL active |
| **`firestore-tests`** | **not owed** — no `firestore.rules` change |
| **Cloud Functions** | **not touched** |

New: `AnalyticsLiteralSweepTest` (2). `AppLocaleInstrumentedTest` rewritten to reflect over
`R.string` rather than a hand-kept list — same test count, much wider reach.

## 8 · What is left of #51

1. **Eight packages unswept** — `lifeareas` (93 literals), `dashboard` (91), `goals` (73),
   `challenges` (71), `social` (39), `health` (14), `profile` (11), `auth` (4). One session each;
   `AnalyticsLiteralSweepTest.SWEPT_PACKAGES` and `AppLocaleInstrumentedTest.OWNED_PREFIXES` are the
   two lists a new sweep extends.
2. **The widget's terminology and prefix defects** — §6 above.
3. **§5.1's first filed defect** — no prompt states an output language; needs `language` threaded to
   `functions/` and a `firebase deploy`, which is always-ask.
4. **The donut's Hebrew caption overrun** (#51 item 3) — needs a Hebrew render of analytics, which
   needs a signed-in account.
5. **`ui/components/` is unswept** and is shared by every screen — `EmptyState`, `LoadingBox` and the
   chart components may carry their own literals. Worth checking before the per-package sweeps, since
   a literal there shows on eight screens.

## Board

Claimed before the first write; released with this commit. Emulator `Pixel_10_Pro_XL` and the Gradle
daemon released. `kb-candidates/2026-08-16-51b-sweep-analytics.md` written — 3 entries, all
`kb/`-destined and cross-repo, none drained here.
