package com.idomarhaim.goalpilot.derived

import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.Leveling
import com.idomarhaim.goalpilot.domain.model.TaskScoring
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.scoringWindowFor
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Test
import java.io.File

/**
 * **The Kotlin half of `C20`'s shared fixture** (`docs/PRODUCT_v0.3.md` §5.2).
 *
 * §5.2 moved the derived-state arithmetic off the client and named the cost in the same
 * breath:
 *
 * > *"the arithmetic now exists in **Kotlin and TypeScript** — a second implementation that
 * > can disagree. Accepted, because avoiding it costs the offline win entirely, and
 * > **pinned by a shared `facts → expected numbers` fixture both test layers run**."*
 *
 * This is one of the two layers that run it. The other is
 * `functions/test/projection.test.mjs`. **Neither owns the file** — both resolve
 * `shared-fixtures/derived-state.json` by walking up out of their own module, so a number
 * cannot be quietly edited into agreement on one side. Add a case there and both suites go
 * red until both languages produce it.
 *
 * ### What is being pinned, exactly
 *
 * Not `TaskRepositoryImpl`. That class no longer computes anything — `C20` reduced
 * `setDone` to a single write of a fact. What has to agree is the **rule**: given a set of
 * completion facts, what total do they imply, and what level does that total sit at. The
 * server implements it in `functions/src/derived.ts` to write `publicProfiles.points`; the
 * client implements it in [TaskScoring] and [Leveling] wherever a number is rendered. If the
 * two drift, a user's own screen and the leaderboard everybody else sees disagree about the
 * same person, silently and in production.
 *
 * ### `#55` gave this suite considerably more to hold
 *
 * The rule used to be *sum a stored number over the done tasks* — arithmetic so thin that
 * both implementations were one `reduce`. §1.4 replaced it with
 * `round(minutes / 3) × difficulty` over **banked inputs**, which is a formula, a rounding
 * order and a multiplier table, in two languages. The fixture's `completionFacts` cases pin
 * all three; its `tasks` cases now pin the **migration** reader, which is the half that
 * decides whether every user's lifetime total survives the deploy.
 *
 * This side deliberately calls the production [TaskScoring.pointsFor]. A suite that
 * reimplemented the formula would agree with itself no matter what the app did.
 *
 * ### The failure this is built to avoid
 *
 * A fixture that silently resolves to nothing makes every case below pass having asserted
 * nothing at all — green, with the guarantee gone. So [fixtureFile] fails loudly naming
 * both paths it tried, and `the fixture is present and non-empty` asserts the case counts
 * before any case runs.
 *
 * **`kotlinx.serialization`, not `org.json`,** because `org.json` is stubbed in the Android
 * unit-test classpath and every call throws `Stub!` at runtime — a parser that fails only
 * inside the layer it was chosen for.
 */
class DerivedStateFixtureTest {

    /**
     * Gradle runs unit tests with the `app` module as the working directory, so the repo
     * root is one level up; the bare path covers a runner started from the repo root
     * instead. Both are tried and the error names both, because "file not found" from a
     * test that deliberately walks out of its own module is otherwise a puzzle.
     */
    private val fixtureFile: File = listOf(
        File("../shared-fixtures/derived-state.json"),
        File("shared-fixtures/derived-state.json"),
    ).firstOrNull { it.isFile }
        ?: error(
            "shared-fixtures/derived-state.json not found from ${File(".").absolutePath}. " +
                "It is read by this suite AND by functions/test/projection.test.mjs; if it " +
                "moves, both readers move with it (docs/PRODUCT_v0.3.md §5.2).",
        )

    private val fixture: JsonObject =
        Json.parseToJsonElement(fixtureFile.readText()).jsonObject

    private val pointsCases: JsonArray = fixture.getValue("pointsCases").jsonArray
    private val scoreCases: JsonArray = fixture.getValue("scoreCases").jsonArray

    /**
     * The scoring **window**, added 2026-08-25 — and the gap it closes is the one this
     * whole file exists for.
     *
     * `scoringWindow` decides **which entries a challenge counts**. It exists in Kotlin
     * ([Challenge.scoringWindowFor]) and in TypeScript (`derived.ts`), it is the rule most
     * likely of all of them to drift — the two are written in different *shapes*, an
     * `if/else` against a nested ternary — and until 2026-08-25 the fixture pinned
     * **nothing** about it. It changed in both languages that day, which is precisely when
     * the fixture header says to add cases first and let both suites go red.
     */
    private val windowCases: JsonArray = fixture.getValue("windowCases").jsonArray

    private fun JsonObject.name(): String = getValue("name").jsonPrimitive.content

    /**
     * The projection, in Kotlin — the union of banked completion facts and the pre-`#55`
     * tasks that have none, keyed by task id, floored at zero.
     *
     * The banked half goes through the **production** [TaskScoring.pointsFor], not through a
     * copy of the formula written here. That is the whole point of this suite: the number
     * `functions/src/derived.ts` computes has to be the number this app computes, and a test
     * that reimplements the rule can only ever agree with itself.
     *
     * The legacy half sums the stored `points` verbatim, mirroring
     * `pointsFromFacts`' second argument. It is the old source draining, not a second rule.
     */
    private fun pointsFromFacts(facts: JsonObject): Long {
        val banked = (facts["completionFacts"] as? JsonArray).orEmpty()
            .map { it.jsonObject }
            .associateBy { it.getValue("taskId").jsonPrimitive.content }
        var total = 0L
        for (fact in banked.values) {
            total += TaskScoring.pointsFor(
                minutes = fact.getValue("minutes").jsonPrimitive.int,
                difficulty = Difficulty.fromName(fact["difficulty"]?.jsonPrimitive?.content),
            ).toLong()
        }
        for (task in (facts["tasks"] as? JsonArray).orEmpty().map { it.jsonObject }) {
            // A fact wins over the legacy fields on the same task — it is what was actually
            // banked. Summing both would double-credit the tasks a user touches most.
            if (task.getValue("id").jsonPrimitive.content in banked) continue
            if (task["done"]?.jsonPrimitive?.boolean != true) continue
            total += task["points"]?.jsonPrimitive?.long ?: 0L
        }
        return total.coerceAtLeast(0L)
    }

    private fun JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> =
        this ?: emptyList()

    @Test
    fun `the fixture is present and non-empty`() {
        // Guards the vacuous-green case named in the KDoc: without this, a fixture that
        // parsed to empty arrays would leave every loop below iterating zero times and
        // reporting pass.
        assertWithMessage("points cases in ${fixtureFile.absolutePath}")
            .that(pointsCases.size).isGreaterThan(0)
        assertWithMessage("score cases in ${fixtureFile.absolutePath}")
            .that(scoreCases.size).isGreaterThan(0)
        assertWithMessage("window cases in ${fixtureFile.absolutePath}")
            .that(windowCases.size).isGreaterThan(0)
    }

    @Test
    fun `every fixture case projects to the same points on this side`() {
        for (element in pointsCases) {
            val case = element.jsonObject
            val facts = case.getValue("facts").jsonObject
            val expected = case.getValue("expected").jsonObject

            assertWithMessage("points — ${case.name()}")
                .that(pointsFromFacts(facts))
                .isEqualTo(expected.getValue("points").jsonPrimitive.long)
        }
    }

    @Test
    fun `every fixture case lands on the same level on this side`() {
        // [Leveling] is the client's curve and `functions/src/derived.ts` mirrors it. This
        // is the assertion that catches one of the two being tuned without the other — the
        // concrete shape of §5.2's "a second implementation that can disagree".
        for (element in pointsCases) {
            val case = element.jsonObject
            val expected = case.getValue("expected").jsonObject
            assertWithMessage("level — ${case.name()}")
                .that(Leveling.levelForPoints(expected.getValue("points").jsonPrimitive.long))
                .isEqualTo(expected.getValue("level").jsonPrimitive.int)
        }
    }

    @Test
    fun `the level curve itself is the one the server mirrors`() {
        // `publicProfiles.level` was deleted by C20 — a stored function of `points` in the
        // same document. What made deleting it safe is that `points` alone determines it,
        // so the curve is the entire contract between the two implementations. The fixture
        // pins the boundaries that matter; this pins the formula they come from.
        assertWithMessage("L1:0, L2:100, L3:300, L4:600, L5:1000")
            .that((1..5).map { Leveling.pointsForLevel(it) })
            .containsExactly(0L, 100L, 300L, 600L, 1000L).inOrder()
        assertWithMessage("clamped at level 1, as the TypeScript Math.max(1, …) is")
            .that(Leveling.pointsForLevel(0)).isEqualTo(0L)
    }

    @Test
    fun `the score projection clamps and reports null the same way on this side`() {
        // Shorter than the points cases but not decorative: `null` means *write nothing*,
        // and a caller that read it as zero would wipe a standing other people are looking
        // at. Mirrors `scoreFromReport` in functions/src/derived.ts.
        for (element in scoreCases) {
            val case = element.jsonObject
            val expected = case.getValue("expected").jsonObject
            val reportField = case.getValue("facts").jsonObject.getValue("report")

            val projected: Double? = (reportField as? JsonObject)
                ?.get("value")?.jsonPrimitive?.double?.coerceAtLeast(0.0)

            val expectedScore = expected.getValue("score")
            if (expectedScore is JsonNull) {
                assertWithMessage("no report → write nothing — ${case.name()}")
                    .that(projected).isNull()
            } else {
                assertWithMessage("score — ${case.name()}")
                    .that(projected).isEqualTo(expectedScore.jsonPrimitive.double)
            }
        }
    }

    @Test
    fun `every fixture case yields the same scoring window on this side`() {
        // The Kotlin half of the pin. `functions/test/projection.test.mjs` runs the same
        // rows through `derived.ts`; a change to either language that this file does not
        // also carry turns one of the two suites red, which is the only mechanism stopping
        // the standings and the card from disagreeing about the same race.
        for (element in windowCases) {
            val case = element.jsonObject
            // `as?`, not `?.jsonObject`: the "absent challenge" case is JSON `null`,
            // which parses to JsonNull -- a present element that is not an object -- and
            // `.jsonObject` THROWS on it rather than returning null. That case is the one
            // pinning the NaN guard, so losing it would lose the guard.
            val challenge = case["challenge"] as? JsonObject
            val expected = case.getValue("expected").jsonObject

            val window = Challenge(
                startAtEpochMillis = challenge?.get("startAt")?.jsonPrimitive?.long ?: 0L,
                endAtEpochMillis = challenge?.get("endAt")?.jsonPrimitive?.long ?: 0L,
            ).scoringWindowFor(
                ChallengeParticipant(
                    joinedAtEpochMillis =
                        case["joinedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                ),
            )

            assertWithMessage("window from — ${case.name()}")
                .that(window.fromEpochMillis)
                .isEqualTo(expected.getValue("from").jsonPrimitive.long)
            assertWithMessage("window until — ${case.name()}")
                .that(window.untilEpochMillis)
                .isEqualTo(expected["until"]?.jsonPrimitive?.longOrNull)
        }
    }
}
