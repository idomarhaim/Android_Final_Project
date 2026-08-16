# KB candidates — `widget-pack`, 2026-08-15

Session: `/implement #10` (the home-screen widget pack) · repo `C:\Dev\Android_Final_Project` ·
branch `feat/goalpilot-implementation` · mode `AUTO MODE`.

**Each entry stands alone.** No transcript is a source: everything needed to write the page is
below, including what was rejected and why.

---

> **DRAINED 2026-08-16 — entries 1 and 2 promoted, entry 3 survives below.**
>
> - **1 · A test that a rule *fired* is only half a test** → new page
>   `C:\Dev\JARVIS\kb\dev\present-and-proportioned.md`
> - **2 · Deleting the shared class is not the same as deleting the shared behaviour** → new page
>   `C:\Dev\JARVIS\kb\devramework-resolves-by-type.md`
>
> Drained by session `kb-drain-goalpilot`, visiting `C:\Dev\JARVIS` with a row on that board.
> Journalled in `kb/log/2026-08-16.md` with this file's repo-qualified name, which is the
> candidate→page tie across the two repos. The full text of both entries is not reproduced here —
> the committed pages are the canonical form.
>
> **This file is kept, not deleted, because entry 3 is still parked.**

---

## Standing — always-ask

## 3 · Shared build output is a fifth kind of contention, and the board has no column for it

**Claim.** The session board partitions **source paths** and names **singletons** (the daemon, a
device, the git index). Neither covers `app/build/` — a *derived* tree that every session writes
concurrently through a tool that assumes it owns it. On Windows this produces failures that look
like defects in the code under test: `NoSuchFileException` on a KSP-generated file, `failed to
make parent directories`, `Could not move temporary workspace … dependencies-accessors`, and
`Cannot find required type element <Application>`. **None of them mention concurrency**, and
`Cannot find required type element` in particular reads as a broken Hilt setup.

**Why.** Claiming the Gradle daemon is not the same as claiming the build directory: the daemon is
a *process* and sessions politely queue on it, while `app/build/` is *state*, and two builds
minutes apart still interleave through it via stale caches and open handles. Four sessions ran in
this tree on 2026-08-15; the KSP failure hit twice, the accessors failure survived four retries and
a `--stop`, and each time the honest diagnosis was *someone else built*.

Two consequences worth stating separately from the diagnosis:

- **Re-running is not the remedy and re-running teaches you it is.** A plain retry sometimes
  succeeds (the other build finished), which trains the reflex, and then fails four times in a row
  when a handle is genuinely held. Reach for `rm -rf app/build/generated/ksp` — and for
  `.gradle/<version>/dependencies-accessors` after a version-catalogue edit — **early**, not after
  the third retry.
- **A red tree is attributable, and attributing it is a courtesy with a deadline.** One red main
  source set stops *every* source set in the module, so it blocks every session at once. Twice in
  one evening a session was blocked by a file it did not own; both times the fix was one sibling
  reading the error, checking `git status` for the owner, and saying so on the board rather than
  editing the file. That is fast when it happens and expensive when it does not — the blocked
  session otherwise concludes its own unit is broken.

**Rejected on the way:** (a) claiming `app/build/` as a board path — it is derived, nobody *owns*
it, and a claim would block every session's build rather than serialise it; (b) a worktree per
session, which fixes it completely and was not authorised (global rules: no worktrees unless
asked); (c) treating it as the existing Gradle-daemon singleton, which is what everyone had
already done and is precisely what did not work.

**Destination.** `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5 — the singleton
clause, which currently names *the build daemon* and should name *the build daemon **and its
output tree***, plus the attribution duty. **Destination `rules/`, therefore ⛔ always-ask in both
modes**, and the 🎬 walkthrough gate owns it.

*(The Windows-lock half alone — symptoms and remedies, no rule change — could land in
`kb/dev/` independently and would be useful there. Splitting it that way is a judgement for the
ingesting session, not one to take here.)*

**Anchors.** `CHANGELOG/2026-08-15/widget-pack.md` (*Two things worth not re-deriving*) ·
`SESSIONS.md` notes of 20:52 and 21:26 · `AGENTS.md` Windows-file-locks pitfall.

**Supersedes.** Nothing; it **extends** the singleton clause.

**Status.** ⛔ **Always-ask — not drained, and not proposed as a diff.** Destination `rules/`.
