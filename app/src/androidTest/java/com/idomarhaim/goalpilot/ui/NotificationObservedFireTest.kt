package com.idomarhaim.goalpilot.ui

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.usecase.OccurrenceReminders
import com.idomarhaim.goalpilot.notifications.GoalPilotChannels
import com.idomarhaim.goalpilot.notifications.GoalPilotNotifier
import com.idomarhaim.goalpilot.notifications.NotificationDeepLink
import com.idomarhaim.goalpilot.ui.navigation.Routes
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * **The notification actually appearing.** The brief's trap, in its own words: *"a notification
 * you cannot see is a notification you have not built"* — every layer here is asynchronous and
 * time-dependent, so the tests pass while the thing never fires on a real phone.
 *
 * This suite is the answer to that, and it is deliberately **not** self-contained: it posts and
 * **leaves the notifications up**, so `adb shell dumpsys notification --noredact` run afterwards
 * shows them in the shade. That is why it does not `cancelAll` in a teardown, and why it must be
 * run through `adb shell am instrument` rather than `connectedDebugAndroidTest` — the Gradle
 * task **uninstalls the app when it finishes**, taking its notifications with it, so the
 * evidence would be gone before anyone could read it (`AGENTS.md`,
 * `kb/dev/android-device-verification.md` §8).
 *
 * The permission is asserted here rather than assumed: an ungranted run must fail loudly, because
 * the failure mode this exists to prevent is a green suite that posted nothing.
 *
 * ⚠️ **It is also granted here, from `@Before`, and that is a change to this file's original
 * procedure — made 2026-08-21 by `c13-key-store`, not by `#8`.** The KDoc used to say the grant
 * came from **outside**, by `adb shell pm grant`. That is true of a human running it, and it is
 * **false of CI**: `.github/workflows/instrumented-tests.yml` has no grant step, so from the
 * moment this suite landed (2026-08-20 12:57) **every push to `main` went red** — four runs,
 * across three sessions' commits, none of which had anything to do with notifications.
 *
 * **Granting does not weaken the assertion, and that is why this is the fix rather than an
 * `assumeTrue`.** `uiAutomation.grantRuntimePermission` performs the real grant; if it does not
 * take, `requirePermission()` still fails exactly as loudly as before. What changes is only that
 * the common CI case stops being a false red. A skip would have been the weakening move — it
 * turns *"nothing was posted"* into a green run, which is the precise failure this suite exists
 * to catch.
 *
 * The other half of the original note still stands and is untouched: for a **human** collecting
 * evidence, run it through `adb shell am instrument`, because `connectedDebugAndroidTest`
 * uninstalls the app at the end and takes the posted notifications with it. CI does not read the
 * shade afterwards, so that concern does not reach it — the assertions here are what CI checks.
 */
class NotificationObservedFireTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifier = GoalPilotNotifier(context)

    /**
     * POST_NOTIFICATIONS is a runtime permission from API 33, and nothing in CI grants it.
     * Guarded on the API level because the permission does not exist below 33 and the grant
     * would fail on a device where none was ever needed.
     */
    @Before
    fun grantPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    @Test
    fun theFilingNotificationReallyAppearsInTheShade() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        notifier.notifyFiling(
            decision = FilingDecision.NewGoal(
                title = "Run better retrospectives",
                category = GoalCategory.CAREER,
                lifeAreaId = null,
            ),
            taskTitle = "Draft the retrospective",
            createdGoalId = "observed-fire-goal",
        )

        // BY ID, not by channel: with both notifications up the system adds its own
        // AUTOGROUP_SUMMARY record carrying the first one's channel, so a channel filter
        // matches two and this fails only in the run orders that leave both posted.
        val posted = awaitPosted(GoalPilotNotifier.ID_FILING)
        assertThat(posted.notification.channelId()).isEqualTo(GoalPilotChannels.FILING)
        assertThat(posted.notification.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("No goal fitted")
        assertThat(posted.notification.extras.getString(Notification.EXTRA_TEXT))
            .contains("Run better retrospectives")
        // The tap-through is the piece a screenshot cannot show.
        assertThat(posted.notification.contentIntent).isNotNull()
    }

    @Test
    fun thePlanTomorrowNotificationReallyAppearsInTheShade() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        notifier.notifyPlanTomorrow()

        val posted = awaitPosted(GoalPilotNotifier.ID_PLAN_TOMORROW)
        assertThat(posted.notification.channelId()).isEqualTo(GoalPilotChannels.REMINDERS)
        assertThat(posted.notification.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("Plan tomorrow")
        assertThat(posted.notification.contentIntent).isNotNull()
    }

    @Test
    fun bothChannelsExistWithTheirUserFacingText() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        GoalPilotChannels.ensure(context)
        val manager = context.getSystemService(NotificationManager::class.java)

        val filing = manager.getNotificationChannel(GoalPilotChannels.FILING)
        val reminders = manager.getNotificationChannel(GoalPilotChannels.REMINDERS)

        // These strings are what the SYSTEM renders on a screen this app never draws, so an
        // empty description is invisible to every other layer of testing.
        assertThat(filing.name.toString()).isEqualTo("Smart filing")
        assertThat(filing.description).isNotEmpty()
        assertThat(reminders.name.toString()).isEqualTo("Reminders and planning")
        assertThat(reminders.description).isNotEmpty()
    }

    @Test
    fun theTapThroughCarriesTheRouteOfTheGoalItIsTalkingAbout() {
        // #8 piece 3. dumpsys can show that a contentIntent EXISTS; only this shows that it
        // carries the right destination, which is the half that can be wrong silently -- a
        // notification about a proposed goal that opens the dashboard has still "worked".
        val intent = NotificationDeepLink.intentFor(context, Routes.goalDetail("g-42"))
        NotificationDeepLink.offer(intent)
        assertThat(NotificationDeepLink.consume()).isEqualTo("goal_detail/g-42")
    }

    @Test
    fun theRouteIsConsumedOnceSoAConfigurationChangeCannotReNavigate() {
        NotificationDeepLink.offer(NotificationDeepLink.intentFor(context, Routes.DASHBOARD))
        assertThat(NotificationDeepLink.consume()).isEqualTo(Routes.DASHBOARD)
        // The second read is what a rotation would do. It must find nothing, or the user is
        // thrown back to a notification they already followed and walked away from.
        assertThat(NotificationDeepLink.consume()).isNull()
    }

    @Test
    fun anIntentWithNoRouteLeavesThePendingRouteAlone() {
        NotificationDeepLink.consume()
        NotificationDeepLink.offer(android.content.Intent())
        assertThat(NotificationDeepLink.consume()).isNull()
    }

    // ── §2.5's occurrence reminder, and the sentence that is the whole feature (`#56`) ────

    /**
     * **The differentiator, in the shade.** §2.5 calls the deadline reminder *"the one thing
     * this app knows that Google Calendar does not"* — and what makes that true is the
     * sentence, not the timing. A calendar can also fire at 22:59; it cannot say why.
     *
     * So this asserts the **copy**, word for word, on the case §2.5 itself writes down: due at
     * 06:00, about four hours of work, therefore *worth starting tonight*. It is
     * [ReminderPlan.movedForSleep] that selects that wording, and this is the only layer that
     * can show it actually reached a notification.
     *
     * Left up on purpose, like everything else in this suite, so `adb shell dumpsys
     * notification --noredact` shows it to a human afterwards.
     */
    @Test
    fun theDeadlineReminderReallyAppearsAndSaysWhyItMoved() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        val due = LocalDate.of(2026, 8, 22).atTime(6, 0)
        val task = Task(
            id = OBSERVED_TASK_ID,
            title = "Finish the quarterly report",
            // Typed, and four hours: #9's sticky duration is what drives this reminder to the
            // previous evening. An estimate could not have moved it there.
            estimatedMinutes = 240,
            durationSource = DurationSource.USER,
            occurrence = Deadline(due),
        )
        val plan = OccurrenceReminders.planFor(task, DaySchedule.DEFAULT)!!

        notifier.notifyOccurrenceReminder(task, plan, task.occurrence!!)

        val posted = awaitPosted(GoalPilotNotifier.ID_OCCURRENCE_REMINDER, OBSERVED_TASK_ID)
        assertThat(posted.notification.channelId()).isEqualTo(GoalPilotChannels.REMINDERS)
        // The task's own title, not a generic "Reminder": the shade is read at a glance.
        assertThat(posted.notification.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("Finish the quarterly report")

        val body = posted.notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()
        // §2.5's three components: the deadline, how long it takes, and why the reminder moved.
        assertThat(body).contains("4h")
        assertThat(body).contains("worth starting tonight")
        // And it really did move -- so the copy above is a report, not a slogan.
        assertThat(plan.movedForSleep).isTrue()
        assertThat(plan.fireAt).isEqualTo(due.toLocalDate().minusDays(1).atTime(22, 59))

        assertThat(posted.notification.contentIntent).isNotNull()
    }

    /**
     * A reminder that did **not** have to move says *now*, not *tonight*.
     *
     * The negative half of the case above: without it, a build that hard-coded the tonight
     * wording would pass every assertion in this file.
     */
    @Test
    fun aReminderThatDidNotMoveDoesNotClaimItDid() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        val task = Task(
            id = UNMOVED_TASK_ID,
            title = "Call the plumber",
            estimatedMinutes = 30,
            occurrence = Deadline(LocalDate.of(2026, 8, 22).atTime(14, 0)),
        )
        val plan = OccurrenceReminders.planFor(task, DaySchedule.DEFAULT)!!
        assertThat(plan.movedForSleep).isFalse()

        notifier.notifyOccurrenceReminder(task, plan, task.occurrence!!)

        val body = awaitPosted(GoalPilotNotifier.ID_OCCURRENCE_REMINDER, UNMOVED_TASK_ID)
            .notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()

        assertThat(body).contains("worth starting now")
        assertThat(body).doesNotContain("tonight")
    }

    /**
     * **Two tasks due the same evening are two notifications**, not one that replaced the other.
     *
     * The one-id-per-kind rule the rest of this class follows is right for a filing receipt and
     * would silently drop half the user's evening here. The tag is what keeps them apart, and
     * nothing else in the suite would notice if it were removed.
     */
    @Test
    fun twoRemindersForTwoTasksBothStayUp() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        listOf("pair-a" to "Pack for the trip", "pair-b" to "Renew the passport").forEach { (id, title) ->
            val task = Task(
                id = id,
                title = title,
                estimatedMinutes = 60,
                occurrence = Deadline(LocalDate.of(2026, 8, 22).atTime(20, 0)),
            )
            notifier.notifyOccurrenceReminder(
                task,
                OccurrenceReminders.planFor(task, DaySchedule.DEFAULT)!!,
                task.occurrence!!,
            )
        }

        assertThat(awaitReminderTags("pair-a", "pair-b")).containsAtLeast("pair-a", "pair-b")

        notifier.cancelOccurrenceReminder("pair-a")
        notifier.cancelOccurrenceReminder("pair-b")
    }

    /**
     * Cancelling takes one task's reminder down and leaves the others alone.
     *
     * This is what `OccurrenceReminderWorker` does when the re-check says the task is done, and
     * it is the difference between a stale reminder disappearing and the whole shade being
     * cleared out from under the user.
     */
    @Test
    fun cancellingOneTasksReminderLeavesTheOthersUp() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        val keep = Task(
            id = "cancel-keep",
            title = "Keep me",
            estimatedMinutes = 60,
            occurrence = Deadline(LocalDate.of(2026, 8, 22).atTime(20, 0)),
        )
        val drop = keep.copy(id = "cancel-drop", title = "Drop me")
        listOf(keep, drop).forEach {
            notifier.notifyOccurrenceReminder(
                it,
                OccurrenceReminders.planFor(it, DaySchedule.DEFAULT)!!,
                it.occurrence!!,
            )
        }

        // Wait for BOTH to land before cancelling, or the cancel can outrun the post it is
        // meant to undo and the negative assertion below passes for the wrong reason.
        awaitReminderTags("cancel-keep", "cancel-drop")
        notifier.cancelOccurrenceReminder("cancel-drop")

        // A CANCEL propagates asynchronously too, and waiting for `cancel-keep` does not wait
        // for it: that tag was already present, so the poll returns immediately and the read
        // still catches `cancel-drop` on its way out. `Observed:` 2026-08-21, in the full-suite
        // run only. So the wait has to be for the ABSENCE, which is the thing being asserted.
        val tags = awaitReminderTagsGone("cancel-drop")
        assertThat(tags).contains("cancel-keep")
        assertThat(tags).doesNotContain("cancel-drop")

        notifier.cancelOccurrenceReminder("cancel-keep")
    }

    /**
     * Takes down only the reminders these cases posted for their own bookkeeping.
     *
     * ⚠️ **Never `cancelAll`**, and never the two observed ones: this suite's whole point is
     * that it *leaves the evidence up* for `dumpsys` to show a human, which is why it has no
     * ordinary teardown. See the class KDoc.
     */
    @After
    fun leaveTheEvidenceUp() = Unit

    private fun requirePermission() {
        assertThat(notifier.hasPostPermission()).isTrue()
        assertThat(notifier.canPost()).isTrue()
    }

    private companion object {
        /** Named so `dumpsys` output can be traced back to the case that posted it. */
        const val OBSERVED_TASK_ID = "observed-deadline"
        const val UNMOVED_TASK_ID = "observed-unmoved"

        /** Generous, because it costs nothing on the runs that do not need it. */
        const val TAG_SETTLE_MS = 3_000L
        const val TAG_POLL_MS = 50L
    }

    /**
     * The notification with this [id] (and [tag], when it has one), **waiting up to
     * [TAG_SETTLE_MS] for it to appear**.
     *
     * ⚠️ This wait was added for the reminders and then had to be applied to the two cases that
     * predate `#56`, because they raced too. `Observed:` 2026-08-21 — the filing case, unchanged
     * since `#8`, failed on a full-suite run with `NoSuchElementException` and passed alone. It
     * is the same asynchrony as below and not
     * [#58](https://github.com/idomarhaim/Android_Final_Project/issues/58)'s IME hypothesis;
     * `#56` did not cause it, but `#56` posts four more notifications and so makes it likelier.
     *
     * Still an assertion, not an `assume`: a notification that never posts fails here, just
     * [TAG_SETTLE_MS] later.
     */
    private fun awaitPosted(id: Int, tag: String? = null): StatusBarNotification {
        val deadline = System.currentTimeMillis() + TAG_SETTLE_MS
        fun found() = active().singleOrNull { it.id == id && (tag == null || it.tag == tag) }
        while (System.currentTimeMillis() < deadline && found() == null) {
            Thread.sleep(TAG_POLL_MS)
        }
        return checkNotNull(found()) {
            "No notification with id=$id tag=$tag after ${TAG_SETTLE_MS}ms. In the shade: " +
                active().joinToString { "id=${it.id} tag=${it.tag}" }
        }
    }

    /**
     * The reminder tags in the shade, **waiting up to [TAG_SETTLE_MS] for [expected] to appear**.
     *
     * ⚠️ `NotificationManagerCompat.notify` returns before `activeNotifications` reflects the
     * post — it is a binder hop to `NotificationManagerService` — so reading the shade on the
     * next line is a race. `Observed:` 2026-08-21 on `emulator-5554`: two reminders posted back
     * to back, and the read found only the first. The same case **passed 3/3 in isolation**,
     * which is the shape [#58](https://github.com/idomarhaim/Android_Final_Project/issues/58)
     * already records for this suite.
     *
     * **This does not weaken the assertion.** The wait is bounded and the caller still asserts
     * on what comes back, so a genuinely missing tag — a notification that replaced another
     * instead of sitting beside it — still fails, just [TAG_SETTLE_MS] later. What it removes
     * is only the false red.
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

    /**
     * The reminder tags in the shade, **waiting up to [TAG_SETTLE_MS] for [gone] to disappear**.
     *
     * The mirror of [awaitReminderTags], and needed for the same reason: `cancel` returns before
     * `activeNotifications` stops reporting the notification. Bounded, so a cancel that genuinely
     * does nothing still fails the assertion that follows.
     */
    private fun awaitReminderTagsGone(vararg gone: String): List<String?> {
        val deadline = System.currentTimeMillis() + TAG_SETTLE_MS
        var tags = reminderTags()
        while (System.currentTimeMillis() < deadline && tags.any { it in gone }) {
            Thread.sleep(TAG_POLL_MS)
            tags = reminderTags()
        }
        return tags
    }

    /** The reminder notifications currently in the shade, by the task id each carries. */
    private fun reminderTags(): List<String?> =
        active()
            .filter { it.id == GoalPilotNotifier.ID_OCCURRENCE_REMINDER }
            .map { it.tag }

    private fun active(): List<StatusBarNotification> =
        context.getSystemService(NotificationManager::class.java).activeNotifications.toList()

    private fun Notification.channelId(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) channelId else null
}
