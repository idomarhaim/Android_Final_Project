# KB candidates — `8-notifications` — 2026-08-20

Session: `8-notifications` · Repo: `C:\Dev\Android_Final_Project` · Issue: `#8`
Changelog: [`CHANGELOG/2026-08-20/8-notifications.md`](../CHANGELOG/2026-08-20/8-notifications.md)

---

## 1. A notification's small icon is alpha-only, and every preview lies about it

**Claim.** Android masks a notification's small icon to a flat colour and reads **only its alpha
channel**. Passing a full-colour launcher icon (`ic_launcher_foreground`, 108 dp) produces a
solid white blob with the shape discarded — and it looks completely correct in Android Studio's
preview, in a vector viewer, and in any test that inspects the drawable, because **nothing masks
it there**. The only instrument that can see the defect is a screenshot of the actual shade or
status bar. Remedy: a dedicated 24 dp, single-colour drawable whose shape is carried entirely by
alpha.

**Why.** This belongs with the existing pages because it is a clean instance of a pattern already
recorded — *the artefact exhibits the behaviour only in the consumer you are not running*. What
makes it worth its own entry rather than a line is the **direction of the failure**: it is not
that verification is hard, it is that four independent instruments all report success. Rejected
framing: *"use a monochrome icon"* as a style tip — that states the fix and hides the reason the
fix is needed, which is the part that generalises to every asset the system re-renders (widget
previews, adaptive icons, tile icons).

`Observed:` 2026-08-20, GoalPilot `#8` — the correct dedicated drawable was written *because*
this was anticipated, and then confirmed by looking at the shade; the blob was never shipped.
So this is a **near-miss recorded deliberately**, not an incident report.

**Destination.** `kb/dev/android-device-verification.md` — new section, beside §8's
`connectedDebugAndroidTest`-uninstalls finding. Cross-ref from
`kb/dev/look-at-your-own-output.md` (it is that rule's *render it and look* clause with a
concrete asset class).

**Anchors.** `app/src/main/res/drawable/ic_notification.xml` ·
`CHANGELOG/2026-08-20/8-notifications.md` § *The observed fire*.

**Supersedes.** Nothing.

**Status.** Ready.

---

## 2. An isolated test run is a weaker instrument than the suite — the platform contributes state

**Claim.** `NotificationObservedFireTest` located its notification with
`single { channel == gp_filing }`. Green when the class was run alone; red in the full suite with
*"Collection contains more than one matching element."* The second element is **Android's own**:
when two of an app's notifications are up at once the system synthesises an `AUTOGROUP_SUMMARY`
record, and **it carries the channel id of the first one**. So a channel filter matches two, and
only in the run orders that leave both posted. Fix: match on the app's own notification **id**,
and assert the channel separately rather than using it as the key.

**Why.** The generalisable half is not *"notifications auto-group"* — it is that **`-e class Foo`
is not a cheap version of the suite, it is a different and weaker experiment**, because the state
the platform contributes is a function of what else ran. A notification test that never sees a
second notification never sees the grouping that exists only because there is a second one. Same
family as the *check the instrument on the hardest input it exists for* rule, with a twist worth
recording: here the hardest input was not a value the test author chooses, it was **the presence
of the other tests**. Rejected framing: filing it as a flaky test — it is perfectly
deterministic given run order, which is what made it look fine.

`Observed:` 2026-08-20, GoalPilot, same code passing and failing in the two invocations.

**Destination.** `kb/dev/look-at-your-own-output.md` — new sub-section under the
*check the instrument* material. Cross-ref from `kb/dev/android-device-verification.md`.

**Anchors.** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/NotificationObservedFireTest.kt` ·
`GoalPilotNotifier.ID_FILING` (made `internal` for this).

**Supersedes.** Nothing.

**Status.** Ready.

---

## 3. `--` is illegal inside an XML comment, and the error names the file but not the habit

**Claim.** An XML comment may not contain `--`. In an Android resource or manifest this fails the
build at `parseDebugLocalResources` / `mergeDebugResources` / `processDebugMainManifest` with
`[Fatal Error] <file>:<line>: The string "--" is not permitted within comments` — three failed
tasks from one prose typo, none of which names the manifest as a second casualty until you read
past the first. The trigger is specific to this methodology's house style: long explanatory
comments in every file, where a double hyphen is the natural ASCII stand-in for the em dash used
throughout. Remedy: `—` inside comment bodies, and re-check mechanically (regex over comment
bodies) rather than by eye.

**Why.** Cheap, recurring, and invisible to review — the comment reads perfectly. It also
generalises past XML: the same house style puts long comments into `.properties`, `.toml` and
YAML, each with its own escaping trap (this repo already records `local.properties` treating `\\`
as an escape). Rejected as too small to record — it cost a build cycle here, and the class of
*"prose punctuation is syntax in this file format"* has now bitten this repo twice.

**Destination.** `kb/dev/powershell-encoding-traps.md` is the nearest existing home but the wrong
title; propose a small new page `kb/dev/prose-punctuation-is-syntax.md` collecting the class
(XML `--`, `.properties` backslash escapes, `%` in Android string resources), with the
`local.properties` finding moved beside it by reference rather than by cutting it out.

**Anchors.** `app/src/main/res/drawable/ic_notification.xml` · `app/src/main/AndroidManifest.xml`
· `CLAUDE.md` (the existing `local.properties` escape entry).

**Supersedes.** Nothing. **Does not** rewrite the `local.properties` entry — it links to it.

**Status.** Ready.

---

## 4. Every `C`-series reference in `PRODUCT_v0.3.md` is a candidate false deferral

**Claim.** `#8` needed §2.1's occurrence model and found it absent: `Task` carries no due date,
§2.2's four rungs (`ALL_DAY`/`DEADLINE`/`BLOCK`/`SPAN`) appear in **no Kotlin file**, and the
spec attributes the work to **`C9a` #25 — which is CLOSED**. The open issues on 2026-08-20 are
`#54 #53 #51 #48 #40 #8 #7`, and none of them carries the build.

This is the **second instance in one day**: `7-quickadd-complete` found the same shape on §1.4,
deferred to `C1` **#19**, also closed. The two were found by different routes — one by auditing a
section, one by trying to *use* the model — which is what makes the generalisation load-bearing:
**the whole `C`-series was closed when the map was charted, so every `(C<n> #<m>)` heading in the
spec is a decision that was *taken*, and the document gives a reader no way to tell that from one
that was *implemented*.** The spec is written in the present tense throughout, so it reads as a
description of the app.

**Why.** The remedy is not "reopen tickets" — the decisions really were made and closing them was
right. It is that a decision-map document needs a **status axis its prose does not carry**, which
is exactly the `DECIDED — NOT BUILT` box `7-quickadd-complete` added to §1.4. The finding here is
that **one box is not enough**: the same audit is owed to every `C`-series section, and the cheap
mechanical first pass is to grep the spec for `(C` headings and check each against HEAD.

Rejected: filing an issue per section — that manufactures a backlog out of an audit nobody has
run yet, and several sections may well be built.

`Observed:` 2026-08-20 by grep across `app/src/main` (zero hits for the rung names) and by
reading the GitHub issues API for the state of `#25` and the open list.

**Destination.** `kb/dev/decision-map-charting.md` — extend the section
`7-quickadd-complete` opened (§12d) with the second instance and the generalisation, rather than
a new page: one page per phenomenon, and this is the same phenomenon at a second site.

**Anchors.** `docs/PRODUCT_v0.3.md` §2.1–§2.5 · `domain/model/Task.kt` ·
`notifications/ReminderScheduler.kt` KDoc (states the absence at the point of use) ·
`CHANGELOG/2026-08-20/7-quickadd-complete-r2.md`.

**Supersedes.** Does not supersede §12d — **extends** it. If the ingest finds §12d already states
the generalisation, this becomes a second `Observed:` line under it rather than new prose.

**Status.** Ready.

---

## 5. The board is the one file the pathspec-commit remedy cannot protect — now observed

**Claim.** `my-rules.instructions.md` predicts that `git commit -F msg -- <path>` protects a
sibling's *other* files and never one that is in your pathspec **and** theirs, naming
`SESSIONS.md` as that file "by construction". This session is the **observed instance**: my claim
row was written into the working tree, and `0f7dadd` — a sibling's correctly-scoped pathspec
commit of `SESSIONS.md` — carried it up under the message *"51-freeze-verify: claim before the
first write"*, whose body says *"Board row claimed (0 rows were active)"* while the commit adds
**two** rows. True when they read, false when they committed.

**Why.** The rule already predicts this, so the entry's value is **evidence that the prediction
fires in ordinary use, not only in principle** — both sessions followed the rule exactly and it
happened anyway, inside a window of seconds, with no carelessness on either side. Worth recording
because the *repair* is the part people skip: you cannot subtract it, an `--amend` of a shared
file re-runs the hazard over a turn-spanning window, so the only move is **naming what rode
along** in the next commit and the changelog. Also worth recording: the ride-along made the
sibling's commit **message** false, which is a provenance defect that no diff review would flag.

Rejected: proposing a lease on `SESSIONS.md` as the fix — a lease was *held* here (acquired
12:05:31Z) and did not prevent it, because a lease governs writers who consult it and the
sibling's commit read the working tree.

**Destination.** `kb/dev/flows/lease.md` §4b already records this class at the *staging* layer;
add the **commit-of-a-shared-path** instance beside it. Cross-ref
`rules/agent-topology-and-model-routing.md` §5.

**Anchors.** `0f7dadd` (GoalPilot) · `CHANGELOG/2026-08-20/8-notifications.md` § *Concurrency*.

**Supersedes.** Nothing.

**Status.** Ready.
