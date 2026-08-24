package com.idomarhaim.goalpilot.domain.model

/**
 * The **surface** the whole app is drawn out of — spec §4.1's four-material
 * contract, the second axis above [AppSkin].
 *
 * > Four materials ship as a user-selectable skin — glassmorphism · liquid
 * > glass · neo · dark neo. **Metal is deleted.**
 *
 * ## The contract, and why it is the expensive half
 *
 * > **No screen may depend on a property a single material has.** A screen
 * > specifies `surface · groove · elevation · accent`, and each material
 * > answers those four its own way — otherwise every design has to be drawn
 * > four times, which is the cost that would make four materials unaffordable.
 *
 * Those four answers live in `ui/theme/MaterialSpec.kt` as `GpMaterialSpec`,
 * read through `LocalGpMaterial`. This enum is the **declaration** half:
 * the persisted [id], the [paletteTransform] the material applies to the skin,
 * and [lockedDark] — and every one of those three is assertable on the JVM,
 * which is the point of keeping them here.
 *
 * ## Why a preference at all
 *
 * §4.1's discriminator, and it is worth restating because it is the reason this
 * is not scope creep: **a layout a user dislikes is evidence the layout is
 * wrong** — storing their workaround preserves the defect and hides it — while
 * **a material a user dislikes is evidence of nothing.** So a preference store
 * belongs exactly where there is nothing to be right about.
 *
 * ## Pure domain, like [AppSkin]
 *
 * Carries no label, no tagline and no `Color`. The words the picker shows are
 * `ui/components/`'s and the hues are `ui/theme/`'s — the same split [AppSkin]
 * takes, and for the same reason: a label in a constructor argument is
 * unreachable by a language switch (`#51`, `AppSkinTest`).
 */
enum class AppMaterial(
    val id: String,
    val paletteTransform: PaletteTransform,
    /**
     * `null` when the material renders in whichever brightness is asked for.
     * Non-null when it has **only one** scheme, and the value is that scheme.
     *
     * §4.1's second named consequence:
     *
     * > **The product is ragged, not rectangular.** Dark neo has **no light
     * > scheme**, so a material must be able to declare itself
     * > **brightness-locked**, and the picker must **say so** rather than
     * > letting the light switch quietly do nothing.
     *
     * `Boolean?` rather than a `isBrightnessLocked: Boolean` flag so the shape
     * generalises to a light-only material without a rewrite — and so
     * [resolveDark] can be one expression with no branch on which material it
     * is.
     */
    val lockedDark: Boolean?,
) {

    /** Depth from **blur** — the canvas stays legible through the panel. */
    GLASS(id = "glass", paletteTransform = PaletteTransform.IDENTITY, lockedDark = null),

    /** Depth from **refraction at the edge** — bright inner rim, dim counter-rim, one streak. */
    LIQUID_GLASS(id = "liquid", paletteTransform = PaletteTransform.IDENTITY, lockedDark = null),

    /** Depth from **a shadow pair** on one flat surface. Inset track, extruded arc, no gloss. */
    NEO(id = "neo", paletteTransform = PaletteTransform.MUTE, lockedDark = null),

    /** Depth from **a deep shadow pair plus one saturated gradient**. Dark only. */
    DARK_NEO(id = "darkneo", paletteTransform = PaletteTransform.SINGLE_ACCENT_RAMP, lockedDark = true),
    ;

    /** Whether the brightness control can move this material at all. */
    val isBrightnessLocked: Boolean get() = lockedDark != null

    /**
     * The brightness this material actually renders in.
     *
     * The **single** place the lock is applied, so the picker's caption, the
     * theme and the tile previews cannot drift apart — a lock enforced in the
     * theme and described in the picker is two answers to one question, which
     * is §0.3's defect installed in the screen built to prevent it.
     *
     * @param requestedDark what [AppBrightness] resolved to for this device.
     */
    fun resolveDark(requestedDark: Boolean): Boolean = lockedDark ?: requestedDark

    companion object {

        /**
         * **Glassmorphism** — changed from [NEO] on 2026-08-24 by `visual-parity`,
         * because §4.9's stated reason for neo is **false about the code that
         * actually shipped**, and the cost of it was the whole of Ido's report.
         *
         * §4.9's defaults table says neo is
         *
         * > the only material with **both** a light and a dark scheme **and** no
         * > blur under it. Glass and liquid glass are `Modifier.blur` /
         * > `RenderEffect` — API 31+, with a fallback below that changes the
         * > look — and §4.1 already states that **a widget has none of those
         * > primitives**, so neo's hand-drawn shadow pair is the only one a
         * > widget can approximate. Dark neo is brightness-locked, so it cannot
         * > be a default at all.
         *
         * **The first clause describes a port that was never built.** `#57` b
         * deliberately did *not* use either primitive: `GpMaterialSpec`'s own
         * header records that "Compose has no backdrop filter, and
         * `Modifier.blur` blurs a composable's *own* content", so glass and
         * liquid glass are **translucent panels over a gradient backdrop** —
         * `MaterialSpec.kt`'s `backdrop`, drawn by `Modifier.gpPage`. Verified
         * mechanically 2026-08-24: `Modifier.blur`, `RenderEffect` and
         * `BlurMaskFilter` have **zero** call sites in `app/src/main`, and every
         * `SDK_INT` gate in the app is in `notifications/`. So glass renders
         * **identically on `minSdk` 26 and on 35**, and there is no fallback to
         * change the look.
         *
         * **The third clause is still true and is deliberately not being
         * honoured here.** A widget genuinely cannot blur — but it never had to,
         * since neither does the app, and a Glance panel can be translucent. The
         * app's default and the widget's renderer do not have to agree, and the
         * prototype's own widgets (`docs/prototypes/2026-08-10-charts-presentation`,
         * widgets view) are translucent panels over the launcher wallpaper.
         *
         * ## What this one line was costing
         *
         * With `DEFAULT = NEO`, [AppBackground.MATCH] — itself the default —
         * resolves neo to [AppBackground.PLAIN], *"one flat tone, no lights at
         * all"*, and `AppRelief.FLAT` is the third default. So a fresh install
         * was **opaque, unlit and flat by construction**, and every one of the
         * four presentation features `#57` shipped was off until the user went
         * looking for a picker they did not know existed. Ido reported the app
         * as not looking like the prototypes on 2026-08-21 and **again on
         * 2026-08-24**, after all four had shipped.
         *
         * ⚠️ **This contradicts a committed spec table, so it is a decision and
         * not a fix.** Taken by the session, recorded as the session's, and
         * Ido's to overturn in one word — the picker still offers all four, so
         * overturning it costs one line and no feature. If it stands, §4.9's
         * table and its justification both need correcting; that edit is **not**
         * made here because `docs/PRODUCT_v0.3.md` is held by other sessions.
         */
        val DEFAULT: AppMaterial = GLASS

        /** Tolerant lookup: unknown/absent ids fall back to [DEFAULT] rather than throwing. */
        fun fromId(id: String?): AppMaterial =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
