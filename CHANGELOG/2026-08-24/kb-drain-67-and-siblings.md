# `kb-drain-67-and-siblings` — 2026-08-24

Drained this repo's `kb-candidates/` folder into the central KB at `C:\Dev\JARVIS\kb\`. **Four**
files, not the three the brief names — `69-one-off-occurrence-edits` wrote its own after the brief
was written and added itself to the roster.

**This repo holds the candidates; the pages are in another repo**, so no single commit spans both.
The tie is the journal entry, [`C:\Dev\JARVIS\kb\log\2026-08-24.md`](file:///C:/Dev/JARVIS/kb/log/2026-08-24.md),
which names each source file **with its repo**. Pages committed as JARVIS `64fc76f`.

## 📥 Ingested — 10 of 11 entries

| Source entry | Landed as |
|---|---|
| `#67` 1 — a bidi isolate splits every substring matcher spanning a number | `kb/dev/look-at-your-own-output.md` **new §5.6** |
| `#67` 2 — `${PIPESTATUS[0]}` caught a failed build that then went green | same page, **§4c-iii** `Observed:` line |
| `#67` 3 — deleting a document does not delete its subcollections | `kb/dev/firestore-write-semantics.md` **new §10** |
| `#68` №4 — clamp before you snap | `kb/dev/look-at-your-own-output.md` **new §4-i** |
| `#69` 1 — `UP-TO-DATE` replays a sibling's **APK** | same page, **new §4c-iv** |
| `#69` 2 — a probe that greps a section, not its table rows | same page, **new §4q** |
| `#69` 3 — `./gradlew --status`, not log mtimes | same page, **merged into §4q** |
| `#70` 1 — `UP-TO-DATE` replays a sibling's **test run** | same page, **§4c-ii widened** |
| `#70` 2 — a cross-axis assertion is blind toward its own defect | `kb/dev/android-device-verification.md` **new §7b** |
| `#70` 3 — a tolerance needs both ratios stated | `kb/dev/look-at-your-own-output.md` **new §4-ii** |

## 🗂️ Candidate files after the drain

| File | Outcome |
|---|---|
| `2026-08-23-67-delete-anything.md` | **rewritten down to entry 4**, kept. Not deleted — a partial drain never is |
| `2026-08-23-68-drag-to-move.md` | fully drained → **deleted** |
| `2026-08-23-69-one-off-occurrence-edits.md` | fully drained → **deleted** |
| `2026-08-23-70-verify-dashboard-average.md` | fully drained → **deleted** |

The three deletions ride this commit under `rules/derivable-decision.md` §1 — every entry in each was
promoted, which is the one deletion that does not need asking.

## ⏸️ Held — `#67` entry 4, always-ask

*A flat list of consequences invites an addition the design does not intend.* Its author marked it
*ask before promoting*; the brief confirms the decision as already taken — **one line as an example
under the visual-acceptance material, or nothing; do not write it a page.** The only open question is
which of the two, and it is Ido's. It stays in the candidate file under a `## Standing — always-ask`
heading, so nothing is lost if this session ends first.

## 🔎 What the drain found that no candidate contained

**No new page was created — every destination already existed**, and three entries proposed one that
was wrong:

- `#69` 1 and `#70` 1 both named §4p. The phenomenon was already at **§4c-ii**, same repo, same week.
- `#67` 1 asked for a new page *"or a section on an existing Compose-testing page if one exists"*.
  **§5.4 had the identical mechanism** — same helper, same two code points — written two tickets
  earlier from `#65`.

None of the four files carried a **bundle check**, which per `/kb-ingest` §3 is the *missing* case
rather than an explicit `not checked`. The clock that puts on this bundle: a mechanism written up on
**2026-08-23** was re-derived from scratch by a sibling **the same day**, at the cost of a device
round trip and a semantics dump.

## 🧪 Tests

**No code changed in this repo** — this unit edits `kb-candidates/`, the changelog and the board, so
**no test layer applies and none was run.** Stated rather than skipped silently. No build ran and no
device was used; `tour-refresh` holds the Gradle daemon and the emulator and neither was needed.

What ran instead, in the bundle's repo:

| Check | Result |
|---|---|
| `Check-KbLinks.ps1 -BundlePath C:\Dev\JARVIS\kb` | **CLEAN** — 117 pages, 0 broken links, 0 orphans, 0 wikilinks |
| Re-ran §4q's published probe against this repo's live board | **it was wrong** — see below |
| Re-read each published code block against the file on disk | 1 defect found and fixed (a collapsed line continuation) |

### The snippet §4q was about to ship was wrong, and running it is what said so

§4q's own remedy is *assert the probe against a clear input before arming it*. Applied to the fix
`#69` entry 2 prescribed, it failed — measured on **this repo's** `SESSIONS.md`, 2026-08-24:

```
section-wide count of a RELEASED label   : 5    <- the bug the candidate reported
table-rows-only count of the same label  : 1    <- the candidate's prescribed fix, STILL WRONG
Session-column-only count                : 0    <- correct (and 1 for the live row, as it should be)
```

The surviving `1` was **this session's own row**, whose `Owns (paths)` column lists the file
`kb-candidates/2026-08-23-67-delete-anything.md`. A released session's label reappears inside a
different, **live** row as a **path**, and row-filtering cannot remove it. Written into §4q as a
measured residual, with the reason the candidate's synthetic clear input would have passed: only the
real board has labels in three columns, one of them holding filenames.

## ⚠️ Deviation from the brief

The brief required `#69` 1 and `#70` 1 to be drained as **ONE** section. They were written as
**two** — §4c-ii widened, plus a new §4c-iv.

The brief's premise was that both halves were new. **The cheaper half was already committed as
§4c-ii** before the brief was written, so merging meant duplicating a standing section or rewriting
it — and rewriting a standing claim is a supersession, which is always-ask, over a claim that is
correct.

The brief's real concern — *a reader who stops at the cheap half leaves believing `--rerun-tasks` is
the whole remedy* — is answered mechanically instead: §4c-ii now closes on a ⚠️ forward pointer that
names §4c-iv and says in terms that the artefact case needs a different habit. §4c-iv then opens by
saying why **neither** neighbouring remedy fires. Cheap half first, expensive half unavoidable from
it; only the section count differs from what the brief asked.

## 🧭 Sibling

`tour-refresh` claimed a row on this board **after** this session claimed its own, and five files
under `ui/tutorial/`, `res/values*/tutorial_strings.xml` and `ui/root/GoalPilotRoot.kt` were dirty in
the tree by the time this commit was prepared. All five are on **its** `owns:` list; none was
touched, and every commit here names explicit paths.

## Files

- `kb-candidates/2026-08-23-67-delete-anything.md` — rewritten to its one always-ask survivor
- `kb-candidates/2026-08-23-68-drag-to-move.md` *(deleted)*
- `kb-candidates/2026-08-23-69-one-off-occurrence-edits.md` *(deleted)*
- `kb-candidates/2026-08-23-70-verify-dashboard-average.md` *(deleted)*
- `CHANGELOG/2026-08-24/kb-drain-67-and-siblings.md` *(new)*
- `SESSIONS.md` — row claimed, then released
- `sessions/kb-drain-67-and-siblings.md` → `sessions/done/` with `status: done`

---

# Follow-on — the held entry, 2026-08-24

`#67` entry 4 is **no longer held.** Ido was asked which of *one line as an example* or *nothing* it
should get and answered **"choose the best solution for the system"** — a **delegation**. Under
`rules/question-axis-naming.md` that removes the judgment half to me, forbids re-asking, and requires
re-opening the problem rather than breaking my own tie, **because the delegated answer is often not
one of the options offered.** It was not.

## The decision, and it is mine to overturn

**Two short additions, in two places, neither of them what was proposed.** The candidate and the
brief both classified this as *one instance of look at the render*, destined for
`look-at-your-own-output.md`'s visual-acceptance material. **That classification was the error**, and
every option built on it was mediocre for the same reason.

The entry's own words are *"every count is individually correct and every matcher passes"* — the
defect is in a **relation between two lines**, not in any line. That is verbatim the claim of **§4e**
of the same page (*"every assertion is correct, and the defect is between them"*). Filed under
visual-acceptance it restates a principle the page already argues at length; filed under §4e it is a
**fourth instance and a new kind**.

| Half | Landed as |
|---|---|
| The **instrument** half — a subset drawn as a peer is unassertable | `kb/dev/look-at-your-own-output.md` **§4e widened** |
| The **design** half — why subordination beat one sentence | `kb/dev/untranslatable-idioms.md` **new §*Two interacting plurals*** |

**§4e widened.** The first instance in that section where the relation is **arithmetic containment**
rather than visual resemblance — so §4e's operational question (*"what else in this frame looks like
what I just added?"*) **would not have caught it**; nothing here looks like anything else. The added
question is *"which of these numbers is inside another one?"*, and the general form covers both:
**a flat list asserts that its items are disjoint, and that assertion is made by the layout, never by
any element in it.**

**The design half had no classification at all**, from anyone. The fix was subordination rather than
folding both counts into one sentence **because** *"12 occurrences, 5 of which already happened"*
needs two **interacting** plural forms per language — Hebrew has four plural categories to English's
two, a 4 × 4 matrix where English reads 2 × 2. Generalised: **where a relationship between two
quantities can be expressed in grammar or in layout, layout is the one that does not multiply per
language.** It sits beneath that page's existing one-count `<plurals>` remedy, which says nothing
about two. Cross-linked both ways, with the honest caveat that choosing layout over grammar trades a
**translation** cost for a **verification** cost — it does not remove the cost.

## Result

- **11 of 11 entries now ingested**, 0 held.
- `kb-candidates/2026-08-23-67-delete-anything.md` was down to this one survivor, so it is **fully
  drained and deleted**. The folder is empty.
- `Check-KbLinks` **CLEAN** (117 pages).
- **No test layer applies** — documentation and a candidate file. No build, no device.
