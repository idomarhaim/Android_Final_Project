# KB candidates — `67-delete-anything`, 2026-08-23

Each entry stands alone. No entry below depends on this session's transcript, per
`rules/memory-promotion.md`.

⚠️ **PARTLY DRAINED 2026-08-24 by session `kb-drain-67-and-siblings`.** Three of the four entries
are ingested; the file is rewritten down to its one survivor and **not** deleted, with the original
numbering kept. What happened to the other three:

- **№1** (a bidi isolate splits every substring matcher that spans a number) — **INGESTED** into
  `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` as **new §5.6** (JARVIS `64fc76f`).
  ⚠️ **Its proposed destination was wrong and this is the useful part.** The entry asked for *"a new
  page, or a section on an existing Compose-testing page if one exists"*. **§5.4 of that page already
  held the identical mechanism** — same helper (`bidiIsolated`), same U+2068/U+2069, same app,
  written up two tickets earlier from `#65`. What survived reconciliation is the half §5.4 does not
  have: §5.4's remedy (assert the invariant frame, drop the number) is **insufficient when the number
  *is* the subject**, and the fix is to repair the **matcher** — a `SemanticsMatcher` that strips
  isolates from the *actual* — which is the exit from the trap loop §5.5 describes. Plus the scale
  (8 of 15) and the `Bidi.strip`-already-existed observation.
- **№2** (`${PIPESTATUS[0]}` caught a failed build that then reported green) — **INGESTED** as an
  `Observed:` line under **§4c-iii** of the same page, exactly as the entry asked. It is a dated
  corroboration in a **second shape**: the standing note names the `grep` case, this was a plain
  `tail`.
- **№3** (deleting a document does not delete its subcollections) — **INGESTED** into
  `C:\Dev\JARVIS\kb\dev\firestore-write-semantics.md` as **new §10**, with the general form (*an
  orphan is invisible exactly when the read path is a join or a fan-out keyed on the parent*) and the
  symptom-free detection procedure. `## 10. Adjacent` renumbered to `## 11`.

`Check-KbLinks` **CLEAN** (117 pages). Journal entry — and the candidate→page tie, since the pages
are in another repo: `C:\Dev\JARVIS\kb\log\2026-08-24.md`.

---

## Standing — always-ask

## 4. A flat list of consequences invites an addition the design does not intend

**Claim.** Where a confirm names several quantities and one of them is a **subset** of another,
drawing them as equal peers invites the reader to total them. No assertion can see it: every count
is individually correct and every matcher passes.

`Observed:` 2026-08-23, GoalPilot `#67`, with all 15 instrumented tests green —
`This task. / 12 scheduled occurrences. / Including 5 that already happened. / The 40 points it
earned.` The third line is 5 **of** the 12 and reads as a fourth item. Found by looking at the PNG.

**The fix that was taken and the one that was not.** Subordination — smaller type, secondary colour,
indent in the layout direction — rather than folding the two counts into one sentence, because a
single *"12 occurrences, 5 of which already happened"* needs two interacting plural forms per
language, and Hebrew has four plural categories to English's two.

**Why:** thin on its own — it is one instance of *look at the render*, which the KB already argues
at length. Flagged rather than promoted; it may be worth one line as an example under
`look-at-your-own-output.md`'s visual-acceptance section, and it may be worth nothing.
**Destination:** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md`, or dropped.
**Anchors:** `app/src/main/java/com/idomarhaim/goalpilot/ui/components/DeleteConfirm.kt`
(`Consequence`) · `docs/render-passes/2026-08-23-67-delete-anything/issue-67-confirm-task-light.png`
**Supersedes:** nothing.
**Status:** **always-ask, still open.** Held 2026-08-24 by `kb-drain-67-and-siblings`, which drained
the other three. Its author marked it *ask before promoting*, and
`sessions/done/kb-drain-67-and-siblings.md` records the decision as already taken and not to be
reopened: **one line as an example, or nothing — do not write it a page.** The only question left for
Ido is which of those two. Asked in that session's final reply; not dropped, so a session that dies
here loses nothing.
