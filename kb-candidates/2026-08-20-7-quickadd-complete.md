# KB candidates — `7-quickadd-complete`, 2026-08-20

Repo: `C:\Dev\Android_Final_Project` · Session: `7-quickadd-complete` · Mode: `AUTO MODE`
Ticket: [#7](https://github.com/idomarhaim/Android_Final_Project/issues/7)

Every entry stands alone: another session's chat history is not a source.

---

## 1 · A grep cannot tell code from the comment explaining why the code is gone

**Claim.** A mechanical precondition check written as `grep <symbol> <file>` fails on a file
that *documents* the symbol's removal — and it fails **more reliably the better the removal was
documented**, because comment density is correlated with the thing being absent. Narrowing the
pattern does not fix it: the narrowed pattern is quoted in the prose too.

**Why.** `Observed:` 2026-08-20. `sessions/7-quickadd-complete.md` carried
`grep -c runTransaction …/TaskRepositoryImpl.kt` must be `0`. `50-finish` ran it, got **3**,
correctly diagnosed all three as prose, and shipped a correction in commit `6fbdea6`:
`grep -n "firestore.runTransaction" …` — *"code only — must be empty"*.

**The correction returns line 31**, which is
`` * writes in one `firestore.runTransaction`: the tick, … `` — a KDoc line. The corrected check
inherits the exact defect it was written to fix, because the file narrates the deleted
transaction in **three** separate passages (`:31`, `:42`, `:136`) precisely *because* `C20`
removed it and wanted the removal to stay removed.

The precondition was in fact **met**, established by reading `setDone` — one
`document(taskId).update(mapOf(…))`, no transaction in any form.

**What actually works:** read the function, or check the *structure* rather than the text
(e.g. that the removed API does not appear in the compiled output / import list). What does not
work is a longer regex, and the tell is that the second attempt looked obviously better.

**Direction of failure:** alarming, not flattering — both greps report the transaction as
*present* when it is absent, so the cost is a session concluding it is blocked when it is not.
The dangerous mirror image exists though: the same grep pointed at a symbol that *should* be
present would pass on a file where only a comment mentions it.

**Rejected:** filing this as an instance of `decision-map-charting.md` §12b and stopping there.
§12b is *a mechanical check is a grep frozen at authoring time* — about **staleness**. This one
was **never** correct, at any moment in time, including the moment `50-finish` wrote it after
observing the failure it was fixing. That is a different mechanism and it needs its own
sentence.

**Destination.** `kb/dev/decision-map-charting.md` — extend §12b with this as a distinct
sub-case, since that is where the brief-rot family already lives and a reader arrives there
first. Cross-link from `kb/dev/look-at-your-own-output.md`.

**Anchors.** §12b.
**Supersedes.** Nothing. Narrows §12b's scope statement by adding a neighbour, does not
contradict it.
**Status.** ready.

---

## 2 · Assertions about a capture's bytes are not assertions about its contents

**Claim.** A Compose render test that captures a PNG and asserts `file.length()`,
`bitmap.width` and `bitmap.height` proves the **file** is a plausible image and proves nothing
about **what is in it**. An unscrollable `Column` gives its overflowing children **zero
height**, so the frame silently loses everything below the fold while every artifact-level
assertion stays green.

**Why.** `Observed:` 2026-08-20 on `Pixel_10_Pro_XL`, `AlreadyDoneRenderTest`. Five states in
one `Column`; the bottom two were absent from the capture and the node reported
`(l=84.0, t=3004.0, r=429.0, b=3004.0)px` — `b == t` — so it could not even be clicked. The
three artifact assertions all passed.

**What caught it** was an assertion on the **subject**, not the artifact: *is the chip I just
tapped now selected?* That is the transferable rule — a render test's floor must assert that
**the state the frame exists to show is really in the frame**, and (for a comparison frame)
that its partner is really in the other state. Height and file size are worth keeping as a
crash-detector and worth nothing as a coverage claim.

**Why it belongs in the KB rather than in the test.** This is the founding claim of
`look-at-your-own-output.md` arriving in a new place: *the instrument degrades silently on
exactly the case it exists for*. The render-pass duty was adopted to catch what assertions
cannot see, and here the render pass itself acquired the same blind spot — with a green suite
on top of it.

**Remedy that is cheap:** split the subject into frames that fit, one per surface, and assert
per-frame. `9-duration-box`'s three-states-in-one-frame pattern is safe only while the subject
fits the viewport, which nothing in it checks.

**Destination.** `kb/dev/look-at-your-own-output.md` — new section on render captures.
**Anchors.** The existing *"rendering only tests what the artefact exhibits"* clause.
**Supersedes.** Nothing.
**Status.** ready.

---

## 3 · `adb pull` from Git Bash rewrites the device path and blames the device

**Claim.** In Git Bash (MSYS2), `adb pull /storage/emulated/0/…` has its leading-slash argument
rewritten into a Windows path before `adb` sees it. The error is
`adb: error: failed to stat remote object 'C:/Program Files/Git/storage/emulated/0/…': No such
file or directory` — which reads as *the file was never written on the device*. Fix:
`export MSYS_NO_PATHCONV=1`, or double the leading slash (`//storage/...`).

**Why.** `Observed:` 2026-08-20, pulling two render-pass PNGs off `Pixel_10_Pro_XL`. Both files
existed — `adb shell ls -la` listed them at 241 KB and 132 KB — while `adb pull` insisted they
did not. The mangled path is visible in the error message, which is the only reason this took a
minute rather than an hour: the prefix `C:/Program Files/Git/` is the tell.

**Why it is worth a line** in a page that already tells sessions to pull a PNG: the failure
appears **after** a long instrumented run, at the last step, and its message points at the
device. The natural next move is to re-run the suite, which is ~3 minutes and changes nothing.

**Destination.** `kb/dev/android-device-verification.md` §8, beside the `install -r` +
`am instrument` recipe that ends in exactly this `adb pull`.
**Anchors.** §8's code block.
**Supersedes.** Nothing.
**Status.** ready.

---

## 4 · A deferral can point at a *closed decision ticket* and read as a live owner

**Claim.** On a repo using a decision map (`C1`–`C22` here), a decision ticket is **closed when
the decision is made**, not when the code lands. A changelog that defers implementation to
*"that is `C1` #19 and not this ticket"* is therefore accurate about **whose call it was** and
silently false about **who will build it** — and the deferral propagates by copying, each copy
reading as corroboration.

**Why.** `Observed:` 2026-08-20. `#9` (`9-duration-box`, twice), `#11` (`11-fill-buttons`) and
`#7`'s own brief all defer §1.4's points inversion — `points = round(minutes/3) × difficulty`,
the `difficulty` enum, the `5..50` cap deletion, `completionFacts` — to `C1`
[#19](https://github.com/idomarhaim/Android_Final_Project/issues/19). #19 is **closed**,
`state_reason: completed`, 2026-08-10, and its closing comment is a *Resolution* carrying the
formula. Checked for another owner: **6** open issues and none is the inversion; `TODO/` lists
`C1` only as a map **question**; no brief in `sessions/` or `sessions/done/` names it.

So the model exists as a **decision with no implementation owner**, in the spec and in a shut
ticket's comment thread.

**The generalisable half:** where a repo separates *deciding* from *building*, closing the
decision ticket destroys the only signal that the build is outstanding — and every downstream
deferral keeps pointing at it, because a ticket number does not carry its state. Anything
deferring to a decision ticket should name **the build item**, or say plainly that none exists.

**Rejected:** filing an implementation issue as part of this. Creating an issue is an
outward-facing write and is Ido's to authorise; and the finding is *that nobody owns it*, which
a session quietly creating an owner would erase rather than report.

**Destination.** `kb/dev/decision-map-charting.md` — this page is about exactly this map, and
this is a property of the map's own mechanics.
**Anchors.** Whatever section covers decision-vs-build separation; new one if absent.
**Supersedes.** Nothing. **Qualifies** the three changelogs above, which stay as written —
they were true about the decision.
**Status.** ready.
