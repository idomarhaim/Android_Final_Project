# KB candidates — `c9c-calendar-sync` (2026-08-10)

Session: `c9c-calendar-sync` · `/wayfinder 12` → resolved
[#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) ·
branch `feat/goalpilot-implementation` · mode: `AUTO MODE` from Ido's second message.

Each entry stands alone. No entry may be reconstructed from this session's transcript.

---

## 1 · An OAuth scope is not a permission model — and incremental authorization is how you keep the promise you cannot enforce

**Claim.** OAuth scopes are granted **per scope, never per resource**. So a product
control of the form *"share these two calendars / mailboxes / repositories fully and
that one only at summary level"* **cannot be enforced by the provider** — the grant is
all-or-nothing across everything the scope reaches, and the per-resource split is a
promise the client keeps. Three consequences, and the third is the one that turns the
constraint into a design:

1. **Say so.** A privacy control the user believes the provider enforces, and which the
   client actually enforces, is worse than no control — the user calibrates their trust
   against the wrong party.
2. **Request incrementally.** Hold at sign-in only the scopes the app is using *now*;
   ask for the broad one **at the moment the feature that needs it is first used, with
   the specific resource named in the sentence.** If the user never uses that feature,
   the promise is never made and the restraint is provider-enforced rather than
   self-enforced. It also replaces one checkbox among five with a consent screen that
   has a legible reason.
3. **Make the restraint visible in *which call is made*, not as a filter after the
   fetch.** Read a summary-level resource through the summary endpoint (e.g. a
   free/busy query) and a full-access one through the full endpoint. Then the promise is
   observable in the code and in a network trace, instead of being an intention applied
   to data already in memory.

**Why (and what was rejected).** Rejected: buy the broad read scope at sign-in and
filter in the UI — simpler, one consent screen, and it makes the app hold a grant it may
never use while presenting a control it cannot back. Also rejected: refuse the
per-resource control and offer only one global level — which is what the question
originally proposed, and it fails because *"the user's calendars"* is not one object: a
shared family calendar and an employer's calendar are different in kind, so a single
level must be set for the most sensitive one and wastes the rest. The deciding argument
for incremental was **the never-used case**: it is the only variant where a user who
declines the feature gets a guarantee rather than a promise.

**Corroborating detail worth keeping.** Granular consent means each checkbox arrives
**unchecked**, so partial grants are the *normal* case once an app requests more than
one scope — which makes "every feature degrades legibly and none gates the app" a
requirement rather than a nicety, and makes incremental requests *easier* to handle than
a single wide one, because a refusal is legible at the point it happens.

**Destination.** Central KB — **fold into `dev/google-oauth-scopes-and-consent.md`** as
a new section. That page already carries *"scopes are researchable and consent is not"*,
the granular-consent-arrives-unchecked finding, and the grant-lives-on-the-account
finding. This is the same page's concern (what a scope does and does not buy), so a new
page would split one topic.

**Anchors.** `docs/research/2026-08-08-google-calendar-scopes-and-consent.md` §2 (the
full scope table, showing every Calendar scope is account-wide except
`calendar.app.created`); `app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt`
(the shipped `NeedsConsent`-carrying-Google's-Intent pattern that makes incremental
authorization the *existing* idiom rather than a new one);
[#27 resolution](https://github.com/idomarhaim/Android_Final_Project/issues/27) §2.

**Supersedes.** Nothing. Additive to `dev/google-oauth-scopes-and-consent.md` —
checked: that page makes no claim about per-resource granularity or incremental
authorization, so there is nothing to contradict.

**Status.** Ready to ingest.

---

## 2 · Two different events, one observable: preserve the superset and ask a human, never guess

**Claim.** When an integration can only see a subset of the world, two *different*
real-world actions collapse into one identical signal at the boundary — and the
automatic responses to each are destructive in **opposite** directions. The rule:
**neither destroy nor re-assert; take the action that loses nothing, and defer the
disambiguation to the next moment a human is present.**

The worked case: an app holding a scope over *only the calendar it created* sees an
event **moved out** of that calendar and an event **deleted** as the same thing — the
event is simply gone (`status: cancelled`). Guess "deleted" and a harmless-looking drag
silently destroys the user's record. Guess "still ours" and re-create it, and the app
fights the user, restoring what they just removed. The resolution keeps the local record
and drops only the *link* (clear the external id), then raises it in a batch the user
already reads.

**Why (and what was rejected).** Both auto-behaviours were rejected on the same test —
*what does the wrong guess cost, and is it recoverable?* Deleting is unrecoverable and
also erases exactly the history the product's later features need. Re-creating is
recoverable but adversarial, and adversarial-but-recoverable still trains the user to
distrust the sync. Rejected too: buying a broader scope so the app can tell the cases
apart — it works, but it makes correctness depend on holding a grant the user may
decline, so the ambiguity would return the moment they did.

**The generalisation, which is where the value is.** This is not a calendar fact. It
applies wherever the visible surface is a *subset* of the real system: a file moved out
of a watched folder vs deleted; a record leaving a filtered query vs being destroyed; a
process leaving a namespace vs exiting. **The test for the pattern: can a user action
that is cheap, common and non-destructive produce the same observable as a destructive
one?** If yes, no default is safe and the deferral is the design.

**Why the deferral is affordable here rather than a cop-out.** It costs nothing extra
only because the product already had a batch surface a human reads regularly. The
generalisation is therefore paired: *if you have such a surface, ambiguity is cheap to
resolve; if you do not, the cost of this pattern is building one* — which is still
cheaper than either wrong guess.

**Destination.** Central KB — **new page**, `dev/indistinguishable-at-the-boundary.md`.

⚠️ **Check two neighbours before creating it** — both were written from this same map on
2026-08-10 and one is genuinely close: `dev/blindness-not-confidence.md` (what an agent
may do silently is decided by what the app cannot *see*, not by model confidence) and
`dev/confirm-the-plan-not-the-item.md`. The distinction I believe holds, stated so the
ingest can test it rather than take it on trust: `blindness-not-confidence` is about the
**absence** of a signal, and answers *"may I act?"*; this entry is about the
**ambiguity** of a signal that is present, and answers *"what does this mean?"* If the
ingest judges them one concern, fold here rather than creating a fourth page.

**Anchors.** [#27 resolution](https://github.com/idomarhaim/Android_Final_Project/issues/27)
§1, *"The real hard part: a move-out is indistinguishable from a delete"*;
`docs/research/2026-08-08-google-calendar-scopes-and-consent.md` §2 (why the app sees
only its own calendar).

**Supersedes.** Nothing.

**Status.** Ready to ingest, subject to the neighbour check above.

---

## 3 · Fourth instance of the picker-axis failure — and this one failed in a way the first three did not

**Claim.** A question picker was offered on the axis *"how much of your calendar data may
the app see"*, cut three ways (nothing / summary / everything). The user answered on a
different axis entirely: **per calendar** — some shared fully, some at summary level.
The offered axis treated *"my calendar data"* as **one object with a sensitivity dial**;
the real discriminator was **per instance**, because the collection is heterogeneous —
a family calendar and an employer's calendar are different in kind, and any global level
is set for the most sensitive member and wastes every other one.

**Why this is a new failure mode, not a repeat.** Three are already on record from this
map: **framing** (`c16`: none of the three options was right), **coverage** (`c10`), and
**ownership** (`c13`: the question was never the user's to answer). This is a fourth —
call it **granularity**: the axis was correct *as a dimension* and wrong *as a unit*.
The tell is diagnostic and worth having, because it looks like success: the user answers
fluently and confidently, and the answer is simply **not on the menu**. That is the
opposite of the ownership failure's tell (*"I could not understand you"*), so a rule that
watches only for confusion will not catch it.

**The cheap check it implies.** Before offering a global setting over a *collection*,
ask whether the collection's members are homogeneous. If any member could reasonably
want a different setting from another, the axis is per-instance and the global cut is
already wrong.

**Destination.** `rules/` — it belongs in the ❓ Ambiguity rule's picker guidance, beside
the axis-naming bullet, not in a KB page.

**Anchors.** `C:\Dev\JARVIS\rules\question-axis-naming.md` (drafted, uncommitted,
awaiting `/walkthrough`); the three prior instances are recorded in `SESSIONS.md` →
*Recently released* under `c10-quote-feed`, `c13-byo-api-key` and `c16-milestone-model`.

**Supersedes.** Nothing, but it **extends a parked candidate** — the always-ask entry in
`kb-candidates/2026-08-10-c16-milestone-model.md` and the drafted rule above.

**Status.** ⛔ **Always-ask in both modes — not ingestible by `/kb-ingest`.** Destination
is `rules/`, which is a change to how the agent behaves and is the 🎬 walkthrough rule's
to move, not `AUTO MODE`'s. Waits on Ido.
