# KB candidates — `c9b-calendar-surface`, 2026-08-10

Written during `/wayfinder 12` (working
[#26](https://github.com/idomarhaim/Android_Final_Project/issues/26), `C9b` — the
in-app calendar surface). **The ticket is still open**; these landed while building
and revising the prototype.

> ⚠️ **Written late.** The duty is to mirror each `📌` into this file *the moment* it
> lands, and three commits (`cb13884`, `9e5f5ef`, `cd30372`, `4e4fa98`) went out before
> this file existed. Nothing was lost — every entry below is reconstructed from the
> committed changelog and the prototype itself, not from chat history — but the drain
> that should have ridden those commits did not, and the ingest is now owed at the
> next commit trigger.

---

## 1. In an RTL UI, a time or date string must be direction-isolated or the bidi algorithm reverses it

**Claim.** `09:00–12:00` is a run of Latin digits inside a right-to-left paragraph. The
Unicode bidirectional algorithm reorders it and it renders as `12:00–09:00` — visually
plausible, completely wrong, and it then clips because the reversed string overflows
differently. The fix is `direction:ltr; unicode-bidi:isolate` (CSS) or the equivalent
`LTR` isolate marks in Compose, applied to **every** time, date, range and version
string, not to the ones that happen to look wrong.

**Why.** Observed live in the `C9b` prototype and photographed by Ido before anyone
looked for it — which is the point: the bug is **silent and plausible**, so it survives
review by anyone reading the text as a sentence rather than as two numbers. It is a
property of the **text**, not of the rendering technology, so it recurs identically in
Compose, in a WebView, and in a PDF export. **Rejected:** widening the container (treats
the clip, leaves the reversal), and hand-ordering the string (breaks the moment the
locale flips back to English).

**Destination.** `kb/dev/` — a page on RTL/bidi traps in bilingual UIs. Likely new;
check for an existing i18n page first.

**Anchors.** `docs/prototypes/2026-08-10-calendar-surface/index.html` (the `.ltr` rule
and the `time()`/`range()` helpers) · `CHANGELOG/2026-08-10/c9b-calendar-surface.md`
§ Rev 3 · GoalPilot ships Hebrew per `C15`
([#15](https://github.com/idomarhaim/Android_Final_Project/issues/15)).

**Supersedes.** Nothing known.

**Status.** Pending — ordinary ingest, `AUTO MODE` eligible.

---

## 2. A UI symbol that has to be cryptic is a decomposition failure, not an icon-choice failure

**Claim.** When a single UI element is forced to encode **two independent axes**, the
weaker axis degrades into an unreadable glyph. The tell is a user saying *"I can't tell
what these little icons mean and they don't look connected to anything"* — which reads
as a request for prettier icons and is actually a report that two things were merged
that should not have been. The fix is to **split the axes**, never to redraw the symbol.

**Why.** In `C9b` one chip carried both the **rung** (a property of the *occurrence*:
block / deadline / span / all-day) and the **life area** (a property of the *goal*).
With one pill for two axes the rung had nowhere to go but a symbol vocabulary
(`▮ ▼ ▭ ⟷`) nobody could learn. Separated, the chip carries only the life area and the
rung is carried by the **form of the leading time column** plus the words themselves —
and the legend disappeared entirely. **Rejected:** better iconography (would have
produced a prettier vocabulary that still had to be learned), and a legend (an element
that exists to explain another element is the same failure with an extra step).

**Destination.** `kb/design/` or `kb/dev/` — a page on encoding axes in UI. New.

**Anchors.** `CHANGELOG/2026-08-10/c9b-calendar-surface.md` § Rev 2 ·
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) Standing
preferences, where Ido made the design standard normative for the whole map on
2026-08-10.

**Supersedes.** Nothing known. **Note:** the map-level *preference* is already committed
to `#12`; this entry is the **generalisable principle** behind it, which belongs outside
this project.

**Status.** Pending — ordinary ingest, `AUTO MODE` eligible.

---

## 3. Generating a test file through a shell heredoc destroys backslash escapes, and a surviving `\b` in a template literal becomes a backspace character

**Claim.** Writing a JavaScript check file via `cat >> file <<'EOF'` does not reliably
preserve `\\`. When `\\b` arrives as `\b` inside a **template literal**, JS interprets it
as **U+0008 BACKSPACE** rather than as a regex word-boundary escape — so
`new RegExp(\`class="[^"]*\\b${c}\\b[^"]*"\`)` silently becomes a matcher containing an
invisible control character. It compiles, it runs, it returns `false`, and it reports a
correctly-rendered element as missing. Write generated test files with a **file-writing
tool**, and prefer `split()`/`includes()` over regexes that need escapes.

**Why.** Cost **four false failures across three revisions** of the `C9b` prototype,
each of which looked exactly like a defect in the artefact. The one that mattered: it
masked the boundary between three test bugs and **one real bug** (variant B not carrying
`AWAY` occurrences forward). The discipline that separates them is *reproduce the
assertion in isolation before touching the artefact* — printing `regex.source` exposed it
in one command. **Rejected:** `String.raw` (works, but only if you remember the heredoc
mangled the input in the first place — it treats the symptom).

**Destination.** `kb/dev/` — extends the existing encoding-traps material; check
`dev/powershell-encoding-traps.md` for fit before creating a page, since that page is
about PowerShell and this is bash-heredoc-into-Node.

**Anchors.** `CHANGELOG/2026-08-10/c9b-calendar-surface.md` § Tests ·
`C:\Dev\JARVIS\kb\dev\powershell-encoding-traps.md` (adjacent, possibly the host page).

**Supersedes.** Nothing — additive to the existing traps material if it lands there.

**Status.** Pending — ordinary ingest, `AUTO MODE` eligible.
