# GoalPilot 🎯

**Pilot your life goals.** An Android app that helps you define life goals
(health, sleep, fitness, nutrition, relationships, career, projects…), track
progress, get **AI-powered recommendations**, and stay motivated with a social
leaderboard and gamification.

> Final project for the *Android Application Development* course.
> Full specification: [`GoalPilot_spec_EN.docx`](GoalPilot_spec_EN.docx).

The system works on three levels (spec §1):

1. **Tracking** — visual progress rings & bars for every goal.
2. **Recommendations** — GROQ LLM analysis, encouragement, and task→goal classification.
3. **Motivation** — points, levels, a friends leaderboard, and shareable summaries.

---

## ✨ Features

**Core (MVP — fully implemented)**
- ✅ Google Sign-In (Firebase Auth)
- ✅ Define goals across life categories
- ✅ Associate tasks with goals, with **points** on completion
- ✅ **AI point scoring** for a task (✨ button on the add-task row)
- ✅ Manual progress logging + **visual progress rings/bars**
- ✅ **Image upload** to Firebase Storage (attach a photo to progress)
- ✅ **Sharing between users**: weekly summaries + a **friends leaderboard** (spec §7),
  friends added with a 6-character **friend code**
- ✅ **AI recommendations & encouragement** via GROQ (through a Cloud Function proxy)
- ✅ Points, levels, and level progress (gamification)
- ✅ **Two selectable colour skins** — *Aurora* (ocean blue → evergreen, default)
  and *Blossom* (sunset pink → orange), chosen on the Profile tab and applied
  instantly across the app. Both ship a full light **and** dark palette.

- ✅ **Life areas** — your own division of your life (health, studies, career,
  relationships…), defined on *Profile → Life areas* or **synced from your Google
  Tasks list names**. Every goal is filed under one, and that is what the time
  chart reports on.

- ✅ **A guided tour on first launch** — seven coach marks over the *real* app: a
  dimmed screen with a hole cut over the thing being described, one step at a
  time. Skippable from any step, and replayable whenever you like from
  *Settings → Help → Replay tutorial*. One step waits for you to open the Goals
  tab yourself and then follows you there.

**Bonus (implemented)**
- 🤖 **LLM task→goal classification** — the "Smart add a task" card: describe a
  task in plain language and GoalPilot files it under the right goal, or proposes
  a new one, with an estimated point value, an estimated **duration**, and the life
  area it belongs to. You confirm before anything is saved.
- 🥧 **"Where your time goes"** — an interactive donut showing what share of your
  life went into each life area, from the AI's duration estimate for every task you
  completed. Switch between **day / week / month / quarter / year**; tap a slice
  for its hours and share.
- 📊 **Analytics charts** (progress per goal, task-focus split) — every chart draws
  itself: bars grow from zero with a staggered sweep, the donut unrolls clockwise,
  and the numbers count up with them.

**Nice-to-have (implemented)**
- ✅ **Import tasks from Google Tasks** — the "Import from Google Tasks" card
  pulls your open tasks in and files each one under the right goal. Deduped
  against what is already there; you review everything before it is saved.
- ✅ **Pull fitness & sleep from Health Connect** — steps and sleep are read
  automatically **every time you open the app** (at most once every fifteen
  minutes) and logged against a fitness or sleep goal, creating one if you have
  none. **Read-only** (GoalPilot never writes to your health store). A day is
  never counted twice, and today is topped up by the difference as you walk, so
  the goal keeps up without the day being logged again. The "Health data" card
  shows when the last sync ran and can force one.

**Nice-to-have (architected + scaffolded, see `TODO/`)**
- 🧩 Shared/competitive **challenges** — model + Firestore rules + preview screen

---

## 🏗️ Tech stack

Kotlin · Jetpack **Compose** + Material 3 · **MVVM** · **Hilt** DI · Coroutines/Flow ·
Navigation-Compose · **Firebase** (Auth · Firestore · Storage · Functions) ·
**GROQ** LLM (via Cloud Functions) · Coil.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the layered design and data model.

---

## 🚀 Quick start

```powershell
# 1) Build (compiles with placeholder config — no credentials needed)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew :app:assembleDebug

# 2) Run unit tests
.\gradlew :app:testDebugUnitTest

# 3) Install on a device/emulator
.\gradlew :app:installDebug
```

To make the real backend work (Firebase, GROQ, Google Sign-In), follow
**[docs/SETUP.md](docs/SETUP.md)** — it lists exactly which credentials to add
and includes your debug **SHA-1**.

---

## 📁 Project layout

```
app/                     Android app (Compose + MVVM)
  src/main/java/com/idomarhaim/goalpilot/
    core/                Resource wrapper, utils, constants
    domain/              models · repository interfaces · use cases  (no Android/Firebase)
    data/                Firebase + GROQ implementations, integration stubs
    di/                  Hilt modules
    ui/                  theme · shared components · navigation · root · tutorial
    feature/             auth · goals · dashboard · social · profile · analytics ·
                         lifeareas · challenges
  src/test/              JVM unit tests
  src/androidTest/       Compose UI + instrumented tests
functions/               GROQ proxy Cloud Functions (TypeScript)
firestore.rules · storage.rules · firebase.json   Backend config & security rules
docs/                    SETUP.md · ARCHITECTURE.md
TODO/                    Backlog for nice-to-have / bonus tiers
```

---

## 🧪 Tests

See the latest [CHANGELOG](CHANGELOG/) entry for pass/fail counts and covered
layers. Run `.\gradlew :app:testDebugUnitTest` for the JVM suite; instrumented
UI tests (`connectedDebugAndroidTest`) require an emulator/device.

---

## 📄 License / attribution

Course project by Ido. Built with Kotlin, Jetpack Compose, and Firebase.
