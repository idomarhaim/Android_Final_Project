package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.firestore.dto.GoalDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.LoggingRule
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import org.junit.Test

/**
 * Free-text `unit` → §1.3's `measureKind` + `measureWord`, spec §7.1 —
 * [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11).
 *
 * **This is the ticket's stated trap.** Every existing goal carries a free-text
 * unit, mostly `"%"`, and mapping those to a closed kind is a *guess* for
 * anything that is not literally `"%"`. The brief's instruction is exact: map
 * what is unambiguous, leave the rest absent and ask, and **never invent a kind
 * from a string match**. So the suite is written mostly as *what the migration
 * refuses to do* — a mapper that classified `"litres"` as `VOLUME` would pass
 * every happy-path test anyone would think to write, and would be wrong the
 * moment the word arrives in Hebrew, abbreviated, or misspelt.
 *
 * Both directions, as the ticket requires: a pre-#11 document must read back
 * sensibly, and a document this app writes must stop carrying the old field at
 * all — otherwise the goal holds two answers to *what does this count?*, which is
 * §0.3's most-repeated finding.
 */
class GoalMeasureMigrationTest {

    // ── Reading a pre-#11 document ─────────────────────────────────

    @Test
    fun `a defaulted percent becomes no measure at all`() {
        // §7.1: "a defaulted `%` becomes absent". Nothing on the wire tells a
        // chosen "%" from a defaulted one — the field was written identically in
        // both cases — and absent is the recoverable direction, because the goal
        // is then asked. This is Ido's live "Drink 4 Liters of Water Daily",
        // which reads `Health - 1/100 %` today and is #11's whole reason to exist.
        val legacy = GoalDto(id = "g", title = "Drink 4 Liters of Water Daily", unit = "%")

        assertThat(legacy.toDomain().measure).isNull()
    }

    @Test
    fun `a blank or absent unit is no measure either`() {
        listOf(null, "", "   ").forEach { stored ->
            assertThat(GoalDto(id = "g", unit = stored).toDomain().measure).isNull()
        }
    }

    @Test
    fun `a free word survives verbatim with its kind left open`() {
        // The word is user content and is kept losslessly; the kind was never
        // recorded and is NOT reconstructed. `C22` #44 asks.
        val measure = GoalDto(id = "g", title = "Read", unit = "books").toDomain().measure

        assertThat(measure).isEqualTo(Measure(kind = null, word = "books"))
        assertThat(measure!!.isClassified).isFalse()
    }

    @Test
    fun `no free word is classified, whatever it says`() {
        // The heart of the trap. Each of these is a word a string matcher would
        // "obviously" get right, and each is why matching is banned: "L" is
        // litres or length, "ק״מ" defeats an English table outright, "kms" and
        // "Litres" defeat an exact one, and "%" appears here to show that even
        // the one unambiguous string is not classified on the read path.
        listOf("litres", "L", "km", "kms", "Litres", "ק״מ", "reps", "₪", "%").forEach { word ->
            val measure = GoalDto(id = "g", unit = word).toDomain().measure
            assertThat(measure?.kind).isNull()
        }
    }

    @Test
    fun `a goal the app authored is classified from its own key, not its word`() {
        // The one branch that DOES classify, and it reads no user text: a
        // healthSourceKey is stamped by the sync at birth and is unreachable from
        // the UI (#47), so this is the app's knowledge of its own goal. Without
        // it every Health Connect goal loses its measure on migration and stops
        // reading "3200 / 70000 steps" on the widget — a regression, not an
        // honest absence.
        val steps = GoalDto(
            id = "g",
            title = "Weekly steps",
            healthSourceKey = HealthMetric.STEPS.goalSourceKey,
            unit = "steps",
        )

        assertThat(steps.toDomain().measure).isEqualTo(Measure(MeasureKind.COUNT, "steps"))
    }

    @Test
    fun `the authored branch keeps the user's word, not the metric's`() {
        // The kind is app logic; the word is the user's, even on a goal the app
        // created — they can edit it on the goal form.
        val renamed = GoalDto(
            id = "g",
            healthSourceKey = HealthMetric.SLEEP.goalSourceKey,
            unit = "שעות",
        )

        assertThat(renamed.toDomain().measure).isEqualTo(Measure(MeasureKind.DURATION, "שעות"))
    }

    @Test
    fun `an unrecognised source key classifies nothing`() {
        // A key from a future integration must not fall through to some default
        // kind. The word survives; the kind stays open.
        val future = GoalDto(id = "g", healthSourceKey = "hc:goal:heartrate", unit = "bpm")

        assertThat(future.toDomain().measure).isEqualTo(Measure(kind = null, word = "bpm"))
    }

    // ── Reading a post-#11 document ────────────────────────────────

    @Test
    fun `the new fields win outright over a stale legacy unit`() {
        // Only reachable if some other writer left both behind. The same posture
        // `resolvedLifeAreaIds` takes: the field this app writes is the one that
        // decides, because two answers to one question is the finding §0.3 names.
        val both = GoalDto(id = "g", measureKind = "VOLUME", measureWord = "L", unit = "stale")

        assertThat(both.toDomain().measure).isEqualTo(Measure(MeasureKind.VOLUME, "L"))
    }

    @Test
    fun `a chosen percent survives, which is what makes chosen and defaulted differ`() {
        // §7.1: "`%` survives as a CHOSEN PERCENT measure". The choosing is
        // recorded in `measureKind` — the thing a defaulted "%" never had — so
        // the two states that were indistinguishable on the old wire are now
        // distinguishable on the new one.
        val chosen = GoalDto(id = "g", measureKind = "PERCENT", measureWord = "%")

        assertThat(chosen.toDomain().measure).isEqualTo(Measure(MeasureKind.PERCENT, "%"))
    }

    @Test
    fun `an unrecognised kind name degrades to an open kind, not to a wrong one`() {
        // A kind written by a newer build, or corrupted. The word still renders;
        // nothing computes with it.
        val odd = GoalDto(id = "g", measureKind = "TEMPERATURE", measureWord = "°C")

        assertThat(odd.toDomain().measure).isEqualTo(Measure(kind = null, word = "°C"))
    }

    @Test
    fun `a migrated document with neither field carries no measure`() {
        // The state a goal reaches after being saved with "Nothing yet" chosen:
        // it must not fall back to reading the legacy field it no longer has.
        assertThat(GoalDto(id = "g", title = "Learn guitar").toDomain().measure).isNull()
    }

    // ── Writing ────────────────────────────────────────────────────

    @Test
    fun `a write drops the legacy unit outright`() {
        // The migrating write. Leaving `unit` populated beside the two fields
        // that replaced it is the second answer that quietly disagrees, and it is
        // exactly what `lifeAreaId = null` already guards against one field over.
        val dto = Goal(id = "g", measure = Measure(MeasureKind.VOLUME, "L")).toDto()

        assertThat(dto.unit).isNull()
        assertThat(dto.measureKind).isEqualTo("VOLUME")
        assertThat(dto.measureWord).isEqualTo("L")
    }

    @Test
    fun `a goal with no measure writes neither field`() {
        val dto = Goal(id = "g", title = "Learn guitar").toDto()

        assertThat(dto.measureKind).isNull()
        assertThat(dto.measureWord).isNull()
        assertThat(dto.unit).isNull()
    }

    @Test
    fun `an unclassified measure writes its word and no kind`() {
        // The half-way state has to survive a save, or opening and saving a
        // pre-#11 goal would silently destroy the word while still not knowing
        // its kind — the worst of both.
        val dto = Goal(id = "g", measure = Measure(kind = null, word = "books")).toDto()

        assertThat(dto.measureKind).isNull()
        assertThat(dto.measureWord).isEqualTo("books")
    }

    // ── Round trips, both directions ───────────────────────────────

    @Test
    fun `every measure survives a domain to wire to domain round trip`() {
        val cases = listOf(
            null,
            Measure(MeasureKind.VOLUME, "L"),
            Measure(MeasureKind.PERCENT, "%"),
            Measure(MeasureKind.COUNT, "books"),
            Measure(kind = null, word = "litres"),
        )

        cases.forEach { measure ->
            val round = Goal(id = "g", measure = measure).toDto().toDomain().measure
            assertThat(round).isEqualTo(measure)
        }
    }

    @Test
    fun `a legacy document reaches a stable state after one save`() {
        // The readable half-way state §7.1 asks for: read the old document, save
        // it once, and the second read must agree with the first. A migration
        // whose output re-migrates differently is not a migration.
        val legacy = GoalDto(id = "g", title = "Read", unit = "books").toDomain()
        val afterSave = legacy.toDto().toDomain()

        assertThat(afterSave.measure).isEqualTo(legacy.measure)
        assertThat(afterSave.measureWord).isEqualTo("books")
    }

    // ── Input mode ─────────────────────────────────────────────────

    @Test
    fun `a document with no input mode reads as NUMBER`() {
        // §7.1's posture: every new field reads identically on day one. NUMBER is
        // what every goal did before the field existed.
        assertThat(GoalDto(id = "g").toDomain().inputMode).isEqualTo(InputMode.NUMBER)
    }

    @Test
    fun `a synced goal is in AUTO whatever the document says`() {
        // Derived, not stored (§0.2): the mode is a property of where the goal's
        // numbers come from. A stale stored NUMBER must not win over the key.
        val synced = GoalDto(
            id = "g",
            healthSourceKey = HealthMetric.STEPS.goalSourceKey,
            inputMode = "NUMBER",
        )

        assertThat(synced.toDomain().inputMode).isEqualTo(InputMode.AUTO)
    }

    @Test
    fun `a stored input mode round trips`() {
        val dto = Goal(id = "g", inputMode = InputMode.BUTTONS).toDto()

        assertThat(dto.inputMode).isEqualTo("BUTTONS")
        assertThat(dto.toDomain().inputMode).isEqualTo(InputMode.BUTTONS)
    }

    @Test
    fun `an unrecognised input mode falls back to NUMBER rather than failing`() {
        assertThat(GoalDto(id = "g", inputMode = "SLIDER").toDomain().inputMode)
            .isEqualTo(InputMode.NUMBER)
    }

    // ── The rule §1.3 pins to the mode ─────────────────────────────

    @Test
    fun `whether logging adds or sets rides the input mode`() {
        // §1.3: "Whether logging adds or sets rides this - per goal, because a
        // global rule is the granularity error." Pinned as a specification: TICK
        // is the only mode that sets, and it is deliberately not in OFFERED
        // because the write path a SETS mode needs does not exist yet (#22).
        assertThat(InputMode.entries.filter { it.logging == LoggingRule.SETS })
            .containsExactly(InputMode.TICK)
        assertThat(InputMode.OFFERED).containsExactly(InputMode.NUMBER, InputMode.BUTTONS)
        assertThat(InputMode.OFFERED).doesNotContain(InputMode.TICK)
    }
}
