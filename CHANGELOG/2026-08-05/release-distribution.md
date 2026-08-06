# Changes — 05/08/2026 · session `release-distribution`

> **Branch:** `feat/goalpilot-implementation`

Two questions started this: *can I send someone an APK on WhatsApp?* and *can the
people who already have it get an update prompt when I push?* The first was
already true and not worth much; the second was false, and making it true is what
this session actually built.

An app that is not on Google Play gets **no** update mechanism from Android — no
auto-update, no notification, nothing. So the deliverable is the mechanism:
Firebase App Distribution on both ends, a real signing key underneath it, and a
tag-triggered workflow that puts a build in front of testers without anyone
opening Android Studio.

## 🔑 The signing key had to come first

`app/build.gradle.kts` signed release builds with the **debug** key — a deliberate
convenience noted in a comment as "replace before publishing". It cannot be
replaced *after* publishing: Android identifies an app by its signature, so the
first build handed to someone locks in the key forever, and switching later means
every user uninstalls and loses their local state.

So `scripts/new-release-keystore.ps1` generates the real key before anything is
distributed. It:

- refuses to overwrite an existing keystore (replacing a release key is the
  unrecoverable move, so the script does not offer it);
- generates a 32-char alphanumeric password — **alphanumeric deliberately**: the
  password travels through a Gradle property, a GitHub secret and a base64
  round-trip, and every quoting layer in that chain is a chance to mangle
  punctuation;
- writes the credentials straight into git-ignored `local.properties` and
  **never prints them**, so a signing password does not end up in a terminal
  scrollback or an agent transcript;
- prints the SHA-1 (public) plus the `firebase apps:android:sha:create` command
  that has to follow.

Signing config now resolves from `local.properties` **or** the environment (CI),
and falls back to the debug key when neither is present so a fresh clone still
builds. The fallback is local-only: a `doFirst` check fails
`appDistributionUpload*` outright rather than warning, because a debug-signed APK
in a tester's hands is not discoverable until the day you try to update it.

## 📲 The update prompt

`firebase-appdistribution` is split into an `-api` stub and a full
implementation, and the split is load-bearing rather than tidy:
`implementation(…-api)` compiles into every variant and does nothing, while
`releaseImplementation(…)` adds the real updater to release builds only. A build
that never reaches a tester therefore carries no updater at all, and a developer
running from source is never interrupted by an update dialog.

`core/update/AppUpdateChecker.kt` is one call —
`updateIfNewReleaseAvailable()` owns sign-in, the version comparison, the dialog,
the download and the install — guarded so it fires once per process. The guard is
not paranoia: `MainActivity` is recreated on every configuration change, so
without it a rotation mid-download restarts the flow on top of itself.

It is hooked from `MainActivity.onCreate` rather than `ui/root/`, because the SDK
drives its own dialogs off the foreground Activity, outside Compose. (`ui/root/`
was also owned by a live sibling session at the time; the Activity was the right
home regardless.)

## 🏷️ Why the workflow is tag-triggered, not push-triggered

`.github/workflows/release.yml` was asked for as "on push to main". It ships on
`v*` tags plus `workflow_dispatch` instead, because `versionCode` is bumped by
hand: a push-triggered job would publish build after build carrying the *same*
`versionCode`, App Distribution would accept every one, testers would be notified
every time, and not one of those notifications would describe an installable
update. A tag is the signal that a version number was actually decided.

Two things in the workflow are non-obvious:

- **`org.gradle.java.home` has to be stripped on the runner.** `gradle.properties`
  pins it to a Windows JDK 21 path because this machine's `JAVA_HOME` is JDK 25,
  which AGP rejects. Correct locally, fatal on `ubuntu-latest`. The line is
  deleted in CI rather than removed from the repo, where it earns its keep daily.
- **The APK's signature is verified with `apksigner` after assembling.** An
  unsigned or debug-signed APK assembles perfectly happily; catching it in CI
  beats catching it on a tester's phone.

Uploads use a **service account**, not a `firebase login:ci` token — those are
deprecated. The APK is also attached to the workflow run, so a build can still be
shared by hand with someone who is not in the tester group yet.

## 📓 What the docs now say

[`docs/RELEASING.md`](../../docs/RELEASING.md) is new and is the whole story:
one-time setup, the per-release checklist, the six GitHub secrets, the traps, and
a blunt walk-through of what a tester actually experiences — five taps and a red
Play Protect dialog on first install, three taps on every update after that.
[`docs/OPERATIONS.md`](../../docs/OPERATIONS.md) gains a short §4a pointing at it
with the two facts worth knowing even if you never cut a release.

The honest answer to the WhatsApp question is in there too: you *can* attach the
APK, but a hand-sent APK is invisible to App Distribution, so that person is
outside the update flow permanently. Send the invitation, not the file.

## 🧪 Tests

No new test layer — this session added no behaviour that a test can observe.
`AppUpdateChecker` is one delegating call into a Firebase singleton whose only
locally reachable outcome is the debug no-op; a unit test around it would assert
that a mock was called, which is the test asserting its own setup. What the
session actually risked breaking was the **build**, so that is what was verified.

| Layer | Result |
|---|---|
| Gradle configuration (`:app:tasks`) | ✅ — surfaced a real defect: plugin 5.x deprecates a bare `firebaseAppDistribution { }` inside a buildType. Fixed by importing `com.google.firebase.appdistribution.gradle.firebaseAppDistribution` |
| JVM unit (`:app:testDebugUnitTest`) | ✅ **197 tests, 0 failures, 0 skipped** |
| Release assembly (`:app:assembleRelease`, R8 + resource shrinking) | ✅ `BUILD SUCCESSFUL in 9m 13s` → `app-release.apk`, 4.59 MB |
| Instrumented | not run — no UI changed, and the emulators belonged to the sibling session for most of this one |
| Firestore rules | not run — no rules file touched |

**Both signing paths were exercised, in order, which is the only way to prove
either.** The first `assembleRelease` ran with no keystore on the machine:
`validateSigningRelease` resolved to the debug config and produced an installable
APK, which is the property the fallback exists to preserve for a fresh clone.
`scripts/new-release-keystore.ps1` then created the real key, and a second
`assembleRelease` produced an APK whose `apksigner` output reads
`CN=Ido Marhaim, OU=GoalPilot, …` with SHA-1 `e7:d5:53:4c:…:90:62` — byte-identical
to the keystore's own fingerprint. The credentials resolve, the config switches
over automatically, and `git check-ignore` confirms `*.jks` keeps the key out of
the index.

### 🐛 The script did not run the first time

`new-release-keystore.ps1` failed to **parse** on its first invocation:

```
The string is missing the terminator: '.
```

Nothing in it involves a stray quote. Windows PowerShell 5.1 decodes a BOM-less
`.ps1` as **ANSI, not UTF-8**, and the em dash in `"NEXT — Google Sign-In…"` is
`E2 80 94`, which as CP1252 is `â€"` — that third character is a double quote,
which closes the string 40 characters early and leaves the rest of the line
parsing as garbage. The error names a symptom three tokens downstream of the
cause, which is what makes it worth writing down.

Fixed by making the script **pure ASCII** rather than by adding a BOM: a BOM is
one careless re-save away from being lost, and every editor and tool in this
repo's path handles ASCII identically. A note in the script's header says so, so
the next person does not "tidy up" the hyphens back into em dashes.

**Not verified, and it cannot be verified from here:** that a tester receives the
prompt. That needs the key created, its SHA-1 registered on `goalpilot-56e30`, a
`testers` group, and *two* release builds with different `versionCode`s. Every one
of those is an outward action on live infrastructure, so none was taken.

## 🚀 The live setup, and the first real release

Committed in `5316782`, this section covers what followed: the infrastructure the
code above needs in order to do anything, all of it run from the CLIs already
authenticated on this machine rather than through the consoles.

- **Release SHA-1 registered** on the Firebase release app
  (`…:android:b5d15ee7…`). The app now carries two SHA-1s — the original debug
  `f1d0964d…` and the release `e7d5534c…` — which is additive, so debug builds
  and the emulators are unaffected.
- **`google-services.json` refreshed**, and this one deserved care.
  `apps:sdkconfig` is per-app, and the committed file describes **both** the
  release and the `.debug` app; a naive overwrite would have silently dropped the
  debug entry and broken every developer build. The fetched file was diffed
  before replacing: it turned out to be a strict superset — one added
  `oauth_client` for the release certificate, nothing removed.
- **`testers` group created** with both existing test accounts. The alias
  (`testers`, not the display name `Testers`) is what `app/build.gradle.kts` and
  the workflow pass, so the two now agree.
- **Service account `github-appdistribution@`** created with
  `roles/firebaseappdistro.admin`, binding verified by reading the IAM policy
  back rather than trusting the write.
- **Six GitHub Actions secrets set** via `gh secret set`. The service-account
  JSON had to go in over **stdin**: passing multi-line JSON as a PowerShell
  `--body` argument word-splits it into five arguments. Its key file was deleted
  from the scratchpad immediately afterwards.

Then `versionCode 1 → 2`, `versionName 0.1.0 → 0.2.0`, tagged `v0.2.0` — the
first build that exists to be *distributed* rather than to be run from a cable.

### 🐛 `v0.2.0` died in 11 seconds: `./gradlew: Permission denied`

`gradlew` was committed from Windows, which carries no POSIX executable bit, so
`ubuntu-latest` refused to run it (exit 126). Nothing local ever catches this —
`gradlew.bat` is what runs on this machine, and the wrapper's mode is invisible
until a Linux runner tries to execute it.

The tidy fix is `git update-index --chmod=+x gradlew`, which restores the mode
`gradle wrapper` generates. It was **not** taken: `gradlew*` is frozen in
AGENTS.md, and a frozen path is not somewhere to make a judgement call
unilaterally. The workflow does `chmod +x ./gradlew` after checkout instead —
one line, no frozen file touched, and correct regardless of what the repo's mode
bits say.

`v0.2.0` is therefore a **burned tag**: it published nothing, and it is left in
place rather than moved, because re-pointing a pushed tag rewrites remote history
for everyone who already fetched it. The release continues at `versionCode 3` /
`v0.2.1`, which costs nothing when the tester count is two and no build has ever
shipped.

### ✅ `v0.2.1` shipped

Run `31114738682`, all 13 steps green, and checked against something other than
the exit code — a workflow can succeed while uploading nothing:

- `uploaded new release 0.2.1 (3) successfully` → `distributed to testers/groups
  successfully` in the CLI output;
- `appdistribution:group:list` independently reports the `testers` group at
  **release count 1**, queried afterwards rather than read from the run;
- the in-workflow `apksigner` step printed
  `CN=Ido Marhaim, OU=GoalPilot, …` / SHA-1 `e7d5534c…` — the release
  certificate, so the debug-key fallback did **not** silently take over on a
  machine where the keystore arrives base64-decoded out of a secret.

That last one is the check worth keeping. Everything else about the pipeline
fails loudly; a debug-signed APK is the one failure that succeeds quietly and
only surfaces months later, when the update that cannot install lands.

**Still unproven:** the update prompt itself. It needs `v0.2.1` installed on a
device and then a *second* release above it, which no amount of CI can
substitute for.

### 📱 `v0.2.1` installed on a real phone

Two things that only a device could establish:

- **Google Sign-In works under the release key.** The SHA-1 was registered
  server-side and, until this install, never exercised — a registration that had
  silently failed would look identical from here and only surface as
  `ApiException: 10` on someone else's phone.
- **Play Protect blocks the first install**, exactly as §4 of the release doc
  predicted, and the dialog *hides its escape hatch*: the visible button is
  "הבנתי", which cancels. "Install anyway" only appears after expanding
  "פרטים נוספים". Worth knowing, because a tester who taps the obvious button
  concludes the app is broken. It fires because the signing key was hours old
  and Google has no reputation for it; it does **not** fire on updates, which is
  the entire point of what was built.

`versionCode 4` / `v0.2.2` follows purely to put a second release above the
installed one, since that is the only way the prompt can be observed at all.

## 🧭 Sessions

Ran alongside `health-autosync`, which owned `ui/root/`, `feature/dashboard/`,
`data/prefs/`, `domain/usecase/` and `#gradle-daemon`. Nothing here touched those
paths. `SESSIONS.md` was leased rather than claimed and came back `BLOCKED` on the
first attempt; the work was reordered onto everything that did not need it, which
cost nothing — by the time the lease came free the sibling had committed
(`6cb178a`) and released its row, leaving the tree exclusively this session's and
the Gradle daemon free.
