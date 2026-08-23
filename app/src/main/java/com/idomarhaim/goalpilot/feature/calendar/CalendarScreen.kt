package com.idomarhaim.goalpilot.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.ProgressRing
import com.idomarhaim.goalpilot.ui.components.toComposeColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * **Spec §4.3 — the calendar you can look at**
 * ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * §4.2 gives it the tab freed by moving Profile into Home's avatar, and gives it one job:
 * ***when?*** — answered at three zooms, opening on the measured default of three days.
 *
 * ### What is on screen, and which clause of §4.3 put it there
 *
 * | Region | §4.3 |
 * |---|---|
 * | The zoom control | *"agenda ⇄ 3 days ⇄ week"* (§4.2) |
 * | The all-day banner strip | *"a `DEADLINE` is only ever a banner"*; challenge windows; goal deadlines; carried work |
 * | The untimed strip | *"a strip for work due today that was never given a time"* |
 * | The hour grid | blocks, the only rung that occupies a slot (§2.4) |
 * | The load bar + booked/free ring | *"arithmetic not inference… red past 75% of waking hours"* |
 * | Tap a slot | *"create by FAB or by tapping a slot"* — [SlotDraft], and the first author `BLOCK` has ever had |
 *
 * ### Nothing here decides anything
 *
 * Every lane, every number and every carried row arrives already computed by [CalendarBuilder],
 * which is pure and JVM-tested. This file positions boxes. That split is why §4.3's rules are
 * checked without an emulator, and why the instrumented test can be about the surface existing and
 * responding rather than about arithmetic.
 *
 * ### §0.8 is suspended, so the literals here are English
 *
 * [AGENTS.md](../../../../../../../../AGENTS.md)'s freeze block: *"write plain English literals in
 * any unswept package"*, and this package is not in `AnalyticsLiteralSweepTest`'s `SWEPT_PACKAGES`.
 * What the freeze does **not** relax is the dialog guard — every window below goes through the
 * `App*` façades in `ui/locale/`, because the defect that guard catches is invisible in an English
 * render and is exactly what would need reworking when `#51` resumes.
 */
@Composable
fun CalendarScreen(
    onOpenTask: (String) -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarSurface(
        state = state,
        onZoom = viewModel::setZoom,
        onShift = viewModel::shift,
        onToday = viewModel::goToToday,
        onOpenTask = onOpenTask,
        onTick = { entry, done -> viewModel.setDone(entry, done) },
        onCreate = { viewModel.create(it) },
    )
}

/**
 * The surface itself — **stateless, and that is what makes it testable at all**.
 *
 * `CalendarScreen` above is four lines of wiring; everything below takes its state as a parameter
 * and reports back through callbacks. The seam exists for one concrete reason: an instrumented test
 * of the Hilt-wired screen needs a signed-in account and a live Firestore, so it would be a test of
 * the network with a calendar attached. Driven here from a hand-built [CalendarUiState] it is a test
 * of the surface, deterministic, with every lane and rung on screen at once — including the ones a
 * real account would rarely be carrying (an overdue carry-forward, an `EXTERNAL` event `#61` cannot
 * yet produce).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSurface(
    state: CalendarUiState,
    onZoom: (CalendarZoom) -> Unit = {},
    onShift: (Int) -> Unit = {},
    onToday: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
    onTick: (CalendarEntry, Boolean) -> Unit = { _, _ -> },
    onCreate: (SlotDraft) -> Unit = {},
) {
    var draft by rememberSaveable(stateSaver = SlotDraftSaver) { mutableStateOf<SlotDraft?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(rangeLabel(state), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = { onShift(-1) }, modifier = Modifier.testTag(TAG_PREV)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    IconButton(onClick = onToday, modifier = Modifier.testTag(TAG_TODAY)) {
                        Icon(Icons.Filled.Today, contentDescription = "Today")
                    }
                    IconButton(onClick = { onShift(1) }, modifier = Modifier.testTag(TAG_NEXT)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { draft = SlotDraft(date = state.anchor) },
                modifier = Modifier.testTag(TAG_FAB),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New")
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .testTag(TAG_SURFACE),
        ) {
            ZoomControl(
                zoom = state.zoom,
                onZoom = onZoom,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            when {
                state.isLoading -> LoadingBox()
                state.zoom == CalendarZoom.AGENDA -> AgendaColumn(
                    day = state.days.firstOrNull(),
                    onOpen = { it.taskId?.let(onOpenTask) },
                    onTick = onTick,
                    onCreate = { draft = SlotDraft(date = state.anchor) },
                )
                else -> DayColumns(
                    state = state,
                    onOpen = { it.taskId?.let(onOpenTask) },
                    onTick = onTick,
                    onTapSlot = { date, hour -> draft = SlotDraft.atSlot(date, hour) },
                )
            }
        }
    }

    draft?.let { current ->
        SlotSheet(
            draft = current,
            goals = state.goals,
            onChange = { draft = it },
            onDismiss = { draft = null },
            onSave = { onCreate(it); draft = null },
        )
    }
}

/** §4.2's zoom, and the only control that changes what the surface *is*. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoomControl(zoom: CalendarZoom, onZoom: (CalendarZoom) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        CalendarZoom.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = zoom == option,
                onClick = { onZoom(option) },
                shape = SegmentedButtonDefaults.itemShape(index, CalendarZoom.entries.size),
                modifier = Modifier.testTag(zoomTag(option)),
            ) {
                Text(option.label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * The grid zooms — three days and week.
 *
 * §4.3's measurement is what fixes the column widths: seven columns on a 390 dp phone is ~46 dp
 * each, at which *"no Hebrew title and no time range survives"*, so [WEEK] stacks its times **start
 * over end** and [THREE_DAYS] (~110 dp) writes them on one line. Both are handled by
 * [TimeColumn]'s `stacked` flag rather than by two grids.
 */
@Composable
private fun DayColumns(
    state: CalendarUiState,
    onOpen: (CalendarEntry) -> Unit,
    onTick: (CalendarEntry, Boolean) -> Unit,
    onTapSlot: (LocalDate, Int) -> Unit,
) {
    val stacked = state.zoom == CalendarZoom.WEEK
    val days = state.days
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
    ) {
        Band(days) { day ->
            // The day's tag goes on the header band ALONE. Putting it on every band would make
            // `onNodeWithTag(dayTag(...))` match once per band, and a selector that matches four
            // nodes is a selector that asserts nothing.
            Column(modifier = Modifier.testTag(dayTag(day.date))) {
                DayHeader(day)
                LoadBar(day.load, Modifier.padding(top = 4.dp))
            }
        }
        if (days.any { it.allDay.isNotEmpty() }) {
            Band(days) { day ->
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    day.allDay.forEach { EntryChip(it, stacked, onOpen, onTick) }
                }
            }
        }
        if (days.any { it.untimed.isNotEmpty() }) {
            Band(days) { day ->
                if (day.untimed.isNotEmpty()) UntimedStrip(day.untimed, stacked, onOpen, onTick)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Box(modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) {
                    HourGrid(day = day, stacked = stacked, onOpen = onOpen, onTick = onTick, onTapSlot = onTapSlot)
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

/**
 * One horizontal band across **every** column, every cell the same height.
 *
 * ⚠️ **This is the fix for a defect the render pass found and no assertion could.** The first
 * build gave each day a `Column` of its own — header, banners, untimed strip, then its grid — so a
 * day carrying four banners started its grid four rows lower than a day carrying one. `Observed:`
 * `issue-60-calendar-3day.png`, first capture: **Monday's 07:00 sat below Tuesday's 09:00.** Every
 * test passed, because every node was present and displayed; what was wrong is that an hour grid
 * whose rows do not line up across columns is not a grid — it is three unrelated lists with hour
 * lines drawn on them, and comparing two days is the one thing a multi-day calendar is for.
 *
 * [IntrinsicSize.Max] makes the band as tall as its tallest cell and every cell that tall, so the
 * grids below all begin at one `y` whatever each day happens to be carrying.
 */
@Composable
private fun Band(days: List<CalendarDay>, cell: @Composable (CalendarDay) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
        days.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp),
            ) {
                cell(day)
            }
        }
    }
}

@Composable
private fun DayHeader(day: CalendarDay) {
    val weekday = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = weekday,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        BookedFreeRing(day.load)
    }
}

/**
 * §4.3's **booked/free ring** — how much of the waking day is already spoken for.
 *
 * It shows the fraction and nothing else: the exact minutes are on the load bar's own label, and a
 * ring carrying a second number would be the same *"one control, two axes"* failure §0.8 deleted
 * the rung glyphs for.
 */
@Composable
private fun BookedFreeRing(load: DayLoad) {
    ProgressRing(
        progress = load.barFraction,
        size = 26.dp,
        strokeWidth = 3.dp,
        color = if (load.isOverloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag(TAG_RING),
    )
}

/**
 * §4.3's **per-day load bar**, red past 75% of waking hours.
 *
 * The threshold is [com.idomarhaim.goalpilot.domain.model.WakingHours.loadBarRedMinutes] and is
 * **not** recomputed here — §4.9's own KDoc says why: *"two places computing 'three quarters of the
 * waking day' is one refactor away from being two different numbers, and the bar is on a screen
 * nobody is looking at while they move this setting."*
 */
@Composable
private fun LoadBar(load: DayLoad, modifier: Modifier = Modifier) {
    val colour = if (load.isOverloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(modifier = modifier.testTag(TAG_LOAD_BAR)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(load.barFraction)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colour),
            )
        }
        Text(
            text = if (load.isEmpty) "free" else "${hoursLabel(load.bookedMinutes)} booked",
            style = MaterialTheme.typography.labelSmall,
            color = if (load.isOverloaded) colour else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/**
 * §4.3: ***"a strip for work due today that was never given a time"*** — *"without which the
 * calendar quietly lies about the day's real workload"*.
 *
 * It is labelled, and that is the point: unlabelled it reads as another band of banners, and the
 * one thing it exists to say — *this is work, and it has no slot* — is exactly what would be lost.
 */
@Composable
private fun UntimedStrip(
    entries: List<CalendarEntry>,
    stacked: Boolean,
    onOpen: (CalendarEntry) -> Unit,
    onTick: (CalendarEntry, Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 6.dp).testTag(TAG_UNTIMED)) {
        Text(
            text = "No time set",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        entries.forEach { EntryChip(it, stacked, onOpen, onTick) }
    }
}

/**
 * The hour grid — **blocks only**, laid out by offsetting each box to its own start minute.
 *
 * Only [HOUR_FROM]..[HOUR_TO] is drawn, and that is a deliberate crop rather than the whole day: a
 * 24-hour grid at this density is mostly empty rows nobody scrolls past. A block outside the window
 * is not lost — it is drawn clamped to the edge, so a 05:00 start reads as *before the grid begins*
 * rather than vanishing, which is the failure a crop normally has.
 */
@Composable
private fun HourGrid(
    day: CalendarDay,
    stacked: Boolean,
    onOpen: (CalendarEntry) -> Unit,
    onTick: (CalendarEntry, Boolean) -> Unit,
    onTapSlot: (LocalDate, Int) -> Unit,
) {
    val hours = HOUR_TO - HOUR_FROM
    Box(modifier = Modifier.fillMaxWidth().height((hours * HOUR_HEIGHT_DP).dp).testTag(TAG_GRID)) {
        Column(modifier = Modifier.fillMaxSize()) {
            (HOUR_FROM until HOUR_TO).forEach { hour ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HOUR_HEIGHT_DP.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant)
                        // 4.3's "create by tapping a slot" -- the affordance that makes BLOCK
                        // reachable for the first time in the product's life.
                        .clickable { onTapSlot(day.date, hour) }
                        .testTag(slotTag(day.date, hour)),
                )
            }
        }
        day.timed.forEach { entry ->
            val minutes = entry.occurrence.opensAt.let { start ->
                if (start.toLocalDate() != day.date) 0 else start.hour * 60 + start.minute
            }
            val top = ((minutes - HOUR_FROM * 60).coerceAtLeast(0) / 60f) * HOUR_HEIGHT_DP
            Box(modifier = Modifier.offset(y = top.dp).fillMaxWidth()) {
                EntryChip(entry, stacked, onOpen, onTick)
            }
        }
    }
}

/** The [CalendarZoom.AGENDA] level — one day, as a list. §4.2: not a third screen. */
@Composable
private fun AgendaColumn(
    day: CalendarDay?,
    onOpen: (CalendarEntry) -> Unit,
    onTick: (CalendarEntry, Boolean) -> Unit,
    onCreate: () -> Unit,
) {
    if (day == null) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag(dayTag(day.date)),
    ) {
        DayHeader(day)
        LoadBar(day.load, modifier = Modifier.padding(vertical = 6.dp))
        if (day.isEmpty) {
            Text(
                text = "Nothing planned. Tap + to place something.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp).clickable(onClick = onCreate),
            )
        }
        day.allDay.forEach { EntryChip(it, stacked = false, onOpen = onOpen, onTick = onTick) }
        if (day.untimed.isNotEmpty()) UntimedStrip(day.untimed, false, onOpen, onTick)
        day.timed.forEach { EntryChip(it, stacked = false, onOpen = onOpen, onTick = onTick) }
        Spacer(Modifier.height(96.dp))
    }
}

/**
 * One row: **the leading time column carries the rung, the chip carries the life area.**
 *
 * §4.3, and it is the sentence the prototype's rev 2 was rewritten around — the chip had been
 * carrying *"two unrelated axes"* and the rung had degraded into a symbol nobody could read. So
 * there is no glyph, no legend, and no symbol vocabulary here: the rung is in the **form** of
 * [TimeColumn], and [LifeAreaDot] carries a colour and a name.
 */
@Composable
private fun EntryChip(
    entry: CalendarEntry,
    stacked: Boolean,
    onOpen: (CalendarEntry) -> Unit,
    onTick: (CalendarEntry, Boolean) -> Unit,
) {
    val isExternal = entry.kind == EntryKind.EXTERNAL
    val accent = when {
        // 4.3: hand-made Google events in grey. Nothing produces one until #61 -- see EntryKind.
        isExternal -> MaterialTheme.colorScheme.outline
        entry.carriedForward -> MaterialTheme.colorScheme.error
        else -> entry.lifeArea?.colorHex?.toComposeColor() ?: MaterialTheme.colorScheme.primary
    }
    val box = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(accent.copy(alpha = 0.14f))
        .clickable { onOpen(entry) }
        .padding(horizontal = 4.dp, vertical = 4.dp)
        .testTag(entryTag(entry))

    if (stacked) StackedChip(entry, accent, box) else WideChip(entry, accent, box, onTick)
}

/**
 * The [CalendarZoom.WEEK] chip: **the stacked time gets the whole column, and the title goes under
 * it.**
 *
 * ⚠️ **The first build put the time column BESIDE the title here, and the render pass produced
 * exactly the failure §4.3 measured.** `Observed:` `issue-60-calendar-week.png`, third capture --
 * every time clipped to `07:0`, `09:3`, `12:3`, and every title reduced to an ellipsis or to
 * nothing. §4.3 had already said why: *"Seven columns on a 390 dp phone is ~46 dp per day -- no
 * Hebrew title and no time range survives it. Week view stacks the times **start over end**, the
 * only thing that fits at 46 dp."* That is not an instruction to stack times inside a narrow column
 * beside a title; it is *the stacked time is what the column is for*.
 *
 * The rung is still carried by **form** -- a 3 dp leading rail, rounded for a capsule -- exactly as
 * at the wider zooms, so no symbol vocabulary appears here either. The tick is dropped: at this
 * width it would take a quarter of the column, and ticking work off is something you do on the day
 * you are looking at, which is what the other two zooms are for.
 */
@Composable
private fun StackedChip(entry: CalendarEntry, accent: Color, box: Modifier) {
    val form = entry.timeColumnForm
    Row(modifier = box) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (form == TimeColumnForm.RAIL) 26.dp else 20.dp)
                .clip(if (form == TimeColumnForm.CAPSULE) RoundedCornerShape(4.dp) else CircleShape)
                .background(accent.copy(alpha = if (form == TimeColumnForm.CAPSULE) 0.45f else 1f)),
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f).testTag(formTag(form))) {
            when (form) {
                TimeColumnForm.RAIL -> {
                    Text(entry.occurrence.opensAt.toLocalTime().format(HH_MM), style = stackedLabel(), maxLines = 1)
                    Text(
                        text = entry.occurrence.closesAt.toLocalTime().format(HH_MM),
                        style = stackedLabel(),
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TimeColumnForm.POINT ->
                    Text(entry.occurrence.opensAt.toLocalTime().format(HH_MM), style = stackedLabel(), maxLines = 1)
                TimeColumnForm.CAPSULE -> {
                    val days = spanDays(entry)
                    Text(
                        text = "$days " + if (days == 1L) "day" else "days",
                        style = stackedLabel(),
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TimeColumnForm.WORDS -> Text(
                    text = "all-day",
                    style = stackedLabel(),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** The [CalendarZoom.THREE_DAYS] and [CalendarZoom.AGENDA] chip: time beside title, and a tick. */
@Composable
private fun WideChip(
    entry: CalendarEntry,
    accent: Color,
    box: Modifier,
    onTick: (CalendarEntry, Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = box) {
        TimeColumn(entry = entry, accent = accent)
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            entry.lifeArea?.let { LifeAreaDot(it) }
            if (entry.carriedForward) {
                Text(
                    text = "overdue since " + entry.date.format(DAY_MONTH),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (entry.isTickable) {
            TickBox(
                checked = entry.outcome is OccurrenceOutcome.Done,
                onCheck = { onTick(entry, it) },
                accent = accent,
            )
        }
    }
}

@Composable
private fun stackedLabel() = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")

/**
 * §4.3: ***"the rung is carried by the form of the leading time column, never by a glyph on the
 * chip."*** The prototype's rev-2 table, drawn.
 *
 * | Rung | Form | Reads as |
 * |---|---|---|
 * | `BLOCK` | start over end with a **filled rail** | a span of time you are inside |
 * | `DEADLINE` | `due` + the time, then a **single point** | a moment, not a duration |
 * | `SPAN` | a date range + a **soft capsule** | days, not hours |
 * | `ALL_DAY` | the words **all-day**, no time | a day with no slot |
 *
 * Times use tabular figures so a column of them stops looking ragged, and every one of them is
 * built from `LocalTime` rather than concatenated — §4.8's bidi defect class starts with a hand-made
 * time string, and although §0.8 is suspended the guard costs nothing to keep.
 */
@Composable
private fun TimeColumn(entry: CalendarEntry, accent: Color) {
    // "tnum" -- tabular figures, so a column of times stops looking ragged. The prototype's
    // rev-3 craft pass named it and it is one line here.
    val label = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    // A FIXED width, and the render pass is why. Unbounded, the CAPSULE's date range ("17 Aug-19
    // Aug") consumed the whole row and the title next to it was squeezed to nothing -- the span
    // rendered as a date with a tick and no name at all. A leading column that can grow is a
    // leading column that wins, and the thing it wins against is the only text that identifies the
    // row.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(TIME_COLUMN_DP.dp)
            .testTag(formTag(entry.timeColumnForm)),
    ) {
        when (entry.timeColumnForm) {
            TimeColumnForm.RAIL -> {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(entry.occurrence.opensAt.toLocalTime().format(HH_MM), style = label, maxLines = 1)
                    Text(
                        text = entry.occurrence.closesAt.toLocalTime().format(HH_MM),
                        style = label,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TimeColumnForm.POINT -> {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(3.dp))
                Column {
                    Text("due", style = label, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(entry.occurrence.opensAt.toLocalTime().format(HH_MM), style = label, maxLines = 1)
                }
            }
            TimeColumnForm.CAPSULE -> {
                Box(
                    modifier = Modifier
                        .width(7.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.45f)),
                )
                Spacer(Modifier.width(3.dp))
                // A day COUNT, not two dates. 2.2's span is "days, not hours", and the count is
                // what says that in the width available; the dates themselves are on the columns
                // the capsule already stretches across, which is where a reader looks for them.
                val days = spanDays(entry)
                Column {
                    Text("$days", style = label, maxLines = 1, textAlign = TextAlign.Start)
                    Text(
                        // Singular at n=1. The prototype's rev 2 fixed exactly this in both
                        // languages and both branches; "1 days" is the same defect in English.
                        text = if (days == 1L) "day" else "days",
                        style = label,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TimeColumnForm.WORDS -> {
                Text(
                    text = "all-day",
                    style = label,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** §4.3's chip, and the whole of it: a colour dot and its name. No second axis. */
@Composable
private fun LifeAreaDot(chip: LifeAreaChip) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(chip.colorHex.toComposeColor()))
        Spacer(Modifier.width(4.dp))
        Text(
            text = chip.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** §4.3's *tick to complete*. It records that the window was honoured; it banks no points (§1.4). */
@Composable
private fun TickBox(checked: Boolean, onCheck: (Boolean) -> Unit, accent: Color) {
    // An OUTLINE until it is done. The render pass showed a filled button here reading as the
    // heaviest thing on every row -- a dark disc per entry, louder than the title it belonged to.
    // A tick is an affordance, not the subject: the ink belongs on the work.
    IconButton(
        onClick = { onCheck(!checked) },
        modifier = Modifier.size(20.dp).testTag(TAG_TICK),
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (checked) "Mark not done" else "Mark done",
            modifier = Modifier.size(14.dp),
            tint = if (checked) accent else MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun rangeLabel(state: CalendarUiState): String {
    val days = state.days
    if (days.isEmpty()) return "Calendar"
    val first = days.first().date
    val last = days.last().date
    return if (first == last) first.format(FULL_DAY) else "${first.format(DAY_MONTH)} – ${last.format(DAY_MONTH)}"
}

private fun hoursLabel(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/**
 * How wide the leading time column is. See [TimeColumn] for what happens when it is not fixed.
 *
 * §4.3's measurement is the budget: *"at 3 days a column is ~110 dp and both fit"* — both being
 * the title and the time. At 46 dp here, plus a 24 dp tick and 6 dp of padding either side, the
 * title was left ~40 dp and Compose broke **"Morning" across two lines** ("Mornin / g run").
 * `Observed:` `issue-60-calendar-3day.png`, second capture. The column, the tick and the padding
 * all give a little back, and the title drops one step on the type scale, so an ordinary word fits
 * on one line at the width §4.3 chose the default for.
 */
private const val TIME_COLUMN_DP = 42

/** How many whole days a span covers. `closesAt` is the day AFTER the last, so this is the count. */
private fun spanDays(entry: CalendarEntry): Long =
    java.time.temporal.ChronoUnit.DAYS.between(
        entry.occurrence.opensAt.toLocalDate(),
        entry.occurrence.closesAt.toLocalDate(),
    ).coerceAtLeast(1)

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val FULL_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

/** The drawn window of the grid. See [HourGrid] for why it is cropped and what happens outside it. */
internal const val HOUR_FROM = 6
internal const val HOUR_TO = 23
private const val HOUR_HEIGHT_DP = 44

// Test tags. Named constants rather than string literals at the assertion site, so a renamed tag
// breaks the compile rather than the run -- the instrumented suite is the expensive place to find
// out that a selector no longer matches anything.
const val TAG_SURFACE = "calendar_surface"
const val TAG_GRID = "calendar_grid"
const val TAG_LOAD_BAR = "calendar_load_bar"
const val TAG_RING = "calendar_ring"
const val TAG_UNTIMED = "calendar_untimed_strip"
const val TAG_FAB = "calendar_fab"
const val TAG_TICK = "calendar_tick"
const val TAG_PREV = "calendar_prev"
const val TAG_NEXT = "calendar_next"
const val TAG_TODAY = "calendar_today"

fun zoomTag(zoom: CalendarZoom): String = "calendar_zoom_${zoom.name}"
fun dayTag(date: LocalDate): String = "calendar_day_$date"
fun slotTag(date: LocalDate, hour: Int): String = "calendar_slot_${date}_$hour"
fun entryTag(entry: CalendarEntry): String = "calendar_entry_${entry.key}"
fun formTag(form: TimeColumnForm): String = "calendar_form_${form.name}"
