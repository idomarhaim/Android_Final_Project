# KB candidates — `55-scoring-model`, 2026-08-21

Session `55-scoring-model` · ticket [#55](https://github.com/idomarhaim/Android_Final_Project/issues/55) ·
mode **AUTO** · account: `CHANGELOG/2026-08-21/55-scoring-model.md`

**Drained 2026-08-21.** Entries 1–4 were ingested into the JARVIS central bundle under AUTO
MODE — journal entry: `C:\Dev\JARVIS\kb\log\2026-08-21.md`. They are **not** reproduced here;
the pages are the record now.

| # | Topic | Landed in |
|---|---|---|
| 1 | a migration needs a grep for every **reader**, not every writer | `kb/dev/schema-migration-readers.md` §1 *(new page)* |
| 2 | deploy order is part of a migration's design, and observable before shipping | `kb/dev/schema-migration-readers.md` §2 |
| 3 | unrepresentability deletes a normaliser's tests — and the sentinel you add back is a new bad state | `kb/dev/unrepresentable-invariants.md` *(new page)* |
| 4 | a cross-language fixture is worth the thinness of its adapters | `kb/dev/mechanism-vs-compliance.md` §9, §9.1 |

**This file stays** because entry 5 below is always-ask and did not drain. It is deleted only
when nothing is left in it.

---

## Standing — always-ask

## 5 · `rules/` candidate — a device pass on live data needs a stated blast radius first

- **Claim.** Before the first tap of a device pass **against a live account**, the session
  states in one line what it will create, what it will touch, and how it will restore — and
  it does not tap anything it did not name. A tap is a write, and on a shared surface an
  unaimed one is somebody else's data.
- **Why.** `Observed:` 2026-08-21. The plan was: create a probe task, complete it, delete it.
  What happened: a `keyevent 4` navigated somewhere unintended, a tap at coordinates computed
  for the previous screen landed on a **different goal's existing task**, and the next two taps
  were a misdiagnosis and its correction — an untick and a re-tick of one of Ido's real
  completed tasks. Nothing was lost and the state was restored and verified, but the account
  now carries a task migrated a day earlier than it would have been, and the recovery took
  three screenshots to reason about because the *baseline was never written down*.
  The cheap remedy is not "be careful with coordinates" — it is naming the blast radius, which
  makes an off-plan tap **visible as off-plan at the moment it happens** rather than three
  screenshots later. It also produces the baseline the restore has to be checked against; here
  the pre-session dashboard screenshot was the only reason *"70 pts, 5 tasks done, goal at 1%"*
  could be restored and confirmed at all.
  *Rejected:* requiring an emulator with a throwaway account for every device pass. The
  signed-in live account is precisely what several exit criteria need
  (`kb/dev/android-device-verification.md` §8 exists to preserve it), and a rule that makes the
  honest check expensive gets skipped.
- **Destination.** **`rules/`** — always-ask in both modes; the 🎬 walkthrough rule owns it.
- **Anchors.** `CHANGELOG/2026-08-21/55-scoring-model.md` §4.2 *(Live data touched)*.
- **Supersedes.** Nothing; adjacent to `kb/dev/android-device-verification.md` §8.
- **Status.** **BLOCKED — destination `rules/`, always-ask in both modes, AUTO included.**
  Parked 2026-08-21 by session `55-scoring-model` at its own drain; awaiting Ido's ruling. The
  other four entries in this file drained the same day, so this one is the only reason the file
  still exists.
