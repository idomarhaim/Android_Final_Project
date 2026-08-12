# KB candidates — `c12-charts-presentation` (2026-08-10 → 12)

Session working `#31` (`C12`, charts and presentation) on map `#12`. Written in `AUTO MODE`.

**Partly drained 2026-08-13.** Entry 5 — *a preference store belongs exactly where there is
nothing to be right about* — was ingested into `C:\Dev\JARVIS\kb` as the new page
`dev/preference-or-defect.md` (journal: `kb/log/2026-08-13.md`). It is **not** reproduced below;
the page is the record now.

**The four that remain are all one blocked group, not four independent items** — see
*Standing — always-ask*. Original numbering kept, so nothing below renumbers when the group
finally drains.

---

## Standing — always-ask

Entry 1 gates the other three: it is `rules/`-shaped, which is ⛔ always-ask in **both** modes
and owned by the 🎬 walkthrough rule, and entries 2–4 each belong in the `kb/dev/` page entry 1
would create. So the group drains together or not at all, and **no `AUTO MODE` drain applies to
any of them** — the eligibility of 2–4 in isolation is not a licence to split the page.

The question for Ido is one question, not four: **is *"an agent must render and look at its own
output when the acceptance criterion is visual"* a change to how agents work (`rules/`), or an
ordinary KB page?** If it is a KB page, all four drain in one pass with no further gate.

---

## 1 · A design agent that cannot see its own output is guessing, and the loop is cheap

**Claim.** When an agent produces something whose acceptance criterion is **visual**, it must
render and **look at** its own output between revisions. Without that it is arguing from source
code about pixels, and the argument is unfalsifiable in both directions. The instrument is
cheap — headless Edge/Chrome, `--headless --disable-gpu --no-sandbox --window-size=W,H
--screenshot=out.png file:///…` — plus a *probe page* that renders only the component under
review, so the image is a close-up rather than a whole screen.

**Why.** Concrete case, and the numbers are the argument. `C12`'s raised-3D arc went through
**three full revisions** (rev 9, 10, 11) of prose reasoning about depth, each shipped, each
rejected by Ido with a screenshot. Once the agent could screenshot, **ten rounds ran in one
turn** and **eight of the nine defects found were invisible in the source** — including two
Ido had never reported (colliding labels, a label running off the card). Rev 4 of that loop is
the sharpest evidence: a fix was applied, the render was **identical**, and the reason was that
the new contact shadow sat *behind its own caster*. No amount of code review finds that; one
look does, in seconds.

**The corollary that makes it a rule rather than a tip:** a round whose render is unchanged is a
**result**, not a wasted round. It falsifies the hypothesis immediately, which is what the prose
rounds could never do.

**Destination.** `kb/dev/` — a new page on verification instruments for non-textual work. Check
overlap with `dev/mechanism-vs-compliance.md` (neighbouring: an observation that looked like
proof of a mechanism) and with anything on prototype review.

**Anchors.** `CHANGELOG/2026-08-10/c12-charts-presentation.md` → *Revision 12* (the ten-round
table) · [#31 rounds comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5269886561)
· the harness now committed at `docs/prototypes/tools/`.

**Supersedes.** Nothing. It **extends** the standing prototype practice: `C9b` and `C12` both
built HTML prototypes precisely so a human could look — this says the *agent* must look too,
before spending the human's turn.

**Status.** ⏸️ **Always-ask.** It is arguably a change to **how agents work**, which makes it
`rules/`-shaped rather than a KB page, and `rules/` is always-ask in both modes and owned by the
🎬 walkthrough rule. If Ido rejects the `rules/` framing it drains here as an ordinary `kb/dev/`
page.

---

## 2 · Faking depth: a silhouette is not a face, and a clip is not a fit

**Claim.** Two distinct errors, repeatedly made, with one shared shape — *the appearance of the
solution without its mechanism*:

- **A silhouette is not a face.** Drawing a body's outline offset behind itself gives a shape
  with no fold, no tone of its own and no end, so it reads as a **shadow behind** the object
  rather than the **side of** it. A solid needs its faces drawn as faces — including **end
  caps**, which are what make one segment of a ring read as its own slab.
- **A clip is not a fit.** Constraining an over-sized body by clipping it to its container
  *hides* the geometry error and produces a shaved, flush-looking object. The extrusion must be
  **budgeted inside** the container's width — then the clip becomes a guarantee that never has
  anything to cut.

**Why.** `C12` rev 10 replaced a stack of translated copies (which read as a pack of cards) with
a single union silhouette — curing the banding and leaving the body flat, because the fix
addressed the *symptom* the user named rather than the property he wanted. Ido's report was
exact: *"as if the blocks have no envelope, no side faces."* Separately, widening the clip to let
a raised body rise produced *"it cuts the channel again"*, and narrowing the body so it fits
solved both at once. Rejected alternative in both cases: tuning the numbers — more steps, fewer
steps, smaller offsets — which cannot work when the mechanism is wrong.

**Destination.** `kb/dev/` — likely one page on simulating physical depth in 2D UI, or a section
in whatever page entry 1 creates. **Not** project-specific: the same two errors recur in any
chart, shadow or elevation work.

**Anchors.** `CHANGELOG/2026-08-10/c12-charts-presentation.md` → revisions 9–12 ·
[#31 envelope comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5269675482)
· `docs/prototypes/2026-08-11-visual-styles/index.html` (`sector`, `wallStrip`, `endCap`).

**Supersedes.** Nothing.

**Status.** 🟢 Ordinary `kb/dev/` page — **`AUTO MODE`-eligible**, but not drained here because
entry 1 may create the page it belongs in, and entry 1 is always-ask. **Drain the two together.**

---

## 3 · A per-element gradient cannot light a multi-element scene

**Claim.** SVG/CSS gradients default to **`objectBoundingBox`** units — relative to *each
element's own box*. Used across the parts of one composite object (the slices of a donut, the
bars of a chart, the cards of a row), that means **every part is lit from a different direction**
and the whole stops reading as one scene. Anything meant to share a light must be
`gradientUnits="userSpaceOnUse"` with coordinates spanning the **whole composite**.

**Why.** This was the least visible and most important of ten fixes in `C12`'s raised chart: each
slice's face gradient ran corner-to-corner of *its own* bounding box, so a slice at 12 o'clock and
one at 5 o'clock were lit from opposite sides. Nothing in the code looked wrong — the defect only
exists *between* elements, which is exactly the class of bug a per-element review cannot see. Same
trap applies to a bevel or shadow wash reused per element.

**Destination.** `kb/dev/` — a short, sharp page on shared-lighting in vector UI, or a section of
entry 2's page.

**Anchors.** `docs/prototypes/2026-08-11-visual-styles/index.html` (the `-f`, `-o`, `-bev`, `-shd`
gradients) · `CHANGELOG/2026-08-10/c12-charts-presentation.md` → *Revision 12*.

**Supersedes.** Nothing.

**Status.** 🟢 Ordinary, **`AUTO MODE`-eligible**; held with entry 2 for the same reason.

---

## 4 · A verification instrument can be broken on exactly the input it exists to check

**Claim.** When you build a tool to *look at* something, check it against the **hardest case the
tool exists for**, not against the easy one — because a tool can degrade silently on precisely the
input that motivated it, and every other input will keep saying it works. The tell is a failure
that appears on **one code path only** (the rewrite path, the transform path, the export path) while
the common path stays clean, so ordinary use never surfaces it.

**Why.** Concrete case, and the shape is what generalises. `C12` built `shoot.ps1` so an agent could
render and look at its own output; the project's standing design rule is *a design is not finished
until it has been seen in Hebrew*. Its `-Probe` mode copies the page before appending the probe
script, and it read that copy with PowerShell's `Get-Content -Raw` and **no `-Encoding`** — so a
BOM-less UTF-8 file was read in the machine's ANSI codepage and written back as UTF-8, double-
encoding every non-ASCII character. **Every close-up probe render showed mojibake where Hebrew
should be:** the one instrument for judging a design closely could not display the language the
standard requires it to be judged in. It survived several revisions because **whole-page renders
never touch that path**, so the tool looked perfect right up until it was pointed at its hardest
case. Rejected framing: "a PowerShell encoding gotcha" — true and useless; the reusable part is that
the instrument's blind spot lined up with its purpose.

**Destination.** `kb/dev/` — a section of whatever page entry 1 creates on verification instruments
for non-textual work; it is the same page's *"and then check the instrument"* half. Check overlap
with anything on Windows/PowerShell encoding, which is the shallow reading of it.

**Anchors.** `docs/prototypes/tools/shoot.ps1` (the `-Encoding UTF8` fix and its comment) ·
`docs/prototypes/tools/README.md` → *The encoding trap* ·
`CHANGELOG/2026-08-10/c12-charts-presentation.md` → *Revision 13 · two Hebrew-only defects*.

**Supersedes.** Nothing. **Extends entry 1** — entry 1 says the agent must look; this says the thing
it looks through can lie, and names the shape of the lie.

**Status.** 🟢 Ordinary `kb/dev/` page material, **`AUTO MODE`-eligible** in itself — but **held with
entries 2 and 3**, for the same reason they are held: it belongs in the page entry 1 would create,
and entry 1 is always-ask. Drain all four together.
