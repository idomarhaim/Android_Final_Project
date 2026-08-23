package com.idomarhaim.goalpilot.ui.tutorial

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.ui.navigation.TopLevelTab
import java.io.File
import org.junit.Test

/**
 * The tour's route: the invariants that make it a walk over the app rather than
 * a list of sentences.
 *
 * ### Why several of these read source text
 *
 * A step names a [TutorialAnchor]; some screen has to *apply* that anchor for
 * the step to point at anything. Nothing connects the two at compile time — the
 * enum is happy to have an entry nobody tags, and the tour is happy to spotlight
 * it — and the failure is silent in the worst way: the overlay falls back to a
 * centred card with no hole, so the tour still runs, still reads correctly, and
 * simply stops pointing. On a screenshot it looks like a design choice.
 *
 * So the connection is asserted here, the same way `DialogLocaleGuardTest`
 * asserts that no window escapes `ui/locale/`. Same honest limit, too: this
 * reads text, so it catches what people write and not what an import alias could
 * hide.
 */
class TutorialStepsTest {

    private val sourceRoot = listOf(
        File("src/main/java/com/idomarhaim/goalpilot"),
        File("app/src/main/java/com/idomarhaim/goalpilot"),
    ).firstOrNull { it.isDirectory }
        ?: error("source root not found from ${File(".").absolutePath}")

    @Test
    fun `the tour is short enough to be watched`() {
        // Not a style rule. Attention across coach marks falls off a cliff past
        // five to seven, and the thing a user skips is never step six — it is
        // the whole feature, once and for good. This app has enough surface to
        // fill twenty steps, and the count is what stops it.
        assertThat(TutorialStep.count).isAtLeast(4)
        assertThat(TutorialStep.count).isAtMost(7)
    }

    @Test
    fun `every step is shown on a top-level destination`() {
        // `navigateForTutorial` pops to the graph's start destination and passes
        // NO arguments, so a step's route has to be both argument-free and
        // reachable that way. The top-level tabs are exactly the set that is:
        // a step pointing at `goal_detail/{goalId}` would compile, read
        // perfectly, and navigate to a route with an unfilled path argument.
        val tabs = TopLevelTab.entries.map { it.route }.toSet()
        val offenders = TutorialStep.entries
            .filterNot { it.route in tabs }
            .map { "${it.name} -> ${it.route}" }

        assertWithMessage(
            "These steps name a route that is not a top-level tab. The tour navigates " +
                "with no arguments and pops to the start destination, so such a step either " +
                "cannot be reached at all or leaves the user somewhere no tab gets them out of.",
        ).that(offenders).isEmpty()
    }

    @Test
    fun `an action step asks the user to go somewhere they are not`() {
        TutorialStep.entries.mapNotNull { step -> step.action?.let { step to it } }
            .forEach { (step, action) ->
                assertWithMessage("${step.name} asks the user to arrive where they already are")
                    .that(action.completedOnRoute)
                    .isNotEqualTo(step.route)
            }
    }

    @Test
    fun `the tour DEMANDS exactly one gesture`() {
        // Two would not break anything mechanically; it would break the tour.
        // Every gesture demanded is a place the user can decide they are being
        // made to work, and a skipped tour teaches nothing at all. One is the
        // budget, and it is spent on the gesture that matters most.
        //
        // ⚠️ It counts REQUIRED actions, and that narrowing is the point of the
        // test rather than a weakening of it. Before 2026-08-24 `action != null`
        // and `is demanded` were the same fact, so counting either measured the
        // budget. They are now separate: an INVITED action opens the spotlight's
        // hole so the control underneath really works, and leaves Next in place.
        // A user who ignores it presses Next exactly as on any other step, so it
        // costs nothing on this budget — and counting it here would forbid the
        // fix Ido asked for while claiming to protect a tour nobody is being
        // made to work through.
        val demanded = TutorialStep.entries.filter { it.action?.required == true }

        assertWithMessage(
            "every step here withholds its Next button until the user performs a gesture",
        ).that(demanded.map { it.name }).hasSize(1)
    }

    @Test
    fun `every step that points at a live control offers to open it`() {
        // The defect Ido reported on 2026-08-24, as an assertion: the spotlight
        // ringed the Calendar tab, the blockers covered the hole, and the tap
        // landed on the scrim and advanced the tour. The invariant that stops it
        // coming back is that a step naming a bottom-bar TAB must carry an
        // action — because `TutorialOverlay` opens the hole exactly when there
        // is one, and a tab's whole result is a destination the tour can follow
        // the user to.
        val tabSteps = TutorialStep.entries.filter {
            it.anchor == TutorialAnchor.TAB_GOALS || it.anchor == TutorialAnchor.TAB_CALENDAR
        }

        assertWithMessage("no tab step found -- this test has stopped testing anything")
            .that(tabSteps).isNotEmpty()
        tabSteps.forEach { step ->
            assertWithMessage(
                "${step.name} rings a tab the user cannot press: it has no action, so " +
                    "TutorialOverlay leaves the spotlight's hole covered and the tap is eaten " +
                    "by the scrim",
            ).that(step.action).isNotNull()
        }
    }

    @Test
    fun `the last step names where the tour lives afterwards`() {
        // The tour records itself as seen the moment it is skipped, which is
        // only honest because it can be got back. The last step is half of that
        // promise (Settings' Help section is the other half), so it must be the
        // step that points at the door to Settings.
        assertThat(TutorialStep.entries.last().anchor).isEqualTo(TutorialAnchor.AVATAR)
    }

    @Test
    fun `only the opening step points at nothing`() {
        val anchorless = TutorialStep.entries.filter { it.anchor == null }

        assertThat(anchorless).containsExactly(TutorialStep.WELCOME)
    }

    @Test
    fun `every anchor the tour names is actually applied by a screen`() {
        val tagged = taggedAnchors()
        val missing = TutorialStep.entries
            .mapNotNull { it.anchor }
            .distinct()
            .filterNot { it in tagged }
            .map { it.name }

        assertWithMessage(
            "No source file calls Modifier.tutorialAnchor(TutorialAnchor.<these>), so the " +
                "step naming them spotlights nothing — and it fails SILENTLY: the overlay " +
                "falls back to a centred card and the tour goes on reading correctly.",
        ).that(missing).isEmpty()
    }

    @Test
    fun `no anchor is declared and then unused`() {
        // The other direction, and the reason it is worth a test of its own: an
        // anchor applied to a widget by a screen that no step ever points at is
        // an onGloballyPositioned callback running on every scroll frame for
        // nothing. Cheap, invisible, and it accumulates.
        val used = TutorialStep.entries.mapNotNull { it.anchor }.toSet()
        val orphans = (TutorialAnchor.entries.toSet() - used).map { it.name }

        assertThat(orphans).isEmpty()
    }

    /** Every `TutorialAnchor.X` that appears inside a `tutorialAnchor(...)` call in main sources. */
    private fun taggedAnchors(): Set<TutorialAnchor> {
        val call = Regex("""tutorialAnchor\s*\(\s*TutorialAnchor\.([A-Z_]+)\s*\)""")
        return sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            // The package that DEFINES the modifier does not count as a caller:
            // its KDoc and its own signature mention every name, so counting it
            // would make this test pass on an app where no screen tags anything.
            .filterNot { it.parentFile?.name == "tutorial" }
            .flatMap { file -> call.findAll(stripComments(file.readText())).map { it.groupValues[1] } }
            .mapNotNull { name -> TutorialAnchor.entries.firstOrNull { it.name == name } }
            .toSet()
    }

    /** So a KDoc sentence naming a call is never read as one. */
    private fun stripComments(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\n]*"""), "")
}
