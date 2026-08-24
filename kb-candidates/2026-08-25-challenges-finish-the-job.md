# KB candidates — `challenges-finish-the-job`, 2026-08-25

Each entry stands alone. A transcript is machine-local, outside git, invisible to the other
agent and truncated by compaction, so nothing here leans on one.

---

## 1 · A Firestore read rule that inspects `resource.data` constrains every QUERY, not only every get

**Claim.** When a security rule's `allow read` reads `resource.data` (e.g.
`resource.data.toUid == request.auth.uid`), Firestore refuses **up front** any query it
cannot *prove* is inside the rule — it will not evaluate document by document. The client
must therefore always carry a matching `whereEqualTo`, and an unconstrained
`collection(...)` listener fails with `PERMISSION_DENIED` **on the query itself**, with
nothing in the message naming the missing filter.

**Why.** This is the standard cost of the only shape Firestore rules offer for *"a document
two named parties can each reach and nobody else"* — the alternative (`allow read: if
isSignedIn()`) makes it world-readable. GoalPilot's `shares/{shareId}` takes the world-
readable branch because a share is published to a feed; `challengeInvites/{inviteId}`
(2026-08-25) cannot, because an invite names two people.

**The trap is that it fails toward panic.** The listener works fine in the emulator against
a filtered query, and the unfiltered case is the one a developer writes first while
exploring. *Rejected:* filtering client-side after an unfiltered read — the read never
happens. *Rejected:* an `allow read: if true` with client-side filtering — that publishes
every invite in the database.

**The remedy that matters is the TEST, not the comment**: assert both directions, because
the filtered success alone passes just as happily against a rule that lets everybody read
everything. The discriminating assertion is *the same user whose document it is, denied on
an unfiltered query*.

- **Destination** `kb/dev/` — a new page, `firestore-query-constraining-rules.md`, or a
  section in `firestore-write-semantics.md`
- **Anchors** `firestore.rules` § challenge invites · `firestore-tests/rules.test.mjs`
  §1 · `ChallengeRepositoryImpl.observeIncomingInvites`
- **Supersedes** nothing
- **Status** ready

---

## 2 · In a suite whose subject is a projection, a hand-written derived value is a fixture the system is entitled to overwrite

**Claim.** When testing a Firestore trigger that *derives* a stored number, seeding that
number by hand produces a test that fails for a reason having nothing to do with the code
under test — the projection fires on the seeding write and correctly republishes its own
answer over the fixture. Seed the **inputs** and wait for the projection to publish, then
begin the test.

**Why it is worth a page.** The failure is maximally misleading in three ways at once. It
**names the wrong test** (the one that read the overwritten value, not the one whose seeding
caused it); it reads as a **product bug** in a code path that is behaving perfectly; and the
obvious diagnosis — a cascade leaking from the *previous* test — is plausible enough to
spend a fix on. `Observed:` 2026-08-25, GoalPilot `functions/test/triggers.emulator.mjs`:
a fixture wrote `score: 3000` on a participant row and also gave it a `goalId` link; the
link fired `projectChallengeScore`, which summed that user's (empty) progress and published
**0**. Assertion: `0 !== 3000`. One wrong fix (a settle delay for a cross-test cascade) was
written, run, and did not help before the real cause was found.

**Second finding from the same repair, and it generalises further:** the sibling test that
asserted the reset case — *score becomes 0* — was proving **nothing**, because `0` is also
what the projection produces from a deleted fact. **In a pair of tests over a
derive-or-preserve branch, only the PRESERVE case discriminates.** Check which half of such
a pair could pass against a no-op implementation.

- **Destination** `kb/dev/` — extend `look-at-your-own-output.md`, whose subject is exactly
  verification that fails silently, or a new `testing-projections.md`
- **Anchors** `functions/test/triggers.emulator.mjs#proposeWithTwo` ·
  `CHANGELOG/2026-08-25/challenges-finish-the-job.md` §3
- **Supersedes** nothing
- **Status** ready

---

## 3 · A Compose DIALOG cannot be photographed the way a modal sheet can

**Claim.** GoalPilot's render passes rescue a modal sheet's content with
`onNode(isRoot() and hasAnyDescendant(hasText(anchor)))` instead of `onRoot()`. **That
selector does not rescue an `AlertDialog`.** Capturing it returns a **single flat colour** —
the scrim — in a bitmap that is full-screen, correctly sized, and tens of kilobytes on disk.
The remedy is the same one `StandingsList` and `InviteList` already use: split the dialog's
body into its own composable and photograph that.

**Why this is not already covered.** `kb/dev/android-device-verification.md` and
`CHANGELOG/2026-08-24/challenge-scoring.md` §9 both record the *sheet* case and both record
the selector as the fix. A reader who has internalised that will apply it to a dialog and
get a green frame that is empty — the failure the existing page is about, arriving through
the page's own remedy.

**And the thing that caught it is the finding's real payload:** a **more-than-one-colour**
floor, added after 2026-08-24's blank-frame incident. Every size assertion passed. Sampling
a grid and counting distinct colours is what turned an invisible false green into a named
failure — *"1 distinct colour, expected at least 3"* — and it is cheap enough to belong in
every render pass.

- **Destination** `kb/dev/android-device-verification.md`, the render-pass section
- **Anchors** `ChallengeMeasureChangeRenderPass` (the comment on `Frame.DIALOG_RESET`) ·
  `ChallengeDialogs.kt#MeasureChangeContent`
- **Supersedes** nothing — it narrows the existing sheet guidance rather than replacing it
- **Status** ready

---

## 4 · `/*` inside a KDoc opens a NESTED comment — a Firestore path glob is the natural way to hit it

**Claim.** Already in this repo's `CLAUDE.md` as of 2026-08-24 (`docs-currency-guard`, from
`feature/*` in a KDoc). This session hit it again within the hour, from a different and more
predictable source: writing the Firestore path `users/{friendUid}/**` in a doc comment. The
compiler reported `Missing '}'` at line 80 and `Unclosed comment` at the file's last line,
naming neither the KDoc nor the token.

**What is new and worth promoting out of `CLAUDE.md`:** the trap has a **predictable
generator** in this codebase, not just a random one. Every security-rules discussion writes
`users/{uid}/**` or `challenges/{id}/**`, and every one of those in a KDoc is a build
break. And the check is **mechanical and cheap** — a depth-counting scan over the file
finds it in milliseconds and confirms the fix, where `grep '/\\*'` drowns in legitimate
`/**` openers.

- **Destination** `kb/dev/` — the same page as the `--`-in-XML and backslash-in-
  `.properties` traps, if one exists; otherwise a new `prose-that-is-syntax.md` gathering
  all three, since they are one family
- **Anchors** `core/util/Constants.kt#CHALLENGE_INVITES` (carries the warning inline) ·
  `CLAUDE.md` § the third member of that family
- **Supersedes** nothing; it strengthens the existing `CLAUDE.md` bullet
- **Status** ready

---

## 5 · A pathspec commit builds a TEMPORARY index, so a generated-index hook sees a different tree than your working index

**Claim.** `git commit -F msg -- <paths>` constructs a temp index of `HEAD` + those paths
and points `GIT_INDEX_FILE` at it for hooks. A pre-commit hook that validates a **generated
file against `git ls-files`** therefore compares your working-tree copy of the generated
file against a *narrower* set of files than your real index holds. With a sibling session's
new file staged in the shared index, the generator writes a correct index for the **index**
and the hook rejects it for the **commit** — and the two tools disagree while both are
right.

`Observed:` 2026-08-25, GoalPilot: `New-ChangelogIndex.ps1 -Staged` printed *"CHANGELOG
index current"* and the pre-commit hook printed *"STALE"*, on the same tree, seconds apart.
Four attempts failed before the mechanism was understood.

**The fix that works:** generate the index under the *same* temp index the commit will use —
`GIT_INDEX_FILE=<tmp> git read-tree HEAD; git update-index --add -- <your paths>`, then run
the generator with that `GIT_INDEX_FILE`. **Rejected:** unstaging the sibling's file (it is
their staging work, and the index is shared); hand-editing the generated row (correct
content, but it races the sibling regenerating the same file).

**This is the parallel-sessions rules' pathspec-commit clause meeting a generated shared
file, and the interaction is not written down anywhere.** Both halves are documented; the
collision is not.

- **Destination** `kb/dev/flows/lease.md` §4, which already owns the pathspec-commit
  material, or `kb/dev/` alongside it
- **Anchors** `.git/hooks/pre-commit` · `scripts/New-ChangelogIndex.ps1` (`-Check -Staged`)
- **Supersedes** nothing
- **Status** ready

---

## 6 · "Reset or adapt" was a false fork, and the closure grep that kills it is a KDoc

**Claim.** `C7` §5 offered a challenge owner two modes for changing its measure: *reset* the
scores or *adapt* them. The fork is false. **Adapt** requires either a unit conversion —
which `Measure`'s own KDoc records the app deliberately does not perform — or a re-link,
which restarts the number anyway. So a **kind** change *is* a reset, and a **word-only**
change needs no adapting at all because nothing computes on the word. The consequence is
therefore **derived from the change**, never chosen; the owner is told which one their edit
carries.

**Why it belongs in the KB rather than only the changelog.** `rules/question-axis-naming.md`
already says to run a derivation closure before drafting a fork's options. This is a worked
instance where the thing that collapsed the fork was **not code and not a measurement** — it
was a **committed KDoc stating a deliberate non-capability** (*this app does not convert
units*). The closure grep as usually described looks for a **write path** between two
quantities. Here there was none to find; what settled it was a documented **refusal**.

Second half, and the one that nearly went the other way: having established that a word-only
change is arithmetically free, the tempting move is to wave it through without approval. It
is the one that can lie hardest — relabelling `km` as `miles` leaves every stored number
alone while changing what all of them claim. **The gate is on the claim, not on the
arithmetic.**

- **Destination** `kb/` — as an instance under whatever page carries `question-axis-naming`
  examples; **`rules/` is NOT the destination** and nothing here proposes a rule change
- **Anchors** `Challenge.pendingConsequence` · `functions/src/measureChange.ts#consequenceOf`
  · `CHANGELOG/2026-08-25/challenges-finish-the-job.md` §3
- **Supersedes** nothing
- **Status** ready

---

## 7 · The same "should this be a Cloud Function?" test, answered opposite ways in one session

**Claim.** `C20`'s criterion is *a derived write gets a server writer if and only if the
write has to see documents the writer cannot*. This session applied it twice, hours apart,
and got opposite answers — which is what makes it a usable criterion rather than a slogan:

| | invites (§1) | measure change (§3) |
|---|---|---|
| what the write must see | one document naming two parties | **every** participant row, then the challenge document |
| can a client reach that? | yes — the rules partition models it exactly | **no**, and no single client can |
| verdict | rules only; a Function would be a second write path for nothing | Function |

**The failure mode it guards against is reaching for a Function because a feature *feels*
collaborative.** Invites are the more social feature of the two and need no server at all;
the measure change is a quiet owner-initiated edit and cannot be done without one.

- **Destination** `kb/dev/` — wherever GoalPilot's `C20` / spec §5.2 material lives
- **Anchors** `functions/src/challenges.ts` (header) · `domain/model/ChallengeInvite.kt`
  (header) · `functions/src/index.ts` (the two export blocks)
- **Supersedes** nothing
- **Status** ready
