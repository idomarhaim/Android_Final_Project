# KB candidates — `c7-what-is-a-unit`, 2026-08-10

Written per `rules/memory-promotion.md`. **Normal mode**, so this is a proposal:
nothing here has been ingested. Each entry stands alone — a reader with no access to
this session's transcript has everything needed to write the page.

**Folder state when this session listed it, before its first unit of work:**
`2026-08-08-c9d-calendar-scopes.md` and `2026-08-08-fix-task-completion-feedback.md`
were both still un-drained. Both sessions have since **released**, so neither is a live
claim — they are simply awaiting Ido's approval. Reported, not absorbed.

---

## 1. Split a field into the half that must be computable and the half that must be free

**Claim.** When a value has to be both *reasoned about by code* and *chosen freely by a
human*, the usual choice — a closed enum, or free text — is a false one, and both
options fail in a predictable direction. A **closed enum** makes the system fully
knowledgeable and partly **mute**: anything not in the list cannot be expressed at all.
**Free text** makes it expressive and **stupid**: nothing downstream can validate,
convert, or branch on it, so every consumer degrades into string matching. The third
shape is to **store two fields** — a small closed *kind* that owns every computation,
and a free *label* that owns everything a human reads.

The discriminator for whether this applies: ask what the code actually needs. If it
needs only a *class* of behaviour (how to round, what increments to offer, whether two
values are comparable) and never the specific word, then the word was never the code's
business and should not have been modelled as if it were.

**Why.** The concrete case: a goal-tracking app stored `unit: String` — `"litres"`,
`"steps"`, `"%"` — and never looked at it, only concatenated it into labels. That made
a whole class of features unbuildable: repeat-tappable increment buttons ("+250 ml") had
nothing to attach to, a health importer's guard against writing step counts into a
"workouts" goal came down to `unit.equals(other, ignoreCase = true)` with a fall-through
that defeated it, and the same matching would have had to work in Hebrew too. Replacing
it with a closed list of concrete units was rejected for the opposite reason: the app
*generates goals from free text*, so "read 30 chapters" and "500 Spanish words" arrive
constantly and a fixed list cannot say them. Kind + word is the only shape where nothing
is unsayable and nothing is unknowable.

Two secondary properties worth stating because they are what settle the argument in
practice:
- **It matches what an LLM is reliably good at.** Choosing among enum values written
  into the prompt was measured at 100% on a small free model; free-form identifiers from
  the same model came back **silently corrupted**. So the computable half is exactly the
  half safe to ask a model for, and an imperfect word is harmless because nothing
  computes on it.
- **It lands on the localization boundary for free.** The label is user content (never
  translated); the kinds are app-authored words (which owe a translation). Systems that
  model this as one free-text field cannot answer "translate this or not?" at all.

Rejected alternative worth recording: a **full dimensional model** (every unit carries a
dimension and a conversion factor to a canonical base, so ml↔L↔m³ interconvert). It is
the "correct" engineering answer and it was wrong here — it buys conversions the
application never performs, at the cost of rewriting every stored value. The cheaper
substitute is **one canonical storage unit per kind, with display as a formatting rule**
(`0.25` volume renders as `250 ml`), which yields the same user-visible behaviour with
no conversion table.

**Destination.** Central KB — `kb/dev/`. A modelling pattern, not Android- or
Firestore-specific. Adjacent to `kb/dev/llm-structured-output.md` (which holds the
enum-reliability measurement this leans on) and to `kb/dev/localization-axes.md` (which
holds the user-content vs app-authored boundary) — but it is neither of those pages'
subject, so it wants its own.

**Anchors.** [#14 resolution §1](https://github.com/idomarhaim/Android_Final_Project/issues/14#issuecomment-5238464343);
`CHANGELOG/2026-08-10/c7-what-is-a-unit.md`;
`app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt` (`unit: String = "%"`);
`app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildHealthProposalsUseCase.kt`
(`matchFor`, the string-equality guard and its fall-through).

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 2. Give the model the categorical half and the code the arithmetic half

**Claim.** When a feature reads as "let the AI decide X", check whether X is really two
jobs: a **judgement** (which class is this? does this apply here? what is being counted?)
and an **arithmetic** (which numbers exactly?). Route the judgement to the model and the
arithmetic to deterministic code. The reason is not aesthetic — it is that the two have
opposite reliability profiles on the same model, and mixing them makes the whole feature
inherit the worse one.

Corollary, and the sharper form of the rule: **arithmetic first, the model only where
arithmetic cannot answer.** If a relationship between two quantities is derivable (a
percentage of a known target, a unit conversion), computing it is both free and exact,
and asking a model for it is strictly worse. The model earns its place precisely where
no arithmetic relationship exists ("how many pages is a book?").

**Why.** The same split resolved two unrelated sub-questions in one design session,
which is what makes it a pattern rather than a one-off:
- *Which increment buttons should a goal offer?* → the model answers **"do buttons fit
  here, and what is being counted"**; the code computes the ladder from the goal's own
  target.
- *How do you reinterpret logged history when a measure changes?* → the code converts
  where the conversion is division; the model proposes only where no relationship
  exists, and its proposal is shown for approval before it applies.

The measurement underneath: on the small free model in use, prompt-declared enum values
came back **50/50 correct**, while free numbers inside an otherwise-valid response
swung **up to 2× between identical runs** and **1.8× between languages**. So a
model-authored numeric ladder would silently differ from one day to the next with no
visible cause — the failure would present as "the app is inconsistent", not as an AI
error.

The user's own framing is worth preserving because it is the common intuition and it is
half wrong: *"I assume only AI would know how to say that."* True of the judgement,
false of the numbers.

**Destination.** Central KB — `kb/dev/`. **Check `kb/dev/llm-structured-output.md`
first**: that page (written 2026-08-08 from the measurement cited here) may be the right
home as a new section rather than a new page, since it already owns the enum-vs-free-value
reliability result. This entry is the *design rule* that result implies; if it lands as a
separate page, the two must cross-link.

**Anchors.** [#14 resolution §4 and §5](https://github.com/idomarhaim/Android_Final_Project/issues/14#issuecomment-5238464343);
`docs/research/2026-08-08-free-model-format-probe.md` (the 248-call probe: enum accuracy,
the 2× numeric swing);
[#16](https://github.com/idomarhaim/Android_Final_Project/issues/16).

**Supersedes.** Nothing. **Extends** `kb/dev/llm-structured-output.md` — resolve where it
belongs before writing, and update in place rather than restating its measurement.

---

## 3. A plausible default can be the defect — make absence representable and default to it

**Claim.** When a field is given a default that is a *valid-looking value* rather than
"unset", the system loses the ability to distinguish **"the user decided this"** from
**"nobody decided anything"**. The damage is not the wrong value; it is that the record
now asserts something no one asserted, and every downstream consumer treats the assertion
as real. The fix is rarely a better default — it is making **absence a representable
state** and making it the resting one, so the lazy path claims nothing.

The tell that you have this bug: a field whose default is technically correct and
semantically empty (a `"%"` unit, a `0` score, an `"Other"` category, a `1.0` weight),
combined with a UI that shows it identically to a value someone chose.

**Why.** The concrete case: `Goal.unit` defaulted to `"%"` and `targetValue` to `100.0`,
so a goal created without thinking about measurement rendered as *"1 / 100 %"* — a
progress bar, a percentage, and a claim to be measuring something. The user's real goal
*"Drink 4 Liters of Water Daily"* had been sitting in that state for weeks, looking
tracked and tracking nothing. The instinct is to fix the *default* (make it "times"?
infer it from the title?); the actual fix is that a goal may legitimately have **no
measure at all** — the user's own example, *"understand real estate"*, is a goal that
should stay unmeasured forever — so absence had to become expressible before any default
could be chosen honestly.

The general shape: **a default that is indistinguishable from a decision converts an
absent decision into a false one**, and it does so silently, at scale, for as long as the
field exists. Nullable-with-meaning is not a code smell here; it is the only honest
model.

Note the deliberate second half, because "just make it nullable" under-delivers: absence
being legal does not mean absence should be silent. In this case the resolution pairs it
with an agent that *offers* a concrete measure and can be dismissed permanently — legal,
but never unremarked.

**Destination.** Central KB — `kb/dev/`. A modelling/defaults principle that generalises
past this app. Possibly the same page as entry 1 (both are "model the thing honestly
rather than conveniently"), but the claims are independent and each stands alone —
whoever ingests should decide.

**Anchors.** [#14 resolution §2 and §3](https://github.com/idomarhaim/Android_Final_Project/issues/14#issuecomment-5238464343);
`app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt`
(`unit: String = "%"`, `targetValue: Double = 100.0`);
`Product and UX Reviews/2026-08-09-entity-model-brief.md` `E6`;
issue [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11) (the live goal).

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 4. For a goal that resists measurement, measure the behaviour that produces it

**Claim.** Some objectives genuinely have no honest outcome metric — *"understand real
estate"*, *"be a better listener"*, *"stay current in my field"*. The two usual responses
are both bad: **force a number** (which produces a fake the user learns to ignore) or
**leave it unmeasured** (which leaves the user with no feedback at all). The third is to
measure a **leading indicator** — the recurring behaviour that produces the outcome
(*"read 2 market reports a week"*) — and to be explicit that this is what is being
measured. It gives real feedback without pretending the outcome itself was quantified.

Practical consequence for software that suggests metrics: the suggestion engine needs
**two output shapes**, not one — a measure *on* the objective, and a recurring activity
*under* it — and choosing between them is part of the suggestion, not a detail.

**Why.** The user wrote both halves of this himself without naming it: that an AI agent
*might* advise making "understand real estate" more measurable, and immediately that
maybe it should not, "because one of the things is to be well-versed in what is current
in the field, and that requires some endlessly recurring task." The recurring task *is*
the measurable thing. Separately he stated the product position that goals in life
usually need to be measurable in some form — so the design has to satisfy both, and a
leading indicator is the only construction that does.

Worth recording because the failure mode is common in goal-tracking and OKR tooling: the
tools that insist on a number for everything produce vanity metrics, and the tools that
allow unmeasured goals quietly abandon them.

**Destination.** **Project-local** — `knowledge/` in this repo, as product-domain
reasoning about GoalPilot's measurement model. Promote centrally only if a second project
shows the same shape; it is closer to product design than to engineering practice, and
the central bundle's `dev/` framing does not fit it.

**Anchors.** [#14 resolution §3](https://github.com/idomarhaim/Android_Final_Project/issues/14#issuecomment-5238464343);
`Product and UX Reviews/2026-08-09-entity-model-brief.md` `E5`/`E6`;
`CHANGELOG/2026-08-10/c7-what-is-a-unit.md`.

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 5. Redacting before publication creates a second copy — and no push rule catches it

**Claim.** The standard way to fix "private content is about to be published" in git is
to redact the file and rewrite the unpushed commit. That is correct, and it silently
creates a **second artifact with the opposite property**: a backup ref holding the
*original*, un-redacted blob, sitting in the same local repository as the branch you are
about to publish. The redaction is only as good as that ref never being pushed — and
**the usual push safety rules do not cover it**, because they are all written about the
*current* branch:

- "read the outgoing diff" reads `@{u}..HEAD` — the backup ref is not in it;
- "fast-forward only / no `--force`" is satisfied — `git push --all` is neither;
- "no secrets in the diff" is satisfied — the secret is in a different ref entirely.

So the one command that would undo the entire fix, `git push --all` (or a mirror push, or
a CI job that pushes every ref), passes every check. The rule that actually holds is
**treat the backup ref as part of the incident**: it is not cleanup to be done later, it
is the second half of the redaction, and the redaction is not complete until it is gone.
Where a backup must be kept while the fix is confirmed, it should be a **tag or ref
outside `refs/heads/`**, or a bundle outside the repo, so no branch-level bulk push can
reach it.

**Why.** The concrete case: a screenshot committed by an earlier session showed a user's
private task list, and the repository was public. It was caught by the pre-push
read-what-you-are-sending check, one commit before publication — the cheap moment. The
fix (redact, `commit --amend`, `rebase --onto`) worked exactly as intended and the push
went out clean. What the fix *left behind* was `backup/pre-redact-<date>`, a local branch
whose only distinguishing content is the un-redacted image. Nothing in the project's
`AGENTS.md`, its `.github/` instruction files, or the six auto-push preconditions in
`general.instructions.md` names `git push --all`, mirror pushes, or backup refs at all —
checked, not assumed. The gap is real and it is in the *general* rules, not in one repo.

Worth a page because the failure is counter-intuitive: it is created **by** doing the
right thing, it is invisible to every check that exists, and the window in which it is
cheap to close is the same window in which everyone has stopped paying attention because
the problem "is fixed".

**Destination.** Central KB — `kb/dev/`. It generalises to any repo and any VCS host.
**Note the routing consequence:** if the conclusion is that the *global push rule* should
name `--all`/mirror pushes and backup refs, that is a `rules/` change, which is
**always-ask** and goes through the 🎬 walkthrough gate — not something an ingest may do.
The KB page can state the finding; only Ido can move the rule.

**Anchors.** `docs/research/2026-08-09-oauth-production-test/README.md` (the redaction
note, in place); commit `3b0340c` (the amended commit and its message);
`CHANGELOG/2026-08-10/c7-what-is-a-unit.md`; `.github/instructions/general.instructions.md`
(the six preconditions, none of which catches this).

**Supersedes.** Nothing. **Adjacent to** the standing global _Commits & pushing_ rule —
this does not contradict it, it names a case it does not cover.

**Status.** Proposed, not ingested. Surfaced by Ido's question *"is it written in the
project's instructions not to do `git push --all`, so the agent knows?"* — the answer was
no, and that is the finding.
