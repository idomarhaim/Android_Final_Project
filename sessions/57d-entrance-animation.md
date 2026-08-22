---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: active
issue: 57
owns:
  # Corrected at /kickoff 2026-08-22: the entrance vocabulary is its own file rather than
  # an addition to ChartAnimation.kt (that one is about *chart* progress and is keyed on
  # data; this one is keyed on screen arrival), Common.kt carries SectionHeader which sits
  # inside the same dashboard column, and the "does not re-run" assertion is cheapest at
  # the JVM layer. ChartAnimation.kt is read, not written.
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/Entrance.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/GpCard.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/Common.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt
  - app/src/test/java/com/idomarhaim/goalpilot/ui/GpEntranceTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/EntranceAnimationUiTest.kt
  - CHANGELOG/2026-08-22/57d-entrance-animation.md
  - sessions/57d-entrance-animation.md
created: 2026-08-21
---

# `#57` d — the blocks arriving, instead of being already there

**The fourth of four briefs on [#57](https://github.com/idomarhaim/Android_Final_Project/issues/57),
and the smallest.** It is **independent of the other three** — it touches motion, not colour or
geometry — so it can run at any point, including first if a short session is what is available.

Ido: *"the blocks don't appear with a fade-in like in the first prototype."*

## What the prototype does

`docs/prototypes/2026-08-10-calendar-surface/index.html`, and the same pattern in
`2026-08-10-charts-presentation`:

```css
@keyframes in    /* opacity */
@keyframes rise  /* translateY */
animation: in   .36s var(--spring);
animation: rise .34s both;   /* .34s / .36s / .38s -- a stagger, not one duration */
transition: all .26s–.32s var(--spring);
```

Two things matter and only one of them is the fade:

1. **It is a rise *and* a fade**, together — the card comes up as it comes in.
2. **The durations differ per card on purpose.** `.34` / `.36` / `.38` is a **stagger**: the list
   arrives as a sequence, not as one block. Ship a single shared duration and you have implemented
   something that is technically a fade-in and visibly not this.

Read the exact values out of the prototype rather than approximating them; `--spring` is defined in
the same file.

## What ships

1. A shared entrance treatment for `GpCard`-shaped blocks — rise + fade, spring-eased, staggered by
   position.
2. Applied to the surfaces Ido actually looks at: the dashboard's card column first.

## Watch for

- ⚠️ **It must not re-run on every recomposition.** An entrance animation that replays whenever
  state changes turns a nice arrival into a flicker on every edit. Key it to first composition, and
  test that by *changing a value on screen* and watching that nothing re-animates.
- ⚠️ **It must not fight the scroll.** These lists are inside a `verticalScroll`; a stagger keyed to
  index animates items that were never off-screen. Decide whether the trigger is *screen entry* or
  *item entry*, and say which.
- **Respect the system's reduce-motion setting.** An entrance animation is exactly the class of
  thing that setting exists for.
- `ui/components/ChartAnimation.kt` already exists and is the house pattern for chart motion. Read
  it before inventing a second one — this brief should extend the vocabulary, not fork it.
- The widget pack (`ui/widget/`) has **no animation at all** — it compiles to `RemoteViews`. Nothing
  here reaches it, and that is fine, but do not write code that assumes otherwise.

## Out of scope

Everything else on `#57`. This brief adds no colour, no background, no geometry.

## Exit

- The dashboard's blocks arrive staggered on a cold open.
- Nothing re-animates on an ordinary state change — asserted, not eyeballed.
- **Seen** on a device. A motion feature cannot be verified from a passing test or a screenshot;
  `screenrecord` is the instrument (`kb/dev/android-device-verification.md` §6.2).
- `CHANGELOG/<today>/57d-entrance-animation.md` · board row released · brief closed to
  `sessions/done/` with `status: done` in the same commit · commit and push under AUTO MODE.
