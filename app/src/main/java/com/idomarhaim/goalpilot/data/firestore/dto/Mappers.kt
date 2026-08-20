package com.idomarhaim.goalpilot.data.firestore.dto

import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric

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
fun TaskDto.toDomain(): Task = Task(
    id = id,
    goalId = goalId,
    title = title,
    points = points,
    isDone = done,
    source = TaskSource.fromName(source),
    progressContribution = progressContribution,
    estimatedMinutes = TaskDuration.sanitize(estimatedMinutes),
    // Absent, unknown and misspelled all read as UNKNOWN — see TaskDto.durationSource
    // for why a legacy row is left absent rather than back-filled.
    durationSource = DurationSource.fromName(durationSource),
    createdAtEpochMillis = createdAt,
    completedAtEpochMillis = completedAt,
)

fun Task.toDto(): TaskDto = TaskDto(
    id = id,
    goalId = goalId,
    title = title,
    points = points,
    done = isDone,
    source = source.name,
    progressContribution = progressContribution,
    estimatedMinutes = TaskDuration.sanitize(estimatedMinutes),
    durationSource = durationSource.name,
    createdAt = createdAtEpochMillis,
    completedAt = completedAtEpochMillis,
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
