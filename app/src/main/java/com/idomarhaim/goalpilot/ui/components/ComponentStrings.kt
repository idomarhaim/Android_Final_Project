package com.idomarhaim.goalpilot.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.bidiIsolated
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
