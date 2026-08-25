# Releasing GoalPilot

**Written 2026-08-05.** How a build gets from this repo onto somebody else's
phone, and how that person gets the *next* build without you chasing them.

GoalPilot is not on Google Play. That single fact drives everything below:
Android gives a sideloaded app **no update mechanism at all** — no auto-update,
no notification, no "update" button anywhere in the system. Firebase App
Distribution is what supplies one.

---

## 1. The shape of it

```
   you                     GitHub Actions              Firebase              tester
    │                            │                        │                    │
    ├─ bump versionCode          │                        │                    │
    ├─ git tag v0.2.0 ──push──►  │                        │                    │
    │                            ├─ assembleRelease       │                    │
    │                            │  (signed w/ real key)  │                    │
    │                            ├──── distribute ──────► │                    │
    │                            │                        ├── email + push ──► │
    │                            │                        │                    │
    │                            │        in-app "Update available" dialog ──► │
```

> ## ⚠️ Updated 2026-08-25 — the diagram above is the DESIGNED route, not the route in use
>
> **Nothing has gone out by tag since `v0.3.1`.** `git tag` still ends there, while
> `app/build.gradle.kts` reads **v0.5.4 / `versionCode` 15** and testers are on it. In between are
> **nine `versionCode` bumps** (7 through 15) and **zero tags** — that half is mechanical, read
> straight off `git tag` and the history of `app/build.gradle.kts`.
>
> `Observed:` **six** of those were distributed, each with a changelog naming the route or the App
> Distribution release id — v0.4.0, v0.4.1, v0.5.0 (`0bnfqsmgtaim0`), v0.5.2 (`4jj8f6l3mmh5g`),
> v0.5.3 and v0.5.4. `Inferred:` the remaining bumps (0.3.2, 0.3.3, 0.5.1) were built but not
> separately distributed; they are not tagged either way. Every distribution was cut **locally**
> — `assembleRelease` + `appDistributionUploadRelease` — because that is what a session with the
> keystore in hand and Ido asking for a build before bedtime can actually do.
>
> **Both routes work and both are documented here** (§3 for the tag, the *No CI* block at the end
> of §3 for the local one). What matters is what you lose by taking the local one:
>
> - **The tag route runs the JVM suite first and stops on a failure**, so a red suite never
>   reaches a tester. Locally, nothing does that for you — run the suite yourself.
> - **No tag means no version marker in git**, so `git tag` is not a release history and
>   `git log v0.3.1..HEAD` spans nine version bumps rather than one release.
> - The local route needs the keystore on this machine. It is here (§2.1a), and that was **not**
>   true between 2026-08-06 and 2026-08-21.
>
> Neither route is deprecated. If you want the tag history back, cut the next release by tag and
> note that the nine versions in between were local.

Two independent halves, and it is worth knowing which is which:

- **The upload half** — the `com.google.firebase.appdistribution` Gradle plugin
  plus [`.github/workflows/release.yml`](../.github/workflows/release.yml). This
  is what publishes a build.
- **The in-app half** — the `firebase-appdistribution` SDK and
  [`AppUpdateChecker`](../app/src/main/java/com/idomarhaim/goalpilot/core/update/AppUpdateChecker.kt),
  called once from `MainActivity.onCreate`. This is what turns a published build
  into a prompt on a phone that already has the app.

An installed build only ever learns about releases newer than **itself**, so the
in-app half is worthless on any APK you distributed *before* wiring it up. The
first release carrying `AppUpdateChecker` is the last one anybody has to install
by hand.

---

## 2. One-time setup

Do these in order. Steps 2.1 and 2.2 are the two that are painful to undo.

### 2.1 Create the release signing key — **before you send anyone an APK**

```powershell
.\scripts\new-release-keystore.ps1
```

Writes `app/goalpilot-release.jks` (git-ignored) and appends its credentials to
`local.properties` (git-ignored). The password is generated and never printed;
read it out of `local.properties` when you need it in step 2.4.

> ⚠️ **This key is permanent.** Android identifies an app by its signature.
> Every future update must be signed with the same key, or the install is
> refused outright and the only recovery is asking every user to uninstall —
> losing their local state. **Back the `.jks` up somewhere that is not this
> machine**, together with the password.
>
> The script refuses to overwrite an existing keystore for this reason.

Local `assembleRelease` still falls back to the debug key when the credentials
are absent, so a fresh clone builds. **Distributing** a debug-signed APK is
blocked by a check in `app/build.gradle.kts` — the upload task fails rather than
warns, because that mistake is only discoverable months later.

### 2.1a Recovering the signing key — read this before you need it

**Status, 2026-08-21: RECOVERED. `app/goalpilot-release.jks` is back on this machine**, restored
from the GitHub secret by the procedure below, with `local.properties` carrying its four
credentials again. A local `assembleRelease` is once more signed with the **real** key —
`apksigner verify --print-certs` reports `CN=Ido Marhaim, OU=GoalPilot` and SHA-1
`e7d5534c…9062`, which is the certificate registered with Firebase and **not** the debug key.

⚠️ **It is still in only two places** — this machine and the GitHub secret — **and both are one
machine failure apart from where it was yesterday.** Copy it somewhere else; see §2.1b.

*The history, kept because it is the reason this section exists:* the keystore was created on
2026-08-06 on the laptop that has since been replaced. It was kept in the Dev folder but **outside
the repository**, so git never carried it, and a search of `C:\Dev` and the whole user profile on
2026-08-21 found no `.jks` at all. Nothing about that was noticed until a release was needed.

**Nothing was broken even then.** The key survives as the repository secret
`RELEASE_KEYSTORE_BASE64`, and [`release.yml`](../.github/workflows/release.yml) restores it on
every tagged run — `v0.3.0` shipped that way.

⚠️ **The two paragraphs that stood here were written BEFORE the recovery and contradicted the
status line above them. They are corrected rather than deleted, because the corrections are the
useful part:**

- *"the tag route is the only one that can produce an installable update"* — **false since
  2026-08-21.** The keystore is on this machine, so a local `assembleRelease` is signed with the
  real key, and in fact **every release from v0.4.0 to v0.5.4 went out that way** (§1). It was
  true while the `.jks` was missing: without it the build silently falls back to the *debug* key,
  and `app/build.gradle.kts` refuses to distribute that on purpose.
- *"the key is now in **one** place"* — it is in **two**: this machine and the GitHub secret. That
  is the number the ⚠️ above already gives, and two is still not enough, for the reason it gives:
  losing the laptop is the scenario that actually happened, and a machine plus a secret in the
  same repository do not fail independently. §2.1b is the fix and it is **still owed**.

**The risk, stated plainly:** if this repository or that secret is lost, no future build can ever
install over what testers already have. Every one of them would have to uninstall and lose their
local data.

#### Step 0 — look for the original first, it is free

*(Done on 2026-08-21 and it came up empty; the steps below are what actually recovered it.)*

If the old laptop, a disk image or any backup still has `app/goalpilot-release.jks`, that is the
whole answer. Copy it to `app/` and append its four credentials to `local.properties`
(`RELEASE_STORE_FILE=app/goalpilot-release.jks`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS=goalpilot`,
`RELEASE_KEY_PASSWORD`). Verify with `keytool -list -v -keystore app/goalpilot-release.jks -alias goalpilot`
and check the SHA-1 matches `e7:d5:53:4c:...:90:62`, the one registered with Firebase.

#### Step 1 — otherwise, pull it out of the secret, encrypted

[`backup-signing-key.yml`](../.github/workflows/backup-signing-key.yml) exists for exactly this. It
never uploads the keystore in the clear: it GPG/AES256-encrypts it under a passphrase that lives in
a second secret, so the artifact — which on a **public** repo anyone can download — is useless
without it. The job refuses to run if that passphrase is missing or shorter than 20 characters,
rather than quietly producing a plaintext artifact.

1. Generate a long random passphrase and **store it in your password manager first**. If you lose
   it the artifact is scrap.
2. GitHub → **Settings → Secrets and variables → Actions → New repository secret**, named
   **`BACKUP_PASSPHRASE`**, value = that passphrase.
3. **Actions → Back up the signing key → Run workflow.**
4. Download the artifact **`goalpilot-release-keystore-encrypted`** from the finished run. It
   expires after one day.
5. Decrypt it:
   ```powershell
   gpg --output app\goalpilot-release.jks --decrypt goalpilot-release.jks.gpg
   ```
6. Verify it is the right key — the run's log prints the alias and SHA-1, and this must match:
   ```powershell
   keytool -list -v -keystore app\goalpilot-release.jks -alias goalpilot
   ```
7. **Delete the `BACKUP_PASSPHRASE` secret.** It has done its job and every day it stays is a day
   it can leak.
8. Put the `.jks` somewhere that is not this machine and not this repository — a password manager
   attachment or an encrypted drive. It is git-ignored, and it must stay that way.

> ⚠️ **Do not "simplify" this by uploading the raw `.jks` as an artifact.** This repository is
> public; artifacts on a public repo are downloadable by anyone, and that would hand your signing
> identity to the internet.

### 2.1b What actually needs backing up — three files, and that is all

Everything else in this project is either in git or regenerable. The irreplaceable set is exactly
what git **ignores**, minus the noise:

| File | If you lose it | Recoverable? |
|---|---|---|
| `app/goalpilot-release.jks` | **no future build can ever install over an existing one** — every tester must uninstall and lose their data | only from the GitHub secret, by §2.1a |
| `local.properties` | the four `RELEASE_*` credentials, `GOOGLE_WEB_CLIENT_ID`, `sdk.dir` | credentials from the same secrets; the rest is quick to rebuild |
| `functions/.env` | `GROQ_API_KEY` — the free tier every AI call in the app runs on | yes, mint a new one at console.groq.com, but every deployed function is down until you do |

`app/google-services.json` is **tracked**, so it needs nothing.

#### Can you keep these in a separate backup repository?

**Yes — with one hard condition: it must be private, and secrets go in encrypted.**

The reason is not paranoia about the repo being private today. It is that **git never forgets**: a
key committed in the clear is in the history permanently, and stays there through every later
"remove the file" commit. If that repository is ever made public, forked, or reached with a leaked
token, the key is out and the only fix is the one this whole section exists to avoid — a new key,
and every tester uninstalling.

So:

- ✅ **A private repo holding the `.gpg` bundle** that §2.1a produces. That is AES256 under a
  passphrase you keep elsewhere, so the repo never holds anything usable on its own. You get
  versioning and an off-machine copy for free.
- ✅ **A password manager attachment** (`.jks` plus the four credentials). Simplest, and the one
  most likely to still be there in two years.
- ✅ **OneDrive**, which is already installed on this machine — but put the **encrypted** bundle
  there, not the bare `.jks`.
- ❌ **A repo holding the raw `.jks` or a plaintext `local.properties`**, private or not.
- ❌ **This repository**, under any circumstances. It is public, and `.gitignore` is the only thing
  standing between the key and the internet.

**Two copies in two different places is the target**, and they should fail independently: this
machine plus a GitHub secret is *not* two places in any meaningful sense, because losing the laptop
is exactly the scenario, and it is what happened.

### 2.2 Register the key's SHA-1 with Firebase

Google Sign-In is restricted by package name **+ signing certificate**. The
release key is a new certificate, so **sign-in fails in release builds until it
is registered** — with a bare `ApiException: 10`, which looks like a code bug.

```powershell
firebase apps:android:sha:create com.idomarhaim.goalpilot <SHA-1> --project goalpilot-56e30
```

The SHA-1 is printed by `new-release-keystore.ps1`; re-read it any time with:

```powershell
keytool -list -v -keystore app/goalpilot-release.jks -alias goalpilot
```

Then re-download `google-services.json` from the Firebase console and commit it.

> The existing debug SHA-1 (`F1:D0:96:…:DB:3F`, see
> [OPERATIONS.md §2](OPERATIONS.md#project-identifiers)) stays — registering a
> second one is additive.

### 2.3 Create the `testers` group and add people

Firebase console → **App Distribution** → **Testers & Groups** → new group with
the alias **`testers`** (the alias, not the display name, is what
`app/build.gradle.kts` and the workflow pass). Add each person **by email
address**.

This is the friction cost of App Distribution and it is unavoidable: a tester
must be invited by email and must accept with a Google account. You cannot hand
the link to an arbitrary stranger.

### 2.4 Create a service account for CI

Google Cloud console (project `goalpilot-56e30` — **check the project picker**,
it defaults to the wrong one, see [OPERATIONS.md §2](OPERATIONS.md)) →
**IAM & Admin → Service Accounts → Create**. Grant it the
**Firebase App Distribution Admin** role, then create a **JSON key** and
download it.

`firebase login:ci` tokens are deprecated; do not use one.

### 2.5 Add the GitHub secrets

Repository → **Settings → Secrets and variables → Actions → New repository
secret**:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/goalpilot-release.jks"))` |
| `RELEASE_STORE_PASSWORD` | from `local.properties` |
| `RELEASE_KEY_ALIAS` | `goalpilot` |
| `RELEASE_KEY_PASSWORD` | same as the store password |
| `GOOGLE_WEB_CLIENT_ID` | from `local.properties` |
| `FIREBASE_SERVICE_ACCOUNT` | the entire JSON file from step 2.4, pasted verbatim |

---

## 3. Cutting a release

**`versionCode` is bumped by hand, and forgetting is silent** — the build
succeeds, the upload succeeds, and no tester is ever prompted, because App
Distribution compares `versionCode` and sees no change. Hence a checklist.

1. `app/build.gradle.kts` → bump **`versionCode`** (strictly upward) and
   **`versionName`**.
2. Write the day's `CHANGELOG/YYYY-MM-DD/<session-label>.md` as usual.
3. Commit.
4. Tag and push — the tag message becomes the release note testers read.
   **Tag an explicit SHA. Never bare `git tag`, which captures whatever `HEAD`
   is at that instant:**

   ```powershell
   git rev-parse HEAD                 # read it, and confirm it is YOUR commit
   git log --oneline "@{u}..HEAD"     # anything here that is not yours?
   git tag -a v0.2.0 <that-sha> -m "Health Connect auto-sync; faster analytics"
   git push origin v0.2.0
   ```

   > ⚠️ **This step said `git tag -a v0.2.0 -m "…"` with no SHA until 2026-08-24,
   > and it has already misfired here.** `release.yml` fires `on: tags:` and does
   > `actions/checkout@v4`, so **the tag chooses the commit that gets signed and sent
   > to testers' phones.** `Observed:` 2026-08-22 — `main` was pushed at `915a388`,
   > `v0.3.1` was created seconds later, and the workflow checked out a **sibling's**
   > `d752342`, which had landed in the shared tree in between. Harmless in fact
   > (documentation only) and harmless by luck, not by procedure.
   >
   > **Tagging before pushing does not help** — that moves the race, it does not
   > close it; the working tree is shared at every instant. And unlike `git commit`
   > there is **no pathspec** to defend with, because a tag names a commit rather
   > than a set of files. Naming a SHA is the only form that removes the window,
   > because a SHA is a value and not a moving reference.
   >
   > It also escapes every other gate in this file: the auto-push preconditions run
   > on `@{u}..HEAD`, so a sibling commit landing *after* they pass is invisible to
   > them. This is the global parallel-sessions rule's *tag an explicit SHA* clause,
   > written down here because this is the file somebody follows during a release.

5. Watch the run under the repo's **Actions** tab. It runs the JVM unit suite
   first and stops on a failure, so a red suite never reaches a tester.

**No CI, or CI is broken?** The same thing locally, needing only step 2.1:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\gradlew :app:assembleRelease :app:appDistributionUploadRelease
```

Edit `app/release-notes.txt` first — that is what testers see.

> ⚠️ **`app/release-notes.txt`, and there is only one of it.** `app/build.gradle.kts` declares
> `releaseNotesFile = "app/release-notes.txt"` — **resolved against the repo root**, which is why
> the value carries its own `app/`.
>
> ⚠️ **This paragraph said the opposite until 2026-08-24, and the wrong version had teeth.** It
> read *"the plugin resolves it against the `app` module"*, the property was the bare
> `"release-notes.txt"`, and on that basis a session **deleted the copy at the repo root on
> 2026-08-22** — the copy the plugin had actually been reading. `Observed:` the next local upload,
> two days later, died with `Failed to read file "C:/Dev/Android_Final_Project/release-notes.txt"`.
> Nothing went red in between because nobody ran the local route.
>
> Two corrections follow from that measurement:
>
> - The `Inferred:` claim that `20f3b7e` and `67c21e5` shipped placeholder text to testers is
>   **refuted**. The plugin read the root file, which is the one people were editing, so those
>   releases shipped the notes written for them.
> - `ReleaseNotesGuardTest` **encoded the same wrong rule** and its *"both routes name the same
>   file"* assertion passed while they named different ones. It now resolves the declared path the
>   way the plugin does, and `app/build.gradle.kts` is declared as one of its inputs — without
>   that, editing the property left `testDebugUnitTest` cacheable and the guard reported green on
>   exactly the edit it exists to catch (measured: 9 s, nothing executed).
>
> The guard still fails the build if a second notes file appears, if the declared path does not
> resolve to a real file, or if the workflow and the Gradle plugin stop naming the same file.

**Also bump the version.** The `versionCode` checklist above is not CI-specific: App Distribution
compares `versionCode`, so a local upload with an unchanged one uploads happily and prompts nobody.

> ⚠️ **Read the APK before you upload it — the output path is not evidence the build is current.**
> `Observed:` 2026-08-24, a stale `app-release.apk` from an earlier run was sitting at the output
> path while the real build was still going. `appDistributionUploadRelease` would have shipped it
> without a word.
>
> ```powershell
> aapt2 dump badging app\build\outputs\apk\release\app-release.apk | Select-String versionCode
> ```
>
> It must read the `versionCode` you just bumped. This is the same family as the
> `${PIPESTATUS[0]}` trap in [OPERATIONS.md §4](OPERATIONS.md) — a build failure that scrolls past
> inside a pipe leaves the *previous* APK in place, and everything downstream succeeds on it.

---

## 4. What a tester actually experiences

Worth knowing precisely, because "one tap" it is not.

**First install** (unavoidably manual, whatever the transport):

1. Invitation email → *Get started* → sign in with the invited Google account.
2. Download the APK.
3. Android: *"…is not allowed to install unknown apps"* → **Settings** → allow
   for that browser/app → back.
4. Play Protect: *"Unsafe app blocked / unknown developer"* → **Install anyway**.

Roughly five taps and one alarming red dialog. That floor is Android's, not
Firebase's, and nothing but a Play Store listing lowers it.

**Every update after that** — this is the part that gets better:

1. They open GoalPilot.
2. A dialog appears: *"New version available — Update"*.
3. They tap **Update**. It downloads and installs in place; their data survives.

### Can I just send the APK on WhatsApp?

You can attach `app-release.apk` as a Document on Android WhatsApp, but it is a
poor transport: iOS WhatsApp will not forward it, some versions reject the
extension, and — the real cost — **a hand-sent APK is invisible to App
Distribution**, so that person is outside the update flow and stays on that build
forever until they manually install another one.

Send them the App Distribution invitation instead. If you must share a file
directly, the workflow also attaches the APK to its run (**Actions** → the run →
*Artifacts*), which is a stabler link than a chat attachment.

---

## 5. Traps

- **A build with the same `versionCode` as the last one notifies nobody.**
  §3 step 1 is the whole reason this document has a checklist.
- **Signing key ≠ package name.** Changing the key later is not a migration; it
  is "everyone uninstalls". Decide once (§2.1).
- **`google-services.json` must know the release SHA-1** or sign-in dies with
  `ApiException: 10` in release builds only — debug keeps working, which makes it
  look like a release-build code problem (§2.2).
- **`org.gradle.java.home` in `gradle.properties` is a Windows path.** It is
  correct locally (it pins Gradle to the Temurin **JDK 21** this toolchain needs)
  and fatal on a Linux runner. The workflow strips the line rather than the repo
  dropping it — see the *Un-pin* step.
- **The `testers` group alias must exist in the console**, or the upload fails
  with a 404 that reads like an authentication error.
- **Debug builds never prompt for updates.** Only the no-op
  `firebase-appdistribution-api` stub is on the debug classpath, by design. To
  see the real prompt you need two *release* builds with different
  `versionCode`s.
- **Keep `new-release-keystore.ps1` pure ASCII.** Windows PowerShell 5.1 decodes
  a BOM-less `.ps1` as ANSI, so a UTF-8 em dash inside a double-quoted string
  arrives as `â€"` — the trailing `"` closes the string early and the file fails
  to parse, with an error pointing at a line well past the real cause.
- **`isMinifyEnabled = true` on release** means the release APK is the first one
  R8 has ever shrunk in anger for a given feature. Smoke-test a release build on
  a device before tagging; a rule missing from `proguard-rules.pro` shows up
  nowhere else.

---

## 6. If you ever do want Play Store

Everything above stays valid — the same signing key, the same `versionCode`
discipline. What changes: a $25 one-time developer account, a store listing,
review waits, and then **real** silent auto-updates with none of the Play
Protect friction in §4. At that point remove the `releaseImplementation` of
`firebase-appdistribution` (Play policy disallows an app distributing its own
updates) and switch to the Play In-App Updates API.
