package com.idomarhaim.goalpilot.ui.widget

import com.idomarhaim.goalpilot.data.widget.WidgetSnapshotSource
import com.idomarhaim.goalpilot.data.widget.WidgetSnapshotStore
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildWidgetTileUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * How the widget pack reaches the object graph.
 *
 * An entry point rather than `@AndroidEntryPoint` + constructor injection,
 * because the objects that need the graph are not created by us: the **system**
 * instantiates `GoalPilotWidgetReceiver`, which constructs its `GlanceAppWidget`
 * as a property. There is nowhere in that chain to inject into, so the widget
 * asks the graph for what it needs at the one moment it has a `Context`.
 *
 * Deliberately narrow — four things, each one already a singleton. A widget that
 * could reach any repository would grow the habit of reading one directly, and
 * the seam that keeps `Goal`'s shape out of twenty layouts
 * (`BuildWidgetSnapshotUseCase`) only holds while there is no second way in.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {

    fun snapshotSource(): WidgetSnapshotSource

    fun snapshotStore(): WidgetSnapshotStore

    fun buildTile(): BuildWidgetTileUseCase

    /**
     * For the selected `AppSkin`. §4.1's enforcement note is the reason this is
     * here at all: *a skin picker which no material reads is a control that does
     * nothing, and it looks correct in source.*
     */
    fun preferences(): AppPreferencesRepository
}
