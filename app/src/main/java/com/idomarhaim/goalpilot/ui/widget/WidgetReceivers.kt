package com.idomarhaim.goalpilot.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.idomarhaim.goalpilot.domain.model.WidgetTile

/**
 * The five tiles of the pack, and the receivers that put them in the launcher's
 * widget picker.
 *
 * **One receiver per tile**, because the picker lists `<receiver>`s. A single
 * receiver with a configuration activity would have been fewer classes and a
 * worse product: it puts a setup screen between *"I want to see my week"* and
 * seeing it, and these are things a user drags on and judges in two seconds.
 * Five entries, each with its own description, is the affordance — and it is
 * what lets the same tile be placed twice at two sizes, which the prototype's
 * mixed home screen does.
 *
 * **One `GlanceAppWidget` subclass per tile**, because
 * `GlanceAppWidgetManager` resolves placed widgets by class: if all five shared
 * one, refreshing any tile would re-render every tile as that one. The classes
 * are otherwise empty — all the behaviour is in [GoalPilotWidget], parameterised
 * by [WidgetTile].
 */
class GoalsWidget : GoalPilotWidget(WidgetTile.GOALS)

class WeekWidget : GoalPilotWidget(WidgetTile.WEEK)

class TrendWidget : GoalPilotWidget(WidgetTile.TREND)

class EffortWidget : GoalPilotWidget(WidgetTile.EFFORT)

class LevelWidget : GoalPilotWidget(WidgetTile.LEVEL)

class GoalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalsWidget()
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}

class TrendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TrendWidget()
}

class EffortWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EffortWidget()
}

class LevelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LevelWidget()
}

/** Every tile class, so a sweep cannot silently miss one that was added later. */
internal fun allWidgets(): List<GoalPilotWidget> =
    listOf(GoalsWidget(), WeekWidget(), TrendWidget(), EffortWidget(), LevelWidget())

/**
 * Redraws every placed tile now, rather than waiting for the next scheduled
 * update.
 *
 * The pack does not need this to be *correct* — each tile re-reads on its own
 * schedule and stamps what it got — but it needs it to stop being **wrong for up
 * to half an hour** at two moments:
 *
 * - **sign-out**, where the tiles would otherwise keep showing the previous
 *   account's week to whoever picks the phone up next;
 * - **a skin change**, where the picker would appear to do nothing on the home
 *   screen — which is §4.1's *a skin picker no material reads is a control that
 *   does nothing*, arriving one surface later.
 *
 * Neither call site exists yet: sign-out lives in `data/auth/` and the skin
 * picker in `feature/profile/`, which §4.9 is about to move to a Settings
 * surface (`#48`). Filed in `TODO/TODO_OPTIONAL/Presentation.TODO.optional.md`
 * rather than wired blind into screens this unit does not own.
 */
suspend fun refreshAllWidgets(context: Context) {
    allWidgets().forEach { it.updateAll(context) }
}

