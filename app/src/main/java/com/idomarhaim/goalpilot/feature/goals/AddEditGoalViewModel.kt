package com.idomarhaim.goalpilot.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    lifeAreaRepository: LifeAreaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle[Routes.ARG_GOAL_ID]

    private val _form = MutableStateFlow(GoalForm(isEdit = goalId != null))
    val form = _form.asStateFlow()

    /** The areas the goal can be filed under; empty until the user defines some. */
    val lifeAreas: StateFlow<List<LifeArea>> = lifeAreaRepository.observeLifeAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
                            lifeAreaIds = g.lifeAreaIds,
                            target = g.targetValue.toTrimmedString(),
                            measureKind = g.measure?.kind,
                            unit = g.measureWord,
                            inputMode = g.inputMode,
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
    /**
     * Toggles one life area on or off. Plural since §1.2 — a goal reaches many
     * areas — so this is a set membership flip rather than a single-choice
     * assignment; *unfiled* is what is left when the last one is turned off.
     */
    fun onLifeAreaToggle(areaId: String) = _form.update {
        it.copy(
            lifeAreaIds = if (areaId in it.lifeAreaIds) {
                it.lifeAreaIds - areaId
            } else {
                it.lifeAreaIds + areaId
            },
        )
    }

    /** The "None" chip: unfile the goal outright. */
    fun onClearLifeAreas() = _form.update { it.copy(lifeAreaIds = emptyList()) }
    fun onTargetChange(value: String) =
        _form.update { it.copy(target = value.filter { c -> c.isDigit() || c == '.' }, error = null) }
    fun onUnitChange(value: String) = _form.update { it.copy(unit = value) }

    /**
     * Picks what the goal counts, or clears it back to *no measure* — §1.3 makes
     * absence legal and the default, so "none" has to be reachable from the form
     * rather than only from never having chosen.
     *
     * Choosing [MeasureKind.PERCENT] here is what §7.1 means by `"%"` surviving
     * as a **chosen** measure: the choice is recorded in `measureKind`, which is
     * the thing a defaulted `"%"` never had.
     */
    fun onMeasureKindChange(value: MeasureKind?) = _form.update {
        it.copy(
            measureKind = value,
            // A kind picked onto a goal with no word yet gets that kind's example
            // word, so the commonest path — pick VOLUME, type 4, save — produces
            // a usable measure without a second decision. Anything the user has
            // already typed is left alone: the word is theirs (§1.3).
            unit = it.unit.ifBlank { value?.wordHint().orEmpty() },
        )
    }

    /** Which of §1.3's input modes this goal uses. */
    fun onInputModeChange(value: InputMode) = _form.update { it.copy(inputMode = value) }

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
                lifeAreaIds = current.lifeAreaIds,
                targetValue = target,
                // `currentValue` is not carried through the form any more (#49). It
                // is derived from the goal's entries and completed tasks, so the
                // edit screen has nothing to preserve and no target to clamp it to
                // — editing a goal's title cannot move its progress, which is what
                // the round-trip through this form could previously do.
                // No `"%"` fallback any more, and that deletion is the ticket.
                // The old one turned "I did not say" into "percent", which is how
                // a live goal called "Drink 4 Liters of Water Daily" came to read
                // `1/100 %`. `Measure.of` collapses *nothing chosen and nothing
                // typed* back to a single absent state (§1.3, `E6`).
                measure = Measure.of(current.measureKind, current.unit),
                inputMode = current.inputMode,
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
    /** Which life areas the goal serves; the empty list = unfiled (§1.2). */
    val lifeAreaIds: List<String> = emptyList(),
    val target: String = "100",
    /** §1.3's kind, or null for a goal that measures nothing — the default. */
    val measureKind: MeasureKind? = null,
    /** §1.3's free word. Named `unit` still because the text field is. */
    val unit: String = "",
    val inputMode: InputMode = InputMode.NUMBER,
    val createdAt: Long = 0L,
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

private fun Double.toTrimmedString(): String =
    if (this % 1.0 == 0.0) toLong().toString() else this.toString()
