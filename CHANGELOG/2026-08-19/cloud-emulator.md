# cloud-emulator — the emulator moves off the laptop and onto GitHub's hardware

> **Summary:** a `workflow_dispatch` workflow boots an Android emulator on a GitHub runner, runs all 15 instrumented tests there and photographs the running app, so a 16 GB machine with 0.85 GB free never has to host an AVD again.

**Issue:** none — Ido's direct ask, 2026-08-19: *"there is a RAM limit on this machine, how can I run the emulator online instead?"*
**Branch:** `main`. **Mode:** AUTO MODE (declared in his first message).
**Scope:** CI and docs only. **No `app/src/` change, no local Gradle build, no local device** — which is
the point of the session, and what made it disjoint from `new-machine-checkup`, live throughout and
holding both `#gradle-daemon` and `#emulator`.

---

## The measurement that started it

`Win32_OperatingSystem` on the dev machine, **2026-08-19**: **15.67 GB total, 0.85 GB free.**
The Gradle daemon is pinned to `-Xmx2560m` in `gradle.properties`, a Pixel AVD asks 2–4 GB more,
and Android Studio wants its own. Three things that must run together do not fit.

Nothing about this project requires the device to be *local*: the backend is a live Firebase
project (`goalpilot-56e30`, Blaze), so a cloud device reaches exactly the data a local one does.

## What landed

| File | What |
|---|---|
| `.github/workflows/instrumented-tests.yml` *(new)* | Two jobs on `ubuntu-latest`: `instrumented` runs `:app:connectedDebugAndroidTest` on an emulated Pixel 6; `screenshots` installs the debug APK, launches it and captures PNGs. Both upload artifacts. |
| `.github/scripts/capture-screens.sh` *(new)* | The screenshot half. Kept out of the YAML because the shots must be spaced in time, and a multi-step shell block inside a `script:` key is where quoting goes to die. |
| `docs/CLOUD-DEVICE.md` *(new)* | The human answer: four places a device can live, what each costs, and the click-path for each. |
| `AGENTS.md` | One doc-index line. |

### Decisions worth keeping

**Public repo, so the minutes are free.** `api.github.com/repos/idomarhaim/Android_Final_Project`
answers **200** unauthenticated. That is what makes a 20-minute emulator run per click a
non-decision rather than a budget question, and it is why Firebase Test Lab was *not* chosen —
Test Lab is billed per device-hour and would additionally need Test Lab IAM roles added to the
service account behind `FIREBASE_SERVICE_ACCOUNT`, which today carries App Distribution Admin only.

**`workflow_dispatch` only — no `push:` trigger yet.** These tests have never run on a cloud
emulator. A workflow that turns `main` red on its first commit is a workflow people switch off,
so the `push:` block is written, commented, and documented as the thing to enable *after* one
green run. The cost of being wrong about that ordering is the whole feature.

**The two jobs run in parallel on two emulators, deliberately.** `connectedDebugAndroidTest`
**uninstalls the app** when it finishes, so a screenshot taken after it would be of an empty
launcher — the identical trap the `new-machine-checkup` row warns about on the local device
(*"DO NOT run `connectedDebugAndroidTest` on it"*). Two devices is the only arrangement where
both artifacts survive.

**`disable-animations` differs between them, and that is not an oversight.** The test job sets
it `true`, because Compose `waitForIdle` and Espresso both flake when animations run. The
screenshot job leaves it `false`, because animation is part of what a person is looking at.

**KVM has to be enabled by hand.** The runner image ships `/dev/kvm` but not permission to use
it; without the udev rule the emulator falls back to software rendering and the job times out
rather than failing, which reads as a hang.

**The screenshot job launches through the launcher intent, not `am start -n`.** The debug build
carries `applicationIdSuffix ".debug"` while `MainActivity` keeps its unsuffixed class name, so
the obvious component string is wrong. Noted inline in the script so nobody "simplifies" it back.

**`adb install -g` is an attempt, not a requirement.** Pre-granting runtime permissions keeps a
system dialog off the first screenshot, but `-g` aborts the entire install when a declared
permission is not grantable that way — and this manifest declares Health Connect permissions.
It falls back to a plain `-r`.

### What this cannot do, stated in the doc rather than discovered later

The CI emulator has `google_apis` (Play Services) but **no Google account**, so the screenshots
stop at the auth gate. Layout, theme and startup are reviewable; a logged-in screen is not.
`docs/CLOUD-DEVICE.md` routes that need to Android Device Streaming (a real streamed phone, free
quota, Blaze already in place) or to the App Distribution path that is **already built** — the
cheapest option in the table and the one most likely to be overlooked because it is not an
emulator.

### The open ticket this touches, and why it was not edited

[`TODO/TODO_MUST/Completion-Roadmap.TODO.must.md`](../../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)
§"The bottleneck is not the emulators" already names host RAM as the limit on parallel sessions
(*"`-Xmx2560m` per daemon × 2 plus two AVDs; … one ANR from host RAM with just the emulators up"*)
and resolves it by dedicating `Pixel_10_Pro_XL_B` to instrumented runs. **A cloud runner is a third
device that costs zero host RAM, which changes that arithmetic** — an instrumented run no longer has
to contend for a local AVD at all.

That file was **not edited**: it is outside this session's claimed paths, it is referenced by several
briefs, and rewriting another lane's scheduling rationale on the way past is exactly the drive-by
this repo's board exists to stop. The connection is recorded here instead; whoever next works that
roadmap can take it or leave it.

## 🧪 Tests

**No test layer exercises a GitHub Actions workflow, and this repo has none for CI.** Said
explicitly rather than skipped. What *was* run, against the actual consumers:

| Layer | Result |
|---|---|
| YAML parse (`yaml.safe_load`, the parser Actions' own schema sits on) | **PASS** — both jobs, both dispatch inputs, all 12 + 10 steps resolve; `release.yml` re-parsed alongside as a control |
| Shell syntax (`bash -n .github/scripts/capture-screens.sh`) | **PASS** |
| JVM unit / instrumented / Firestore rules | **not run — not touched.** No `app/`, `functions/` or `firestore-tests/` file changed. |

⚠️ **`unverified`, and this is the honest limit of the session: the workflow has never executed.**
A GitHub Actions workflow's real consumer is GitHub, and it cannot be reached from a working tree
— the file has to be on the remote before the Actions tab will list it, and `gh` is not installed
on this machine, so the run cannot be dispatched from here either. Parsing the YAML proves the
*syntax*, never that `reactivecircus/android-emulator-runner` boots or that
`connectedDebugAndroidTest` passes on API 34. **The first dispatch run is the test**, and Ido has
the click-path in `docs/CLOUD-DEVICE.md` §3.

Two things it is most likely to fail on, so the next session does not re-derive them:

1. **Test failures, not infrastructure ones.** The 15 instrumented tests have only ever run on a
   local Pixel AVD. Density, API level (34 vs the local AVD's) and the absent Google account are
   all live differences.
2. **`assembleDebugAndroidTest` build time.** KSP + Hilt on a cold runner cache is the long pole;
   the 60-minute timeout is generous on purpose for the first run and can come down after one.

### `defect` found in passing, reported and NOT fixed

`SESSIONS.md`'s **Recently released** table holds a `brief-refresh` row whose last cell names
`C:/Dev/JARVIS` immediately followed by `ules/scale-adaptive-ceremony.md` — the intended
`.../JARVIS/rules/...` with its **`r`-escape collapsed into a literal carriage return** somewhere
in transit. It is the only lone CR in the file.

Two things make it worth a paragraph rather than a shrug. It renders as `JARVISules`, which reads
as a **typo** rather than as damage, so nobody investigates. And it makes that row invisible to any
tool that treats a bare CR as a line break — this session's own rewrite of `SESSIONS.md` split the
row in two, and only a byte-level comparison against `HEAD` caught it before the commit.

**Left exactly as found, byte for byte.** It is another session's *released* row; the correct text
is obvious but the row is still someone else's account, and a silent repair riding an unrelated
commit is precisely the drive-by this repo's board exists to forbid. One edit fixes it, and it is
the owner's or Ido's to make. Mechanism and its family: JARVIS `kb/dev/escapes-die-in-transit.md`.

---

## Second unit — the KB drain (cross-repo)

`kb-candidates/2026-08-19-cloud-emulator.md`, both entries, drained under AUTO MODE at this
session's commit trigger rather than routed to a session of its own — the JARVIS board had
**no active row**, so the contention that made the three previous GoalPilot drains wait did not
exist. Claimed as a visitor there (`JARVIS@9596fe0`), released in `JARVIS@aa90ecb`.

📥 **Ingested:** verification when the consumer is unreachable from the working tree →
`kb/dev/look-at-your-own-output.md` §5.3
📥 **Ingested:** an Android CI emulator and the three invisible defaults →
`kb/dev/android-device-verification.md` §7

**0 new pages, 2 in-place extensions, 2 reciprocal cross-refs, 2 index rows rewritten; bundle
stays at 90 pages, `Check-KbLinks.ps1` CLEAN.** Update-in-place won against the candidate's own
proposal on entry 2: it asked for a new `android-emulator-in-ci.md`, and §5 of the existing
`android-device-verification.md` already owned the *local* RAM floor this is the off-machine
counterpart to. Neither always-ask gate opened — no `rules/` destination, nothing superseded.
Full account: `C:\Dev\JARVIS\CHANGELOG\2026-08-19\cloud-emulator.md`.

The candidate file is deleted in the same commit as this note, per the drained-candidate
carve-out. **Two candidate files in this repo remain undrained and were deliberately untouched**
— `2026-08-19-docs-hygiene-backfill.md` and `2026-08-19-new-machine-checkup.md`, each belonging
to a session of its own, and `new-machine-checkup` is live on the board and owns its by name.
