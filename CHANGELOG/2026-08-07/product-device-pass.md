# 2026-08-07 — `product-device-pass` (second sitting)

A short follow-up to [2026-08-06](../2026-08-06/product-device-pass.md), re-claimed
to close the one item that sitting had to leave `unverified`: the **first-run
empty states**. Ido approved `pm clear` on the emulator. Markdown only — no
`app/`, `functions/`, `firestore.rules` or `scripts/` file was touched.

Disjoint from the concurrent `product-model-map` session by construction: this
takes the device, which that session's board row explicitly says it does not.

## What `pm clear` actually bought — and what it did not

**Did not** reach the true zero-data empty states, exactly as predicted when the
option was put to Ido: signing back in restores every goal from Firestore, and the
app was full again within seconds. Those still need a throwaway Google account,
which is now the only remaining route. Recorded as such in the backlog rather than
quietly dropped.

**Did** buy a genuine new finding, `A10`, which is arguably the more valuable one
because it is what every user gets on a **new phone or a reinstall**:

> Between sign-in completing and Firestore's first snapshot arriving, the dashboard
> is a **blank page** — the word "GoalPilot", the bottom nav, and a **single ~8 px
> blue dot**. It held for ~10 s on a healthy connection.

Nothing says the app is fetching anything, and at that size the dot reads as a
rendering artefact rather than a spinner. The same undersized indicator serves as
the sign-in button's progress state. A skeleton, or just a normal spinner and one
line of text, would cost almost nothing.

## An environment trap, recorded so nobody re-diagnoses it

Immediately after `pm clear`, **Play Services wedged**. `SignInActivity` took
focus and rendered nothing — scrimmed screen, 450-byte accessibility tree — and
stayed that way through two full retries and ~90 s, while logcat showed GMS
cold-compiling its Chimera modules (`has no usable artifacts`, repeatedly).

`adb shell am force-stop com.google.android.gms` cleared it instantly, and sign-in
then worked first time.

**This is the emulator, not GoalPilot**, and the discriminator is worth keeping:
the focused window belonged to `com.google.android.gms`, not to the app, so
`dumpsys window | grep mCurrentFocus` answered in one command a question that
would otherwise have looked exactly like a hung app. Written into
`ProductReview.TODO.optional.md` as an environment note.

## `D1` handed over as a candidate `C14`

On Ido's call, `D1`'s verdict goes to the live `product-model-map` session to be
charted with the other thirteen decisions rather than bolted on afterwards. Since
that session owns `TODO/TODO_FUTURE/`, **this session did not write there.**
Instead `D1` now carries a **fenced, liftable `C14` block** that the map session
can paste verbatim — the decision stated, the four candidate answers enumerated,
the `C7` dependency named, and the anti-cheat coupling flagged.

## What to do next, and why

`sessions/fix-task-completion-feedback.md` — a brief for **issue #3**, written
because it is the right next unit *while the map is running*: it touches
`feature/goals/` and `data/firestore/`, and it needs the emulator and the Gradle
daemon, all of which the map session takes none of. Fully disjoint, and it depends
on no `C` decision. By contrast issues #9, #10 and #11 are explicitly blocked on
`C1`, `C7` and `C12` — the very decisions being charted right now — so they must
wait.

## 🧪 Tests

**No suite run, and none applicable.** Nothing compilable changed. Verification
was behavioural: screenshots and `dumpsys window` focus checks across a full
clear → sign-in → cold-load → settled cycle, plus logcat for the GMS diagnosis.

The app was returned to exactly the state it was found in — Aurora skin, 70 pts,
24 % overall, 7 goals — confirmed on screen. Live `goalpilot-56e30` was **read
only**; nothing was created, edited or deleted.
