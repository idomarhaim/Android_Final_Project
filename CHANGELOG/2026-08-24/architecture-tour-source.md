# 2026-08-24 — `architecture-tour-source`

## 🎯 What Ido asked for

One file to drop into a Gemini / NotebookLM notebook, which the notebook turns into
a presentation about **the system's architecture** — a guided tour of the internals
as if touring the human body, naming every organ and its job: Firebase, Firestore,
the client, the languages, every component. Audience: people who understand software
and want to understand this system in depth.

**Distinct from `presentation-source`**, which is running concurrently and writes the
**product** story (need, gap, features) into `docs/presentation/**`. This one is the
machine, and it took a folder nobody holds.

## 📦 What shipped

**`docs/architecture-tour/GoalPilot-System-Anatomy.md`** (new) — one self-contained
Markdown source, ~700 lines, in four parts:

1. **§0 — a brief for the notebook.** What deck to build, for whom, at what depth, in
   what shape (22 content slides, each *organ → mechanism → the rejected alternative*),
   what to leave out, and a one-line switch for Hebrew output.
2. **§1 — the body at a glance.** Vital statistics (all counted mechanically, not
   estimated) and an ASCII anatomical chart of the whole system.
3. **Part 2 — 17 organ slides.** Skeleton (Gradle, variants, the split App Distribution
   dependency) · genome (`domain/`) · circulation (Hilt) · nervous system (MVVM,
   `StateFlow`, `Resource<T>`, uid-reactive flows) · face (Compose, 24 components,
   seven appearance axes) · brain stem (the auth gate) · digestion (the DTO boundary)
   · long-term memory (the Firestore tree) · immunity (`firestore.rules` and
   field-level ownership via `diff().affectedKeys()`) · endocrine (the projection
   triggers) · the consulted specialist (the four callables, four providers, the
   failure contract) · the senses (Tasks, Calendar, Health Connect) · circadian rhythm
   (WorkManager, the notification-permission policy) · the reflex outside the body
   (Glance and its process boundary) · handedness (Hebrew, RTL, bidi) · anaerobic
   metabolism (offline and `Freshness`) · growth (the leveling curve).
4. **Part 3 — five system slides:** immune memory (the test pyramid and the guards
   that read the project as text), life support (CI, signing, delivery), **a single
   heartbeat traced end to end** (a tick, from the checkbox to the launcher's process),
   the five laws the codebase obeys, and an honest list of current limits.
   Plus three appendices: every deployed backend symbol, every feature package, and
   where each claim in the document can be checked.

## 🔍 Fact-checking, and the one defect it found

Every number in the document was **counted**, not estimated — `find | wc -l` over each
source tree, `ls | wc -l` per package. Every symbol named was grepped for at `HEAD`
before it went in.

That pass caught **`docs/ARCHITECTURE.md` describing an arithmetic direction that was
inverted four weeks ago.** Its *Durations* paragraph says a task with no LLM estimate
falls back to `TaskDuration.fallbackMinutes(points)` — "3 minutes per difficulty
point". `Observed:` in `domain/model/TaskEstimate.kt` at `HEAD`:

- `TaskScoring`: `points = round(minutes / 3) × difficulty` — **minutes are the input,
  points are the view**;
- `TaskDuration.minutesOf(task)` = `estimatedMinutes ?: DEFAULT_MINUTES (30)`, with the
  KDoc saying in as many words *"No longer derived from points (`#55`, §1.4)"*;
- the old backwards path survives only as `legacyMinutesFromPoints`, narrowed to a
  **migration** for pre-`#55` tasks.

The new document states the current arithmetic and carries a presenter's note saying
ARCHITECTURE.md is stale on that line. **`docs/ARCHITECTURE.md` was not edited** —
`docs-repair` holds it live on the board; an addressed note was left below the claims
table instead.

A second, smaller correction caught the same way: `TaskRepositoryImpl.setDone` is a
**`WriteBatch` of two writes** (set the fact, clear the legacy `done` field), not the
single write ARCHITECTURE.md's phrasing suggests. The load-bearing distinction is
**batch vs transaction** — a transaction needs a server round trip and cannot be
served from the offline cache; a batch can — and the document now says that, rather
than "one document".

## 🧪 Tests

**No test layer applies to this change, and here is why that is a statement rather
than a skip.** The deliverable is one new Markdown file in a `docs/` **subdirectory**.

- `DocsCurrencyTest` — the guard that reads documentation as text — enumerates docs
  with `File(repoRoot, "docs").listFiles().filter { it.extension == "md" }`, which is
  **not recursive**. A file under `docs/architecture-tour/` is never in that list, so
  no assertion in the suite reads it. `Inferred:` from that call site, not from a run.
- `app/build.gradle.kts` declares `inputs.dir(docs)` on every `Test` task, so adding
  this file **does** invalidate `:app:testDebugUnitTest`. The next run re-executes the
  suite; nothing about it can fail differently.
- **Gradle was not run.** The daemon is a claimed singleton held by `docs-repair`, and
  this change compiles nothing. Not run rather than green — said plainly.

## 🧭 Session board

- Claimed `architecture-tour-source` before the first write; owns
  `docs/architecture-tour/**` plus its own changelog. No path overlaps any live row.
- ⚠️ **This session's claim row was published by somebody else.** It was written into
  `SESSIONS.md`'s working tree at 18:31 and committed by `presentation-source` in
  `33679f0` a few minutes later, under their message. Nothing was lost; the record is
  simply wrong about who wrote that line, which is why it is written down here. It is
  the documented shared-singleton hazard — a pathspec commit takes the working-tree
  content of the paths it names, and `SESSIONS.md` is in everyone's pathspec.
- This commit carries **`exam-qa-pack`'s claim row**, which was uncommitted in the
  working tree when this commit was staged. It is theirs, not this session's work.

## 🛠️ Tooling note

A `cat > file <<'EOF'` heredoc carrying the full ~500-line document died with
`unexpected EOF while looking for matching ''` at line 115, while an identical
heredoc with a short payload (quotes, backticks, braces) worked. Not root-caused;
the file was written with the dedicated write tool instead. Recorded because the
error message names a quoting problem that is not there, which is a minute wasted
by the next person who hits it.
