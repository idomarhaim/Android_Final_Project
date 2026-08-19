# 2026-08-15 — `social-share-bugs`

`/kickoff social-share-bugs` — issues
[`#4`](https://github.com/idomarhaim/Android_Final_Project/issues/4) (a shared photo cannot be
opened, and has no content description) and
[`#5`](https://github.com/idomarhaim/Android_Final_Project/issues/5) (a user cannot delete a share
they made). One pass over the Social feed card, as the brief argued: they are two things you cannot
do to a card that responds to nothing.

Mode: **`AUTO MODE`** (the brief said `normal`; this session's opening message wins).

---

## What was wrong, and what is now true

The reproduction for both bugs was the same sentence: **the feed card contained no interactive node
at all**, and the screen was pixel-identical after every tap. It now contains two — the photo, and
an overflow button on your own post — and `SocialFeedUiTest` asserts that count directly, on the
device, because that count *is* the text of both reports.

### `#4` — two faults, fixed as two things

1. **The photo opens.** `AsyncImage` gained a click handler onto a new
   [`FullScreenPhotoDialog`](app/src/main/java/com/idomarhaim/goalpilot/feature/social/PhotoViewer.kt) —
   pinch-zoom to 5×, double-tap to 2.5×, pan clamped so a zoomed photo cannot be flung out of view,
   and a labelled close button. A `Dialog` rather than a nav route on purpose: the only argument is
   a download URL, and a URL in a route has to be encoded in and decoded out — two places for
   Firebase's `?alt=media` token to be mangled, bought with a back-stack entry nobody wants for a
   photo.
2. **The photo is announced.** `contentDescription` was `null`, which is the API's way of saying
   *decorative*, so a screen reader read the post with the picture simply missing. It now reads
   "Photo shared by <author>" — deliberately not the headline or message, which are their own text
   nodes and would be read twice.

The second does not follow from the first, exactly as `#4` says: the viewer would have re-committed
the same omission on its own image, so `FullScreenPhotoDialog` takes a **non-nullable**
`contentDescription`.

### `#5` — all five layers, and one of them turned out to be two

| `#5`'s step | State |
|---|---|
| 1. repository method | **Added** — `SocialRepository.deleteShare(shareId, imageUrl)` + impl |
| 2. `firestore.rules` author-only delete | **Already there**, unchanged since `1e56ee3` — see below |
| 3. rules test in `firestore-tests/` | **Added** — 8 `shares` tests, and 6 more for Storage |
| 4. UI affordance | **Added** — overflow menu on your own post, with a confirmation |
| 5. delete the image alongside the post | **Added** — and it did not work, for a reason no Kotlin layer could see |

**Step 2 needed no change.** `match /shares/{shareId}` has carried
`allow update, delete: if isSignedIn() && resource.data.authorUid == request.auth.uid` since the
challenges work. The brief and the issue both assumed this had to be written; it did not. What was
missing was any test that it was there — which is why nobody knew.

**Step 5 was broken at the rules layer, and the test is the only reason it is not still broken.**
`storage.rules` had a single `allow write` clause guarding `request.resource.size` and
`request.resource.contentType`. `write` also covers **delete**, and a delete sends no object — so
`request.resource` is null, both guards raise, and the rule denies. *The owner could not delete their
own image.* `SocialRepositoryImpl.deleteShare` would have run, reported success, and left the photo
in Storage forever. Split into `allow create, update` (guards intact) and `allow delete` (owner, no
conditions).

`Observed:` the emulator names the line itself — `storage.rules line [12], column [12]. Null value
error.`

### Two design decisions worth the ink

- **Delete order is post-then-photo, and the order is the argument.** What the user asked for is
  that the post stop existing, so that is what must not be left undone. A failed image cleanup
  leaves an orphan nobody can see; the other order can leave a *visible* post pointing at a photo
  that is already gone.
- **`SharedItem.isMine` is stamped by the repository from `uidFlow()`**, not derived in the UI from
  the leaderboard. The leaderboard is a bounded top-100, so a user outside it would silently lose
  the ability to delete their own posts. `uidFlow()` and not `auth.currentUser` for the reason
  `AGENTS.md` already gives: the Flow is built once at ViewModel-creation time.

---

## 🧪 Tests

**Every layer this touches ran. Counts are from the run, not from memory.**

| Layer | Command | Result |
|---|---|---|
| Security rules (Firestore **and** Storage) | `cd firestore-tests; npm test` | **30 pass, 0 fail** (was 16 tests; +14) |
| Server unit / integration / endpoints | — | **No such layer** for this change. `functions/` is the GROQ proxy and is untouched. |
| Client JVM unit | `.\gradlew :app:testDebugUnitTest` | **218 pass, 0 fail, 0 skipped** across 24 suites (+5 new in `SocialViewModelTest`) |
| Client UI (instrumented, Compose) | `.\gradlew :app:connectedDebugAndroidTest` | **39 pass, 0 fail** on `Pixel_10_Pro_XL` (+10 new in `SocialFeedUiTest`) |
| Full-app device reproduction | manual | **Was blocked; now PASSED** — Ido signed in later the same day. See the addendum at the end of this file, which supersedes the "does and does not" section below |

### The non-vacuity check, run and then thrown away

`AGENTS.md`'s standing warning — *"run the new suite against the old rules too: pure negative tests
pass vacuously when nothing matches at all"* — was paid, not asserted. A throwaway
`vacuity-check.mjs` degraded both rulesets to their pre-`#5` state (`/shares` block stripped;
`storage.rules` back to one `allow write`) and asserted the two *positive* tests now fail:

```
✔ OLD RULES: the author canNOT delete their own post (3341ms)
!  com.google.firebase.rules.runtime.common.EvaluationException:
   Error: storage.rules line [8], column [12]. Null value error.
✔ OLD RULES: the uploader canNOT delete their own image (459ms)
ℹ tests 2  ℹ pass 2  ℹ fail 0
```

Both pass, so both new `assertSucceeds` cases are load-bearing. The script was deleted; it is
reproduced in full in this session's KB candidate rather than left in the repo as a suite that only
ever tests history.

### What "device-verified" does and does not mean here

`Observed:` the ten new `SocialFeedUiTest` cases ran the **real `FeedCard` composable on the real
`Pixel_10_Pro_XL`**, and `onAllNodes(hasClickAction())` returned **2** where both issues measured
**0**. That is the same instrument on the same device against the same composable.

`Untested:` the end-to-end path — sign in as Ido, open the Social tab, tap a real post's photo. The
app installs and launches (`com.idomarhaim.goalpilot.debug/…MainActivity`, confirmed in
`dumpsys window`) and stops at the Google sign-in screen. Reaching a feed post with an attached
image needs Ido's own Google account, which is his to use. **`#4` and `#5` are therefore left open**,
per the brief: *"close `#4` and `#5` only after the device re-verification."*

> ⬆️ **Superseded the same day — see the addendum at the end of this file.** Ido signed in on the
> emulator, the reproduction was re-run against the running app, and it **passes**. This paragraph is
> left standing rather than rewritten because it is the honest record of what was true when the work
> was committed; only its verdict has moved. **Addendum 2 then closed the last gap too** — the live
> round-trip was proven with a throwaway post and `gsutil` against the live bucket, so nothing in
> this session remains `Untested:`.

---

## ⚠️ Two things that are not this session's work but blocked it

- **The pinned JDK 21 is a wreck on this machine.**
  `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` holds an orphaned `lib/` and **no
  `bin/java.exe`**. Both `gradle.properties`' `org.gradle.java.home` and the machine `JAVA_HOME`
  point at it, so Gradle died before reading a build file (*"JAVA_HOME is set to an invalid
  directory"*) and `firestore-tests` fell back to the JDK 17 on `PATH` and refused to start
  (*"firebase-tools no longer supports Java version before 21"*). Repointed to the intact
  `jdk-21.0.12.8-hotspot`; the pin's intent is unchanged. **`JAVA_HOME` itself is still wrong** —
  that is a machine setting, not a repo one, and it is Ido's to fix. Note `AGENTS.md` and
  `CLAUDE.md` both say the machine default is JDK **25**; it is now the broken 21.
- **The `firestore-tests` suite now needs the Storage emulator**, so `package.json`'s test script
  is `--only firestore,storage`. `firebase.json` already configured port 9199, so nothing else moved.

---

## ⛔ Not done, and why

- ~~**`storage.rules` is not deployed to live `goalpilot-56e30`.**~~ **RESOLVED the same day.** A
  rules deploy is an outward action and always-ask in both modes, so it waited for Ido's word; he
  gave it, and `firebase deploy --only storage` reported `released rules storage.rules to
  firebase.storage`. Addendum 2 then proved the deployed rule actually accepts the author's delete,
  which is the claim the deploy alone could not support.
  The sub-item — *whether the live project's firestore rules already carry the author-delete clause*
  — is answered too, and by the same test: the throwaway post's **document** was deleted by the
  client, which the deployed firestore rules had to permit for the post to leave the feed.
- **A failed image cleanup is not retried and not surfaced.** `deleteShare` discards
  `deleteImage`'s result by design (the post *is* gone; reporting failure would invite a retry that
  can only fail again). The honest consequence: a Storage error at that moment leaves an orphan
  object and nobody learns. Fixing it properly wants a cleanup job, not a bigger error message.
- **`C21`'s two additions to this screen** — an *as-of* stamp on cross-boundary numbers and a
  "Not loaded yet" empty state — are untouched, as the brief instructed. The header row now lays out
  as `avatar | name+time (weight 1f) | overflow`, which leaves both of them room.

---

## Files

**Changed**

- `app/src/main/java/com/idomarhaim/goalpilot/feature/social/SocialScreen.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/feature/social/SocialViewModel.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/domain/model/Social.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/domain/repository/SocialRepository.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/domain/repository/StorageRepository.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/data/firestore/SocialRepositoryImpl.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/data/storage/StorageRepositoryImpl.kt`
- `storage.rules`
- `firestore-tests/rules.test.mjs`, `firestore-tests/package.json`
- `gradle.properties`

**New**

- `app/src/main/java/com/idomarhaim/goalpilot/feature/social/PhotoViewer.kt`
- `app/src/test/java/com/idomarhaim/goalpilot/feature/social/SocialViewModelTest.kt`
- `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/SocialFeedUiTest.kt`

**Unchanged, and that is a finding** — `firestore.rules`.

---

# Addendum — the signed-in device pass (2026-08-15, ~15:1x)

Ido signed in on `Pixel_10_Pro_XL`, which lifted the one blocked Exit condition. The reproduction
was re-run **the way both issues were reported** — `uiautomator dump`, against the running app, on
his own real feed posts. Emulator claimed on the board before the first device command; `#12`/`#44`
were untouched.

## The reproduction, inverted

`#4` and `#5` both reported: *"the accessibility tree shows **zero interactive nodes in the entire
feed card**"*, and *"the last `clickable="true"` node on the screen is the 'Challenges' link
**above** the feed."*

`Observed:` the Social tab's tree now contains, inside the feed card:

```
CLICKABLE | desc='Photo shared by עידו מר-חיים' | [96,2381][1248,2680]
          | desc='Post options'                 | [1140,1599][1212,1671]   (Aug 8 post)
          | desc='Post options'                 | [1140,2101][1212,2173]   (Aug 6 post)
```

Both of `#4`'s faults are visible in that one line: the photo is `clickable` **and** it carries a
content description naming its author, where it previously had neither.

## Every affordance driven, end to end

| Step | Result |
|---|---|
| Tap the photo | Full-screen viewer opens — the tree replaces the whole feed with `desc='Photo shared by עידו מר-חיים' [0,159][1344,2992]` plus `desc='Close photo'`. The screen is emphatically **not** pixel-identical, which is what `#4` measured (PSNR ∞ / MSE 0.00) |
| Double-tap | **Zooms to 2.5× and stays centred** — confirmed by screenshot, not by node bounds |
| `Close photo` | Returns to the feed with scroll position intact |
| Tap `Post options` | Menu opens with exactly one item, **`Delete post`** |
| Tap `Delete post` | Confirmation: *"Delete this post? It will disappear from everyone's feed, **and the attached photo will be deleted too.** This cannot be undone."* — the photo-carrying variant, correctly chosen for a post that has one |
| `Cancel` | Dialog dismissed; **1** photo and **2** `Post options` buttons still in the feed. Nothing destroyed |

**One instrument was inconclusive and is recorded as such rather than reported as a pass.** Node
bounds after the double-tap were unchanged at `[0,159][1344,2992]` — `graphicsLayer` is a draw-time
transform, so uiautomator bounds cannot distinguish *"the gesture did not register"* from *"it
zoomed"*. The screenshot settles it; the bounds never could.

## What is still not verified, and it needs a destructive act

`Untested:` the **live round-trip** — that the deployed `storage.rules` actually lets the author's
image delete through against `goalpilot-56e30`. The emulator suite proves the ruleset is right and
`firebase deploy --only storage` reported `released rules storage.rules to firebase.storage`, but
the only way to observe the deployed rule accepting a real delete is to **delete one of Ido's real
posts and its photo**, which is irreversible. Not done; his call.

`#4` is fully re-verified. `#5`'s affordance and its refusal path are re-verified; its *effect* is
not.

---

# Addendum 2 — the live round-trip, proven from outside the app (2026-08-15, ~15:3x)

Ido authorised the **non-destructive** form of the test: create a throwaway post with a photo, then
delete that one. His two real posts were never touched.

## What was done

1. Home → **Share with photo** → the Android photo picker → selected **one of this session's own
   screenshots** (`Photo taken on Aug 15, 2026 3:14 PM` — an artifact this session created, not
   Ido's personal content) → **Done**.
2. The post appeared at the top of the feed: *"Earned 0 pts • 0 tasks done • avg 26% across 8
   goals"*, with a `CLICKABLE desc='Photo shared by עידו מר-חיים'`. `Observed:` Coil fetched the
   image with `last-modified: Sat, 15 Aug 2026 15:26:08 GMT`, so the object existed in live Storage.
3. `Post options` → `Delete post` → **Delete**.
4. Feed re-read: the throwaway post is gone; Ido's Aug 8 and Aug 6 posts remain, the Aug 6 photo
   still loading.

## The instrument, and the two that failed first

The whole point was to observe the **deployed** `storage.rules` accepting the image delete — and the
app cannot tell you that, by design: `deleteShare` treats image cleanup as best-effort, so success
and failure look identical on screen. Two instruments were tried and **discarded as uninformative
rather than read as passes**:

- **Coil's disk-cache metadata** holds the response headers but **not the request URL**, so the
  download URL (and its token) could not be recovered — and without the token an HTTP GET returns
  403 whether or not the object exists, which is not a test.
- **logcat** showed zero Storage errors — and zero app-tagged lines **at all** during the delete
  window. The app logs nothing either way at default level, so "no error" was *silence*, not
  evidence. Recorded here because reading it as a pass is exactly the failure this session's KB page
  warns about.

## The instrument that settled it: `gsutil`, against the live bucket

Out-of-band, from Google Cloud Storage itself rather than from the app:

```
$ gsutil ls -lr gs://goalpilot-56e30.firebasestorage.app/
  147443  2026-07-31T13:51:11Z  .../progress_images/cTmjUK.../d918581e-....jpg
  186736  2026-08-06T01:33:50Z  .../summary_images/cTmjUK.../5c6d1b9c-....jpg
```

**Two objects in the entire bucket, and neither is from today.** A filter for `2026-08-15` returns
nothing. The image uploaded at 15:26 — which demonstrably existed, because the app rendered it and
Coil cached it with that exact `last-modified` — **is gone from live Storage**, and the 2026-08-06
object (186,736 bytes, matching Coil's cached body size for Ido's Aug 6 photo byte-for-byte) is
untouched.

**So the deployed rule accepts the author's delete, and `#5`'s step 5 works in production.** Had
`storage.rules` still carried the single `allow write` clause, that object would still be sitting
there — which is precisely the silent failure the whole ticket was about.

## Verdict

| | |
|---|---|
| `#4` | **Fully verified** — reproduction inverted on the running app, viewer opens/zooms/closes, photo announced |
| `#5` | **Fully verified** — affordance, refusal path, *and* the live round-trip including Storage |

Both issues are closable. Nothing in this session is now `Untested:`.

---

# Addendum 3 — the `JAVA_HOME` breakage, fixed (2026-08-15, ~15:5x)

Ido asked for the JDK fault to be handled. It turned out to be **three faults**, not one, and only
the first is what he'd been told about.

## What was actually wrong

| | |
|---|---|
| **User `JAVA_HOME`** | `jdk-21.0.11.10-hotspot` — an orphaned `lib/`, **no `bin/java.exe`**. Gradle: *"JAVA_HOME is set to an invalid directory"* |
| **Machine `JAVA_HOME`** | `jdk-17.0.20.8-hotspot` — intact, but JDK **17**, which AGP rejects |
| **Machine `PATH`** | offers JDK **17** before JDK 21, so `java` resolves to 17 |
| **User `PATH`** | three JDK `bin` entries pointing at directories that **do not exist** (`jdk-25.0.1.8`, `jdk-21.0.9.10`, `jdk-17.0.16.8`) |

Adoptium inventory: `21.0.11.10` and `21.0.6.7` are **wrecks** (no `java.exe`); `17.0.20.8`,
`21.0.12.8` and `25.0.4.7` are intact.

## Fixed

**User `JAVA_HOME` → `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot`.** User scope
overrides Machine scope, so no admin was needed. `Observed:` from a shell carrying only the
persisted environment, `gradlew --version` now reports `Launcher JVM: 21.0.12`, where it previously
refused to start.

## Could not be fixed from here — and why the repo was fixed instead

`java` on `PATH` is still JDK 17, and that ordering lives in the **Machine** `PATH`, which needs
administrator rights. That matters because **`firebase-tools` ignores `JAVA_HOME` and reads `PATH`**
— so `firestore-tests` still died with *"firebase-tools no longer supports Java version before 21"*
even after `JAVA_HOME` was correct.

Rather than leave that, the dependency was removed: **`firestore-tests/run-tests.mjs`** prepends
`JAVA_HOME/bin` to `PATH` for the emulator's child process only. `npm test` now runs on a machine
whose `PATH` prefers 17, needs no admin, and honours the convention `AGENTS.md` already states.

`Observed:` **30 pass, 0 fail** from a shell where `java` on `PATH` is `jdk-17.0.20.8-hotspot`.

**Both fallback branches were exercised rather than assumed** — the control-arm discipline this
session already wrote up:

- `JAVA_HOME` unset → *"JAVA_HOME is not set — using whatever `java` is on PATH"*, then firebase's own refusal.
- `JAVA_HOME` naming a wreck (this machine's exact fault) → *"JAVA_HOME is set to … but there is no java there. Falling back to PATH; if the emulators refuse to start, that is why."*

The second is the diagnostic that did not exist this morning, and its absence is what made the
original failure read as a firebase-tools problem.

## Two bugs found while writing the wrapper, both worth knowing

- **Windows env vars are case-insensitive, and Node returns `Path`, not `PATH`.** Writing
  `env.PATH` **adds a second key**; the spawned `cmd.exe` reads the untouched original. The visible
  symptom was `'firebase' is not recognized` — because the key that won was the one npm had *not*
  augmented with `node_modules/.bin`. The fix is to find the existing key case-insensitively.
- **`spawn(cmd, argsArray, { shell: true })` concatenates without quoting**, so the script argument
  `"node --test"` lost its quotes and firebase parsed `--test` as its own flag
  (*"error: unknown option '--test'"*). Passing one command string fixes it and clears Node's
  args-plus-shell deprecation warning.

## Docs corrected

`AGENTS.md` and `CLAUDE.md` both claimed *"the machine's `JAVA_HOME` is JDK 25"*. That was **false**
— it was a broken JDK 21 — and the claim had been standing long enough to be copied into two files.
Both now state the real trap: **Gradle reads `JAVA_HOME`, firebase-tools reads `PATH`, and they can
disagree.**

## Left for Ido — needs admin

Reordering the Machine `PATH` so a JDK 21 precedes JDK 17, and removing the three phantom user
`PATH` entries. Neither is required for this repo any more; both would make every *other* tool on
the machine behave. **Not attempted:** deleting the two wrecked Adoptium directories — that is a
deletion, and it is his.
