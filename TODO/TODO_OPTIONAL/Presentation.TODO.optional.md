# 🎨 Presentation — open items after `C12`

Area file for the visual language decided on
[#31 · `C12`](https://github.com/idomarhaim/Android_Final_Project/issues/31) (map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)), resolved 2026-08-12.

**What was decided, so nothing here re-opens it:** all four materials —
**glassmorphism · liquid glass · neo · dark neo** — ship as a **user-selectable material**,
**metal is deleted**, and the prototype keeps its **raised-3D** and **empty-channel** toggles.
The resolution is the ticket; this file is only what that decision left open.

**The artifact:** [`docs/prototypes/2026-08-11-visual-styles/`](../../docs/prototypes/2026-08-11-visual-styles/)
— standalone HTML, no build. Render it with
[`docs/prototypes/tools/shoot.ps1`](../../docs/prototypes/tools/shoot.ps1) and **look at the
image**; every item below marked *(found by render)* was invisible in the source and was found
that way rather than reported.

Status legend is [`TODO.md`](../TODO.md)'s: `[ ]` todo · `[~]` in-progress · `[x]` done ·
`[-]` deferred.

---

## 1 · Prototype refinements still open

These are defects and unanswered questions in the **prototype**, not in shipped app code. They
are listed first because the prototype is what a build session will implement from, so a defect
left here is a defect that gets built.

- [ ] **The donut's centre caption overruns its hole in Hebrew, and collides with the labels**
  *(found by render, 2026-08-12)*. `of a 26h week` is 13 characters; `מתוך שבוע של 26 ש׳` is
  wider than the hole at the phone's `donut(310, 50, 18, …)` geometry, so it runs out of the ring
  and lands on top of the left-hand label column (`ללא שיוך 3%`). Correct in English at the same
  size, which is why nothing caught it: this is exactly the standing rule *a design is not
  finished until it has been seen in Hebrew*. The fix is a budget, not a shorter string —
  the caption has to be measured against the hole diameter the way `clampLabel` already measures
  the label against the card.
- [ ] **The slice percentage reorders under bidi and needs direction isolation**
  *(found by render, 2026-08-12)*. The label is `<name> <pct>%`, and the `%` run is neutral, so
  in an RTL paragraph the bidi algorithm puts the number at the **visual start** — `27% לימודים`
  where English renders `Studies 27%`. It reads as the percentage belonging to the *previous*
  label. This is `C9b`'s finding 3 (`09:00–12:00` rendering as `12:00–09:00`) in a new place:
  **any latin/numeric run inside a Hebrew string owes isolation**. The file already has an
  `.ltr` class (`unicode-bidi:isolate`) for HTML; the `<tspan>` carrying the percentage inside
  SVG `<text>` does not use it. **Carries to Compose**, where the same string is built the same
  way — so this is a spec line for the build session, not only a prototype fix.
- [ ] **Whether the raised-3D arc ships, and for which materials.** Ido asked for the toggle to
  be **kept**, which keeps the instrument rather than settling the question. What is decided:
  raised is a **property of the two soft-UI materials** (it is a no-op on glass and liquid,
  where height would contradict what the material is), so it is *not* a separate user setting —
  a global toggle that does nothing on half the materials is one control carrying two axes. What
  is open: whether `neo` and `dark neo` ship raised, flat, or as two entries in the picker.
  Twelve revisions of geometry are behind it (`CHANGELOG/2026-08-10/c12-charts-presentation.md`,
  revisions 9–12); it needs one look, not more building.
- [ ] **`Relationships` truncates at 310 dp.** Stated at revision 12 as a **width fact, not a
  rendering defect** — `clampLabel` is doing its job. Left open because the honest fixes are
  product choices: a shorter default life-area name, or a two-column label layout at that width.
- [ ] **Dark neo has no light scheme, and the light toggle keeps it dark on purpose.** Picking it
  is also picking *GoalPilot is dark-only*, or that light mode is a **different material**.
  Recorded on the ticket before the choice was made; still unanswered, and it becomes real the
  first time a user on dark neo switches to light.
- [ ] **Neo's affordances are shadow-only, which is a contrast failure waiting to happen.** A
  button that is only an extrusion has no contrast anchor, so it can fail WCAG outright. Needs a
  real anchor (border, fill or text contrast) specified **per material**, not per component.

## 2 · The bill the material decision sends to the build session

Nothing here is a defect; it is what shipping four materials costs, named on the ticket so it is
not discovered during implementation.

- [ ] **`AppSkin` and the material are two different axes, and the code has a name collision.**
  [`domain/model/AppSkin.kt`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/AppSkin.kt)
  already ships as a user-selectable *colour* skin (`AURORA`, `BLOSSOM`), persisted device-local
  by `AppPreferencesRepository` and chosen in `ui/components/SkinPicker.kt`. The new axis is
  *surface treatment*. They must not be jammed into one enum, **and they do not multiply
  freely** — see the ticket's resolution for the composition rule (dark neo's single accent and
  neo's 30 % hue mute both reach into the palette).
- [ ] **`ThemePaletteTest`'s matrix grows, and it is already the guard on this palette.** It
  asserts WCAG contrast across `skin × brightness` today. The material is a third dimension that
  changes the surface a colour is read against, and dark neo does not have a light scheme at all
  — so the product is **ragged, not rectangular**. Decide the matrix before writing it.
- [ ] **`GoalCategory.defaultColorHex` is still owed the harmonised set from C12 rev 4.** The
  prototype replaced seven full-chroma primaries with a set built *as a set*; those are committed
  values, so the update lands with a `ThemePaletteTest` change or not at all.
- [ ] **Every material's depth primitive is a web primitive.** `backdrop-filter`, SVG filters and
  CSS shadow pairs have no direct Compose equivalent: glass and liquid need `Modifier.blur` /
  `RenderEffect` (**API 31+**, with a fallback below), and the soft-UI groove and raised arc are
  hand-drawn `Canvas` work. Liquid glass is the most expensive of the four.
- [ ] **A widget has none of them.** [#10](https://github.com/idomarhaim/Android_Final_Project/issues/10)
  (`U5`, the widget pack) is unblocked by this ticket, and Glance renders into a `RemoteViews`
  bitmap: no blur, no filters, no animation (`ChartAnimation.kt` does not run there), and the
  launcher — not the app — decides the real dp. Each material owes a tile version that survives
  that, or the widget pack picks one material and says so.
