# Ingest Log

Append-only journal of what entered this bundle and when. Newest entries at the
bottom. Each entry: date, source, page(s) touched. Corrections are new entries —
never edit an old one.

---

- **2026-07-15** — Bundle created during the `/jarvis-ize` governance pass
  (`index.md`, `log.md`). `deployment-conventions.md` copied verbatim from the
  template library (`C:\Dev\JARVIS\templates\knowledge\`, user-template v1) —
  conditional bootstrap item; applies because this project deploys Firebase
  rules + Cloud Functions.

- **2026-08-06** — Source: [CHANGELOG/2026-08-05/release-distribution.md](../CHANGELOG/2026-08-05/release-distribution.md)
  (session `release-distribution`, commit `5316782`), drained by the `kb-audit`
  sweep that went looking for KB candidates nobody had ingested. **New page**
  [release-distribution.md](release-distribution.md): an app outside Google Play
  gets no update mechanism from Android, so the mechanism is the deliverable —
  Firebase App Distribution, a real signing key created **before** the first APK
  leaves the machine (the signature is the app's identity forever, so switching
  later costs every user their local state), an updater wired through
  `releaseImplementation` so it exists only in builds that reach a tester, and a
  **tag-triggered** workflow because `versionCode` is bumped by hand — a
  push-triggered job would notify testers about builds that are not installable
  updates. Records what was proven (both signing paths exercised in order,
  `apksigner` output matching the keystore fingerprint) and what cannot be proven
  from this machine (that a tester receives the prompt — every step of it is an
  outward action on live infrastructure).
  **No claim superseded** — the bundle had no page on distribution.
  Cross-project material from the same session (PowerShell 5.1's encoding traps)
  went to the central KB instead, and is restated here only as the local
  ASCII-scripts convention.

- **2026-08-10** — Source: `kb-candidates/2026-08-08-fix-task-completion-feedback.md`
  (session `fix-task-completion-feedback`, 2026-08-08), **entry 4 of 5** — the only
  one of that file's five that is GoalPilot-local; the other four went to the
  central bundle in the same pass (`kb-ingest-backlog-drain`, pass 2 of 5).
  **New page** [ui-error-conventions.md](ui-error-conventions.md): the
  `Resource.Error` house rule refined at its boundary — surface the repository's own
  message for refusals the **domain** generated, substitute a written message for
  failures the **network** generated and keep the original in the log. What makes it
  worth a page is that the convention was **right** and the call site followed it
  **correctly**, and a raw gRPC `UNAVAILABLE: Unable to resolve host
  firestore.googleapis.com` still reached a real screen: a boundary case inside an
  existing rule, not a misapplication of it. It passed unit tests and review because
  both were checking that the error *reached* the user, which it did.
  **No claim superseded** — the bundle had no page on UI error conventions;
  `SocialViewModel.report` remains the reference implementation of the domain half.
  Deliberately **not** promoted centrally: it is about this codebase's `Resource`
  convention, and promotion waits for a second project showing the same shape.

---

## 2026-08-10 — `kb-candidates/2026-08-10-c7-what-is-a-unit.md` (entry 4)

**Source:** the `c7-what-is-a-unit` session's own candidate file, drained by that
session. GitHub issue [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14)
(`C7` on the decision map), resolved 2026-08-10.

**Page:** **new** [goal-measurement.md](goal-measurement.md) — when a goal resists an
outcome metric, measure the **leading indicator**: the recurring behaviour that produces
the outcome. `E6` of the 2026-08-09 entity brief holds both positions in one paragraph —
the agent might advise making *"understand real estate"* measurable, and maybe not,
"because that requires some endlessly recurring task" — and the recurring task **is** the
measurable thing. Ido then stated the product position separately: goals in life usually
need to be measurable in some form, so if a user may choose an unmeasured goal, the agent
must recommend one **and recommend how, for that goal**. The leading indicator is the only
construction satisfying both.

**No claim superseded** — this bundle had no page on measurement. It does not contradict
`C7`'s decision that absence of a measure is legal and is the default state; it is what
the app does *about* that absence.

**Deliberately project-local**, and the reason is recorded on the page: product-domain
reasoning about how GoalPilot measures, not engineering practice. The other four entries
in the same candidate file went to the central bundle
(`C:\Dev\JARVIS\kb\log\2026-08-10.md`); this one stays with the repo it describes, and is
promoted only if a second project shows the same shape.
