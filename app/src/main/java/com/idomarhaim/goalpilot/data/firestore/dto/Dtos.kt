package com.idomarhaim.goalpilot.data.firestore.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore data-transfer objects. They are mutable [var]s with defaults and a
 * no-arg constructor so Firestore's reflective (de)serialization works reliably.
 * [DocumentId] auto-populates the id from the document path on read and is
 * excluded from the written payload.
 */

data class GoalDto(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "OTHER",
    /**
     * `USER` | `AI_SUGGESTED` | `NONE` — §1.1's intrinsic marker, and the field `#6`'s silent
     * filing is only legal because of.
     *
     * **Three states on the wire, and they are not the domain's three.** The domain says
     * *instrumental* with a `null` [com.idomarhaim.goalpilot.domain.model.Goal.declaredBy];
     * storage cannot, because §7.1 makes an **absent** field mean something else entirely —
     * *"backfill `UNKNOWN`: nothing records who made the existing goals, and the migration
     * must not pretend otherwise"*. So absence is already spoken for, and a demoted goal needs
     * a value of its own or its demotion does not survive a round trip.
     *
     * | Stored | Reads as | Means |
     * |---|---|---|
     * | *(absent)* | `UNKNOWN` | written before `#6`; nobody recorded an author |
     * | `"USER"` | `USER` | Ido wrote it, or kept one the sorter proposed |
     * | `"AI_SUGGESTED"` | `AI_SUGGESTED` | the sorter proposed it; **pending** |
     * | `"NONE"` | `null` | the marker was deliberately dropped — instrumental |
     *
     * `"NONE"` is **not** the fourth enum value §1.1 rejects. That rejection is about the
     * *domain*, where a `NONE` constant would be a stored judgement duplicating a null that
     * already says it — and the domain still has exactly three values plus null. This is a
     * **wire** distinction between *never written* and *written as none*, which no absence can
     * carry, and it is the same shape as [unit]'s null below: one value whose whole job is to
     * mean *this document predates nothing*.
     *
     * **No backfill write runs.** Absence is mapped to `UNKNOWN` on read, and the field is
     * written on the next save of the goal like every other migrating field here — safe today
     * because nothing in the app can create an instrumental objective: `parentIds` does not
     * exist on this DTO and no screen renders a sub-objective, so absence provably cannot
     * already mean *milestone*. `C16` #37 changes that, and owes a real backfill when it does.
     */
    var declaredBy: String? = null,
    /**
     * **Legacy, read-only.** The single life-area id goals carried before
     * `PRODUCT_v0.3` §1.2 made the edge plural. Kept on the DTO purely so an
     * un-migrated document still resolves to `[current]` on read; every write
     * emits null here and the id in [lifeAreaIds] instead, so a document is
     * migrated the first time it is saved and the two can never disagree.
     */
    var lifeAreaId: String? = null,
    /**
     * Ids of the user's [com.idomarhaim.goalpilot.domain.model.LifeArea]s this
     * goal serves; absent on documents written before §1.2, where [lifeAreaId]
     * backfills it. Empty means unfiled.
     */
    var lifeAreaIds: List<String>? = null,
    /**
     * Automatic data source this goal belongs to, e.g. `"hc:goal:steps"`; absent on
     * older docs and on goals nobody syncs into. Additive — a document written
     * before #47 simply reads back null and gets stamped on the next sync.
     */
    var healthSourceKey: String? = null,
    var targetValue: Double = 100.0,
    // No `currentValue`. It stopped being a stored aggregate in #49 (§7.1: "stops
    // being stored — a sum over entries"), and the field is absent rather than
    // retained-and-ignored on purpose: `upsertGoal` writes with `set()`, so a
    // retained field would be re-persisted on every save and a *kept* stale number
    // is exactly the second number that quietly disagrees. Firestore ignores
    // document fields a DTO has no property for, so documents written before this
    // still deserialize; their orphaned `currentValue` is dropped the next time the
    // goal is saved and read by nobody in the meantime.
    /**
     * §1.3's measure, as §7.1's two fields: the closed kind by name, and the
     * user's free word. Both absent means the goal measures nothing, which is
     * §1.3's default; a word with no kind is the migration's half-way state
     * (`resolvedMeasure` has the whole rule).
     */
    var measureKind: String? = null,
    var measureWord: String? = null,
    /** §1.3's per-goal input mode by name; absent reads as `NUMBER`. */
    var inputMode: String? = null,
    /**
     * ⚠️ **Legacy free-text unit — read on migration, never written** (§7.1:
     * *`measureKind` + `measureWord` replaces free-text `unit`*).
     *
     * **Nullable, and the default moved from `"%"` to `null` on purpose.** With
     * the old default, a document written *after* this change — which carries no
     * `unit` field at all — would deserialize to `"%"` and be indistinguishable
     * from a real pre-migration percent goal, so every new goal would arrive
     * looking like something the migration had to rescue. Null is the only value
     * that means *this document predates nothing*.
     */
    var unit: String? = null,
    var colorHex: String = "",
    var deadline: Long? = null,
    var archived: Boolean = false,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,
) {
    companion object {
        /**
         * What [declaredBy] holds when the marker has been **dropped** — §1.1's lossless
         * demotion, spelled out on the wire because absence already means *predates `#6`*.
         *
         * Deliberately not a member of `DeclaredBy`: the domain says *instrumental* with a
         * null, and adding a fourth constant there would be the stored judgement §1.1 rejects.
         * `DeclaredBy.fromName` returns null for this string, which is exactly the point.
         */
        const val DECLARED_BY_NONE = "NONE"
    }
}

/**
 * One edge from a task to an objective, on the wire — §1.5, `#55`.
 *
 * A nested POJO rather than a raw `Map`, so Firestore's reflective (de)serialization
 * produces the same `var`-with-defaults shape the rest of this file uses and a missing
 * `contribution` arrives as `null` rather than as an absent key the caller has to probe for.
 */
data class GoalEdgeDto(
    var goalId: String = "",
    /** `null` means *undeclared*, which contributes nothing to the measure (§1.5). */
    var contribution: Double? = null,
)

data class TaskDto(
    @DocumentId var id: String = "",
    /**
     * **A stored projection of `goalEdges[0].goalId`, kept only so Firestore can index it.**
     *
     * `observeTasks` filters with `whereEqualTo("goalId", …)`, and an array of maps cannot be
     * queried on one of its members' fields. So this is written from the edge list on every
     * write and is never the truth — `Task.goalId` reads the edge, not this. It is also what
     * a **pre-`#55` document** carries instead of an edge list, which is the other half of
     * why it stays: see [toDomain].
     */
    var goalId: String? = null,
    var title: String = "",
    /**
     * The objectives this task serves, each with its own contribution (§1.5).
     *
     * Absent on every document written before `#55`; such a document is read into a
     * one-element edge list built from [goalId] and [progressContribution], with **no
     * migrating write required to see it**.
     */
    var goalEdges: List<GoalEdgeDto> = emptyList(),
    /** `LIGHT` | `ROUTINE` | `DEMANDING` (§1.4, `#55`). Absent reads as `ROUTINE`, which is ×1.0. */
    var difficulty: String? = null,
    /**
     * ⚠️ **Legacy — read on the way in, written as `null` on the way out.**
     *
     * The stored point value of a task written before `#55`. Points are now derived from
     * `minutes × difficulty` (§1.4), so nothing computes from this any more except the
     * migration read path: a *completed* legacy task has no other record of the effort it
     * banked, and [toDomain] reconstructs one from it losslessly
     * (`TaskDuration.legacyMinutesFromPoints`).
     *
     * Nulled by every write, following the same migrating-write rule `GoalDto.lifeAreaId` and
     * `GoalDto.unit` already follow: leaving a superseded field populated beside the thing
     * that replaced it is §0.3's *second number that quietly disagrees*.
     */
    var points: Int? = null,
    /**
     * ⚠️ **Legacy — the pre-`#55` completion flag.** The fact now lives in its own document at
     * `users/{uid}/completionFacts/{taskId}` ([CompletionFactDto]); see [toDomain] for how a
     * document carrying this instead is read, and `TaskRepositoryImpl.setDone` for why the
     * tick clears it in the same batch that writes the fact.
     */
    var done: Boolean? = null,
    var source: String = "MANUAL",
    /**
     * ⚠️ **Legacy — the pre-`#55` contribution, which lived on the task instead of on the
     * edge** (§1.5).
     *
     * Nullable **and defaulting to null**, which is load-bearing: it is the only way to tell
     * a document that stored `1.0` from one that never had the field. Everything written by
     * this app before `#55` stored it, so every such document keeps its number verbatim and
     * no goal's progress moves on upgrade. A new task's edge declares nothing instead.
     */
    var progressContribution: Double? = null,
    /** Duration in minutes; absent on tasks created before it existed. */
    var estimatedMinutes: Int? = null,
    /**
     * `USER` | `AI` | `UNKNOWN` — where [estimatedMinutes] came from (#9, §1.4).
     *
     * Absent on every task written before #9, and absent reads as `UNKNOWN`, so
     * **there is no migration write**: the honest provenance of a legacy row is
     * *not recorded*, and nothing is gained by stamping a guess onto it. Safe
     * because no code path let a person type a duration before this ticket, so no
     * stored value can be a sticky one.
     */
    var durationSource: String? = null,
    var createdAt: Long = 0L,
    /** ⚠️ **Legacy — the pre-`#55` completion stamp.** See [done]. */
    var completedAt: Long? = null,
)

/**
 * A banked completion — `users/{uid}/completionFacts/{taskId}`, §1.4, `#55`.
 *
 * The document id **is** the task id, which is what makes the tick and the untick a `set` and
 * a `delete` of one known path rather than a query: no read-then-write, nothing to
 * accumulate, and *"an untick removes exactly the fact it added"* holds by construction.
 *
 * It carries its own [minutes] and [difficulty] rather than pointing at the task's, because
 * §1.4 banks the **inputs**: correcting an estimate afterwards must not re-price what was
 * already earned. It is also what lets `functions/src/projection.ts` total a user's points by
 * reading this collection alone.
 */
data class CompletionFactDto(
    @DocumentId var id: String = "",
    var completedAt: Long = 0L,
    var minutes: Int = 30,
    var difficulty: String = "ROUTINE",
)

data class LifeAreaDto(
    @DocumentId var id: String = "",
    var name: String = "",
    var colorHex: String = "",
    var iconKey: String = "flag",
    var sortOrder: Int = 0,
    /** Google Tasks list this area mirrors, if it was synced from one. */
    var googleListId: String? = null,
    var archived: Boolean = false,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,
)

data class ProgressDto(
    @DocumentId var id: String = "",
    var goalId: String = "",
    var value: Double = 0.0,
    var note: String = "",
    var imageUrl: String? = null,
    var createdAt: Long = 0L,
    /** Identity of an imported reading (e.g. `hc:steps:2026-08-01`); null if logged by hand. */
    var sourceKey: String? = null,
)

data class UserDto(
    @DocumentId var uid: String = "",
    var displayName: String = "",
    var email: String = "",
    var photoUrl: String? = null,
    var points: Long = 0L,
    var friendCode: String = "",
    var createdAt: Long = 0L,
)

data class PublicProfileDto(
    @DocumentId var uid: String = "",
    var displayName: String = "",
    var photoUrl: String? = null,
    /**
     * **Written only by `functions/src/projection.ts`** (`C20` #42, spec §5.2).
     *
     * The one number on this document a reader cannot compute: it sums the completion
     * facts under `users/{uid}/tasks`, which `firestore.rules` keeps private to their
     * owner. `firestore.rules` refuses a client write that moves it.
     *
     * **There is no `level` field here any more, and that is the point.** It was a
     * stored function of *this* value in *this* document, so everyone who could read
     * it could already compute it; §5.2 deleted it outright and the client derives it
     * through `Leveling` where it is displayed. Its `resolvedLevel()` fallback went
     * with it — it could never fire, because both writers wrote at least 1.
     */
    var points: Long = 0L,
    /** Mirrored from the private user doc so friends can look this profile up. */
    var friendCode: String = "",
    /**
     * When the owner last wrote this row — the *as-of* stamp (#50, spec §5.3 §3).
     *
     * **Server-set, never device-set**, and that is the whole point: the reader of
     * this document is on a *different device* from its writer, so a writer whose
     * clock is wrong would caption a stale number with a time the reader has no way
     * to doubt. [ServerTimestamp] fills it in on a POJO write; a map write does the
     * same with `FieldValue.serverTimestamp()`.
     *
     * `null` on a row written before #50 shipped, and on a locally-pending write
     * the server has not yet confirmed — both mean *no stamp*, and the caption says
     * nothing rather than inventing one.
     */
    @ServerTimestamp var updatedAt: Timestamp? = null,
)

data class SharedItemDto(
    @DocumentId var id: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var authorPhotoUrl: String? = null,
    var period: String = "WEEKLY",
    var headline: String = "",
    var message: String = "",
    var points: Long = 0L,
    var completedTasks: Int = 0,
    var imageUrl: String? = null,
    var createdAt: Long = 0L,
)

data class ChallengeDto(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var type: String = "CUSTOM",
    var metricUnit: String = "points",
    var ownerUid: String = "",
    var startAt: Long = 0L,
    var endAt: Long = 0L,
    var createdAt: Long = 0L,
)

/**
 * `challenges/{challengeId}/participants/{uid}` — the document id **is** the
 * participant's uid, which is what the security rule matches on. Never write
 * this to a path whose id differs from the signed-in user; the rule rejects it.
 */
data class ChallengeParticipantDto(
    @DocumentId var uid: String = "",
    var displayName: String = "",
    var photoUrl: String? = null,
    var score: Double = 0.0,
    var joinedAt: Long = 0L,
    /**
     * When the owner last wrote this row — the *as-of* stamp (#50, spec §5.3 §3).
     *
     * A **different fact** from [joinedAt], which says when they entered and never
     * moves again; this says when the *score* last moved, which is what a reader
     * looking at somebody else's standing actually needs. See
     * [PublicProfileDto.updatedAt] for why it is server-set.
     */
    @ServerTimestamp var updatedAt: Timestamp? = null,
)
