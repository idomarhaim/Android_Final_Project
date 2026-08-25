package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import javax.inject.Inject

/**
 * Points a challenge at **Health Connect**, without the user authoring anything.
 *
 * Ido, 2026-08-25:
 *
 * > *"if I make a steps competition, there should also be an option to pull the logs
 * > straight into the CHALLENGE and not only through a personal GOAL of mine"*
 *
 * He named the conflict with §6 himself and ruled that the new instruction wins, so this
 * exists. What it delivers is exactly the thing he asked for — **a choice that is not a
 * goal** — and the sections below say honestly what it does underneath and why that part
 * was never negotiable.
 *
 * ### §6 said the opposite, and here is what survived it
 *
 * §6's rule was *"a challenge scores from nothing of its own: it scores from each
 * participant's goal"*, and its reason was that the app had been keeping **two
 * representations of the same walk** — which is what `R1` actually was. Ido has overruled
 * the *product* half of that: Health Connect is now a first-class option in the picker.
 *
 * The *engineering* half is not a preference and did not move:
 *
 * > **The scoring Function runs in the cloud and cannot read Health Connect.** Health
 * > Connect is an on-device API. The only way a reading has ever reached Firestore — and
 * > the only way it can — is [SyncHealthDataUseCase] writing a `ProgressEntry` against a
 * > goal, which `projectChallengeScoreOnProgress` then sums.
 *
 * So a *literally* direct pipe would mean the app writing the same readings into Firestore
 * **a second time**, under the challenge, to be summed by a second code path that can
 * disagree with the first. That is `R1` rebuilt, and it would show up as a challenge and a
 * goal reporting different step counts for the same day.
 *
 * **What this does instead:** find the canonical Health-Connect-owned goal for the metric,
 * create it if the user has never synced, and link the challenge to it. The user picks
 * *"Steps · from Health Connect"* and is done. One representation, one number, no
 * authoring.
 *
 * ### Why `healthSourceKey` and not the category or the word
 *
 * [HealthMetric.goalSourceKey] is an identity **the user cannot edit** — `"hc:goal:steps"`.
 * Matching on [Goal.category] instead is exactly `#47`: the category is a chip the user can
 * change, one edit orphaned the goal, and the next sync created a duplicate. Matching on
 * the measure **word** is worse still, because §1.3 makes the word user content in the
 * user's own language, so a Hebrew user's `"צעדים"` goal would be missed and a second one
 * made beside it.
 *
 * ### It is deliberately not idempotent about ARCHIVED goals
 *
 * An archived Health Connect goal is one the user has put away, and `canBeScoredFrom`
 * already refuses to score from it. Reviving it silently would resurrect something they
 * chose to retire, so a fresh one is created instead — the same judgement
 * [BuildHealthProposalsUseCase] makes when it looks for a home for a reading.
 */
class LinkChallengeToHealthUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val challengeRepository: ChallengeRepository,
) {

    /**
     * Links [challenge] to [metric]'s Health Connect goal, creating that goal if the user
     * does not have one yet.
     *
     * [goals] is passed in rather than read here because the caller already observes the
     * user's goals for the picker beside this option — re-reading them would be a second
     * source for the same list, and the two could disagree within one sheet.
     */
    suspend operator fun invoke(
        challenge: Challenge,
        metric: HealthMetric,
        goals: List<Goal>,
    ): Resource<Unit> {
        // Stated rather than assumed. A caller that offered this option for a challenge of
        // the wrong kind would produce a goal that cannot score it, and `canBeScoredFrom`
        // would then refuse silently -- the user would see a link that never moves.
        if (challenge.measure?.kind != metric.measureKind) {
            return Resource.Error("Health Connect does not track what this challenge counts")
        }

        val existing = goals.firstOrNull {
            it.healthSourceKey == metric.goalSourceKey && !it.isArchived
        }

        val goalId = if (existing != null) {
            existing.id
        } else {
            // THE GOAL THE SYNC WOULD HAVE MADE, MADE EARLY.
            //
            // Every field here is `HealthMetric`'s own, so a goal created from this screen
            // is byte-for-byte the goal `SyncHealthDataUseCase` creates on its first run --
            // same title, same target, same category, same measure, same source key. That
            // is what stops this becoming a second way to own the same metric: the next
            // sync finds THIS goal by its key and tops it up, rather than making its own.
            val created = goalRepository.upsertGoal(
                Goal(
                    title = metric.defaultGoalTitle,
                    // The metric's own unit word, not the challenge's. The challenge's word
                    // is whatever its owner typed and may be in another language; this goal
                    // belongs to the sync, which has always known what it counts (§1.3).
                    measure = Measure(kind = metric.measureKind, word = metric.unit),
                    targetValue = metric.defaultGoalTarget,
                    category = metric.category,
                    healthSourceKey = metric.goalSourceKey,
                    // The user chose Health Connect on purpose, so they declared it -- the
                    // same value the goal editor stamps (§1.1, `#6`). Not `AI_SUGGESTED`
                    // and not `UNKNOWN`: nobody guessed, and nothing here is a proposal.
                    declaredBy = DeclaredBy.USER,
                ),
            )
            when (created) {
                is Resource.Success -> created.data
                is Resource.Error -> return created
                Resource.Loading -> return Resource.Error("Could not create the goal")
            }
        }

        return challengeRepository.linkGoal(challenge.id, goalId)
    }
}
