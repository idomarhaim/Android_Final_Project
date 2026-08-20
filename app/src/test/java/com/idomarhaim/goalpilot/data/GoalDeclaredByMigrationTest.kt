package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.firestore.dto.GoalDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Goal
import org.junit.Test

/**
 * `declaredBy` — §1.1's intrinsic marker and §7.1's backfill (`#6`).
 *
 * The field has to carry **three** answers on the wire where the domain has two shapes, and the
 * whole of this suite is about the pair that a careless encoding collapses:
 *
 *  - an **absent** field means *this document was written before `#6`* → `UNKNOWN`;
 *  - the **`"NONE"`** sentinel means *the marker was deliberately dropped* → `null`, instrumental.
 *
 * Collapse those two and §1.1's *lossless demotion* stops working in the worst possible way: a
 * goal Ido has just told the app is **not** a goal reads back as a goal on the next snapshot,
 * silently, with nothing in the UI to say why. That is the failure these cases exist to make
 * unshippable.
 *
 * No backfill write runs anywhere, so *"day one reads identically"* (§7.1's migration posture)
 * has to hold on read alone — which is what the first case checks.
 */
class GoalDeclaredByMigrationTest {

    @Test
    fun `a document written before the field reads as UNKNOWN, not as Ido's own goal`() {
        // §7.1: "nothing records who made the existing goals, and the migration must not
        // pretend otherwise." Reading these as USER would manufacture a consent never given,
        // on precisely the objects §0.7 says need consent.
        val legacy = GoalDto(id = "g1", title = "Run", declaredBy = null)

        assertThat(legacy.toDomain().declaredBy).isEqualTo(DeclaredBy.UNKNOWN)
    }

    @Test
    fun `a legacy goal is not a pending suggestion`() {
        assertThat(GoalDto(id = "g1", title = "Run").toDomain().isPendingSuggestion).isFalse()
    }

    @Test
    fun `the NONE sentinel reads back as instrumental, which absence cannot say`() {
        val demoted = GoalDto(id = "g1", title = "Run", declaredBy = GoalDto.DECLARED_BY_NONE)

        assertThat(demoted.toDomain().declaredBy).isNull()
    }

    @Test
    fun `both declared values survive the round trip`() {
        for (value in listOf(DeclaredBy.USER, DeclaredBy.AI_SUGGESTED, DeclaredBy.UNKNOWN)) {
            val round = Goal(id = "g1", title = "Run", declaredBy = value).toDto().toDomain()

            assertThat(round.declaredBy).isEqualTo(value)
        }
    }

    @Test
    fun `a demotion survives the round trip — the object comes back instrumental`() {
        // The one case the obvious encoding gets wrong. Writing null and reading absence back
        // would return UNKNOWN here, and the demotion would silently undo itself.
        val round = Goal(id = "g1", title = "Run", declaredBy = null).toDto().toDomain()

        assertThat(round.declaredBy).isNull()
        assertThat(round.isPendingSuggestion).isFalse()
    }

    @Test
    fun `a demoted goal is written as NONE and never as an absent field`() {
        val dto = Goal(id = "g1", title = "Run", declaredBy = null).toDto()

        assertThat(dto.declaredBy).isEqualTo(GoalDto.DECLARED_BY_NONE)
    }

    @Test
    fun `only AI_SUGGESTED is pending`() {
        val pending = { v: DeclaredBy? -> Goal(id = "g", title = "t", declaredBy = v).isPendingSuggestion }

        assertThat(pending(DeclaredBy.AI_SUGGESTED)).isTrue()
        assertThat(pending(DeclaredBy.USER)).isFalse()
        assertThat(pending(DeclaredBy.UNKNOWN)).isFalse()
        assertThat(pending(null)).isFalse()
    }

    @Test
    fun `an unreadable value degrades toward NOT a goal, never toward one Ido declared`() {
        // A misspelling, or a value written by a future version. The safe direction is a
        // milestone the app forgot to show — never a goal it asserted on his behalf.
        val alien = GoalDto(id = "g1", title = "Run", declaredBy = "MAYBE")

        assertThat(alien.toDomain().declaredBy).isNull()
    }

    @Test
    fun `DeclaredBy_fromName is exact and does not match on case`() {
        // The wire value is written by this app from `enum.name`, so a case-insensitive match
        // would only ever soften a genuine corruption into a plausible-looking answer.
        assertThat(DeclaredBy.fromName("USER")).isEqualTo(DeclaredBy.USER)
        assertThat(DeclaredBy.fromName("user")).isNull()
        assertThat(DeclaredBy.fromName(null)).isNull()
        assertThat(DeclaredBy.fromName(GoalDto.DECLARED_BY_NONE)).isNull()
    }

    @Test
    fun `a hand-built Goal is UNKNOWN, so no code path claims Ido declared something by default`() {
        assertThat(Goal(title = "Run").declaredBy).isEqualTo(DeclaredBy.UNKNOWN)
    }
}
