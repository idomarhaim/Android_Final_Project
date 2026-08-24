# The tour, re-shot against the app that actually shipped — and the choreography committed this time

**Session** `62-tour-video-v2` · **2026-08-24** · ticket
[`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62) · brief
[`sessions/62-tour-video-v2.md`](../../sessions/62-tour-video-v2.md)

Ido opened this with `/kickoff 62-tour-video-v2` and one instruction in the same message: *base it
on the latest version of the app, not on the brief.* That instruction was the whole session. The
brief's own state block is dated **2026-08-24** and was already stale when it was read.

---

## 1 · What had moved since the brief was last amended

The brief was amended at `27e9be0` (session `62-kickoff-refresh`). Four things changed after that,
and two of them would have produced a film of an app that does not exist.

| | brief says | actually true at `HEAD` |
|---|---|---|
| `#67` | *"still open — it touches five of these surfaces, so recording before it lands buys a `v3`"* | **CLOSED** (`70bb1d3`). The open list is `#51` (parked) and `#62` alone. **Nothing is ahead of this session.** |
| The app | *"`v0.3.3` is the build with all of it in"* | **`v0.4.0`** (`9af6424`), versionCode 9, shipped to Ido and rachil |
| `TUTORIAL_VERSION` | `2` | **`3`** — and step 6's spotlight is now a **live hole** |
| The `#` glyph, the sync cards | described as they were | `#` is gone (`No number`); Google Tasks and Health Connect moved **Home → Settings** |

Verified rather than trusted, as the brief itself asks: `gh issue list` for the tickets, the commit
graph for the build, `TUTORIAL_VERSION = 3` read out of `TutorialStep.kt`, and the moved cards read
off a live `uiautomator` dump of the running app.

## 2 · The emulator was carrying a build that was `9af6424` **except on the screen the tour opens on**

This is the finding that would have quietly ruined the take, so it is first.

The debug APK on `emulator-5554` was built at **01:56**. `DashboardScreen.kt` and
`DashboardViewModel.kt` were last written at **02:01:43**, five minutes later, and committed at
02:13. Its `versionName` read `0.3.3-debug`, because the version bump landed after the build — so
the version string was evidence about neither the build nor the code.

**Nothing on screen would ever have said so**: this app never renders `BuildConfig.VERSION_NAME`
anywhere. `grep -rn 'BuildConfig.VERSION_NAME' app/src/main/java` returns nothing.

So `assert_build_is_current` in the new script does not read a version at all. It asserts that **no
file under `app/src/main` is newer than the built APK**, which is a question with a mechanical
answer. Rebuilt (`:app:assembleDebug`, 39 s), installed with **`adb install -r`** — never
`connectedDebugAndroidTest` — and the Firebase auth store survived, checked by name in
`shared_prefs/` afterwards.

## 3 · `scripts/record-tour.sh` — the deliverable the last session lost

The 2026-08-22 choreography lived in a session scratchpad and died with the session, which is the
entire reason re-recording became a ticket instead of a command. It is committed now, with the five
traps encoded in the helpers rather than in a comment nobody re-reads.

**The one change to the method that the brief asked for, and it is the important one.** The old beat
map was **reconstructed** — the take was shot blind, so every timecode was modelled from the script's
own sleeps plus a measured per-`adb` overhead, then scaled to land on the real duration. `beat()` now
logs a wall-clock reading against a `T0` pinned the moment `screenrecord`'s output file first has
bytes in it. Every row of the new map is **observed**, to the polling interval (50 ms) plus a frame.

### The five traps, and which of them cost a take

| # | trap | how it was found |
|---|---|---|
| 1 | `input keyevent 4` after `input text` **exits the app** (no IME window on this AVD) | inherited from 2026-08-22; `commit_text` exists so nobody reaches for it. Verified: after typing and committing, focus is still `MainActivity` |
| 2 | never locate a control by a fixed swipe count | inherited; `scroll_to_text` re-checks after every short swipe |
| 3 | Git Bash rewrites `/sdcard/...` into `/Files/Git/sdcard/...` | **found here.** `uiautomator dump` wrote to the wrong path and the pull returned a *stale* hierarchy — which reads as *the screen did not change* |
| 4 | `-vsync 0` is a hard error in current ffmpeg | inherited from `CLAUDE.md` |
| 5 | **`screenrecord` silently downgrades to 720×1280 and exits 0** | **cost a take.** See below |

### Trap 5, measured

`screenrecord` at the AVD's native 1344 × 2992 cannot configure the AVC encoder. It prints a
warning, **falls back to 720×1280, records anyway, and exits 0** — so nothing downstream fails and
what you get is a marketing film at 720p. The first take ran two and a half minutes that way.

Binary search against the same encoder:

| size | |
|---|---|
| 1344 × 2992 (native) | ERROR |
| 1280 × 2848 | ERROR |
| 1216 × 2704 | ERROR |
| **1152 × 2560** | **OK — the ceiling, and the new default** |
| 1080 × 2400 | OK — what the 2026-08-22 cut used |

So the new recording is **larger than the previous one**, and `start_recording` now reads
`screenrecord`'s own stderr and refuses to continue if the size did not survive.

## 4 · Rehearse with writes disabled — it found twenty failures no take would have reported loudly

`--dry-run --no-writes` walks every screen and skips the three steps that change real data: the
smart-add sentence, the task added to a goal, and the calendar drag. It exists because a rehearsal
that creates two junk tasks and reschedules a real block buys itself a cleanup pass, and a cleanup
pass is a deletion, which is always-ask.

**Rehearsal 1 — 43 beats, and everything from Act 6 onward was wrong.** One cause: Act 5 ends on the
goal-detail screen, which has no bottom bar, and nothing brought the app back. Seven acts then ran
against a screen that had none of the things they were looking for, and logged eighteen misses that
all read as *the label must have changed*.

**Rehearsal 2 — 58 beats, clean end to end.** What the two rounds cost: about twenty minutes. What
they found:

- **A substring match over a hierarchy needs a positional band.** The rehearsal tapped `Goals` and
  hit the friends feed's *"avg 24% across 7 goals"* at **y = 1735** instead of the tab at
  **y = 2848**. Nothing failed. `tap_tab` now filters candidates to the bottom 12 % of the screen.
- **The avatar is `content-desc="Your account"`**, and Analytics, Challenges and Life areas all hang
  off the sheet behind it — not off any tab. Three acts were opening the wrong door.
- **Analytics must shoot the Year view.** Week says *"Nothing completed in this week yet"* and Month
  is empty; only Year carries the 67 % / 20 % / 13 % over 3h 45m that the narration describes.
- **`scroll_to_text` stops when its needle is visible, which can be on the bottom edge** — so the
  `Material` header appeared while all four of its cards were still below the fold, and four taps
  missed. It now nudges once when the hit is in the bottom 30 %.
- **`Filed nowhere` is drawn *above* the smart-add card**, and `scroll_to_text` only searches
  downward, so once Act 2 had scrolled down to type, `#67`'s beat could never be found again. It was
  silently dropped from a whole take before `scroll_to_top` was added.
- **The add-task controls are icon buttons.** `Estimate difficulty and duration with AI` and
  `Add task` are `content-desc`s, not text, so `tap_text` could never find them.
- **`uiautomator` wedges, and then hands back the previous run's file.** A killed instance leaves its
  accessibility service registered; the next dump dies with `UiAutomationService … already
  registered!`, writes nothing, and `cat` returns a stale hierarchy. That read as *the app is stuck
  on the launcher* for several minutes while the app was running normally. `dump_ui` now deletes the
  device-side file first, so a failed dump is a miss rather than a lie.
- **A wedged dump is not an *empty* file — it is a well-formed 56-byte one.** `[ -s ]` passes it, so
  the guard I first wrote never fired. `dump_ok` asks the question that discriminates: does the
  document contain a `<node`? *(And `grep -c '<node'` is not that question either — uiautomator
  writes the whole hierarchy on one line, so a healthy dump counts as `1`.)*
- **The recovery I wrote for it was itself the bug, and it cost the fourth take.** `dump_ui`
  recovered from a wedge by killing the instance — and killing an instance is *precisely* what
  leaves the accessibility service registered. So the first wedge triggered a kill, the kill
  guaranteed the second wedge, and the run never came back: take 4 wedged at beat 7 and then failed
  every dump for **27 more minutes** while the app sat in the foreground working perfectly. 102
  misses, and `FATAL: could not pull the recording` at the end of it.

  **Waiting works.** The service clears on its own, so `dump_ui` now retries three times with a
  growing pause and kills nothing; `kill_uiautomator` survives for **preflight only**, where a wedge
  inherited from a previous run does have to be broken once.

  Verified before spending another take on it, under the condition that broke it — **30 consecutive
  dumps with `screenrecord` running: 30 ok, 0 fail.** That number is also what rules `screenrecord`
  out as the cause, which was the more obvious suspect and the wrong one.

## 5 · The narration was not merely dated — two paragraphs were false

`docs/marketing/explainer-video-brief.md` §3, rewritten against `v0.4.0`:

- **Act 3** narrated the Google Tasks and Health Connect cards as living on the home screen. They
  moved to Settings on 2026-08-24. They are narrated in Act 12 now.
- **Act 5** was worded around the app having *no calendar grid and no Google Calendar sync*, with
  §7 listing both under *not in the app*. Both shipped on 2026-08-23. That paragraph was the film
  throwing away its two strongest beats.

Thirteen acts now, up from twelve. New: **Act 6** (the calendar, the load bar, drag-to-move and the
scope sheet), **Act 7** (Google Calendar, whose proof shot is Google Calendar's *own* UI — showing
it from inside GoalPilot asserts the sync, showing it from inside Google Calendar demonstrates it),
and **Act 8**'s second half (`#64`'s kept / missed / still-owed run).

Three warnings are written into the script itself, because they are the ones a voiceover artist
would get wrong: `#64`'s run must not be called a **score**, a **rate**, or described as **red** — it
is drawn by form, carries no hue coding and shows no ratio anywhere; and Act 13's claim that the
tour *waits where you pressed* is only true from `v0.4.0`.

## 6 · A marketing film of an app is a film of its **data**, and this account is empty of the new features

Audited before the shot list was written, with `uiautomator` against the live account. Nothing here
is a bug and nothing is a doc error — it is an account that has not been used for two weeks. It
matters because five beats have nothing to draw:

| beat | what the screen actually says |
|---|---|
| Overall progress (home) | *"No goal has a number yet"* — **no** goal on the account carries a measure |
| `#64`'s kept / missed / still-owed run | *"Nothing has been due here yet"* on **every** life area |
| The calendar (`#60`) | **one** entry, and the day reads *free* |
| The scope sheet (`#68`) | needs a **recurring** block; there is none |
| Analytics by week or month | empty; only **Year** has data |

`#64` and `#65`/`#66` are the two beats the brief singles out as *"the ones that say something about
the product rather than about a feature"*, and one of them renders an empty state.

**Also on screen, and Ido's call rather than mine:** the four life areas are named in Hebrew
(בריאות · לימודים · קריירה · זוגיות, synced from his Google Tasks lists), the home screen greets him
in Hebrew, and the friend in the leaderboard has a Hebrew name — in a film whose narration is
English. Recorded in §7 of the brief as a fact about the *footage*, not about the app.

## 7 · The model report — 🎬 video · 🗣️ voice · 🖼️ image

**None of the three was reached, and the reason is one fact rather than three.**

⚠️ **The OpenArt MCP server is not connected to this session at all.** It is not in the connected
list and not in the needs-authorisation list — there is no `openart` tool of any kind, confirmed by
enumerating the deferred tool roster. OAuth needs a browser and cannot be driven from a tool shell,
which the brief already predicted: *"the MCP is Ido's to connect."*

So the **roster call** — which the brief makes the first model call of the session, and the only
thing that settles whether OpenArt's roster actually carries Seedance 2.5 — could not be made.

| component | model used | why |
|---|---|---|
| 🎬 **Video** | **not reached** | no MCP surface. The brief's prior is **Seedance 2.5**; `Untested:` whether the roster carries it, since OpenArt's own MCP page still lists 2.0 while a 2026 round-up lists 2.5 |
| 🗣️ **Voice** | **not reached** | as predicted: OpenArt's published MCP surface lists no TTS, audio or Director timeline control at all. The brief's prior is **ElevenLabs v3**, with Multilingual v2 per-act if delivery drifts |
| 🖼️ **Image** | **not reached** | no MCP surface. The brief's prior is **FLUX.2 Pro** for reference and start frames |

Credits spent: **zero**. Re-rolls: **zero**. This is the brief's middle row — *"only model calls are
reachable"* — degraded one step further, to *no model calls are reachable*, and it is a
connection Ido has to make.

## 🧪 Tests

**No application code changed.** This session wrote one shell script, two Markdown documents and a
KB-candidates file, and rebuilt an unmodified tree. No suite applies to any of them and none was
run — stated rather than skipped silently.

The layer that does apply here is **the script against the running app**, and it was exercised the
way the testing rule means it: two full rehearsals and three takes against `emulator-5554`, with
every selector resolved against a live `uiautomator` hierarchy rather than read off the source. The
count of things that fixed: **eleven**, listed in §3 and §4 above.

One build ran and passed: `:app:assembleDebug`, **BUILD SUCCESSFUL in 39s**, 42 tasks. The JVM suite
was **not** re-run, because no source file changed — the last recorded run is
`s25-layout-and-tour`'s 1,093 tests, 0 failures, against this same tree.

## Board

Claimed at `750ff35`. The **Gradle daemon** was taken from `docs-repair` for one 39-second
`assembleDebug`, announced on the board **before** it was taken with the evidence that it had been
claimed-but-unused for twenty-two minutes, and released immediately afterwards. `emulator-5554` was
free (released by `s25-layout-and-tour` at 02:23) and its geometry was confirmed at the native
1344 × 2992 / 480 dpi before anything was shot.

---

## 8 · The take that shipped — five attempts, and what each one taught

| take | outcome |
|---|---|
| 1 | killed at 2m30s — `screenrecord` had silently downgraded to **720×1280** |
| 2 | killed — `Sort` scrolled out of reach, `Filed nowhere` unreachable (it is drawn *above* the smart-add card and the search only goes down), add-task controls not found |
| 3 | killed at Act 2 — a wedged `uiautomator` inherited from take 2 |
| 4 | ran 28 minutes, 102 misses, no file — the kill-based dump recovery death-spiralled |
| **5** | **shipped** |

**Take 5:** **70 beats, `11:57.4`, 1152 × 2560, constant 30 fps, 21 522 frames, 44 MB.** The raw
`screenrecord` output beside it is VFR at 5 275 frames over the same duration. Seven misses, all
seven inside Act 5's add-task sub-flow, none anywhere else.

`adb pull` failed on the way out and the `exec-out cat` fallback added an hour earlier is what saved
the take — the device shell's view of scoped storage and adb's did not agree after the reboot, while
the dump written to the same directory read back fine.

### Verified by looking, not by reading the log

Three beats extracted from the finished file at the timecodes the file itself claims, and looked at:

| beat | claimed | what the frame shows |
|---|---|---|
| 27 · `4:26.0` | the calendar | ✅ `24 Aug – 26 Aug`, 3-day view, all-day strip, load bars reading *free*, the `+` FAB |
| 66 · `11:32.3` | tour step 6 | ✅ **Step 6 of 7**, the spotlight ring around the **Calendar tab**, and *"Tap Calendar to open it, or press Next"* — the `v0.4.0` live hole, which is most of why this was re-shot |
| 10 · `0:45.7` | smart add filed it | ⚠️ the payoff is **behind the keyboard** in that frame — but it did work: the goal screen later in the same recording carries *"Practice saxophone for 20 minutes on Sunday · 20m · +7"*, filed by the AI under the right goal |

**The first spot-check was wrong and it was my error, not the map's.** I extracted at seconds
remembered from the choreography instead of read out of the generated file, got a frame from a
different act, and briefly had the beat map down as misaligned. Re-run against the file's own
timecodes, three of three match. *Worth recording because the failure mode is the one this whole
session is about: an instrument that is not itself checked will report on the wrong thing and sound
just as confident.*

### What is honestly not in this take

Act 5's add-task sub-flow — the AI estimate, the date picker, the submit — did not fire. The
measure-proposal hunt earlier in that act scrolls to the bottom of the goal screen, and the add-task
row is at the **top** of it, so a downward-only search had already gone past it. **Exactly the same
shape as Act 3's `Filed nowhere`**, one act later and found one take later. Fixed in the script with
`scroll_to_top`; **not re-shot**, and named in `explainer-video-brief.md` §1 rather than left for a
reader to notice.

### One thing was destroyed and it is worth saying plainly

**The 2026-08-22 cut is gone**, overwritten by this one. The Exit criterion names
`GoalPilot-full-tour.mp4` as the destination while the brief also lists the previous cut under *what
you inherit* — both true, and the second lost. Nothing else held a copy and a video is not in git.
`archive_previous` now stamps and keeps whatever is already there, so this cost one cut rather than
becoming a habit. `GoalPilot-highlights-60s.mp4` survived only because its name differs.

---

## 9 · Committed, NOT pushed — and it is precondition 5, not a judgement call

`1d6ef09` and `cf4adb0` are on `main` locally and **held**. Checked at the moment of writing:
`git fetch` at **2026-08-24 04:29:56**, upstream still `4db36d9`, so **still unpublished as of that
check** — which is the dated form the rule asks for, because `git push` is branch-scoped and a
sibling can publish my commits on their schedule with no gate of mine involved.

`git log @{u}..HEAD` carries **six foreign commits**, and they are named here because the reply that
also names them scrolls away and this file does not:

| commit | session | board state |
|---|---|---|
| `3e4f381` | `docs-repair` | **LIVE row in Active claims** |
| `59283d0` | `docs-repair` | **LIVE row in Active claims** |
| `9af6424` | `s25-layout-and-tour` | released 02:23 |
| `e56bd1a` | `s25-layout-and-tour` | released 02:23 |
| `2a55e19` | `exam-qa-pack` | released |
| `750ff35` | mine (the claim) | — |

**`docs-repair`'s live row is the stop.** Precondition 5 is explicit: a foreign commit whose paths
sit under a live row is a session mid-unit, and in auto mode *"name it in the reply"* is a disclosure
Ido may read hours after the push landed. Un-publishing needs a force-push, which is always-ask.

**The §5.3(c) liveness check was run anyway, and it does not change the answer.** `docs-repair`'s
last commit is `59283d0` at **01:50**, and its last transcript turn — found by the `file-history-*`
records naming the label, not by `grep -l`, and read from the last `user`/`assistant` `timestamp`
rather than the file's mtime — is **2026-08-23T22:51:00Z (01:51 local)**. Both quiet for **2h39m**.

That is a **cross-check, not a substitute**: the transcript escalation is scoped to the
*absent-row* branch, and `docs-repair`'s row is present and live. A negative result there means
*nothing has been observed*, never *released*. Its own last turn also reads *"the suite has not been
run, and the push is held"*, so that session considered its own work unfinished.

**Nothing is lost by the hold.** Both commits are on `main` locally, and `s25-layout-and-tour`
already established that distribution did not depend on the push — v0.4.0 reached Ido and rachil
through App Distribution from a local build.
