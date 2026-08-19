# KB candidates — `new-machine-checkup`, 2026-08-19

Session: first build and first device run on the machine that replaced the one in repair.
Account: [`CHANGELOG/2026-08-19/new-machine-checkup.md`](../CHANGELOG/2026-08-19/new-machine-checkup.md).

**Three candidates, none drained.** All three are cross-repo (pages belong in
`C:\Dev\JARVIS\kb\`), so they are routed to the already-briefed
[`sessions/kb-drain-51e-backfill.md`](../sessions/kb-drain-51e-backfill.md) rather than
drained inline — the same routing `completion-roadmap` recorded on 2026-08-17, and for the
same reason: a cross-repo drain owes a row on JARVIS's board, and two sessions writing
`kb/log/2026-08-19.md` on one day is the contention that brief exists to avoid.
**This is a deviation from AUTO MODE's drain-at-the-commit-trigger default and is logged
as one**, not a silent skip.

---

## 1 · An escape-processed config file fails in the layer *below* the one that reads it

- **Claim.** When a config file's format has **escape processing** — Java `.properties`,
  `.ini`, YAML single vs double quotes, `.env` — a wrong value does not fail at the parser.
  It parses **successfully into a different value**, and the error surfaces one layer down,
  in a component that names neither the file nor the key. So when a build fails on a *path*,
  a *URL* or an *identifier* that "looks right in the file", read the file **as its parser
  would**, not as text.
- **Why.** GoalPilot's `local.properties`, regenerated on a new machine, read
  `sdk.dir=C\:\Users\namei\AppData\Local\Android\Sdk`. `java.util.Properties` treats `\` as
  an escape, so `\U` `\A` `\L` `\S` are each consumed and the value becomes
  `C:UsersnameiAppDataLocalAndroidSdk`. `assembleDebug` died on
  `:app:compileDebugJavaWithJavac` with
  `java.io.IOException: The filename, directory name, or volume label syntax is incorrect`
  — no file name, no property name, no mention of the SDK. **The `\:` is what makes it
  convincing:** escaping the key/value separator is legitimate and deliberate-looking, so
  the line reads as *carefully* escaped and therefore correct, and every subsequent reading
  of it confirms that. **Rejected framing:** *"validate config at startup"* — AGP does
  validate, and the value it validated was a syntactically fine string that happened to name
  nothing. The remedy is a reading habit, not a check. Second instance the same hour, which
  is what makes it a class rather than an anecdote: the same `\U` swallowed a Python
  heredoc three times while *writing the fix*, failing as
  `truncated \UXXXXXXXX escape`.
- **Destination.** `kb/dev/` — a new page. Nearest neighbour is
  `kb/dev/look-at-your-own-output.md` (*recompute what a consumer computes, don't read it*),
  and this is a **specialisation** of it — the consumer here is a parser — so it should link
  there rather than duplicate it.
- **Anchors.** GoalPilot `04e944f`; `CHANGELOG/2026-08-19/new-machine-checkup.md` §1;
  `CLAUDE.md` (the third JDK bullet); `local.properties` is git-ignored and carries the
  warning inline.
- **Supersedes.** Nothing.
- **Status.** Pending — routed to `kb-drain-51e-backfill`.

---

## 2 · An agent tool shell holds a snapshot of the environment, so `$env:X` and the machine value disagree

- **Claim.** A Claude Code tool shell inherits the environment as it was when the harness
  started, so an environment variable set by an installer **during** the session is absent
  from every shell the agent opens — indefinitely, not until the next command. The tell is
  that `[Environment]::GetEnvironmentVariable('X','Machine')` and `$env:X` **disagree**, and
  the correct reading of that disagreement is *my shell is stale*, not *the machine is
  broken*. Read the persisted value whenever a session's own earlier step, or a previous
  session, installed the thing you are looking for.
- **Why.** GoalPilot, 2026-08-19. `0e52a66` (an earlier session) installed Temurin 21 and
  the machine `JAVA_HOME` and `PATH` were correct. In this session's shells `JAVA_HOME` was
  **empty** and `java` was on no `PATH` — which looks exactly like the blocker the previous
  session had just reported fixing, i.e. it looks like *the fix did not take*. It had.
  **This lands in the dangerous direction**: the natural next move is to re-diagnose or
  re-install a machine that is already correct. **Rejected framing:** *"just always set it
  yourself"* — that is the right remedy for the JDK specifically and it is written into
  `CLAUDE.md`, but it does not generalise: the failure is a **reading** failure about what a
  shell's environment represents, and it recurs for every tool installed mid-session.
  Sub-finding, Git Bash only: exporting a Windows-form path into `PATH`
  (`C:/Program Files/...`) resolves nothing and produces the same *command not found* — Git
  Bash `PATH` wants `/c/Program Files/...`, while `JAVA_HOME` accepts the Windows form.
- **Destination.** `kb/dev/claude-code-surfaces.md` — a section, if that page's scope covers
  the tool-shell environment; otherwise a short page of its own. **Check the page before
  choosing**, and grep `kb/` for *stale environment*, *tool shell*, *inherited env* first.
- **Anchors.** GoalPilot `04e944f`; `CHANGELOG/2026-08-19/new-machine-checkup.md` §3;
  `CLAUDE.md` (the second JDK bullet).
- **Supersedes.** Nothing. It **adds to** the JDK trap already recorded in GoalPilot's
  `AGENTS.md` §JDK rather than replacing it.
- **Status.** Pending — routed to `kb-drain-51e-backfill`.

---

## 3 · A text-mode round-trip is not lossless, and `SESSIONS.md` is where it bites

- **Claim.** Editing a shared board or log file with a script that **reads and writes it as
  text** can silently corrupt lines the script never intended to touch. A lone `\r` inside a
  line — routinely produced when someone writes a Windows path like `C:\Dev\JARVIS\rules\…`
  into prose — is translated to a newline by universal-newline reading, splitting one line
  into two. On `SESSIONS.md` that splits a **table row**, which breaks a *different
  session's* released account. Read and write these files **binary** and insert your line
  into the byte sequence.
- **Why.** GoalPilot, 2026-08-19. A one-line claim-row insertion done with Python's default
  text mode split `brief-refresh`'s released row in two (`698ff54`, repaired in `b5fb371`).
  The file held **exactly one** CR in 3,623 lines — inside the string `C:\Dev\JARVIS\rules\`,
  where `\r` had already become a carriage return at authoring time and rendered as
  `JARVISules`. **The damage is not in your own edit**, which is what makes it survive
  review: the diff of the line you *meant* to add is perfect, and the corruption is in an
  untouched line elsewhere in the file. `git diff --stat` reported `1 insertion` for a
  one-line insert and `3 insertions, 1 deletion` for this one — a one-glance tell that was
  available and easy to skim past. **Rejected framing:** *"normalise the file's line endings"*
  — that is a change to another session's committed bytes to make your tooling convenient,
  and the mangled path is theirs to fix.
- **Destination.** `kb/dev/` — likely a section on an existing page about editing the
  claim board or shared records; the concept sits beside `rules/read-before-write.md`'s
  *records of events get the hard form: append, never rewrite the file as a whole*, which
  this is a **mechanism** for rather than a new rule. Grep `kb/` and `rules/` for
  *round-trip*, *line endings*, *binary* before creating a page.
- **Anchors.** GoalPilot `698ff54` (the damage) and `b5fb371` (the repair, whose message
  carries the mechanism); `SESSIONS.md` `brief-refresh` released row.
- **Supersedes.** Nothing.
- **Status.** Pending — routed to `kb-drain-51e-backfill`.

---

## Deviation note

AUTO MODE was in effect, and its default is to drain at the commit trigger. These three were
**not** drained, for the reason at the top: all three are cross-repo, `kb-drain-51e-backfill`
already exists to drain this repo's backlog into `C:\Dev\JARVIS`, and it now has **four**
files to take instead of three. Nothing here is always-ask — no `rules/` destination, and
nothing supersedes a standing KB claim — so this is a routing decision, not a permission one,
and it is Ido's to overturn.
