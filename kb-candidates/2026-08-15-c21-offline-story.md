---
session: c21-offline-story
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
created: 2026-08-15
mode: AUTO MODE
---

# KB candidates — `c21-offline-story`, 2026-08-15

Filed while resolving [#43 · `C21`](https://github.com/idomarhaim/Android_Final_Project/issues/43)
on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12).

**Each entry stands alone.** No transcript is a source: everything needed to write the page is
below, including what was rejected and why.

**Nothing is drained by this session, and the reason is dated.** `Observed:` `C:\Dev\JARVIS` has a
live sibling — `sibling-wait-banner` committed `50c1d79` at **2026-08-15 13:58:10**, roughly three
minutes before this check, and that commit's own subject says it claimed `rules/memory-promotion.md`.
Its Active-claims table read **empty** at the same moment, which is precondition 5's *an absent row
is not proof the session is finished* in its documented form, so the board is not evidence of
release. A drain is a cross-repo write into a board that session is actively editing. Held, not
dropped.

---

## 1 · Key a disclosure to the variable that moves the fact, not the one that co-occurs with it 🟢

**Claim.** When a UI has to admit that a number might be wrong, the instinct is to key the
admission to the **connection** — an offline banner, a greyed-out card, a cloud-with-a-slash. That
is almost always the wrong variable. What makes a cross-boundary number wrong is **when its owner
last wrote it**, and that is independent of the reader's radio: a leaderboard fetched forty minutes
ago over perfect Wi-Fi is exactly as stale as one served from cache with the radio off. So a
connectivity-keyed affordance **over-fires** (shouting offline over data that is complete and
correct) and **under-fires** (silent over an hours-old number while online) — it is wrong in both
directions at once, which is the signature of a proxy variable rather than the real one.

The replacement is an **as-of stamp**: render *when the fact was last written*, unconditionally.
It is true online and offline, it needs no connectivity API, and it degrades to nothing — an
as-of that is a second old simply reads as current.

**The test, stated generally.** Name the quantity the affordance asserts. Then ask what actually
moves that quantity. If the answer is not the variable you were about to key on, you have a proxy.
Here the affordance asserts *this number may not reflect reality*; what moves that is the owner's
write time, not the reader's link.

**Why this matters, and what it rejects.** It kills three plausible designs in one stroke:
a **global offline banner** (a larger claim than the facts support — after the owner's numbers are
derived locally, almost everything under the banner is correct); **per-number "cached" styling**
(a *confidence* claim about a number that is not uncertain, merely old — the same
visibility-vs-confidence distinction the same project's calendar ticket had already drawn for
`SILENT` vs `PROVISIONAL`); and a **staleness threshold** (*"only warn if older than N"*), rejected
because the threshold is a second number nobody can source, and because an as-of caption is not
noise on a screen whose entire purpose is comparison.

**The one residue that IS connection-shaped, and why it is a different case.** A cache serves what
it has seen. A collection **never fetched on this device** returns an *empty* result rather than an
error, so the app renders *"you have no friends"* — an assertion about someone else's data it has
never read. That is not staleness, it is **not knowing**, and it needs its own empty state
(*"Not loaded yet"*). The discriminator is a property of the read (`metadata.isFromCache && isEmpty`
in Firestore), still not a connectivity API. Keeping the two cases separate is what stops the
as-of rule from quietly growing an offline banner back.

**Worked case.** GoalPilot `C21`: the ownership boundary in `firestore.rules` is a grep that returns
the complete set of surfaces that can be stale (two screens), and the as-of stamp costs one
`updatedAt` field on a write a prior ticket (`C20`) had already put a server function behind — so
the honest design was also the cheapest, and the app's existing `ConnectivityMonitor` was **deleted**
rather than repurposed.

**Destination.** `kb/dev/` — most likely a **new page**, working title
*"Key the disclosure to the variable that moves the fact"*.

**Bundle check, and its width.** `ls kb/dev/` (54 pages) plus
`grep -rln -i "stale|as-of|as of|offline|freshness" kb/`. Three near-neighbours were opened and
read, not merely listed: **`blindness-not-confidence.md`** (nearest by title, but its subject is
*agent autonomy* — what the agent cannot see — not UI disclosure); **`degraded-mode-decides.md`**
(about choosing between designs by their fallback, not about admitting staleness);
**`indistinguishable-at-the-boundary.md`** (two actions collapsing to one observable — adjacent to
the §"residue" paragraph above, and the best candidate for absorbing *that* half if a new page is
refused). `Untested:` whether `one-metric-and-its-mechanism.md`, `disclosure-is-not-a-gate.md` or
`render-site-vs-query-site.md` already carry this — their titles are close enough that the ingesting
session **must open them** before creating a page. That is the width failure `c20-derived-state`
recorded on 2026-08-15 (a check that passes because it looked at the wrong page), and it is named
here rather than assumed away.

**Anchors.** [#43's resolution](https://github.com/idomarhaim/Android_Final_Project/issues/43) §§1–4 ·
[`C9b` #26](https://github.com/idomarhaim/Android_Final_Project/issues/26) (visibility, not
confidence) · [`C12` #31](https://github.com/idomarhaim/Android_Final_Project/issues/31) (the
smallest true sentence) · [`C20` #42](https://github.com/idomarhaim/Android_Final_Project/issues/42)
(the ownership-boundary grep).

**Supersedes.** Nothing. If it lands as a section on an existing page instead of a new one, that is
an extension, not a rewrite — no standing claim is contradicted.

**Status.** 🟢 `AUTO MODE`-eligible on its own merits — a new page, superseding nothing. **Held**
only on the cross-repo liveness fact recorded at the top of this file.
