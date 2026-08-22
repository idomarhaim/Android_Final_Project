# KB candidates — `63-occurrences-and-recurrence`, 2026-08-23

Each entry stands alone: another session's chat history is not a source.

---

## 1 · A security-rules suite where only the POSITIVE case discriminates — measured, 1 of 6

- **Claim.** In a rules suite for a collection covered by a recursive wildcard
  (`match /{document=**}`), the **denial** tests pass whether or not the wildcard is there,
  because a path that matches *no rule* is denied — which is the same observable outcome as a
  path denied *by* the rule. Only the **owner-succeeds** test can tell the two apart. So a suite
  written as *"a stranger cannot read it, a stranger cannot write it, a stranger cannot delete
  it"* is fully green against a rules file that denies the owner as well, and against one that
  never mentions the collection at all.

  `Observed:` 2026-08-23, `C:\Dev\Android_Final_Project`, session
  `63-occurrences-and-recurrence`. Six new cases were added for `users/{uid}/occurrences`. The
  mutation — replacing `match /{document=**}` under `users/{uid}` with three explicit
  per-collection matches that omit `occurrences` — produced **1 failure out of 6**: the single
  case that asserts the owner *can* create, read, update and delete. All four denial cases and
  the repeat-rule privacy case stayed green. Restoring the wildcard: 50/50 pass.

- **Why it is worth recording despite AGENTS.md already saying it.** This repo's `AGENTS.md`
  carries the claim in one clause — *"pure negative tests ('X is denied') pass vacuously when
  nothing matches at all"* — with no measurement and no ratio. The ratio is the part that
  changes behaviour: **5 of 6 cases were decorative**, and a reader who assumes the proportion
  is the other way round will write four denials, feel covered, and be covered by nothing. The
  mutation that produces the number costs one edit and one run.

- **The generalisation, and its boundary.** It is not about Firestore. It fires wherever the
  **absence of a rule** and the **presence of a denying rule** are observationally identical:
  an allowlist with a default-deny, a CORS origin list, a firewall's last rule, a router with a
  404 fallback. The discriminator is always the same — assert the thing that is supposed to
  *work*, on the same path, in the same run. Where default is **allow**, the polarity flips and
  it is the denial that discriminates.

- **Destination.** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` — a § under the
  mutation-check material, because this is *the instrument passes on the input it exists for*
  with a measured ratio, not a new subject.
- **Anchors.** `firestore-tests/rules.test.mjs` (the `#63` section header carries the argument) ·
  `CHANGELOG/2026-08-23/63-occurrences-and-recurrence.md` §Tests · `AGENTS.md` §Pitfalls.
- **Supersedes.** Nothing; it measures a standing claim that had no number.
- **Bundle check.** Grepped 2026-08-23: `look-at-your-own-output.md` holds the mutation-check
  material; no page covers vacuous negative tests specifically.
- **Status.** Pending.

---

## 2 · `UP-TO-DATE` can mean **a sibling session already ran it** — the concurrency variant

- **Claim.** The standing warning that a Gradle task reporting `UP-TO-DATE` hands you the
  *previous* run's result assumes the previous run was **yours**. In a repo where two sessions
  share one working tree and one Gradle daemon, it need not be: a sibling can execute
  `testDebugUnitTest` between your two commands, and your next invocation then reports
  `BUILD SUCCESSFUL` with `Task :app:testDebugUnitTest UP-TO-DATE` for a run **you did not
  start, over a source tree that included their uncommitted edits**. The XML under
  `build/test-results/` is theirs too, so reading the results file does not catch it either.

  `Observed:` 2026-08-23, `C:\Dev\Android_Final_Project`. `:app:testDebugUnitTest` came back
  `UP-TO-DATE` in 7 s immediately after new test classes had been added and had never once been
  executed. `--rerun` produced `1 executed` in 27 s and the new classes appeared.

- **Why it is worse than the single-session case.** The single-session failure is *stale*: the
  result is old but it is about your code. This one is *foreign*: the result is fresh, green,
  and about a **tree containing another session's half-finished work**. It fails toward
  confidence — a green you did not earn, on code you have not tested, at exactly the moment two
  sessions are interleaving.

- **The remedy, and the cheap tell.** `--rerun` on the one task (not `--rerun-tasks`, which
  rebuilds everything). The tell is unchanged and still the only one: **read the task line**, not
  `BUILD SUCCESSFUL`, and treat a suite that finishes in single-digit seconds as not having run.

- **Destination.** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` §4c — an instance appended
  to *the build never ran the check you are reading the result of*, adding the concurrency shape.
  It sits beside the `kb-candidates/2026-08-22-tutorial-onboarding.md` entry 1 destined for the
  same §, which is the undeclared-inputs shape of the same failure.
- **Anchors.** `CHANGELOG/2026-08-23/63-occurrences-and-recurrence.md` §Tests ·
  `SESSIONS.md` (four concurrent rows on this date).
- **Supersedes.** Nothing; it widens §4c's trigger from *your own inputs* to *anyone's*.
- **Bundle check.** Grepped 2026-08-23: §4c and §4c-i are the existing home and neither mentions
  a second session.
- **Status.** Pending.

---

## 3 · A compile error in files you do not own is a sibling's tree, not your defect

- **Claim.** In a shared working tree, the **first** reading of a Kotlin compile failure should
  be `git status`, not the error. A test source set compiles as one unit, so a sibling
  mid-edit — an interface that gained a member, a fake that has not caught up, a constructor
  that gained a parameter — takes your test layer down entirely, with an error naming **their**
  files and yours nowhere in it. Nothing in the message says which session authored the lines.

  `Observed:` 2026-08-23, `C:\Dev\Android_Final_Project`. `:app:compileDebugUnitTestKotlin`
  failed with four errors across `RecommendationRepositoryFallbackTest`,
  `GoalDetailViewModelTest` and `FakeAppPreferences` — an unimplemented
  `isMeasureProposalDismissed` and a constructor that had gained a `preferences` parameter.
  All of it was `65-measure-proposal`'s uncommitted work; none of it was in this session's
  claimed paths. Re-running the same command later, unchanged, succeeded — their tree had
  settled.

- **Why it needs saying.** The instinctive response to a compile error is to **fix it**, and the
  fix is usually one line. Doing so edits a file another session holds, in a tree where they are
  still writing — the exact write the board exists to prevent, arriving disguised as
  housekeeping. The board and the lease both guard *deliberate* edits; nothing guards the one
  you make because the compiler asked you to.

- **What to do instead.** Read the board row that owns those paths, keep working on the layers
  that do not share their compile unit — here `firestore-tests/`, which is a separate Node suite
  and was entirely unaffected — and retry. A sibling mid-unit is measured in minutes, and the
  wait costs nothing if it is spent on the other half of your own work.

- **Destination.** `C:\Dev\JARVIS\kb\dev\` — a § in the parallel-sessions material, or a short
  page of its own if none fits. It is a **reading** rule, not a mechanism, so it belongs with
  the board rather than with the build.
- **Anchors.** `SESSIONS.md` (`63-occurrences-and-recurrence` and `65-measure-proposal`,
  2026-08-23) · `CHANGELOG/2026-08-23/63-occurrences-and-recurrence.md` §Notes.
- **Supersedes.** Nothing.
- **Bundle check.** Grepped 2026-08-23: `rules/agent-topology-and-model-routing.md` §5 covers
  claims and staging; nothing covers a shared **compile unit**, which no claim can partition.
- **Status.** Pending.
