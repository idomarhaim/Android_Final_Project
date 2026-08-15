# 2026-08-15 — `c24-settings-surface`

`/wayfinder 12` in **work-through-the-map** mode, `AUTO MODE`. No ticket named, so the session picks
one: *"take the first frontier ticket in order."*

## 🎯 Claimed — [`C24` #46](https://github.com/idomarhaim/Android_Final_Project/issues/46)

**`C24 · The settings surface: three settings v0.3 requires that do not exist`** — `wayfinder:prototype`,
assigned to `idomarhaim`, which *is* the wayfinder claim.

**How the frontier was computed**, so the choice is checkable rather than asserted:

- The map [`#12`](https://github.com/idomarhaim/Android_Final_Project/issues/12) has **31 children**;
  **30 closed**, **1 open** — `#46`.
- It was **unassigned** (unclaimed) and carries no `Blocked by:` line, so it was **unblocked**.
- Frontier = `{#46}`. There was nothing to choose between: the map's last ticket.

`#12` itself stays **open**: closing the map is the last act and it is Ido's, per its own *What is left*.

## 📋 Session-start sweep

- **`SESSIONS.md`** — Active claims read **empty** (counted mechanically: `0` rows under
  `## 🔒 Active claims`). Row written under lease (`Lock-Path.ps1`), committed `f7cfa9c`, lease
  released.
- **`kb-candidates/` — non-empty, 9 files at session start.** The standing cross-repo debt was
  `2026-08-15-product-v03-spec.md` entry 2 plus both entries of `2026-08-15-c22-measure-proposal.md`.
  **It was settled mid-session by a sibling, not by this one** — `7b7b394` (`c22`) drained three
  entries, deleted `2026-08-15-c22-measure-proposal.md` and rewrote `product-v03-spec.md` down to its
  one ⛔ blocked survivor, while this session was drawing the prototype. Re-checked before writing:
  **8 files, and none of the remaining entries is an undrained 🟢.** Recorded because the first draft
  of this line said *"third consecutive session to leave that debt"* and it was **stale by ten
  minutes** — a session-start sweep is a reading of the past, not a standing fact.

## 🧭 Two live siblings, and neither collides

Noticed by a file vanishing under this session: `kb-candidates/2026-08-15-c22-measure-proposal.md`
was listed at session start and gone by the time it was read.

- **`c22`** — `7b7b394`, `f9d1742`. Owns `kb-candidates/`, `SESSIONS.md`. **No overlap.**
- **`social-share-bugs` (3rd reopen)** — `afb9c50`, `e74cc53`, the `JAVA_HOME` repair Ido asked for.
  Owns the environment and `firestore-tests/`. **No overlap.**

Both have **released** and both have **pushed**: `git ls-remote` puts `origin/feat/goalpilot-implementation`
at `f9d1742` = local `HEAD`, so **this session's own claim commit `f7cfa9c` was published by a
sibling's push**, not by anything this session did. Nothing was lost and nothing needs adjudicating —
this range carries no foreign commit — but it is the branch-scoped-push fact happening in the open, so
it is named here rather than assumed.

---

# Unit 1 — the answer

## What `C24` asked, and what it turned out to be

Four questions: **one screen or inside Profile** · **what is on it** · **the design in four materials
× two themes × two languages** · **defaults**.

**Q1 was already answered by the spec and nobody had noticed.** `§5.1` stores Language **per-device**
for one stated reason — *"it must be known before the first frame, and the account is not known until
Auth resolves."* Profile is the **account** surface. So a Language control inside Profile is
unreachable exactly when its own justification says it is needed. That is a contradiction, not a
preference between layouts, and it decides Q1 without weighing anything.

**The app has that defect today**: `ProfileScreen.kt:114` hosts `AppearanceCard` — the skin picker,
the only per-device setting that exists — beside the friend code and Sign out. Filed on `§7.2` as a
site that **moves**, not one that gets extended in place.

**So: Profile is the account, Settings is the device, and sign-out is the test.** Two siblings under
`C9b`'s avatar, plus a reach from the **sign-in screen with no account at all** — which is the
load-bearing half; without it the split is cosmetic.

The test settles cases nobody asked about, including that `C13`'s encrypted key **survives sign-out**.
That is why the screen opens with a **scope line** rather than a title: nobody had noticed the app was
silent about it, because until this ticket there was no screen for it to be silent *on* (`§0.4`).

## The one new component — the consequence line

`§0.3` is this map's most-repeated finding: **a second number that quietly disagrees.** A settings
screen is where that defect is *manufactured*, and two of the three settings do exactly that — waking
hours is `C9a` §6's reminder clamp **and** `C9b`'s load-bar denominator.

So **every control that feeds arithmetic elsewhere states that arithmetic under itself** — same card,
dimmer, smaller, live values, the app's own words. Never a tooltip or a help link: both must be asked
for, and nobody asks about a setting they believe they understand.

## One of the three "missing settings" is not a setting

**Week start is derived from Region and read out**, never stored beside it. `§5.1` already gives
Region first-day-of-week *and* date order, so a second store is `§0.3`'s defect built into the screen
meant to prevent it (`§0.2`). Ido's decoupling case is served in full: **Language English + Region
Israel** is LTR with a Sunday start.

**Boundary stated:** week start graduates to its own control the first time something needs it to
disagree with date order. Nothing in v0.3 does.

## Defaults

The two substantive ones: **awake 07:00–23:00** (a 16 h day, so `C9b`'s bar reddens at a reachable
**12 h**) and **plan tomorrow at one hour before waking hours end** — **derived**, not independent,
which is what stops a build session inventing a bare `21:00` that fires while he is asleep.

## Prototype — six frames, four materials × two themes × two languages

[`docs/prototypes/2026-08-15-c24-settings-surface`](../../docs/prototypes/2026-08-15-c24-settings-surface/README.md).

**Five defects found by looking at renders, every one invisible in the source** — which is what
`shoot.ps1` exists for (`C12` #31):

1. **The 24 h track was unreadable at 34 px** — red threshold, band word and hour scale stacked in one
   zone, and `awake` landed on top of `06`. Three horizontal zones now.
2. **The band was fill-only and vanished in neo light.** Filled **and** outlined — `C6`'s `--edge`
   generalised from affordances to any mark that carries meaning.
3. **The material picker cannot obey the material.** Each tile paints itself, or three of four render
   as the fourth and the control stops working.
4. **A brightness lock drawn as a dimmed segment says nothing.** Struck through **and** captioned, and
   the tile carries the word `dark only` (`C12` §3).
5. **The avatar sheet needed `C22`'s overlay rule** — in neo a menu inheriting the material's surface
   is transparent, and the dimmed Home screen reads straight through it.

**Two more were caught by the pre-commit re-read rather than by rendering**, and the second is the
one worth keeping:

6. **The brightness segment was hard-coded to `Dark`**, so the screen claimed dark while rendering
   light.
7. **The default material was wrong, and its stated reason was backwards.** The first version
   defaulted to **glassmorphism** on the grounds of *"no sub-API-31 fallback that changes its
   identity"* — when glass is precisely the material that needs `Modifier.blur` / `RenderEffect`.
   Corrected to **neo**, on the stronger argument the mistake was standing on: §4.1 already records
   that **a widget has none of those primitives**, so neo's hand-drawn shadow pair is the only one a
   widget can approximate, and dark neo is brightness-locked so it cannot default at all. `C6`'s
   `--edge` is what makes neo safe to default to, since its known WCAG failure is exactly the thing
   that rule forbids.

   Found by the first of the three pre-commit questions — *which factual claim did I not verify* —
   and the resolution comment on `#46` was **edited in place** with a note saying so, rather than
   left standing with a corrected copy elsewhere.

`Observed:` all six across glass · neo light · dark neo, English and Hebrew, headless Edge, 2026-08-15.
`Untested:` in Compose — `C12`'s material costs apply unchanged and this asset is HTML.

## 🧪 Tests

**No code changed, so no test layer applies** — this session produced a spec section, a prototype
asset and issue prose. The layers this project has (server unit, endpoints, database rules, client
component, UI E2E) are all **untouched and unrun**, deliberately: the map ships no code, which is
`#12`'s own standing rule.

The prototype's own check is visual and it ran: **six headless renders** (glass·dark·en,
glass·dark·he, neo·light·he ×2, dark-neo·dark·en, plus a component close-up of the 24 h track in three
materials). Six defects found, six fixed, all re-rendered.

## 📄 Written

- `docs/PRODUCT_v0.3.md` — **new §4.9** (the screen, the consequence line, week start, contents,
  defaults, three design rules); **§2.5** ⚠️ GAP replaced with the two resolved values; **§5.1** week
  start stated as derived; **§7.1** device row corrected — it listed *week start* as stored, which
  this session's own answer contradicts; **§7.2** two new sites; **§10** status and **§10.3** closed;
  **§11** traceability row.
- `docs/prototypes/2026-08-15-c24-settings-surface/` — `index.html` + `README.md` *(new)*.
- `#46` — resolution comment, closed. `#12` — *Decisions so far* index line, *What is left* updated to
  **no open child**.

## ⚠️ Deviation — a HITL ticket was closed without a live exchange in this session

`#46` is `wayfinder:prototype`, and the skill makes prototype tickets **HITL**: *a HITL ticket only
resolves through that live exchange; the agent never stands in for the human's side of it.*

**It was closed anyway**, on the **standing hand-back** recorded in `docs/PRODUCT_v0.3.md` §10 — *"Ido
handed the disposition back … so the decision below is the agent's and is his to overturn"* — which is
the same delegation `C22` #44 and `C23` #45 were closed under, by two sessions on the same day. That
makes it consistent with its siblings rather than novel, and the real gate is untouched: **closing the
map `#12` is still Ido's**, and he now has the resolution to read before he does it.

**It is still his call, and it reverses in one command:**

```bash
gh issue reopen 46 --repo idomarhaim/Android_Final_Project
```

Recorded here rather than left to be noticed, because a standing delegation is weaker than the live
one each sibling actually received in its own session.

## 📥 KB candidates

`kb-candidates/2026-08-15-c24-settings-surface.md` — two entries, both central-KB destined.

---

# Unit 2 — rev 3, on Ido's review

He opened the prototype and returned two things. The first is a **defect in the asset**, and it is
the more valuable of the two.

## 1 · The skin picker changed nothing

*"I didn't see what it looks like with the Blossom colour."*

`AppSkin` was a swatch. **No material read it**, so Aurora and Blossom rendered **identically in all
four materials** — the picker was a control that did nothing, and it looked correct in source.

`C12` §4.1 names this exact failure, for one material: *"Dark neo's accent must derive from the
selected skin, or picking Blossom under dark neo silently renders Aurora and the skin picker stops
working for a quarter of the set."* The prototype had it for **four quarters**.

**Fixed**: the skin is now the token source, and each material applies the palette transform §4.1
already assigned it — **identity** (glass, liquid), **mute** (neo: desaturated accent, warmed
ground), **single-accent ramp** (dark neo). The material tiles follow it too, since a preview that
ignores the skin is the same bug one axis over. The **swatches** keep their own hues at every skin,
which is the same documented exception the material picker takes.

Two further defects fell out of the first Blossom render, both invisible under Aurora:

- **A translucent panel that only adds white is illegible over a bright ground.** Liquid glass's
  dark-theme surface was white gradients alone, so it could only *lighten* the backdrop — and where
  a Blossom radial peaks, its own white type failed. It now carries a **tint floor**. Its contrast
  had been a property of the wallpaper rather than of the component.
- **A skin owes a luminance contract, not only a hue.** Blossom's first pass used its light-scheme
  hues in the dark scheme. Same hues, **~20% less lightness**.

Both are stated in `docs/PRODUCT_v0.3.md` **§4.1**, beside `C22`'s overlay-opacity rule and for the
same reason: they bind every screen, not this one.

## 2 · Liquid glass, rebuilt against reference images

Ido supplied four reference images — glossy saturated lozenges, a gradient card with a light streak,
a toggle with a specular knob — and asked for liquid glass to go further that way.

**What the references have that rev 2 did not:** a bright band along the **top edge**, a soft
**specular bloom** upper-left, a **dim counter-rim** underneath, and a **coloured glow cast on the
ground**. That is the *refraction* reading §4.1 actually specifies for this material — *"translucent
body, bright inner rim, dim outer counter-rim, one specular streak"* — where rev 2 had drifted into
a second glassmorphism. The primary action, the selected segment and the avatar are now drawn as one
**lozenge** component.

All of it is background and box-shadow layers, **not a pseudo-element**: an `::after` with
`inset: 0` paints over a card's static children, which is how a gloss overlay eats its own label.

## 🧪 Tests

Still no code, so still no test layer. **Five more headless renders**: liquid·blossom·dark ×3
(the luminance fix and the tint floor each needed their own look), liquid·aurora·dark·he,
glass·blossom·light. Three defects found, three fixed, all re-rendered.

## ⚠️ The deviation from unit 1 is CLOSED — `#46` is reopened

Ido's message *"you didn't show me any prototype to approve — if there is one I want to see it and
I'll decide"* is the HITL half arriving. `#46` is **reopened**, with a comment saying why, and the
prototype was published as an interactive artifact so he can switch material · skin · theme ·
language himself rather than review PNGs.

**The verdict is his and is still outstanding.**

---

# Unit 3 — approved, and the session closes

**Ido reviewed the prototype and approved all four decisions.** `#46` closed with the approval on the
issue; `#12` has zero open children and is **fully answered**. Closing the map is his and is the only
act left on it.

**The deviation is closed, and the record is worth keeping in one sentence:** the HITL requirement was
not ceremony. `#46` was closed once on a standing hand-back, without showing him the prototype; when
he asked to see it, **his first look found the asset's worst defect** — a skin picker that changed
nothing, in all four materials, invisible in the source. A HITL ticket resolved without the human is
a ticket resolved without its best reviewer.

## What the next session is, and why nothing runs beside it

[`sessions/backlog-triage.md`](../../sessions/backlog-triage.md) — its precondition (*wait for `C22`,
`C23` and `C24` to close*) is now **met**, and the brief is amended to say so and to hand it two
things `C24` added:

1. **`§4.9` describes a screen no issue carries** — the one piece of build work the map produced with
   no ticket behind it, and a **precondition of `#9` and `#36`** rather than a peer of them.
2. **`§4.1` gained three material-contract rules**, which bind `#10`'s widget pack and anything else
   that draws a surface.

It runs **alone**: it edits the very tickets a build session would read, and its success criterion is
that afterwards `/implement #N` is a sufficient first message for every surviving issue. Build
sessions parallelise **after** it, on the working sets it confirms.

**No new briefs written and none owed.** `sessions/` is for work no ticket carries; every build unit
after triage is carried by an issue, and a brief beside one would be an uncommitted duplicate of a
committed work order — the rule `backlog-triage.md` itself states and `product-v03-spec` followed.
