package com.idomarhaim.goalpilot.domain.model

/**
 * What a material does to the selected [AppSkin]'s palette — spec §4.1.
 *
 * > The material is a **second axis**, not the `AppSkin` the app already has.
 * > `AppSkin` is a **palette**; the material is a **surface**. They do **not**
 * > multiply freely, so each material declares a **palette transform** —
 * > `identity · mute · single-accent ramp` — and the schemes are **generated,
 * > not hand-authored**.
 *
 * ## Why this is an enum in `domain/` and not a lambda in `ui/theme/`
 *
 * The transform is a **declaration a material makes about itself**, and the
 * thing that has to be true of it is checkable without a renderer: *every*
 * material names one, and the one dark neo names is the ramp. A lambda could
 * satisfy the type and still be `{ it }` for all four, which is the defect
 * §4.1 records as having actually happened — a prototype where the skin picker
 * changed nothing in **any** material because no material read the skin. An
 * enum makes that a JVM assertion instead of a render pass.
 *
 * `ui/theme/MaterialPalettes.kt` is where each of these is *applied*; nothing
 * about the arithmetic lives here, because `domain/` may not see
 * `androidx.compose`.
 */
enum class PaletteTransform {

    /**
     * The skin's own hues, at full strength — glassmorphism and liquid glass.
     *
     * Both draw their depth out of **translucency**, so the palette shows
     * through rather than being restated by the surface: transforming it would
     * be transforming it twice.
     */
    IDENTITY,

    /**
     * One desaturated accent on a warmed, flattened ground — neo.
     *
     * Neumorphism's whole claim is that depth comes from a **shadow pair on one
     * flat surface**. A palette with tonal steps in it contradicts that: the
     * step draws the boundary the shadow was supposed to draw, and the surface
     * stops reading as extruded. So the ground collapses to a single tone and
     * the accents lose saturation to sit on it — which is also why neo is the
     * material with the known WCAG failure, and why `--edge` is not optional.
     */
    MUTE,

    /**
     * One two-stop ramp built from the skin, on charcoal — dark neo.
     *
     * §4.1's first named consequence, and it is the one that gets found late:
     *
     * > **Dark neo's accent must derive from the selected skin**, or picking
     * > Blossom under dark neo silently renders Aurora and the skin picker
     * > stops working for a quarter of the set.
     *
     * The ramp is therefore **derived** from the skin's own hero gradient
     * rather than hand-picked per skin — a third skin then gets a dark-neo
     * ramp by existing, instead of by somebody remembering to add one.
     */
    SINGLE_ACCENT_RAMP,
}
