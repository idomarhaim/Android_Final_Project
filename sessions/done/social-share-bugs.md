---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: done
issue: https://github.com/idomarhaim/Android_Final_Project/issues/4
created: 2026-08-15 by c21-offline-story
closed: 2026-08-15 — claim `b99c5da`, work `b762520` (pushed); KB ingest `aac7502` in `C:\Dev\JARVIS` (held)
---

> **Closed with two of its Exit conditions unmet, and both are Ido's to lift, not the session's to waive.**
> The `storage.rules` deploy to live `goalpilot-56e30` is an outward action and always-ask — the brief says so
> itself — and the end-to-end device reproduction needs Ido's Google account. `#4` and `#5` are therefore left
> **open**, exactly as the brief's last line instructs. Everything else landed: both bugs fixed, tests at every
> layer that exists (rules 30/30, JVM 218/218, instrumented 39/39), the non-vacuity check paid against the old
> rules and recorded, and `CHANGELOG/2026-08-15/social-share-bugs.md` written.
>
> **The brief's own premise was half wrong, in the useful direction.** It said `#5` needed a `firestore.rules`
> clause written; that clause has existed since `1e56ee3`. What was actually broken was `storage.rules`, which
> the brief never mentions — `allow write` covers `delete`, a delete sends no `request.resource`, and the
> size/contentType guard therefore denied the owner their own image. Found only because step 3's test was
> written before step 5 was believed.

# Fix the two Social feed bugs — `#4` (photo cannot be opened, no label) and `#5` (cannot delete your own share)

## Why these two are one session

They are the **same screen and the same root cause**: `SocialScreen.kt`'s feed card has
**zero interactive nodes** in the accessibility tree. `#4` and `#5` are two things you cannot do to
a card that responds to nothing. Fixing them separately means two passes over one composable.

**They are also the only build work that can run today.** Map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) never ruled on `#4` or `#5`
(verified 2026-08-15: neither issue number appears anywhere in the map body), so nothing here waits
on `docs/PRODUCT_v0.3.md`. Every other open issue does.

## Read first

1. `AGENTS.md` and the rules it links — methodology as it stands then, not as it stood now.
2. [`#4`](https://github.com/idomarhaim/Android_Final_Project/issues/4) and
   [`#5`](https://github.com/idomarhaim/Android_Final_Project/issues/5) in full. Both carry a
   device-verified reproduction; `#5` lists the five layers it needs.
3. `SESSIONS.md` — **read it, and expect siblings.** `product-v03-spec` is live writing
   `docs/PRODUCT_v0.3.md`. Working sets are disjoint (`feature/social/` and `firestore.rules` vs
   `docs/`), which is exactly why this brief exists.
4. `AGENTS.md`'s standing warning on `firestore-tests/` — quoted again in `#5` step 3, and it is the
   one thing in this session that fails silently if ignored.

## Task

Both bugs, in one pass over `feature/social/`.

**`#4` — two faults in one file.** `SocialScreen.kt:269-271`'s `AsyncImage` has no click handler and
no `contentDescription`. Give it a full-screen/zoom destination to open into, **and** a label. The
second does not follow from the first: making the image tappable does not make it announceable, and
the screen reader currently reads the post with the picture simply missing.

**`#5` — a small feature, not a one-liner.** All five layers `#5` enumerates, in its order:
repository method → `firestore.rules` clause restricting deletion to the post's author → a rules
test in `firestore-tests/` → the UI affordance → deleting the uploaded image alongside the post, or
the photo outlives the share that referenced it.

**The rules clause is the load-bearing part.** A client-only delete is denied; a permissive one lets
anyone delete anyone's post. `#5`'s own step 2 is the specification.

## Carries over

- **`firestore.rules` gains its first field-level condition under `C20`** — the challenge
  participant row. Not this session's work, but it means the rules file is about to be edited by a
  build session too; keep this change narrow and additive. Committed:
  [`#42`'s resolution](https://github.com/idomarhaim/Android_Final_Project/issues/42).
- **`C21` puts two more things on this same screen, later**: an *as-of* stamp on cross-boundary
  numbers, and a *"Not loaded yet"* empty state distinct from *"no friends"*. **Do not implement
  either here** — they belong to the spec and its build session. They are named so that this
  session's layout does not have to be redone around them. Committed:
  [`#43`'s resolution](https://github.com/idomarhaim/Android_Final_Project/issues/43) §§3–4.
- **The reproductions are device-verified, not inferred** — PSNR ∞ / MSE 0.00 against the
  pre-tap frame, plus the accessibility tree. Reproduce before fixing and after; a fix that changes
  no interactive node has not landed. Committed: `CHANGELOG/2026-08-06/product-device-pass.md`.

## Out of scope

- **`#2`, `#6`–`#11`, `#34`, `#36`** — all of them depend on decisions `docs/PRODUCT_v0.3.md` is
  being written to record. Do not touch them, even where the file is open in front of you.
- **`docs/PRODUCT_v0.3.md`** — `product-v03-spec` owns it.
- **Redesigning the Social screen.** These are two defects, not `C12`'s design pass.

## Exit

- Both bugs fixed, and **re-verified on a device** the way they were reported — the accessibility
  tree must now show interactive nodes in the feed card.
- **Tests at every layer this touches**: a `firestore-tests/` rules suite for author-only deletion
  (**run it against the *old* rules too** — a pure negative test passes vacuously when nothing
  matches), plus client component tests for the new affordances. Say in the changelog which layers
  ran and which do not exist.
- `CHANGELOG/<the day you work>/social-share-bugs.md` written, with the test results verbatim.
- **The `firestore.rules` deploy to live `goalpilot-56e30` is always-ask** — it is an outward action
  in both modes. Write the rules and the tests; **ask Ido before deploying**, and say plainly in the
  changelog that `#5` is not closable until that deploy happens.
- Commit on approval; close `#4` and `#5` only after the device re-verification.
