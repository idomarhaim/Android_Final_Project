# `docs-currency-refresh` — 2026-08-25

> **Summary:** Ido asked (in Hebrew) for the six files under `docs/` to be brought up to the
> current system. They were last repaired on 2026-08-24 against **v0.5.0**; `HEAD` is
> **v0.5.4 / `versionCode` 15**, and four days of work had landed in between. Twenty-three
> findings, all repaired. **Documentation only — no code was touched**, which Ido asked me to
> confirm mid-session and which the diff proves.

**Inherited scope.** `docs-repair` still held a live row on exactly these paths. It was
established **gone** rather than assumed: last commit `3e4f381`/`59283d0` at 2026-08-24 01:50,
last transcript turn 2026-08-23T22:51Z (~39 h quiet), `docs/` clean in the tree, and
`visual-parity` had already read it as gone on 2026-08-25. Its row is released on the board with
that evidence written out. Nothing of theirs was adopted or edited — there was nothing
uncommitted to advance. `README.md` is in their scope and **not** in mine: Ido named the six
`docs/` files.

---

## What was wrong, by file

### `docs/ARCHITECTURE.md`

| # | Finding |
|---|---|
| 1 | **"Eleven feature packages"** against twelve — `feature/sync/` was missing entirely |
| 2 | **Challenges had no section at all** — four lines of a collection tree for the app's largest subsystem, so every reader had to go to the spec |
| 3 | The `challengeInvites` / `fileGoal` / `planGoal` entries were **names with no prose**, pasted in by a session that owned none of them to unblock a release |
| 4 | `users/{uid}/challenges/{id}` — the private mirror edge — **absent from the data-model tree** |
| 5 | `participants/{uid}` listed only `{ score }`, not `joinedAt` / `linkedGoalId` / `approvedChangeId` |
| 6 | Nothing on the **narrow-screen layout rule** (`FlowRow` + `maxLines = 1`), the repair that produced v0.5.4 |
| 7 | Nothing on the **widget panel translucency** (`WIDGET_PANEL_ALPHA = 0.78`) |

### `docs/OPERATIONS.md`

| # | Finding |
|---|---|
| 8 | Callables listed as **four**; there are **six** (`fileGoal`, `planGoal` missing) |
| 9 | Triggers listed as **three**; there are **six** (`projectChallengeScoreOnProgress`, `applyMeasureChangeOnApproval`, `applyMeasureChangeOnProposal` missing) |
| 10 | Challenges described as *"shipped"* with no mention that `#23` **closed**, nor of Health Connect as a source or retroactive scoring |
| 11 | No shipped-version row at all |
| 12 | **Ido's real S25 Ultra is routinely attached and nothing said so** — a bare `adb` command can reach his phone |
| 13 | Section 7 *"Open item not yet resolved"* had been resolved; `git status .github/` is clean and the file it names does not exist |
| 14 | The `PIPESTATUS` trap was recorded only in its harmless form, not the one that **installs a stale APK and reports the previous build's results** |
| 15 | Nothing on the emulator capture-surface wedge that reads exactly like a code regression |

### `docs/SETUP.md`

| # | Finding |
|---|---|
| 16 | Section 5 said *"keep OAuth in **Testing** mode"* — **the project has been In production since 2026-08-09**, and `OPERATIONS.md` said so four hundred lines away. Two docs, opposite answers |
| 17 | No mention of the phone workflow at all: `mirror-phone.ps1`, `Run On Phone.cmd`, or the `adb -s` requirement |
| 18 | No warning that `local.properties` is a Java `.properties` file where **a backslash is an escape character** |

### `docs/RELEASING.md`

| # | Finding |
|---|---|
| 19 | **The documented tag route is not the route in use.** `git tag` ends at `v0.3.1`; since then there are **nine `versionCode` bumps and zero tags**, and the six distributions with evidence all went out **locally**. A reader following section 3 would have been following a path nobody has taken since v0.3.1 |
| 20 | Section 2.1a **contradicted its own status line four paragraphs above it** — *"the tag route is the only one that can produce an installable update"* and *"the key is now in one place"* are both pre-recovery text left standing after the 2026-08-21 recovery |
| 21 | No instruction to verify the APK before uploading it |

### `docs/PRODUCT_v0.3.md`

| # | Finding |
|---|---|
| 22 | Section 6 carries two decisions Ido **overrode on 2026-08-25** in as many words (*"there is a conflict with what I said before — update per what I said now"*), and the spec still stated the old ones flatly |
| 23 | A link to `sessions/c20-build-half.md`, which has since moved to `sessions/done/` |

---

## The corrections that are corrections, not additions

Three of the above are **sentences that were simply wrong**, and they are the ones worth naming
separately, because `DocsCurrencyTest` cannot reach any of them — every assertion it makes is a
presence check over an enumeration, and its own KDoc says so.

- **`SETUP.md` section 5 vs `OPERATIONS.md` on OAuth mode.** A reader setting up the project would
  have put it in Testing mode and inherited the seven-day authorization expiry that OPERATIONS
  spends a page explaining why the project left.
- **`RELEASING.md` section 2.1a's two pre-recovery sentences.** Both are repaired **in place with
  the correction named**, rather than deleted — the correction is the useful part, and one of them
  (the local build silently falling back to the debug key) is still true whenever the keystore is
  absent.
- **`RELEASING.md` section 1's tag route.** Not wrong as a description of the workflow; wrong as an
  answer to *"how does a build get to a tester here?"*, which is the question the file opens by
  saying it answers.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **1197 / 1197**, 0 failures, 0 errors, 0 skipped, 93 suites |
| `DocsCurrencyTest` | **5 / 5** — and it **executed**, not `UP-TO-DATE`: the docs edits forced a re-run, which is the `inputs` declaration in `app/build.gradle.kts` working as designed |
| `ReleaseNotesGuardTest` | **4 / 4** |
| `Assert-NoControlChars.ps1` | clean |
| Instrumented / rules / functions | **not run — no code changed at any of those layers.** Stated explicitly rather than skipped silently |

**Test counts read from the JUnit XML, not from `BUILD SUCCESSFUL`** — a Gradle test task goes
green when zero tests are discovered, which is how a harness failure reports as a pass.

### Checks the guard cannot make, run mechanically instead

Every enumeration and count written into these files was **recomputed from the source and
diffed**, rather than re-read:

- feature packages — doc says `Twelve`, `ls` says 12, and the twelve names match the directory
  listing exactly (no missing, no extra);
- callables — 6 in `functions/src/index.ts`, all 6 present in the OPERATIONS row;
- triggers — 6 across `projection.ts` + `challenges.ts`, all 6 present;
- version — `versionCode 15` / `0.5.4` from `app/build.gradle.kts`, matched in both docs that
  quote it;
- **all 48 relative links across the six files resolve** — URL-decoded before testing, or two
  `%20` paths read as broken when they are fine. This is what found finding 23.

**And the self-review caught a number I had not computed.** The first draft of finding 19 said
*"the last five releases"*, reasoning from the tag gap rather than counting. Reconstructing
`versionCode` from the history of `app/build.gradle.kts` gives **nine** bumps since `v0.3.1`, of
which **six** have a changelog naming the route or an App Distribution release id. The document
now carries the mechanical half as fact and the rest as `Observed:` / `Inferred:`.

**One of those checks caught its own author.** The Python heredoc written to add the
`local.properties` backslash warning to `SETUP.md` died with `truncated \UXXXXXXXX escape` — the
exact trap the paragraph was about, one layer up. That is now in the paragraph.

---

## ⚠️ One defect found and NOT fixed, because it is code

`.github/workflows/instrumented-tests.yml` **contradicts itself.** Lines 14–17 say the workflow is
*"DELIBERATELY MANUAL"*, that there is *"no `push:` trigger yet"*, and that the commented-out block
below should be enabled once a green run is on the record. Lines 35–39, twenty lines later, **are
that block — enabled**, with its own dated comment (*"Enabled 2026-08-19 after run #1 came back
green"*). The trigger is live; the header comment still tells you it is not.

`docs/CLOUD-DEVICE.md` section 3 is **right** and the workflow's own comment is wrong, so no doc
change was needed. Left alone because it is a source file and Ido confirmed mid-session that this
session changes documents only. It is a one-line deletion for whoever picks it up.
