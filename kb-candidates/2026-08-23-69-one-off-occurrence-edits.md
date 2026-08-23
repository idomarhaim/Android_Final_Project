# KB candidates — `69-one-off-occurrence-edits`, 2026-08-23

Each entry stands alone. No entry below depends on this session's transcript, per
`rules/memory-promotion.md`.

⚠️ **Written at close-out rather than at the commit trigger, which is late.** Under `AUTO MODE` the
drain fires when a commit-worthy unit is done, and this session's unit committed (`49e1bde`,
`afb84bb`) with these findings recorded only in its changelog and its board release note. The file
is written now so nothing is lost — each entry stands alone, which is the format's whole point —
and it is added to `sessions/kb-drain-67-and-siblings.md`'s roster rather than drained here, on
`67-delete-anything`'s own precedent: the drain is a different repo, a different board and a
different unit, and that session wrote the brief for exactly this reason.

**Entry 1 extends a candidate already queued** — `2026-08-23-70-verify-dashboard-average.md` entry 1.
They want **merging**, not two sections. Whoever drains reads both together.

---

## 1. `UP-TO-DATE` replays another session's build at the **APK**, not only at the test task

**Claim.** The stale-green failure `#70` recorded for `:app:testDebugUnitTest` reaches
`:app:assembleDebugAndroidTest` **identically**, and there it is worse: what gets replayed is not a
test result but an **artifact you then install and run**. `adb install -r` succeeds, `am instrument`
runs, and the suite reports about a build made by **another session over its own tree**. Nothing in
Gradle's output, in `adb`'s output, or in the instrumentation summary says so.

`Observed:` 2026-08-23, GoalPilot `#69`. `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`
returned **`BUILD SUCCESSFUL in 5s`**, `74 actionable tasks: 74 up-to-date`. The APKs on disk were
stamped **20:05:53** and **19:55:39** — the androidTest one being `67-delete-anything`'s build, made
while its two scratch test classes (`DeleteAnythingUiTest.kt`, `TmpDeleteDump.kt`) were still
untracked in the shared tree. Re-run with `--rerun-tasks`: `74 executed`, 3 m 21 s, both APKs stamped
`20:33`.

**The confirming measurement, and it is the part worth copying.** The androidTest APK **shrank from
1,665,402 to 1,531,663 bytes**. That byte drop is the sibling's two classes leaving the artifact, and
it is *independent* evidence that the first build was **theirs** rather than merely stale — a
timestamp alone cannot tell "old build of my tree" from "recent build of someone else's". Nothing
else in the toolchain distinguishes those two, and they need opposite responses.

**Why it is not just "the same bug one task over".** The unit-test case fails **inside** Gradle, so
`--rerun-tasks` on the same invocation fixes it. The APK case fails **across a process boundary**:
the build is one command, the install is a second, the run a third, and only the first has any
notion of up-to-date-ness at all. So the habit that saves you is different — it is *check the
artifact's mtime and size before installing it*, not *check the build log*.

**Rejected:** *"always `--rerun-tasks` before a device run"* — same objection `#70` raised and it
holds here at ~3.5 minutes a time; the point is to know which kind of green you hold. Also rejected:
*"clean first"* — a full clean is far more expensive and destroys other sessions' incremental state
in a shared tree.

- **Destination:** `kb/dev/look-at-your-own-output.md` — **merged into the widening of §4p that
  `2026-08-23-70-verify-dashboard-average.md` entry 1 already proposes**, as its second half. One
  section covering both tasks, not two.
- **Anchors:** `kb/dev/look-at-your-own-output.md` §4p, §4k · `kb/dev/android-device-verification.md`
  §8 (the `adb install -r` + `am instrument` path this was found on)
- **Supersedes:** nothing. **Extends** the queued `#70` entry 1, which is correct as written and does
  not reach the APK.
- **Status:** ready.

---

## 2. A probe that greps a *section* instead of its *table rows* can never fire, and it fails silently

**Claim.** When a structured region (a Markdown table) lives inside a prose region (a `##` section),
a check written against the **section** matches the prose and reports the structure's contents
wrongly. Where that check gates a **background wait**, the wait never fires and produces **no
output at all** — which is indistinguishable from a wait that is patiently working.

`Observed:` 2026-08-23, GoalPilot. A background wait armed on *"has session `67-delete-anything`
released its board row?"* counted the label with
`awk '/^## 🔒 Active claims/{f=1;next} /^## /{f=0} f' SESSIONS.md | grep -c '67-delete-anything'`.
That returned **5** and never moved: `SESSIONS.md`'s Active-claims section carries **4,500+ lines of
release-note prose** below its table, and the label appears there. The sibling had released **half an
hour** earlier. The correct probe restricts to the table's own rows — `grep '^| '` first, then match
the label within those.

**Why it is more than a shell typo.** Three things compound, and each is general:

1. **The board's own known pathology is the input.** GoalPilot's Active-claims section holds one live
   row and thousands of lines of prose; JARVIS's held **1,778 lines and zero rows**. The rule that
   already exists — *count mechanically with `grep -c '^| '`* — is written for a **human reading**
   the board. Nobody had applied it to a **machine polling** the board, and the polling case is the
   one that fails silently.
2. **A never-firing wait is worse than no wait.** A wait that cannot succeed emits nothing, so it is
   indistinguishable from one that is working, right up to its timeout — and its timeout was 45
   minutes away. The session sat idle believing it was blocked. **A human noticed before the
   instrument did.**
3. **It was the third instrument error of one family in one session**, after a board read that
   reported zero live rows (a sibling's row landed between read and write) and a "is the daemon
   busy?" probe that counted **idle** daemons' registry housekeeping as work. All three read a
   *proxy* for the thing rather than the thing.

**The generalisable rule:** a condition that gates a wait must be **provably reachable** — assert the
probe returns the "clear" value against a hand-made clear input **before** arming it. One line, and
it converts a silent 45-minute stall into an immediate error.

**Rejected:** *"parse the table properly"* — an over-fix; `grep '^| '` is sufficient and is what the
existing rule already prescribes for the reading case. Also rejected: *"shorten the timeout so it
fails faster"* — that trades a long silent stall for a short one and still never answers the
question.

- **Destination:** `kb/dev/look-at-your-own-output.md` — it is an *instrument* failure, which is that
  page's subject, and specifically the **silent-failure** family. Cross-link to
  `rules/agent-topology-and-model-routing.md` §5's mechanical-count clause rather than restating it.
- **Anchors:** `kb/dev/look-at-your-own-output.md` · `rules/agent-topology-and-model-routing.md` §5,
  §5.2 (the background wait this gated)
- **Supersedes:** nothing. The existing *count the rows mechanically* clause is correct; this is the
  case it does not cover, because it addresses a reader and not a poller.
- **Status:** ready.

---

## 3. `./gradlew --status` is the instrument for "is the daemon busy" — log mtimes are not

**Claim.** Deciding whether another session is mid-build by looking at `~/.gradle/daemon/*/daemon-*.out.log`
modification times reports **idle** daemons as busy: an idle daemon writes to its own log
continuously, polling the daemon-addresses registry. `./gradlew --status` prints `IDLE` / `BUSY` /
`STOPPED` per PID, takes the project lock nowhere, and answers the question directly.

`Observed:` 2026-08-23, GoalPilot `#69`. A wait armed on *"no daemon log touched in the last 120 s"*
timed out after **45 minutes** reporting `busy=4`. Run at that moment, `./gradlew --status` showed
every daemon `IDLE`; the four "busy" logs were two shutting down and two writing
`Waiting to acquire shared lock on daemon addresses registry` / `Lock acquired` / `Releasing lock`.
The 45 minutes bought nothing.

**Why.** Thin on its own and recorded for the shape: the mtime probe is a **proxy** chosen because
it needs no tooling, and it fails in the **conservative-looking** direction — it says *busy*, which
reads as caution rather than as error, so nobody questions it. Same family as entry 2 and worth
merging with it if the drainer prefers one section.

- **Destination:** `kb/dev/look-at-your-own-output.md`, merged with entry 2 or as a short worked
  example beneath it — **not** its own page.
- **Anchors:** `kb/dev/look-at-your-own-output.md` · `AGENTS.md` § *Singletons in this repo*
- **Supersedes:** nothing.
- **Status:** ready, **low value on its own**. Merge it or drop it.
