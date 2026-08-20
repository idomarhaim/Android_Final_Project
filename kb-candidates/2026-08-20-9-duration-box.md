# KB candidates — `9-duration-box`, 2026-08-20

Session: `9-duration-box` · issue [#9](https://github.com/idomarhaim/Android_Final_Project/issues/9) ·
mode `AUTO MODE`. Rounds 1–3 drained in `48e94bc` and `8187bbb`; this file holds round 4.
Account: [`CHANGELOG/2026-08-20/9-duration-box.md`](../CHANGELOG/2026-08-20/9-duration-box.md).

---

## 4 — The KB has a fully triggered deposit path and no withdrawal path at all

**Claim.** Getting knowledge *into* the KB is specified at every step and fires on its own:
`/kb-flag` → `kb-candidates/<date>-<session>.md` → `/kb-ingest`, triggered at the commit trigger, on
any 🔀 split signal, on `/handoff`, on `/kickoff`, and backstopped by *"every session lists this
folder before its first unit of work"*. Getting knowledge **back out** has **no trigger, no skill and
no rule** — there is no moment in the methodology at which a session is told to *read* a KB page.
The pipeline is a funnel with no outlet, and it fails silently because a page nobody reads looks
exactly like a page nobody needed.

**Why — measured, not impressionistic.** `Observed:` 2026-08-20.

- `grep -rniE "consult the kb|read the kb|search the kb|retriev" rules/ user-rules/` returns
  **nothing about reading**. The single hit, `derivable-decision.md:195`, is about *deleting* a
  drained candidate file — the write side again.
- **65 of 76** `kb/dev/` pages name `GoalPilot`/`Android_Final_Project`, and that project's
  `AGENTS.md` — the file every session is required to read first — pointed at the KB **once**, to
  cite graphify's *policy*. Nothing in it pointed at `kb/index.md`, the lookup surface.
- The concrete cost this session: `#9` ran `connectedDebugAndroidTest`, which uninstalls the app,
  which deleted the render-pass PNG it had just written. The workaround was re-derived from scratch;
  `kb/dev/android-device-verification.md` **§8 had documented it on 2026-08-19**, five days earlier,
  from a session in the same repo.
- **The damning detail is not that the KB was not searched — it was searched twice.** Both searches
  were for a **destination to write to** (*where does this candidate belong?*), never for an **answer
  to read**. So the habit is not absent; it is **wired to the wrong verb**.

**Why it stays invisible.** Every other gap in this methodology announces itself — an unclaimed path
collides, an undrained `kb-candidates/` file is found by the next session, a stale brief fails its
`/kickoff` checks. A page that is never read produces **no signal whatsoever**: the session solves the
problem anyway, slightly slower, and reports success. The only evidence is a coincidence someone
happens to notice afterwards, which is how this one surfaced — and only because Ido asked what a
one-line footnote meant.

### 4a — the mechanism half (ingestable now)

Everything above, as a finding about the pipeline. Destination:
`kb/dev/learning-pipeline.md`, whose existing scope is *external sources → KB* — the funnel in. This
is the observation that the funnel has no outlet, so it extends that page rather than needing a new
one.

**Anchors.** The two greps above; GoalPilot `AGENTS.md` before/after `bc66295`;
`kb/dev/android-device-verification.md` §8 (2026-08-19) vs `#9`'s re-derivation (2026-08-20).

**Supersedes.** Nothing. `learning-pipeline.md` is silent on retrieval; it is not wrong about it.

**Status.** drained 2026-08-20 → `kb/dev/learning-pipeline.md` § *The funnel has no outlet*.

---

## Standing — always-ask, parked

### 4b — the `rules/` half: give retrieval a trigger, the way deposit has one

**Claim.** The fix that would actually hold is a **rule with a definite moment**, mirroring the one
that already works for deposit. The candidate wording, deliberately narrow:

> **Before your first command against a shared or expensive surface — a device, an emulator, a
> deploy, a live cloud project — read that surface's KB page.** And when a command fails in a way you
> did not predict, search the KB **before** working around it.

It has the two properties that make the deposit rule stick: a **named moment** (not *"stay alert"*),
and a **surface with one obvious page per member**.

**Why it is parked.** Destination `rules/` is **always-ask in both modes**, and it is a change to how
the agent behaves, so the 🎬 walkthrough rule owns it. Not shipped, not synced, not committed to
`rules/`.

**What the project-local fix does and does not cover.** `AGENTS.md` now points at `kb/index.md` with
three named moments (`bc66295`…, GoalPilot). That is **one repo**, and it works only if AGENTS.md is
read attentively — a pointer, not a gate. The rule would cover every repo and attach to an action
rather than to a document. **Honest limit either way:** neither is mechanical. The only mechanical
version is a `PreToolUse` hook on `adb`/`gradlew connected*`/`firebase deploy` that prints the
matching KB page, which is a bigger thing and should not be decided inside this ticket.

**Anchors.** Entry 4 above. `rules/memory-promotion.md` for the deposit-side triggers this mirrors.

**Supersedes.** Nothing.

**Status.** **parked — awaiting 🎬**, offered 2026-08-20.
