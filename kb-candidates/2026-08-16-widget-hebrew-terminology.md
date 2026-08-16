# KB candidates — `widget-hebrew-terminology`, 2026-08-16

Session: `widget-hebrew-terminology` · unit `cd49bda` / `3335202`, pushed.
Account: [`CHANGELOG/2026-08-16/widget-hebrew-terminology.md`](../CHANGELOG/2026-08-16/widget-hebrew-terminology.md).

> ## ⛔ NOTHING HERE WAS DRAINED, AND THAT IS A DECISION, NOT AN OMISSION
>
> `AUTO MODE` was in effect and would normally ingest at the commit trigger. Entry
> **1** supersedes a standing claim on `kb/dev/untranslatable-idioms.md` §3, which
> is always-ask in **both** modes (`rules/memory-promotion.md`; `/kb-ingest` §3
> rewrites the old claim in place, and overwriting committed knowledge is a
> deletion).
>
> **Entries 2–4 are additive and would have been drained on their own — they are
> held because of where they land, not because of what they claim.** All three
> belong on that same §3, and two of them qualify the very sentence entry 1
> retires. Ingesting them while §3 still asserts the retired claim would leave one
> page contradicting itself in three places, which is worse than an un-ingested
> candidate. So the set moves together or not at all.
>
> **Ido's call.** One `/kb-ingest` drains all four.

---

## 1 · `untranslatable-idioms.md` §3's live contradiction is resolved — **SUPERSEDES**

**Claim.** §3 closes with an *"Observed contradiction, recorded because it is live"*
block: `res/values-iw/widget_strings.xml` carries both of §3's defects, six strings
say מטרה for `Goal`, and it is *"outside the sweeping session's scope"*. **That is
now fixed** (`cd49bda`) and the block needs rewriting from a live defect into a
resolved one.

Two of its facts were also wrong, and both understate:

- **six strings** → **six *resources* on the terminology rule and three on the bidi
  rule, one of them on both — eight resources in total.**
- **`ל־%1$d` was not the only bidi defect.** `gp_widget_effort_lead`
  (`ו־%1$s נמצאת ב־%2$s.`) carries it **twice** in one string and appeared in no
  enumeration — not §3's, and not the brief's list of nine.

**Why.** The block is doing real work as written — it is the page's only concrete
instance — so it should keep being an instance and gain a resolution, not be
deleted. The corrected counts matter because the page's own argument is that these
defects are *undercounted by reading*, and its example undercounted them.

**Destination.** `kb/dev/untranslatable-idioms.md` §3, rewritten in place.
**Anchors.** `#51`; `Android_Final_Project@cd49bda`.
**Supersedes.** The §3 "Observed contradiction" block, in full.
**Status.** ⛔ **Always-ask — supersedes a standing claim.** Not drained.

---

## 2 · A translated string is read against the code that fills it, not as prose

**Claim.** §3 says its two defects *"are caught only by rendering in Hebrew."*
**That is not sufficient, and this session found two defects a render would have
shown without flagging.**

- `נכון לתאריך %1$s` — *"as of the **date** %1$s"* — was the brief's own
  pre-approved fix. The argument is `AndroidWidgetStrings.asOfShort` =
  `DateFormat.getTimeFormat(context)`: **a clock reading, never a date.** It renders
  as `נכון לתאריך 14:32`, which is grammatical, idiomatic, and false. A Hebrew
  reader proofing the screen has no reason to stop.
- `ב־%2$s` is a §4.8 defect **only because `%2$s` holds digits** —
  `strings.percent(...)`. Whether the rule is even violated is a fact about the
  **call site**, invisible in the resource file.

**So the check is: for every format argument, name what fills it.** Type (digits?
Latin? user-authored?) and meaning (a date? a time?). Both defects fall out
immediately; neither falls out of reading Hebrew.

**Why.** This is the localization instance of *verify by re-running whatever
consumes your output* — with the twist that here the **producer** is what must be
read, because the consumer (`String.format`) will accept anything. Rejected framing:
*"proofread more carefully"*. The first defect survived a careful writer, a careful
brief, and a pre-approval; care is not the variable.

**Destination.** `kb/dev/untranslatable-idioms.md`, new subsection under §3 or a §5.
**Anchors.** `kb/dev/look-at-your-own-output.md` (adjacent, not duplicate — that
page is about re-running your consumer; this is about reading your producer).
**Supersedes.** Qualifies §3's *"caught only by rendering in Hebrew"* — narrows it,
does not retire it. Rendering remains necessary.
**Status.** Additive, but lands on the §3 entry 1 rewrites. Held with the set.

---

## 3 · When the source distinguishes two terms and the target has one word, restructure — do not coin

**Claim.** English `Goal` (the entity) and `target` (its number) are one word in
Hebrew: **יעד**. Once the entity takes it, there is nothing left for the number.
**The remedy is to rewrite the sentence so the second noun is not needed** — here,
naming the **measure** the goal already carries (`לפי המדד שהגדרתם לו`, *by the
measure you set for it*) instead of the target.

**`Observed:` the collision is in the language as shipped software uses it, not in
one app's vocabulary.**

- **Google Fit, Hebrew help** — *"מדידה אוטומטית של ההתקרבות ליעדים והשגתם"*. יעד
  for the thing you approach; no separate word for the number.
  <https://support.google.com/fit/answer/6075067?hl=iw>
- **Microsoft Dynamics 365, Hebrew** — one page uses **both** `מדדי יעד` and
  `מדדי מטרה` for one concept, sentence to sentence. That is the collision surfacing
  inside a product with a paid localization budget.
  <https://learn.microsoft.com/he-il/dynamics365/sales/create-edit-goal-metric>

**Why.** Two answers present themselves and both are wrong. **Coining a phrase**
(`היעד המספרי`, the brief's proposal) reintroduces the collision one adjective
weaker — the same file would then use יעד for the entity and for a number two
strings apart. **Calquing** produces something that parses and that nobody says.
The third answer — *find what the sentence is actually about and name that* —
worked because the app's own model already had the word: `hasMeasure` is
`targetValue > 0 && unit is real`, so **a measure *is* the unit plus the target**,
and naming it is not a paraphrase but the more precise statement.

**Generalizes past Hebrew.** The trigger is *the source's distinction does not exist
in the target*, which is ordinary. The tell is a translator asking *"what's the word
for X?"* and getting the word already spent.

**Destination.** `kb/dev/untranslatable-idioms.md` — a fifth structural idiom
alongside §1's four, or a §3 sibling. Its §1 covers idioms of **form** (word order,
plurals, case); this is an idiom of **vocabulary**, and the page has none.
**Anchors.** `Android_Final_Project@cd49bda`; spec §5.1 / `E1`;
`Product and UX Reviews/2026-08-09-entity-model-brief.md`.
**Supersedes.** Nothing.
**Status.** Additive. Held with the set.

---

## 4 · A rule in a file header does not reach the next file — only a guard over the bucket does

**Claim.** `values-iw/analytics_strings.xml` states both wording rules in its
header, with the reasoning and a worked example. `values-iw/widget_strings.xml`,
written **afterwards, in the same `res/` tree**, violated both — eight resources.
A header is read by whoever writes that file and by nobody who writes the next one.

**Remedy: a regex guard over the whole bucket, not the file.**
`HebrewTerminologyTest` greps every `values-iw/*.xml` for the forbidden noun and for
`[א-ת]־?%\d\$`. XML comments are stripped first, because three headers legitimately
contain the forbidden word **in order to forbid it** — a guard that fires on its own
documentation gets deleted.

**Why this class of defect has no other layer.** A wrong noun compiles, packs and
renders. A name-parity test (`WidgetHebrewResourceTest`) compares resource *names*
and is blind to values **by construction**. The bidi defect happens in the Unicode
algorithm at draw time, which no JVM test can reach. What **is** checkable is the
string that causes it — so check that.

**The evidence that reading does not scale**: a careful human enumeration of one
80-line file, written specifically to list its defects, missed one of eight.

**Destination.** `kb/dev/jvm-vs-android-locale-codes.md` §5 (*a sweep is an event,
not a state*) — this is that argument one level up: **a documented rule is also an
event, not a state.**
**Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/resources/HebrewTerminologyTest.kt`;
`kb/dev/untranslatable-idioms.md` §4.
**Supersedes.** Nothing — extends §5.
**Status.** Additive. Held with the set.
