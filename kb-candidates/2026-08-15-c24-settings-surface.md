# KB candidates — `c24-settings-surface`, 2026-08-15

Session: `/wayfinder 12`, work-through-the-map, `AUTO MODE`. Resolved
[`C24` #46](https://github.com/idomarhaim/Android_Final_Project/issues/46).

Each entry stands alone — no transcript is a source.

---

## 1 · A settings screen is where "a second number that quietly disagrees" gets manufactured

**Claim.** When a control changes a quantity that some *other* screen computes with, the control must
state that consequence **at the point of setting**, with live values, in the product's own words —
inside the same card, never a tooltip and never a help link. Call it a **consequence line**.

**Why.** The generic advice — *settings should be self-explanatory* — is about the setting's own
meaning and misses the actual failure, which is **coupling the user cannot see**. GoalPilot's map had
already named the underlying defect class three times over as *a second number that quietly
disagrees*: two quantities that must agree, with nothing making them. A settings screen does not merely
risk that defect, it is where the defect is **created** — one control, silently moving a number on a
screen nobody is looking at. Concretely: *waking hours* was simultaneously a reminder clamp and the
denominator of a per-day load bar that reddens at 75%, specified on two different tickets, neither of
which owned a surface.

**Rejected on the way:** (a) a tooltip or an info icon — both must be *asked for*, and nobody asks
about a setting they believe they understand; (b) documenting the coupling in the spec only, which
leaves the user with the same invisible edge; (c) hiding derived settings, which trades an invisible
coupling for an invisible control.

**The corollary, and it is the sharper half:** run the same test on every candidate control and some
of them **stop being settings**. *Week start* was filed as one of three missing settings and turned
out to be a **read-out of Region**, which already owned first-day-of-week and date order — storing it
beside Region would have built the very defect the screen exists to prevent. A settings screen is a
good place to discover that a setting is derived, because it is the one place all of them are listed
side by side.

**Destination.** `kb/dev/` — a new page, e.g. `settings-surface-design.md`. Central KB (`C:\Dev\JARVIS\kb\`),
so draining it is a **cross-repo visit** owing a row on that board.

**Anchors.** `docs/PRODUCT_v0.3.md` §4.9 · §0.2 · §0.3 ·
[`#46` resolution](https://github.com/idomarhaim/Android_Final_Project/issues/46) ·
`docs/prototypes/2026-08-15-c24-settings-surface/README.md`.

**Supersedes.** Nothing.

**Status.** 🟢 Ordinary `kb/dev/` page, **`AUTO MODE`-eligible**, **not drained here** — it is
central-KB destined, which makes the drain a cross-repo `C:\Dev\JARVIS` visit owing its own board row,
and the same debt was left by three sessions running before a sibling settled it today. Flagged rather
than half-done.

---

## 2 · "Where does this control live?" is often already decided by *why* the value is stored where it is

**Claim.** Before adjudicating which screen hosts a control, read the **stated reason** its value is
stored where it is. Storage justifications routinely contain a **reachability constraint** that
decides placement outright — and when they do, the placement question was never a design fork at all.

**Why.** The instance: a spec stored Language **per-device** and wrote down the reason — *it must be
known before the first frame, and the account is not known until Auth resolves.* The open question was
whether settings live inside the account/Profile screen or beside it. The stored reason answers it: a
control inside an **account** surface is unreachable exactly when its own justification says it is
needed. Not a preference between two layouts — a contradiction, which is a different kind of finding
and needs no taste at all.

The general shape is worth keeping because the reason is usually written down **once, far from the
screen that will host the control**, and it reads as a storage note rather than a UI constraint — so
nobody re-reads it when the placement question comes up later. The tell that it applies: a value whose
storage note contains the words *before*, *not yet*, *not known*, or names a lifecycle stage.

**The derived test, which is the reusable part:** for an account-bearing app, **sign-out is the
discriminator** — whatever survives sign-out is a *device* setting, whatever leaves with the account is
*profile*. It also settles the cases nobody asked about; here it surfaced that an encrypted
third-party API key **outlives sign-out**, which nobody had disclosed because there had never been a
screen for it to be disclosed on.

**And the corroborating evidence is usually already in the code**, as the defect: the app's only
per-device setting sat on the account screen (`ProfileScreen.kt:114`), beside the friend code and Sign
out.

**Destination.** `kb/dev/` — same page as entry 1, or its own; the ingest decides.

**Anchors.** `docs/PRODUCT_v0.3.md` §4.9 · §5.1 ·
`app/src/main/java/com/idomarhaim/goalpilot/feature/profile/ProfileScreen.kt:114` ·
[`#46` resolution](https://github.com/idomarhaim/Android_Final_Project/issues/46) §1.

**Supersedes.** Nothing.

**Status.** 🟢 Ordinary `kb/dev/` page, **`AUTO MODE`-eligible**, **not drained here** — held with
entry 1 for the same cross-repo reason.
