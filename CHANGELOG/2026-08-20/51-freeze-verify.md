# `51-freeze-verify` — 2026-08-20

> **Branch:** `main`
> **Summary:** The `#51` freeze is now **seen**, not asserted — Home, Profile and Analytics all
> render in English on a device whose **primary** locale is Hebrew and whose stored
> `app_language` is `"he"`, with an instrument check proving that stored value was actually
> read. And the brief's other half was already discharged: **all three owed `#51` writes landed
> on 2026-08-17**, hours after the changelog that declared them owed.

The brief `sessions/51-freeze-verify.md` carried two halves. One turned out to be **already
done by someone else**; the other turned out to be **bigger than the brief thought**, because
the device had changed under it in two ways nobody recorded.

---

## 1 · What the brief believed, and what was actually true

Three of the brief's premises were stale. None of them was wrong when written.

| The brief said | `Observed:` 2026-08-20 |
|---|---|
| `Pixel_10_Pro_XL` **is signed out** — 51e's instrumented run uninstalled the app | **Signed in** as `name.iddo@gmail.com`. A later session signed in and did not amend the brief. |
| The device **may hold a stored `"he"`** from before the freeze — *"the one machine in the world where the pre-freeze state can be observed"* | **It holds no `app_language` key at all** — `Observed:`, the file holds only `app_brightness`. `Inferred:` that 51e's uninstall took the SharedPreferences with it, since an uninstall clears app data and the brief itself attributes the sign-out to that run; **not** independently confirmed. Either way the evidence was gone **before** the brief was written. |
| The three `#51` writes are **owed** | **All three are on the issue**, posted 2026-08-17 by `idomarhaim`. |

The signed-in state is what made this session worth running at all: the deferral comment on
`#51` records that the 2026-08-17 render pass could not reach Home, Profile or Analytics
**because `GoalPilotRoot` puts the whole nav graph behind the auth gate** and the device was
signed out. Today it was not.

---

## 2 · The render pass — described, not asserted

`assembleDebug` + `installDebug` first: the installed APK was from `b4f4980` (11:24), three
commits behind `HEAD`. A render pass that verifies a stale build verifies nothing.

### Test A — Hebrew as the **primary** device locale

```
adb root
adb shell settings put system system_locales "iw-IL,en-US"
adb shell setprop persist.sys.locale iw-IL
adb reboot
```

This is the channel `hebrew-defer-freeze` recorded as `Untested:` — *"whether a genuine
device-locale change reproduces the direction defect at all"*, because that session's emulator
was a no-root `user` build and only `LocaleManager`'s per-app locale was reachable. It is also
a **stronger** input than `new-machine-checkup` round 5's, which put Hebrew in the list as a
**secondary** locale.

`Observed:` the **launcher** renders `יום ה׳, 20 באוג׳` — so the device locale really did
change, which is the check that keeps this from being a vacuous pass.

| Screen | What was on it |
|---|---|
| **Home** | Every word English: *Overall progress · Averaged across all your goals · Goals · Tasks done · This week · View analytics · Smart add a task · Describe anything you want to do — GoalPilot files it under the right goal. · e.g. Run 5 km on Friday · Sort · Already done · Import from Google Tasks*. Bottom nav *Home · Goals · Social · Profile*. |
| **Analytics** (`feature/analytics`, swept) | *Day · Week · Month · Quarter · Year · Where your time goes · Aug 16 – Aug 22 · share of your tracked time per life area · How it moves · Tracked time per day, stacked by life area. · Progress by goal · How far along each goal is.* Both empty states in English. |
| **Profile** | *Profile · Level 1 · 70 pts · 30 pts to level 2 · Your friend code · Share it so friends can add you · Life areas · Analytics · Challenges · Sign out.* |

**Layout was LTR on all three** — nav ordered Home→Goals→Social→Profile left-to-right, the
`Level 1` chip on the right, the donut on the left, bars filling left-to-right, the Analytics
back-arrow top-left. That is the half that **failed** on 2026-08-17 (*"every word rendered in
English inside a fully mirrored, right-to-left layout"*) and was fixed by deriving direction
from the clamped target locale. It is now confirmed through the device-locale channel the fix
was never tested against.

**The only Hebrew on screen was user content** — the display name `עידו מר-חיים`, the greeting
`Hi עידו 👋`, the avatar initial `ע`. That is §8's *content never moves*, working.

Percentages came back through `uiautomator` as `⁨0%⁩`, `⁨149%⁩` — the U+2068/U+2069 isolate pair
`51e` added to `SimpleBarChart`'s `trailing`, present and doing its job. Rendered as `0%`, not
`%0`.

### Test B — the persistence door, on a **constructed** pre-freeze state

The device had no stored `"he"` to find, so one was written into
`/data/data/com.idomarhaim.goalpilot.debug/shared_prefs/goalpilot_ui_prefs.xml` by hand. **Say
what that is:** a reconstruction of the pre-freeze state, not a found one. It is the exact
input `offeredFromId` takes, but it is not archaeology.

`Observed:` stored `app_language=he` **and** Hebrew-primary device locale together → the app
still rendered entirely in English, LTR.

### The instrument check, which is what makes Test B mean anything

A pass here has an obvious silent failure: if the app never read that file, the result is
identical and the test proves nothing. So the same file was rewritten with
`app_brightness=dark` **beside** the `app_language=he`, and the app relaunched.

`Observed:` **the app came up dark.** That read is the same `getSharedPreferences` call, in the
same startup path, from the same file — so the file was read, and the `"he"` it carried was
read with it. English survived it.

That converts `#51`'s door 3 from *proven as logic by JVM tests* to *seen*.

### Restored

Prefs restored from the backup taken before the first write (`app_brightness=system`, no
language key, backup deleted); `system_locales` back to `en-US,iw-IL` and `persist.sys.locale`
back to `en-US` — the state `new-machine-checkup` round 5 established for Ido's *"both
languages together"*. Verified after reboot: light theme, English, sign-in intact.

**No instrumented suite was run**, deliberately — `connectedDebugAndroidTest` uninstalls the
app and would have taken the Google account with it, which is the collision the device-state
rule exists for.

---

## 3 · The three `#51` writes were already posted — by whom, and when

`hebrew-defer-freeze`'s changelog §6 says both GitHub writes were denied by the harness
classifier, and lists three owed items. **All three are on the issue**, and the timestamps say
they landed the same evening:

| Owed item | Landed | Evidence |
|---|---|---|
| 1 · `51e-sweep-components`' comment | comment `5318172476`, 2026-08-17T17:26:26Z | Diffed against `CHANGELOG/2026-08-17/51e-sweep-components.md` lines 248–347 — **byte-identical**. |
| 2 · the deferral comment | comment `5318401943`, 2026-08-17T17:59:13Z | *"`#51` deferred 2026-08-17 — and the freeze leaked in the axis nobody was watching"*. |
| 3 · the body edit | body `updated_at` 2026-08-17T18:26:59Z | Line 2 now opens *"⚠️ **It WAS a precondition of every screen ticket; as of 2026-08-17 it is not.**"* |

So a session after `hebrew-defer-freeze` — the one whose render pass the deferral comment
describes — got the writes through and **did not update the brief**, which is why this session
was queued to redo them. Checking the tracker before writing to it cost one unauthenticated
`curl`.

### Item 3 was half-done, and the missed half is the one that re-blocks

The body edit rewrote the **top** of the issue and left the **bottom** standing. `## Sequencing`
still read:

> **Before, or alongside, the first screen ticket.** Every one of #2, #7, #9, #10, #11 and the
> settings surface owes §0.8 …

That is the same re-blocking claim the edit existed to remove, surviving 57 lines further down —
and it is now doubly stale, because `#7`, `#9` and `#11` have since **closed**. A session that
reads an issue to the end gets re-blocked by the paragraph the fix did not reach.

**Decision, recorded as mine** (per the derivable-decision rule; the brief authorises *"the body
edit dropping the precondition claim"* and this is that claim): **annotated, not deleted.** The
top of the body was handled that way by whoever made the edit — the filed sentence kept, a dated
suspension notice above it — and deletions are always-ask regardless. Matching that precedent
keeps the issue readable as a record of what was believed when.

---

## 4 · What this session wrote to `#51`

One comment, recording the render pass, because two items it closes are named as open on the
thread and nowhere else. `#51` **stays open** — eight packages are still unswept and that is
the whole of what the deferral parked.

---

## 5 · A defect seen in passing, not this ticket's

Analytics → *Progress by goal* shows **`Strength Training 224415%`** and **`Sleep 7 hours 149%`**
with bars pinned full. Goal progress is not clamped to 100%. `Observed:` on the render pass;
**not investigated**, not filed, and out of this brief's scope (`app/src/` changes only if the
render pass found a *locale* leak). Recorded here so it is not lost — it is a real number on a
real screen and it is wrong.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| JVM unit | **not re-run** — no code changed. The brief's `Exit` requires it only if code changed. |
| Instrumented | **not run, and deliberately so** — `connectedDebugAndroidTest` uninstalls the app and would destroy the Google sign-in that made this session's render pass possible at all. |
| `assembleDebug` / `installDebug` | ✅ green — run to put `HEAD` on the device rather than the three-commits-old APK that was there. |
| Device / manual render | ✅ Home, Profile, Analytics all English, all LTR, with `persist.sys.locale=iw-IL` **primary**. |
| Device / persistence door | ✅ stored `app_language=he` → still English; **and** the read proven live by a `dark` probe through the same file. |
| Firestore rules | n/a — nothing touched. |

Screenshots and UI dumps in this session's scratchpad; the state is reproducible from the four
`adb` commands in §2.

---

## 📋 What this leaves

- **`#51` stays open.** Unswept, unchanged: `auth`, `challenges`, `dashboard`, `goals`,
  `health`, `lifeareas`, `profile`, `social`.
- **The freeze's own claim is no longer `Unverified:`.** All three doors have now been seen on a
  device: the picker (JVM), the `SYSTEM` clamp (here, primary locale), the persistence read
  (here, constructed input with a live-read proof).
- **The unclamped goal percentage in §5 is unowned.**
