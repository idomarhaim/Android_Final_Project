package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.firestore.dto.CompletionFactDto
import com.idomarhaim.goalpilot.data.firestore.dto.GoalEdgeDto
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import org.junit.Test

/**
 * `#55`'s data migration, **both directions** — `docs/PRODUCT_v0.3.md` §1.4 and §1.5.
 *
 * ### What moved
 *
 * Three facts changed where they live in one ticket, and every one of them has documents in
 * the live database written the old way:
 *
 * | Was, on the task document | Is now |
 * |---|---|
 * | `points: Int` | derived — `round(minutes / 3) × difficulty` |
 * | `progressContribution: Double` | `goalEdges[].contribution`, defaulting to **undeclared** |
 * | `done` + `completedAt` | a `CompletionFact`, in its own document |
 *
 * ### The posture, and why there is no backfill
 *
 * The spec's migration posture is *"additive with a readable half-way state"* (§7.1), and the
 * half-way state here is not a transitional inconvenience — it is what every account is in on
 * the day this deploys, for as long as the user leaves an old task alone. So the read path
 * has to be **lossless in both directions**, and this suite is where that claim is checked
 * rather than asserted:
 *
 * - a document written **before** `#55` reads back to the same points, the same contribution
 *   and the same completion it had;
 * - a document written **after** it does not lose contribution, and carries none of the four
 *   superseded fields — because a document holding two answers is the map's most-repeated
 *   finding, a second number that quietly disagrees.
 *
 * The lossless half rests on one arithmetic identity, and it is the reason the migration
 * needs no write at all: a legacy point value `p` reconstructs to `3p` minutes, which prices
 * back at `round(3p / 3) × 1.0 = p`. `TaskDurationTest` pins the identity itself; this pins
 * that the mapper actually uses it.
 */
class TaskScoringMigrationTest {

    // ── §1.5 — contribution moves onto the edge ──────────────────────

    @Test
    fun `a pre-migration document reads its stored contribution onto the edge, verbatim`() {
        // The decision this whole suite turns on. §1.5 calls `progressContribution`'s 1.0 a
        // SILENCE rather than a value, and new edges therefore declare nothing — but a
        // stored number is data, and re-reading it as `null` would zero the task half of
        // every existing goal's progress with no user action and no way back.
        val legacy = TaskDto(id = "t1", goalId = "g1", progressContribution = 1.0)

        assertThat(legacy.toDomain().goalEdges)
            .containsExactly(GoalEdge(goalId = "g1", contribution = 1.0))
    }

    @Test
    fun `a document that never had the field reads as undeclared, not as one`() {
        // Why `TaskDto.progressContribution` is nullable with a null default rather than
        // `Double = 1.0`. It is the ONLY way to tell "stored 1.0" from "never had the
        // field" — and with a non-null default the two are the same document.
        val fresh = TaskDto(id = "t1", goalId = "g1", progressContribution = null)

        assertThat(fresh.toDomain().goalEdges)
            .containsExactly(GoalEdge(goalId = "g1", contribution = null))
    }

    @Test
    fun `an edge list wins outright over the legacy pair`() {
        // Only reachable if a write left both behind. `goalEdges` is what this app writes,
        // so it is the one that decides — the same rule `GoalDto.lifeAreaIds` follows.
        val both = TaskDto(
            id = "t1",
            goalId = "stale",
            progressContribution = 99.0,
            goalEdges = listOf(GoalEdgeDto(goalId = "g1", contribution = 2.5)),
        )

        assertThat(both.toDomain().goalEdges)
            .containsExactly(GoalEdge(goalId = "g1", contribution = 2.5))
    }

    @Test
    fun `an unfiled legacy task has no edges rather than one edge to nothing`() {
        assertThat(TaskDto(id = "t1", goalId = null).toDomain().goalEdges).isEmpty()
        assertThat(TaskDto(id = "t1", goalId = "   ").toDomain().goalEdges).isEmpty()
    }

    @Test
    fun `a blank edge id is dropped rather than filed under the empty string`() {
        val junk = TaskDto(id = "t1", goalEdges = listOf(GoalEdgeDto(goalId = "  ")))

        assertThat(junk.toDomain().goalEdges).isEmpty()
    }

    // ── §1.4 — the completion, and what it was worth ─────────────────

    @Test
    fun `a legacy completed task keeps exactly the points it had`() {
        // The identity the no-backfill decision rests on: 40 stored points reconstruct to
        // 120 minutes at ROUTINE, which price back at 40. If this goes red, upgrading the
        // app silently re-prices somebody's history.
        val legacy = TaskDto(id = "t1", points = 40, done = true, completedAt = 1_700L)

        val task = legacy.toDomain()

        assertThat(task.isDone).isTrue()
        assertThat(task.completedAtEpochMillis).isEqualTo(1_700L)
        assertThat(task.completion!!.minutes).isEqualTo(120)
        assertThat(task.points).isEqualTo(40)
    }

    @Test
    fun `a legacy task with a real duration is re-priced from it, which is the inversion`() {
        // Deliberately NOT the identity above. A task that recorded 90 real minutes is now
        // worth `round(90/3) = 30` regardless of the 12 points a word count once gave it —
        // that IS §1.4: the reward number stops being an input and becomes a view of effort.
        val legacy = TaskDto(id = "t1", points = 12, estimatedMinutes = 90, done = true, completedAt = 1_700L)

        assertThat(legacy.toDomain().points).isEqualTo(30)
    }

    @Test
    fun `a legacy done task with no stamp is dated from its creation, not from the epoch`() {
        // `#7` closed the hole that produced these, but documents written through it exist.
        // Epoch-zero would drop the task out of every window-based reader — the summary, the
        // done-this-week count and the time chart all filter on the stamp.
        val legacy = TaskDto(id = "t1", points = 10, done = true, completedAt = null, createdAt = 900L)

        assertThat(legacy.toDomain().completedAtEpochMillis).isEqualTo(900L)
    }

    @Test
    fun `a legacy open task has no completion at all`() {
        val legacy = TaskDto(id = "t1", points = 10, done = false, completedAt = 1_700L)

        // Note what is asserted: the stray stamp on an open task -- the half-written fact
        // `TaskCompletion.stamp` used to clean up -- is not repaired, it is unrepresentable.
        assertThat(legacy.toDomain().completion).isNull()
        assertThat(legacy.toDomain().isDone).isFalse()
    }

    @Test
    fun `an unparseable stored difficulty prices the task on its minutes alone`() {
        val odd = TaskDto(id = "t1", difficulty = "BRUTAL", estimatedMinutes = 60)

        assertThat(odd.toDomain().difficulty).isEqualTo(Difficulty.ROUTINE)
        assertThat(odd.toDomain().points).isEqualTo(20)
    }

    // ── The write path: a document must not carry two answers ────────

    @Test
    fun `a write carries the edge list and nulls all four superseded fields`() {
        val task = Task(
            id = "t1",
            title = "Run 5 km",
            goalEdges = goalEdgesOf("g1", contribution = 2.0),
            difficulty = Difficulty.DEMANDING,
            estimatedMinutes = 90,
            durationSource = DurationSource.USER,
            createdAtEpochMillis = 100L,
            completion = CompletionFact(completedAtEpochMillis = 500L, minutes = 90),
        )

        val dto = task.toDto()

        assertThat(dto.goalEdges).containsExactly(GoalEdgeDto(goalId = "g1", contribution = 2.0))
        assertThat(dto.difficulty).isEqualTo("DEMANDING")
        assertThat(dto.points).isNull()
        assertThat(dto.done).isNull()
        assertThat(dto.completedAt).isNull()
        assertThat(dto.progressContribution).isNull()
    }

    @Test
    fun `the stored goalId is written from the edge, because the query needs an indexed field`() {
        // It is a PROJECTION, not a second answer: rewritten from `goalEdges` on every write,
        // and never read as the truth once an edge list is present. Firestore cannot filter
        // an array of maps on one member's key, which is the whole reason it survives.
        val dto = Task(id = "t1", goalEdges = goalEdgesOf("g1", 2.0)).toDto()

        assertThat(dto.goalId).isEqualTo("g1")
    }

    @Test
    fun `an unfiled task writes a null goalId rather than an empty string`() {
        assertThat(Task(id = "t1").toDto().goalId).isNull()
    }

    @Test
    fun `a document written after the change does not lose contribution`() {
        // The round trip, which is the half a one-directional migration test always misses.
        val original = Task(
            id = "t1",
            title = "Run 5 km",
            goalEdges = listOf(GoalEdge("g1", 2.0), GoalEdge("g2", null)),
            difficulty = Difficulty.LIGHT,
            estimatedMinutes = 45,
            durationSource = DurationSource.AI,
            createdAtEpochMillis = 100L,
        )

        val reread = original.toDto().toDomain()

        assertThat(reread.goalEdges).isEqualTo(original.goalEdges)
        assertThat(reread.difficulty).isEqualTo(Difficulty.LIGHT)
        assertThat(reread.points).isEqualTo(original.points)
    }

    @Test
    fun `the completion survives its own round trip through the fact document`() {
        val fact = CompletionFact(
            completedAtEpochMillis = 1_755_000_000_000L,
            minutes = 90,
            difficulty = Difficulty.DEMANDING,
        )

        val reread = fact.toDto("t1").toDomain()

        assertThat(reread).isEqualTo(fact)
        assertThat(reread.points).isEqualTo(45)
    }

    @Test
    fun `the fact document is keyed by the task id`() {
        // What makes a tick a `set` and an untick a `delete` of one known path — no query,
        // no read-then-write, and "an untick removes exactly the fact it added" for free.
        assertThat(CompletionFact().toDto("t-42").id).isEqualTo("t-42")
    }

    @Test
    fun `an unparseable stored difficulty on a fact is ROUTINE, so a banked total never breaks`() {
        val odd = CompletionFactDto(id = "t1", completedAt = 1L, minutes = 30, difficulty = "??")

        assertThat(odd.toDomain().difficulty).isEqualTo(Difficulty.ROUTINE)
        assertThat(odd.toDomain().points).isEqualTo(10)
    }
}
