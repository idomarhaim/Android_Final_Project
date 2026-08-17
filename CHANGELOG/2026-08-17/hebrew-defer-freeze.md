# `hebrew-defer-freeze` — 2026-08-17

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** `#51` is frozen behind one switch — `AppLanguage.OFFERED` — and §0.8's *"not finished until seen in Hebrew"* is suspended in writing, so no feature ticket blocks on Hebrew again; the brief named two doors into a half-Hebrew app and there turned out to be a third, in the persistence read path.

`/kickoff hebrew-defer-freeze` — wave 1 of
[`TODO/TODO_MUST/Completion-Roadmap.TODO.must.md`](../../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)
· issue [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) · mode `AUTO MODE`
(Ido's message opened with it; the brief's front matter said `normal`, and this session's
message wins).

**Nothing is reverted.** `values-iw/`, `Bidi.kt`, `LocaleAwareWindows.kt`, both swept
packages, every locale test and `AppLanguage.HEBREW` itself are exactly where five sessions
left them. `#51` stays **OPEN**. What stops is the per-package literal sweep.

---

## 1 · The brief named two doors into a half-Hebrew app. There are three.

The brief's steps 1 and 2 are right and are done. Reading the call graph out from
`AppLanguage` for the write path turned up a third door the brief does not mention, and it
is the one that would have bitten **this project's own emulator first**:

| # | Door | Fires when | Closed by |
|---|---|---|---|
| 1 | the picker | user taps **עברית** | `LanguagePicker` iterates `AppLanguage.OFFERED`, not `entries` |
| 2 | the `SYSTEM` branch | device locale is Hebrew, **nobody touched a setting** | `AppLocale` → `AppLanguage.clampToOffered(deviceLocale)` |
| 3 | **persistence** | `"he"` is already in SharedPreferences from before the freeze | `AppPreferencesRepositoryImpl` → `AppLanguage.offeredFromId(stored)` |

**Door 3 is not hypothetical and it is not tidy-up.** `AppPreferencesRepositoryImpl` reads
`AppLanguage.fromId(prefs.getString(KEY_LANGUAGE, null))`, and `fromId` is *faithful* — it
hands `"he"` back as `HEBREW` regardless of what the picker is willing to show. Hebrew was
selectable in the app right up to this commit, and `51e-sweep-components`' release note
records `Observed:` **rendering the app in both languages on the device**. So the freeze
would have held on every device except the ones that had actually used the feature. A door
that is shut everywhere except where you look is worse than no door.

`Inferred:` — that this project's own emulator is one of those devices. It follows from
`51e` having rendered Hebrew there, but which AVD and whether the preference survived that
session's uninstall were **not** checked; `adb` was blocked (§5). `Untested:` no device
confirmation of door 3 at all this session.

### What was deliberately *not* done

- **`DEFAULT` is still `SYSTEM`.** Setting it to `ENGLISH` is the obvious fix for door 2 and
  it is wrong twice: it leaves `SYSTEM` selectable and still resolving Hebrew, and it
  discards the follow-the-device semantics `#51` wants back intact.
- **`fromId` is not narrowed.** It is the id round-trip for *every* entry
  (`AppLanguageTest.every id round-trips`), and `#51` needs it whole. `offeredFromId` wraps
  it for the one caller — persistence — that must not be faithful right now.
- **The stored `"he"` is not rewritten.** Clamping on read, not on load, means resuming `#51`
  is one list change and the user's old preference is honoured again untouched.
- **`AppLanguage.HEBREW` stays.** `values-iw/`, `isRtl`, `locale`, `HebrewLocaleResourceTest`
  and the whole `androidTest/locale/` suite are written against it.

### One warning knowingly stepped over, and why it does not apply

`AppLanguage.locale`'s KDoc says **nothing may branch on `locale.language`** — the `he`/`iw`
normalization split. `clampToOffered` branches on exactly that, and it is safe for the reason
that warning is really about: the hazard is a *hand-written table of language codes*, which
must spell Hebrew both ways and silently returns the wrong answer when a runtime changes
which spelling it reports. Nothing is hand-spelled here — both sides of the comparison come
out of `java.util.Locale`, so whatever normalization a runtime applies, it applies to both.
The only literal in play is `"en"`, which no runtime has ever spelled twice. The test asserts
the clamp on **both** `he` and `iw` anyway, so a runtime that disagreed would fail loudly
rather than leak.

---

## 2 · §0.8 suspended in writing — the load-bearing step

`docs/PRODUCT_v0.3.md` §0.8 sub-rule 3 is what made `#51` *"a precondition of every screen
ticket"*: a feature session reads it and re-blocks itself, which is the failure this whole
session exists to stop.

- **[AGENTS.md](../../AGENTS.md) § *🛑 §0.8 is suspended*** — new, placed **above** the
  authoritative-docs list, because a session that reaches §0.8 by way of the doc index has
  already been blocked by the time it gets there. States what is permitted (plain English
  literals in any unswept package; ship without a Hebrew render pass), what is **not**
  relaxed (`DialogLocaleGuardTest` stays armed; delete nothing), and that resuming is one
  list change.
- **`docs/PRODUCT_v0.3.md` §0.8** — a suspension note beside the rule, scoped to **sub-rule 3
  only**. Sub-rules 1 (one chip may not carry two axes) and 2 (form and words before
  iconography) are untouched and still bind; neither was ever about language, and suspending
  §0.8 wholesale would have quietly dropped two rules Ido bought with defects he caught.
  Re-checked AGENTS.md's frozen list first: it names `GoalPilot_spec_EN.docx` and the gradle
  wrapper, not this file.

---

## 3 · The sweep guard is frozen, and freezing it changed no behaviour

`AnalyticsLiteralSweepTest.SWEPT_PACKAGES` stays at `["feature/analytics", "ui/components"]`.
The added KDoc names the eight deferred packages, points at the roadmap, and says the thing a
future session most needs to hear: **do not add your package here as a favour.**

**This is documentation, not a behaviour change, and the reason is that the guard is
opt-in** — it reads only what is in that list, so an absent package is *unswept*, not
failing. Nothing had to be relaxed, disabled or `@Ignore`d to park the sweep, and the two
listed packages stay guarded exactly as strictly as before. A deferral that had required
weakening the instrument would be a different and much worse thing.

---

## 4 · A released session's uncommitted work, folded in first — `105baaf`

`CHANGELOG/2026-08-17/51e-sweep-components.md` was sitting **dirty in the working tree** at
session start with +135 lines its author never committed: the section recording that the
`#51` comment is *owed* (GitHub 503, not a skip) and the appendix holding that comment
verbatim. The brief's step 5 depends on that appendix being at HEAD.

Established the owner was gone **before** touching it, per §5.4 / precondition 5:

- `SESSIONS.md` carries an **explicit release note** for `51e-sweep-components` — a positive
  self-report, which settles liveness without a transcript.
- Active claims held **zero** rows at HEAD (counted mechanically, not eyeballed).
- Transcript last turns: `51e`-era sessions at **15:31Z / 15:36Z**; the most recent session
  in this project, `completion-roadmap` (`fc7afa54`), last spoke at **16:06Z** to report *all
  done, committed `da20225` and pushed*. Nothing live.

Committed **their paths alone, in a commit of their own, content unedited** — a commit that
also changes what it commits is indistinguishable from your own work. The index row for that
file gained its summary as a side effect, because the dirty half is what added the mandatory
`> **Summary:**` line.

---

## 5 · 🧪 Tests

| Layer | Result |
|---|---|
| JVM unit (`:app:testDebugUnitTest`) | **364 / 0**, 0 skipped — was 358 before, +6 new |
| Build (`:app:assembleDebug`) | **green** |
| Instrumented (`connectedDebugAndroidTest`) | **not run** — device commands blocked, see below. Not owed either: nothing this session changed is in the instrumented suites' path, and running it would have uninstalled the app |
| Firestore rules (`firestore-tests/`) | **not applicable** — no rules, functions or Firestore access changed |
| Cloud Functions | **not applicable** — `functions/` untouched |

Six new tests in `AppLanguageTest`, one per claim rather than one per method:

1. `hebrew is withheld from the picker but still exists` — both halves, because they pull
   opposite ways and a change that satisfied only one would be a regression either way.
2. `what the picker offers is exactly system and english`.
3. `system clamps a device language the app does not offer to english` — asserted on `he`,
   `iw` **and** `fr-FR`.
4. `system leaves an offered device locale alone, region and all` — `en-GB` must not be
   flattened to bare `en`, or the clamp silently eats §5.1's Region/Language decoupling.
5. `a hebrew preference stored before the freeze reads back as the default` — and asserts in
   the same test that plain `fromId` still returns `HEBREW`, so narrowing the wrong one fails.
6. `an offered preference still reads back unchanged` — the clamp must be invisible to
   everyone it does not apply to.

### ⚠️ The visual claim is NOT verified — step 6 could not run

The brief's step 6 is the real acceptance criterion (*the app is uniformly English*, on a
Hebrew-locale device, looked at rather than reasoned about). **`adb` is blocked by the
harness classifier in this session**, so no emulator was booted, no locale was changed and
nothing was rendered. `Unverified:`, not *passed* — the JVM tests prove the three clamps are
correct as **logic**, and prove nothing about what the two swept packages actually paint.

`Pixel_10_Pro_XL` was **never touched**: no sign-in was needed, none was consumed, and the
device is in whatever state `51e`'s instrumented run left it.

---

## 6 · ⚠️ Both `#51` GitHub writes are OWED — blocked, not skipped

Neither of the brief's GitHub steps could run. `gh api repos/:owner/:repo/issues/51/comments`
was **denied by the harness classifier** (an outward-action gate, not the 503 outage `51e`
hit — REST itself is healthy: `gh api …/issues/51` reads fine and returns `open`). Attempting
the same write under a different command spelling would be working around the gate rather
than satisfying it, so it was not attempted.

Three things are therefore still owed on `#51`, and **nobody should assume any of them
happened**:

1. **`51e-sweep-components`' comment**, owed since 2026-08-17 and now owed by two sessions.
   Body verbatim: `CHANGELOG/2026-08-17/51e-sweep-components.md` lines 248–347, at HEAD
   since `105baaf`.
2. **A deferral comment**, so `#51`'s own thread says why it stopped and in what state.
3. **The body edit** dropping *"a precondition of every screen ticket"* — the line that
   re-blocks every feature session that reads the issue.

**Item 3's damage is already contained** even while it is owed: AGENTS.md's suspension block
is the first thing in the file every agent reads, and it says in as many words that the
sub-rule does not apply. The issue body is the second place a session would look, not the
first.

---

## 7 · What `#51` still owes, unchanged

Unswept, in the roadmap's order: `auth`, `challenges`, `dashboard`, `goals`, `health`,
`lifeareas`, `profile`, `social`. The Hebrew *mechanisms* are all solved and pinned by tests;
only the per-package grind remains, which is the part that can wait.

**Resuming is one line:** put `HEBREW` back in `AppLanguage.OFFERED`. Everything else —
`values-iw/`, the guard, the dialog façades, `GoalCategory.localizedLabel()` — is already
there and already tested.
