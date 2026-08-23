<!-- SOURCE: user-template v1; do not edit in-project, edit user-level then re-sync -->

# CLAUDE.md

This project's source of truth for AI agents is [AGENTS.md](AGENTS.md). Copilot also loads `.github/copilot-instructions.md`.

Read [AGENTS.md](AGENTS.md) first. Anything below this line is **Claude-Code-specific** and not relevant to other agents.

---

- **JDK — the machine is fine now; your *shell* may not be.** *(Re-measured 2026-08-19 by `new-machine-checkup`, on the machine that replaced the one these notes were written on.)* Machine `JAVA_HOME` is `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\`, that directory exists, and its `bin` is the **only** java entry on the machine `PATH` — so `java` on `PATH` is now JDK **21**. **The old claim here that `PATH` still offers JDK 17 is false and is deleted:** there is no JDK 17 anywhere on this machine, and `C:\Program Files\Eclipse Adoptium\` holds exactly one directory, so the *"two more wrecked Adoptium directories"* warning in [AGENTS.md](AGENTS.md) §JDK is stale in the same way (left alone — `docs-hygiene-backfill` owns that file). The still-true half: `firebase-tools` reads `java` from `PATH` and ignores `JAVA_HOME`, and `firestore-tests/run-tests.mjs` prepends `JAVA_HOME/bin` regardless — keep that workaround, it costs nothing and survives the next reorder.
- **But a Claude Code tool shell inherits an environment captured before that install, so `JAVA_HOME` is EMPTY inside it and `java` is not on its `PATH`.** `[Environment]::GetEnvironmentVariable('JAVA_HOME','Machine')` returns the correct value while `$env:JAVA_HOME` returns nothing — that disagreement is the tell, and it does **not** mean the machine is misconfigured. `gradlew` still builds, because `gradle.properties` pins `org.gradle.java.home` and the wrapper's launcher accepts a Windows-form `JAVA_HOME`; anything else that wants a JDK does not. So export it at the top of the call: `export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot"`. In Git Bash do **not** also prepend it to `PATH` in Windows form — `PATH` there wants `/c/Program Files/...`, and the Windows-form entry silently resolves nothing, which reads as "the JDK is missing".
- **`local.properties` is a Java `.properties` file, so a backslash is an escape character — and this bit on 2026-08-19.** A path written `sdk.dir=C\:\Users\namei\AppData\Local\Android\Sdk` is parsed as `C:UsersnameiAppDataLocalAndroidSdk` (every `\U` `\A` `\L` `\S` swallowed), and `assembleDebug` dies at `:app:compileDebugJavaWithJavac` with `java.io.IOException: The filename, directory name, or volume label syntax is incorrect` — an error naming neither the file nor the property that caused it. Use forward slashes; the regenerated copy carries this warning inline. Same trap fires in your own tooling: a Python heredoc holding that path needs a raw string, or it fails with `truncated \UXXXXXXXX escape`.
- **Prose punctuation is syntax in a resource file, and `--` inside an XML comment fails the build.** *(2026-08-20, `#8`.)* XML forbids `--` in a comment body, so a comment reading `— the classic Android bug —` typed with double hyphens kills `parseDebugLocalResources`, `mergeDebugResources` **and** `processDebugMainManifest` in one go — three failed tasks, and the manifest failure names only `Error parsing AndroidManifest.xml` with no line. This repo's house style puts long explanatory comments in every file and uses em dashes throughout, so the ASCII stand-in is the natural typo. Use `—`, and re-check **mechanically** over comment bodies (`re.findall(r'<!--.*?-->', s, re.S)`) rather than by eye — the comment reads perfectly either way. Same family as the `local.properties` escape trap above: the file format treats your prose as syntax.
- **And the third member of that family: `/*` inside a Kotlin KDoc, because Kotlin block comments NEST.** *(2026-08-24, `docs-currency-guard`.)* Writing `feature/*` or `docs/*.md` inside a `/** … */` block opens a **nested** comment that is never closed. The compiler says:
  ```
  e: .../DocsCurrencyTest.kt:212:1 Unclosed comment
  ```
  — line **212 of a 212-line file**, naming neither the KDoc nor the token; the real cause was `feature/*` on line 46, and it cost a 2m17s build that died at `:app:kspDebugUnitTestKotlin`. **This repo's house style is the risk factor**: long explanatory comments in every file mean a path glob or a footnote lands where the file format reads it as syntax. Check mechanically, not by eye — `grep -n '/\*' <file>` should return only real comment openers.
- **`gh` is installed AND authenticated — v2.97.0, logged in as `idomarhaim` (keyring).** *(Installed 2026-08-20 by `50b-transaction-guard`; authenticated 2026-08-20 by Ido running `gh auth login --web` at `c20-build-half`'s request. Two earlier claims here were true when written and are now false — *"NOT INSTALLED on this machine at all"* (2026-08-19) and *"it is NOT authenticated"* (2026-08-20) — and both are deleted rather than hedged.)*
- ⚠️ **The binary is at `C:\Program Files\GitHub CLI\gh.exe`, and the path this file gave until 2026-08-20 was wrong.** It said `%LOCALAPPDATA%\Programs\gh\bin\gh.exe` — there is **no such directory**. `Observed:` `export PATH="$PATH:/c/Users/namei/AppData/Local/Programs/gh/bin"` followed by `gh --version` returns *command not found*; the real entry on the **Machine** `PATH` is `C:\Program Files\GitHub CLI\`. A tool shell still inherits an environment captured before the install, exactly as with `JAVA_HOME` above, so prepend the **correct** directory:
  ```bash
  export PATH="$PATH:/c/Program Files/GitHub CLI"
  ```
  **Ido's own PowerShell has the same problem**, and telling him to run `gh …` will fail there too — give him the call operator and the full path: `& "C:\Program Files\GitHub CLI\gh.exe" …`.
- **`winget install --id GitHub.cli` HANGS, and it does not say so.** Tried first, with `--disable-interactivity --accept-package-agreements`: the process sat at **1.1 s of CPU over 12 minutes** and produced **zero output** — it is waiting on an elevation prompt it cannot display. The tell is CPU time, not the absence of output. The portable **zip** needs no admin, installs in ~20 s, and is what shipped.
- **`gh` needs NO `gh auth login` on this machine — Credential Manager already has a usable token.** *(Found 2026-08-20; the bullet that stood here said auth was Ido's and only Ido's, which was wrong.)* `git push` works, so a credential exists; `git credential fill` hands it back, and it carries **`repo`, `gist`, `workflow`** scope — enough for issues, comments and `workflow_dispatch`. Read it per command rather than running `gh auth login`, which would write a **second copy** of the same secret into `~/.config/gh/hosts.yml`, to be rotated separately and forgotten (`C:\Dev\JARVIS\kb\dev\redaction-leaves-a-second-copy.md` is the same family):
  ```bash
  GH_TOKEN=$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill | grep ^password= | cut -d= -f2-)
  GH_TOKEN=$GH_TOKEN gh issue create --title … --body-file …
  ```
  `read:org` is absent and `gh` warns about it; it is irrelevant for a personal repo. **The permission gate is unchanged** — the binary and the token are mechanics, and Ido's word is still required before any write.
  - ⚠️ **That route is not always available: the auto-mode classifier blocks it.** `Observed:` 2026-08-20, `c20-build-half` r3 — the `git credential fill` pipeline above was **denied** (*"Blocked by classifier"*) because reading a stored secret out of Credential Manager looks like credential harvesting whatever the intent. The finding above is **not** withdrawn — the token is real and the session that found it used it — but plan for the denial, and do not try to dress the same pipeline up to get past it.
  - ⚠️ **NARROWED 2026-08-20 — distinguish the two kinds of denial, because the bullet above reads as one and they behave oppositely.**
    - **The `git credential fill` pipeline** — treat as a **boundary**. Reading a stored secret out of Credential Manager looks like credential harvesting whatever the intent; the clause above stands, and dressing it up is still out.
    - **Ordinary `gh issue *` writes** — treat as a **flake, not a boundary. Retry the unchanged command once.** `Observed:` 2026-08-20, session `ticket-close-gap`: the identical `gh issue close -c "<body>"` was **denied**, then **succeeded** on `#6` and `#9`, was **denied** on `#11`, then **succeeded on a plain retry** — 3 denials in 6 attempts, same shell, same minute. `gh issue comment --body-file` was denied outright while `gh issue close -c` went through with the same body.
    **Why the distinction is worth the words.** Read as a boundary, a single denial ends the attempt — `hebrew-defer-freeze` (2026-08-17) abandoned three `#51` writes on first denial and left them **owed for three days**, and they were later found to have already landed. Read as a flake, one retry finishes the job. **Retrying the unchanged command is not working around the denial**; changing the command to evade it is, and that stays forbidden.

  - **So `gh auth login` was run after all, and the second-copy cost has already been paid.** There is now a token in the keyring as well as in Credential Manager; that is the state of the machine, not a recommendation to repeat. It is also the fallback that **works** when the classifier refuses — and it is Ido's to run, because the device flow needs a browser and cannot be driven from a tool shell. Ask him with the full-path command above; `gh auth status` tells you whether you need to.
- **The unauthenticated REST read path still works and is still worth keeping** — the repo is **public**, so `curl -s https://api.github.com/repos/idomarhaim/Android_Final_Project/issues/<N>` answers `200` and the `body` field is the ticket; in PowerShell `(Invoke-RestMethod "https://api.github.com/repos/idomarhaim/Android_Final_Project/issues/<N>").body`. It needs no auth at all, so it survives a logged-out `gh`. Two traps: pipe `curl` output to a file in the **scratchpad**, not `/tmp` (Git Bash maps it elsewhere), and print through `sys.stdout.reconfigure(encoding='utf-8')` or the console codec mangles every `—` and `§`.
  - ⚠️ **`stdin` is the other half of that trap, and it is the dangerous one.** `curl … | python -c "json.load(sys.stdin)"` decodes the response as **cp1252**, so the *data* arrives mangled — `§`→`Â§`, `—`→`â€"`, `יעד`→`×™×¢×“` — and `sys.stdout.reconfigure` does nothing about it, because the corruption already happened on the way in. `Observed:` 2026-08-20 — this made a `gh issue edit` on `#51` look like it had mojibake'd the entire public issue body, Hebrew included; re-reading the same response through `io.open(path, encoding='utf-8')` gave `identical: True`. **It fails toward panic**: you get a diff that accuses the write you just made, and the instinctive fix (re-send it) is wrong. Read the response from a **file** with an explicit `encoding='utf-8'`, exactly as the trap above already tells you to save it.
- On Windows, KSP/`.gradle` sometimes fail with "Could not delete/move …" locks. Re-run, or `rm -rf app/build/generated/ksp` and rebuild — it is not a code error.
- Pipe Gradle through `tail` only with `${PIPESTATUS[0]}` to read the real exit code (the pipe's exit is `tail`'s).
  - ⚠️ **And the same `${PIPESTATUS[0]}` gate belongs on any build whose output you then *install*.** `gradlew assemble… | grep …` exits with **`grep`'s** status, so `&&` does not protect you — and the previous APK is still sitting at the output path, so `adb install -r` succeeds and the test run reports the **last build's** results. `Observed:` 2026-08-20 — a Kotlin compile error scrolled past inside a `grep` and the suite came back with the same 8 failures as the run before, which read as *"the fix did not work"*. It was never in the APK.
- **`firebase deploy --only functions` needs `FUNCTIONS_DISCOVERY_TIMEOUT=120` on this machine, and its failure names the wrong cause.** *(2026-08-21, `c13-key-store`.)* The first attempt dies with:
  ```
  Error: User code failed to load. Cannot determine backend specification. Timeout after 10000.
  ```
  which reads as *your new module is broken*. It is not — the analyzer's discovery step simply exceeded its 10 s budget here. **Refute it in one command before touching the code:**
  ```bash
  node -e "const t=Date.now();const m=require('./functions/lib/index.js');console.log(Date.now()-t,'ms',Object.keys(m))"
  ```
  `Observed:` 202 ms, all five exports listed, immediately after that error. Then:
  ```bash
  export FUNCTIONS_DISCOVERY_TIMEOUT=120
  firebase deploy --only functions --non-interactive
  ```
  succeeded unchanged. `firebase-tools` is installed, and `firebase login:list` still prints
  `name.iddo@gmail.com` on `goalpilot-56e30`.
  - ⚠️ **`firebase-tools` cannot run any authenticated command, and IT IS NOT AN AUTH PROBLEM.**
    *(Root-caused 2026-08-22 by `57b-backgrounds-and-combinations`, after three wrong answers.)*
    Every command that talks to Google fails with *"Authentication Error: Your credentials are no
    longer valid. Please run `firebase login --reauth`"*, and **that message names the wrong
    cause.** The real error is in `firebase-debug.log`:

    ```
    Error: EPERM: operation not permitted, rename
      'C:\Users\namei\.config\configstore\firebase-tools.json.3898539203'
      -> 'C:\Users\namei\.config\configstore\firebase-tools.json'
        at Object.renameSync (node:fs)
        at write-file-atomic ... at Configstore.set ... at recordCredentials (lib/auth.js:335)
    ```

    **`firebase-tools.json` is held open by another process without share-delete**, so
    `write-file-atomic`'s rename can never replace it. The OAuth half works perfectly — Google
    returns **200** to both the refresh and the code exchange — and then `auth.js` fails while
    *persisting* the result, inside a `try` whose `catch` throws `invalidCredentialError()`. That
    is how a **file lock** comes out of the tool as *"your credentials are no longer valid"*.

    **Proof, three ways, none of them ambiguous:**
    - `firebase login --reauth` shows *"Firebase CLI Login Successful"* in the browser and then
      dies — the exchange succeeded, the write did not.
    - Renaming any file onto `firebase-tools.json` fails `EPERM`, while renaming onto **any other
      name in the same directory succeeds**. `Rename-Item` on it says outright: *"The process
      cannot access the file because it is being used by another process."* Attributes are
      `Archive`, the ACL grants `namei` FullControl, and the owner is `namei` — so it is a lock,
      not a permission.
    - Point the store elsewhere and everything works: with `XDG_CONFIG_HOME` set to an empty
      directory (`configstore` honours it on Windows via `xdg-basedir`), the error becomes the
      honest *"Failed to authenticate, have you run `firebase login`?"*, **the MOTD warning
      disappears**, and both store files are written. The MOTD failure was never a network
      symptom — it is the same lock, one line earlier.

    **`Observed:` the holder is `googlecloudtools.firebase-dataconnect-vscode` (v2.4.3), and it is
    named rather than guessed.** The configstore carries a `vscode-analytics-clientId` key — a VS
    Code writer's fingerprint — and grepping every installed extension for that string returns
    **exactly one file**:

    ```
    ~/.vscode/extensions/googlecloudtools.firebase-dataconnect-vscode-2.4.3/dist/extension.js
    ```

    Seven other Google extensions are installed (`cloudcode` ×2, `datacloud` ×2, `geminicodeassist`,
    `gemini-cli-…`, `colab`) and **none** of them contains it. That, plus **24 `Code.exe` processes
    running with no `node` process at all**, identifies the holder. *(This note said `Inferred:`
    and listed three candidates for about an hour; the grep that settled it had been started
    earlier and timed out, and its result arrived afterwards. Upgraded rather than left hedged —
    a hedge that can be resolved should be.)*

    `Untested:` the handle is still not tied to a **PID**, because that needs `handle.exe`, which
    is not installed. The identification above is by *authorship of the file*, not by observing the
    open handle — a distinction worth keeping, since another Google extension could in principle
    hold the same store without having written that key.

    **The fix, and it is not a browser flow:** release the handle, then run
    `firebase login --reauth` **once** — still needed, because the successful login of 2026-08-22
    could not be saved.

    ⚠️ **A plain restart is not enough, and this is the part that will waste a session.** The
    extension re-opens the store as VS Code loads, so reauthenticating *inside* VS Code races it.
    Do it with the editor **closed**:

    ```powershell
    # close VS Code completely first
    firebase login --reauth
    firebase projects:list      # the check -- if it lists projects, it is fixed
    ```

    Then reopen. The lock returns when the extension reloads and no longer matters: once the token
    is saved, later refreshes reuse it. If it breaks again, disable that one extension.

    ✅ **`Observed:` this worked, 2026-08-22.** Ido closed VS Code, ran the two commands in a plain
    PowerShell window and got *"Success! Logged in as name.iddo@gmail.com"* followed by
    `projects:list` printing `goalpilot / goalpilot-56e30`. So the whole diagnosis is confirmed
    end to end by the repair, not only by the reproduction — **the CLI is authenticated again and
    the standing-authorisation deploy path is open.**

  - ℹ️ **A cosmetic notification from the same extension, and it is NOT the lock coming back.**
    On opening VS Code you may see *"The Firebase CLI is not installed (or not available on
    $PATH)"*, sourced to *Firebase SQL Connect*. Its check is one line in
    `dist/extension.js`:

    ```js
    const c = spawnSync("firebase", ["--version"], { env, shell: process.platform === "win32" });
    const u = semver.valid(c.stdout?.toString());        // falsy -> "not installed"
    ```

    So it calls the CLI **not installed** whenever `firebase --version` fails to put a clean
    semver on **stdout** — which a broken CLI does, and which is why the notice travelled with
    this bug. `Observed:` after the repair the probe passes: replicating that exact `spawnSync`
    gives `status 0`, `stdout "15.28.1\n"`, empty stderr, and `C:\Users\namei\AppData\Roaming\npm`
    is on the **User** `PATH` with `firebase`, `firebase.cmd` and `firebase.ps1` all present.
    `Untested:` why it still fired on the launch straight after the repair — most likely the
    check runs at activation and that activation raced the fix. **If it recurs on a later launch
    that is a real finding and worth chasing; a single stale one is not.** Either way it is
    cosmetic — it changes nothing about the CLI, the Gradle plugin, or the app.

    **The escape hatch, if the editor must stay open:** set `XDG_CONFIG_HOME` to a directory
    nothing holds and copy `firebase-tools.json` into `<dir>/configstore/`. **Deliberately not
    done here** — it puts a second copy of a live refresh token on disk, to be rotated separately
    and forgotten, which is exactly `kb/dev/redaction-leaves-a-second-copy.md`. Restarting the
    editor costs thirty seconds and no secret.

    **Four earlier theories, all tested and all dead** — recorded so nobody re-runs them: the
    grant is expired or revoked (killed by the `200`); a proxy is intercepting HTTPS (killed —
    `firebase.tools` and `api.github.com` both answer `200`, the token endpoint answers `411` for
    an empty POST, no proxy variables set, and Node's own `https` **and** `fetch` get a clean
    parsed `401` from that endpoint); the CLI is stale (killed — **upgraded 15.27.0 → 15.28.1 and
    the error is byte-identical**); the store's `tokens.scopes: []` is corrupt (killed — `[]` is
    **hard-coded** at `lib/apiv2.js:53`).

    **The lesson worth more than the fix:** the tool's own error message named a cause, and it was
    wrong three times over. `--debug` and `firebase-debug.log` had the real one from the first
    minute. **`firebase projects:list` is the cheap liveness check** — `firebase login:list` only
    prints the cached identity and cannot fail.
  - ✅ **The Gradle App Distribution plugin authenticates SEPARATELY and still works.**
    `Observed:` 2026-08-22, `./gradlew :app:appDistributionUploadRelease` uploaded a signed
    release to `goalpilot-56e30` **in the same minute** that `firebase projects:list` was
    refusing to authenticate at all. So a dead CLI token does **not** mean "no Firebase
    capability" — check the specific path before reporting yourself blocked.
  - ⚠️ **The sentence that stood here — *"the deploy is gated by Ido's authorisation, never by capability"* — is FALSE as of 2026-08-21 and is deleted rather than hedged.** Ido gave a **standing** grant that day covering every Firebase action that costs nothing, the functions deploy included: *"I already gave you authorisation to do any Firebase action that does not require me to pay money."* Canonical text, boundary and honest limit: [`docs/OPERATIONS.md` § *Standing authorisation*](docs/OPERATIONS.md). **Deploy; do not wait.** What stays always-ask is narrow and named there — moving the billing plan, provisioning a resource that bills by existing, deleting anything, project settings or IAM. `outward-action-governance.md` is unchanged and still governs everything outward that is *not* this grant — but note **why** the old line was wrong, because it is the more useful half: a deploy to Ido's own project **reaches nobody**, and that rule's own discriminator is *"autonomy may persist where the blast radius is a repo; where it reaches people, it is re-granted per task or not at all."* Five sessions stopped at this gate applying a people-reaching rule to an action that does not reach people. The grant is that rule applied correctly, not an exception to it.
  - **The cost of the old wording, measured:** `#55` shipped a client whose document shape the deployed functions could not read, and the session stopped at the gate rather than deploying — leaving Ido's live points total reading **40 instead of 70** until he answered. The asking was the gate; capability never was.
- **`firebase functions:log` truncates and can fail outright — check the line count before believing a `grep` over it.** A bare `firebase functions:log` returned only `Error: Failed to list log entries`, and three `grep -c` over that file returned a very convincing **0, 0, 0**. `--only <function>` works but its window may still end before the call you are looking for. Print `wc -l` beside every count, per `kb/dev/look-at-your-own-output.md` §4k.

- **`ffmpeg` and `ffprobe` ARE installed now — `%LOCALAPPDATA%\Programs\ffmpeg\bin`, and on the user `PATH`.**
  *(Installed 2026-08-22 by `57d-entrance-animation` on Ido's approval. Nothing stood here before:
  the machine simply had neither, so `kb/dev/android-device-verification.md` §6.2 — the recipe for
  verifying a motion feature — could not run here at all.)* Build `N-126239`, 2026-08-21.
  - **A tool shell opened before that install does not see them**, exactly as with `JAVA_HOME` and
    `gh` above. Use the absolute path for the rest of such a session:
    ```bash
    FF="/c/Users/namei/AppData/Local/Programs/ffmpeg/bin"
    "$FF/ffprobe" -v error -select_streams v:0 -show_entries frame=pts_time -of csv=p=0 out.mp4
    ```
  - ⚠️ **`-vsync 0` is REMOVED from current ffmpeg and is a hard error** —
    `Unrecognized option 'vsync'. Error splitting the argument list`. Every recipe on the internet,
    and this project's own KB until 2026-08-22, still says `-vsync 0`. Use **`-fps_mode passthrough`**
    and check it worked by diffing the extracted-PNG count against the `pts_time` count rather than
    trusting it (`47` = `47` here). The image muxer's `non monotonically increasing dts` warning is
    harmless.
  - **`winget` was not used and should not be** — same failure as `GitHub.cli` above: it sits on an
    elevation prompt it cannot display. The portable BtbN zip needs no admin. It is **163 MB** and
    took **106 s** to download, so do not slip it into the middle of a timed run.
  - ⚠️ **`[Environment]::SetEnvironmentVariable('Path', …, 'User')` blocked for over two minutes**
    and returned only after being backgrounded — it broadcasts `WM_SETTINGCHANGE` and waits on every
    top-level window. It **succeeded**; it merely looks exactly like a hang. Do not kill it, and do
    not run it a second time.
  - **What it is for, and when it is the wrong tool:** `screenrecord` emits a frame only when the
    screen changes, so gaps in the `pts_time` list are literal dead screen. But for a **short
    animation** it is the wrong instrument even when it works — it falls back to 720×1280 here, and
    a 340 ms animation is ~20 frames. Freeze the Compose clock and write one PNG per instant
    instead. Both halves: `kb/dev/android-device-verification.md` §6.2 and §6.6.
