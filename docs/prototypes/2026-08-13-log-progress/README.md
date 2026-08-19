# C6 · LOG PROGRESS — prototype

Asset for [`C6` #22](https://github.com/idomarhaim/Android_Final_Project/issues/22) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). Open `index.html`;
material, theme and language are switchable in the bar, or by query string so
`shoot.ps1` can render any state:

```powershell
cd docs\prototypes\tools
./shoot.ps1 -Page ..\2026-08-13-log-progress\index.html -Out out.png `
            -Query "m=neo&t=light&l=he" -Width 1880 -Height 1180
```

`m=glass|liquid|neo|darkneo` · `t=dark|light` · `l=en|he`.

## What the four frames prove

| Frame | Goal | Proves |
|---|---|---|
| 1 | Drink 2 L a day (`VOLUME`) | buttons ladder, **additive**, repeat-tappable with a `×2` tally (`C7` + [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)); **no duration row** — "how long did drinking take" is not a question |
| 2 | Run 20 km a week (`DISTANCE`) | number, additive; the **optional duration row**, never pre-filled and never guessed; the effort/outcome disclosure line |
| 3 | Lose 5 kg (`MASS`) | **absolute reading**, may fall; progress reads `2.6 of 5 kg lost` rather than a percentage (`C3`'s `(current − start)/(target − start)`) |
| 4 | Sleep 56 h a week (`DURATION`) | Health Connect rows are **locked**; your own entry is logged beside them |

Every frame's history shows Ido's two decisions: an entry is **always editable** and an edit is
**always marked**, with the original one tap away and a soft-deleted row struck through rather
than vanishing.

## Material contract

`#12`'s Standing preferences require a screen to be designed against **surface · groove ·
elevation · accent**, not against one look, so the four material blocks here are adapted from
[`2026-08-11-visual-styles`](../2026-08-11-visual-styles/index.html) (`C12` #31). Two additions
this screen forces, both from `C12`'s own warning:

- **`--edge`** — every control carries a hairline contrast anchor, so no affordance is ever
  shadow-only. That is neo's known WCAG failure, and this screen is entirely data entry.
- **`.tag`** — a category is written in words beside its dot, because dark neo replaces the six
  categorical hues with one accent ramp and colour stops carrying identity.

## Rounds, and what the renders caught

Six rounds. **Four of the five defects were invisible in the source** — the rule `C12`
established, reproduced here on a different screen:

1. **The sheet never inherited the material's foreground colour.** `color` was set on `.sc`, and
   the sheet is a *sibling* of it, so it fell back to the page's own grey. Looked correct on
   every dark canvas and was unreadable in neo light.
2. **The primary button lost its accent in light themes.** `.st-liquid[data-theme="light"] .btn`
   is (class + attr + class) and re-paints every `.btn` with the surface gradient, beating
   `.btn.primary`. Save rendered white-on-white. Fixed by raising the selector to
   `.st .btn.primary`.
3. **The number being logged was the smallest thing on the screen** — four large chips over a
   12.5 px muted "this entry: 500 ml". Now a 21 px total.
4. **Hebrew: `מ‑Health Connect` renders as `Health Connect‑מ`** — a Hebrew prefix on a Latin run
   is laid out on the far side of it. Replaced with `מקור: Health Connect`.
5. A steps reading was filed under a weight goal — a content error, corrected by giving the
   synced rows their own sleep goal, which is what Health Connect actually provides
   (`BuildHealthProposalsUseCase` reads **steps and sleep only**; there is no workout read, so a
   run's duration has no measured source today).

Numbers and clock times are wrapped in `<bdi>`, since bidi reorders `09:00–12:00` — the same
finding `C9b` recorded, and it recurs in Compose.
