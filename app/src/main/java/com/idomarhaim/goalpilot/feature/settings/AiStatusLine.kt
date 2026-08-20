package com.idomarhaim.goalpilot.feature.settings

import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiKeyFailure

/**
 * §4.9's AI status line — **which provider answered**, in a sentence
 * (#54 piece 3, #32 §5).
 *
 * ## Why this is the honesty half of `C13`
 *
 * The moment a key exists the app has two credentials and three rungs, and
 * nothing on any screen says which one ran. That is `docs/PRODUCT_v0.3.md`
 * §0.3's *second number that quietly disagrees* in a new costume: a revoked key
 * silently riding the free model looks **identical** to a key that works. Three
 * weeks later the user is on the free tier and no screen has ever said so.
 *
 * §5 answered it with two things that must both exist:
 *
 * * **a message at the point of use, once**, for a key only the user can fix
 *   ([AiKeyFailure.DEAD]) — that is `deadKeyUnannounced`, not this function;
 * * **a permanent status row here**, covering **every** class *including the
 *   silent ones*. The message tells you now; the row tells you later.
 *
 * `C7`'s house rule, which this is an instance of: **legal, but never silent.**
 *
 * ## Why it is a pure function and not a `when` inside the composable
 *
 * [SettingsContent]'s own KDoc says §4.9's consequence lines are *"arithmetic
 * rendered as a sentence, and the only way to catch one that silently stops
 * moving with its setting is to move the setting and read the sentence"*. This
 * one has **eight** reachable states across two inputs, which is more than a
 * render test wants to enumerate and exactly what a JVM test is for —
 * `AiStatusLineTest` moves both and reads all eight.
 *
 * English literals rather than `res/` strings, matching every other sentence in
 * `feature/settings/`: this package is **unswept** by #51's literal sweep
 * (`AnalyticsLiteralSweepTest.SWEPT_PACKAGES`), and half-resourcing one card
 * would leave the section harder to sweep than it is now, not easier.
 *
 * @param credential what Settings holds — `null` is the default and the state
 *   almost every install is in.
 * @param answer who answered the last call **this process** made, or `null`
 *   before there has been one.
 */
fun aiStatusLine(credential: AiCredential?, answer: AiAnswer?): String {
    // No key: there is exactly one credential and no ambiguity to resolve. The
    // row still speaks, because "GoalPilot has a free model and you are on it"
    // is the fact this section exists to make legible.
    if (credential == null) {
        return when (answer) {
            is AiAnswer.Local ->
                "GoalPilot's own free model answers. It could not be reached last time, " +
                    "so offline guidance was used."
            else -> "GoalPilot's own free model answers. You have not added a key."
        }
    }

    val name = credential.provider.displayName
    return when (answer) {
        // Nothing has gone through yet. Deliberately NOT "your key answers" —
        // it has not answered anything, and asserting it would be the same
        // unearned confidence the whole status line exists against.
        null -> "$name is set. Nothing has been asked yet, so it has not answered yet."

        is AiAnswer.UserKey -> "Answered by your $name key."

        is AiAnswer.Proxy -> when (answer.keyFailure) {
            AiKeyFailure.DEAD ->
                "Your $name key was rejected — GoalPilot's free model answered instead. " +
                    "Check the key, or remove it."
            AiKeyFailure.QUOTA ->
                "Your $name key is out of quota — GoalPilot's free model answered instead. " +
                    "It will be used again when the quota resets."
            AiKeyFailure.TRANSIENT ->
                "$name did not respond — GoalPilot's free model answered instead. " +
                    "Nothing to fix; it will be retried."
            // The proxy answered and said nothing about the key. Two ways to get
            // here and they are the same fact: a deployed function that predates
            // C13 and ignored the key, or one that was never sent it. Either way
            // the free model really did answer, and naming the provider here
            // would put its name on a call it never saw.
            null -> "GoalPilot's free model answered, not your $name key."
        }

        // Neither credential was reached: spec §8's local heuristics spoke. Kept
        // distinct from every Proxy case above, because "the free model
        // answered" would be false.
        //
        // The DEAD branch is the combination that used to disappear: a rejected
        // key AND an outage behind it. Two facts, and the first is the one only
        // the user can act on, so it is said first.
        is AiAnswer.Local -> when (answer.keyFailure) {
            AiKeyFailure.DEAD ->
                "Your $name key was rejected, and GoalPilot's free model could not be " +
                    "reached either — offline guidance was used. Check the key, or remove it."
            else ->
                "Nothing could be reached — GoalPilot used its own offline guidance. " +
                    "Your $name key was not the problem."
        }
    }
}

/**
 * The consequence line under the AI card — §4.9's *what this setting does to
 * the app*, stated from the setting rather than described.
 *
 * It moves with [credential] because #32 §6 fixed **quality only**: the ladder
 * changes who answers and nothing else, so the honest consequence of adding a
 * key is a *better answerer*, never a new feature. Saying so on the screen is
 * what stops the section reading like a paywall.
 */
fun aiConsequenceLine(credential: AiCredential?): String = if (credential == null) {
    "Everything here works without a key. Adding one changes who answers, not what " +
        "GoalPilot can do."
} else {
    "Used for every AI answer: your feed, task filing and time estimates. Same features, " +
        "same screens — a better answerer. If ${credential.provider.displayName} fails, " +
        "GoalPilot's free model answers instead."
}
