# Running the emulator somewhere other than this laptop

**Written 2026-08-19** (`cloud-emulator`), because the development machine ran
out of RAM. Read [AGENTS.md](../AGENTS.md) first. This file answers one question:
*where can GoalPilot run when there is no room for an AVD here?*

---

## 1. The problem, measured

> **Observed 2026-08-19**, `Win32_OperatingSystem` on the dev machine: **15.67 GB
> total, 0.85 GB free** while idle-ish. The Gradle daemon is pinned to
> `-Xmx2560m` ([gradle.properties](../gradle.properties)) and a Pixel AVD asks for
> 2–4 GB more, on top of Android Studio.

So the three things a normal Android workflow does at once — build, emulate,
edit — do not fit. Something has to move off the machine. The good news is that
**nothing about this project needs the device to be local**: the backend is a
live Firebase project (`goalpilot-56e30`, Blaze), so a cloud device talks to
exactly the same data a local one does.

---

## 2. The four places a device can live

| # | Where | What you get | RAM here | Cost | Blocked on |
|---|---|---|---|---|---|
| **A** | **GitHub Actions emulator** — [§3](#3-option-a--the-emulator-that-is-already-set-up) | Tests run + screenshots of the app | **0** | **Free** (public repo) | nothing — it is built |
| **B** | **Android Device Streaming** (Studio + Firebase) | A **real** phone, streamed, fully interactive | Studio only (~2 GB) | Free quota, then billed to `goalpilot-56e30` | you, ~5 min in Studio |
| **C** | **Appetize.io** | An emulator **in a browser tab**, interactive, shareable link | **0** | Free tier, then paid | you, a signup |
| **D** | **A real Android phone** via App Distribution | The real thing | **0** | **Free** — already wired | owning a phone |

**The short answer:** use **A** for anything automatable (it costs nothing and is
already done), **B** when you need to touch the app yourself. **D** is free and
already built if you have an Android phone in your pocket — see
[RELEASING.md](RELEASING.md).

---

## 3. Option A — the emulator that is already set up

[`.github/workflows/instrumented-tests.yml`](../.github/workflows/instrumented-tests.yml)
boots an Android emulator on GitHub's hardware, runs the 15 instrumented tests
in [`app/src/androidTest/`](../app/src/androidTest/), and photographs the running
app. **Your machine does nothing** — you click a button in a browser.

### How to run it

1. Go to **<https://github.com/idomarhaim/Android_Final_Project/actions>**
2. In the left sidebar click **Instrumented tests (cloud emulator)**
3. Click **Run workflow** (top right) → leave the defaults → **Run workflow**
4. Wait ~20 minutes. Refresh; click into the run.
5. At the bottom of the run page, under **Artifacts**, download:
   - `androidTest-report-api34` — open `index.html` inside it: the test report
   - `app-screenshots-api34` — PNGs of the app running, plus `logcat.txt`

### It runs on every app change too, since 2026-08-19

**Run #1 was green** — manually dispatched on `19ff290`, **12m 02s** total
(`androidTest on API 34` 11m 56s, `Photograph the running app` 8m 08s, in
parallel). That was the condition this workflow shipped waiting on, so the
`push:` trigger is now on: any commit to `main` touching `app/**`, `gradle/**`
or the root build files gets a device run automatically. **Docs and changelog
commits — most of them here — trigger nothing**, and two pushes in a row cancel
the older run rather than queueing it.

Dispatching by hand still works and is the only way to get screenshots.

### "Success" is now a number, not a word

`connectedDebugAndroidTest` goes green when **zero** tests are discovered — that
is how a device job reports a harness failure as a pass, and it is §1 of the
`android-device-verification` page in the JARVIS KB. Run #1 said *Success* and
nothing on the page said whether 15 tests had run or none.

So the job now counts the JUnit XML itself, prints
`Instrumented: N tests, N failures, …` in the run summary, and **fails outright
if N is 0**. You never have to download an artifact to find out whether anything
ran.

### What it cannot do

- **You cannot click around.** It is a batch run; screenshots, not a session.
- **It cannot sign you in.** The emulator has `google_apis` (Play Services) but
  no Play Store and no Google account, so the screenshots stop at the auth gate.
  That is enough to check layout, theme and startup, and not enough to review a
  logged-in screen. For those, use option B or D.
- **Runner minutes are free only while this repo is public.** *Inferred* from
  GitHub's published billing model, not measured here. If the repo is ever made
  private, an emulator run bills against the monthly Actions allowance.

---

## 4. Option B — a real phone streamed into Android Studio

Google runs a fleet of physical devices you can drive from Android Studio over
the network. The device is real, has a real Play Store, and Google Sign-In works
— which is the thing the CI emulator cannot give you.

1. Android Studio → **Device Manager** → the **Remote** tab (in older builds:
   *Firebase Device Streaming*).
2. Sign in with the Google account that owns `goalpilot-56e30`, and pick that
   project when asked. It is on the **Blaze** plan already
   ([OPERATIONS.md](OPERATIONS.md) §1), which is the prerequisite.
3. Pick a device → **Start**. It appears in the device dropdown like any AVD; you
   `Run` the app onto it normally.

**RAM cost:** Android Studio itself, and no AVD process. That is roughly a third
of what a local emulator setup costs.

> **Untested here** — no session has run this on the new machine. The quota is a
> free monthly allowance that Studio shows you before you connect; past it, time
> is billed to the Firebase project. Check the number Studio displays rather than
> trusting a figure written in this file.

---

## 5. Option C — an emulator in a browser tab

[Appetize.io](https://appetize.io) takes an APK and gives back a URL that streams
a running emulator into any browser. Nothing installs, and the link is
shareable — useful for showing the app to someone without asking them to
sideload it.

1. Get an APK: easiest is the `app-debug.apk` produced by any run of the workflow
   in §3 (or the signed release APK attached to a **Release** workflow run).
2. Sign up at appetize.io, **Upload** the APK, open the link it returns.

**The catch for this app specifically:** Appetize devices are stock Android
images. Firebase Auth's Google Sign-In needs Play Services, so expect sign-in to
fail there. Everything before the auth gate renders fine.

> **Untested here.** The free tier's monthly minute allowance changes; read it on
> their pricing page rather than from this sentence.

---

## 6. Option D — the path that is already built

The app already ships to testers through **Firebase App Distribution** on every
`v*` tag ([RELEASING.md](RELEASING.md)). If you have an Android phone, install
the tester build on it: zero RAM, a real device, real Play Services, real sign-in,
and no new tooling at all. This is the cheapest option in the table and the one
most likely to be overlooked because it is not an emulator.

---

## 7. Considered and not taken

- **Firebase Test Lab batch runs.** Same tests as §3 but billed per device-hour
  and needing a service account with Test Lab IAM roles added — the existing
  `FIREBASE_SERVICE_ACCOUNT` secret only carries App Distribution Admin. Strictly
  worse than a free GitHub runner for *this* repo's needs. Revisit only if the
  tests need to run on many real device models at once.
- **Genymotion Cloud SaaS.** Paid from the first minute, and its advantage
  (device farm breadth) is not a problem this project has.
- **Just shrinking the local AVD.** Worth knowing as a fallback, not a fix: in
  Device Manager, edit the AVD and set RAM to 1536 MB, VM heap to 256 MB, and
  internal storage to 2 GB, then close Android Studio and build from the terminal.
  It buys perhaps 1.5 GB — enough to limp, not enough to work.

---

## 8. If you are debugging one of these

| Symptom | Cause |
|---|---|
| Workflow step "Enable KVM" prints no `/dev/kvm` | The runner image changed. Without KVM the emulator boots ~10× slower and the job times out. |
| `Activity not found` in the screenshot job | The debug build carries `applicationIdSuffix ".debug"`. `capture-screens.sh` launches via the launcher intent for exactly this reason — do not "simplify" it to `am start -n`. |
| Three identical blank screenshots | Read `screenshots/logcat.txt` in the same artifact; the script flags `FATAL EXCEPTION` as a workflow warning. |
| Tests pass locally, hang on CI | System animations. The test job sets `disable-animations: true`; the screenshot job deliberately does not, because animations are part of what you are looking at. |
