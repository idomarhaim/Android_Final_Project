# KB candidates — `65-measure-proposal`, 2026-08-23

**Partly drained 2026-08-23.** Four of five entries are closed; this file is rewritten down to its
one survivor rather than deleted (`rules/derivable-decision.md` §1 permits deleting only a **fully**
drained file). Ingest log: `C:\Dev\JARVIS\kb\log\2026-08-23.md`.

| Closed entry | Outcome |
|---|---|
| `/**` inside a KDoc opens a nested Kotlin block comment | **already recorded** — `kb/dev/prose-punctuation-is-syntax.md`; its published regex was re-run against this session's own broken text and **fires** |
| A Compose render capture drops everything below the fold | **already recorded** — `kb/dev/look-at-your-own-output.md` §4g, same repo, same fix |
| Git Bash rewrites the device path in `adb pull` | **already recorded** — `kb/dev/android-device-verification.md` §8a, already widened twice |
| `bidiIsolated()` defeats an exact-text Compose matcher | 📥 **ingested** — `kb/dev/look-at-your-own-output.md` §5.4 *(new section)* |

---

## 5 · A spec row's literal reading can produce a self-disagreeing number

**Claim.** `PRODUCT_v0.3` §3.4 reads *"`openStepCount >= 2` → count the steps you already listed —
target = the count"*. Taken literally, the target is the **open** count, which **shrinks every time a
step is completed** — the goal reads `0/6`, then `0/5`, after real progress. That violates §0.3, the
map's own most-repeated finding. The design asset renders the other reading (frame 4: *"2 of 8
done"*). Resolution shipped in `#65`: the **gate** stays `openStepCount >= 2` exactly as specced, the
**target** is the total step count.

`Observed:` 2026-08-23 while building
[`#65`](https://github.com/idomarhaim/Android_Final_Project/issues/65).

**Why.** The general shape is worth more than the instance: **a spec sentence and its own design
asset can disagree, and the disagreement is only visible once you run the arithmetic forward in
time.** Both readings are correct at the moment the feature fires — *when the goal first becomes
eligible*, nothing is done, so open == total. The divergence appears only later, which is exactly why
reading the sentence against the mockup does not surface it. Adjacent to but not the same as
`kb/dev/a-later-prototype-outranks-the-brief.md`, which is about **precedence** between two
artifacts; this is about two artifacts that **look like they agree** and do not.

**Destination.** ⚠️ Deliberately unresolved. Either `kb/dev/a-later-prototype-outranks-the-brief.md`
(as a second failure mode of the same pair) or an annotation on `docs/PRODUCT_v0.3.md` §3.4 itself.

**Status.** **blocked — needs Ido, and the block is the always-ask rule, not uncertainty.**
Annotating §3.4 is a rewrite of committed decision text in a map he owns. The *code* is shipped and
argued in `GoalStructure`'s KDoc and in
`MeasureProposalTest.the steps target does not shrink as steps are completed`, so nothing is lost if
this sits.

**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/domain/model/MeasureProposal.kt`
(`GoalStructure`) · `CHANGELOG/2026-08-23/65-measure-proposal.md` §*The three decisions worth arguing
with*.

**Supersedes.** Nothing; it would *annotate* §3.4.
