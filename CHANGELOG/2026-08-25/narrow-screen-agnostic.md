# `narrow-screen-agnostic` — 2026-08-25

> **Summary:** Ido photographed his S25 Ultra with the challenge card's `Standings` button
> rendered as a column of single letters. Seven `Row`s app-wide could do the same thing;
> all seven are now `FlowRow`s with single-line labels. The guard written to hold the repair
> was **measured not to hold it** and says so in its own KDoc — the render pass at his
> geometry is what actually verifies this fix.

His ask was the general one, and it is the right one:

> *"the text, and generally the proportions and shapes and sizes, should be agnostic to
> different screen sizes"*

---

## What was wrong

A `Row` gives its last child whatever width is left over. When that is less than one word,
the text has nowhere to go but downwards — so `Standings` became nine lines of one character.

**A mechanical sweep found seven such rows**, in five files, written by different sessions
months apart: two in `SlotSheet`, two in the challenges feature, one in `DashboardScreen`,
one in `SuccessFailureRun`, and the one he photographed. `FlowRow` is already this repo's
answer — `FillButtonRow.kt` documents it in as many words, *"a Row would clip the last one
rather than wrap it"* — and these simply never got it.

### ⚠️ I predicted this exact defect and got the number wrong

`challenges-finish-the-job` §1, hours earlier, reasoned about this card at this geometry:

> *"the action row is already at its width limit — **Change goal · Type a score · Standings**
> is three, and a **fourth** wraps at his 384 dp / font 1.15"*

and put the invite affordance in the card header on that basis. The mechanism was right and
the margin was wrong: **three** already wrap. The header decision stands; the row it was
measured against was already broken when the measurement was made.

The render pass that session ran showed nothing, because it photographed at AVD width.

---

## The repair

All seven rows are `FlowRow`s, so a row that will not fit **wraps** instead of crushing its
last child. Every action label additionally carries `maxLines = 1`, so a single over-wide
button cannot stack even when it is alone.

---

## ⚠️ The guard I wrote does not catch what I wrote it for, and I am saying so

`NarrowScreenGuardTest` composes the card at his geometry and asserts no control grows tall
enough to be a stacked label. **It passes with the `FlowRow` reverted.** Verified twice.

The reason is the other half of the repair: once every label has `maxLines = 1`, a crushed
button cannot stack — it **truncates**. Truncation changes neither height nor width enough to
separate from a legitimately short button, and Compose's semantics do not expose *"this text
was ellipsised"*. A width floor was tried and dropped: at his geometry the crushed
`Standings` still measures about 74 dp, above any floor a real 40 dp icon button permits.

So the file now states its limits in its own KDoc. **It guards the other half** — a label
added later without `maxLines`, a font scale raised, a translation three times its English
length — any of which brings the stacking straight back. That is worth having. It is not
worth misdescribing.

**`NarrowScreenRenderPass` is what verifies this fix**, at his geometry, by looking.

### And the harness was wrong before that, in a way worth recording

The first version composed the card in a **384 dp** box. The mutation test passed, and the
reason was arithmetic: `ChallengesScreen`'s `LazyColumn` spends `start = 16, end = 16`
*before any card exists*, so the card's real width on his phone is **352**, and its own
`padding(16.dp)` leaves **320** for buttons that need 337. At the full 384 there is no
defect to find.

**A harness that does not spend the container's padding is not asking the user's question.**

---

## 🧪 Tests

| layer | result |
|---|---|
| **JVM unit** — whole suite | **1197 / 1197** |
| **Instrumented** — whole suite | **335 / 335, 0 failing**, 452 s — after the emulator restart described below |
| **`NarrowScreenGuardTest`** | **3 pass** — and honestly scoped, see above |
| **`NarrowScreenRenderPass`** | **1 test, 4 frames**, all opened |

## 📸 Render pass — `docs/render-passes/2026-08-25-narrow-screen/`

At **352 dp / font 1.15**, the card's real width on his phone.

- **`card-at-his-width`** — *Change goal · Type a score* on the first line, **`Standings`
  whole** on the second. Three complete words, no column of letters.
- **`card-with-pending`** — the same card carrying §3's approval banner, which adds two more
  controls. `Agree` and `Withdraw` both intact.

## ⚠️ Two instrument failures on the way, neither of them the app

1. **`r8` rejects backtick test names in instrumented tests.** Every JVM suite here names its
   tests in backticks with spaces; **no instrumented one does**, and this is why:
   `Space characters in SimpleName 'the challenge card's actions survive Ido's phone' are not
   allowed prior to DEX version 040`. The error names a synthetic `…$1.class` before it names
   the method, so the first repair guessed at the lambdas and was wrong.
2. **The emulator's window-capture surface wedged**, and it looked exactly like a code
   regression: six `EntranceAnimationUiTest` failures reading *transparent* where they
   expected red, plus a render frame that came back one flat colour — on code that had passed
   **331/331** an hour earlier. `captureRegionToImage` was throwing underneath. Neither the
   display state nor the host window size was the cause; **an emulator restart fixed it
   completely**, 6/6 immediately afterwards. Worth knowing before anybody spends an hour
   bisecting their own diff.
