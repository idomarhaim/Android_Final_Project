# `C24` · The settings surface — prototype

Asset for [`C24` #46](https://github.com/idomarhaim/Android_Final_Project/issues/46) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — the map's **last open
child**. Open `index.html`; material, theme and language switch in the bar or by query string so
`shoot.ps1` can render any state:

```powershell
cd docs\prototypes\tools
./shoot.ps1 -Page ..\2026-08-15-c24-settings-surface\index.html -Out out.png `
            -Query "m=neo&t=light&l=he" -Width 2560 -Height 1220
```

`m=glass|liquid|neo|darkneo` · `t=dark|light` · `l=en|he`.

The page **opens on glassmorphism** because it is the most fragile surface to draw a dense control
screen on, not because it is the product default — that is **neo**, and the reason is in *Defaults*
below.

## The answer, in three lines

> **1. Profile is the account, Settings is the device, and sign-out is the test.** Two siblings under
> one avatar, never one screen inside the other.
>
> **2. Every control that feeds arithmetic elsewhere shows that arithmetic under itself** — the
> **consequence line**. It is the screen's only new component and it is the whole design.
>
> **3. One of the three "missing settings" is not a setting.** Week start is **derived from Region**
> and read out; storing it beside Region would manufacture the exact defect `§0.3` names.

## Why the Profile/Settings split is forced rather than chosen

`C24` asks it as an open question — *"Is it one screen or does it live inside Profile?"* — and the
spec has already answered it without noticing.

`§5.1` stores Language **per-device** for one stated reason: *"it must be known before the first
frame, and the account is not known until Auth resolves."* Profile is the **account** surface. So a
Language control that lives inside Profile is unreachable exactly when its own justification says it
is needed. That is not a preference between two layouts; it is a contradiction, and it decides Q1.

**Today the app has the defect.** `ProfileScreen.kt:114` hosts `AppearanceCard` — the skin picker,
the only per-device setting that exists — beside the friend code and Sign out. The screen this
prototype draws is where it moves to.

**Sign-out is the test, and it is worth stating because it also decides the cases nobody asked
about.** Whatever survives sign-out is a setting; whatever leaves with the account is profile.

| Survives sign-out → **Settings** | Leaves with the account → **Profile** |
|---|---|
| skin · material · brightness | friend code |
| language · region | level, points, streak |
| waking hours · planning hour | Google Calendar consent (`C9d`, `C9f`) |
| the encrypted API key (`C13`) | Sign out itself |

The last row of the left column is the reason the screen opens with a **scope line** rather than a
title alone: an encrypted third-party secret that outlives sign-out is exactly the kind of thing
`§0.4` forbids the app to be silent about.

## The consequence line

The map's most-repeated finding, `§0.3`, is **a second number that quietly disagrees**. A settings
screen is where that defect is *manufactured*: a control here silently changes a number on a screen
the user is not looking at. Two of `C24`'s three settings do precisely this — waking hours is
`C9a` §6's reminder clamp **and** `C9b`'s load-bar denominator.

So every control that feeds arithmetic elsewhere carries a **consequence line**: dimmer, smaller,
inside the same card, with **live values**, in the app's own words. Not a tooltip, not a help link —
both of which are things you have to go and ask for, and nobody asks about a setting they think they
understand.

| Control | Consequence line says |
|---|---|
| Region | which day your week starts, and how a date reads |
| Awake between | how many hours that is, and where the load bar turns red |
| Plan tomorrow at | that it follows waking hours, so moving them moves it |
| Material | that dark neo is brightness-locked (`C12` §3) |
| Your own key | which provider answered last, permanently (`C13`) |
| Your profile | that nothing on this screen belongs to the account |

## What the six frames prove

| Frame | Screen | Proves |
|---|---|---|
| **1** | Settings, upper | the material picker **paints each tile in its own material** — the one control in the app that must not obey the current one, or three of four options are invisible; and week start as a **read-out** of Region |
| **2** | Settings, lower | the two settings that change arithmetic elsewhere, with the **24 h track** drawing the awake window and `C9b`'s 75% mark on one axis, because they are the same quantity read twice |
| **3** | the avatar sheet | Q1 answered visually — `C9b`'s avatar opens a sheet carrying **two siblings**, not one screen containing the other |
| **4** | sign-in | language and region reachable with **no account at all** — the load-bearing half of the split |
| **5** | dark neo | the **brightness lock**, said in a word on the tile and a struck-through segment with a sentence, never a quiet no-op and never a hue |
| **6** | the key | one **permanent** status component in all three states, so a dead key is visible the day it dies |

## Defaults, so the screen can be ignored

`C24` §4 asks for these explicitly rather than leaving them to a build session.

| Setting | Default | From |
|---|---|---|
| Language | device language | `§5.1` |
| Region | device country | `§5.1` |
| Week start | **derived**, never stored | Region |
| Skin | `AURORA` | `AppSkin.DEFAULT`, unchanged |
| Material | **neo** | the only one with **both** a light and a dark scheme **and** no blur under it — glass and liquid glass are `Modifier.blur` / `RenderEffect`, API 31+ with a look-changing fallback below, and `§4.1` already says **a widget has none of those primitives**. Dark neo is brightness-locked, so it cannot default. `C6`'s `--edge` is what makes neo safe here: its known WCAG failure is shadow-only affordances, and that rule forbids them |
| Brightness | follow the device | except under dark neo, where it is locked and says so |
| Awake between | **07:00 – 23:00** | a 16 h day; the load bar then reddens at 12 h, which is a plausible threshold rather than one nobody would ever hit |
| Plan tomorrow at | **one hour before waking hours end** — 22:00 by default | derived, so it has a sane value on a device where waking hours were never touched, and it moves when they do |
| Provider | GROQ, free | `§3.6`'s ladder floor |

## What was found by drawing it, not by arguing it

Five things, all of which were invisible in the source and obvious in the first render — which is
what `shoot.ps1` exists for (`C12`).

1. **The 24 h track was unreadable at 34 px.** Three pieces of information — the red threshold, the
   band's own word, the hour scale — were stacked in one zone and `awake` landed on top of `06`. It
   is three horizontal zones now.
2. **The band was fill-only and vanished in neo light** — a pale accent on a pale page. It is filled
   **and** outlined, which is `C6`'s `--edge` rule generalised from affordances to any mark that
   carries meaning.
3. **The material picker cannot obey the material.** Drawn against the contract like everything else,
   three of the four tiles render as the fourth and the picker stops working. Each tile paints
   itself; it is the documented exception.
4. **A brightness lock drawn as a dimmed segment says nothing.** `C12` §3 requires the picker to
   *say so*; 40% opacity is a quiet no-op with extra steps. The segment is struck through **and**
   captioned, and the tile carries the word `dark only`.
5. **The avatar sheet needed `C22`'s overlay rule.** In neo, a menu that inherits the material's
   surface is the page colour plus a shadow pair — transparent — and the dimmed Home screen reads
   straight through it. It is the one neo surface that must be opaque.

**Then Ido asked to see Blossom, and rev 3 found three more — the first of which was the worst
defect in the asset.**

6. **The skin picker changed nothing.** `AppSkin` was a swatch and no material read it, so Aurora and
   Blossom rendered **identically in all four materials**. `C12` §4.1 names this exact failure for
   dark neo — *"picking Blossom under dark neo silently renders Aurora and the skin picker stops
   working for a quarter of the set"* — and the prototype had it for **four quarters**. The skin is
   now the token source and each material applies its declared transform: **identity** (glass,
   liquid), **mute** (neo — desaturated accent, warmed ground), **single-accent ramp** (dark neo).
   The material tiles follow it too, since a preview that ignores the skin is the same bug one axis
   over.
7. **A translucent panel that only adds white is illegible over a bright ground.** Liquid glass's
   dark-theme surface was white gradients alone, so it could only *lighten* the backdrop — and under
   Blossom, where a radial peaks, its own white type failed. It now carries a **tint floor**. Invisible
   under Aurora, whose hues are already dark.
8. **A skin owes a luminance contract, not just a hue.** Blossom's first pass used its light-scheme
   hues in the dark scheme; the fix is the same hues at **~20% less lightness**.

`Observed:` all eight in headless Edge renders on 2026-08-15, across glass · liquid · neo light ·
dark neo, both skins, English and Hebrew. `Untested:` in Compose — every material cost `C12` already
priced (`Modifier.blur`, `RenderEffect` API 31+, hand-drawn `Canvas` shadows) applies here unchanged,
and this asset is HTML.

## Liquid glass, rev 3

Rebuilt against reference images Ido supplied — glossy saturated lozenges rather than frosted panes.
What the references have that rev 2 did not: a **bright band along the top edge**, a soft **specular
bloom** in the upper-left, a **dim counter-rim** underneath, and a **coloured glow cast on the
ground**. That is the *refraction* reading `§4.1` actually specifies for this material
(*"refraction at the edge · translucent body, bright inner rim, dim outer counter-rim, one specular
streak"*), where rev 2 had drifted into a second glassmorphism.

All four are background and box-shadow layers, **not a pseudo-element**: an `::after` with `inset: 0`
paints over a card's static children, which is how a gloss overlay eats its own label.

## Bidi

Every time, range, date and count is `<bdi>`-isolated. Without it the algorithm renders
`07:00 – 23:00` as `23:00 – 07:00` — `§0.8`'s third sub-rule, and the reason the Hebrew renders are
part of the review rather than a courtesy pass afterwards. The 24 h track mirrors as a whole: `00`
sits on the right, the awake band grows leftward, and the threshold caption hangs off the rule on the
side that has room.

Hebrew strings here are **authored, not translated** (`§5.1`): every sentence on this screen embeds a
number or a proper noun, which is exactly the class that rule covers.
