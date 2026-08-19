# KB candidates — `48-settings-surface`, 2026-08-20

Session: `48-settings-surface` · issue [#48](https://github.com/idomarhaim/Android_Final_Project/issues/48) ·
mode `AUTO MODE` (brief front matter, Ido's standing instruction of 2026-08-17).
Account: [`CHANGELOG/2026-08-20/48-settings-surface.md`](../CHANGELOG/2026-08-20/48-settings-surface.md).

---

## Standing — always-ask, parked

## 1 — An instrumented run and a render pass are **not** mutually exclusive on one device; `connectedDebugAndroidTest` is

**Claim.** The device-state rule added on 2026-08-16 says *"`connectedDebugAndroidTest`
**uninstalls the app**, which takes a Google account with it. So an instrumented run and a render
pass cannot share a device session: whichever runs second destroys what the first needed."* The
first sentence is true and the second does not follow from it. The uninstall is a property of
**that Gradle task**, not of instrumented testing, and there is a path with the same coverage and
no uninstall:

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <serial> shell am instrument -w <appId>.test/<runner>
```

`install -r` is a data-preserving reinstall, so the app's own Firebase auth store survives — and
that store, not the device's AccountManager entry, is what a sign-in actually is from the app's
point of view. `Observed:` 2026-08-19 on `Pixel_10_Pro_XL_B`, which was signed in as
`rachil751@gmail.com` with a persisted `FIREBASE_USER`: the full suite ran **82/82 green** and the
store still held its `FIREBASE_USER` at the same size afterwards, with the app still landing on the
dashboard rather than the sign-in screen. The Gradle task was never invoked. *(`Observed:` the
store's size and key, and the app's own behaviour — not a byte comparison; the file's mtime does
move, which is consistent with the token being refreshed on the next launch.)*

**Why it matters beyond convenience.** The rule's remedy is *"it says which it is doing this turn,
and defers the other to its own session"* — one session's work split across two, each costing a full
kickoff. On the day this was measured the machine had **0.5 GB of 15.7 GB free**, so booting a
second AVD to dodge the conflict was not available either; without this path the instrumented half
of `#48` would have been reported as owed.

**What it does not change.** `connectedDebugAndroidTest` still uninstalls, so a brief that names it
still carries the warning. And the two *are* mutually exclusive when the render pass needs a
**signed-out** device and the instrumented one needs a signed-in one — that is a conflict about the
state itself, not about the task, and this session hit exactly that (see the changelog's *Tests*).

**Why.** Considered and rejected: leaving the rule alone and treating this as a local trick. That is
what the rule already costs — a session reading it concludes it must choose, and the conclusion is
wrong in the common case. The narrowing belongs where the claim is, not in one changelog.

**Destination.** `C:\Dev\JARVIS\kb\dev\android-device-verification.md` (the mechanism, the commands,
the measurement) **and** a narrowing clause on the device-state rule in
`C:\Dev\JARVIS\user-rules\my-rules.instructions.md` § 📱, plus
`C:\Dev\JARVIS\walkthroughs\device-state-banner.md`, which is the run that would test it.

**Anchors.** `android-device-verification.md` §7 (added 2026-08-19 by `cloud-emulator`) is the
nearest existing section; this is a sibling to it, not a replacement.

**Supersedes.** Narrows — does not delete — the *"cannot share a device session"* half of the
📱 device-state rule, 2026-08-16.

**Status.** ⛔ **PARKED — always-ask in both modes**, on two independent grounds and either alone
is enough: the destination includes `rules/`, which `AUTO MODE` never drains and which the 🎬
walkthrough gate owns; and it **modifies a standing claim**, which is a deletion of committed
knowledge. Ido decides.
