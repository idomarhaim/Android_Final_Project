# KB candidates — `c23-goal-category`, 2026-08-15

Session: `/wayfinder 12` → [`C23` #45](https://github.com/idomarhaim/Android_Final_Project/issues/45),
resolved and closed. Every entry stands alone: no entry here may be reconstructed from this session's
transcript, which is machine-local and invisible to the next agent.

---

## 1. A false fork is found in the duplicated *derivation*, not in the duplicated field

**Claim.** When two objects look like rival answers to one question, the intersection the
derivation-closure grep is meant to find usually surfaces as **the same value computed twice** —
a palette, a formatter, a label mapper — not as two fields with the same name. Grep the *derivations*
of the two candidates before grepping the candidates.

**Why.** `C23` asked whether `GoalCategory` (app-authored, closed, English) or `LifeArea`
(user-authored, open, Hebrew) is the real axis. Grepping the two models against each other returns a
clean separation — different packages, different lifecycles, one references the other only in a doc
comment. The collapse is in `LifeAreaPalette`: **the same ten hex values, copy-pasted**, plus
`iconKeyFor(name)`, a bilingual guesser that already maps `בריאות`→favorite and `ריצה`→fitness. So the
user-authored object was *already* deriving colour and icon better than the enum, and three of the four
options put to the user were dead on arrival. The direct-pair grep passes and says nothing about being
run at the wrong width — the known failure mode of a search run too narrow.

**Rejected:** reading the duplication as a naming problem (rename one of them), which is what a
field-level grep suggests.

**Destination.** Extends the fork-check in `C:\Dev\JARVIS\rules\question-axis-naming.md` (the
derivation-closure bullet) — **`rules/`, therefore always-ask in both modes**, and the 🎬 walkthrough
gate owns it.

**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/domain/model/LifeArea.kt` L38-L108 ·
`domain/model/Goal.kt` L59-L78 · issue #45's resolution comment §1.

**Supersedes.** Nothing; it narrows an existing rule's *how*, not its *what*.

**Status.** ⛔ **Always-ask** — `rules/` destination. Not drained.

---

## 2. A user-editable display attribute must never be a background writer's join key

**Claim.** Where an automatic, unreviewed writer decides *"does this thing already exist?"*, the key it
matches on must be an identity the user cannot edit. A presentation attribute — category, label,
colour, title — offered to the user as a chip is a **join key that silently changes under the writer**,
and the failure is a duplicate record nobody is watching for.

**Why.** GoalPilot's Health Connect sync matches an existing goal by
`it.category == metric.category` (`BuildHealthProposalsUseCase.kt:167`), and the same category is an
editable chip in `AddEditGoalScreen`. Editing it orphans the goal from the sync, which then creates a
second "Weekly steps" goal. The sync runs on every foreground with **no review sheet**, so the duplicate
appears as a goal the user thinks they forgot making. Filed as
[#47](https://github.com/idomarhaim/Android_Final_Project/issues/47). The general shape is the one
`AGENTS.md` already states for this sync — *"no human is watching"* — applied to the **key** rather than
to the lookups.

**Rejected:** treating it as a Health-Connect bug. It is a schema-discipline bug that Health Connect
merely made visible; the same defect would appear in any importer, sync or dedupe.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — new page (working title
*a display attribute is not an identity*). Cross-repo: ingesting it owes a row on the JARVIS board.

**Anchors.** `BuildHealthProposalsUseCase.kt:167`, `:30`, `:38` · `AddEditGoalScreen.kt:84` ·
`AGENTS.md` pitfall *"the Health Connect sync is automatic and unreviewed"*.

**Supersedes.** Nothing.

**Status.** 🟢 **`AUTO MODE`-eligible, not drained** — deliberately: this session already carries a
cross-repo debt it did not create (see `2026-08-15-product-v03-spec.md` entry 2), and opening a JARVIS
board row for two unrelated entries at once is one visit, not two. Next session into JARVIS takes both.

---

## 3. The comprehension complaint arrived a second time on a picker that had already been reduced

**Claim.** *"I couldn't understand the options — you choose"* fired **twice in one day on the same map**
(`product-v03-spec` ~14:3x, `c23-goal-category` ~17:5x), in near-identical Hebrew wording. The second
instance is evidence the first diagnosis was incomplete: that session concluded the reply above the
picker had not carried the reasoning, and the second picker **did** carry it — full prose ground above
the tool call, four thin options, a single question, framed as a situation with consequences — and drew
the same answer.

**Why it matters.** The check-order rule sorts this tell into ownership → premise → form → density. On
the second instance, ownership was sorted (the artifact half was derived and shown, not asked), the
premise was false and the picker inherited it (see entry 1), and the form was already a situation. So
either the premise failure alone explains it — which entry 1 would then fully cover — or the true
variable is that **the schematic explanation is wanted before any question is put at all**, and no
amount of reducing a picker reaches that. Distinguishing the two needs a third instance, or Ido's word.

**Rejected:** concluding from one repeat that pickers do not work for this user. Two instances, both on
the same map and both on genuinely load-bearing product questions, do not support that.

**Destination.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — **`rules/`, always-ask**, and it is a
protocol change, so the 🎬 walkthrough gate applies.

**Anchors.** `SESSIONS.md` release note for `product-v03-spec` (the 🧠 block quoting his words) ·
`CHANGELOG/2026-08-15/c23-goal-category.md` · issue #45's resolution comment, opening paragraph.

**Supersedes.** Nothing yet — it is a **second data point on a standing diagnosis**, which is exactly
what the rule asks a session to record rather than re-derive.

**Status.** ⛔ **Always-ask** — `rules/` destination. Not drained.
