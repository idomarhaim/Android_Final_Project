package com.idomarhaim.goalpilot.domain.model

/**
 * The **ground** the app is drawn on — spec §4.1's third axis, above
 * [AppSkin] and [AppMaterial].
 *
 * ## Why this is an axis and not a two-state toggle
 *
 * The prototype
 * (`docs/prototypes/2026-08-11-visual-styles/`) offers a **toggle**: *shared
 * canvas* (all four materials on the glassmorphism ground) or *native canvas*
 * (each material on the ground it was designed for). Ido's words on
 * [#57](https://github.com/idomarhaim/Android_Final_Project/issues/57) were
 * different and wider:
 *
 * > the same backgrounds aren't there, and there's **no option to choose
 * > different combinations between the backgrounds and the blocks**.
 *
 * Two states are a toggle, not *combinations*. And in the same answer he made
 * raised-3D *"an option that can be implemented **in addition** on each of the
 * design types"* — presentation composable, rather than a second list of
 * presets.
 *
 * **The two designs were never rivals.** Both write exactly one quantity —
 * `GpMaterialSpec.backdrop`, read by `Modifier.gpPage` at its two call sites —
 * so the toggle's two states are two *values* of this enum and nothing else:
 * [MATCH] **is** *native canvas*, and [GLOW] **is** *shared canvas*, because
 * the shared canvas is the glass ground and [GLOW] is the glass ground. The
 * real question was never *which design*, it was **how wide this enum is**, and
 * the sentence above answers it.
 *
 * ## The three grounds, and why they are named for what they look like
 *
 * The prototype has three distinct grounds across its four materials — glass
 * and liquid glass each have their own, neo and dark neo share a flat one — so
 * three is the whole vocabulary, not a subset of it. They are named [GLOW],
 * [SPECTRUM] and [PLAIN] rather than `GLASS_GROUND` / `LIQUID_GROUND` /
 * `NEO_GROUND` for one reason: **the point of the axis is that a ground is no
 * longer a property of a material**, and a name that says *"glass's ground"*
 * re-asserts the ownership the axis exists to break. (`AURORA` would also have
 * collided with [AppSkin.AURORA], one axis down.)
 *
 * ## What a combination costs — stated, because two of them change what the
 * material *is*
 *
 * - **Neo on [GLOW] or [SPECTRUM] stops being neumorphism, definitionally.**
 *   Neo *is* the surface being the same colour as what is behind it — that is
 *   what makes the shadow pair read as an extrusion rather than a floating
 *   card. A gradient has no single such colour. `MaterialSpec.kt` renders the
 *   prototype's own honest answer (a translucent neutral plate carrying the
 *   shadow pair) rather than pretending, but the card now has an **edge it
 *   would not otherwise have**. That is a real difference, not a bug.
 * - **Glass or liquid glass on [PLAIN] stops being glass**, symmetrically and
 *   for the same reason one step over: a translucent panel over a flat ground
 *   is not translucent, it is a slightly lighter flat panel. It stays legible
 *   — the rim, the shadow pair and `--edge` are all still drawn — it just has
 *   nothing to be transparent *about*.
 *
 * Neither is forbidden. `#57`'s own raised-3D ruling already overruled exactly
 * this kind of objection, and [MATCH] is the default precisely so that nobody
 * meets either case without choosing it.
 *
 * ## Pure domain, like [AppSkin] and [AppMaterial]
 *
 * No label, no `Color`, no anchors. The words live in `feature/settings/` and
 * the hues in `ui/theme/MaterialSpec.kt` — the same split the two axes below
 * take, and for the same reason (`#51`, `AppSkinTest`): a label in a
 * constructor argument is unreachable by a language switch.
 */
enum class AppBackground(val id: String) {

    /**
     * The ground the selected material was designed for — the prototype's
     * *native canvas*, and the same **structure** every material had before
     * this axis existed.
     *
     * ⚠️ **Not pixel-for-pixel what shipped before, and the difference is
     * deliberate.** `#57` b also fixed two contrast defects in the same commit —
     * glass's and liquid glass's dark `tintFloor`, and the dark ground alpha —
     * so a glass or liquid install in **dark** mode renders a slightly deeper
     * panel on a slightly quieter ground than it did. Every other cell of the
     * matrix is unchanged: neo and dark neo are untouched, and every light
     * scheme is untouched. Saying *"today's look, exactly"* here would have been
     * the more comfortable sentence and it would have been false.
     *
     * Resolved through [resolve], which is the only place the material→ground
     * mapping exists.
     */
    MATCH("match"),

    /**
     * Three wide, soft lights over a deep diagonal wash — glassmorphism's own
     * ground, and the prototype's *shared canvas*.
     */
    GLOW("glow"),

    /**
     * Four tighter lights over a deeper diagonal — liquid glass's own ground.
     * Busier than [GLOW]: one more light, and all of them smaller.
     */
    SPECTRUM("spectrum"),

    /** One flat tone, no lights at all — neo and dark neo's own ground. */
    PLAIN("plain"),
    ;

    /**
     * The ground actually drawn, with [MATCH] resolved against [material].
     *
     * **The single place the material→ground mapping lives.** A second copy is
     * how the picker's preview and the page itself come to disagree, which is
     * the defect `AppMaterial.resolveDark` was given the same treatment for.
     *
     * Never returns [MATCH].
     */
    fun resolve(material: AppMaterial): AppBackground = when {
        this != MATCH -> this
        material == AppMaterial.GLASS -> GLOW
        material == AppMaterial.LIQUID_GLASS -> SPECTRUM
        else -> PLAIN
    }

    /** Whether this ground carries radial lights, once resolved for [material]. */
    fun isLit(material: AppMaterial): Boolean = resolve(material) != PLAIN

    companion object {

        /**
         * [MATCH] — so an install that never opens Settings keeps the ground its
         * material was designed for, rather than being moved onto someone
         * else's (see [MATCH] for the one way this is not byte-identical to
         * what shipped).
         *
         * That is not merely conservative: it is the only default that cannot
         * be wrong, because it is the only one that is a *function* of the
         * material rather than a fixed ground the material may not suit.
         */
        val DEFAULT: AppBackground = MATCH

        /** Tolerant lookup: unknown/absent ids fall back to [DEFAULT] rather than throwing. */
        fun fromId(id: String?): AppBackground =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
