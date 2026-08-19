package com.idomarhaim.goalpilot.domain.model

/**
 * Whether the signed-in account actually holds the Google Tasks scope.
 *
 * [MISSING] is the normal case rather than an edge case (spec §2.6, issue #36):
 * Google's granular consent screen shows *"View your tasks"* **unticked**, so a
 * sign-in can succeed while granting nothing. Until this was read up front the
 * only way to find out was to run an import and watch it fail, which is why a
 * declined scope and a first-ever run were pixel-identical.
 *
 * This lives in `domain/` rather than beside `GoogleTasksClient` because the two
 * screens that render it are `feature/`, and `feature → domain → data` is the
 * layering (AGENTS.md). Its siblings `TasksImportResult` / `TaskListsResult`
 * cannot follow it here — they carry an Android `Intent`, and domain holds no
 * Android types.
 */
enum class TasksConsent {
    /** The scope is held; an import can run. */
    GRANTED,

    /** Signed in, but the Tasks scope is not granted. */
    MISSING,

    /**
     * No Google account is cached, so nothing has been asked and nothing was
     * declined. Deliberately distinct from [MISSING]: telling someone they left
     * a box unticked when they were never shown it is the same class of lie the
     * generic "grant permission" prompt already told.
     */
    NOT_SIGNED_IN,
}
