/**
 * Security-rules tests for ../firestore.rules.
 *
 * These run against the local Firestore emulator under the project id
 * `demo-goalpilot`. The `demo-` prefix is load-bearing: the emulator suite
 * treats such ids as offline-only and refuses to reach any real backend, so a
 * test can never touch the live project `goalpilot-56e30`.
 *
 * The suite exists because of the challenges feature, where the rules decide
 * the data model rather than merely guarding it: the challenge document is
 * owner-only, so joining CANNOT be an edit to it, and participation has to
 * live in a subcollection each user writes for themselves. The two tests named
 * "regression" and "the fix" below are that argument, executable.
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

const here = dirname(fileURLToPath(import.meta.url))
const rules = readFileSync(resolve(here, '..', 'firestore.rules'), 'utf8')

const OWNER = 'uid_owner'
const JOINER = 'uid_joiner'
const CHALLENGE = 'challenge_run_streak'

let env

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-goalpilot',
    firestore: { rules },
  })
})

after(async () => {
  await env?.cleanup()
})

beforeEach(async () => {
  await env.clearFirestore()
  // Seed one challenge owned by OWNER, bypassing rules — a fixture is not the
  // thing under test.
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'challenges', CHALLENGE), {
      title: '7-day run streak',
      description: 'Most km this week',
      ownerUid: OWNER,
      type: 'RUNNING',
      metricUnit: 'km',
    })
  })
})

const asUser = (uid) => env.authenticatedContext(uid).firestore()
const asVisitor = () => env.unauthenticatedContext().firestore()

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
