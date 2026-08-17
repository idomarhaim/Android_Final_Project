---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: auto
status: ready
issue: 51
created: 2026-08-17
---

# Finish `hebrew-defer-freeze`'s last two steps: look at the freeze, and post the three #51 writes

**Short session, and it needs two permissions Ido must grant first** — `adb` and `gh` writes
were both denied by the harness classifier during wave 1, which is why these two steps
survived a session that otherwise completed. **Nothing here blocks wave 2**; run it whenever
the permissions exist.

## Read first

1. [AGENTS.md](../AGENTS.md) — the §0.8 suspension block wave 1 added
2. [CHANGELOG/2026-08-17/hebrew-defer-freeze.md](../CHANGELOG/2026-08-17/hebrew-defer-freeze.md) — what shipped in `7baf120`, including the third door
3. [CHANGELOG/2026-08-17/51e-sweep-components.md](../CHANGELOG/2026-08-17/51e-sweep-components.md) **lines 248–347** — the appendix holding the verbatim #51 comment body
4. [sessions/done/hebrew-defer-freeze.md](done/hebrew-defer-freeze.md) — steps 5 and 6 are this brief's whole scope

## Task

### 1 · The render pass — the freeze's own claim, still unseen

`AppLanguage.OFFERED` closes **three** doors and is proven by six new JVM tests (364/0). But
the claim under test — *the app is uniformly English* — is **visual**, and the standing lesson
from `51c`/`51d` is exactly that logic does not settle it: **correct RTL mirroring was twice
mistaken for correct localization**, at two different layers, with two different causes.

So: boot `Pixel_10_Pro_XL`, set the device locale to **Hebrew**, launch, and **look** at
**Home, Profile and Analytics**. Those three because `feature/analytics` and `ui/components`
are the two *swept* packages — the only places with Hebrew strings on disk, so the only places
a leak can show.

**The strongest single check is door 3, and it needs a device that has used the feature.**
Persistence was the door the brief missed: `AppPreferencesRepositoryImpl` read
`AppLanguage.fromId(stored)`, and `fromId` faithfully returns `HEBREW` for `"he"` regardless of
what the picker offers. 51e's release note records this device **rendering the app in Hebrew**,
so `Pixel_10_Pro_XL` may well hold a stored `"he"`. If it does, this is the one machine in the
world where the pre-freeze state can be observed — **check it before wiping anything.**

### 2 · The three #51 writes wave 1 could not make

`gh` writes were denied by the harness classifier — correctly, as an outward-action gate the
session declined to route around. **GitHub itself is fine**: the API recovered on 2026-08-17
after a partial outage, and reads work normally. The blocker is permission, not the service.

```
gh api repos/:owner/:repo/issues/51/comments -F body=@<CHANGELOG/2026-08-17/51e-sweep-components.md lines 248-347>
gh api repos/:owner/:repo/issues/51/comments -f body='#51 deferred 2026-08-17 — see AGENTS.md § "§0.8 is suspended"'
gh api repos/:owner/:repo/issues/51 -X PATCH -f body='<body minus the "precondition of every screen ticket" line>'
```

**Read each body before sending it.** These are outward-facing writes to a public repo: the
first is another session's prose, the third **overwrites** an issue body. Fetch the current
body first (`gh issue view 51 --json body`), remove only the one line, and diff what you are
about to send against what is there.

## Carries over

- **`#51` stays OPEN.** None of these three writes closes it; the deferral comment says why it
  stopped, not that it is finished.
- **Do not re-open the freeze design.** `DEFAULT` stays `SYSTEM` deliberately, `fromId` stays
  whole because #51 needs the full id round-trip, and `offeredFromId` is used **only** by the
  preferences read path. All three were decided in `7baf120` with reasons.
- **The picker is not broken.** Hebrew missing from `LanguagePicker` is the freeze working.
- **`Pixel_10_Pro_XL` is signed out** — 51e's instrumented run uninstalled the app. Step 1 needs
  no account, so **do not ask Ido to sign in**. Announce `## 📱 DO NOT SIGN IN` before the
  device run.
- **`CHANGELOG_README.md` is generated** — run `scripts/New-ChangelogIndex.ps1`, and prefer
  `-Staged` if any sibling file is dirty in the tree.

## Out of scope

- **Any sweep of any package.** #51 is deferred; this session verifies the *deferral*.
- **Any `app/src/` change**, unless the render pass finds a real leak — in which case fix the
  leak, say so loudly, and add the test that would have caught it.
- **Closing #51**, editing its title, or touching its labels.
- **Wave 2's work.** If wave 2 is already running, claim only this brief's paths and check the
  board's singleton column before booting the emulator.

## Exit

- The Hebrew-device render **described, not asserted** — what you saw on each of the three
  screens, and explicitly whether the device held a stored `"he"` from before the freeze.
- The three writes posted, each with the body you actually sent named in the changelog; or, if
  a permission is still missing, the exact command left owed rather than silently dropped.
- JVM unit re-run only if you changed code. `assembleDebug` green if you did.
- `CHANGELOG/<today>/51-freeze-verify.md` written, index regenerated.
- Board row released; brief closed to `sessions/done/` with `status: done` in the same commit.
  **Commit and push under AUTO MODE** — run the sibling checklist in the roadmap's §🔀 first,
  and say in your reply that the mode acted.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

End your final reply with **exactly one** of these headings, below the three file lists.
Full definition — the **seven** `GO` conditions, and which reply carries the heading:
[TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)
§🚥. Ido must never have to work out for himself whether the next kickoff is safe.
**This brief runs in AUTO MODE** (`mode: auto`, Ido's standing instruction of 2026-08-17), so
condition 1 is met by **you having committed**, not by his approval. ⚠️ **Auto mode does NOT
cover the three `gh` writes** — those are outward-facing, and Ido granted them for *this task*
on 2026-08-17. If a harness prompt appears, that is the grant being exercised, not a rule being
bypassed; if it is denied, the write stays owed and named. Name the slug either way.

- **On success:** name whichever wave-2 slug is still unrun —
  `## 🚥 GO — NEXT: /kickoff 50-offline-stamps` **or** `/kickoff 48-settings-surface`, plus a
  Lane C session. If wave 2 is already finished, `STOP` — the wave-3 briefs are owed.
- **If a permission was still denied:** that is a `STOP` naming which one and that it is Ido's
  move — the same shape wave 1 ended in. Do not report the step as done because it was
  attempted.
- **If the render pass found a leak:** `STOP` regardless of everything else. A freeze that
  leaks is not a freeze, and wave 2 would build on it.
