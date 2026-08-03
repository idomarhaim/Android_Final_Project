package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.usecase.BuildLifeAreaProposalsUseCase
import com.idomarhaim.goalpilot.domain.usecase.GoogleTaskList
import com.idomarhaim.goalpilot.domain.usecase.LifeAreaSyncAction
import org.junit.Test

/**
 * Turning Google Tasks lists into life areas. The list names in these cases are
 * the user's real ones — Hebrew — because that is what the icon guesser and the
 * name-collision check actually have to cope with.
 */
class LifeAreaSyncTest {

    private val useCase = BuildLifeAreaProposalsUseCase()

    private val googleLists = listOf(
        GoogleTaskList(id = "list-health", title = "בריאות"),
        GoogleTaskList(id = "list-study", title = "לימודים"),
        GoogleTaskList(id = "list-career", title = "קריירה"),
        GoogleTaskList(id = "list-couple", title = "זוגיות"),
    )

    @Test
    fun `every list becomes a create proposal when nothing exists yet`() {
        val proposals = useCase(googleLists, existingAreas = emptyList())

        assertThat(proposals).hasSize(4)
        assertThat(proposals.map { it.action }.toSet())
            .containsExactly(LifeAreaSyncAction.CREATE)
        assertThat(proposals.map { it.name })
            .containsExactly("בריאות", "לימודים", "קריירה", "זוגיות")
        assertThat(proposals.all { it.selected }).isTrue()
    }

    @Test
    fun `an already-synced list produces no row at all`() {
        val existing = listOf(
            LifeArea(id = "a1", name = "בריאות", googleListId = "list-health"),
        )

        val proposals = useCase(googleLists, existing)

        assertThat(proposals.map { it.googleListId })
            .containsExactly("list-study", "list-career", "list-couple")
    }

    @Test
    fun `a name that already exists is linked, not duplicated`() {
        val existing = listOf(
            LifeArea(id = "a1", name = " בריאות ", colorHex = "#123456", iconKey = "spa"),
        )

        val proposal = useCase(googleLists, existing).first { it.googleListId == "list-health" }

        assertThat(proposal.action).isEqualTo(LifeAreaSyncAction.LINK)
        assertThat(proposal.existingAreaId).isEqualTo("a1")
        // The user's own colour and icon survive the link.
        assertThat(proposal.colorHex).isEqualTo("#123456")
        assertThat(proposal.iconKey).isEqualTo("spa")
    }

    @Test
    fun `two lists with the same name only produce one row`() {
        val duplicated = googleLists + GoogleTaskList(id = "list-health-2", title = "בריאות")

        val proposals = useCase(duplicated, existingAreas = emptyList())

        assertThat(proposals.count { it.name == "בריאות" }).isEqualTo(1)
    }

    @Test
    fun `blank ids and titles are dropped`() {
        val messy = listOf(
            GoogleTaskList(id = "", title = "No id"),
            GoogleTaskList(id = "x", title = "   "),
            GoogleTaskList(id = "ok", title = "Fitness"),
        )

        val proposals = useCase(messy, existingAreas = emptyList())

        assertThat(proposals.map { it.name }).containsExactly("Fitness")
    }

    @Test
    fun `new areas in one run get distinct colours`() {
        val proposals = useCase(googleLists, existingAreas = emptyList())

        assertThat(proposals.map { it.colorHex }.toSet()).hasSize(proposals.size)
    }

    @Test
    fun `sort order continues after the areas that already exist`() {
        val existing = listOf(LifeArea(id = "a1", name = "Kept", sortOrder = 7))

        val proposals = useCase(googleLists, existing)

        assertThat(proposals.map { it.sortOrder }).containsExactly(8, 9, 10, 11).inOrder()
    }

    @Test
    fun `icons are guessed from the name in Hebrew and English`() {
        val proposals = useCase(googleLists, existingAreas = emptyList())
            .associate { it.name to it.iconKey }

        assertThat(proposals["בריאות"]).isEqualTo("favorite")
        assertThat(proposals["לימודים"]).isEqualTo("school")
        assertThat(proposals["קריירה"]).isEqualTo("work")
        assertThat(proposals["זוגיות"]).isEqualTo("people")
        assertThat(LifeAreaPalette.iconKeyFor("Finance & budget")).isEqualTo("finance")
        assertThat(LifeAreaPalette.iconKeyFor("Gym")).isEqualTo("fitness")
        // Nothing recognisable is never wrong, only unhelpful.
        assertThat(LifeAreaPalette.iconKeyFor("Miscellany")).isEqualTo("flag")
    }

    @Test
    fun `nextHex spreads colours before it reuses one`() {
        val all = LifeAreaPalette.hexes

        assertThat(LifeAreaPalette.nextHex(emptyList())).isEqualTo(all.first())
        assertThat(LifeAreaPalette.nextHex(all.dropLast(1))).isEqualTo(all.last())
        // Past the palette size it degrades to evenly reused, not "always the first".
        assertThat(LifeAreaPalette.nextHex(all + all.first())).isEqualTo(all[1])
    }
}
