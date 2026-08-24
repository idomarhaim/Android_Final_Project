/**
 * Security-rules tests for ../firestore.rules and ../storage.rules.
 *
 * These run against the local Firestore and Storage emulators under the project
 * id `demo-goalpilot`. The `demo-` prefix is load-bearing: the emulator suite
 * treats such ids as offline-only and refuses to reach any real backend, so a
 * test can never touch the live project `goalpilot-56e30`.
 *
 * The suite exists because of the challenges feature, where the rules decide
 * the data model rather than merely guarding it: the challenge document is
 * owner-only, so joining CANNOT be an edit to it, and participation has to
 * live in a subcollection each user writes for themselves. The two tests named
 * "regression" and "the fix" below are that argument, executable.
 *
 * The `shares` and Storage sections were added for issue #5 (deleting your own
 * post). They carry the same argument for a different feature: a post's delete
 * has to be author-only in the rules, and the photo it carried has to be
 * deletable too, or the image outlives the share that referenced it.
 */
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import test, { before, after, beforeEach } from 'node:test'
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} from '@firebase/rules-unit-testing'
import { doc, getDoc, setDoc, updateDoc, deleteDoc } from 'firebase/firestore'
import {
  ref as storageRef,
  uploadBytes,
  deleteObject,
  getBytes,
} from 'firebase/storage'

const here = dirname(fileURLToPath(import.meta.url))
const rules = readFileSync(resolve(here, '..', 'firestore.rules'), 'utf8')
const storageRules = readFileSync(resolve(here, '..', 'storage.rules'), 'utf8')

const OWNER = 'uid_owner'
const JOINER = 'uid_joiner'
const CHALLENGE = 'challenge_run_streak'
const SHARE = 'share_weekly_owner'

// What SocialRepositoryImpl.shareSummary writes, minus the fields no rule reads.
const shareFixture = (authorUid) => ({
  authorUid,
  authorName: 'Ido',
  period: 'WEEKLY',
  headline: 'Weekly progress',
  message: 'Earned 120 pts',
  points: 120,
  completedTasks: 4,
  imageUrl: null,
  createdAt: 1,
})

// StorageRepositoryImpl.uploadImage writes to "<folder>/<uid>/<uuid>.jpg", so
// the uid is a path segment and that is what storage.rules matches on.
const imagePath = (uid) => `shares/${uid}/photo.jpg`
const aJpeg = () =>
  new Blob([new Uint8Array([0xff, 0xd8, 0xff, 0xd9])], { type: 'image/jpeg' })

let env

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-goalpilot',
    firestore: { rules },
    storage: { rules: storageRules },
  })
})

after(async () => {
  await env?.cleanup()
})

beforeEach(async () => {
  await env.clearFirestore()
  await env.clearStorage()
  // Seed one challenge owned by OWNER and one share authored by OWNER,
  // bypassing rules — a fixture is not the thing under test.
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'challenges', CHALLENGE), {
      title: '7-day run streak',
      description: 'Most km this week',
      ownerUid: OWNER,
      type: 'RUNNING',
      metricUnit: 'km',
    })
    await setDoc(doc(ctx.firestore(), 'shares', SHARE), shareFixture(OWNER))
    await uploadBytes(storageRef(ctx.storage(), imagePath(OWNER)), aJpeg(), {
      contentType: 'image/jpeg',
    })
  })
})

const asUser = (uid) => env.authenticatedContext(uid).firestore()
const asVisitor = () => env.unauthenticatedContext().firestore()
const storageAs = (uid) => env.authenticatedContext(uid).storage()
const storageAsVisitor = () => env.unauthenticatedContext().storage()

// ── Reading ──────────────────────────────────────────────────────────

test('any signed-in user can read a challenge', async () => {
  await assertSucceeds(getDoc(doc(asUser(JOINER), 'challenges', CHALLENGE)))
})

test('a signed-out visitor cannot read a challenge', async () => {
  await assertFails(getDoc(doc(asVisitor(), 'challenges', CHALLENGE)))
})

// ── Creating ─────────────────────────────────────────────────────────

test('a user can create a challenge naming themselves as owner', async () => {
  await assertSucceeds(
    setDoc(doc(asUser(JOINER), 'challenges', 'new_one'), {
      title: 'Sleep 8h',
      ownerUid: JOINER,
    }),
  )
})

test('a user cannot create a challenge owned by someone else', async () => {
  await assertFails(
    setDoc(doc(asUser(JOINER), 'challenges', 'new_one'), {
      title: 'Sleep 8h',
      ownerUid: OWNER,
    }),
  )
})

// ── Editing the challenge itself ─────────────────────────────────────

test('the owner can edit their own challenge', async () => {
  await assertSucceeds(
    updateDoc(doc(asUser(OWNER), 'challenges', CHALLENGE), { title: 'Renamed' }),
  )
})

test('regression: a non-owner cannot edit the challenge document', async () => {
  // This is precisely why Challenge.participantUids / Challenge.standings
  // cannot be fields on this document — a joiner has no write access to it, so
  // those fields could never be maintained by the person joining.
  await assertFails(
    updateDoc(doc(asUser(JOINER), 'challenges', CHALLENGE), {
      participantUids: [JOINER],
    }),
  )
})

test('a non-owner cannot delete the challenge', async () => {
  await assertFails(deleteDoc(doc(asUser(JOINER), 'challenges', CHALLENGE)))
})

// ── Joining, via the participants subcollection ──────────────────────

test('the fix: a non-owner can join by writing their own participant row', async () => {
  await assertSucceeds(
    setDoc(doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER), {
      displayName: 'Joiner',
      score: 0,
    }),
  )
})

// -- C20: `score` is the server's, and this is where that is enforced --
//
// The test that used to sit here asserted the opposite -- `a participant can update
// their own score` -- and it was right until 2026-08-20. Spec 5.2 moved the number to a
// projection function, so the same write must now be refused. It is kept as the
// inverted pair rather than deleted, because the pair is the argument: a participant
// still owns their row, and what changed is exactly one field on it.

test('C20: a participant can still edit their own row -- just not the score', async () => {
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await assertSucceeds(updateDoc(ref, { displayName: 'Joiner Renamed' }))
})

test('C20: a participant cannot move their own score any more', async () => {
  // The positive half of this pair is the test above: without it, this would still
  // pass with the whole participants block deleted, because an unmatched path is
  // denied by default. AGENTS.md's standing warning about vacuous negatives.
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await assertFails(updateDoc(ref, { score: 12.5 }))
})

test('C20: nor set it high while joining', async () => {
  // The create path is a separate clause, and would otherwise be the way round the
  // update rule -- leave, re-join at 9999.
  await assertFails(
    setDoc(doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER), {
      displayName: 'Joiner',
      score: 9999,
    }),
  )
})

test('C20: a whole-document set that drops `score` is refused too', async () => {
  // The trap that a `request.resource.data.score == resource.data.score` rule misses
  // in the other direction: this write does not CHANGE the score, it REMOVES it, and
  // the standings would then render the row at zero. affectedKeys() sees a removal.
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(
      doc(ctx.firestore(), 'challenges', CHALLENGE, 'participants', JOINER),
      { score: 42 },
    )
  })
  await assertFails(setDoc(ref, { displayName: 'Joiner' }))
})

test('C20: the projection reads a report fact the standings reader cannot see', async () => {
  // Where the score now comes from. The fact is private to the reporter under
  // users/{uid}/**, which is what makes `score` cross the ownership boundary -- and so
  // the one derived quantity in spec 5.2's map of seven that needed a stored writer.
  await assertSucceeds(
    setDoc(doc(asUser(JOINER), 'users', JOINER, 'challengeReports', CHALLENGE), {
      value: 12.5,
      reportedAt: 1,
    }),
  )
  await assertFails(
    getDoc(doc(asUser(OWNER), 'users', JOINER, 'challengeReports', CHALLENGE)),
  )
  await assertFails(
    setDoc(doc(asUser(OWNER), 'users', JOINER, 'challengeReports', CHALLENGE), {
      value: 9999,
    }),
  )
})

// -- spec 6 (`C14` #23): the PROVENANCE of the score is the server's too --------------
//
// Ido, 2026-08-24: "If someone updated manually and it was not updated through HEALTH
// CONNECT, then it should say there who performed the update and what they updated."
//
// So a participant row now carries `scoreSource` and `reportedAt` beside `score`. Both are
// pinned here for a reason `score` alone does not cover: a participant who could write
// their own label could TYPE a number and mark it DERIVED, and the label would then assert
// exactly the thing it exists to deny.

test('spec6: a participant cannot label their own score DERIVED', async () => {
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await assertFails(updateDoc(ref, { scoreSource: 'DERIVED' }))
})

test('spec6: nor move the date their score was reported', async () => {
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await assertFails(updateDoc(ref, { reportedAt: 1 }))
})

test('spec6: a participant may still edit the identity fields beside them', async () => {
  // The pin is field-level, not row-level. Renaming yourself is yours; the three server
  // fields are not, and this is what proves the block did not simply close the row.
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await assertSucceeds(updateDoc(ref, { displayName: 'Joiner Renamed' }))
})

test('spec6: a whole-document set that drops the provenance is refused', async () => {
  // The same trap `score` already has, one field over: this write does not CHANGE
  // `scoreSource`, it REMOVES it -- and `affectedKeys()` catches a removal where an
  // equality comparison would not.
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(
      doc(ctx.firestore(), 'challenges', CHALLENGE, 'participants', JOINER),
      { displayName: 'Joiner', score: 8200, scoreSource: 'REPORTED', reportedAt: 7 },
    )
  })
  await assertFails(setDoc(ref, { displayName: 'Joiner', score: 8200 }))
})

test('spec6: the goal link is a fact the reporter owns, and nobody else can read it', async () => {
  // The second shape of the same fact document -- a LINK instead of a typed number. It
  // sits under users/{uid}/**, so the goal a competitor is scoring from stays private:
  // publishing it would put a goal's identity on a world-readable row for a badge that
  // only ever needed to say *derived*.
  await assertSucceeds(
    setDoc(doc(asUser(JOINER), 'users', JOINER, 'challengeReports', CHALLENGE), {
      goalId: 'g1',
      linkedAt: 1,
    }),
  )
  await assertFails(
    getDoc(doc(asUser(OWNER), 'users', JOINER, 'challengeReports', CHALLENGE)),
  )
  await assertFails(
    setDoc(doc(asUser(OWNER), 'users', JOINER, 'challengeReports', CHALLENGE), {
      goalId: 'someone-elses-goal',
    }),
  )
})

test('a participant can leave by deleting their own row', async () => {
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await assertSucceeds(deleteDoc(ref))
})

test('a user cannot write another user’s participant row', async () => {
  await assertFails(
    setDoc(doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', OWNER), {
      displayName: 'Owner',
      score: 9999,
    }),
  )
})

test('even the challenge owner cannot write someone else’s participant row', async () => {
  // Documented limitation, not an oversight: there is no "kick a participant".
  // Granting it needs a get() on the parent challenge inside the rule, which
  // bills a document read on every single evaluation.
  await assertFails(
    setDoc(doc(asUser(OWNER), 'challenges', CHALLENGE, 'participants', JOINER), {
      displayName: 'Joiner',
      score: 0,
    }),
  )
})

test('a signed-out visitor cannot join', async () => {
  await assertFails(
    setDoc(doc(asVisitor(), 'challenges', CHALLENGE, 'participants', JOINER), {
      score: 0,
    }),
  )
})

test('any signed-in user can read the standings', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(
      doc(ctx.firestore(), 'challenges', CHALLENGE, 'participants', OWNER),
      { displayName: 'Owner', score: 30 },
    )
  })
  await assertSucceeds(
    getDoc(doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', OWNER)),
  )
})

// -- #55: completion facts are private, like everything else under users/{uid} --
//
// §1.4 moved the completion out of the task document and into
// `users/{uid}/completionFacts/{taskId}`, banking the minutes and difficulty it was worth.
// That is a NEW COLLECTION holding a record of what somebody did and when, and the only
// thing standing between it and the world is the recursive `{document=**}` wildcard under
// `users/{uid}` -- a rule written years before this collection existed.
//
// Which is exactly why it is worth a case. The wildcard is correct today and a future
// narrowing of it (per-collection rules, a carve-out for one subcollection) would expose
// these silently: no client error, no failing Kotlin test, and this is the ONLY layer that
// can see `firestore.rules` at all.

test('#55: the owner can write and read their own completion facts', async () => {
  const ref = doc(asUser(OWNER), 'users', OWNER, 'completionFacts', 'task_1')
  await assertSucceeds(
    setDoc(ref, { completedAt: 1_755_000_000_000, minutes: 30, difficulty: 'ROUTINE' }),
  )
  await assertSucceeds(getDoc(ref))
  // An untick is a delete of this same path -- "removes exactly the fact it added".
  await assertSucceeds(deleteDoc(ref))
})

test('#55: nobody else can read a completion fact, signed in or not', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users', OWNER, 'completionFacts', 'task_1'), {
      completedAt: 1_755_000_000_000,
      minutes: 30,
      difficulty: 'ROUTINE',
    })
  })
  await assertFails(getDoc(doc(asUser(JOINER), 'users', OWNER, 'completionFacts', 'task_1')))
  await assertFails(getDoc(doc(asVisitor(), 'users', OWNER, 'completionFacts', 'task_1')))
})

test('#55: nobody else can bank a fact into someone else’s account', async () => {
  // The write direction matters as much as the read: a fact IS points, so a writable
  // collection here would be a leaderboard anybody could inflate for anybody.
  await assertFails(
    setDoc(doc(asUser(JOINER), 'users', OWNER, 'completionFacts', 'task_1'), {
      completedAt: 1_755_000_000_000,
      minutes: 480,
      difficulty: 'DEMANDING',
    }),
  )
})

// -- #63: the occurrences collection -----------------------------------
//
// §2.1's occurrences live at `users/{uid}/occurrences/{id}` -- flat and per-user, with the
// owning task as a `taskId` field rather than as a path segment (spec §7.1, `#63`).
//
// THE RULES FILE DID NOT CHANGE, AND THAT IS THE FINDING RATHER THAN THE OMISSION.
// `users/{uid}/{document=**}` already matches every per-user subcollection with an owner-only
// rule, so a new one under that path is a client-side change -- the same result life areas
// produced, which AGENTS.md records as a pitfall. Nothing in an occurrence is server-owned
// either: no Cloud Function reads the collection, and `googleEventId` is written by the client
// because §2.6 buys `calendar.app.created` CLIENT-SIDE and §2.7 says outright that "there is no
// credential for a background sync and cannot be one". So a field-level condition like
// `serverOwns('score')` would have had nothing to protect, and spec §5.2's own test for when a
// derived number needs a stored writer -- does it cross an ownership boundary? -- says no.
//
// Which is exactly why these cases exist. An untested "it is covered by the wildcard" is a
// claim about a file this layer is the ONLY one that can read: the Kotlin suites cannot reach
// `firestore.rules` at all, so a future narrowing of that wildcard would expose an occurrence
// -- what somebody plans to do and when -- with no client error and no failing unit test.
//
// Note the positive cases below and not only the negative ones. AGENTS.md: "pure negative tests
// ('X is denied') pass vacuously when nothing matches at all", so a suite of denials would go
// green against a rules file that denied everything, including the owner.

const occurrenceFixture = (taskId = 'task_1') => ({
  taskId,
  rung: 'BLOCK',
  start: '2026-08-17T09:00',
  end: '2026-08-17T10:30',
  placement: 'CONFIRMED',
  seriesDate: '2026-08-17',
  outcome: 'PLANNED',
  outcomeAt: null,
  googleEventId: null,
})

test('#63: the owner can create, read, edit and delete their own occurrences', async () => {
  const ref = doc(asUser(OWNER), 'users', OWNER, 'occurrences', 'occ_1')
  await assertSucceeds(setDoc(ref, occurrenceFixture()))
  await assertSucceeds(getDoc(ref))
  // Recording an outcome and linking a Google event are both owner updates, and both are
  // field updates rather than whole-document writes -- see OccurrenceRepositoryImpl.
  await assertSucceeds(updateDoc(ref, { outcome: 'DONE', outcomeAt: 1_755_000_000_000 }))
  await assertSucceeds(updateDoc(ref, { googleEventId: 'gcal_abc' }))
  // A THIS_AND_FUTURE skip deletes the stored instances from that date on.
  await assertSucceeds(deleteDoc(ref))
})

test('#63: nobody else can read an occurrence, signed in or not', async () => {
  // An occurrence is a plan -- what this person intends to do, and exactly when. It is at
  // least as private as the completion fact above, which is a record of what they already did.
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users', OWNER, 'occurrences', 'occ_1'), occurrenceFixture())
  })
  await assertFails(getDoc(doc(asUser(JOINER), 'users', OWNER, 'occurrences', 'occ_1')))
  await assertFails(getDoc(doc(asVisitor(), 'users', OWNER, 'occurrences', 'occ_1')))
})

test('#63: nobody else can write an occurrence into someone else’s schedule', async () => {
  await assertFails(
    setDoc(doc(asUser(JOINER), 'users', OWNER, 'occurrences', 'occ_2'), occurrenceFixture()),
  )
  await assertFails(
    setDoc(doc(asVisitor(), 'users', OWNER, 'occurrences', 'occ_2'), occurrenceFixture()),
  )
})

test('#63: nobody else can move or delete an occurrence that already exists', async () => {
  // The write direction that is easy to forget: a stranger who cannot CREATE one might still
  // be able to EDIT one, and moving somebody's block to 03:00 or deleting it outright is the
  // more damaging of the two.
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users', OWNER, 'occurrences', 'occ_1'), occurrenceFixture())
  })
  await assertFails(
    updateDoc(doc(asUser(JOINER), 'users', OWNER, 'occurrences', 'occ_1'), {
      start: '2026-08-17T03:00',
    }),
  )
  await assertFails(deleteDoc(doc(asUser(JOINER), 'users', OWNER, 'occurrences', 'occ_1')))
})

test('#63: the repeat rule on the task is as private as the task it sits on', async () => {
  // §2.1 puts the rule on the task and the instances in their own collection, so the two
  // halves are guarded by two different matches. Asserting only the collection would leave the
  // half that says "every Tuesday at 19:00, indefinitely" checked by nothing.
  const ref = doc(asUser(OWNER), 'users', OWNER, 'tasks', 'task_1')
  await assertSucceeds(
    setDoc(ref, {
      title: 'Water the flowers',
      occurrenceRung: 'ALL_DAY',
      occurrenceStart: '2026-08-17',
      repeatRule: { unit: 'WEEK', interval: 2, weekdays: [], endKind: 'NEVER' },
      pausedUntil: null,
    }),
  )
  await assertFails(getDoc(doc(asUser(JOINER), 'users', OWNER, 'tasks', 'task_1')))
})

test('#63: a whole batch of occurrences is refused for a stranger, one document at a time', async () => {
  // A THIS_AND_FUTURE move materialises the series' past in one WriteBatch. A batch is not a
  // transaction and the rules are evaluated per document, so this asserts the property the
  // batch actually has: every document in it stands or falls on its own path.
  await Promise.all(
    ['occ_a', 'occ_b', 'occ_c'].map((id) =>
      assertFails(
        setDoc(doc(asUser(JOINER), 'users', OWNER, 'occurrences', id), occurrenceFixture()),
      ),
    ),
  )
})

// -- C20: the leaderboard projection ---------------------------------
//
// `publicProfiles` had no coverage in this file before 2026-08-20, because until then
// its rule was one line that said what every other collection said. It now carries the
// same field-level condition as a participant row, for the same reason: `points` is a
// number a leaderboard reader cannot compute, since the tasks it sums live under
// users/{uid}/** where only the owner can read them.

test('C20: any signed-in user can read the leaderboard', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'publicProfiles', OWNER), {
      displayName: 'Owner',
      points: 120,
    })
  })
  await assertSucceeds(getDoc(doc(asUser(JOINER), 'publicProfiles', OWNER)))
})

test('C20: sign-up may create the row, at zero', async () => {
  await assertSucceeds(
    setDoc(doc(asUser(JOINER), 'publicProfiles', JOINER), {
      displayName: 'Joiner',
      points: 0,
      friendCode: 'ABC123',
    }),
  )
})

test('C20: but not create it already scored', async () => {
  await assertFails(
    setDoc(doc(asUser(JOINER), 'publicProfiles', JOINER), {
      displayName: 'Joiner',
      points: 5000,
    }),
  )
})

test('C20: the owner can still refresh their display name and photo', async () => {
  // AuthRepositoryImpl does exactly this on every sign-in, as a merge. If this goes
  // red, signing in stops updating the leaderboard row. It is also the positive half
  // that stops the two negatives below from passing vacuously.
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'publicProfiles', JOINER), {
      displayName: 'Joiner',
      points: 120,
    })
  })
  await assertSucceeds(
    setDoc(
      doc(asUser(JOINER), 'publicProfiles', JOINER),
      { displayName: 'Renamed', photoUrl: null },
      { merge: true },
    ),
  )
})

test('C20: the owner cannot award themselves points', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'publicProfiles', JOINER), {
      displayName: 'Joiner',
      points: 120,
    })
  })
  await assertFails(
    updateDoc(doc(asUser(JOINER), 'publicProfiles', JOINER), { points: 999999 }),
  )
})

test('C20: nor quietly drop the field on a whole-document write', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'publicProfiles', JOINER), {
      displayName: 'Joiner',
      points: 120,
    })
  })
  await assertFails(
    setDoc(doc(asUser(JOINER), 'publicProfiles', JOINER), { displayName: 'Joiner' }),
  )
})

test('C20: and still cannot touch another user\u2019s row', async () => {
  await assertFails(
    setDoc(doc(asUser(JOINER), 'publicProfiles', OWNER), {
      displayName: 'Owner',
      points: 0,
    }),
  )
})

// ── The mirror edge that answers "which challenges am I in?" ─────────

test('a user can write their own users/{uid}/challenges mirror edge', async () => {
  await assertSucceeds(
    setDoc(doc(asUser(JOINER), 'users', JOINER, 'challenges', CHALLENGE), {
      joinedAt: 1,
    }),
  )
})

test('a user cannot write into another user’s challenges mirror', async () => {
  await assertFails(
    setDoc(doc(asUser(JOINER), 'users', OWNER, 'challenges', CHALLENGE), {
      joinedAt: 1,
    }),
  )
})

// ── The shared feed: deleting your own post (issue #5) ───────────────
//
// Every test here is paired, positive with negative, and that is the point
// rather than symmetry for its own sake. AGENTS.md's standing warning is that a
// pure negative test ("X is denied") passes vacuously when nothing matches at
// all: delete the whole `match /shares/{shareId}` block and every `assertFails`
// below still passes, because an unmatched path is denied by default. The
// `assertSucceeds` cases are the ones that would go red, so they are what make
// this suite evidence rather than decoration.

test('any signed-in user can read the feed', async () => {
  await assertSucceeds(getDoc(doc(asUser(JOINER), 'shares', SHARE)))
})

test('a signed-out visitor cannot read the feed', async () => {
  await assertFails(getDoc(doc(asVisitor(), 'shares', SHARE)))
})

test('a user can post a share naming themselves as author', async () => {
  await assertSucceeds(
    setDoc(doc(asUser(JOINER), 'shares', 'new_share'), shareFixture(JOINER)),
  )
})

test('a user cannot post a share attributed to someone else', async () => {
  // Otherwise anyone could put words in a friend's feed under their name.
  await assertFails(
    setDoc(doc(asUser(JOINER), 'shares', 'new_share'), shareFixture(OWNER)),
  )
})

test('the fix: the author can delete their own post', async () => {
  await assertSucceeds(deleteDoc(doc(asUser(OWNER), 'shares', SHARE)))
})

test('a non-author cannot delete someone else’s post', async () => {
  await assertFails(deleteDoc(doc(asUser(JOINER), 'shares', SHARE)))
})

test('a signed-out visitor cannot delete a post', async () => {
  await assertFails(deleteDoc(doc(asVisitor(), 'shares', SHARE)))
})

test('a non-author cannot edit someone else’s post', async () => {
  // Delete is the feature; edit is the same clause, and a rule that let a
  // stranger rewrite the message would make the delete guard pointless.
  await assertFails(
    updateDoc(doc(asUser(JOINER), 'shares', SHARE), { message: 'hijacked' }),
  )
})

// ── The photo that rode along with the post (issue #5, step 5) ───────
//
// SocialRepositoryImpl.deleteShare deletes the Storage object after the
// document, so "the photo outlives the share" is only fixed if the author is
// actually permitted to delete it. That permission lives in storage.rules, which
// no Kotlin suite can reach either.

test('a user can upload an image under their own uid', async () => {
  await assertSucceeds(
    uploadBytes(storageRef(storageAs(JOINER), imagePath(JOINER)), aJpeg(), {
      contentType: 'image/jpeg',
    }),
  )
})

test('a user cannot upload under someone else’s uid', async () => {
  await assertFails(
    uploadBytes(storageRef(storageAs(JOINER), imagePath(OWNER)), aJpeg(), {
      contentType: 'image/jpeg',
    }),
  )
})

test('any signed-in user can read a shared image', async () => {
  // Friends have to be able to see the photo on the post.
  await assertSucceeds(getBytes(storageRef(storageAs(JOINER), imagePath(OWNER))))
})

test('the fix: the uploader can delete their own image', async () => {
  // The one that catches the `write`-covers-delete trap: on a delete there is no
  // `request.resource`, so a rule guarding size and contentType denies the
  // owner their own object and the photo outlives every post that used it.
  await assertSucceeds(deleteObject(storageRef(storageAs(OWNER), imagePath(OWNER))))
})

test('a user cannot delete someone else’s image', async () => {
  await assertFails(deleteObject(storageRef(storageAs(JOINER), imagePath(OWNER))))
})

test('a signed-out visitor cannot delete an image', async () => {
  await assertFails(
    deleteObject(storageRef(storageAsVisitor(), imagePath(OWNER))),
  )
})
