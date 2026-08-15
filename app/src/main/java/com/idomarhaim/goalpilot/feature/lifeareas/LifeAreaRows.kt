package com.idomarhaim.goalpilot.feature.lifeareas

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.toGoalAccent

/**
 * Drag-to-reorder for the life-areas list.
 *
 * It is its own file, and its rows go into the screen's existing `LazyColumn` as a
 * [LazyListScope] extension rather than a nested list, for two reasons: a second
 * scrollable inside the screen would fight the first, and a state holder plus a
 * pure-Compose row function is the only shape in which the drag arithmetic can be
 * driven by `LifeAreaReorderUiTest` without Firebase or Hilt in the room.
 *
 * The list follows the finger locally and only writes on drop — see
 * [LifeAreaReorderState.onDragEnd] — because a write per crossed neighbour would
 * be a batch per frame.
 */
@Stable
class LifeAreaReorderState(val listState: LazyListState) {

    /** What the list currently shows: the flow's rows, or the dragged arrangement. */
    var order by mutableStateOf<List<LifeAreaRow>>(emptyList())
        private set

    var draggingId by mutableStateOf<String?>(null)
        private set

    /** Pixels the dragged card is translated by, relative to the slot it now occupies. */
    var offsetY by mutableFloatStateOf(0f)
        private set

    private var fromIndex = -1
    private var fromFlow: List<LifeAreaRow> = emptyList()

    /**
     * Adopts a fresh list from the repository flow. Ignored mid-drag: the finger
     * outranks a snapshot, and the drop reconciles the two.
     */
    fun sync(rows: List<LifeAreaRow>) {
        fromFlow = rows
        if (draggingId == null) order = rows
    }

    fun onDragStart(areaId: String) {
        val index = order.indexOfFirst { it.area.id == areaId }
        if (index < 0) return
        fromIndex = index
        draggingId = areaId
        offsetY = 0f
    }

    /**
     * Moves the card by [deltaY] and, once its centre has entered a neighbour's
     * bounds, swaps the two locally.
     *
     * Neighbours are found through [LazyListState.layoutInfo] rather than by
     * dividing the offset by a row height: the screen's list also holds a sync
     * card, section headers and unfiled goals, so rows are neither uniform in
     * height nor contiguous in index.
     */
    fun onDrag(deltaY: Float) {
        offsetY += deltaY
        val id = draggingId ?: return
        val visible = listState.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.key == id } ?: return
        val centre = dragged.offset + dragged.size / 2f + offsetY
        val target = visible.firstOrNull { item ->
            item.key != id &&
                order.any { it.area.id == item.key } &&
                centre >= item.offset &&
                centre <= item.offset + item.size
        } ?: return

        val from = order.indexOfFirst { it.area.id == id }
        val to = order.indexOfFirst { it.area.id == target.key }
        if (from < 0 || to < 0) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        // The card is about to be laid out in the slot it just displaced, so the
        // translation has to absorb the difference or it would jump out from
        // under the finger by exactly one row.
        offsetY += (dragged.offset - target.offset).toFloat()
    }

    /**
     * Ends the drag.
     *
     * @return the move to persist as `from to to`, or null when the card came
     *   back to where it started — a drag that changes nothing must not write.
     */
    fun onDragEnd(): Pair<Int, Int>? {
        val to = order.indexOfFirst { it.area.id == draggingId }
        val from = fromIndex
        reset()
        // The dragged arrangement is deliberately kept, not rolled back: Firestore
        // echoes a successful write to the snapshot listener within a frame or
        // two, and rolling back first would show one frame of the old order.
        return if (from >= 0 && to >= 0 && from != to) from to to else null
    }

    fun onDragCancel() {
        reset()
        order = fromFlow
    }

    private fun reset() {
        draggingId = null
        fromIndex = -1
        offsetY = 0f
    }
}

@Composable
fun rememberLifeAreaReorderState(
    listState: LazyListState = rememberLazyListState(),
): LifeAreaReorderState = remember(listState) { LifeAreaReorderState(listState) }

/**
 * The reorderable life-area rows, for a `LazyColumn` that also holds other items.
 *
 * @param onMove called with positions in the *displayed* list, on drop or on a
 *   "Move up" / "Move down" accessibility action.
 */
fun LazyListScope.lifeAreaRows(
    state: LifeAreaReorderState,
    onMove: (from: Int, to: Int) -> Unit,
    onOpen: (LifeArea) -> Unit,
    onEdit: (LifeArea) -> Unit,
    onDelete: (LifeArea) -> Unit,
) {
    val rows = state.order
    itemsIndexed(rows, key = { _, row -> row.area.id }) { index, row ->
        LifeAreaCard(
            area = row.area,
            goalCount = row.goalCount,
            onOpen = { onOpen(row.area) },
            // One area cannot be reordered, and a handle that does nothing is
            // worse than no handle.
            reorderable = rows.size > 1,
            isDragging = state.draggingId == row.area.id,
            dragOffsetY = { state.offsetY },
            onDragStart = { state.onDragStart(row.area.id) },
            onDrag = state::onDrag,
            onDragEnd = { state.onDragEnd()?.let { (from, to) -> onMove(from, to) } },
            onDragCancel = state::onDragCancel,
            onMoveUp = if (index > 0) ({ onMove(index, index - 1) }) else null,
            onMoveDown = if (index < rows.lastIndex) ({ onMove(index, index + 1) }) else null,
            onEdit = { onEdit(row.area) },
            onDelete = { onDelete(row.area) },
        )
    }
}

@Composable
private fun LifeAreaCard(
    area: LifeArea,
    goalCount: Int,
    onOpen: () -> Unit,
    reorderable: Boolean,
    isDragging: Boolean,
    dragOffsetY: () -> Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = area.colorHex.toGoalAccent()
    GpCard(
        // The whole card opens the area (#2). The drag handle below consumes its
        // own pointer events, so long-pressing it to reorder never lands here.
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            // Above its neighbours while it is in the air, or the cards it passes
            // over would be drawn on top of it.
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY() else 0f
                alpha = if (isDragging) 0.94f else 1f
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (reorderable) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(22.dp)
                        .pointerInput(area.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDrag = { change, amount ->
                                    change.consume()
                                    onDrag(amount.y)
                                },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel,
                            )
                        }
                        // Dragging is unusable with a screen reader, so the same
                        // two moves are offered as custom actions on the handle —
                        // the node the reader lands on when it announces it.
                        .semantics {
                            this.contentDescription = "Reorder ${area.name}"
                            customActions = listOfNotNull(
                                onMoveUp?.let {
                                    CustomAccessibilityAction("Move up") { it(); true }
                                },
                                onMoveDown?.let {
                                    CustomAccessibilityAction("Move down") { it(); true }
                                },
                            )
                        },
                )
            } else {
                Box(Modifier.size(width = 12.dp, height = 1.dp))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForKey(area.iconKey),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    area.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        // The count is direction-isolated (§4.8): a Latin digit run
                        // inside a Hebrew paragraph is reordered by the bidi
                        // algorithm, and this one sits in a sentence.
                        append(goalCountLabel(goalCount))
                        if (area.isLinkedToGoogleTasks) append(" · synced from Google Tasks")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${area.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${area.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
