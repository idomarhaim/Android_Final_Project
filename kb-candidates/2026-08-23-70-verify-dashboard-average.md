# KB candidates — `70-verify-dashboard-average`, 2026-08-23

Each entry stands alone. No entry below depends on this session's transcript, per
`rules/memory-promotion.md`.

---

## 1. `UP-TO-DATE` replays another session's test run, and it is indistinguishable from a pass

**Claim.** In a shared working tree, a Gradle test task that reports `UP-TO-DATE` is not reporting
**your** run — it is replaying whatever the last session compiled, over **their** tree. The output is
`BUILD SUCCESSFUL`, exit code `0`, and no line anywhere in it says the tests did not execute. So the
§4p failure that a careful session refuses to risk by *waiting for the daemon* arrives anyway, one
step later, wearing a green tick.

`Observed:` 2026-08-23, GoalPilot `#70`. This session existed **because** `f25cca5` had refused to
run its own verification while `68-drag-to-move` was mid-build — the refusal was correct and
explicitly cited §4p. Its first `:app:testDebugUnitTest` returned **`BUILD SUCCESSFUL in 4s`**, every
task `UP-TO-DATE`. The 88 result XMLs were all stamped **16:40:45**, before the session opened, and
held **1068** tests where the brief predicted **1018** — the 50-test gap being `68-drag-to-move`'s
calendar tests. Re-run with `--rerun-tasks`: **2 m 21 s**, same 1068, genuinely executed.

**Why it matters more than an ordinary stale-cache note.** The two cheap tells point the wrong way.
**Duration** is the honest one and it is *inverted* — 4 s reads as *fast*, and the session that has
been waiting on a busy daemon is primed to be relieved by it. **The test count** is the tell that
looks like corroboration: it disagreed with the brief in the direction of *more* tests, which reads
as *the suite grew*, not *these are not my results*. Nothing in Gradle's own output distinguishes
"nothing changed since **you** last ran" from "nothing changed since **someone else** last ran".

**The check, and it is two commands, not a habit.** Read the result files, never the verdict:

```bash
ls -l --time-style=full-iso app/build/test-results/testDebugUnitTest/*.xml | head -1   # whose run?
grep -c UP-TO-DATE <build log>                                                          # did it run?
```

A timestamp older than your session is the whole diagnosis. Then `--rerun-tasks`.

**Rejected:** *"just always pass `--rerun-tasks`"* — it forces a full recompile on every invocation
and would have cost this session ~2 minutes on each of four builds; the point is to **know which
kind of green you are holding**, not to buy the expensive one blindly. Also rejected: *"check the
daemon is idle first"* — that is what the previous session did, correctly, and it does not help,
because the cache outlives the build that filled it.

- **Destination:** `kb/dev/look-at-your-own-output.md`, as a widening of **§4p** (the section about
  running in a tree that is not yours) — it is the same failure with the daemon *released*, which
  §4p as written does not reach. §4k's *print the count beside the verdict* is the habit that caught
  it, so cross-link rather than duplicate.
- **Anchors:** `kb/dev/look-at-your-own-output.md` §4p, §4k
- **Supersedes:** nothing. §4p's claim is unchanged and correct; this is a case it does not cover.
- **Status:** ready.

---

## 2. A layout assertion on one axis is blind in the direction of the defect it was written for

**Claim.** When a composable is swapped for a differently-sized one inside a `Row` or `Column`, an
assertion on the container's **cross-axis size** can pass over exactly the bug it exists to catch —
because if the replacement comes out **smaller**, a sibling becomes the tallest child and the
container does not move at all. The assertion has a blind spot pointing at the failure.

`Observed:` 2026-08-23, GoalPilot `#70`. `ProgressRing(size = 92.dp)` was replaced by
`UnmeasuredMarker(size = 56.dp, modifier = Modifier.padding(18.dp))`, and the whole risk was whether
`56 + 18 + 18` really measures 92 — which it does **only** because `UnmeasuredMarker` composes
`modifier.size(size)`, putting the caller's padding outside the box. Written
`Modifier.size(size).then(modifier)` — same numbers, one order apart, in a **different file** — the
node is 56 dp. The card-height assertion would then have reported **green**, because at 56 dp the
marker is shorter than the text column beside it and the `Row`'s height is set by the text.

The check that does work is the **main-axis** one: the left edge of the first sibling *after* the
swapped node is where that node's width ends, so comparing it across states pins the box directly.
No test tag is needed on the node itself — an existing text node one position over is the probe.

**Why.** The generalisable part is not about Compose. It is: *when your assertion measures a
`max()`, it can only observe your subject growing.* Ask which direction the defect moves the number,
and if the answer is "smaller, and something else takes over", the axis is wrong.

`Observed:` also the reason this was found at all — the render PNG made the marker *look*
horizontally offset (a progress arc is open on the left, so its **ink** is off-centre while its
**box** is not). The eye was wrong about the offset and right that the horizontal axis was untested.

- **Destination:** `kb/dev/android-device-verification.md`, near the render-pass material — it is
  advice about what to assert when a render pass replaces one composable with another.
- **Anchors:** `kb/dev/android-device-verification.md`, `kb/dev/look-at-your-own-output.md`
  (*check the instrument on the hardest input it exists for* — the same shape: an instrument that
  cannot fail on its own motivating case)
- **Supersedes:** nothing.
- **Status:** ready.

---

## 3. A tolerance that had to be justified, not just widened

**Claim.** `getUnclippedBoundsInRoot()` returns `Dp` converted from pixels, so two nodes that are
laid out identically can differ in the last float digit. An exact-equality assertion on them fails.
The fix is a tolerance, and the tolerance is only honest if the write-up states **both** ratios: how
much smaller it is than the defect being hunted, and how much larger than the observed noise.

`Observed:` 2026-08-23, GoalPilot `#70` — `236.33334 dp` against `236.33331 dp`, a gap of
**0.00003 dp**. The defect being hunted was **36 dp**. The chosen `0.05 dp` is 700× under the defect
and 1600× over the noise, so it cannot hide the one or fire on the other.

**Why it is worth a line at all.** *Widen the tolerance until it passes* is the default move and it
silently converts a geometry test into a smoke test; nobody reviewing it later can tell which
happened. Writing the two ratios into the test's own comment is what makes the difference legible
without re-deriving it.

- **Destination:** merge into `kb/dev/look-at-your-own-output.md`'s existing tolerance/instrument
  material — **not** a new section. Thin on its own.
- **Anchors:** `kb/dev/look-at-your-own-output.md`
- **Supersedes:** nothing.
- **Status:** ready, **low value** — worth one paragraph, not a page. Merge it or drop it; do not
  create a section for it.
