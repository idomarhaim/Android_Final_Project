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

test('a participant can update their own score', async () => {
  const ref = doc(asUser(JOINER), 'challenges', CHALLENGE, 'participants', JOINER)
  await assertSucceeds(setDoc(ref, { displayName: 'Joiner', score: 0 }))
  await assertSucceeds(updateDoc(ref, { score: 12.5 }))
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
