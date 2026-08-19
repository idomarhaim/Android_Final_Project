# 50-offline-stamps — the as-of stamp ships; the deletion does not, and the reason is checkable

> **Summary:** #50 items 1–4 land — a server-set `updatedAt` on the two cross-boundary DTOs, an
> unconditional *as-of* caption on `feature/social` and `feature/challenges`, and a *"Not loaded
> yet"* state discriminated on `isFromCache && isEmpty`. **Item 5, the deletion of
> `ConnectivityMonitor`, is held:** the ticket authorises it on the premise that `C20` removed
> `setDone`'s transaction, and at `HEAD` that transaction is still there.

**Issue:** [#50](https://github.com/idomarhaim/Android_Final_Project/issues/50) · **Session:**
`50-offline-stamps` · **Date:** 2026-08-19 · **Mode:** `AUTO MODE` (Ido's first message)
**Branch:** `main` · **Brief:** [`sessions/done/50-offline-stamps.md`](../../sessions/done/50-offline-stamps.md)
**Singletons held:** Gradle daemon. **No device touched** — see *Tests*.

> **Staleness is a property of the *data*, not of the *connection*.**

---

## 1 · `updatedAt` on the two cross-boundary DTOs — shipped, from a writer the ticket did not expect

`PublicProfileDto` and `ChallengeParticipantDto` each gain one field:

```kotlin
@ServerTimestamp var updatedAt: Timestamp? = null
```

`Observed:` line numbers re-checked at `HEAD` as the brief required, and they had drifted again —
the ticket cites `Dtos.kt:83` / `:124`; they sat at **`:101`** and **`:143`** before this change.
The DTOs and the finding are unchanged.

**It is written by the same write that sets the number, and by nothing else.** Five sites, two
answers:

| Site | Stamped? | Why |
|---|---|---|
| `TaskRepositoryImpl.setDone` → `publicProfiles/{uid}` | **yes** | the write that moves `points` / `level` |
| `AuthRepositoryImpl.ensureProfile`, first-time create | **yes** | sets `points = 0`, `level = 1` |
| `AuthRepositoryImpl.ensureProfile`, returning-user merge | **no** | refreshes display name and photo only |
| `ChallengeRepositoryImpl.reportScore` | **yes** | the write that moves `score` |
| `ChallengeRepositoryImpl.meAsParticipant` (create / join) | **yes** | `@ServerTimestamp` fills it on the POJO write |

The returning-user merge is the one worth stating out loud: stamping it would announce a score
change to every reader on the strength of somebody renaming their Google account — the same class of
lie the ticket exists to remove, arriving from the other side.

**Server-set, and that is load-bearing rather than tidy.** The reader of these rows is on a
*different device* from the writer, so a writer with a wrong clock would caption a stale number with
a time the reader has no way to audit. `FieldValue.serverTimestamp()` on the map writes,
`@ServerTimestamp` on the POJO write.

### The writer is a client transaction, not `C20`'s projection function — because that function does not exist

The ticket says the field is *"written by `C20`'s projection function"* and lists `functions/` under
*Where*. `Observed:` at `HEAD`, **there is no such function.**

- `functions/src/index.ts` is 165 lines and holds **only** the AI callables (`scoreTask` and the
  summary generator). No projection, no trigger registrations.
- `firestore.rules:22–24` still carries its original NOTE: *"points/level are written by the client
  for this course project. In production, compute them in a Cloud Function and lock writes down."*
  `:53` says the participant score *"is client-written, exactly as `publicProfiles.points` is
  above."* There is no field-level condition anywhere, which §5.2 says `C20` would add.
- `publicProfiles.level`, which §5.2 decided is *"deleted outright"*, is still on the DTO.

`C20` is [#42](https://github.com/idomarhaim/Android_Final_Project/issues/42), a **decision** issue,
closed 2026-08-14 as *decided*. Its build half — *"one projection function, two trigger
registrations, and zero client writers of derived state"* — has never shipped, and **no open issue
carries it**. #49 landed one adjacent piece (`goal.currentValue` derived rather than stored), which
is what makes the decision easy to read as built.

**This changed the field's location and nothing about its contract.** The ticket's own constraints —
*one field, the same write that sets the number, no new trigger, no new read path* — are all met by
riding the client writer that actually exists. When `C20`'s function is built, the stamp moves with
the write it rides on, and neither the DTO nor any reader changes.

## 2 · The as-of caption — unconditional, and one caption per list

`ui/components/FreshnessNote.kt` *(new)*: one quiet line of `bodyMedium` / `onSurfaceVariant`, drawn
**identically online and offline**, taking its words from the caller.

- `feature/social` — *"Leaderboard as of 09:14"*, under the `Leaderboard` section header.
- `feature/challenges` — *"Standings as of 09:14"*, in `StandingsSheet` under the participant line.

No connectivity banner, no per-number "cached" styling, and nothing on the path asks the OS about the
radio. Styled as ordinary secondary text on purpose: an error colour would make a statement of fact
read as a fault.

**`DateTimeUtils.formatAsOf` splits on the local calendar day** — `"09:14"` today,
`"Aug 17, 2026 09:14"` before that. Not a nicety: a bare clock reading on a stamp three days old
reads as *this morning*, which would make the caption assert the opposite of what it is for.

**Two decisions the ticket left open, taken here and recorded so they can be overturned:**

1. **One caption per list, stamped from the *newest* row.** *"As of T"* claims *nothing here reflects
   a write after T*, which is a `max`; a `min` would report the list as older than it is. Each row's
   own stamp says when its owner last wrote **the copy we hold** — it never promises they have not
   written since, which is exactly why this is a caption and not a freshness guarantee.
2. **Suppressed when there is no stamp to state** — every row predates the field, or its
   `serverTimestamp()` is still pending. The ticket's *"unconditional"* is about **connectivity**
   (*"always, online and offline alike"*), not about having something true to say; a caption that
   cannot name a time would have to invent one, which is the failure it exists to prevent.

## 3 · *empty* and *never loaded* now render differently

`snapshot.metadata.isFromCache && snapshot.isEmpty` had **0 usages** in the app at `HEAD`, exactly as
the ticket said. It now has one, in `QuerySnapshot.crossBoundaryFreshness()`
(`data/firestore/FirestoreExt.kt`), feeding a small domain type:

```kotlin
data class Freshness(val asOfEpochMillis: Long = 0L, val neverLoaded: Boolean = false)
```

Copy is the smallest true sentence — **"Not loaded yet"**, never *"No friends"*. Both halves of the
conjunction are load-bearing, and each alone is wrong in a way that reads as working:
served-from-cache-and-non-empty is ordinary offline reading (no banner, per §2), and
empty-from-the-server is a genuine *"nobody is here"* the app may state.

### One defect found while building it, and fixed here

`snapshotsFlow()` registered every listener with `MetadataChanges.EXCLUDE`, which raises no event when
**only** metadata changes. An empty result set going from cache-served to server-confirmed changes
**no documents** — so it is precisely such an event. Left alone, a cross-boundary collection that is
genuinely empty on the server would render *"Not loaded yet"* once and **stay there until somebody
else wrote a document**: the never-loaded state would be a trap rather than a transient, which is the
opposite of what it is for.

`snapshotsFlow()` now takes a `MetadataChanges` parameter defaulting to `EXCLUDE` (right for
owner-side reads), and the three cross-boundary listeners pass `INCLUDE`. Nothing downstream pays for
the extra emissions — they carry an equal value, and `StateFlow` drops those.

`Untested:` this is `Inferred:` from Firestore's documented listener semantics and from the shape of
the empty-snapshot case — not observed. Observing it needs a first-run install against a genuinely
empty collection with the radio off. What would check it: a cloud-emulator run, or a device pass.

## 4 · Exactly two screens

`feature/social` and `feature/challenges`, per the ticket's read-rule table, and **not widened**.
Nothing was added to `users/{uid}/**` (the reader is the writer), to `shares/{shareId}` (an immutable
event — `SocialUiState` carries freshness for the leaderboard and deliberately not for the feed), or
to the `challenges/{id}` document itself (owner-authored title and dates, not a derived number).

## 5 · `ConnectivityMonitor` — **NOT deleted**, and the reason is one grep

**The deletion the brief authorised is held, not skipped, and nothing else was extended.**

The ticket's §5 authorises it on a stated premise:

> Its whole premise ... was that `setDone` is a **server-only** Firestore transaction, so offline the
> optimistic tick had to be taken back after a measured **7.9 s**. `C20` removes the transaction.

`Observed:` at `HEAD`, `TaskRepositoryImpl.kt:98` is `firestore.runTransaction { txn -> … }`. **The
transaction is still there.** What #49 removed was the *goal* write from inside it, not the
transaction itself — it still reads the task document and the user document and writes three
documents atomically. A Firestore transaction is server-only by construction and cannot be satisfied
from the persistent cache, so offline `setDone` still fails and the optimistic tick is still taken
back after eight seconds.

**Deleting the pre-check would therefore re-open
[#3](https://github.com/idomarhaim/Android_Final_Project/issues/3)**, which is closed. The
authorisation was conditional on a premise that is false at `HEAD`, so it does not reach this
deletion — and not-deleting is the reversible direction. It is one line to remove on the day `C20`'s
projection function lands and `setDone` stops being a transaction.

`core/net/ConnectivityMonitor.kt`, `GoalDetailViewModel`'s pre-check and its `OFFLINE_MESSAGE` are
all **unchanged**. `OFFLINE_MESSAGE` remains a hardcoded English literal; #51 owns that sweep, and
nothing here was added to `SWEPT_PACKAGES`.

**What #50 still owes, and who owes it:** items 1–4 are complete. Item 5 needs `C20`'s server half
first — a projection function plus its two trigger registrations, `firestore.rules` locked down to
match, and `setDone` reduced to a single-document write. That is a build issue nobody has filed.

## Files

| File | What |
|---|---|
| `data/firestore/dto/Dtos.kt` | `updatedAt` on `PublicProfileDto` and `ChallengeParticipantDto` |
| `domain/model/Freshness.kt` *(new)* | the two facts a cross-boundary read knows about itself, plus `merge()` |
| `domain/model/Social.kt` | `Leaderboard` — entries + freshness as one read |
| `domain/model/Challenge.kt` | `ChallengeWithStandings.standingsFreshness` |
| `domain/repository/SocialRepository.kt` | `observeLeaderboard` returns `Leaderboard` |
| `data/firestore/FirestoreExt.kt` | `crossBoundaryFreshness()`, `UPDATED_AT`, the `MetadataChanges` parameter |
| `data/firestore/SocialRepositoryImpl.kt` | reads both `publicProfiles` listeners' freshness; chunk merge |
| `data/firestore/ChallengeRepositoryImpl.kt` | stamps `reportScore`; reads the participants freshness |
| `data/firestore/TaskRepositoryImpl.kt` | `updatedAt` on the projection write inside `setDone` |
| `data/auth/AuthRepositoryImpl.kt` | `updatedAt` on profile creation, not on the merge |
| `core/util/DateTimeUtils.kt` | `formatAsOf` + a 24-hour formatter |
| `ui/components/FreshnessNote.kt` *(new)* | the shared line — literal-free, so the swept package stays swept |
| `feature/social/SocialViewModel.kt`, `SocialScreen.kt` | freshness through the state; caption + never-loaded |
| `feature/challenges/ChallengesViewModel.kt`, `ChallengeDialogs.kt` | the same, on the standings sheet |

## 🧪 Tests

**JVM unit — `./gradlew testDebugUnitTest`: 384 tests, 0 failures, 0 errors, 0 skipped, 41 suites.**
Re-run with `--rerun-tasks` after the final change, so no result is an up-to-date cache hit.

| Suite | New | Covers |
|---|---|---|
| `data/firestore/CrossBoundaryFreshnessTest` *(new)* | 9 | both halves of `isFromCache && isEmpty`; the stamp is a `max`; a missing stamp is absent, not `0L`; the chunk merge |
| `core/util/FormatAsOfTest` *(new)* | 4 | the same-day split, including the two boundaries that make it a *calendar day* rather than an elapsed-time threshold |
| `feature/social/SocialViewModelTest` | +4 | never-loaded reaches the screen as never-loaded; empty-from-server does not; the stamp survives; no stamp means no caption |
| `feature/challenges/ChallengesViewModelTest` | +3 | the same three, on the standings sheet's card |

**The *Not loaded yet* state is exercised at two layers**, deliberately: the ViewModel tests prove the
flag reaches the screen, and only `CrossBoundaryFreshnessTest` can prove what *sets* it, since
`metadata.isFromCache` exists nowhere else.

**`assembleDebug` — green.** **`compileDebugAndroidTestKotlin` — green**, which is the whole of the
instrumented layer reachable without a device.

**Instrumented (`connectedDebugAndroidTest`) — not run *locally*, deliberately; the cloud runs it on
this push.** Locally it uninstalls the app and takes the Google account with it, and
`new-machine-checkup` signed this device in yesterday; per the device-state rule a session does not
get to want both, and this one wanted neither and touched no device.

It is covered anyway, and the timing is luck rather than planning: `cloud-emulator` enabled the
`push:` trigger on `.github/workflows/instrumented-tests.yml` at `4866324`, **thirty-five minutes
before this commit**, after run #1 came back green in 12m 02s. Its path filter is
`['app/**', 'gradle/**', …]` and every source file here is under `app/**`, so pushing this dispatches
the 15 instrumented tests on a GitHub-hosted API-34 emulator with no local device involved. That run
is also the first exercise of the trigger by a commit rather than by hand.

**It reported, and it is green.** `Observed:` run **#2**, event `push`, head `d577dcf` —
**success**, every step of *androidTest on API 34* green, including *"Count what actually ran, and
fail if nothing did"*, the guard `cloud-emulator` added hours earlier because
`connectedDebugAndroidTest` goes green on **zero** discovered tests. So the suite genuinely ran and
genuinely passed. `Untested:` the exact count was not read — it is written into the run summary
page, and the jobs API returns step conclusions only; what is verified here is *non-zero and
passing*, not *15 of 15*. The *Photograph the running app* job was **skipped** on a `push` event,
so there are no screenshots from this run.

That covers regression. It does not cover appearance: the changed Compose surfaces —
`FreshnessNote`, and the caption and never-loaded branches on both screens — are still
**`Untested:` as pixels**. `ChallengesUiTest` builds
`ChallengeWithStandings` with named arguments and the new field is defaulted, so it still compiles
and still covers what it covered; it drives `MyChallengeCard` and never `StandingsSheet`, so the new
sheet content sits outside it either way. Nothing in the suite renders `StandingsSheet` or the
leaderboard section, so the green run proves **no regression**, not that the new pixels are right;
seeing those needs a render pass, or a `workflow_dispatch` of the same workflow with
`capture-screenshots` on — which is Ido's to trigger.

**`functions/` has no test layer at all** (#50 §7.2). Stated rather than skipped silently — and this
session added nothing to `functions/`, for the reason in §1.

**`firestore-tests/rules.test.mjs` — not run.** No rule changed: both collections already allow the
owner to write arbitrary fields, so `updatedAt` needs no rule edit. `Inferred:` from reading
`firestore.rules` at `HEAD`, where neither `publicProfiles/{uid}` nor
`challenges/{id}/participants/{uid}` carries any field-level condition.
