# KB candidates — `c22-measure-proposal`, 2026-08-15

Repo: `C:\Dev\Android_Final_Project` · session `c22-measure-proposal` · working
[`C22` #44](https://github.com/idomarhaim/Android_Final_Project/issues/44) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12).

Each entry stands alone. Another session's chat history is not a source.

---

## 1 · A material defined as "the page colour plus shadows" has no opaque layer, and every overlay built on it leaks

- **Status:** 🟡 not drained — destination is the **central KB**, so draining it is a cross-repo
  `C:\Dev\JARVIS` visit that owes a row on that board. Not blocked, just not this unit.
- **Claim.** When a design system ships several *materials* and one of them expresses surface through
  **shadow against the page colour** rather than through its own fill (neumorphism is the archetype),
  every component that has *something behind it* — a bottom sheet, a dialog, a dropdown, anything over
  a scrim — must **declare its own opacity explicitly**. Inheriting the material's surface rule gives
  a transparent overlay, and the content behind reads straight through it.
- **Why, and what was rejected.** The tempting reading is *"a scrim bug — darken it more"*, and that
  is wrong: the scrim was working. The overlay itself had no fill. It was also invisible in three of
  the four materials — glass and liquid glass blur whatever is behind them, so the leak looks like
  the intended effect, and dark neo happened to carry a gradient. So the defect is **material-specific
  and hides in the majority case**, which is why it survives review and only a render in the one
  material that lacks a fill exposes it. Generalises past this project: any theming layer with a
  "surface = elevation, not fill" material has the same hole.
- **Destination:** `kb/dev/` — a new page, or a section on an existing design-system page if one
  covers material/theming contracts.
- **Anchors:** `docs/prototypes/2026-08-15-measure-proposal/index.html` (`.st-neo .sheet`,
  `--sheet`); the same file's `.scrimwrap`. Observed 2026-08-15 in light neo; not reproducible in
  glass or liquid glass.
- **Supersedes:** nothing.

---

## 2 · A prototype frame that *explains* an empty state cannot test whether the empty state reads as deliberate

- **Status:** 🟡 not drained — same cross-repo destination as entry 1.
- **Claim.** When the design decision under test is *"the app should say nothing here"*, a frame that
  renders a card reading *"nothing is shown here"* proves nothing at all. The only instrument that
  answers the question is the **real screen with nothing on it**, because what is being judged is
  whether emptiness reads as *deliberate* or as *broken* — and a caption explaining the emptiness
  removes exactly the ambiguity being measured.
- **Why, and what was rejected.** Rev 1 drew the no-model cases as two explanatory cards on a page
  titled *When there is no model*. It was cheaper and it read well, which is the trap. Replacing them
  with two real goal screens immediately surfaced a second finding that the card version could not
  have: an empty **section header** with nothing under it reads as a *loading failure*, so the silent
  screen needed a quiet positive statement (`Nothing scheduled`) rather than an absence. That defect
  exists only on the real screen.
- **Destination:** `kb/dev/` — prototyping practice; sits beside the existing *render and look between
  revisions* finding from `C12`.
- **Anchors:** `docs/prototypes/2026-08-15-measure-proposal/README.md` (frames 5 and 6, round 6);
  `docs/prototypes/tools/README.md` for the parent rule.
- **Supersedes:** nothing.
