# KB candidates — `67-delete-anything`, 2026-08-23

Each entry stands alone. No entry below depends on this session's transcript, per
`rules/memory-promotion.md`.

---

## 1. A bidi isolate splits every substring matcher that spans a number

**Claim.** In an app that direction-isolates its counts (`BidiFormatter.unicodeWrap`, `<bdi>`, or a
hand-rolled `FSI…PDI` pair), a Compose `onNodeWithText("4 entries", substring = true)` **cannot
match** — `U+2069 POP DIRECTIONAL ISOLATE` sits between the digit and the space, so the substring
does not exist in the node's text. The marks are zero-width, the text renders perfectly, and
Compose reports `Assert failed: The component is not displayed!`, which points at **layout**.
Nothing about the failure suggests the string.

`Observed:` 2026-08-23, GoalPilot `#67`, on `emulator-5554`. **8 of 15 instrumented tests failed on
the first run and every one of them was matching a string containing a number**; the 7 that passed
matched either a test tag or a literal with no argument (`"This task."`). Diagnosed by dumping the
semantics tree on the device — walking every node's `SemanticsProperties.Text` and logging
`t.text.map { it.code }` — which showed
`⁨4⁩ entries in its progress log.` as codes `8296, 52, 8297, 32, 101, …`. Reading the composable
would not have settled it; the isolation happens two files away, in the string helper.

**Why it matters more than an ordinary matcher bug.** It fails toward the **wrong repair**. The
obvious fix is to match `"entries in its progress log"` and drop the number — which goes green while
leaving the one thing the feature added, the count, unasserted. So the guard reports success on
exactly the assertion it exists to make. The tell that you are about to make that mistake: the
failing assertions all contain digits and the passing ones do not.

**The fix is to the instrument, not the assertion.** A `SemanticsMatcher` that strips isolates
before comparing:

```kotlin
private fun hasStrippedText(substring: String) =
    SemanticsMatcher("text (isolates stripped) contains '$substring'") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .any { Bidi.strip(it.text).contains(substring) }
    }
```

GoalPilot already had `Bidi.strip`, whose own KDoc says *"for tests and for logging, never for
display"* — the helper existed and no test had ever used it, which is the second half of the
finding: an isolation utility ships with a stripping counterpart and nothing points the test author
at it.

**Scope.** Not `#67`-specific. Every swept-package string in that app interpolates a
`bidiIsolated()` count, so **any** future substring assertion spanning a number fails this way.

**Rejected:** *"match on the number alone"* (`"4"`) — matches the wrong node on any screen with two
numbers. *"assert on `contentDescription` instead"* — most of these strings have none, and adding
one to make a test pass is a semantics change for a test's convenience.

**Why:** the diagnosis cost a device round trip plus a semantics dump, and the failure message
points at the wrong layer. Anyone writing an instrumented assertion in this app will hit it.
**Destination:** `C:\Dev\JARVIS\kb\dev\` — a new page, or a section on an existing Compose-testing
page if one exists; it is a **testing** finding, not a device-verification one, so
`android-device-verification.md` is the wrong home unless nothing better exists.
**Anchors:** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/DeleteAnythingUiTest.kt`
(`hasStrippedText`) · `app/src/main/java/com/idomarhaim/goalpilot/core/util/Bidi.kt`
**Supersedes:** nothing.
**Status:** ready.

---

## 2. `${PIPESTATUS[0]}` caught a failed build that then reported a green test run — the recorded trap firing exactly as written

**Claim.** GoalPilot's `CLAUDE.md` already records that a Gradle build piped to another command must
be gated on `${PIPESTATUS[0]}`, *"because the previous APK is still sitting at the output path, so
`adb install -r` succeeds and the test run reports the last build's results."* This session hit it
and the gate held.

`Observed:` 2026-08-23. `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest | tail -3`
returned `BUILD FAILED in 3s` with `GRADLE_EXIT=1` (a transient Windows KSP lock — it succeeded
unchanged on the next invocation). Both `adb install -r` calls then printed `Success` and
`am instrument` printed `OK (15 tests)` — for the build **before** the fix under test. Without
reading the exit code, that run would have been reported as verifying a change it never contained.

**Why this is worth a line rather than a page.** It adds no new mechanism; the value is a
**dated confirmation** that the recorded trap is live on this machine and that the prescribed guard
is sufficient. A prediction that has fired once is worth more than the same sentence unwitnessed —
and the note's own wording (*"a Kotlin compile error scrolled past inside a `grep`"*) had only the
`grep` case, where this instance was a plain `tail`.

**Why:** cheap corroboration of an existing claim, with a second shape of the same trap.
**Destination:** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` — as an `Observed:` line under the
existing section, **not** a new section.
**Anchors:** `c:\Dev\Android_Final_Project\CLAUDE.md` (the `${PIPESTATUS[0]}` bullet).
**Supersedes:** nothing; it corroborates.
**Status:** ready.

---

## 3. Deleting a document does not delete its subcollections, and a per-parent fan-out hides the orphans perfectly

**Claim.** Where a repository reads a subcollection by **fanning out over the parents that exist**
rather than by a collection-group query, deleting a parent makes its subcollection unreachable by
every reader **and** invisible to every check. No number goes wrong, no screen misbehaves, and the
documents accumulate for the life of the account.

`Observed:` 2026-08-23, GoalPilot `#67`. `GoalRepositoryImpl.deleteGoal` removed
`users/{uid}/goals/{id}` and nothing else, while `entriesFlow` builds one snapshot listener **per
live goal id** — so `goals/{deletedId}/progressEntries` had no reader and no route to one. The same
shape appeared one collection over with a different mechanism: `deleteTask` left
`users/{uid}/occurrences` rows whose `taskId` was gone, and all four consumers of that collection
join it back to the task list, so every orphan is silently dropped from every count.

**The general form.** *An orphan is invisible exactly when the read path is a join or a fan-out
keyed on the parent.* That is also the property that makes it feel safe to skip the cleanup — you
cannot find a symptom, because the reader that would show one is the reader that filters it out.

**How to find them without a symptom.** For each `delete` in a repository, list every collection
whose documents carry the deleted entity's id (as a subcollection path segment **or** as a field),
then ask of each: *would any reader ever return this row again?* If the answer is no, the delete is
incomplete. Reading the delete method alone never surfaces it, because the omission is not in the
method.

**Rejected:** *"leave them, they cost nothing"* — they cost storage on the user's own project
forever, and the codebase already had the counter-argument written down one method over: *"an orphan
fact would add points the user cannot see, find or remove."*

**Why:** it is a Firestore-shaped trap with a general detection procedure, and it was found twice in
one ticket by two different mechanisms.
**Destination:** `C:\Dev\JARVIS\kb\dev\firestore-write-semantics.md` — a new section.
**Anchors:** `app/src/main/java/com/idomarhaim/goalpilot/data/firestore/GoalRepositoryImpl.kt`
(`deleteGoal`) · `.../TaskRepositoryImpl.kt` (`deleteTask`).
**Supersedes:** nothing.
**Status:** ready.

---

## 4. A flat list of consequences invites an addition the design does not intend

**Claim.** Where a confirm names several quantities and one of them is a **subset** of another,
drawing them as equal peers invites the reader to total them. No assertion can see it: every count
is individually correct and every matcher passes.

`Observed:` 2026-08-23, GoalPilot `#67`, with all 15 instrumented tests green —
`This task. / 12 scheduled occurrences. / Including 5 that already happened. / The 40 points it
earned.` The third line is 5 **of** the 12 and reads as a fourth item. Found by looking at the PNG.

**The fix that was taken and the one that was not.** Subordination — smaller type, secondary colour,
indent in the layout direction — rather than folding the two counts into one sentence, because a
single *"12 occurrences, 5 of which already happened"* needs two interacting plural forms per
language, and Hebrew has four plural categories to English's two.

**Why:** thin on its own — it is one instance of *look at the render*, which the KB already argues
at length. Flagged rather than promoted; it may be worth one line as an example under
`look-at-your-own-output.md`'s visual-acceptance section, and it may be worth nothing.
**Destination:** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md`, or dropped.
**Anchors:** `app/src/main/java/com/idomarhaim/goalpilot/ui/components/DeleteConfirm.kt`
(`Consequence`) · `docs/render-passes/2026-08-23-67-delete-anything/issue-67-confirm-task-light.png`
**Supersedes:** nothing.
**Status:** ready, thin — ask before promoting.
