# `#64` · `C19`'s success/failure run — render pass, 2026-08-23

Seven frames from `SuccessFailureRunUiTest`, captured on `emulator-5554`
(`Pixel_10_Pro_XL`) with `adb install -r` + `am instrument` — never
`connectedDebugAndroidTest`, which uninstalls the app. §0.8 is suspended by this
ticket's brief, so **English only**.

| Frame | Shows | Why it exists |
|---|---|---|
| `issue-64-area-light.png` · `-dark.png` | Two life-area cards: an area in **good shape** (`kkkmkkko`) above one in **bad shape** (`mmmmmkmm`) | §4.7's tone rule is only tested where the news is **bad**. Same component, **no red**, and `kept` is still the filled dot |
| `issue-64-quiet-light.png` · `-dark.png` | An area where almost nothing was ever scheduled — the two **offers**, which differ | The case no closed ticket covered. `Break it into steps` (`open work = 0`) beside `Schedule the first one` (work without dates), counted in neither number |
| `issue-64-analytics-light.png` · `-dark.png` | The same component with `showAsymmetryNote = true` | §4.7 puts `C17`'s asymmetry sentence beside the time donut **and nowhere else**. The area frames above are the other half of that claim |
| `issue-64-thirty-days-light.png` | The **30-day** window — 29 dots | The only frame that answers whether the widest run wraps legibly or turns into a grey band. Nothing in the source says; only the frame does |

Dark is not a duplicate: §4.7's whole material argument is that outcome state is
carried by **form** and never hue, and a dark frame is where that claim is
actually tested. `#65` found its defect in a dark render and `C12` before it.

## What the render caught

**One defect, invisible in the source.** The `no next step` mark — §4.7's *"a
dashed ring carrying a `+`… deliberately unlike all four, because it is an
invitation and not an outcome"* — was first drawn at `17.dp` with arms at `0.24`
of the diameter and a plus as thick as its ring. It **filled itself in**: the
mark read as a dense blob and its nearest neighbour on the screen was the
**still-owed pip**, which is the one dot it must never be confused with. That is
§0.8's *one chip may not carry two axes*, and every assertion in the suite passed
while it was true — a per-node query cannot see that two marks resemble each
other.

Fixed at `20.dp` with a smaller, thinner plus and air between it and the ring.
The frames here are the second pass; the mark now reads as an invitation.

## What the frames confirm, and what they cannot

**Confirmed by looking:** all four outcomes are distinguishable with hue removed
(filled · hollow · dashed-with-a-pip · dotted); there is no red anywhere; the pair
carries no rate; the 30-dot run wraps to two rows and stays readable; and the two
offers fit on one screen, which is what prototype revision 5 deleted a duplicate
line to achieve.

**Not confirmed here:** the Hebrew render (§0.8 suspended — the strings are
authored in `values-iw/` and pass the parity guard, but nobody has seen them), and
the four **materials**. These frames are the default material only;
`MaterialRenderPass` is the surface that photographs all four, and this component
inherits `GpCard`, so it is covered by that pass rather than by this one.
