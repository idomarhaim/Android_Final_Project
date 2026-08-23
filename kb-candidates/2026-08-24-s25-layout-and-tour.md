# KB candidates — `s25-layout-and-tour`, 2026-08-24

Four entries. All four come from one session fixing four user-reported defects that
1,093 green tests could not see, and three of them are about **how you find out**
rather than about Compose.

---

## 1 · Force the reporter's screen geometry, then measure NODES — a screenshot diff cannot do this

**Claim.** When a user reports *"it looks wrong on my phone and right on yours"*, the
cheapest reproduction is to make your device **be** their device, and the honest
verification is to read **node bounds**, not to compare screenshots.

```bash
adb shell wm size 1080x2340      # their pixels
adb shell wm density 480         # their dpi  -> width_dp = px / (dpi/160)
adb shell dumpsys window displays | grep -o 'sw[0-9]*dp w[0-9]*dp'   # CONFIRM it took
adb shell wm size reset; adb shell wm density reset                  # put it back
```

Then `adb shell uiautomator dump /sdcard/ui.xml`, pull it, and read every node's
`bounds="[x1,y1][x2,y2]"`.

**Observed:** 2026-08-24, `Android_Final_Project`. Ido reported goal titles rendering
as one vertical letter per line on a Galaxy S25 Ultra; the same build on the
`Pixel_10_Pro_XL` emulator looked fine. His screenshots were 1080x2340; the emulator
is 1344x2992 at the same 480dpi. That is **360dp against 448dp** — the whole defect.

Forced to 360dp, against the **pre-fix APK already installed** (a free reproduction —
no build needed), the dump gave numbers instead of impressions:

| node | at 360dp | at 448dp |
|---|---|---|
| the goal's title | **40px wide (13dp)** | ~100px, merely truncated |
| its meta line | 40px wide × **205px tall** | one line |
| the two buttons | full size, unchanged | full size |

**Why the numbers matter more than the frame.** At 448dp the title read as *tight*
(`Learn to play t…`) and at 360dp as *broken*. A screenshot at the developer's width
shows the first and nothing tells you the second exists. `40px` versus `524px` after
the fix is a claim that cannot be argued with, and it is what turned "looks better"
into a verified fix.

**Rejected:** *ask the user for more screenshots* — they had already sent six, and
six screenshots of the symptom still do not give you the width. *Trust the emulator's
"phone" profile* — every stock AVD in this project is wider than the phone the app is
actually used on, which is why the class of defect ships at all.

**Destination.** `kb/dev/android-device-verification.md` — a new §, beside §6's render
recipes. It is the same family (*how do you actually see it*) and this is the missing
case: reproducing **someone else's** device rather than inspecting your own.

**Anchors.** `kb/dev/look-at-your-own-output.md` (measure, do not read); the
`wm size`/`wm density` pair is the mechanism, `dumpsys window displays` the check that
it took.
**Supersedes.** Nothing.
**Status.** Ready.

---

## 2 · A Compose `Row` starves its WEIGHTED child, and the defect is width-dependent so it ships

**Claim.** `Row` measures **unweighted** children first, against the full incoming
constraints, and hands the weighted ones what is left. So `Row { Text(Modifier.weight(1f)); Button(); Button() }`
— the shape everyone writes for *label on the left, actions on the right* — gives the
**label** away first. It is correct at every width the author tried and catastrophic
below the width where the buttons' intrinsic size exceeds the row.

**Why it survives review.** Three things make it invisible:

1. `weight(1f)` **reads** like a priority claim. It is the opposite: it means *take
   the remainder*.
2. The obvious defence is already in place and does nothing. The title here was
   `maxLines = 1, overflow = Ellipsis` — which cannot help, because ellipsis needs a
   width to ellipsise **into**, and the column had 13dp.
3. It degrades continuously. At the developer's width you get a slightly tight
   truncation, which reads as a design choice.

**The fix, and the three that do not work.** Put the actions on their own line.
*Weighting the buttons too* moves the truncation onto the **button labels**, and
`Schedule the fi…` is worse than a wrapped title. *Icons* trade a legibility problem
for the one in entry 3 below. *Truncating the title harder* was already done.

**Generalisation worth keeping:** in a `Row`, the child the row is **about** must
never be the only weighted one. Either everything competes or nothing does.

**Destination.** `kb/dev/` — a Compose-layout page, or a § on an existing one. This is
the first entry of its kind in the bundle.
**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/ui/components/SuccessFailureRun.kt`
`NoNextStepSection` KDoc carries the worked instance.
**Supersedes.** Nothing.
**Status.** Ready.

---

## 3 · A glyph with three paragraphs of rationale, that the user could not read

**Claim.** Design rationale density is not evidence of legibility, and the two are
easy to confuse because they live in the same file. A mark can have a defensible
shape, a defensible colour, a documented reason for being a square rather than a
circle — and still fail the only test it has, which is *can the person using the app
tell what it means*.

**Observed:** 2026-08-24. `UnmeasuredMarker` drew a dashed square carrying a bold `#`,
meaning *this goal has no number yet*. Its KDoc ran to four sections and was **good**:
why the shape is a square (a dashed circle beside `C19`'s dashed circle made one chip
carry two axes — caught by a render pass), why it is hollow, why a silent component
still needs a `contentDescription`. Every one of those arguments is about the
**square**. Not one of them is about the `#`, which was justified in a single inline
comment as *"a glyph standing for a number slot, the same way `+` does"*.

Ido's report, in full: *"there is a picture of `#` that I did not understand what it
is supposed to express."*

**The tell, and it is checkable.** The project's own committed rule already said it —
*form and words before iconography* — and the file was on the wrong side of it. So the
question is not *was there a rule* but **why did four sections of careful reasoning not
reach the one clause that decided the case**. Because the reasoning was all about the
*container* and the glyph arrived inside it as an afterthought, in a comment rather
than in the design.

**Cheap check:** for every symbol you are about to ship, ask which paragraph of the
rationale is about **the symbol** rather than about the thing carrying it. If the
answer is *none*, it has not been designed, it has been assumed.

**What replaced it.** Split by what is beside it: words (`No number`) where the marker
replaces a percentage in a list and no sentence is in the glance; **nothing at all** —
an empty dashed square — at hero sizes, where the full sentence was already printed
next to it. The glyph was never carrying meaning the sentence did not; it was
competing with it.

**Destination.** `kb/dev/` or the design material — the same neighbourhood as
`describing-is-not-exhibiting.md`, and possibly a § on it: that page is about a frame
that *describes* rather than *exhibits*, and this is a frame that **exhibits perfectly
and communicates nothing**.
**Anchors.** `ui/components/UnmeasuredMarker.kt` KDoc.
**Supersedes.** Nothing.
**Status.** Ready.

---

## 4 · A fix can be HALF a fix, and only running it tells you which half you got

**Claim.** When you fix a reported symptom, the fix can satisfy the literal report and
still fail the sentence the user actually said — and the second half is routinely
**created by the first**. Reading the diff cannot find this. Running the user's own
gesture can, and it is the only thing that can.

**Observed:** 2026-08-24, the guided tour. Ido: *"when it marked me to press certain
buttons (for example the calendar), I did not see it open what I pressed on."*

- **Defect 1.** The overlay's touch blockers derived *is the spotlight's hole live*
  from `onTapThrough == null`. On an informational step that meant the hole was
  **covered**: the tap on the pulsing ring hit the scrim, whose behaviour was *advance
  the tour*. The ring invited a press and then ate it. Fixed by splitting one derived
  value into the two independent facts it had been conflating.
- **Defect 2, which defect 1's fix created the conditions for.** Now the Calendar
  really opened — and the step advanced, and the **next** step lives on the dashboard,
  so the host navigated straight back to Home. The calendar was on screen for about
  one frame. **The literal report was satisfied and the sentence was not.**

Defect 2 is invisible in the diff: every line of the first fix is correct, and the
thing that breaks it is a property of the *next* step, in a different file. It showed
up in the first `adb shell input tap` on the real widget, in about three seconds.

**The remedy that generalises:** after fixing a reported behaviour, **perform the
reported gesture and watch what the user would have watched** — not the unit under
test, the *outcome they described*. And read the user's sentence for what it asks for
rather than for the bug it names: *"I want to see the result of what I press"* is
satisfied by neither *nothing happens* nor *it happens and is undone*.

**Rejected:** *a test for it* — one was added afterwards (`an INVITED action does not
advance -- it stops the tour steering`), and it is worth having, but it was written
**from** the observation. Nothing about the code suggested the assertion beforehand,
which is the point.

**Destination.** `kb/dev/look-at-your-own-output.md` — a § under its
*re-run whatever will consume your output* clause. The consumer here is **the user's
finger**, which is the case that clause does not yet have an instance of.
**Anchors.** `ui/tutorial/TutorialController.kt` `TutorialUiState.route` KDoc;
`CHANGELOG/2026-08-24/s25-layout-and-tour.md` §4b.
**Supersedes.** Nothing.
**Status.** Ready.
