# Submission — MUST backlog

The two items that block handing the project in. Everything else in spec §6 Core
is built and verified against the live backend (`CHANGELOG/2026-07-31.md`).

Both are **largely manual**. An agent can boot the emulator, install, and verify
the results in Firestore afterwards — it cannot sign into Google as a second
person, and it cannot supply your name, ID or course number.

---

## [ ] 1. Two-account sharing demo (spec §7)

Spec §7 requires the social layer demonstrated with **two real users**. Only one
account has ever signed in, so the leaderboard, friends and shared feed have
never been seen with more than a single row.

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

1. Sign in as **account B** — either on a second AVD, or sign out on the existing
   one. If reusing one device, **kill and relaunch the app between accounts**:
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

## [ ] 2. Spec title page

`GoalPilot_spec_EN.docx` still reads `[Full name & ID] · [Course number]`.

⚠️ That file is listed under **Frozen / off-limits** in [AGENTS.md](../../AGENTS.md)
— it is the course spec and agents must not edit it. **Confirm with the user
before touching it**, and prefer that they fill it in themselves.

---

## Not blocking submission

Tracked in [`TODO_OPTIONAL/Integrations.TODO.optional.md`](../TODO_OPTIONAL/Integrations.TODO.optional.md):
Health Connect and competitive challenges are the two remaining spec §6
nice-to-haves. Google Tasks import and the LLM classification bonus are **done**.

One governance item, not a feature: [AGENTS.md](../../AGENTS.md) carries template
marker **v4** while the library is at **v7** (v5 knowledge-graph section, v6
session hygiene, v7 concurrent sessions). Needs the template-sync rule's
diff-and-confirm flow — a task for a session, not a silent copy.
