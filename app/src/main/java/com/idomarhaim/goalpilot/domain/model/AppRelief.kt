package com.idomarhaim.goalpilot.domain.model

/**
 * Whether a chart's bodies are **extruded** — spec §4.1's raised-3D toggle,
 * promoted to the fourth appearance axis by `#57` c.
 *
 * ## Why this is an axis and not a property of the soft materials
 *
 * ⛔ **A recorded decision was overturned here, and it is kept in full because
 * somebody will re-derive it.** `TODO/TODO_OPTIONAL/Presentation.TODO.optional.md`
 * used to read:
 *
 * > raised is a **property of the two soft-UI materials** (it is a no-op on
 * > glass and liquid, where height would contradict what the material is), so
 * > it is **not** a separate user setting — a global toggle that does nothing
 * > on half the materials is one control carrying two axes.
 *
 * That is a real design argument. **Ido overruled it as a product call on
 * 2026-08-21**, asked directly:
 *
 * > *"neo, dark-neo both. 3d graphs is an option that can be implemented **in
 * > addition on each of the design types** (not only the two mentioned)."*
 *
 * So [RAISED] is available on all four materials, glass and liquid glass
 * included, and it is **not** a no-op there: [ui.theme.GpChartVolume] gives each
 * material its own extrusion tones, exactly as each answers `surface` its own
 * way. The old objection would only have held if raised meant *one* fixed
 * treatment; it does not.
 *
 * ## Its whole reach is the chart primitives, and that is deliberate
 *
 * A card is not extruded by this setting — the four materials already answer
 * *"how deep is a panel?"* through `GpMaterialSpec.shadow`, and a second control
 * over the same question is §0.3's two-answers defect. What this axis moves is
 * the **bodies a chart draws**: the donut's wedges, a ring's arc, a column, a
 * bar. Ido's word for it was *"3d graphs"*.
 *
 * ## Pure domain, like [AppSkin], [AppMaterial] and [AppBackground]
 *
 * No label, no `Color`, no heights. The words live in `feature/settings/` and
 * the tones in `ui/theme/ChartVolume.kt` — the same split the three axes below
 * take, and for the same reason (`#51`, `AppSkinTest`): a label in a constructor
 * argument is unreachable by a language switch.
 *
 * ## An enum rather than a `Boolean`, for two reasons
 *
 * A stored `Boolean` has no tolerant read: `getBoolean` cannot tell *absent*
 * from *false*, so a key renamed later silently reads as **flat** rather than as
 * *the default*, which is the bug [fromId] exists to prevent on the three axes
 * above. And the picker that shows this axis renders a **preview per option**
 * (a body is judged by looking at it, the same argument `BackgroundPicker`
 * makes), which wants `entries` to iterate.
 */
enum class AppRelief(val id: String) {

    /**
     * Bodies are painted, not built: a stroked arc, a filled column.
     *
     * They still carry **volume** — the three-stop fill, the sheen along the lit
     * edge, the cast shadow and the grain — because volume is not this axis.
     * `#57` c's finding, stated where it will be looked for: a flat chart in
     * this app is *flat*, not *plain*.
     */
    FLAT("flat"),

    /**
     * Bodies are solids: a closed annular sector with real side walls and end
     * caps, lit from one direction for the whole chart.
     *
     * The geometry is the 2026-08-12 rebuild of
     * `docs/prototypes/2026-08-11-visual-styles/`, whose own comment says what
     * the first attempt got wrong — *"a pack of cards, wider than the channel,
     * cutting its walls"* — and why: N translated copies instead of **one
     * body**.
     */
    RAISED("raised"),
    ;

    /** Whether bodies are extruded. One reader: `materialSpecFor`. */
    val isRaised: Boolean get() = this == RAISED

    companion object {

        /**
         * [FLAT].
         *
         * Not a taste call: raised bodies are **wider** than flat ones at the
         * same channel (the extrusion comes out of the width budget, not on top
         * of it — see `ChartVolume.kt`), so a donut that has always fitted its
         * hole would change shape on every install at once. The user opts in.
         */
        val DEFAULT: AppRelief = FLAT

        /** Tolerant lookup: unknown/absent ids fall back to [DEFAULT] rather than throwing. */
        fun fromId(id: String?): AppRelief =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
