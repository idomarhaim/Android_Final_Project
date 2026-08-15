package com.idomarhaim.goalpilot.data.firestore.dto

import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.Leveling
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.model.User

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

fun GoalDto.toDomain(): Goal = Goal(
    id = id,
    title = title,
    description = description,
    category = GoalCategory.fromName(category),
    lifeAreaIds = resolvedLifeAreaIds(),
    healthSourceKey = healthSourceKey?.takeIf { it.isNotBlank() },
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
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
    currentValue = currentValue,
    unit = unit,
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

// ── PublicProfile → level convenience ──────────────────────────────
fun PublicProfileDto.resolvedLevel(): Int =
    if (level > 0) level else Leveling.levelForPoints(points)
