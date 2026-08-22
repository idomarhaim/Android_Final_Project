package com.idomarhaim.goalpilot.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory

/**
 * The presentation half of the two domain enums this package renders — issue
 * #51's literal sweep for `ui/components/`.
 *
 * Modelled on `iconForKey` and on `feature/analytics`'s `AnalyticsStrings.kt`:
 * the identity lives in `domain/`, the words live in `res/`, and neither knows
 * about the other. Before this sweep the words were **constructor arguments on
 * the enums**, which is the fourth idiom in
 * `kb/dev/untranslatable-idioms.md` — *a language switch cannot reach a
 * constructor argument*, so `AppSkin.BLOSSOM.tagline` stayed English on a
 * Hebrew device while everything around it mirrored correctly.
 *
 * ### Why the two enums are treated differently
 *
 * [AppSkin]'s copy is **gone from the enum**; [GoalCategory]'s is **still
 * there**. The discriminator is not the idiom — it is identical in both — but
 * **who else reads it**:
 *
 * - `AppSkin.label`/`tagline` had exactly one production consumer, [SkinPicker]
 *   in this package, so removing them cost two test edits and nothing else.
 * - `GoalCategory.label` has three more, in `feature/dashboard` and
 *   `feature/goals`, and **those packages are unswept**. Deleting the property
 *   would drag two feature packages into this unit half-swept, which is worse
 *   than leaving one deprecated property behind a pointer.
 *
 * So [localizedLabel] is the replacement and `GoalCategory.label` is marked as
 * superseded where it is declared. The Hebrew is authored once either way,
 * which is the part that mattered.
 */

/**
 * The category's name in the app's language.
 *
 * Named `localizedLabel` rather than `label` on purpose: `GoalCategory.label`
 * still exists for the three unswept call sites, and two members one character
 * apart — one language-aware, one not — is exactly how the wrong one gets
 * picked. The name says which is which.
 */
@Composable
@ReadOnlyComposable
fun GoalCategory.localizedLabel(): String = stringResource(labelRes)

/** The skin's name, e.g. *Aurora* / *זוהר*. */
@Composable
@ReadOnlyComposable
fun AppSkin.label(): String = stringResource(labelRes)

/** The one-line description under the skin's swatch. */
@Composable
@ReadOnlyComposable
fun AppSkin.tagline(): String = stringResource(taglineRes)

/** The material's name, e.g. *Soft* — spec §4.1's four surfaces. */
@Composable
@ReadOnlyComposable
fun AppMaterial.label(): String = stringResource(labelRes)

/** The one-line description under the material's tile. */
@Composable
@ReadOnlyComposable
fun AppMaterial.tagline(): String = stringResource(taglineRes)

/**
 * The design of record's own name for this material — *Neo*, *Dark neo* — framed
 * so it reads as a designation rather than as a second label.
 *
 * ### Why this exists at all (`#53`, 2026-08-21 comment)
 *
 * §4.1 calls the four materials *glassmorphism · liquid glass · neo · dark neo*.
 * The picker calls two of them **Soft** and **Soft dark**, and the word "neo"
 * appeared **nowhere in the UI**. So the two vocabularies existed with nothing
 * linking them: a user who had read the spec could not find the control, and a
 * session receiving *"no dark blue neo"* could not match the report to a tile.
 * Both halves actually happened on that ticket.
 *
 * The rejected fix was renaming the tiles to the spec's words. *Soft* / *Soft
 * dark* are the better **user-facing** words, and the failure was never that the
 * wrong vocabulary won — it was that neither reached the other.
 *
 * ### Why the name is `translatable="false"` and the frame is not
 *
 * The name's job is to be the **same token** as the design of record, which is
 * written in English. A Hebrew rendering would name the control after a word
 * that appears in no document — the failure this fixes, translated, rather than
 * a translation of the fix. So `res/values/` carries the four names once, marked
 * untranslatable, and only the frame (`Spec: %1$s` / `במפרט: %1$s`) is authored
 * per language.
 *
 * That is the opposite call from [AppSkin.label], and the discriminator is what
 * the word is **for**: *Aurora* and *Blossom* are evocative product words whose
 * job is to read well, so leaving them Latin would put a bare Latin run in an
 * otherwise Hebrew list for no gain (§4.8). A designation has no such gain to
 * forfeit.
 *
 * The isolate is the price of that decision: a Latin run inside a Hebrew
 * paragraph is re-ordered by the bidi algorithm exactly as `5/10` is, so the
 * name is wrapped the same way `GoalCard` wraps a user-authored measure unit
 * whose script it cannot know.
 */
@Composable
@ReadOnlyComposable
fun AppMaterial.specName(): String = stringResource(
    R.string.components_material_spec_name,
    stringResource(specNameRes).bidiIsolated(),
)

/**
 * A percentage, isolated.
 *
 * The `%` sign's placement is the translator's — it is a resource, not a Kotlin
 * `"$n%"` — and the whole thing is one isolate so the sign cannot migrate to
 * the far side of the digits in an RTL paragraph.
 *
 * A near-duplicate of `feature/analytics`'s `percentText`, and deliberately so:
 * the one-file-per-package convention that lets two sessions sweep two packages
 * without contending on one resource file necessarily duplicates the handful of
 * format-only strings. Converging them would mean a shared file, which is the
 * contention this convention exists to avoid.
 */
@Composable
@ReadOnlyComposable
fun percentText(percent: Int): String =
    stringResource(R.string.components_percent, percent).bidiIsolated()

@get:StringRes
private val GoalCategory.labelRes: Int
    get() = when (this) {
        GoalCategory.HEALTH -> R.string.components_category_health
        GoalCategory.FITNESS -> R.string.components_category_fitness
        GoalCategory.SLEEP -> R.string.components_category_sleep
        GoalCategory.NUTRITION -> R.string.components_category_nutrition
        GoalCategory.RELATIONSHIPS -> R.string.components_category_relationships
        GoalCategory.CAREER -> R.string.components_category_career
        GoalCategory.PROJECTS -> R.string.components_category_projects
        GoalCategory.LEARNING -> R.string.components_category_learning
        GoalCategory.FINANCE -> R.string.components_category_finance
        GoalCategory.OTHER -> R.string.components_category_other
    }

@get:StringRes
private val AppSkin.labelRes: Int
    get() = when (this) {
        AppSkin.AURORA -> R.string.components_skin_aurora
        AppSkin.BLOSSOM -> R.string.components_skin_blossom
    }

@get:StringRes
private val AppSkin.taglineRes: Int
    get() = when (this) {
        AppSkin.AURORA -> R.string.components_skin_aurora_tagline
        AppSkin.BLOSSOM -> R.string.components_skin_blossom_tagline
    }

@get:StringRes
private val AppMaterial.labelRes: Int
    get() = when (this) {
        AppMaterial.GLASS -> R.string.components_material_glass
        AppMaterial.LIQUID_GLASS -> R.string.components_material_liquid
        AppMaterial.NEO -> R.string.components_material_neo
        AppMaterial.DARK_NEO -> R.string.components_material_darkneo
    }

@get:StringRes
private val AppMaterial.specNameRes: Int
    get() = when (this) {
        AppMaterial.GLASS -> R.string.components_material_glass_spec
        AppMaterial.LIQUID_GLASS -> R.string.components_material_liquid_spec
        AppMaterial.NEO -> R.string.components_material_neo_spec
        AppMaterial.DARK_NEO -> R.string.components_material_darkneo_spec
    }

@get:StringRes
private val AppMaterial.taglineRes: Int
    get() = when (this) {
        AppMaterial.GLASS -> R.string.components_material_glass_tagline
        AppMaterial.LIQUID_GLASS -> R.string.components_material_liquid_tagline
        AppMaterial.NEO -> R.string.components_material_neo_tagline
        AppMaterial.DARK_NEO -> R.string.components_material_darkneo_tagline
    }
