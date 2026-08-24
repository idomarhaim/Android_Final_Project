# `manual-demo-script` — 2026-08-24

> **Summary:** The running order Ido reads while recording a feature-review screencast of the
> app himself — thirteen acts, the exact taps, the line to say, the prep traps that already
> cost a take, and three ordering departures from the automated tour, which is optimised for
> unattended capture rather than for a viewer.

Ido is recording a feature-review screencast of the app **himself**, on the mirrored phone,
and asked for the running order to read while he does it. One new document:
[`docs/marketing/manual-demo-script.md`](../../docs/marketing/manual-demo-script.md).
`AUTO MODE`.

---

## 0 · What was asked, and what already existed

> *"I need to make a screen recording myself, reviewing all the app's features and showing
> them one by one. Prepare me a list with the best logical sequence, to have in front of my
> eyes while I record."* (Hebrew, translated.)

Three documents in `docs/marketing/` already describe this app on video, and **none of them
answers this**:

| file | what it is | why it does not fit |
|---|---|---|
| [`explainer-video-brief.md`](../../docs/marketing/explainer-video-brief.md) | the brief for a narrated explainer, plus the footage inventory | specifies a **film**, and its narration is written for an edited cut, not for a person talking live |
| [`tour-timecodes.md`](../../docs/marketing/tour-timecodes.md) | 70 measured beats of the automated 11-minute capture | a **beat map of an existing file**, not instructions for a human |
| [`marketing-film-cut.md`](../../docs/marketing/marketing-film-cut.md) | the 2:10 marketing film, its cut plan and its voiceover | a 1.8×-sped, eight-feature **advert** — the opposite of a feature-by-feature review |

So the fourth thing was genuinely missing, and the new file says so at the top so the next
reader does not have to work it out again.

## 1 · What the document contains

Six sections: **prep** (§1), the **running order as a table** (§2), **act by act** with the
exact taps and the line to say (§3), **why this order** (§4), a **7-minute cut** (§5), and
**what to do when a beat goes wrong mid-take** (§6). Thirteen acts, `13:30` target.

Every feature in [`goalpilot-presentation-source.md`](../../docs/presentation/goalpilot-presentation-source.md)
Part 4 is covered — §4.1 through §4.13 — plus `#24`'s AI goal onboarding, which shipped
**today** and postdates that document's feature list.

## 2 · The three ordering decisions, and the reason for each

The automated tour's twelve acts are a proven order, but they are optimised for **capture
reliability on an unattended device**, not for a viewer's comprehension. Three departures:

1. **The four-level model is *stated* over the Goals tab (Act 4) and the Life areas *screen*
   is *shown* much later (Act 10).** The model has to arrive early or nothing after it parses
   — but the Goals tab already renders life areas as its **group headers**, so it can be
   explained there at **zero navigation cost**, leaving the Life areas screen next to the run
   and Analytics, all three behind the same avatar tap.
2. **`#24`'s new-goal AI plan (Act 5) runs before goal detail (Act 6) and the calendar
   (Act 7).** It writes dated tasks, so the next two acts display **work the viewer just
   watched being created**. In the other order they are three unrelated screens.
3. **Kept from the automated order: capture before structure.** Smart add is Act 2. A viewer
   who has not seen it assumes a four-level structure is expensive to maintain — which is the
   objection the whole product exists to answer.

## 3 · The prep section is where the recorded failures went

§1 is not generic advice. Every item is a trap already paid for by an earlier take, drawn from
`scripts/record-tour.sh`'s own comments and from `explainer-video-brief.md` §1:

- **`screenrecord` downgrades to 720×1280 silently and still exits `0`** — read its log, not
  its exit code.
- **`--time-limit 0`** is what removes the 3-minute cap.
- **SystemUI demo mode**, verbatim from the script's `demo_mode_on`.
- **Analytics: Week and Month are empty on this account; Year has the 67 / 20 / 13 split.**
  Shooting Month puts an empty donut under the film's payoff line.
- **A life area with no windows renders the honest empty state** — correct behaviour, useless
  footage. Pick the area before recording.
- **Tap the four materials by their taglines** (*Frosted panels*, *Glossy, lit*, *One flat
  surface*, *Charcoal, with one*), never the `Material` header — anchoring on the header put
  four misses in the 2026-08-24 rehearsal, and *Soft* is a substring of *Soft dark*.
- **Scroll to the top of a goal before the add-task beat** — the add row is at the top, and
  the 2026-08-24 automated take lost that entire sub-flow by scrolling past it.
- **Do the Hebrew/RTL flip near the end**, because everything recorded after it is in the
  other language.

## 4 · The privacy window, restated for a live take

The automated tour's window is a pair of timecodes. A live recording has no timecodes yet, so
§1.4 restates it as **three screens** — leaderboard (a friend's real name), profile (email and
friend code), friends feed — and forces the decision **before** the record button, since the
alternative is discovering it in the edit.

## 5 · Verification

**Every UI label in the document was read out of the source, not recalled.** The one that was
wrong on the first pass is worth naming: the dashboard's share card says **"Share your weekly
progress"**, not *"Share this week's progress"* (which is the wording of a **beat label** in
`record-tour.sh`, not of the button). Corrected before commit.

Checked against source: the four bottom tabs (`TopLevelTab` — Home · Goals · Calendar ·
Social), `New goal`, `Suggest a work plan`, `Add N steps`, `Filed nowhere`, `AI coach`,
`Life areas` / `Analytics` / `Challenges` / `Copy friend code` on Profile, `Friends` /
`Everyone` / `Report score` / `Standings` on Social, and the Settings section order
(**Help → Connected apps → Appearance → Language & region → Your day → AI → Account**) from
`SettingsScreen.kt`'s own call order plus `tutorial_strings.xml`.

**The three internal anchors were recomputed and diffed, not eyeballed** — a script derived
GitHub's slug from every heading and compared it with every `](#…)` link. `BROKEN: none`.

## 🧪 Tests

**No test layer applies.** This commit adds one Markdown document and touches no Kotlin, no
Cloud Function, no resource and no rule — there is nothing to compile and nothing to assert.
No build was run and no device was touched, deliberately: three sibling sessions hold the
Gradle daemon and the emulator today.

The one mechanical check that *did* exist for this artifact — the internal-anchor recompute in
§5 — was run rather than skipped.

## 📥 KB candidates

**None, and that is a decision rather than an omission.** The transferable finding here — *an
automated capture order is optimised for reliability and a human review order for
comprehension, so the second is not the first* — is written up in §4 of the deliverable
itself, in a committed document, in the repo where the two orders both live. It is
project-specific demo craft, not cross-project methodology, so promoting it to the central KB
would put a page somewhere no future reader of either video document would look.

## Files

- `docs/marketing/manual-demo-script.md` *(new)*
- `CHANGELOG/2026-08-24/manual-demo-script.md` *(new)*
- `SESSIONS.md` — this session's claim row
