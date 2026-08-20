package com.idomarhaim.goalpilot.feature.goals

import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.MeasureKind

/**
 * Display text for §1.3's two closed lists — the seven measure kinds and the four
 * input modes.
 *
 * **Why it is here and not on the enums.** A language switch cannot reach an enum
 * constructor argument (`kb/dev/untranslatable-idioms.md` §1) — the defect
 * `GoalCategory.label` is deprecated for. The kind is **app logic**, so its seven
 * labels *are* translated when #51 resumes; the goal's own **word** is user
 * content and never is (§1.3, §5.1 `C15b`). Keeping the labels in one file in
 * `feature/goals` is what makes that sweep a single edit.
 *
 * **Plain English literals are legal here.** `feature/goals` is not in
 * `AnalyticsLiteralSweepTest.SWEPT_PACKAGES`, and `AGENTS.md` §0.8's suspension
 * block permits them in an unswept package — adding this package to the sweep as
 * a favour is what it explicitly forbids.
 */

/** The seven kinds, in words. Translated when #51 resumes; English until then. */
fun MeasureKind.label(): String = when (this) {
    MeasureKind.COUNT -> "Count"
    MeasureKind.DURATION -> "Duration"
    MeasureKind.DISTANCE -> "Distance"
    MeasureKind.VOLUME -> "Volume"
    MeasureKind.MASS -> "Weight"
    MeasureKind.MONEY -> "Money"
    MeasureKind.PERCENT -> "Percent"
}

/** An example word for each kind, to show what the free field is *for*. */
fun MeasureKind.wordHint(): String = when (this) {
    MeasureKind.COUNT -> "books"
    MeasureKind.DURATION -> "hours"
    MeasureKind.DISTANCE -> "km"
    MeasureKind.VOLUME -> "L"
    MeasureKind.MASS -> "kg"
    MeasureKind.MONEY -> "₪"
    MeasureKind.PERCENT -> "%"
}

/** The offered input modes, in words. */
fun InputMode.label(): String = when (this) {
    InputMode.BUTTONS -> "Fill buttons"
    InputMode.NUMBER -> "Type a number"
    InputMode.TICK -> "Tick"
    InputMode.AUTO -> "Synced"
}

/** One line saying what picking this mode changes, for under the picker. */
fun InputMode.explanation(): String = when (this) {
    InputMode.BUTTONS -> "Tap a button as many times as you like — each tap logs that amount."
    InputMode.NUMBER -> "Open a dialog and type the amount."
    InputMode.TICK -> "Done or not done."
    InputMode.AUTO -> "Filled in automatically from Health Connect."
}
