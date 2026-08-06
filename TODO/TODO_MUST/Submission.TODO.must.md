# Submission — MUST backlog

The two items that block handing the project in. Everything else in spec §6 Core
is built and verified against the live backend (`CHANGELOG/2026-07-31.md`).

Both are **largely manual**. An agent can boot the emulator, install, and verify
the results in Firestore afterwards — it cannot sign into Google as a second
person, and it cannot supply your name, ID or course number.

---

## [x] 1. Two-account sharing demo (spec §7) — **done 05/08/2026**

Spec §7 requires the social layer demonstrated with **two real users**. Done and
captured on 05/08/2026 with both emulators up: friends-only leaderboard showing
both accounts, the shared feed item, and — on top of what this item originally
asked for — a challenge created by A, **joined by B as a non-owner**, scored by
B, and the owner's screen re-ranking itself live. See
`CHANGELOG/2026-08-05/submission.md`.

> ⚠️ This item's original premise was **wrong**, and it cost a session's worth of
> planning. It claimed *"Only one account has ever signed in"* and listed the
> friend-code exchange as work still to do. Live Firestore had held **two**
> profiles (`NDXVJC` and `8ZFFSM`), friend edges in **both** directions, and a
> share by account A **since 02/08/2026**. The demo needed capturing, not
> building. Check the live project before believing a backlog item about it.

**Already in place — do not redo:**

| | |
|---|---|
| Account A | `name.iddo@gmail.com` — signed in, friend code **`NDXVJC`** |
| Account B | `rachil751@gmail.com` — **already an OAuth test user** |
| Publishing status | **Testing** (leave it there — production hard-blocks sensitive scopes) |

> ⚠️ An earlier note in this backlog claimed the second account "still needs
> adding" and that the project owner is implicitly allowed as a tester. **Both
> were wrong.** Both accounts are on the Test users list, and the
> `Ineligible accounts not added` dialog you get when re-adding one means
> *already on the list* — a duplicate rejection, not a permissions failure.

**Steps**

1. Sign in as **account B** — on the second AVD, which exists as of 05/08/2026:

   ```powershell
   .\scripts\run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B    # or the desktop shortcut
   ```

   It boots alongside account A's emulator rather than replacing it, so both
   screens stay visible for the demo. Expect **one** ANR while it settles (host
   RAM, not the AVD) — tap *Wait*. Details in
   [`scripts/README.md`](../../scripts/README.md).

   The old route — sign out on the single device and back in as B — still works,
   but then **kill and relaunch the app between accounts**:
   Firestore listeners are per-process and a stale one will serve account A's
   data (`data/auth/AuthExt.kt` fixed the Flow-construction bug, but a warm
   process is still the riskier demo).
2. As B: create a goal, add a task, complete it — B needs points, or the
   leaderboard has nothing to rank.
3. As B: Profile → note B's friend code. Add A's code **`NDXVJC`**.
4. As A: add B's code back. Friendship is stored as two one-way edges
   (`users/{uid}/friends/{friendUid}`), so **both directions must be added** or
   the friends-only leaderboard looks broken from one side.
5. As either: Social tab → toggle **Friends only** → both users should appear,
   ranked by points.
6. As either: Dashboard → **Share your weekly progress** (optionally with a
   photo) → confirm the item lands in the other account's feed.

**How to verify it actually worked** (the UI alone is not proof):

```bash
T=$(gcloud auth print-access-token)
B=https://firestore.googleapis.com/v1/projects/goalpilot-56e30/databases/(default)/documents
curl -s -H "Authorization: Bearer $T" "$B/publicProfiles"   # expect TWO profiles, both with points
curl -s -H "Authorization: Bearer $T" "$B/shares"           # expect the shared summary
```

Friend edges live under `users/{uid}/friends/` — one document per friend, id =
the friend's uid.

**Capture for the report:** screenshots of the friends-only leaderboard showing
both users, and of the shared feed item.

---

## [x] 2. Spec title page — **done 06/08/2026**

Filled in by Ido himself, as this item preferred. `GoalPilot_spec_EN.docx` now
reads `Submitted by: Ido · [Ido Mar-Chaim 209497072] · [10208]`; the
`[Full name & ID] · [Course number]` placeholder is gone.

The template's square brackets survive around the name/ID and the course number.
Ido ruled them **cosmetic** on 06/08/2026, so this item is closed rather than
deferred — do not reopen it for the brackets.

⚠️ That file remains listed under **Frozen / off-limits** in
[AGENTS.md](../../AGENTS.md) — it is the course spec and agents must not edit it.
No agent touched it here, and the modified docx is **not** part of the
`product-review` commit; committing it is Ido's move.

---

**Both MUST items are now closed. Nothing blocks handing the project in.**

---

## Not blocking submission

Tracked in [`TODO_OPTIONAL/Integrations.TODO.optional.md`](../TODO_OPTIONAL/Integrations.TODO.optional.md):
Health Connect and competitive challenges are the two remaining spec §6
nice-to-haves. Google Tasks import and the LLM classification bonus are **done**.

~~One governance item, not a feature: [AGENTS.md](../../AGENTS.md) carries template
marker **v4** while the library is at **v7**~~ — **closed 05/08/2026.**
`AGENTS.md` is at **v14**, brought current by the mechanical template sweep in
`f7ae3dd`. It needed the diff-and-confirm flow when it was written; it no longer
does, because verbatim projections are now swept in bulk by
`Update-TemplateConsumers.ps1` rather than upgraded a repo at a time.
