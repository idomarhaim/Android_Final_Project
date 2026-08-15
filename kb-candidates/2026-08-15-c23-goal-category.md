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

## 2. ~~A user-editable display attribute must never be a background writer's join key~~ — **DRAINED 2026-08-15**

**Ingested** into `C:\Dev\JARVIS\kb` as the new page `dev/display-attribute-is-not-an-identity.md`, by session `c21-offline-story` visiting that repo with a row on its board. Index row added; the journal entry in `kb/log/2026-08-15.md` is the cross-repo tie. `Check-KbLinks` **CLEAN at 75 pages**. Nothing superseded.

**The destination this entry named held exactly** — new page, working title *a display attribute is not an identity*, and the page kept it. The entry's own reason for not draining itself (*opening a JARVIS board row for two unrelated entries at once is one visit, not two*) is what made this drain cheap: the visit was already open for four entries.

**Entries 1 and 3 remain below and keep their original numbers** — both are `rules/`-destined and ⛔ always-ask in both modes, so no `AUTO MODE` drain reaches them.

---

## 3. The comprehension complaint fired three times in one day — and the third kills the standing diagnosis

**Claim.** *"I couldn't understand the options — you choose, and improve it"* fired **three times on
2026-08-15** in verbatim-identical Hebrew: `product-v03-spec` ~14:3x, `c23-goal-category` ~17:5x, and
`c23-goal-category` again ~18:1x. **The third was not a product question.** It asked which session to
open next, with four thin options naming concrete actions and a consequence table in the reply above it.
Same answer.

**Why it matters.** The check-order rule sorts this tell into ownership → premise → form → density, and
the first two instances were each explained by one of those cells — dense options, then a false premise
(entry 1). The third has **none of them**: ownership was sorted, the options were actions rather than
mechanisms, the premise was a live board state, and the set was already minimal. So the diagnosis those
two produced is **wrong, or at least not the operative cause**. What survives all three instances is the
request itself: *a schematic explanation and a decision, not a menu.*

`Observed:` three instances, timestamped, one of them non-product.
`Inferred:` that the operative variable is the **format of being asked**, not any property of the
particular question.
`Untested:` whether Ido would say the same — nobody has asked him, and asking is the very act in question.

**Rejected:** (a) *"pickers do not work for this user"* — too strong; he answered the first picker of the
day fluently. (b) Reducing the picker further, which is what the density cell prescribes and what the
second instance already disproved.

**What it would change.** The remedy is not a better picker but a different default on this project:
derive, explain schematically, decide, record the decision as the agent's, and say plainly it is his to
overturn — reserving the picker for what genuinely turns on his life or values. Mirrored, for the
harness's own recall, in `~/.claude/projects/c--Dev-Android-Final-Project/memory/delegation-instead-of-picker.md`.

**Destination.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — **`rules/`, always-ask**, and it is a
protocol change, so the 🎬 walkthrough gate applies.

**Anchors.** `SESSIONS.md` release note for `product-v03-spec` (the 🧠 block quoting his words) ·
`CHANGELOG/2026-08-15/c23-goal-category.md` · issue #45's resolution comment, opening paragraph.

**Supersedes.** Nothing yet — it is a **second data point on a standing diagnosis**, which is exactly
what the rule asks a session to record rather than re-derive.

**Status.** ⛔ **Always-ask** — `rules/` destination. Not drained.
