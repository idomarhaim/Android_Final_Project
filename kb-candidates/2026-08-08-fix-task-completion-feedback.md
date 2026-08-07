# KB candidates — `fix-task-completion-feedback`, 2026-08-08

Written per `rules/memory-promotion.md`. **Normal mode**, so this is a proposal:
nothing here has been ingested. Each entry stands alone — a reader with no access
to this session's transcript has everything needed to write the page.

---

## 1. A Firestore transaction is server-only, so it has no offline behaviour at all

**Claim.** `firestore.runTransaction` is the one Firestore write that cannot be
served from the offline cache. An ordinary `set()`/`update()`/`WriteBatch` is
applied locally first, so snapshot listeners fire immediately and the write is
queued for sync — offline or online. A transaction goes straight to the server.
Consequences, both observed on a real device:
- **Online**, any UI that renders only what the listener reports sits still for the
  entire server round trip. Measured at **2.24 s** on an emulator with a good
  connection.
- **Offline**, there is no local write at all, so a UI built on the listener shows
  *nothing* — indistinguishable from the tap not registering.
- The failure is **not prompt**: Firestore spent a measured **7.9 s** resolving DNS
  and retrying before returning `UNAVAILABLE`. Any "just show the error" design
  inherits that eight-second delay.

**Why.** This is the kind of fact that is documented but not *felt* — "transactions
require a connection" reads as a footnote until you measure what it does to a UI.
It also explains a whole class of bug reports that look like "the app is slow" or
"the button does nothing". Rejected while investigating: swapping the transaction
for a `WriteBatch` + `FieldValue.increment`, which is the standard advice and is
wrong whenever the transaction is also enforcing a clamp or a derived field — see
entry 3.

**Destination.** Central KB — `kb/dev/firestore-offline-semantics.md` (new). This
generalises well past GoalPilot; any Firebase client hits it.

**Anchors.** `CHANGELOG/2026-08-08/fix-task-completion-feedback.md` (the
before/after tables); issue
[#3](https://github.com/idomarhaim/Android_Final_Project/issues/3);
`app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt`.

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 2. Retire an optimistic update against observed data, never against the write's completion

**Claim.** When a UI draws an optimistic change and then confirms it against a
server, clearing the optimistic overlay *when the write returns* causes a visible
flicker: the write's completion callback and the snapshot/subscription that
reflects that write arrive on **two independent channels** with no ordering
guarantee. Between them, the UI has dropped the overlay but not yet received the
new data, so it re-renders the **old** state for a few frames on every *successful*
action. The fix is to retire an overlay entry only once the observed data already
agrees with it, which cannot flicker by construction — at that point the overlay
is a no-op. Failures are the opposite: clear those immediately, since clearing
*is* the undo.

**Why.** This is a real, non-obvious ordering bug that unit tests do not catch
(both orderings pass) and that reads as "a rendering glitch" rather than a logic
error. It generalises to any optimistic-UI-over-a-subscription design — Firestore,
websockets, React Query, whatever. Rejected alternatives: a second subscription to
watch for confirmation (a duplicate listener, real cost); mutating the overlay
inside the stream's own transform (re-entrant state mutation, subtle); a fixed
delay (arbitrary and wrong under load).

**Destination.** Central KB — `kb/dev/optimistic-ui-patterns.md` (new).

**Anchors.**
`app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailViewModel.kt`
(the `uiState` transform comment and `_pendingToggles`); the tests
*"a successful write keeps the tick while the snapshot is still catching up"* and
*"once the snapshot catches up the overlay retires without double-counting"* in
`app/src/test/.../GoalDetailViewModelTest.kt`.

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 3. `FieldValue.increment` can express neither a clamp nor a derived field

**Claim.** The standard advice for making a Firestore counter offline-capable is to
replace a read-then-write transaction with `FieldValue.increment(delta)`. That is
correct only when the counter is *unconstrained*. `increment` cannot:
- **clamp** — there is no way to say "add 1 but never exceed `targetValue`" or
  "subtract 10 but never below 0"; and
- **derive** — a field computed *from the result* (a level from a points total, a
  tier from a score) is a function of the new value, not of the delta, so no
  increment expresses it.

So for a document where a transaction is enforcing bounds or maintaining a derived
projection, swapping in a batch + increment does not relocate the consistency
guarantee — it **deletes** it, and the damage usually surfaces somewhere
downstream (a denormalised leaderboard, a cached tier) rather than at the write.
The honest alternatives are: keep the transaction and fix the UX around it, or move
the derivation server-side into a triggered function that owns it exclusively.

**Why.** This is the trap in the most-recommended Firestore refactor, and the
reason it is dangerous is that it *appears* to work — the counter moves, the app
feels faster, and the clamp violation only shows up at a boundary nobody tests.
Worth a page because the reasoning is what makes it memorable, not the API detail.

**Destination.** Central KB — a section of
`kb/dev/firestore-offline-semantics.md` (entry 1), rather than its own page: it is
the same decision seen from the other side.

**Anchors.** Issue
[#34](https://github.com/idomarhaim/Android_Final_Project/issues/34) (the full
argument, including the proposed Cloud-Function inversion);
`TaskRepositoryImpl.setDone`; `domain/model/Leveling.kt`.

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 4. Surfacing "the repository's own error text" is right until the error is a transport failure

**Claim.** Passing a data layer's error message straight to the user is good when
the message is *domain* text the user can act on (*"no user with that code"*,
*"join the challenge before reporting a score"*) and bad when it is *transport*
text. This session shipped a snackbar reading
`UNAVAILABLE: Unable to resolve host firestore.googleapis.com` to a real screen,
from a call site that was following the codebase's own established and otherwise
correct convention. The rule that survives: surface repository text for refusals
the domain generated; substitute a written message for failures the network
generated, and keep the original in the log.

**Why.** The failure mode is interesting because the convention was *right* and
still produced this — it is a boundary case in an existing house rule, not a
mistake in following it. Cheap to state, and it is the sort of thing that is only
ever noticed by looking at a real screen (it passed review and unit tests).

**Destination.** Project-local — `knowledge/` in this repo, appended to whatever
page covers UI error conventions; it is about *this* codebase's `Resource`
convention. Promote centrally only if a second project shows the same shape.

**Anchors.** `CHANGELOG/2026-08-08/fix-task-completion-feedback.md` → *"A second
defect the measurement exposed"*; the test *"the raw gRPC failure text never
reaches the user"*.

**Supersedes.** Nothing, but it **refines** the existing convention visible in
`SocialViewModel.report` — check that page before writing, and update rather than
contradict it.

**Status.** Proposed, not ingested.

---

## 5. Verifying a UI latency fix needs frame timing, not a stopwatch

**Claim.** `adb shell screenrecord` is **variable-frame-rate**: it emits a frame
only when the screen actually changes. That makes
`ffprobe -show_entries frame=pts_time` a precise instrument for UI responsiveness —
a gap between frame timestamps is *literal stillness*, not a sampling artefact. It
answers the question a stopwatch cannot: **"was the screen dead, or was it
animating?"** — which is exactly the difference between "slow" and "broken" in a
user's perception. Practical notes: the first changed frame after a still period is
the tap; `screenrecord` may refuse the device's native resolution and silently
retry smaller (so letterboxing must be accounted for when cropping); and extracting
frames with `ffmpeg -vsync 0` preserves the 1:1 frame↔timestamp mapping needed to
label them.

**Why.** This technique was invented by `product-device-pass` to *prove* a defect
and reused here to *prove the fix*, on the same numbers — which is the only reason
the before/after table means anything. It deserves a page so the next session does
not reinvent it or fall back to eyeballing. Rejected: Perfetto/systrace (heavier,
and answers a different question — where the time went, not whether the user saw
anything).

**Destination.** Central KB — `kb/dev/android-ui-latency-measurement.md` (new).
**Check first**: `product-device-pass` may already have ingested this on 2026-08-07;
if so, this entry is an **update in place** adding the fix-side usage, the
letterboxing trap and the `-vsync 0` note — not a new page.

**Anchors.** `CHANGELOG/2026-08-06/product-device-pass.md` (the original
measurement); `CHANGELOG/2026-08-08/fix-task-completion-feedback.md` (the
before/after).

**Supersedes.** Possibly an existing page from `product-device-pass` — resolve
before ingesting.

**Status.** Proposed, not ingested. **Always-ask** if it turns out to supersede a
standing claim (`rules/memory-promotion.md`).
