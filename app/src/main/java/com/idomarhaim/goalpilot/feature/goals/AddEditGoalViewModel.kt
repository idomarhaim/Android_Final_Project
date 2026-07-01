package com.idomarhaim.goalpilot.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle[Routes.ARG_GOAL_ID]

    private val _form = MutableStateFlow(GoalForm(isEdit = goalId != null))
    val form = _form.asStateFlow()

    init {
        if (goalId != null) {
            viewModelScope.launch {
                goalRepository.observeGoal(goalId).first()?.let { g ->
                    _form.update {
                        it.copy(
                            id = g.id,
                            title = g.title,
                            description = g.description,
                            category = g.category,
                            target = g.targetValue.toTrimmedString(),
                            unit = g.unit,
                            currentValue = g.currentValue,
                            createdAt = g.createdAtEpochMillis,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = _form.update { it.copy(title = value, error = null) }
    fun onDescriptionChange(value: String) = _form.update { it.copy(description = value) }
    fun onCategoryChange(value: GoalCategory) = _form.update { it.copy(category = value) }
    fun onTargetChange(value: String) =
        _form.update { it.copy(target = value.filter { c -> c.isDigit() || c == '.' }, error = null) }
    fun onUnitChange(value: String) = _form.update { it.copy(unit = value) }

    fun save() {
        val current = _form.value
        if (current.title.isBlank()) {
            _form.update { it.copy(error = "Please enter a title") }
            return
        }
        val target = current.target.toDoubleOrNull()
        if (target == null || target <= 0.0) {
            _form.update { it.copy(error = "Target must be a number greater than 0") }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true, error = null) }
            val goal = Goal(
                id = current.id,
                title = current.title.trim(),
                description = current.description.trim(),
                category = current.category,
                targetValue = target,
                currentValue = current.currentValue.coerceIn(0.0, target),
                unit = current.unit.ifBlank { "%" },
                colorHex = current.category.defaultColorHex,
                createdAtEpochMillis = current.createdAt,
            )
            when (val result = goalRepository.upsertGoal(goal)) {
                is Resource.Success -> _form.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _form.update { it.copy(isSaving = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
}

data class GoalForm(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: GoalCategory = GoalCategory.HEALTH,
    val target: String = "100",
    val unit: String = "%",
    val currentValue: Double = 0.0,
    val createdAt: Long = 0L,
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

private fun Double.toTrimmedString(): String =
    if (this % 1.0 == 0.0) toLong().toString() else this.toString()
