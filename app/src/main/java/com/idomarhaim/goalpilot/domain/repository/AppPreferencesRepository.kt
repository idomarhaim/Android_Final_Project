package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppRelief
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import kotlinx.coroutines.flow.StateFlow

/**
 * Device-local UI preferences. Nothing here is synced to Firestore — a skin is a
 * property of *this install*, not of the account.
 *
 * [skin] and [language] are [StateFlow]s rather than cold `Flow`s so the very
 * first composition already holds the stored value; a cold flow would render one
 * frame in the default skin — or, worse, one frame in the wrong language and
 * direction — and then visibly repaint.
 */
interface AppPreferencesRepository {

    val skin: StateFlow<AppSkin>

    fun setSkin(skin: AppSkin)

    /**
     * The language the app speaks, per spec §5.1.
     *
     * **Device-local and beside the skin, on the same reasoning:** it must be
     * known before the first frame, and the account is not known until Auth
     * resolves. §5.1 states this directly — *"per-device, beside the skin"*.
     *
     * Only *speech* follows this. User-authored content — goal titles, task
     * titles, life-area names — is never translated (§8, closed scope): §5.1's
     * discriminator is speech vs content, and content never moves.
     */
    val language: StateFlow<AppLanguage>

    fun setLanguage(language: AppLanguage)

    /**
     * Light or dark, per spec §4.9's Appearance section.
     *
     * A [StateFlow] for the same reason [skin] is, and more sharply: brightness
     * is applied *outside* the theme in `MainActivity`, so a cold flow would
     * render the first frame at the system's brightness and then repaint the
     * whole window.
     */
    val brightness: StateFlow<AppBrightness>

    fun setBrightness(brightness: AppBrightness)

    /**
     * The surface the app is drawn out of, per spec §4.1 — the second axis
     * above [skin].
     *
     * A [StateFlow] for the same reason [skin] and [brightness] are, and it
     * inherits one extra duty from being *above* brightness: the material
     * decides whether the brightness setting can move at all
     * ([AppMaterial.resolveDark]), so a cold flow here would render the first
     * frame in a brightness the material forbids and then repaint the window.
     *
     * Device-local, like everything else here. §4.9's sign-out test puts
     * material in the left column beside skin and brightness.
     */
    val material: StateFlow<AppMaterial>

    fun setMaterial(material: AppMaterial)

    /**
     * The **ground** the app is drawn on, per spec §4.1's third axis — `#57` b,
     * and the half of Ido's complaint that asked for *"combinations between the
     * backgrounds and the blocks"*.
     *
     * A [StateFlow] beside [material] and for the same reason: the two are read
     * together to build `GpMaterialSpec` before the first frame, so a cold flow
     * here would render one frame on the wrong ground and then visibly repaint
     * the whole window.
     *
     * **Stored unresolved.** [AppBackground.MATCH] is persisted as itself and
     * resolved against the material at draw time, never flattened to the ground
     * it currently means. Storing the resolved value would silently pin a user
     * who never chose a ground to whichever one their material had when they
     * last changed materials — and the next material change would then not move
     * the page, which is the bug the default exists to avoid.
     *
     * Device-local like everything else here; §4.9's sign-out test puts every
     * appearance axis in the left column.
     */
    val background: StateFlow<AppBackground>

    fun setBackground(background: AppBackground)

    /**
     * Whether chart bodies are **extruded**, per spec §4.1's raised-3D toggle —
     * `#57` c's fourth axis, and Ido's *"3d graphs is an option that can be
     * implemented in addition on each of the design types"*.
     *
     * A [StateFlow] beside [material] and [background] for the same reason: all
     * three are read together to build `GpMaterialSpec` before the first frame,
     * so a cold flow here would draw one frame of flat charts and then repaint.
     *
     * **Orthogonal to [material], and stored that way.** It is not folded into
     * the material id even though the two only ever appear together — the whole
     * content of the decision this axis records is that raised is *not* a
     * property of a material (see [AppRelief]), and a composite key would encode
     * the overturned answer in the schema.
     *
     * Device-local like everything else here; §4.9's sign-out test puts every
     * appearance axis in the left column.
     */
    val relief: StateFlow<AppRelief>

    fun setRelief(relief: AppRelief)

    /**
     * Where the user is, for week start and date order — spec §5.1's **Region**,
     * decoupled from [language] on Ido's call (see [AppRegion]).
     *
     * **Stored and read out, not yet wired.** The app's date formatters are
     * `#51`'s and `#51` is deferred; §4.9's Settings screen states what this
     * choice means using [AppRegion]'s own arithmetic rather than asserting a
     * change nothing has made.
     */
    val region: StateFlow<AppRegion>

    fun setRegion(region: AppRegion)

    /**
     * Waking hours and the nightly planning time — spec §4.9's **Your day**.
     *
     * One value rather than three keys, because *Plan tomorrow at* is a
     * function of *Awake between* until somebody overrides it; see [DaySchedule].
     * The two setters below are the only two edits the screen can make, which is
     * what keeps "unset" a reachable state rather than a value that has to be
     * maintained.
     *
     * Neither consumer is built: `#8`'s scheduled half owns the reminder clamp
     * and `C9b`'s load bar. §4.9 builds the controls; wiring is theirs.
     */
    val daySchedule: StateFlow<DaySchedule>

    fun setWakingHours(wakingHours: WakingHours)

    /** `null` restores the derived time — one hour before waking hours end. */
    fun setPlanningOverrideMinutes(minutes: Int?)

    /**
     * When Health Connect was last read for [uid], in epoch millis, or 0 if never.
     *
     * Per-account, because the throttle it drives decides whether *this* user's
     * readings are already in Firestore — a shared timestamp would make a freshly
     * signed-in account wait out the other one's window. Device-local like
     * everything else here: it describes this install's sync clock, not the user.
     */
    fun healthLastSyncAt(uid: String): Long

    fun setHealthLastSyncAt(uid: String, epochMillis: Long)

    /**
     * When Google Calendar was last **pulled** for [uid], in epoch millis, or 0 if never
     * ([`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61), §2.7).
     *
     * Per-account for [healthLastSyncAt]'s reason, and named *pull* rather than *sync* for a
     * second one that is this feature's own: §2.7 throttles the two directions differently —
     * *"pull is foreground + the shipped 15-minute per-uid throttle; **push is not
     * throttled** (a write must not lag the user)"*. A stamp called `calendarLastSyncAt` would
     * read as though it gated both, and the first person to use it that way would make a user's
     * edit wait a quarter of an hour to reach their own calendar.
     */
    fun calendarLastPullAt(uid: String): Long

    fun setCalendarLastPullAt(uid: String, epochMillis: Long)

    /**
     * The id of the GoalPilot calendar this install created for [uid], or `null` if it has not
     * created one (§2.6).
     *
     * ### Why it is cached at all, when Google can be asked
     *
     * Because the alternative to remembering is a `calendarList` request before every sync, and
     * the cost of getting it wrong is not a wasted request but a **second calendar** in Ido's
     * list with the same name. `SyncCalendarUseCase` still falls back to asking Google when
     * this is absent, so a fresh install finds the calendar it made last time rather than
     * duplicating it — the cache is a shortcut, never the source of truth.
     *
     * ### Per-uid, and that is §2.7's account-switch clause
     *
     * *"An account switch reads as **not mirrored**, not as events to patch."* A shared key
     * would hand the new account the previous one's calendar id, and the first sync would write
     * one person's schedule into another person's calendar. Keyed by uid, a new account simply
     * has no id yet.
     *
     * ### Signing out does not clear it, and that is deliberate
     *
     * §2.7: *"**Sign-out does not delete** the calendar Ido owns."* Forgetting the id here
     * would not delete anything either, but the next sign-in would create a duplicate — so the
     * memory outlives the session on purpose.
     */
    fun goalPilotCalendarId(uid: String): String?

    fun setGoalPilotCalendarId(uid: String, calendarId: String)

    /**
     * When §2.5's **daily miss review** was last put in front of the user, or `null` if it
     * never has been (`#56`).
     *
     * ### This is the one thing about the review that cannot be derived
     *
     * §2.5: *"Misses meet Ido **once**, in a daily review on app open."* Everything else about
     * a miss is a function of its occurrence and the clock (§2.3) — but *whether he has
     * already seen it* is a fact about what happened on a screen, and no amount of arithmetic
     * over the tasks can recover it. So it is stored, and it is the only stored thing this
     * feature has.
     *
     * ### A moment, not a date, and the two jobs it does
     *
     * `DailyMissReview` reads it twice: as a **calendar day** to decide whether today's review
     * is due at all, and as a **boundary** to leave out misses that closed before the last
     * one. A stored date could do the first and not the second — two reviews on the same day
     * would each show everything again.
     *
     * ### Device-local, and per-install rather than per-account
     *
     * Unlike [healthLastSyncAt], which is keyed by uid because it gates a *network sync* whose
     * cost belongs to one account, this gates a *screen*. The misses it shows are recomputed
     * from whoever is signed in, so the worst a shared stamp can do is defer one review by a
     * day on the same day a second account signs in on the same phone — against which a
     * per-uid key would be a second thing to migrate for a case this app has never had.
     */
    fun missReviewLastShownAt(): Long

    fun setMissReviewLastShownAt(epochMillis: Long)

    /**
     * Which version of the in-app guided tour this install has already been
     * shown, or `0` for *none*.
     *
     * ### An Int, not a Boolean, and that is the whole design
     *
     * `hasSeenTutorial` can answer *this install has run the tour* and can never
     * answer *this install has run **this** tour*. The day a step is added for a
     * feature that did not exist, every existing user is precisely the group that
     * has not seen it — and the flag says they have. The version costs the same
     * four bytes and turns that into a one-line change at
     * [com.idomarhaim.goalpilot.ui.tutorial.TUTORIAL_VERSION].
     *
     * ### A [StateFlow], for the same reason [skin] is
     *
     * It is read on the first composition after sign-in to decide whether the
     * tour starts at all. A cold flow would emit the default first, so every
     * returning user would be shown the opening step of a tour they finished
     * months ago and have it snatched away a frame later.
     *
     * ### Device-local, and per-install rather than per-account
     *
     * This is a fact about a **screen**, exactly like [missReviewLastShownAt],
     * not about a person: a phone that has been walked through the app has been
     * walked through it whoever was signed in at the time. The worst a shared
     * value can do is deny a second account on the same phone a tour of an app
     * it is already watching someone else use — against which a per-uid key would
     * be a second thing to migrate for a case this app has never had.
     */
    val tutorialSeenVersion: StateFlow<Int>

    fun setTutorialSeenVersion(version: Int)

    /**
     * Whether the measure proposal has been dismissed for [goalId] — spec §1.3,
     * `C22` [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
     * [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65).
     *
     * ### Permanent, not snoozed, and that is the whole point
     *
     * §1.3: *"the offer is dismissible per goal — dismissal is **permanent**, not
     * snoozed, because a default that re-asks is not a default."* So there is no
     * un-dismiss and no timestamp to expire: [dismissMeasureProposal] is one-way.
     * The manual path always exists — the goal editor sets a measure by hand — so
     * nothing is unreachable; it is simply never volunteered again.
     *
     * ### Device-local, and that is a real limit rather than an oversight
     *
     * This sits in [android.content.SharedPreferences] beside
     * [tutorialSeenVersion] and not on the goal document, so a **second device
     * re-offers** on a goal already dismissed on this one. Two reasons, and the
     * second is the one that decides it:
     *
     *  1. §1.3's requirement is *permanent*, and #65's own exit criterion is
     *     *"a dismissed goal never offers again, **across process death**"* —
     *     which is what this satisfies exactly.
     *  2. the `data/firestore` package was held by a live sibling session
     *     (`63-occurrences-and-recurrence`) when this shipped, so the goal
     *     document was not this session's to widen.
     *
     * `Untested:` whether the re-offer on a second device is felt as a defect —
     * this app has one user and, as of 2026-08-23, one signed-in device. Moving
     * it onto `GoalDto` is additive and costs one nullable field when someone
     * decides it is.
     */
    fun isMeasureProposalDismissed(goalId: String): Boolean

    /**
     * Records that the offer was declined for [goalId], for good.
     *
     * One-way by design — see [isMeasureProposalDismissed]. Accepting a proposal
     * does **not** call this: an accepted goal stops being eligible because it
     * has a measure, which is a fact about the goal rather than a suppression.
     */
    fun dismissMeasureProposal(goalId: String)
}
