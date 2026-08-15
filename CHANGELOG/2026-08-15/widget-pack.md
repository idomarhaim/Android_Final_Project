# 2026-08-15 — `widget-pack`

`/implement #10` — the home-screen widget pack. Spec §4.5 (widgets), §4.1 (the material
contract), §4.4 (charts and the dashboard), §4.9 (why neo), §4.8 / §0.8 (bidi and Hebrew).

Ran alongside two live siblings, `36-tasks-consent` and `d2-life-area-route`, in one working
tree. Board: [`SESSIONS.md`](../../SESSIONS.md).

---

## What shipped

**Five of §4.5's seven tiles, each at `2×2` / `4×2` / `2×4` / `4×4`** — `goals · week · trend ·
effort · level` — as real Android app widgets, placed from the launcher's picker, outside the
app. Design of record: [`docs/prototypes/2026-08-10-charts-presentation/`](../../docs/prototypes/2026-08-10-charts-presentation/README.md).

`decisions` and `today` are **not** built. See *What was left out* — the reason is data, not
design, and it is filed rather than silent.

### The seam, and why it is the load-bearing decision

Everything a tile renders comes from one flat `WidgetSnapshot`. **`BuildWidgetSnapshotUseCase` is
the only file in the pack that reads a domain model**; nothing under `ui/widget/` imports `Goal`,
`LifeArea` or `User` at all.

That was chosen on the day rather than in the abstract: `d2-life-area-route` was mid-rename of
`Goal.lifeAreaId` → `lifeAreaIds: List<String>` (§1.2 / §7.1) while this was being written. A
model change that reaches one function is a different thing from one that reaches five tiles and
twenty layouts — and the rename did in fact arrive, into exactly one line.

### Where §4.5's size rule lives

`BuildWidgetTileUseCase` is pure — no Glance, no `Context`, no `R` — and decides **which rows
survive, which sentence the disclosure shrinks to, and what the headline says**. The renderer
decides nothing. That split is the whole reason the rule can be tested: a rule that exists only
inside a `RemoteViews` tree is a rule nobody can check.

Ido's overturn of revision 1's widget ban was re-cut as *the disclosure shrinks to the smallest
true sentence the tile can hold, and no size ships without one*. Implemented as
`WidgetTile.derivesNumbers`: `week`, `trend` and `effort` divide shared minutes and `level` shows
points (which are `round(minutes/3) × difficulty`), so all four carry a disclosure at **every**
size; `goals` shows a goal against its own target, which is neither derived nor divided, so it
owes one only where it hides a goal it could not draw.

> The prototype gives the `4×2` trend tile no footnote. It gets one here — the columns are the
> same divided minutes as the donut, and the rule is about the **number**, not the size.

### Neo, and this ticket's own decision

[#10](https://github.com/idomarhaim/Android_Final_Project/issues/10) asked this ticket to decide
whether every material owes a tile version or the pack picks one. **The pack picks neo, on the
record.** §4.9 already defaults the material to neo *partly for this ticket* — the only material
with both a light and a dark scheme **and** no blur under it. Glass and liquid glass are *made
of* blur and refraction and have no `RemoteViews` form at all; dark neo is brightness-locked and
a home screen must follow the device's own switch.

§4.1's enforcement note is honoured rather than assumed: every colour derives from
`colorSchemeFor(skin, isDark)` — the app's own theme function, so there is one palette in this
codebase and not two that agree until one is edited. Neo's ground is tinted **toward** the skin;
neo's `mute` transform moves saturation and lightness and leaves hue alone, which is what keeps
Aurora looking like Aurora after it. Life-area hues are **not** muted — they are categorical and
muting them toward a common ground is the collapse §4.1's `.tag` rule exists to survive.

### Depth without any of the primitives

§4.5: *Glance has no equivalent of the SVG specular bevel and turbulence — in a widget that depth
must come from a pre-rendered bitmap or from `Canvas` drawing.* So `WidgetCharts` draws the
donut, the stacked columns, the rings and the effort bars with `android.graphics.Canvas`: each
shape drawn three times (shadow down-right, fill, highlight up-left) inside an inset groove, then
a hairline `--edge` stroke, because §4.1 forbids an affordance that is shadow-only. Nothing
animates — `ChartAnimation.kt` does not run there — and every bitmap is capped at 320 px, because
an oversized one does not render slowly, it **throws**, on somebody else's launcher.

### The pull, not the push

The widget reads its own snapshot in `provideGlance` through a Hilt `EntryPoint`, with a 4 s
ceiling and a `SharedPreferences` cache underneath. A push design (the app writing on every
foreground) would have needed a hook in `ui/root/` — another session's file that day — but it is
also simply worse: a tile that only updates when you open the app is stale exactly when you are
looking at the home screen instead of the app, which is the entire situation a widget is for.

**Added beyond the design of record, deliberately:** every tile carries an **as-of stamp**. The
prototype has none because a prototype cannot be stale. A snapshot surface that Android refreshes
on a schedule it is free to defer, showing a number with no stamp, is asserting a freshness
nobody promised it — §5.3's shape, one surface over.

### Hebrew and RTL

`values-he/widget_strings.xml` ships, and §4.8's defect class is fixed **in code**, not in
translation: `core/util/Bidi.kt` wraps every mixed run in the Unicode isolate pair, so
`3.2 / 4 km` and `14:32` cannot reverse inside an RTL paragraph. The trend tile's columns are a
**bitmap**, and a bitmap does not mirror itself, so they are drawn in reverse under RTL to agree
with the label row that does.

---

## What was left out, and why

**`decisions` and `today` are schedule surfaces, and §2 scheduling is unbuilt.** `Task` carries no
due time at all, and §7.2 already records that `GoogleTasksClient.kt:145` parses Google's `due`
into a field no other line in the repo reads. A *"4 decisions waiting"* tile computed from nothing
would be §0.3's *second number that quietly disagrees* manufactured on purpose. They become nearly
free once §2 lands — one `when` branch each, plus a receiver — and are filed in
[`TODO/TODO_OPTIONAL/Presentation.TODO.optional.md`](../../TODO/TODO_OPTIONAL/Presentation.TODO.optional.md)
§3 with four other owed items.

**The honest product criticism, stated rather than buried:** those two are the tiles that answer
*what needs me now?*, which §4.2 says is Home's entire job. So this pack ships the **analytics**
half of the product and none of the **act on it** half. That is correctly attributed to §2, but it
does change what a user gets from the pack today: a dashboard, not a prompt.

---

## 🧪 Tests

- **Layers covered**: JVM unit (`app/src/test`).
  *Skipped: instrumented UI (`app/src/androidTest`) — a Glance tile renders to `RemoteViews` in
  the **launcher's** process, which Espresso cannot reach; the equivalent is
  `androidx.glance:glance-appwidget-testing` + Robolectric, neither of which is a dependency and
  adding them was out of this unit's scope. Covered instead by moving every **decision** into a
  pure use case, which is what `BuildWidgetTileUseCaseTest` exercises.*
  *Skipped: `firestore-tests/` — this unit adds no rule, reads no new collection, and writes
  nothing to Firestore.*
  *Skipped: `functions/` — untouched, and §7.2 already records it has no test layer at all.*
- **Results**: **317 passed · 0 failed · 0 skipped** (`:app:testDebugUnitTest`, after the device-pass fixes), of which
  **51 are this unit's**.
  `:app:assembleDebug` ✅ (22:00) — which is what proves the manifest's five receivers, the five
  `appwidget-provider` XMLs and both string tables actually resolve.
- **New test files**:
  - `app/src/test/java/com/idomarhaim/goalpilot/widget/BuildWidgetTileUseCaseTest.kt` (26)
  - `app/src/test/java/com/idomarhaim/goalpilot/widget/BuildWidgetSnapshotUseCaseTest.kt` (10)
  - `app/src/test/java/com/idomarhaim/goalpilot/widget/WidgetSizeTest.kt` (7)
  - `app/src/test/java/com/idomarhaim/goalpilot/widget/WidgetPaletteResourceTest.kt` (3) *(new — the
    colour resources are a hand-written projection of `WidgetPalette`'s arithmetic, so this
    recomputes all sixteen values and fails if the XML drifts. It is also why that arithmetic is
    now pure Kotlin: `android.graphics.Color` is an unmocked stub on the JVM, so the version that
    used it could not be checked without Robolectric)*
  - `app/src/test/java/com/idomarhaim/goalpilot/widget/BidiTest.kt` (5)
  - `app/src/test/java/com/idomarhaim/goalpilot/widget/FakeWidgetStrings.kt` *(fake, not a suite)*
- **Failures**: none.

**The test that matters most** is `every tile showing a derived or divided number carries a
disclosure at every size`, paired with `the disclosure really does shrink`. The first alone would
pass against a fake returning one constant at every size — which is exactly the tautological test
that makes a rule look guarded while it rots. The second is the half that bites.

**Verified after the adversarial pass, not before it.** The first green run was at 21:26; the five
fixes landed after it, and re-verification was blocked for 14 minutes by a fourth session's
`GoalProgress.kt` holding the whole module red — zero of those errors named a widget-pack file.

---

## 📱 The device pass (2026-08-16) — and what it found

Ido offered a device, which closed the `unverified` this entry originally carried. Emulator
`Pixel_10_Pro_XL`, API 37, 1344×2992 @ 480 dpi, real signed-in account.

**It found five defects, all mine, none of which any test could have caught.** That is the point
worth keeping: the suite proves *which rows survive and which sentence the disclosure shrinks to*,
and it is structurally incapable of proving that a `RemoteViews` inflated by another process looks
like anything at all.

| # | Defect | Root cause | Fix |
|---|---|---|---|
| 1 | **Dark mode ignored.** Device went dark, tiles stayed light — and stayed light through a forced `APPWIDGET_UPDATE` | A `RemoteViews` is inflated *later, by the launcher*, so a single colour resolved while building the tree is the wrong one by then. A widget cannot ask what the theme is | The palette is now a **colour resource** (`values/` + `values-night/widget_colors.xml`) read via `ColorProvider(resId)`; the launcher resolves it at inflate time. Chart bitmaps, which cannot carry two answers, are redrawn in theme-neutral `ChartInk` |
| 2 | **A cold process left tiles blank** — Glance's loading placeholder, 30 s+ | The cache fallback ran *after* a 4 s network attempt, inside the same coroutine, so it protected everything except the case it existed for | The cache renders **first**, and the refresh runs inside the composition (`LaunchedEffect`) and replaces it when it arrives |
| 3 | **The four-size ladder collapsed.** A nominal `2×2` is ~190 dp on this device, past the 180 dp threshold, so the smallest tile drew the **largest** layout | A fixed dp threshold guessed from Android's nominal `70n − 30` cell formula. §4.5 says outright that dp per cell varies by device and launcher, so no constant could be right | `SizeMode.Responsive` with the four **declared design sizes**. The launcher matches against what it really granted; `LocalSize` returns one of the four exactly |
| 4 | **`trend` and `effort` arrived cramped** — declared `targetCellWidth="4"`, offered at `2×2` | `minWidth="110dp"` wins over `targetCellWidth` in the picker | `minWidth="250dp"` on those two, `minResizeWidth` left at 110 dp so they can still be shrunk. `Observed:` the picker now offers `Day by day` at **3×2** |
| 5 | **All five picker previews are the app icon**, so they are indistinguishable | `previewImage="@mipmap/ic_launcher"` — rated 🔵 in the adversarial pass, which was too low | **Not fixed.** Cosmetic and confined to the picker; filed in `Presentation.TODO.optional.md` §3 |

**Confirmed working after the fixes**, both brightnesses, no refresh in between: the tiles flip the
instant the device switches. The `level` tile renders its ring, its real values (`1`,
*30 to the next level*), the **short** form of its disclosure at `2×2`, and the as-of stamp in the
device's own 12-hour clock. The `goals` header reads **Goal**, singular — the `2×2` layout, which
is what proves the ladder is no longer collapsed.

**One product finding, not a defect.** On the real account, **8 goals carry no measure** (all still
on the `%` placeholder) and **0 tasks were completed this week**, so four of the five tiles
correctly show an empty state. The pack is not broken; it is reporting that nothing is being
measured — `C7` and §4.6 arriving on the home screen.

**A pre-existing defect was spotted in passing and is not this unit's:** the dashboard reads
**"Overall progress 16259%"**. §4.4 predicted exactly this — `DashboardViewModel.kt:103`, a plain
mean of `progressFraction`, which breaks once overshoot is legal.

⚠️ **Still `unverified`: Hebrew.** The app has no language picker until §5.1, so the RTL render —
in particular the trend tile's bitmap columns, reversed in code rather than by the layout — has
been read but not seen. §0.8 is satisfied in intent and not in fact.

---

## 🧷 The adversarial pass, and what it actually found

`/adversarial-review` on the diff, all three passes. It was not clean — five findings, all fixed
before this commit:

1. **🔴 `Bidi.isolate`'s idempotence guard was wrong.** It skipped any string starting with `FSI`
   and ending with `PDI` — which is also true of `⁨a⁩ · ⁨b⁩`, **two** isolates with text between
   them. A row composed from two already-isolated parts was therefore left un-isolated *while
   looking isolated*: §4.8's own defect surviving the fix for it, and invisible in an English
   render. Now walks the depth. Pinned by `a composite of two isolated parts is itself isolated`.
2. **🟡 `firstOrNull()` on the plural life-area edge was a silent narrowing.** `d2-life-area-route`
   adapted the seam that way to keep the tree compiling, correctly for the moment. But §4.7 says a
   success counts **in full** in every area the work serves and only its *minutes* divide — so a
   goal filed under Health *and* Career would have vanished from one of them on the effort tile,
   only for the users who file that way. Carried through as `areaIds: List<String>`; two new tests.
3. **🟡 `glance-material3` was an unused dependency.** Never imported. Dropped — decision ladder
   rung 5.
4. **🟡 Four dead symbols**, found by running the deletion test on every new name rather than by
   eye: `WidgetTileBody.Headline` (never constructed once the effort tile changed shape),
   `WidgetPalette.surface`, `.edgeProvider`, `.grooveProvider`, and `hasPlacedWidgets()`. All cut.
   `refreshAllWidgets()` and `WidgetSnapshotStore.clear()` were **kept** despite having no caller —
   both have a *named* owed call site in the TODO, and `refreshAllWidgets` encodes the
   one-class-per-tile subtlety below, which is not obvious and was already got wrong once.
5. **🔵 The shrink test excluded `trend` for no stated reason.** Now derived from
   `derivesNumbers`, so a tile added later cannot quietly escape it.

---

## 🧰 Two things worth not re-deriving

**`GlanceAppWidgetManager` resolves placed widgets by *class*.** Five receivers sharing one
`GlanceAppWidget` class means `updateAll` walks every id declaring that class — so refreshing one
tile re-renders **all** of them as that tile. Place the week donut and the level ring, refresh
one, and both become the same card. Hence one concrete subclass per tile, otherwise empty.

**KSP and Gradle accessors lock under concurrent builds on Windows, and re-running does not clear
it.** Both hit during this session, with three sessions sharing one `app/build`:
`NoSuchFileException` on `hilt_aggregated_deps` (cleared by `rm -rf app/build/generated/ksp`) and
`Could not move temporary workspace … dependencies-accessors` after a `libs.versions.toml` edit —
that one persisted through four retries and cleared only after waiting for the sibling's daemon to
go idle. The `AGENTS.md` pitfall is real and it is provoked by concurrency, not by chance.

---

## 🤝 Sibling sessions

Two live throughout. **No file belonging to either was edited.** The three shared files this unit
would otherwise have needed — `ui/root/GoalPilotRoot.kt`, `ui/navigation/Destinations.kt`,
`ui/components/BidiText.kt` — were **designed out rather than deferred**, which is why the widget
pulls its own snapshot and carries its own string-level bidi helper.

`36-tasks-consent` flagged on the board at 20:52 that this unit's
`BuildWidgetSnapshotUseCase.kt:88` was the single error stopping `:app:compileDebugKotlin`, and
therefore stopping all three sessions from running any test. `d2-life-area-route` repaired it in
place. Both were told the tree was green at 21:26, before this unit finished.
