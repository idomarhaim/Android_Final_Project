# KB candidates — `6-silent-filing` round 2, 2026-08-20

Session: `6-silent-filing` · issue [#6](https://github.com/idomarhaim/Android_Final_Project/issues/6) ·
mode `AUTO MODE`. Round 1's file was drained in full and deleted (`13338dc` in `C:\Dev\JARVIS`);
this is a **second** file for what the device pass found afterwards.
Account: [`CHANGELOG/2026-08-20/6-silent-filing.md`](../CHANGELOG/2026-08-20/6-silent-filing.md)
§ *Round 2*.

---

## 1 · Consuming the state a `LaunchedEffect` is keyed on, **inside that effect**, cancels it before it runs

**Claim.** A one-shot UI event delivered as nullable state — `StateFlow<Event?>`, a
`mutableStateOf(event)` — is normally drained by the effect that renders it. If the effect is
**keyed on that state** and clears it **before** the suspending work, it destroys itself: clearing
changes the key, `LaunchedEffect` restarts, and the coroutine is cancelled at its first suspension
point. The event is consumed and **never rendered**.

```kotlin
LaunchedEffect(event) {                 // key
    val e = event ?: return@LaunchedEffect
    consume()                           // ← changes the key; this coroutine is now cancelled
    host.showSnackbar(e.text)           // never reached
}
```

**Why it survives review.** Each half is a recognised good practice. Draining a one-shot event
promptly is the standard fix for *"the snackbar reappears on rotation"*. Keying the effect on the
event is the standard way to re-fire it when a new one arrives. The defect is only in the
**composition**, and the wrong order is the one that *sounds* more careful — it is chosen to stop a
second event queueing behind a long-duration snackbar.

**That motivating concern is real, and the key already answers it.** A new event changes the key,
restarting the effect, which dismisses the stale snackbar and shows the current one. So the fix is
**await first, consume after** — and deliberately **not** in a `finally`: on cancellation the state
already holds the *next* event, and clearing it there swallows exactly the event the wrong order
was trying to protect.

**Observed** 2026-08-20, GoalPilot `#6`. The consumed-early version filed every task correctly and
showed its confirmation snackbar **zero** times. Nothing failed: JVM unit 506/0, instrumented
114/0, functions 37/0, emulator 10/0.

**Why no test layer could hold it.** The bug is in a **coroutine's lifetime**, not in a value, so
no unit test at any layer can reach it. And the instrumented suite that *did* exist drove the two
composables in isolation — an isolated-component suite cannot fail on a wiring defect, because the
wiring is exactly what it substitutes for. What found it was rendering the screen and looking.

**Severity is domain-specific and worth stating:** the snackbar was the app's **only** record of a
filing it had performed without asking, under a rule that permits silence *only* in exchange for an
after-the-fact witness. A defect that removes a witness is not the same class as a missing toast.

- **Destination:** `C:\Dev\JARVIS\kb\dev\` — `screen-entry-effects-and-viewmodel-lifetime.md` looks
  like the existing home (same family: effects, keys, lifetimes); confirm by reading it before
  filing, and if it is about entry rather than one-shot events, a small page of its own.
- **Anchors:** `DashboardScreen.SmartAddReceiptSnackbar`, `SmartAddReceiptUiTest`,
  `CHANGELOG/2026-08-20/6-silent-filing.md` § *Round 2*.
- **Supersedes:** nothing.
- **Status:** ready to ingest.

---

## 2 · A test harness that stubs a callback as an **inert counter** cannot see a bug whose cause *is* the callback

**Claim.** When writing a regression test for a defect, the harness's fake collaborators are
usually written as recorders — `onFoo = { count++ }` — because the assertion is about *whether* the
callback fired. If the defect's **mechanism** is what that callback *does* to shared state, an
inert recorder deletes the mechanism, and the test passes against the very code it was written to
catch. It then ships as a guard, and it guards nothing.

**Observed** 2026-08-20, GoalPilot `#6`, measured in three runs on one device:

| Version | Result |
|---|---|
| broken code, `onConsume = { count++ }` | 2 of 7 red — and **not** the headline case |
| broken code, `onConsume = { count++; state = null }` | **5 of 7 red**, headline case included |
| fixed code, same harness | 7 of 7 green |

The headline case — *"a filing is announced at all"* — is the one a reader trusts, and it was the
one that passed. Two narrower cases named the *mechanism* rather than the symptom
(*"the receipt is not consumed before it is shown"*) and went red either way, which is the
secondary lesson: **a case that asserts the mechanism survives a weak harness; a case that asserts
the symptom does not.**

**The general rule:** a stub must reproduce every effect of the real collaborator that the code
under test can *observe*. Here the observable effect was nulling the state the effect was keyed on
— the whole causal chain. Cheap to get right, and undetectable when wrong except by the check
below.

**This is what running the instrument against the defect buys**, and it is the specific reason the
*look-at-your-own-output* rule says to check the tool on the hardest input it exists for. Verifying
green-against-the-fix proves nothing: **run it red against the defect, and read which cases fail.**
Had that step been skipped, the recorded outcome would have been *"regression test added, green"*
with the headline assertion silently inert.

- **Destination:** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` as a section — it already owns
  *check the instrument itself on the hardest input it exists for*, and this is a concrete,
  measured instance with a named failure shape. Alternatively `breaking-a-guard-must-compile.md`,
  which is the neighbouring *does the guard actually fire* page; read both before filing.
- **Anchors:** `SmartAddReceiptUiTest.setHost`, the three-run table above.
- **Supersedes:** nothing. **Sharpens** the existing *check the instrument* clause with a shape it
  does not name: the instrument was checked, and the first check was itself too weak.
- **Status:** ready to ingest.
