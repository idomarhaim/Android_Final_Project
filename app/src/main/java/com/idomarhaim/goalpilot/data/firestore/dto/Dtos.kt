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
    /**
     * `ALL_DAY` | `DEADLINE` | `BLOCK` | `SPAN` — §2.2's rung, and the **discriminator** for
     * the three fields below (`#56`).
     *
     * Absent on every document written before `#56`, and absent means *this task has no
     * occurrence* — which is what every existing task genuinely is, so there is **no backfill
     * and nothing to migrate**. An unrecognised value reads as absent too: a rung this build
     * does not know is a rung whose miss semantics it cannot honour, and guessing one would
     * put a task into a state §2.2 does not define.
     */
    var occurrenceRung: String? = null,
    /**
     * When the occurrence starts — **ISO-8601 local text, not epoch millis**, and that is the
     * decision worth reading before changing it.
     *
     * §2.2's `ALL_DAY` is *"a day with no slot"* and `SPAN` is *"days, not hours"*. A day is
     * not an instant: stored as millis it becomes one, and then *which day it is* depends on
     * the zone of whichever device reads it back. A user who files an all-day task in Tel Aviv
     * and opens the app after a flight would find it on the previous day, and §2.3 derives
     * every temporal state from exactly this value — so the miss would move with the reader.
     *
     * So the two date rungs store `2026-08-22` and the two instant rungs store
     * `2026-08-22T06:00`. Both parse back to the same wall-clock the user typed, on any
     * device, which is what a commitment about *your* day has to mean.
     *
     * The rest of this DTO is epoch millis and stays that way: `createdAt` and `completedAt`
     * are records of **when something happened**, which genuinely is an instant. The
     * difference is real, not stylistic.
     */
    var occurrenceStart: String? = null,
    /**
     * When it ends — the last day for a `SPAN`, the slot's end for a `BLOCK`, and **absent for
     * `ALL_DAY` and `DEADLINE`**, whose ends are implied by their starts.
     *
     * Absent where the rung has no end rather than duplicated from the start: the domain's
     * `AllDay` and `Deadline` carry no end field at all, so writing one would create a stored
     * value with nothing to read it and nothing to keep it true.
     */
    var occurrenceEnd: String? = null,
    /**
     * `PROVISIONAL` | `SILENT` | `CONFIRMED` — §2.3's placement, **meaningful for `BLOCK`
     * alone** (§2.4: the other three rungs occupy no slot and cannot collide).
     *
     * Absent reads as `CONFIRMED`, which is honest for everything the app can create today: a
     * block a person typed is one they endorsed by typing it, and the agent that would write
     * `PROVISIONAL` is §3.7's proposed plan (`#24`) and does not exist. Written only when the
     * rung is `BLOCK`, so a stored value can never contradict a rung that has no placement.
     */
    var occurrencePlacement: String? = null,
    /**
     * §2.1's recurrence rule, or absent for a task that happens once (`#63`, §7.1).
     *
     * A **nested map** rather than four more flat fields, for the reason the four above are
     * flat and this is not: those four describe one value the task genuinely has, while this
     * describes a *different* object that happens to be stored here. Nesting it means
     * `repeatRule` is present or absent as a whole, so there is no state in which a stored
     * interval sits beside an absent unit and something has to decide what that means.
     *
     * Absent on every document written before `#63`, and absent means *does not repeat* —
     * which every existing task genuinely is, so there is **no backfill and nothing to
     * migrate**, exactly as with the four occurrence fields.
     */
    var repeatRule: RepeatRuleDto? = null,
    /**
     * Epoch millis before which generated instances are suppressed, or absent for *not paused*
     * (§7.1, whose name and `Long?` type are normative).
     *
     * ⚠️ **The one instant in the scheduling set, and it is the spec's choice rather than this
     * file's.** Everything else about a *when* is stored as local text, and
     * [occurrenceStart] explains at length why a **day** stored as millis moves between time
     * zones. A pause is not a day — it is a moment after which the task resumes — so millis is
     * defensible, but the comparison against a local `opensAt` still needs a zone, and
     * `TaskSchedule.occurrencesIn` therefore takes one as a parameter instead of reaching for
     * `systemDefault()` where nobody would see it.
     */
    var pausedUntil: Long? = null,
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

/**
 * §2.1's recurrence rule, as it is stored inside [TaskDto.repeatRule] (`#63`).
 *
 * ### `endKind` is a discriminator, not a third bound
 *
 * The domain's `RepeatEnd` is a sealed type with exactly one of three answers, and the wire
 * cannot nest a sealed type. So it names which answer it is and carries the payloads beside
 * it — and the mapper reads **only the field the discriminator names**, so a stray [endDate]
 * on an `AFTER_COUNT` rule is ignored rather than adjudicated. That is the same contract
 * [TaskDto.occurrenceEnd] already has with [TaskDto.occurrenceRung], and it is what keeps the
 * *"exactly one bound"* guarantee true after a round trip.
 *
 * An unrecognised [endKind] reads as `NEVER` for the reason `occurrenceRung` reads an
 * unrecognised rung as absent: a bound this build cannot honour is a bound it must not
 * pretend to, and *"it does not stop"* is the one reading that cannot silently truncate a
 * user's series.
 */
data class RepeatRuleDto(
    /** `DAY` | `WEEK` | `MONTH` | `YEAR`. Unrecognised or absent reads as *no rule at all*. */
    var unit: String? = null,
    /** How many [unit]s between instances. Below 1 is coerced to 1 — a 0 would never advance. */
    var interval: Int = 1,
    /** `MONDAY`…`SUNDAY`, for a `WEEK` rule. Empty means the anchor's own weekday. */
    var weekdays: List<String> = emptyList(),
    /** `NEVER` | `ON_DATE` | `AFTER_COUNT`. Absent reads as `NEVER`. */
    var endKind: String? = null,
    /** ISO-8601 `2026-09-01`, read only when [endKind] is `ON_DATE`. Inclusive. */
    var endDate: String? = null,
    /** Instances including the first, read only when [endKind] is `AFTER_COUNT`. */
    var endCount: Int? = null,
)

/**
 * **One occurrence, stored** — `users/{uid}/occurrences/{id}` (`#63`, spec §2.1 and §7.1).
 *
 * ### Only the instances somebody touched are here
 *
 * A repeating task generates its instances from [TaskDto.repeatRule] and stores none of them.
 * A document appears the moment an instance stops being what the rule says — it was moved,
 * skipped, done, or given a Google event id — which is what §2.1 means by needing *both* a rule
 * and documents, and what stops `R18`'s fortnightly flowers becoming 26 documents a year.
 *
 * ### The four *when* fields are the same four, on purpose
 *
 * [rung], [start], [end] and [placement] carry exactly what [TaskDto]'s four occurrence fields
 * carry, in the same encoding, read and written by the same codec. `#56` shipped that codec and
 * §7.1's migration is *"the four task fields become the first occurrence in it"* — which is a
 * copy only while both sides spell it the same way. See [TaskDto.occurrenceStart] for why a
 * *day* is stored as local ISO text and never as millis.
 *
 * ### [outcome] is what the clock cannot tell you, and nothing else
 *
 * §2.3 derives every temporal state from the dates and a `now`, and forbids storing any of
 * them. So there is no `missed`, no `overdue`, no `status`. What is here is whether the person
 * **did** it and whether they deliberately **did not** — two facts no derivation can reach.
 */
data class OccurrenceDto(
    @DocumentId var id: String = "",
    /** The task this is an occurrence of. The link is a field because the collection is flat. */
    var taskId: String = "",
    /** `ALL_DAY` | `DEADLINE` | `BLOCK` | `SPAN` — §2.2's rung. See [TaskDto.occurrenceRung]. */
    var rung: String? = null,
    /** ISO-8601 local text — a date for the day rungs, a date-time for the instant ones. */
    var start: String? = null,
    /** The slot's end or the span's last day; absent for `ALL_DAY` and `DEADLINE`. */
    var end: String? = null,
    /** `PROVISIONAL` | `SILENT` | `CONFIRMED`, for `BLOCK` alone. Absent reads as `CONFIRMED`. */
    var placement: String? = null,
    /**
     * ISO-8601 date of the **rule-generated instance this document stands in for**, or absent
     * for an occurrence belonging to no series.
     *
     * ⚠️ Not the same as [start] once the instance has been moved, and that difference is the
     * entire recurrence mechanism: it is what lets the next expansion recognise a moved Tuesday
     * as still being Monday's instance rather than generating Monday again beside it.
     */
    var seriesDate: String? = null,
    /** `PLANNED` | `DONE` | `SKIPPED`. Absent reads as `PLANNED`. */
    var outcome: String? = null,
    /** When [outcome] happened, epoch millis. Read only for `DONE` and `SKIPPED`. */
    var outcomeAt: Long? = null,
    /**
     * The Google Calendar event mirroring this occurrence (§2.6, §2.7, `#61`), or absent.
     *
     * §2.7 requires clearing it to be expressible on its own — *"a disappearance never deletes
     * and never re-creates"*; the occurrence keeps its date and loses the id.
     */
    var googleEventId: String? = null,
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

/**
 * `challenges/{challengeId}` — spec §6 (`C14` #23).
 *
 * **Four fields, two eras.** [measureKind] + [measureWord] are the live shape; [type] and
 * [metricUnit] are the pre-§6 one and are read-only here — nothing writes them any more.
 * See [resolvedMeasure] for what the migration recovers and what it deliberately refuses to.
 */
data class ChallengeDto(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    /**
     * The measure's closed kind — a `MeasureKind` name, or null on a pre-§6 document.
     * App logic, translated at the point of display (§1.3).
     */
    var measureKind: String? = null,
    /** The user's own word for the unit — user content, never translated (§5.1 `C15b`). */
    var measureWord: String? = null,
    /**
     * **Legacy, read-only.** `ChallengeType` was purely presentational and §6 deleted it;
     * this field survives on documents written before that, and is the only thing that can
     * still say a 2026-08 "August Steps Race" counted *steps* rather than *kilometres*.
     */
    var type: String? = null,
    /**
     * **Legacy, read-only.** Free text with `Goal.unit`'s exact disease (`A3`), defaulted to
     * `"points"` — which is the default that produced #23 in the first place.
     */
    var metricUnit: String? = null,
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
     * Where [score] came from — a `ScoreSource` name. **Server-written**, like [score]
     * itself: `firestore.rules` pins it against every client write, because a participant
     * who could set their own label could write `DERIVED` over a number they typed, and the
     * label would then say the one thing it exists to deny.
     */
    var scoreSource: String? = null,
    /**
     * When the typed number behind [score] was reported, or `0`. **Server-written**, and
     * zero for a derived score — see `ChallengeParticipant.reportedAtEpochMillis`.
     */
    var reportedAt: Long = 0L,
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
