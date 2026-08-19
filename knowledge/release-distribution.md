# Release distribution — getting an APK to other people, and updating it later

**TL;DR:** An app that is not on Google Play gets **no** update mechanism from
Android — no auto-update, no notification, nothing. GoalPilot builds one:
Firebase App Distribution on both ends, a **real signing key created before the
first APK leaves the machine**, and a **tag-triggered** workflow. A hand-sent APK
(WhatsApp, email) is permanently invisible to that flow: send the invitation, not
the file.

*Provenance: session `release-distribution`, 2026-08-05, commit `5316782` — see
[CHANGELOG/2026-08-05/release-distribution.md](../CHANGELOG/2026-08-05/release-distribution.md).
Operational how-to lives in [docs/RELEASING.md](../docs/RELEASING.md); this page
is the why.*

## 1. The signing key is unrecoverable, so it comes first

Release builds were signed with the **debug** key as a deliberate convenience.
That cannot be undone after the fact: Android identifies an app by its signature,
so the first build handed to a person locks in the key **forever**, and switching
later means every user uninstalls, reinstalls and loses their local state.

`scripts/new-release-keystore.ps1` therefore runs before anything is distributed,
and three of its choices are load-bearing:

- **It refuses to overwrite an existing keystore.** Replacing a release key is the
  unrecoverable move, so the script does not offer it.
- **The generated password is 32 characters, alphanumeric on purpose.** It travels
  through a Gradle property, a GitHub secret and a base64 round-trip, and every
  quoting layer in that chain is a chance to mangle punctuation.
- **Credentials are written straight into git-ignored `local.properties` and never
  printed** — a signing password must not end up in a terminal scrollback or an
  agent transcript. What it does print is the SHA-1 (public) and the
  `firebase apps:android:sha:create` command that has to follow it.

Signing config resolves from `local.properties` **or** the environment (CI), and
falls back to the debug key when neither is present so a fresh clone still builds.
That fallback is local-only: a `doFirst` check **fails** `appDistributionUpload*`
outright rather than warning, because a debug-signed APK in a tester's hands is not
discoverable until the day you try to update it.

## 2. The updater exists only in builds that reach a tester

`firebase-appdistribution` ships as an `-api` stub plus a full implementation, and
using both is structural rather than tidy: `implementation(…-api)` compiles into
every variant and does nothing, while `releaseImplementation(…)` adds the real
updater to release builds only. A developer running from source is never
interrupted by an update dialog, and a build that never reaches a tester carries no
updater at all.

Two details worth keeping:

- **The check is guarded to fire once per process.** `MainActivity` is recreated on
  every configuration change, so without the guard a rotation mid-download restarts
  the flow on top of itself.
- **It is hooked from `MainActivity.onCreate`, not the Compose root**, because the
  SDK drives its own dialogs off the foreground Activity, outside Compose.

## 3. Tag-triggered, not push-triggered — the reason is `versionCode`

The workflow was asked for as *"on push to main"*. It ships on `v*` tags plus
`workflow_dispatch` because **`versionCode` is bumped by hand**. A push-triggered
job would publish build after build carrying the *same* `versionCode`, App
Distribution would accept every one, testers would be notified every time, and not
one of those notifications would describe an installable update. **A tag is the
signal that a version number was actually decided**; a push is not.

Two CI traps in `.github/workflows/release.yml`:

- **`org.gradle.java.home` must be stripped on the runner.** `gradle.properties`
  pins it to a Windows Temurin **JDK 21** path, which is the JDK this toolchain
  needs. Correct locally, fatal on `ubuntu-latest` — so the line is
  deleted in CI rather than removed from the repo, where it earns its keep daily.
- **Verify the APK signature with `apksigner` after assembling.** An unsigned or
  debug-signed APK assembles perfectly happily; catching it in CI beats catching it
  on a tester's phone.

Uploads authenticate with a **service account**, not a `firebase login:ci` token —
those are deprecated.

## 4. What is proven, and what cannot be proven from here

**Both signing paths were exercised in order, which is the only way to prove
either.** The first `assembleRelease` ran with no keystore on the machine:
`validateSigningRelease` resolved to the debug config and produced an installable
APK — the property the fallback exists to preserve for a fresh clone. The real key
was then created, and a second `assembleRelease` produced an APK whose `apksigner`
output matches the keystore's own fingerprint byte for byte.

**Not verified, and not verifiable from this machine:** that a tester actually
receives the prompt. That needs the key created, its SHA-1 registered on
`goalpilot-56e30`, a `testers` group, and *two* release builds with different
`versionCode`s — every one of them an outward action on live infrastructure.

## 5. Two conventions this session left behind

- **Scripts in `scripts/` are ASCII-only.** Windows PowerShell 5.1 decodes a
  BOM-less `.ps1` as CP1252, where an em dash's last byte becomes a closing quote —
  `new-release-keystore.ps1` failed to parse on its first run with
  *"The string is missing the terminator"* pointing nowhere near the cause. Fixed
  with ASCII rather than a BOM (a BOM is one careless re-save from being lost), and
  the script's header says so, so the hyphens are not "tidied" back into em dashes.
- **The honest answer to "can I just send the APK?"** is yes, and it costs the
  recipient every future update. It is written down in `docs/RELEASING.md` beside
  the walk-through of what a tester actually experiences — five taps and a red Play
  Protect dialog on first install, three taps on every update after that.
