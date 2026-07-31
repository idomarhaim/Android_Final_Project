package com.idomarhaim.goalpilot.data.firestore.dto

import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.Leveling
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.model.User

// ── Goal ───────────────────────────────────────────────────────────
fun GoalDto.toDomain(): Goal = Goal(
    id = id,
    title = title,
    description = description,
    category = GoalCategory.fromName(category),
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
    createdAt = createdAtEpochMillis,
    completedAt = completedAtEpochMillis,
)

// ── ProgressEntry ──────────────────────────────────────────────────
fun ProgressDto.toDomain(): ProgressEntry = ProgressEntry(
    id = id,
    goalId = goalId,
    value = value,
    note = note,
    imageUrl = imageUrl,
    createdAtEpochMillis = createdAt,
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

// ── PublicProfile → level convenience ──────────────────────────────
fun PublicProfileDto.resolvedLevel(): Int =
    if (level > 0) level else Leveling.levelForPoints(points)
