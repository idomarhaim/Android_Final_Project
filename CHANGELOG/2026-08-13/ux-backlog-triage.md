# ux-backlog-triage — #6–#11 are not parallelisable, and two of them were lying about why

**Session:** `ux-backlog-triage` · **Date:** 2026-08-13 · **Mode:** normal (interactive)
**Branch:** `feat/goalpilot-implementation` · **Tickets:** [#10](https://github.com/idomarhaim/Android_Final_Project/issues/10), [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11) — titles only, neither resolved

## The question

*Can `#6`–`#11` be run as parallel sessions right now, given that none of them carries a
`blocked` label?*

## The answer, in three independent layers

Three reasons, and they fail separately — clearing the blocks would still leave the other two.

1. **Wrong entry point.** `#6`–`#11` are not `/wayfinder` tickets. The 2026-08-06 brief was split
   at the ceremony boundary: undecided product questions became `C1`–`C18` under map `#12`
   (children `#13`–`#39`), and the ordinary build work became `#2`–`#11`. `/wayfinder` charts
   decisions and has nothing to chart here; these want build sessions, as `#3` got.
2. **Two of the six were genuinely gated at the time of asking** — `#7` on `C3` `#18` for
   points/progress consistency, `#9`'s precedence rule on `C1` `#19` — and `#6`+`#8` are one unit
   by their own text (*"ship them together, or ship this one first"*), since silent filing is
   unsafe until the notification substrate exists.
3. **The working sets are not disjoint, which is what actually decides it.** Grepped, not
   assumed: `DashboardScreen.kt` + `DashboardViewModel.kt` are contested by **four of the six**
   (`SmartAddCard` at `:486`, `SmartAddDialog` at `:529`, the `minutes` field at
   `DashboardViewModel.kt:232`); `GoalDetailScreen.kt` by two (`AddTaskRow` at `:315` and the
   fill-button row); the manifest and `build.gradle.kts` by two. And beneath all of it the board
   already records the decisive constraint: **one working tree, one `app/build/`, one Gradle
   daemon** — two build sessions would each compile an APK containing the other's uncommitted
   edits. This is what separates `#6`–`#11` from the map's HITL tickets, which take no singleton.

## The defect this surfaced

Two titles asserted blocks that had already cleared:

| Ticket | Stale title suffix | Reality |
|---|---|---|
| `#11` | *— blocked on the C7 unit decision* | `C7` `#14` closed 3 days earlier; the issue's **own comment** is headed *"Unblocked"* |
| `#10` | *— blocked on the C12 presentation decision* | `C12` `#31`'s resolution comment ends *"Unblocks #10."* |

**Both suffixes removed** (`gh issue edit`, titles only — no body, comment, label or dependency
edge touched). Nothing else in the repo was written.

**The cause is structural, not sloppiness.** `blocked_by` is **`[]` for both** — GitHub's
dependency graph never held these edges. `#12`'s children *do* use it, which is why five
released sessions could each re-derive the frontier from the API and stay honest. `#2`–`#11`
encode blocks in prose that nothing ever reads back, so no re-derivation can contradict them.
In both cases the unblocking session **did** report correctly — in a comment on the blocked
ticket, directly beneath a title still saying the opposite. Flagged as a KB candidate.

## Verification

No code changed, so no test layer applies. Verified by re-query: both titles re-read after the
edit, `#6`–`#9` unchanged, no block suffix remains on any of the six.

## Session discipline

**No `SESSIONS.md` row.** Two mechanical title edits do not earn a claim, and writing one would
have meant editing the board — the one file the two live sessions (`c6-log-progress` on `#22`,
`c2-task-type` on `#20`) are most likely to be touching. Neither owns `#10` or `#11`. No
singleton taken: no build, no device, no Firebase, no git index until this commit, which stages
**explicit paths only**.

## 📥 KB candidates

One, pending Ido's word (normal mode): *one relation encoded in two places rots in the half
nothing reads* → `kb-candidates/2026-08-13-ux-backlog-triage.md`.
