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

## 3 · What `/implement #10` left open (widget pack, 2026-08-15)

`#10` shipped **five of §4.5's seven tiles** — `goals · week · trend · effort · level`, each at
`2×2` / `4×2` / `2×4` / `4×4`. Everything below is what that build could not close, written here
so it is not re-derived from the code.

- [ ] **`decisions` and `today` are not built, and the reason is data rather than design.** Both
  are schedule surfaces and §2 scheduling does not exist: `Task` carries **no due time at all**,
  and §7.2 already records that
  [`GoogleTasksClient.kt:145`](../../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt)
  parses Google's `due` into a field no other line in the repo reads. A *"4 decisions waiting"*
  tile computed from nothing would be §0.3's *second number that quietly disagrees* manufactured
  on purpose, so the two were **left out rather than faked**. They become nearly free once §2
  lands — the tile builder, the neo surface, the size rule and the string table are all already
  there, and each is one `when` branch in
  [`BuildWidgetTileUseCase`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildWidgetTileUseCase.kt)
  plus one receiver.
- [ ] **`refreshAllWidgets()` has no call site, and two are owed.** It exists in
  [`ui/widget/WidgetReceivers.kt`](../../app/src/main/java/com/idomarhaim/goalpilot/ui/widget/WidgetReceivers.kt).
  The pack is *correct* without it — every tile re-reads on its own schedule and stamps what it
  got — but it stays **wrong for up to half an hour** at two moments: after **sign-out**, where
  the tiles keep showing the previous account's week to whoever picks the phone up next; and
  after a **skin change**, where the picker appears to do nothing on the home screen, which is
  §4.1's *a skin picker no material reads is a control that does nothing* arriving one surface
  later. Both call sites are in files `#10` did not own — sign-out in `data/auth/`, the picker in
  `feature/profile/`, which §4.9 is about to move to Settings
  ([#48](https://github.com/idomarhaim/Android_Final_Project/issues/48)) — so wiring them belongs
  to whoever touches those next, not to a blind edit.
- [ ] **A widget tap opens the app, not yet a destination.** §4.5's contract is *a tap can only
  open the app at a destination*. The tile already puts one in the intent
  (`WidgetDestination.INTENT_EXTRA`); nothing reads it yet, because routes live in
  `ui/navigation/Destinations.kt`. One `when` in `MainActivity` closes it, and until then every
  tile still opens the app — the half of the contract that must never fail.
- [ ] **The pack ships neo, and that is the ticket's own decision, on the record.** Derived from
  §4.9, which defaults the material to neo *partly for this ticket*: the only material with both
  a light and a dark scheme **and** no blur under it. Glass and liquid glass are *made of* blur
  and refraction and have no `RemoteViews` form at all; dark neo is brightness-locked and a home
  screen must follow the device's own switch. If the material picker later offers a per-surface
  choice, the widget is the surface where three of the four cannot be honoured — say so in the
  picker rather than letting it silently do nothing.
- [ ] **The widget picker shows the app icon five times.** `previewImage="@mipmap/ic_launcher"` on
  all five `res/xml/widget_*_info.xml`, so the one place a user sees a tile *before* placing it
  shows five indistinguishable blue targets. Found by the 2026-08-16 device pass; the adversarial
  pass had rated it 🔵, which was too low — the picker is the whole first impression of a pack
  whose point is visual. Fix is `previewLayout` per tile (API 31+), which needs five small static
  layouts approximating each card. Deliberately not bundled into the defect fixes, because unlike
  those four it is cosmetic and confined to the picker.
- [ ] **Hebrew is written but not seen.** `values-he/widget_strings.xml` ships and the bidi
  isolation is in code
  ([`core/util/Bidi.kt`](../../app/src/main/java/com/idomarhaim/goalpilot/core/util/Bidi.kt)),
  but §0.8 asks for a screen **seen** in Hebrew and the app has no language picker until §5.1.
  Render the five tiles on an RTL device once
  [#51](https://github.com/idomarhaim/Android_Final_Project/issues/51) lands — in particular the
  trend tile, whose columns are a **bitmap** and are reversed in code rather than by the layout.
- [ ] **`core/util/Bidi.kt` should collapse into `ui/components/BidiText.kt`.** `#2` is adding the
  Compose-side helper for the same §4.8 defect class. Mine is the string half and has no Android
  dependency, which is what let it be tested on the JVM; when both exist, the Compose one should
  call it rather than repeat it.
