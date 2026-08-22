---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 53
owns:
  - app/src/main/res/values/components_strings.xml
  - app/src/main/res/values-iw/components_strings.xml
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/MaterialPicker.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/ComponentStrings.kt
  - docs/PRODUCT_v0.3.md
  - app/src/test/java/com/idomarhaim/goalpilot/resources/
  - CHANGELOG/<today>/53-material-naming.md
  - sessions/53-material-naming.md
created: 2026-08-22 by 53-tag-sweep
---

# `#53`'s last item — the material picker cannot be named, so it cannot be reported

**This is the only thing left on [`#53`](https://github.com/idomarhaim/Android_Final_Project/issues/53).**
Everything else shipped: the four-material contract in `05ec6aa` (`c12-material-contract`) and the
§4.1 `.tag` sweep in `70922d7` (`53-tag-sweep`). This is the gap `#53`'s **2026-08-21 comment**
filed against its own deliverable, and that comment is the contract — read it first.

> 🔒 **Singletons.** Needs the **Gradle daemon**. A device is optional: the change is two string
> resources and a table, and `MaterialPicker` already has a render pass from `c12-material-contract`
> if you want to see it.

## Read first

1. [AGENTS.md](../AGENTS.md)
2. `#53`'s **second** comment (2026-08-21) — *"The picker's labels share no vocabulary with the
   design of record"*. It states the problem, the two options, and its own recommendation.
3. [`docs/PRODUCT_v0.3.md`](../docs/PRODUCT_v0.3.md) **§4.1** — the material table, which uses
   *neo* / *dark neo* throughout.
4. `res/values/components_strings.xml` `:107-114` and its `values-iw` twin — where the picker's
   words actually live.

## The gap

| §4.1's name | `AppMaterial` | the picker says |
|---|---|---|
| Glassmorphism | `GLASS` | **Glass** |
| Liquid glass | `LIQUID_GLASS` | **Liquid glass** |
| Neo | `NEO` | **Soft** |
| Dark neo | `DARK_NEO` | **Soft dark** |

**The word "neo" appears nowhere in the UI.** Every other artefact — the spec, the briefs, the
changelogs, the render-pass filenames, the issue — calls it *dark neo*. So a user who has read the
spec cannot find the control, and a session receiving a bug report cannot match the report to a
control by name. **Both halves have already happened on this ticket**, which is why it is a defect
in `#53`'s deliverable rather than a cosmetic wish: this ticket's own rule is that a picker has to
*say* things, and §0.3's is that a control that changes nothing is a defect. A control that cannot
be **named** is the same failure one step over.

## Task

**Take option 2 from that comment — it is the recommended one and it is nearly free.** Keep the
user-facing words plain English and make the spec's vocabulary *reachable* rather than replacing it:

1. Carry the spec name on each tile — its `contentDescription`, or its subtitle line, whichever
   `MaterialPicker` already has room for. *"Soft dark — dark neo"* is the shape; the exact wording
   is yours.
2. Add the mapping table above to **§4.1 of `docs/PRODUCT_v0.3.md`**, beside the material table, so
   the two vocabularies are tied together in the one document that defines both.
3. New strings go in **`values/` and `values-iw/` together** — `HebrewLocaleResourceTest` and
   `ComponentsLocaleTest` both have an opinion, and `AnalyticsLiteralSweepTest` sweeps
   `ui/components/`, so a bare literal there fails the JVM suite.

⚠️ **Do not rename the picker to *Neo* / *Dark neo*.** That is option 1, and the comment rejects it
with a reason worth keeping: *Soft* / *Soft dark* are the better **user-facing** words, which is
presumably why they were chosen. The failure is that the two vocabularies exist with nothing linking
them — not that the wrong one won.

## Carries over

- **`53-tag-sweep` (2026-08-22) closed the `.tag` half of `#53` and left this open deliberately**,
  naming it on the ticket. See `CHANGELOG/2026-08-22/53-tag-sweep.md` §9.
- **`c12-material-contract` built `MaterialPicker.kt`** and its 16-frame render pass at
  `docs/render-passes/2026-08-20-c12-material-contract/`. The tile subtitles are already written
  (*"Charcoal, with one bright accent."*), so there is a place for the spec name to sit.
- **`ui/components/` is swept for literals.** `AnalyticsLiteralSweepTest.SWEPT_PACKAGES` includes it.

## Out of scope

- **Anything about dark neo's charcoal *ground*.** `#53`'s comment establishes that the charcoal is
  §4.1's material table being implemented correctly, with a render pass proving the cyan→blue accent
  is present. If Ido's answer to the open question below turns out to be *"I expected blue"*, that is
  a **spec** change and a different ticket — do not fold it in here.
- The `.tag` sweep, the palette values, chart volume, backgrounds — all shipped.

## Exit

- The spec's name is reachable from the UI for all four materials, in **both** locales.
- §4.1 carries the mapping table.
- JVM suite green (expect `HebrewLocaleResourceTest`, `ComponentsLocaleTest` and
  `AnalyticsLiteralSweepTest` to be the ones with opinions).
- `CHANGELOG/<today>/53-material-naming.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit.
- **Close `#53`** with the evidence — this is the last item, so the *nothing left → close it* branch
  of `/kickoff` §5 step 4 applies. Grep `sessions/` and `sessions/done/` for `issue: 53` first and
  confirm nothing else is `ready` or `active`.
- Commit and push under AUTO MODE.

## ❓ One question for Ido, and it does NOT block this brief

When Ido reported *"no dark blue neo"* on `v0.3.0`, he meant one of two things, and only he knows
which:

- **the name** — he could not find the control, because the UI never says *neo*. Then this brief is
  the whole fix and `#53` closes on it.
- **the ground** — he found it, and expected the surface to be blue rather than charcoal. Then this
  brief is still correct and still closes `#53`'s naming gap, but a **second, separate** ticket is
  owed against §4.1's material table, which specifies *"charcoal groove … one cyan→blue accent"*.

**Do the work either way.** If the answer arrives mid-session, note it in the changelog and file the
spec ticket separately; if it does not, say in the closing comment that the question is still open
and that this brief answered only the naming half.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Say whether `#53` closed. If it did, say that `#48`, `#53`, `#54` and `#55` are all closed and
**v0.3 is feature-complete except `#51` (frozen)**, and name the Wave 4 verification pass as the
next step.
