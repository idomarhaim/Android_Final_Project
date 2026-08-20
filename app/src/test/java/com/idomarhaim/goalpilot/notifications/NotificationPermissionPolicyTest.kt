package com.idomarhaim.goalpilot.notifications

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [NotificationPermissionPolicy] — #8 piece 2, *when* to ask.
 *
 * The decision is a table, and its whole value is in two rows that are easy to get backwards:
 * **do not ask before something has happened**, and **do not ask twice**. Both fail silently on
 * a device — the first as a dialog at launch that most people deny, the second as two stacked
 * dialogs nobody can reproduce on purpose.
 */
class NotificationPermissionPolicyTest {

    private val tiramisu = Build.VERSION_CODES.TIRAMISU

    @Test
    fun `below API 33 there is nothing to request and posting is allowed`() {
        val step = NotificationPermissionPolicy.decide(
            sdkInt = tiramisu - 1,
            granted = false,
            askedThisProcess = false,
            eventSpeaks = true,
        )
        assertThat(step).isEqualTo(PermissionStep.NOT_APPLICABLE)
        assertThat(step.canPost).isTrue()
    }

    @Test
    fun `a silent filing outcome never raises the dialog`() {
        // FilingDecision.ExistingGoal. §3.4 calls this row silent; interrupting the user to ask
        // about a notification they were never going to get is the launch-time prompt in
        // disguise.
        val step = NotificationPermissionPolicy.decide(
            sdkInt = tiramisu,
            granted = false,
            askedThisProcess = false,
            eventSpeaks = false,
        )
        assertThat(step).isEqualTo(PermissionStep.WAIT_FOR_A_REASON)
        assertThat(step.canPost).isFalse()
    }

    @Test
    fun `the first speaking outcome is what raises the dialog`() {
        val step = NotificationPermissionPolicy.decide(
            sdkInt = tiramisu,
            granted = false,
            askedThisProcess = false,
            eventSpeaks = true,
        )
        assertThat(step).isEqualTo(PermissionStep.ASK_NOW)
    }

    @Test
    fun `a second speaking outcome in the same process does not stack a second dialog`() {
        val step = NotificationPermissionPolicy.decide(
            sdkInt = tiramisu,
            granted = false,
            askedThisProcess = true,
            eventSpeaks = true,
        )
        assertThat(step).isEqualTo(PermissionStep.ALREADY_ASKED)
        assertThat(step.canPost).isFalse()
    }

    @Test
    fun `once granted the answer is post, whatever else is true`() {
        listOf(true, false).forEach { asked ->
            val step = NotificationPermissionPolicy.decide(
                sdkInt = tiramisu,
                granted = true,
                askedThisProcess = asked,
                eventSpeaks = true,
            )
            assertThat(step).isEqualTo(PermissionStep.ALREADY_GRANTED)
            assertThat(step.canPost).isTrue()
        }
    }

    @Test
    fun `granted outranks a silent event - a granted app is not asked to wait for a reason`() {
        val step = NotificationPermissionPolicy.decide(
            sdkInt = tiramisu,
            granted = true,
            askedThisProcess = false,
            eventSpeaks = false,
        )
        assertThat(step).isEqualTo(PermissionStep.ALREADY_GRANTED)
    }

    @Test
    fun `exactly two steps permit posting`() {
        // The guard on canPost: adding a state later must force a decision about it rather than
        // inheriting `false` and silently muting the app.
        val posting = PermissionStep.entries.filter { it.canPost }
        assertThat(posting)
            .containsExactly(PermissionStep.NOT_APPLICABLE, PermissionStep.ALREADY_GRANTED)
    }
}
