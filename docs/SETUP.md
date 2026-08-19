# GoalPilot — Setup Guide

This project builds and runs **as-is** with placeholder config, but to exercise
the real backend (Google Sign-In, Firestore, Storage, GROQ recommendations) you
must plug in your own credentials. Nothing here is committed as a secret.

Everything you need to provide is listed below. Steps marked **(you)** need your
Google/Firebase/GROQ accounts.

---

## 0. Prerequisites (already verified on this machine)

| Tool | Version |
|------|---------|
| JDK (for Gradle) | Temurin **21** (pinned in `gradle.properties` → `org.gradle.java.home`) |
| Android SDK | platform **35**, build-tools 35 |
| Node | 22+ (Cloud Functions target the **nodejs22** runtime) |
| Firebase CLI | `npm i -g firebase-tools` |

> **Runtime note.** Cloud Functions used to target `nodejs20`, which Google
> deprecated on 2026-04-30 and decommissions on 2026-10-30. `firebase.json` and
> `functions/package.json` now both pin **Node 22** (supported until 2027).

Build the app now (no credentials required to compile):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew :app:assembleDebug
```

---

## 1. Create the Firebase project **(you)**

1. Go to <https://console.firebase.google.com> → **Add project** (e.g. `goalpilot`).
2. **Upgrade the project to the Blaze (pay-as-you-go) plan.** This requires a
   credit card. Both features below need it:
   - **Cloud Functions** has always required Blaze to deploy.
   - **Cloud Storage** requires a linked billing account since 2026-02-03,
     regardless of usage volume.

   Real cost for this project is **$0** — pick region `us-central1` so Storage
   stays inside the always-free 5 GB tier, and set a budget alert (Google Cloud
   console → Billing → Budgets & alerts) at ~$1 so nothing can surprise you.

   *Without Blaze* the app still builds and runs, and Auth + Firestore work, but
   image upload and the GROQ coach do not — that costs you two spec features.
3. **Build → Authentication → Sign-in method →** enable **Google**.
4. **Build → Firestore Database →** create (production mode is fine; we ship rules).
5. **Build → Storage →** enable (region `us-central1`).

## 2. Register the Android app + add the debug SHA-1 **(you)**

1. Project settings → **Your apps → Add app → Android**.
2. Package name: **`com.idomarhaim.goalpilot`** — and add a second app for the
   debug build id **`com.idomarhaim.goalpilot.debug`** (the debug build appends
   `.debug`). *(Or just add the `.debug` one if you only run debug builds.)*
3. **Add this debug signing certificate SHA-1** (already computed from your
   local debug keystore):

   ```
   F1:D0:96:4D:54:41:D5:99:86:7D:AE:83:0F:77:16:23:BB:64:DB:3F
   ```

   > Regenerate anytime with:
   > ```bash
   > keytool -list -v -keystore "$HOME/.android/debug.keystore" \
   >   -alias androiddebugkey -storepass android | grep SHA1
   > ```
4. Download **`google-services.json`** and replace the placeholder at
   [`app/google-services.json`](../app/google-services.json).

## 3. Provide the Web client ID **(you)**

Google Sign-In needs the OAuth 2.0 **Web** client id (type 3). Find it in the
Google Cloud console (**APIs & Services → Credentials**, the "Web client
(auto created by Google Service)" entry) or inside the downloaded
`google-services.json` under `oauth_client` with `"client_type": 3`.

Add it to **`local.properties`** (git-ignored):

```properties
GOOGLE_WEB_CLIENT_ID=1234567890-xxxxxxxxxxxxxxxx.apps.googleusercontent.com
FUNCTIONS_REGION=us-central1
```

## 4. Deploy security rules + Cloud Functions **(you)**

```powershell
firebase login
firebase use --add            # pick your new project, alias it "default"

# GROQ key for the LLM proxy — free, no credit card, at https://console.groq.com/keys
Copy-Item functions\.env.example functions\.env
# edit functions\.env and set GROQ_API_KEY=gsk_...

cd functions; npm install; cd ..
firebase deploy --only firestore:rules,storage,functions
```

> For production, store the key as a secret instead of `.env`:
> `firebase functions:secrets:set GROQ_API_KEY` and read it via `defineSecret`.

### GROQ model — check before you demo

`functions/src/index.ts` pins **`openai/gpt-oss-20b`**, overridable with
`GROQ_MODEL` in `functions/.env`. GROQ retires models on a rolling schedule
(`llama-3.1-8b-instant`, the previous default, was announced 2026-06-17 and shut
down 2026-08-16). A retired model makes every LLM call fail **silently** — the
client falls back to local tips by design (spec §8), so the AI simply looks
generic rather than broken.

Confirm the pinned id is still listed at <https://console.groq.com/docs/models>
and not on <https://console.groq.com/docs/deprecations> before the demo. Free
tier is 30 requests/min and 14,400/day — far above what a demo needs.

## 5. OAuth consent screen (test mode) **(you)**

Per spec §8, keep OAuth in **Testing** mode and add your Google account under
**OAuth consent screen → Test users**. No app verification needed for the demo.

## 6. Run

One command brings up a device, builds, installs and launches — no Android
Studio required (see [scripts/README.md](../scripts/README.md)):

```powershell
.\scripts\run-goalpilot.ps1          # phone if plugged in, else the emulator
```

Or double-click `scripts\Run GoalPilot.cmd`. The manual equivalent:

```powershell
# start an emulator (or plug in a device), then:
.\gradlew :app:installDebug
```

Sign in with a **test user** Google account. You should land on the dashboard.

### Installing on a physical phone

The debug keystore SHA-1 registered in Firebase is this machine's
(`F1:D0:96:...:DB:3F`), and both `com.idomarhaim.goalpilot` and
`com.idomarhaim.goalpilot.debug` are registered. So a build produced **on this
machine** signs in correctly on a real phone with no extra setup:

1. Phone: `Settings → About phone` → tap **Build number** ×7.
2. `Settings → System → Developer options` → **USB debugging** on.
3. Plug in with a data cable, accept *"Allow USB debugging?"*.
4. `.\scripts\run-goalpilot.ps1 -Target device`

Building on a *different* machine produces a different debug keystore, and
Google Sign-In then fails with **code 10 (DEVELOPER_ERROR)** — see the
troubleshooting table below.

---

## Local development without a real backend (optional)

Use the **Firebase Local Emulator Suite** to run Auth/Firestore/Storage/Functions
locally:

```powershell
firebase emulators:start
```

Then point the app at the emulator by adding this to `FirebaseModule` (debug
only) — see the commented block in [ARCHITECTURE.md](ARCHITECTURE.md#emulator).

---

## What each credential unlocks

| Missing | Symptom |
|---------|---------|
| Real `google-services.json` | App builds, but sign-in/Firestore fail at runtime |
| `GOOGLE_WEB_CLIENT_ID` | Google Sign-In returns "no ID token" / DEVELOPER_ERROR |
| Debug SHA-1 in Firebase | Google Sign-In fails with code 10 (DEVELOPER_ERROR) |
| `GROQ_API_KEY` in functions | AI coach silently uses local fallback tips (by design) |
