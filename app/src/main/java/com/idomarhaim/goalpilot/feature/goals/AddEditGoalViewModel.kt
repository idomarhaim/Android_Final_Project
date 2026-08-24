package com.idomarhaim.goalpilot.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.GoalFiling
import com.idomarhaim.goalpilot.domain.model.GoalPlan
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.usecase.ApplyGoalPlanUseCase
import com.idomarhaim.goalpilot.domain.usecase.GoalPlanOutcome
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
    private val recommendations: RecommendationRepository,
    private val applyGoalPlan: ApplyGoalPlanUseCase,
    lifeAreaRepository: LifeAreaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle[Routes.ARG_GOAL_ID]

    private val _form = MutableStateFlow(GoalForm(isEdit = goalId != null))
    val form = _form.asStateFlow()

    /**
     * §3.7's draft, or [PlanState.Idle] when there is none.
     *
     * A separate flow from [form] because the two have different lifetimes: the form is the goal
     * and closes when it is saved, while the plan is proposed **after** the goal exists and the
     * user can leave it without leaving the goal. Folding it into `GoalForm` would put a
     * *"nothing the model decides may reach Firestore"* draft inside the object the save path
     * writes, which is the one place it must not be.
     */
    private val _plan = MutableStateFlow<PlanState>(PlanState.Idle)
    val plan = _plan.asStateFlow()

    /**
     * Whether closing the draft should also close the **screen**.
     *
     * `true` only for the plan offered automatically after a *create*, where the goal is already
     * written and the sheet is the last step. `false` for the button on the *edit* form, where
     * the user has unsaved edits behind the sheet — see [dismissPlan].
     *
     * A plain field rather than state: nothing renders it, and it is written before the draft
     * that reads it exists.
     */
    private var planFinishesScreen: Boolean = false

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
                            // A stored goal's category and filing are the USER's, whatever wrote
                            // them originally: they survived at least one pass through this form.
                            // So an edit never asks the model where the goal belongs -- see
                            // `fileSilently`.
                            categoryTouched = true,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = _form.update { it.copy(title = value, error = null) }
    fun onDescriptionChange(value: String) = _form.update { it.copy(description = value) }

    /**
     * Picks a category **and records that the user picked it**.
     *
     * [GoalForm.categoryTouched] is what stops the silent filing below overwriting a deliberate
     * choice. Without it the form's default (`HEALTH`) and a hand-picked `HEALTH` are the same
     * value, and the model's answer would win over the user's on every goal where they happen to
     * agree with the default — which is §0.7's asymmetry inverted.
     */
    fun onCategoryChange(value: GoalCategory) =
        _form.update { it.copy(category = value, categoryTouched = true) }

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
            lifingTouched = true,
        )
    }

    /**
     * The "None" chip: unfile the goal outright.
     *
     * This counts as **touching** the filing, and that is the point of the chip: choosing *none*
     * is a decision, not the absence of one, so the model does not then file the goal anyway.
     */
    fun onClearLifeAreas() = _form.update { it.copy(lifeAreaIds = emptyList(), lifingTouched = true) }
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
            val filed = fileSilently(current)
            val goal = Goal(
                id = filed.id,
                title = filed.title.trim(),
                description = filed.description.trim(),
                category = filed.category,
                // §1.1's intrinsic marker, and this form is the one place that can set it
                // honestly (#6): a goal that has been through *New goal* / *Edit goal* is one
                // Ido wrote or read and saved, which is exactly the consent §0.7 asks for
                // before an intrinsic edge is asserted. It is set on every save rather than
                // only on create, so keeping a suggestion by editing it is a declaration too
                // — and so that an edit can never silently strip a marker back to UNKNOWN,
                // which is what leaving the field to its default here would do.
                //
                // ⚠️ **The silent filing above does not weaken this, and the reason is §0.7's
                // own split.** What the model chose is the goal's life area and category —
                // *instrumental* structure, one tap to change. The goal's **existence** is
                // still the user's assertion: they typed the title and pressed the button.
                declaredBy = DeclaredBy.USER,
                lifeAreaIds = filed.lifeAreaIds,
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
                measure = Measure.of(filed.measureKind, filed.unit),
                inputMode = filed.inputMode,
                colorHex = filed.category.defaultColorHex,
                createdAtEpochMillis = filed.createdAt,
            )
            when (val result = goalRepository.upsertGoal(goal)) {
                is Resource.Success -> onSaved(goal.copy(id = result.data))
                is Resource.Error -> _form.update { it.copy(isSaving = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    /**
     * Asks the model where this goal belongs, and applies the answer **silently** — §0.7, and
     * Ido's ask of 2026-08-24: *"assign it to the relevant life area, on the basis of the goal's
     * name alone, without my having to say what the life area is."*
     *
     * ## It only ever fills a silence
     *
     * Three guards, and each one exists because the alternative overwrites a decision:
     *
     * - **Never on an edit.** A stored goal's filing survived a save; it is the user's.
     * - **Never over a chosen life area**, including the *None* chip — choosing *none* is a
     *   decision, which is why [GoalForm.lifingTouched] and not `lifeAreaIds.isEmpty()` is the
     *   test.
     * - **Never over a chosen category.** The form's default is `HEALTH`, so *"they left it
     *   alone"* and *"they picked HEALTH"* are the same value and only [GoalForm.categoryTouched]
     *   tells them apart.
     *
     * ## It cannot fail the save
     *
     * `fileGoal` never returns an error — a call that did not happen is an empty [GoalFiling] and
     * the goal is written exactly as the user left it, unfiled if that is what they left. So this
     * is `await`ed on the save path rather than fired alongside it: it costs one round trip on a
     * button the user pressed once, and doing it afterwards would mean writing the goal twice.
     */
    private suspend fun fileSilently(current: GoalForm): GoalForm {
        if (current.isEdit) return current
        if (current.lifingTouched && current.categoryTouched) return current

        val areas = lifeAreas.value
        val filing: GoalFiling = when (val result = recommendations.fileGoal(current.title.trim(), areas)) {
            is Resource.Success -> result.data
            else -> GoalFiling()
        }
        if (filing.isEmpty) return current

        val filed = current.copy(
            lifeAreaIds = if (current.lifingTouched) {
                current.lifeAreaIds
            } else {
                // RESOLUTION, not validation (§3.4): an id naming no area of the user's simply
                // finds nothing and the goal stays unfiled. There is no rule here that could
                // disagree with the Cloud Function's, because there is no rule.
                listOfNotNull(areas.firstOrNull { it.id == filing.lifeAreaId }?.id)
            },
            category = if (current.categoryTouched) current.category else filing.category ?: current.category,
        )
        // Shown, not asked (§0.4). The chips update under the user's eyes on the way out, so a
        // filing they disagree with is one tap from being fixed and is never a surprise found
        // later in the time chart.
        _form.update { filed.copy(isSaving = true) }
        return filed
    }

    /**
     * The goal is written. For a **new** goal, offer §3.7's plan before leaving the screen.
     *
     * An edit finishes here exactly as it always did: a plan is an offer about work that does not
     * exist yet, and re-offering one every time a title is corrected would be the *"over-eager
     * agent"* §2.3 names. The plan is still reachable on an existing goal — [requestPlan] is
     * bound to a button on the edit form — it is just never automatic there.
     */
    private fun onSaved(goal: Goal) {
        if (_form.value.isEdit) {
            _form.update { it.copy(isSaving = false, saved = true) }
            return
        }
        _form.update { it.copy(id = goal.id, isSaving = false) }
        requestPlan(goal, finishesScreen = true)
    }

    /**
     * Asks for §3.3 B's plan and holds it as a draft.
     *
     * Nothing is written by this, and nothing is written until [applyPlan]. That gap **is** the
     * feature: §3.7's *"nothing the model decides here may reach Firestore without passing his
     * eyes"*.
     */
    fun requestPlan(goal: Goal = formAsGoal(), finishesScreen: Boolean = false) {
        planFinishesScreen = finishesScreen
        _plan.value = PlanState.Loading
        viewModelScope.launch {
            _plan.value = when (val result = recommendations.planGoal(goal)) {
                is Resource.Success ->
                    // An empty plan is REPORTED, not treated as a dismissal. §0.4: the user
                    // pressed a button, so *"the model had nothing to propose"* is a sentence
                    // they are owed — and it is a different sentence from *"the call failed"*,
                    // which is why the repository keeps the two apart.
                    if (result.data.isEmpty) PlanState.Empty else PlanState.Draft(result.data)
                is Resource.Error -> PlanState.Failed(result.message)
                Resource.Loading -> PlanState.Loading
            }
        }
    }

    /** Keep or drop one step. The only thing the draft gate actually needs. */
    fun onStepKeepToggle(index: Int) {
        val draft = _plan.value as? PlanState.Draft ?: return
        _plan.value = draft.copy(
            plan = draft.plan.copy(
                steps = draft.plan.steps.map {
                    if (it.index == index) it.copy(keep = !it.keep) else it
                },
            ),
        )
    }

    /**
     * Writes the kept steps, then leaves the screen.
     *
     * [ApplyGoalPlanUseCase] does the writing and asks the calendar to catch up; the only thing
     * decided here is what the user sees when part of it did not land. A partial write says so
     * and **stays on the sheet**: the steps are in the draft, and closing over them would leave
     * the user with a goal half-planned and nothing to press.
     */
    fun applyPlan() {
        val draft = _plan.value as? PlanState.Draft ?: return
        if (draft.plan.kept.isEmpty()) {
            dismissPlan()
            return
        }
        _plan.value = draft.copy(isApplying = true)
        viewModelScope.launch {
            val outcome = applyGoalPlan(draft.plan)
            if (outcome.isCompleteSuccess) {
                _plan.value = PlanState.Idle
                // Same rule as `dismissPlan`: a plan applied from the edit form has written its
                // tasks, but the user's unsaved edits to the GOAL are still on screen and theirs.
                if (planFinishesScreen) _form.update { it.copy(saved = true) }
            } else {
                _plan.value = draft.copy(isApplying = false, outcome = outcome)
            }
        }
    }

    /**
     * Close the draft without writing anything.
     *
     * ⚠️ **Whether that also leaves the screen depends on why the draft was open, and getting
     * this wrong loses the user's work.** After a *create* the goal is written and the sheet is
     * the last step, so closing it finishes the screen — that is the flow Ido asked for. On the
     * *edit* form the plan is a button the user pressed **without having saved**, so finishing
     * the screen there would discard every edit they had typed. `planFinishesScreen` is the only
     * thing that tells the two apart; the plan's own state cannot, because it is identical in
     * both cases.
     */
    fun dismissPlan() {
        _plan.value = PlanState.Idle
        if (planFinishesScreen) _form.update { it.copy(saved = true) }
    }

    /**
     * The form as the [Goal] the plan call describes.
     *
     * Only ever the **prompt's** input — title, description and §1.3's word — never a document.
     * It exists so the edit form can ask for a plan without re-reading the goal it is already
     * showing.
     */
    private fun formAsGoal(): Goal {
        val f = _form.value
        return Goal(
            id = f.id,
            title = f.title.trim(),
            description = f.description.trim(),
            category = f.category,
            lifeAreaIds = f.lifeAreaIds,
            measure = Measure.of(f.measureKind, f.unit),
        )
    }
}

/**
 * §3.7's draft, as the four states the screen can actually be in.
 *
 * [Empty] and [Failed] are deliberately **two** states and not one: the first is the model
 * answering *"I have nothing to propose"* and the second is nothing answering at all. §0.4 says
 * to speak about a failure the user can act on, and the two are acted on differently — one is
 * retried, the other is not.
 */
sealed interface PlanState {

    /** No plan asked for, or the draft is closed. */
    data object Idle : PlanState

    /** The call is out. */
    data object Loading : PlanState

    /** The model answered, and had nothing to propose. */
    data object Empty : PlanState

    /** Nothing answered. [message] is the failure, and the sheet offers a retry. */
    data class Failed(val message: String?) : PlanState

    /** A plan is on the table. Nothing is written until it is confirmed. */
    data class Draft(
        val plan: GoalPlan,
        val isApplying: Boolean = false,
        /** Set only when a write was attempted and did not fully land. */
        val outcome: GoalPlanOutcome? = null,
    ) : PlanState
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
    /**
     * Whether the user has chosen a category, as opposed to leaving the default.
     *
     * Not derivable from [category], and that is the whole reason it exists: the default is
     * `HEALTH`, so *"they left it alone"* and *"they picked HEALTH"* are the same value. Without
     * this flag the silent filing would overwrite a deliberate `HEALTH` with the model's answer
     * on exactly the goals where the two happen to agree.
     */
    val categoryTouched: Boolean = false,
    /**
     * Whether the user has said anything about the goal's life areas — **including saying
     * *none***.
     *
     * `lifeAreaIds.isEmpty()` cannot answer this: an empty list is both *"nobody said"* and
     * *"unfiled, deliberately"*, and §1.2 makes the second a real choice rather than a gap. The
     * *None* chip sets this, which is what makes it mean something.
     */
    val lifingTouched: Boolean = false,
)

private fun Double.toTrimmedString(): String =
    if (this % 1.0 == 0.0) toLong().toString() else this.toString()
