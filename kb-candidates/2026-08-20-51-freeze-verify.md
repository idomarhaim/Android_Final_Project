# KB candidates — `51-freeze-verify`, 2026-08-20

---

## 1 · An owed outward write can land without the record that owes it ever being updated

**Claim.** Before re-making an outward write that a brief or changelog says is **owed**, read the
target. A blocked write is usually blocked on a *permission*, and permissions get granted — the
session that finally got the write through is often a **different** session from the one that
recorded the debt, and it has no reason to go back and amend that session's changelog or someone
else's brief.

**Why.** `Observed:` 2026-08-20. `hebrew-defer-freeze`'s changelog §6 (2026-08-17) lists three
`#51` writes as owed, denied by the harness classifier, and says *"nobody should assume any of
them happened"*. `sessions/51-freeze-verify.md` was carved out of that residue and made those
three writes its whole second half. **All three had landed on 2026-08-17** — comments
`5318172476` (17:26Z) and `5318401943` (17:59Z), body `updated_at` 18:26Z — by the session whose
render pass the deferral comment describes. Cost of checking: one unauthenticated
`curl .../issues/51`. Cost of not checking: three duplicate comments on a public issue, one of
them 5 KB.

The asymmetry is what makes this a rule rather than a nicety: **the debt record is written by
the session that could not act, and the payment is made by a session that can.** Nothing
structurally connects them. This is the same family as `/kickoff` §2's *"has the work already
landed?"* — that step asks the **repo**; this one says the **tracker** needs asking too, and the
tracker is the one artifact `ls CHANGELOG/*/<slug>.md` cannot see.

Rejected: *"the changelog is the record, trust it."* It is the record of what **that session**
observed, and it was accurate when written. The failure is not in the record, it is in treating
a statement about a permission gate as durable.

**Destination.** `kb/dev/flows/` — alongside the `/kickoff` flow page, or a short page on
outward-write bookkeeping. It amends `/kickoff` §5 step 4's *"a refused write stays owed and gets
said"*, which currently tells the **blocked** session what to do and says nothing to the session
that inherits the debt.

**Anchors.** `sessions/done/51-freeze-verify.md`; `CHANGELOG/2026-08-17/hebrew-defer-freeze.md` §6;
`CHANGELOG/2026-08-20/51-freeze-verify.md` §3.

**Supersedes.** Nothing. Extends `/kickoff` §2 and §5 step 4.

**Status.** ready to ingest.

---

## 2 · `json.load(sys.stdin)` on Windows fabricates mojibake — and it fabricates it in the *alarming* direction

**Claim.** When reading a UTF-8 HTTP response in Python on Windows, write it to a file and read
it back with `io.open(..., encoding='utf-8')`, or set `sys.stdin.reconfigure(encoding='utf-8')`.
A bare `curl … | python -c "json.load(sys.stdin)"` decodes the bytes as **cp1252**, so `§`
becomes `Â§`, `—` becomes `â€"`, and every Hebrew word becomes `×™×¢×“`.

**Why.** `Observed:` 2026-08-20, verifying a `gh issue edit` against `#51`. The comparison
reported the live body as differing from what was sent, with **every non-ASCII character on
every line** mangled — which reads exactly like *"the write corrupted a public issue body,
including its Hebrew"*. It had not: re-reading the same response through a UTF-8 file gave
`identical: True`, Hebrew intact. **The instrument was the only thing that was broken.**

Two properties make this worth a page rather than a note:

1. **It fails toward panic, not toward silence.** A reader that mangles text does not report
   *"I could not read this"*; it reports a diff, and the diff accuses the thing you just did.
   The instinctive next move — revert the write, re-send it, apologise — makes it worse.
2. **This project's `CLAUDE.md` already carries the other half and reads as complete.** It says
   *"print through `sys.stdout.reconfigure(encoding='utf-8')` or the console codec mangles every
   `—` and `§`"* — correct, about **output**, and it is the same sentence a session reaches for
   when the mangling is on **input**. A half-stated encoding rule is worse than none, because it
   makes the reader stop looking.

**Destination.** `kb/dev/` — the Windows-tooling page, cross-linked from
`kb/dev/look-at-your-own-output.md` (the *check the instrument itself* clause: here the
instrument degraded on precisely the input it existed to check, a non-ASCII body).
Also a one-clause correction to this project's `CLAUDE.md`, made in the same commit.

**Anchors.** `CLAUDE.md` § the unauthenticated REST read path; `CHANGELOG/2026-08-20/51-freeze-verify.md` §3.

**Supersedes.** Nothing; **completes** the `sys.stdout.reconfigure` claim in this project's
`CLAUDE.md`, which was true and partial.

**Status.** ready to ingest.

---

## 3 · A test whose pass looks like "nothing happened" needs a second, visible probe in the same channel

**Claim.** When the correct behaviour under a test input is *no observable change*, the test
cannot distinguish **the guard worked** from **the input never arrived**. Put a second value
through the **same channel, in the same operation**, whose effect is plainly visible, and check
that one first.

**Why.** `Observed:` 2026-08-20, verifying `#51`'s door 3. `AppLanguage.offeredFromId` is meant
to clamp a pre-freeze stored `"he"` to the default, so the pass condition is *the app renders in
English* — which is also exactly what you see if the injected SharedPreferences file was never
read at all (wrong path, wrong package, app not force-stopped, write lost). The whole test is
vacuous in that case and looks like a clean pass.

The fix was to write `app_brightness=dark` into the **same file** beside the `app_language=he`,
in the **same write**, and relaunch. The app came up dark → that file was read on startup by
that `getSharedPreferences` call → the `"he"` beside it was read too. One extra key converted
*"we saw English"* into *"we saw English **after** the stored `he` was read"*.

Generalisation: the co-probe must share the **channel**, not merely the intent. A separate
assertion that the file exists tests the filesystem; a key in the same file read by the same call
tests the read.

**Destination.** `kb/dev/look-at-your-own-output.md` — this is a concrete instance of *check the
instrument itself on the hardest input it exists for*, in the shape that suppression tests take.

**Anchors.** `CHANGELOG/2026-08-20/51-freeze-verify.md` §2 *The instrument check*;
`app/src/main/java/com/idomarhaim/goalpilot/data/prefs/AppPreferencesRepositoryImpl.kt:51`.

**Supersedes.** Nothing.

**Status.** ready to ingest.

---

## 4 · A brief's device-state facts rot faster than anything else in it

**Claim.** Treat every device-state claim in a brief (*signed in* / *signed out* / *holds X* /
*locale is Y*) as a reading taken on the day it was written, and re-read it before relying on it.
Say in the changelog what you actually found.

**Why.** `Observed:` 2026-08-20. `sessions/51-freeze-verify.md` (written 2026-08-17) asserted two
device facts and both were false by the time it ran: `Pixel_10_Pro_XL` was **signed in**, not
signed out; and it held **no** stored `app_language`, so the brief's *"this may be the one machine
in the world where the pre-freeze state can be observed — check it before wiping anything"* was
pointing at evidence that a prior instrumented run had already destroyed. Neither error was
avoidable when written — a device is shared mutable state with no version control and no diff.

Both errors mattered, in **opposite** directions, which is why this is not just *"briefs go
stale"*: the false *signed out* nearly caused the session to skip the three screens it existed to
look at, and the false *may hold `"he"`* would have been reported as a found pre-freeze state
rather than the reconstruction it actually had to be.

**Destination.** `kb/dev/flows/` — the brief-writing half of §4.1, or the device-state rule's
page. Adjacent to the 📱 banner rule, which governs what you **ask** for; this governs what you
**believe**.

**Anchors.** `sessions/done/51-freeze-verify.md` § *Carries over*;
`CHANGELOG/2026-08-20/51-freeze-verify.md` §1.

**Supersedes.** Nothing.

**Status.** ready to ingest.
