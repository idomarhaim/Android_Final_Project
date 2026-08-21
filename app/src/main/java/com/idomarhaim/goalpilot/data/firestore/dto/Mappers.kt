package com.idomarhaim.goalpilot.data.firestore.dto

import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import java.time.LocalDate
import java.time.LocalDateTime

// ── Goal ───────────────────────────────────────────────────────────

/**
 * The plural life-area edge, backfilled from the pre-§1.2 singular field.
 *
 * `lifeAreaIds` wins whenever the document has one, so a migrated document never
 * consults the legacy field again; absent it, `[lifeAreaId]` / `[]` is the
 * translation §7.1 prescribes. Blanks and duplicates are dropped here rather than
 * anywhere downstream — a blank id resolves to no area, and a repeated id would
 * count one goal twice in the same band.
 */
private fun GoalDto.resolvedLifeAreaIds(): List<String> =
    (lifeAreaIds ?: listOfNotNull(lifeAreaId))
        .mapNotNull { it.takeIf(String::isNotBlank) }
        .distinct()

/**
 * §1.3's measure, resolved from the new fields or migrated off the free-text
 * `unit` this app wrote until #11 — §7.1.
 *
 * ### The trap, and the decision written down
 *
 * Mapping free text to a closed kind is a **guess** for anything that is not
 * literally `"%"`, and #11's brief forbids inventing one from a string match. It
 * is not squeamishness: the word is user content, so it may be Hebrew, an
 * abbreviation, or a typo, and a matcher that reads `"L"` as litres reads `"l"`
 * as nothing and `"ליטר"` as nothing. So the rule is *map what is unambiguous,
 * keep the word either way, and let `C22` #44 ask about the rest.*
 *
 * Four branches, in order:
 *
 * 1. **The new fields win outright.** A document carrying either of them has been
 *    migrated and is never re-derived — the same posture `resolvedLifeAreaIds`
 *    takes, and for the same reason: two answers to one question is the map's
 *    most-repeated finding.
 * 2. **Blank or `"%"` becomes absent.** `"%"` was the *default*, so a stored
 *    `"%"` means either *the user chose percent* or *nobody chose anything*, and
 *    **nothing on the wire distinguishes them** — the field was written the same
 *    way in both cases. §7.1 resolves it toward absent (*"a defaulted `"%"`
 *    becomes absent"*), and the asymmetry is what makes that right: absent is
 *    recoverable, because the goal is *asked* and the user answers in one tap,
 *    whereas a wrongly-kept `PERCENT` looks decided and is never asked again.
 *    Ido's live *"Drink 4 Liters of Water Daily"* — reading `1/100 %` today — is
 *    exactly this document, and it is #11's whole reason for existing. `"%"`
 *    survives from here on as a measure the user *picked*, written into
 *    `measureKind` where the choosing is recorded.
 * 3. **A goal the app itself authored is classified from its own key.** A
 *    `healthSourceKey` is stamped by [SyncHealthDataUseCase] at birth and cannot
 *    be reached by the user (#47), so it is app knowledge about an app-written
 *    goal, not a reading of user text — `hc:goal:steps` counts things, `hc:goal:sleep`
 *    counts time, and neither depends on what the `unit` string happens to say.
 *    Without this branch every Health Connect goal would lose its measure on
 *    migration and stop reading `3200 / 70000 steps` on the widget, which is a
 *    regression rather than an honest absence.
 * 4. **Everything else keeps its word and leaves its kind open.** `"books"`
 *    survives verbatim and still renders; it simply cannot be computed with until
 *    somebody says what it counts.
 */
private fun GoalDto.resolvedMeasure(): Measure? {
    val stored = Measure.of(MeasureKind.fromName(measureKind), measureWord)
    if (stored != null || !measureWord.isNullOrBlank() || measureKind != null) return stored

    val legacy = unit?.trim().orEmpty()
    if (legacy.isEmpty() || legacy == "%") return null

    val authored = HealthMetric.entries.firstOrNull { it.goalSourceKey == healthSourceKey }
    return Measure.of(authored?.measureKind, legacy)
}

fun GoalDto.toDomain(): Goal = Goal(
    id = id,
    title = title,
    description = description,
    category = GoalCategory.fromName(category),
    // §1.1's intrinsic marker, with §7.1's backfill applied on READ rather than by a
    // migration write: an absent field is `UNKNOWN`, and the explicit `"NONE"` sentinel is
    // the only thing that reads back as null. See GoalDto.declaredBy for why absence cannot
    // do both jobs, and why no goal in goalpilot-56e30 is rewritten to find this out.
    // NOT `fromName(declaredBy) ?: UNKNOWN`: that elvis fires for BOTH an absent field and
    // the NONE sentinel, so a demoted goal reads back as UNKNOWN — a goal again — on the very
    // next snapshot. The two cases must be split before the enum is consulted, and
    // `GoalDeclaredByMigrationTest` is what caught it.
    declaredBy = declaredBy.let { stored ->
        if (stored == null) DeclaredBy.UNKNOWN else DeclaredBy.fromName(stored)
    },
    lifeAreaIds = resolvedLifeAreaIds(),
    healthSourceKey = healthSourceKey?.takeIf { it.isNotBlank() },
    targetValue = targetValue,
    // `currentValue` is left at its `0.0` default and filled in by
    // `withDerivedProgress` at the repository boundary (#49). A goal that never
    // reaches that seam honestly reads zero rather than carrying a stale number.
    measure = resolvedMeasure(),
    // A goal the sync owns is in AUTO whatever the document says, because the
    // mode is a property of where its numbers come from rather than a choice
    // (§0.2 — derive, don't store). Absent otherwise means NUMBER, which is what
    // every goal did before the field existed, so day one reads identically.
    inputMode = when {
        healthSourceKey?.isNotBlank() == true -> InputMode.AUTO
        else -> InputMode.fromName(inputMode) ?: InputMode.NUMBER
    },
    colorHex = colorHex.ifBlank { GoalCategory.fromName(category).defaultColorHex },
    deadlineEpochMillis = deadline,
    isArchived = archived,
    createdAtEpochMillis = createdAt,
    updatedAtEpochMillis = updatedAt,
)

fun Goal.toDto(): GoalDto = GoalDto(
    id = id,
    title = title,
    description = description,
    category = category.name,
    // Never null: absence is reserved for documents that predate the field, so writing null
    // here would re-open the ambiguity the DTO's sentinel exists to close — a demoted goal
    // would read back as UNKNOWN and quietly become a goal again on the next snapshot.
    declaredBy = declaredBy?.name ?: GoalDto.DECLARED_BY_NONE,
    // Null, always: a write migrates the document, and leaving the old field
    // populated would be exactly the "second number that quietly disagrees" the
    // map names three times over.
    lifeAreaId = null,
    lifeAreaIds = lifeAreaIds,
    healthSourceKey = healthSourceKey,
    targetValue = targetValue,
    // `currentValue` is not written: the facts are the record (#49, §7.1).
    measureKind = measure?.kind?.name,
    measureWord = measure?.word?.takeIf { it.isNotBlank() },
    inputMode = inputMode.name,
    // Null, always — the same migrating-write rule `lifeAreaId` follows. A write
    // migrates the document, and leaving the free-text unit populated beside the
    // two fields that replaced it is the second answer that quietly disagrees.
    unit = null,
    colorHex = colorHex,
    deadline = deadlineEpochMillis,
    archived = isArchived,
    createdAt = createdAtEpochMillis,
    updatedAt = updatedAtEpochMillis,
)

// ── Task ───────────────────────────────────────────────────────────

/**
 * The task's edges, from whichever of the two shapes the document is in (§1.5, `#55`).
 *
 * A document written **after** `#55` carries `goalEdges` and is read straight off it. One
 * written **before** carries `goalId` plus `progressContribution`, and is read into the one
 * edge it describes — **with its stored contribution verbatim**. That last word is the whole
 * migration decision: `progressContribution`'s `1.0` was a silence rather than a value
 * (§1.5), but it is a *stored* silence, and re-reading stored numbers as `null` would zero
 * the task half of every existing goal's progress with no user action. So the undeclared
 * default governs **new** edges, and nothing already written changes value.
 *
 * `progressContribution` is nullable with a null default for exactly this: it tells a
 * document that stored `1.0` apart from one that never had the field.
 */
private fun TaskDto.edges(): List<GoalEdge> =
    goalEdges
        .filter { it.goalId.isNotBlank() }
        .map { GoalEdge(goalId = it.goalId, contribution = it.contribution) }
        .ifEmpty { goalEdgesOf(goalId, contribution = progressContribution) }

/**
 * The completion a **pre-`#55`** document describes, or `null`.
 *
 * The fact now lives in its own document, and `TaskRepositoryImpl` overlays it. This is what
 * a task that has never been re-ticked since the migration reads as, and it is **lossless**:
 * a legacy point value `p` reconstructs to `3p` minutes at `ROUTINE`, which prices back at
 * exactly `p`. So the migration moves where the fact lives without re-pricing one historical
 * completion — see `TaskDuration.legacyMinutesFromPoints`.
 *
 * A stored duration wins over the reconstruction when there is one, because it is a better
 * record of the same thing; the two can differ, and where they do the task *was* re-priced
 * by the inversion, which is what §1.4 asks for.
 */
private fun TaskDto.legacyCompletion(): CompletionFact? {
    if (done != true) return null
    return CompletionFact(
        completedAtEpochMillis = completedAt ?: createdAt,
        minutes = TaskDuration.sanitize(estimatedMinutes)
            ?: TaskDuration.legacyMinutesFromPoints(points ?: 10),
        difficulty = Difficulty.fromName(difficulty),
    )
}

/**
 * §2.2's occurrence, from the four wire fields — or `null`, which is what **every task written
 * before `#56`** reads as and is the honest answer for a task nobody gave a *when* (`#56`).
 *
 * ### Nothing here guesses, and each `return null` is the same decision
 *
 * A rung this build does not recognise, a start that will not parse, a `BLOCK` or `SPAN` with
 * no end — all read as *no occurrence*. The alternative in every case is to invent a rung, and
 * §2.2 discriminates rungs by **what a miss means**, so an invented one puts a task into a
 * failure state nobody chose. Half a window is not a window.
 *
 * None of those states is reachable from this app: [Task.toDto] writes a rung with its start
 * always, and an end exactly for the two rungs that have one. They are here because the
 * document is public to any future writer, and because a `DateTimeParseException` between a
 * snapshot and a frame would take down the task list over one bad string.
 *
 * ### Local text, parsed as local text
 *
 * [TaskDto.occurrenceStart] carries `2026-08-22` or `2026-08-22T06:00` and never an instant —
 * see that field for why a *day* stored as millis moves between time zones. `LocalDate.parse`
 * and `LocalDateTime.parse` read exactly the ISO-8601 forms `toDto` writes.
 */
private fun TaskDto.occurrence(): Occurrence? {
    val start = occurrenceStart?.takeIf { it.isNotBlank() } ?: return null
    return when (OccurrenceRung.fromName(occurrenceRung)) {
        null -> null
        OccurrenceRung.ALL_DAY -> parseLocalDate(start)?.let { AllDay(it) }
        OccurrenceRung.DEADLINE -> parseLocalDateTime(start)?.let { Deadline(it) }
        OccurrenceRung.BLOCK -> {
            val from = parseLocalDateTime(start) ?: return null
            val to = parseLocalDateTime(occurrenceEnd) ?: return null
            Block(start = from, end = to, placement = BlockPlacement.fromName(occurrencePlacement))
        }
        OccurrenceRung.SPAN -> {
            val from = parseLocalDate(start) ?: return null
            val to = parseLocalDate(occurrenceEnd) ?: return null
            Span(from = from, to = to)
        }
    }
}

/**
 * What [TaskDto.occurrenceStart] holds for this occurrence — a **date** for the two rungs that
 * are about days, a **local date-time** for the two that are about instants.
 *
 * `LocalDate.toString()` and `LocalDateTime.toString()` are ISO-8601 by contract, which is
 * exactly what [parseLocalDate] and [parseLocalDateTime] read back, so the round trip is the
 * platform's rather than a format string kept in step in two places.
 */
private fun wireStart(occurrence: Occurrence): String = when (occurrence) {
    is AllDay -> occurrence.date.toString()
    is Deadline -> occurrence.at.toString()
    is Block -> occurrence.start.toString()
    is Span -> occurrence.from.toString()
}

/**
 * What [TaskDto.occurrenceEnd] holds — `null` for [AllDay] and [Deadline], whose ends are
 * implied by their starts and would be a second stored value with nothing to keep it true.
 */
private fun wireEnd(occurrence: Occurrence): String? = when (occurrence) {
    is AllDay, is Deadline -> null
    is Block -> occurrence.end.toString()
    is Span -> occurrence.to.toString()
}

/** ISO-8601 `2026-08-22`, or `null` for anything else. Never throws — see [occurrence]. */
private fun parseLocalDate(text: String?): LocalDate? =
    text?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** ISO-8601 `2026-08-22T06:00`, or `null` for anything else. Never throws. */
private fun parseLocalDateTime(text: String?): LocalDateTime? =
    text?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

fun TaskDto.toDomain(): Task = Task(
    id = id,
    title = title,
    goalEdges = edges(),
    difficulty = Difficulty.fromName(difficulty),
    source = TaskSource.fromName(source),
    estimatedMinutes = TaskDuration.sanitize(estimatedMinutes),
    // Absent, unknown and misspelled all read as UNKNOWN — see TaskDto.durationSource
    // for why a legacy row is left absent rather than back-filled.
    durationSource = DurationSource.fromName(durationSource),
    occurrence = occurrence(),
    createdAtEpochMillis = createdAt,
    // The legacy shape only. A real fact document, when there is one, is overlaid by
    // `TaskRepositoryImpl.observeTasks` and wins — a task that has been ticked since the
    // migration has both, and the fact is the one that was actually banked.
    completion = legacyCompletion(),
)

fun Task.toDto(): TaskDto = TaskDto(
    id = id,
    // Written from the edge list, never from `Task.goalId` directly -- same value, but this
    // says out loud which of the two is derived from which.
    goalId = goalEdges.firstOrNull()?.goalId,
    title = title,
    goalEdges = goalEdges.map { GoalEdgeDto(goalId = it.goalId, contribution = it.contribution) },
    difficulty = difficulty.name,
    source = source.name,
    estimatedMinutes = TaskDuration.sanitize(estimatedMinutes),
    durationSource = durationSource.name,
    // §2.2's occurrence, as the four fields TaskDto documents (`#56`). All four are null for a
    // task with no *when*, which is most of them — and a task whose occurrence was **removed**
    // writes four nulls over whatever was there, because `upsertTask` is a whole-document
    // `set()` and a retained start beside an absent rung is the second answer §0.3 keeps
    // naming. `occurrence()` reads a rung with no start as no occurrence, so a half-cleared
    // document would be silently un-schedulable rather than loudly wrong.
    occurrenceRung = occurrence?.rung?.name,
    occurrenceStart = occurrence?.let { wireStart(it) },
    occurrenceEnd = occurrence?.let { wireEnd(it) },
    // Written for BLOCK alone, so a stored placement can never sit on a rung that has none.
    occurrencePlacement = (occurrence as? Block)?.placement?.name,
    createdAt = createdAtEpochMillis,
    // ── The four legacy fields, nulled on every write (`#55`) ──────────────
    //
    // Same migrating-write rule `GoalDto.lifeAreaId` and `GoalDto.unit` already follow: a
    // write migrates the document, and leaving a superseded field populated beside the thing
    // that replaced it is exactly the "second number that quietly disagrees" the map names
    // three times over.
    //
    // `done`/`completedAt` matter most here. The fact is its own document now, so a leftover
    // `done: true` on the task would be re-synthesised by `legacyCompletion()` after an
    // untick had already deleted the fact -- a task that cannot be un-ticked. Nulling them
    // is what closes that, and `setDone` clears them in its own batch for the same reason.
    points = null,
    done = null,
    progressContribution = null,
    completedAt = null,
)

fun CompletionFactDto.toDomain(): CompletionFact = CompletionFact(
    completedAtEpochMillis = completedAt,
    minutes = minutes,
    difficulty = Difficulty.fromName(difficulty),
)

fun CompletionFact.toDto(taskId: String): CompletionFactDto = CompletionFactDto(
    id = taskId,
    completedAt = completedAtEpochMillis,
    minutes = minutes,
    difficulty = difficulty.name,
)

// ── LifeArea ───────────────────────────────────────────────────────
fun LifeAreaDto.toDomain(): LifeArea = LifeArea(
    id = id,
    name = name,
    colorHex = colorHex.ifBlank { LifeAreaPalette.DEFAULT_HEX },
    iconKey = iconKey.ifBlank { "flag" },
    sortOrder = sortOrder,
    googleListId = googleListId?.takeIf { it.isNotBlank() },
    isArchived = archived,
    createdAtEpochMillis = createdAt,
    updatedAtEpochMillis = updatedAt,
)

fun LifeArea.toDto(): LifeAreaDto = LifeAreaDto(
    id = id,
    name = name,
    colorHex = colorHex,
    iconKey = iconKey,
    sortOrder = sortOrder,
    googleListId = googleListId,
    archived = isArchived,
    createdAt = createdAtEpochMillis,
    updatedAt = updatedAtEpochMillis,
)

// ── ProgressEntry ──────────────────────────────────────────────────
fun ProgressDto.toDomain(): ProgressEntry = ProgressEntry(
    id = id,
    goalId = goalId,
    value = value,
    note = note,
    imageUrl = imageUrl,
    createdAtEpochMillis = createdAt,
    sourceKey = sourceKey,
)

// ── User ───────────────────────────────────────────────────────────
fun UserDto.toDomain(): User = User(
    uid = uid,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    points = points,
    friendCode = friendCode,
    createdAtEpochMillis = createdAt,
)

// ── SharedItem ─────────────────────────────────────────────────────
fun SharedItemDto.toDomain(): SharedItem = SharedItem(
    id = id,
    authorUid = authorUid,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    period = runCatching { SummaryPeriod.valueOf(period) }.getOrDefault(SummaryPeriod.WEEKLY),
    headline = headline,
    message = message,
    points = points,
    completedTasks = completedTasks,
    imageUrl = imageUrl,
    createdAtEpochMillis = createdAt,
)

// ── Challenge ──────────────────────────────────────────────────────
fun ChallengeDto.toDomain(): Challenge = Challenge(
    id = id,
    title = title,
    description = description,
    type = ChallengeType.fromName(type),
    metricUnit = metricUnit.ifBlank { "points" },
    ownerUid = ownerUid,
    startAtEpochMillis = startAt,
    endAtEpochMillis = endAt,
    createdAtEpochMillis = createdAt,
)

fun Challenge.toDto(): ChallengeDto = ChallengeDto(
    id = id,
    title = title,
    description = description,
    type = type.name,
    metricUnit = metricUnit,
    ownerUid = ownerUid,
    startAt = startAtEpochMillis,
    endAt = endAtEpochMillis,
    createdAt = createdAtEpochMillis,
)

fun ChallengeParticipantDto.toDomain(): ChallengeParticipant = ChallengeParticipant(
    uid = uid,
    displayName = displayName,
    photoUrl = photoUrl,
    score = score,
    joinedAtEpochMillis = joinedAt,
)

// `PublicProfileDto.resolvedLevel()` used to live here. `C20` (#42, spec §5.2) deleted
// `publicProfiles.level`, so there is no stored value left to prefer over the computed
// one and the fallback became the whole function. Callers use `Leveling.levelForPoints`
// directly — see `SocialRepositoryImpl.toEntries`.
