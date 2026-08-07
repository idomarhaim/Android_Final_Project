# KB candidates — `product-model-map`, 2026-08-08

Un-ingested. Normal mode, so this list is a **proposal**: nothing is ingested until
Ido approves it. Each entry stands alone — no transcript is a source.

Note: `kb-candidates/2026-08-06-product-review.md` and
`kb-candidates/2026-08-06-product-device-pass.md` were both still un-drained when
this session ran, and were reported rather than absorbed. They are not this
session's to drain.

---

## 1. A constraint ticket that "prices everything" is usually two tickets, and only one of them is takeable

**Claim** — When planning work, a cross-cutting constraint often presents as the
obvious root: *"answer this first, it prices all the others."* Before accepting it
as the root, split it on the axis of **what is measurable now versus what is
downstream of the very things it claims to price.** The measurable half is real,
unblocked, and usually AFK. The other half is not a root at all — it is the most
blocked ticket on the map. Charting them as one produces a root ticket that must
*invent* the artifacts it was meant to be evaluating.

**Why** — The concrete case: `C11`, *"what can a free model be trusted to do, and
what are the formats?"*, was proposed as the root that prices four other decisions.
It is two questions. *What can the model do against a fixed format* is measurable
today with a probe. *What are the formats* cannot be written until the features
exist — **you cannot test a format nobody has designed yet.** As one ticket it
would have had to invent candidate formats in order to test them, and those
inventions would then have constrained the feature decisions that were supposed to
produce them. Split, the probe became one of only two AFK tickets on the frontier
and the format spec became the most-blocked ticket on the map (four blockers).
Rejected: keeping it as the single root, which the source TODO explicitly proposed
and which produced a cleaner-looking graph.

The same session found the **converse** error in the same document and it is worth
recording together, because the two look alike and the fix is opposite — see
candidate 2.

**Destination** — central KB, `kb/dev/`. It is a planning/decomposition method,
not GoalPilot-specific and not Android-specific. Adjacent to
`rules/scale-adaptive-ceremony.md` and to the `/wayfinder` flow page if one exists.

**Anchors** — [#16](https://github.com/idomarhaim/Android_Final_Project/issues/16),
[#30](https://github.com/idomarhaim/Android_Final_Project/issues/30),
`TODO/TODO_FUTURE/ProductModel.TODO.future.md` (the *tested and partly overturned*
section), `CHANGELOG/2026-08-08/product-model-map.md`.

**Supersedes** — nothing.

**Status** — pending Ido's approval.

---

## 2. "These are one knot, none can be answered alone" usually means the chain has never been drawn

**Claim** — A set of decisions described as inseparable is a signal worth
inspecting, not a verdict. It has two possible causes and they take **opposite**
fixes: either they genuinely are one decision (merge into one ticket), or nobody
has yet worked out which precedes which (order them, and the knot dissolves). The
discriminator is whether you can state a *reason* one question's answer changes the
shape of another's — if you can, that reason is a blocking edge, not evidence of a
knot.

**Why** — The concrete case: four decisions (`C1` points-and-time model, `C2` task
types, `C3` points↔progress, `C4` goal↔task ontology) were handed over as *"one
knot; none can be answered alone."* Drawing it out gave a clean chain —
`C4` → `C3` → `C1` → `C2` — on a stated reason at each edge: what a task and a goal
*are* precedes whether their numbers join, which precedes who may author those
numbers, which precedes whether a type feeds the authoring. The practical payoff is
not tidiness: it moved the map's root from the constraint ticket everyone expected
(`C11`) to `C4`, and it made `C7` visible as **unblocked** when it had been filed
under the knot's consequences. Rejected: merging the four into one ticket, which is
the standard advice for a genuine knot and would have produced one ticket far too
large for a session.

**Destination** — central KB, `kb/dev/`, same page as candidate 1 — they are the
two halves of one method (split what looks atomic, order what looks tangled) and
were found in the same document within minutes of each other.

**Anchors** — [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13),
[#14](https://github.com/idomarhaim/Android_Final_Project/issues/14),
[#18](https://github.com/idomarhaim/Android_Final_Project/issues/18)–[#20](https://github.com/idomarhaim/Android_Final_Project/issues/20),
`TODO/TODO_FUTURE/ProductModel.TODO.future.md`.

**Supersedes** — nothing.

**Status** — pending Ido's approval.

---

## 3. GitHub now has native sub-issues *and* native blocked-by, reachable from `gh`

**Claim** — A `/wayfinder` map on GitHub Issues no longer needs the body-convention
fallback for either hierarchy or blocking. Both are native and scriptable:

- **Sub-issues** — `gh issue edit <parent> --add-sub-issue <n>[,<n>…]` and
  `gh issue edit <child> --parent <n>`, present in `gh` **2.96.0**. Also
  `--remove-parent` / `--remove-sub-issue`.
- **Blocking** — no `gh` porcelain, but the GraphQL API exposes the mutations
  `addBlockedBy` / `removeBlockedBy` and the fields `blockedBy`, `blocking`,
  `issueDependenciesSummary` on `Issue`. Input shape is
  `addBlockedBy(input:{issueId, blockingIssueId})` — both **node IDs**, not
  numbers, so a number→id resolution pass is needed first.
- **The frontier is therefore one query**: children of the map that are `OPEN`,
  have zero `OPEN` entries in `blockedBy`, and have no assignee.

**Why** — The wayfinder skill states that native blocking is *essential* rather
than cosmetic, because it renders the frontier in the tracker's own UI, so a human
sees what is takeable without opening the map — and it only falls back to a
`Blocked by: #N` line in the body where the tracker lacks the primitive. Whether
GitHub had it was unknown before this session and was established by probing the
schema (`__type(name:"Issue")`, `__type(name:"Mutation")`), not by documentation.
Worth writing down because the alternative is a body convention that no query can
read reliably, and because the node-ID requirement is the one thing that makes the
mutation fail on a first attempt.

**Destination** — central KB, `kb/dev/`. A tool-capability fact about `gh` and the
GitHub API; it generalises to every repo, and it is the kind of thing that is
re-derived from scratch each time it is needed.

**Anchors** — `CHANGELOG/2026-08-08/product-model-map.md`,
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) and its 20
children — a live example of the shape.

**Supersedes** — nothing. **Caveat for whoever ingests it:** this is a
*capability* claim about a third-party product, so it carries a date and is the
kind of page that rots. `gh` 2.96.0, checked 2026-08-08.

**Status** — pending Ido's approval.
