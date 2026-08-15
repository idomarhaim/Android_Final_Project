# KB candidates — `social-share-bugs`, 2026-08-15

Session: `/kickoff social-share-bugs` (issues `#4`, `#5`). Mode: `AUTO MODE`.
Each entry stands alone — no transcript is a source.

---

## 1. In Firebase **Storage** rules, `allow write` silently forbids deletion whenever it guards `request.resource`

**Claim.** Storage rules expand `write` to `create, update, delete`. A **delete carries no incoming
object**, so `request.resource` is `null`. Any condition that reads a field of it — the near-universal
`request.resource.size < N` / `request.resource.contentType.matches('image/.*')` pair from every
tutorial — therefore raises inside the rule, and a raising rule **denies**. The result is a ruleset
that looks like "the owner may write their own files" and in fact means "**nobody**, including the
owner, may ever delete one." Nothing about the symptom points at the rule: the client gets
`storage/unauthorized` on an object it just uploaded.

The fix is to split the clause, not to loosen it:

```
allow create, update: if request.auth != null
  && request.auth.uid == uid
  && request.resource.size < 10 * 1024 * 1024
  && request.resource.contentType.matches('image/.*');
allow delete: if request.auth != null && request.auth.uid == uid;
```

**Why.** Found 2026-08-15 in GoalPilot while implementing issue `#5`, and found *only* because a
Storage-rules test was written for it — the Kotlin layers cannot reach a rules file at all, and the
app's own code path swallows the failure (it deletes the Firestore document first and treats image
cleanup as best-effort), so in production this would have presented as "shared photos accumulate
forever" with no error anywhere. `Observed:` the emulator names it exactly —
`com.google.firebase.rules.runtime.common.EvaluationException: Error: storage.rules line [12],
column [12]. Null value error.`

**Rejected:** dropping the size/contentType guards to make one `allow write` work — that removes the
upload guard, which is the thing actually protecting the bucket, to fix a delete. Also rejected:
assuming symmetry with **Firestore** rules, where the same `write` expansion exists but the idiom
`resource.data.authorUid == request.auth.uid` reads the *stored* document (`resource`, not
`request.resource`) and so is null-safe on a delete by construction. The two products differ exactly
where it matters, which is why the Firestore half of the same feature was already correct and the
Storage half was not.

**Destination.** `kb/dev/` — a Firebase security-rules page (new, or folded into an existing Firebase
page if one exists). Pairs naturally with the already-committed GoalPilot note that
`firebase emulators:exec` does not validate a rules file at all.

**Anchors.** `storage.rules`, `firestore-tests/rules.test.mjs`,
`CHANGELOG/2026-08-15/social-share-bugs.md` (GoalPilot).

**Supersedes.** Nothing.

**Status.** 🟢 Eligible. Cross-repo (`C:\Dev\JARVIS`) — owes a row on that board before the write.

---

## 2. Pay a rules suite's non-vacuity check by degrading the ruleset, not by reasoning about it

**Claim.** `AGENTS.md` already warns that a pure negative rules test ("X is denied") passes vacuously
when nothing matches at all. The warning says *what* to fear; the cheap procedure that discharges it
is: write a **throwaway** script that loads the ruleset, degrades it to its pre-change state — string-
strip the block under test, or inline the old clause verbatim — and asserts that the **positive**
tests now `assertFails`. Run it once, record the output in the changelog, delete the script.

```js
const withoutShares = firestoreRules.replace(
  /\n\s*\/\/ ── Shared achievement feed[\s\S]*?\n {4}}\n/, '\n')
if (withoutShares.includes('/shares/')) throw new Error('failed to strip')
// ... initializeTestEnvironment({ firestore: { rules: withoutShares } })
test('OLD RULES: the author canNOT delete their own post', async () => {
  await assertFails(deleteDoc(doc(asUser(OWNER), 'shares', SHARE)))
})
```

**Why.** The instinct on reading the warning is to add a positive test and call it handled — but
"there is an `assertSucceeds` in the file" is not the same claim as "*this* `assertSucceeds` fails
without the rule", and only the second is what makes the suite evidence. Throwaway rather than
committed on purpose: a permanent old-rules suite tests history, goes stale at the next rules change,
and has to be maintained forever to prove something that was true once. The changelog keeps the
result; the repo keeps nothing.

**Rejected:** editing the real rules file in place and reverting — it is a shared, committed file and
a sibling session could commit the degraded copy, which is the read-before-write hazard in a form
where the damage looks like a working tree. Loading a modified *string* touches no file.

**Destination.** `kb/dev/` — a testing-discipline page, or alongside entry 1 on the rules page.

**Anchors.** `CHANGELOG/2026-08-15/social-share-bugs.md` §"The non-vacuity check, run and then thrown
away" (GoalPilot), `AGENTS.md` pitfall on `firebase emulators:exec`.

**Supersedes.** Nothing. Extends the existing `AGENTS.md` warning from a caution into a procedure.

**Status.** 🟢 Eligible. Cross-repo (`C:\Dev\JARVIS`) — owes a row on that board before the write.
