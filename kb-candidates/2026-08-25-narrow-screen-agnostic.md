# KB candidates — `narrow-screen-agnostic`, 2026-08-25

Each entry stands alone; nothing leans on a transcript.

---

## 1 · A guard can pass its own mutation test for a reason that has nothing to do with the guard

**Claim.** `NarrowScreenGuardTest` was written to hold one specific repair — `FlowRow`
instead of `Row`, so a button row wraps instead of crushing its last child. Reverting the
repair left it **green**. Two separate causes, found in that order, and both generalise:

1. **The harness did not reproduce the user's width.** It composed the *card* in a box of the
   *screen's* width, 384 dp. On the real screen the enclosing `LazyColumn` spends `start =
   16, end = 16` before any card exists, so the card gets **352**, and its own `padding(16)`
   leaves **320** for buttons needing 337. At 384 there was simply no defect to find, and the
   fixed and mutated layouts logged **identical control sizes, to the pixel**.
   → *A harness that does not spend the container's padding is not asking the user's
   question.* Reproduce the width the component actually receives, not the width of the
   device.
2. **The other half of the same repair erased the symptom being measured.** The fix was
   `FlowRow` **and** `maxLines = 1`. With `maxLines = 1` a crushed button can no longer
   stack; it **truncates**. The guard measured height — the stacking symptom — which the
   second half of the repair had made unreachable. A width floor was then tried and also
   failed to discriminate: the crushed control still measured ~74 dp, above any floor a real
   40 dp icon button allows.
   → *When a fix has two parts, check that the assertion still discriminates with both
   applied.* One part can mask the signal the other part's test depends on.

**What was done about it, and this is the transferable half:** the guard's KDoc and the
changelog now **state that it does not catch the regression it was written for**, and name
the render pass that does. A guard whose limits are misdescribed is worse than no guard,
because it is trusted. The temptation to quietly keep the reassuring name was real.

- **Destination** `kb/dev/look-at-your-own-output.md` — its subject is verification that
  fails silently; this is a verification that fails silently *while reporting success*
- **Anchors** `NarrowScreenGuardTest` (its "what this does NOT catch" section) ·
  `NarrowScreenRenderPass` · `CHANGELOG/2026-08-25/narrow-screen-agnostic.md`
- **Supersedes** nothing
- **Status** ready

---

## 2 · `r8` rejects backtick test names with spaces — so instrumented tests cannot use the convention every JVM suite here uses

**Claim.** Kotlin backtick identifiers with spaces are legal Kotlin and legal JVM bytecode.
**Dex rejects them**:

```
D8: Space characters in SimpleName 'the challenge card's actions survive Ido's phone'
    are not allowed prior to DEX version 040
```

So the repo's universal JVM-test naming convention is unavailable in `androidTest`, and every
existing instrumented suite here already uses camelCase — a convention nobody had written
down, so a new file naturally broke it.

**Why it costs more than a rename.** The error names a **synthetic lambda class** before it
names the method:
`NarrowScreenGuardTest$and it still holds at a width nobody designs for$1$1$1$1.class`.
That reads as *"the lambdas are the problem"*, and the first repair chased it — extracting
every `setContent` lambda into a helper — which changed nothing, because the **method name
itself** is what dex cannot encode. Only after that did the plain `NarrowScreenGuardTest.class`
line in the same output become readable.

- **Destination** `kb/dev/` — alongside `android-device-verification.md`, or wherever
  instrumented-test mechanics live
- **Anchors** `NarrowScreenGuardTest`'s KDoc · `CHANGELOG/2026-08-25/narrow-screen-agnostic.md`
- **Supersedes** nothing
- **Status** ready

---

## 3 · An emulator's window-capture surface can wedge, and it presents as a code regression

**Claim.** Six `EntranceAnimationUiTest` cases failed reading **transparent** where they
expected red, and a render frame came back **one flat colour** — on code that had passed
**331 / 331** an hour earlier. Underneath, `captureRegionToImage` was throwing.

**Everything about it points at the diff.** The failures are in pixel assertions, they arrive
together, and they arrive right after a batch of UI changes — so the natural next move is to
bisect one's own work, which cannot converge because the code is fine.

**What was ruled out, in order:** the display being asleep (`mWakefulness=Awake` throughout);
the host window having been resized (restoring its native geometry changed nothing); the test
class being order-dependent (it failed alone too). **An emulator restart fixed it
completely** — 6/6 on the next run, same APK, same code.

**The cheap discriminator, worth reaching for first:** *did this exact code pass on this exact
device recently?* If yes, and the new failures are all pixel-reading, restart the emulator
**before** reading the diff. Two minutes against an hour.

**And the thing that made it legible at all** was a floor already in this repo's render
passes: *more than one distinct colour*. Size assertions passed on the blank frame, as they
always do.

- **Destination** `kb/dev/android-device-verification.md`
- **Anchors** `CHANGELOG/2026-08-25/narrow-screen-agnostic.md` · `NarrowScreenRenderPass`'s
  flat-colour floor
- **Supersedes** nothing
- **Status** ready

---

## 4 · The emulator window opens ~900 px above the visible desktop on this machine

**Claim.** `emulator -avd Pixel_10_Pro_XL_B` opens its window at approximately **`y = -897`**
— almost entirely above the screen, with a sliver overlapping the editor's tab bar. Observed
on every launch this session (three of them). It is also **taller than the work area** (978
against 912), so centring alone leaves it overflowing the bottom.

Ido asked for it to be centred, with a screenshot of it covering his tabs.

**Two details that matter:**
- **Verify the move by re-reading the rect.** The first attempt reported success while still
  hanging 66 px off the bottom.
- **Centre without resizing where possible.** Scaling was tried first and is what made the
  capture wedge a plausible suspect later; keeping the native size and clamping `y` to the
  work area's top is enough and disturbs nothing.

- **Destination** `kb/dev/android-device-verification.md`, or the machine-setup notes
- **Anchors** the session memory `centre-the-emulator-window`
- **Supersedes** nothing
- **Status** ready

---

## 5 · `Row { Button(); Button() }` is the obvious spelling and is wrong on a phone the author does not own

**Claim.** A mechanical sweep of one app found **seven** rows holding two or more buttons with
no `weight()`, across five files, written by different sessions months apart — every one
capable of rendering its last label one character per line on a narrow screen. `FlowRow` was
already the documented house answer in a sixth file, whose comment explains the exact hazard.

**Why care does not prevent it.** `Row { Button(); Button() }` reads correctly, reviews
correctly, and is only wrong at a width the author never sees. The session that shipped the
worst instance had **explicitly reasoned about this card at this geometry** hours earlier and
concluded a *fourth* button would be the one to wrap — right about the mechanism, wrong about
the margin, and the render pass it ran photographed at emulator width, where nothing breaks.

**The cheap sweep is the finding.** A dozen lines of regex over the source — *a `Row` whose
body contains two or more button calls and no `weight()`* — turns a bug report about one card
into a complete list. Run it before fixing the reported instance, not after.

- **Destination** `kb/dev/` — a short page on layout at unknown widths, or the Compose notes
- **Anchors** `FillButtonRow.kt` (the pre-existing rationale) ·
  `CHANGELOG/2026-08-25/narrow-screen-agnostic.md`
- **Supersedes** nothing
- **Status** ready
