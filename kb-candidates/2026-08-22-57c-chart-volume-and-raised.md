# KB candidates — `57c-chart-volume-and-raised`, 2026-08-22

Written under AUTO MODE. Entry 1's destination is `rules/`, which is **always-ask in both
modes** (`rules/memory-promotion.md`), so it is parked here rather than drained. Entry 2 is
drainable and is **owed** — see its `Status`.

Each entry stands alone. No transcript is a source.

---

## 1 · `git tag` has the shared-working-tree hazard, and it has no pathspec form

**Claim.** In a repo running parallel sessions in **one working tree**, `git tag <name>` with no
explicit commit is exactly as unsafe as a bare `git commit` — it captures whatever `HEAD` is at
that instant, which a sibling may have moved since your own push. And unlike `git commit`, **there
is no pathspec form to defend with**: the remedy the commit rule prescribes (*"name your paths"*)
has no analogue, because a tag names a commit rather than a set of files. The remedy is to
**create the tag at an explicit SHA you have read** — `git tag -a v1.2.3 <sha>` — and never at
`HEAD`.

**Why.** `Observed:` 2026-08-22, GoalPilot. This session pushed `main` at `915a388`, then created
and pushed `v0.3.1`. The release workflow's checkout reported `HEAD is now at d752342` — a commit
authored by `57b-backgrounds-and-combinations`, landed in the shared tree in the ~90 seconds
between the push and the tag. So the tag **published a foreign commit that was not yet on the
remote branch**, under this session's release, without it having been read.

Three things make this worth a rule rather than a note:

1. **It escapes every existing gate.** The six auto-push preconditions were all run and all passed
   — on `@{u}..HEAD`, *before* the sibling commit existed. A gate that runs before the window
   opens is not a gate for that window. Precondition 5's foreign-commit adjudication never fires,
   because at the moment it ran there was nothing foreign in the range.
2. **A tag push IS a publish, and is separately always-ask.** *"Creating or deleting a branch/tag
   on the remote"* stays always-ask in both modes precisely because it is outward — and the thing
   published here was chosen by a race rather than by the person who asked for it.
3. **The blast radius is larger than a commit's.** This tag drives a **signed release build** to
   real phones. It happened to be harmless — the delta was `CLAUDE.md`, 37+/11−, documentation
   only, no app code and no build file, so the distributed APK was exactly this session's code —
   but that was luck, not a property of the procedure.

**Rejected alternatives.** *Tag before pushing* moves the race, it does not close it — the tree is
shared at every instant, not only after a push. *Fetch-and-check first* is the same
after-the-fact-gate failure the rule already names, and the sibling can commit between the check
and the tag. Only naming the SHA removes the window, because the SHA is a value you have already
read.

**Destination.** `rules/` — a clause on `my-rules.instructions.md` § *Commits & pushing*, beside
the existing *"commit explicit paths too — `git commit` commits the index, not your paths"*
bullet. It is the same failure at a third layer, and that bullet already predicts this: *"expect
it at any layer whose remedy is name your paths."* This is the layer where that remedy **does not
exist**, which is the new information.

**Anchors.** `rules/agent-topology-and-model-routing.md` §5 (parallel sessions),
`kb/dev/flows/lease.md` §4b (the same class at the staging layer),
`my-rules.instructions.md` § *Commits & pushing* precondition 2 (*"send exactly the branch you
read"* — the same instinct, one ref-type over).

**Supersedes.** Nothing. It **extends** the pathspec-commit clause rather than replacing it.

**Status.** ⛔ **BLOCKED — always-ask.** Destination is `rules/`, so it needs Ido's word and the
🎬 walkthrough gate, which is `rules/`-changes' own owner. Not drained by this session. Carried
here so the next session finds it without anyone having remembered.

---

## 2 · A prototype's later revision can overrule its own spec, and the newer one may port for free

**Claim.** Before porting a design artifact, **check whether a later artifact in the same folder
tree supersedes it** — and check what each is *made of*, because that decides the porting cost far
more than the design does. In GoalPilot, `docs/prototypes/2026-08-10-charts-presentation/` (rev 4)
specifies chart depth as five SVG-filter layers including `feSpecularLighting`.
`docs/prototypes/2026-08-11-visual-styles/`, written the next day and rebuilt on 08-12, **deletes
that filter in as many words** — *"NO `feSpecularLighting` anywhere: that filter over a fat stroke
is what inflated rev 4's ring into a balloon"* — and replaces it with a clipped directional
gradient wash.

**Why.** `Observed:` 2026-08-22. `#57` c's brief was written from rev 4 and carries its five-layer
table as the spec, plus a correct warning that Compose has no such filter and that faking it would
be the session's hard part. It would have been the hard part, and it would have been **wasted**:
the design had already thrown that layer away, and the session found this only because it opened
the *other* prototype while looking for the raised-3D geometry.

The generalisable half is about **materials, not authorship**: rev 4 is built from **SVG filters**
(`feSpecularLighting`, `feTurbulence`, `feDropShadow`), none of which Compose has, so every layer
is a judgement call about how to fake it. The 08-12 rebuild is built from **closed paths and
linear gradients** — annular sectors, wall strips, end caps, user-space three-stop fills — every
one of which Compose draws **exactly**. So the newer artifact was not merely more current, it was
**an order of magnitude cheaper to port**, and the cost difference is legible from the source
before any code is written.

`Untested:` nobody re-rendered rev 4 to confirm the balloon; the finding is documentary — two
committed files, both read.

**Destination.** `kb/dev/` — a new page, or an addition to whichever page carries the
prototype-to-Compose porting record. The reusable check is two lines: *list the prototype
directory by date before porting the one the brief names*, and *grep the newer one for the
primitives the older one relies on before pricing the work*.

**Anchors.** `CHANGELOG/2026-08-22/57c-chart-volume-and-raised.md` § *what did Compose not reach*;
`sessions/done/57c-chart-volume-and-raised.md` `result:`; `SESSIONS.md`'s `57c` release note;
`app/src/main/java/com/idomarhaim/goalpilot/ui/theme/ChartVolume.kt` header.

**Supersedes.** Nothing. It does **not** contradict `#57`'s briefs — they are correct about what
rev 4 says.

**Status.** 📥 **OWED.** Drainable under AUTO MODE and **not drained**: `/kb-ingest`'s destination
is `C:\Dev\JARVIS\kb\`, a cross-repo write that owes a claim on *that* repo's board, and this
session had already released its row and shipped its release. Reported as remaining work rather
than done. Everything a page needs is above; no transcript is required.
