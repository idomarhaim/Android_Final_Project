# KB candidates — `cloud-emulator`, 2026-08-19

Session: move the Android emulator off a RAM-bound dev machine onto a GitHub-hosted runner.
Account: [`CHANGELOG/2026-08-19/cloud-emulator.md`](../CHANGELOG/2026-08-19/cloud-emulator.md).

**Two candidates, none drained yet.** Both are cross-repo (pages belong in `C:\Dev\JARVIS\kb\`),
and a cross-repo drain owes a row on JARVIS's board — so they are drained as this session's
**second** unit of work, after the deliverable Ido is waiting on is pushed, rather than inline.
**This is a deferral within AUTO MODE, not a skip**, and it is logged as one.

---

## 1 · When the consumer of your output is unreachable from the working tree, the duty converts to naming the first-run risk

- **Claim.** *"Verify by re-running whatever will consume your output"* has a case it does not
  cover: artifacts whose **only** consumer lives on the far side of a publish — a CI workflow
  (GitHub Actions), a webhook receiver, a scheduled job, a store listing. You cannot re-run the
  consumer, because the consumer does not exist until you push. The remedy is **not** to declare
  it verified on a syntax check, and **not** to withhold the work until someone else runs it.
  It is to (a) run every consumer that *is* reachable — the parser the real consumer sits on, the
  shell that will execute the script — (b) mark the artifact `unverified` explicitly, in the
  committed account and not only in chat, and (c) **name the two or three specific things the
  first real run is most likely to fail on**, so the person who triggers it is debugging from a
  list rather than from zero.
- **Why.** `cloud-emulator` wrote `.github/workflows/instrumented-tests.yml` and could reach
  exactly two consumers locally: `yaml.safe_load` (the parser Actions' schema sits on) and
  `bash -n`. Both passed, and both together prove only that the file is *syntactically* a
  workflow. Whether `reactivecircus/android-emulator-runner` boots, whether KVM is permitted on
  the current runner image, and whether 15 instrumented tests written against a local Pixel AVD
  pass on an API-34 cloud one are all untouched by that. The failure mode this guards against is
  the flattering one: a green syntax check reads exactly like a green run in a changelog written
  a week later. **Rejected framing:** *"just push it and watch"* — correct where pushing is free,
  but it makes the verification duty conditional on the author's push rights and on there being
  no gate in between, and it still leaves nothing written down when the run is deferred.
- **Destination.** `kb/dev/look-at-your-own-output.md` — a new subsection under the existing
  *"it fires only where nothing already recomputes it for you"* boundary, which currently
  discusses consumers that exist and does not consider ones that are out of reach.
- **Anchors.** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` (extend, in place);
  `C:\Dev\Android_Final_Project\CHANGELOG\2026-08-19\cloud-emulator.md` §🧪 Tests as the incident.
- **Supersedes.** Nothing. This is an extension at a boundary the page already names, not a
  correction to any standing claim — so neither always-ask gate opens.
- **Status.** Not drained. Queued as this session's second unit.

---

## 2 · An Android CI emulator is cheap, and the three things that make it not work are all invisible

- **Claim.** Running instrumented Android tests on a GitHub-hosted runner costs nothing on a
  public repo and needs no local device — but three defaults make it fail in ways whose symptom
  does not name the cause:
  1. **KVM is present but not permitted.** The Ubuntu runner image ships `/dev/kvm`; the job's
     user cannot open it until a udev rule grants it. Without the rule the emulator silently
     falls back to software rendering, boot goes from ~2 minutes to ~20, and the job **times
     out** — which reads as a hang, not as a permissions problem.
  2. **`connectedAndroidTest` uninstalls the app when it finishes.** So any screenshot, logcat
     dump or manual poke taken *after* a test run photographs an empty launcher. Screenshots and
     tests therefore need **two devices**, not two steps on one.
  3. **System animations decide whether Compose/Espresso tests hang**, and the right setting is
     opposite for the two purposes: `disable-animations: true` for tests (`waitForIdle` and
     Espresso idling both flake otherwise), `false` for screenshots, where animation is part of
     what is being looked at.
- **Why.** Points 2 and 3 are the ones that get re-derived: 2 had already been paid for *locally*
  in this same repo — the `SESSIONS.md` row for `new-machine-checkup` carries a shouted
  *"DO NOT run `connectedDebugAndroidTest` on it"* because an earlier session's instrumented run
  wiped a Google account the session had asked Ido to create by hand. That is the same mechanism
  discovered a second time, on different hardware, which is the tell that it belongs on a page
  rather than in a board row. **Rejected framing:** *"use Firebase Test Lab"* — considered and
  rejected here: billed per device-hour, and needs Test Lab IAM roles added to a service account
  that today carries App Distribution Admin only, to solve a problem a free runner already
  solves. It wins only when the requirement is *many real device models at once*.
- **Destination.** `kb/dev/` — likely a new page (`android-emulator-in-ci.md`), since no existing
  page covers CI device provisioning. Check for an existing Android-CI page before creating one.
- **Anchors.** `.github/workflows/instrumented-tests.yml` and `.github/scripts/capture-screens.sh`
  in this repo as the worked example; `docs/CLOUD-DEVICE.md` §2 for the options table and what
  each cannot do.
- **Supersedes.** Nothing.
- **Status.** Not drained. Queued as this session's second unit.
