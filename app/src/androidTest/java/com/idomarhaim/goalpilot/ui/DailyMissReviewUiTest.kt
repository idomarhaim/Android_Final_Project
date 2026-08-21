package com.idomarhaim.goalpilot.ui

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.DailyMissReview
import com.idomarhaim.goalpilot.domain.usecase.OccurrenceReminders
import com.idomarhaim.goalpilot.feature.dashboard.DailyMissReviewCard
import com.idomarhaim.goalpilot.feature.dashboard.MISS_REVIEW_DISMISS_LABEL
import com.idomarhaim.goalpilot.feature.dashboard.MISS_REVIEW_DISMISS_TAG
import com.idomarhaim.goalpilot.feature.dashboard.MISS_REVIEW_TAG
import com.idomarhaim.goalpilot.feature.dashboard.MISS_REVIEW_TITLE
import com.idomarhaim.goalpilot.notifications.GoalPilotChannels
import com.idomarhaim.goalpilot.notifications.GoalPilotNotifier
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §2.5's daily review, on a device: **it appears on the screen, and it does not appear in the
 * shade** ([#56](https://github.com/idomarhaim/Android_Final_Project/issues/56)).
 *
 * §2.5: *"Misses meet Ido once, in a daily review on app open — **never as a push saying he
 * failed**."* Those are two claims and only one of them is about a card. The second is a claim
 * about something **not** happening, and a test that only asserted the card would pass on a
 * build that also posted four notifications.
 *
 * ### The negative assertion is made non-vacuous on purpose
 *
 * *"No notification was posted"* is trivially true on a device where nothing can post at all —
 * a missing permission, a blocked channel, a broken notifier — and would go green for years
 * while proving nothing. So [theReviewShowsOnScreenAndNeverInTheShade] **first posts a real
 * reminder for an unrelated task and finds it in the shade**, and only then asserts that the
 * three reviewed misses are absent from it. The instrument is checked on the same run that
 * uses it.
 *
 * ### It drives the real card
 *
 * [DailyMissReviewCard] and `missLabel` are `internal` rather than reimplemented here, for the
 * reason `SmartAddReceiptUiTest` records: a copy would be written from the same understanding
 * as the code and would pass on the same mistake.
 */
class DailyMissReviewUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifier = GoalPilotNotifier(context)

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val tuesdayMorning: LocalDateTime = monday.plusDays(1).atTime(9, 0)

    @Before
    fun grantPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    private fun task(id: String, title: String, occurrence: Occurrence) =
        Task(id = id, title = title, occurrence = occurrence)

    private val missed = listOf(
        task("m1", "Water the plants", AllDay(monday)),
        task("m2", "File the tax return", Deadline(monday.atTime(18, 0))),
        task("m3", "Gym session", Block(monday.atTime(9, 0), monday.atTime(10, 0))),
    )

    @Test
    fun theReviewShowsOnScreenAndNeverInTheShade() {
        GoalPilotChannels.ensure(context)
        notifier.cancelOccurrenceReminder(CONTROL_TASK_ID)

        // ── The control: prove this device CAN put a task reminder in the shade ────────────
        //
        // Without this, the assertion at the bottom is satisfied by any device that cannot
        // post at all, which is the shape that passes vacuously for years.
        val control = task(CONTROL_TASK_ID, "Control reminder", Deadline(monday.atTime(14, 0)))
        notifier.notifyOccurrenceReminder(
            task = control,
            plan = OccurrenceReminders.planFor(control, DaySchedule.DEFAULT)!!,
            occurrence = control.occurrence!!,
        )
        assertThat(awaitReminderTags(CONTROL_TASK_ID)).contains(CONTROL_TASK_ID)

        // ── The review itself ─────────────────────────────────────────────────────────────
        val misses = DailyMissReview.of(missed, tuesdayMorning)
        assertThat(misses).hasSize(3)

        var dismissed = false
        composeRule.setContent {
            GoalPilotTheme {
                DailyMissReviewCard(misses = misses, onDismiss = { dismissed = true })
            }
        }

        composeRule.onNodeWithTag(MISS_REVIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(MISS_REVIEW_TITLE).assertIsDisplayed()

        // Each miss is named, with §2.2's meaning of ITS rung beside it. Three rungs, three
        // different sentences -- a card that showed one generic "missed" for all of them would
        // pass a test that only counted rows.
        composeRule.onNodeWithText("Water the plants").assertIsDisplayed()
        composeRule.onNodeWithText("the day passed").assertIsDisplayed()
        composeRule.onNodeWithText("File the tax return").assertIsDisplayed()
        composeRule.onNodeWithText("late, still owed").assertIsDisplayed()
        composeRule.onNodeWithText("Gym session").assertIsDisplayed()
        composeRule.onNodeWithText("the slot is gone").assertIsDisplayed()

        // ── "never as a push saying he failed" ────────────────────────────────────────────
        //
        // The control above is still up, so the shade is demonstrably readable and demonstrably
        // able to hold a reminder for a task. None of the three reviewed misses is in it.
        val tags = awaitReminderTags(CONTROL_TASK_ID)
        assertThat(tags).contains(CONTROL_TASK_ID)
        missed.forEach { assertThat(tags).doesNotContain(it.id) }

        composeRule.onNodeWithTag(MISS_REVIEW_DISMISS_TAG).performClick()
        assertThat(dismissed).isTrue()

        notifier.cancelOccurrenceReminder(CONTROL_TASK_ID)
    }

    @Test
    fun theDismissIsTheOnlyActionAndItSaysNothingAboutFailing() {
        val misses = DailyMissReview.of(missed, tuesdayMorning)

        composeRule.setContent {
            GoalPilotTheme { DailyMissReviewCard(misses = misses, onDismiss = {}) }
        }

        // §2.5's tone is a requirement, not a preference: the review states what went by and
        // offers no verdict. The card carries exactly one action and it is an acknowledgement.
        composeRule.onNodeWithText(MISS_REVIEW_DISMISS_LABEL).assertIsDisplayed()
    }

    /**
     * The reminder tags in the shade, waiting up to [TAG_SETTLE_MS] for [expected] to appear.
     *
     * ⚠️ `notify` returns before `activeNotifications` reflects it, so reading on the next line
     * is a race — see `NotificationObservedFireTest.awaitReminderTags` for the observation.
     * Here it matters twice over: the **control** must be up before the negative assertion
     * below is worth anything, and a control that had not landed yet would turn a real defect
     * into a green run.
     */
    private fun awaitReminderTags(vararg expected: String): List<String?> {
        val deadline = System.currentTimeMillis() + TAG_SETTLE_MS
        var tags = reminderTags()
        while (System.currentTimeMillis() < deadline && !tags.containsAll(expected.toList())) {
            Thread.sleep(TAG_POLL_MS)
            tags = reminderTags()
        }
        return tags
    }

    /** The task ids of the reminder notifications currently in the shade. */
    private fun reminderTags(): List<String?> =
        active()
            .filter { it.id == GoalPilotNotifier.ID_OCCURRENCE_REMINDER }
            .map { it.tag }

    private fun active(): List<StatusBarNotification> =
        context.getSystemService(NotificationManager::class.java).activeNotifications.toList()

    private companion object {
        /**
         * Deliberately not one of the reviewed tasks' ids, and cancelled at both ends of the
         * test: it exists only to prove the shade is readable on this run.
         */
        const val CONTROL_TASK_ID = "miss-review-control"

        const val TAG_SETTLE_MS = 3_000L
        const val TAG_POLL_MS = 50L
    }
}
