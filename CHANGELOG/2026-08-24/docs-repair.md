# docs-repair — 2026-08-24

All six files under `docs/` **and** `README.md` brought up to the system as it actually is.
Follows `docs-currency-guard`, which audited them and repaired only the subset its guard asserts on.

## 🗳️ Ido's three answers, and what each authorised

| Question | Answer | Consequence |
|---|---|---|
| Scope | **Not one of the options.** He named the *set* instead: every file in the screenshot plus `README.md` | Seven files, not the three I proposed |
| Test counts in prose | **Delete the numbers** | A deletion, so his to give — taken |
| OPERATIONS §3 backlog | **Delegated** — *"pick the best solution for the system, and if you have a better one than you proposed, do that"* | Decided by deriving; recorded below as **mine** |

The scope answer is the *enumeration* tell: the question quantified over a set **I** had
enumerated (three depths of repair), and the answer added a member. Per the rule that is not a
re-ask — the named set is the answer, and the depth it left open is derived.

## 🧭 The delegated decision — §3, and it is MINE to be overturned

**None of the three options I offered was right, which is what a delegation is for.**

*Rewrite it against `HEAD`* keeps a second backlog beside GitHub issues, and that is what rotted:
by 2026-08-24 every item in §3 was false — Health Connect called a stub after it shipped,
challenges a preview screen after `ChallengeRepositoryImpl` landed, and a ⛔ box declaring the
whole §1.4/§1.5 points model absent from `HEAD` when `Difficulty`, `completionFacts` and
`goalEdges` were **all** in the code (verified clause by clause before deleting it). *Delete it and
point at the tracker* throws away two things the tracker cannot hold.

**What I did instead: split the section by what rots.** The live list of open work moves to the
issue tracker, which is written to when work happens rather than when somebody remembers. What
stays is only what a tracker structurally cannot carry:

1. **A judgement** — *what blocks submission* (the spec title page, and only that).
2. **A procedure** — the two-account §7 demo, which is instructions, not a backlog item, and does
   not change when a ticket closes.

The section now opens by saying it deliberately does not list open work, and why.

## 🛠️ Per file

- **ARCHITECTURE.md** — Overview and layer diagram rewritten (it still said *"**GROQ** for
  recommendations"* and *"Health/Tasks stubs"*); `ui/` corrected from one bullet to its seven
  sub-packages; `notifications/` added as its own layer; the eleven feature packages at `HEAD`
  named. Theming corrected from *one skin enum* to the **seven appearance axes** that exist
  (`AppSkin`, `AppBrightness`, `AppBackground`, `AppMaterial`, `AppRelief`, `AppLanguage`,
  `AppRegion`). **Six new sections**: scheduling/occurrences/calendar, Settings, bring-your-own AI
  key, the home-screen widget, the guided tour, Hebrew/RTL. Data model corrected — tasks carry
  `goalEdges` with `goalId` as an indexed projection, not a single id. Two more false limitations
  struck: goal deletion **does** cascade now (`deleteGoal` batch-deletes `progress` and *unlinks*
  tasks, since a task may hang off several goals).
- **OPERATIONS.md** — §1 status table rewritten against `HEAD` (four callables **and** three
  triggers, challenges shipped, Health Connect shipped, the four test layers named); §3 as above.
- **SETUP.md** — a new section saying the AI is multi-provider and that the steps in this file
  configure the *default* route only; a user's own key needs no setup here.
- **README.md** — the AI bullet, the "two colour skins" bullet (now seven axes incl. Hebrew/RTL),
  and the source-tree map (`notifications/`, the real `ui/` and `feature/` lists).
- **CLOUD-DEVICE.md** — "the 15 instrumented tests" → "the instrumented suite". Per Ido: delete
  the number, do not update it.
- **PRODUCT_v0.3.md** — status box only. It said three tickets and `#12` remained open; all four
  are closed. The paragraph is **kept** and stamped rather than rewritten, because it records *why*
  those three were filed rather than waved through — only its status was wrong.

## 🧪 Tests

- ⚠️ **`unverified` by the real consumer.** `s25-layout-and-tour` holds the **Gradle daemon** and
  is live with 16 dirty files under `app/src/main/`, so `:app:testDebugUnitTest` was **not run**.
- **Verified by proxy instead:** a script replicating `DocsCurrencyTest`'s four assertions outside
  Gradle — callables **4/4**, collections **14/14**, bottom-bar tabs **4/4**, JDK paths **1/1**
  matching the pin. **Would be GREEN.** A proxy is not the consumer; the suite runs when the daemon
  frees.
- Nothing here touches app code, rules or the functions runtime — the diff is prose only.

## 😅 The escape trap, third and fourth instances in one session

Writing OPERATIONS §3's PowerShell block, `.\scripts\run-goalpilot.ps1` inside a `python - <<'PY'`
heredoc lost a backslash level and Python read **`\r`** as a carriage return: the file shipped
`.\scripts` + a bare **CR** + `un-goalpilot.ps1`, which renders as a plausible-looking line.

**And the checker written earlier this session to catch exactly this missed it.** It scanned for
control characters `c not in '\r\n\t'` — excluding `\r` wholesale because the file is CRLF — so a
**stray** CR passed. Fixed by scanning for `CR not followed by LF`, which is the right width; all
seven files now report **0 stray CR, 0 other control characters**.

That is `look-at-your-own-output.md` §4k a second time, in the instrument built in response to
§4k the first time. Same session, same shape, and the reason it was caught at all is that the
rendered line was read rather than the check believed.
