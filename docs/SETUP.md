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
| Node | 20+ (Cloud Functions) |
| Firebase CLI | `npm i -g firebase-tools` |

Build the app now (no credentials required to compile):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew :app:assembleDebug
```

---

## 1. Create the Firebase project **(you)**

1. Go to <https://console.firebase.google.com> → **Add project** (e.g. `goalpilot`).
2. **Build → Authentication → Sign-in method →** enable **Google**.
3. **Build → Firestore Database →** create (production mode is fine; we ship rules).
4. **Build → Storage →** enable.

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

# GROQ key for the LLM proxy — get a free key at https://console.groq.com/keys
Copy-Item functions\.env.example functions\.env
# edit functions\.env and set GROQ_API_KEY=gsk_...

cd functions; npm install; cd ..
firebase deploy --only firestore:rules,storage,functions
```

> For production, store the key as a secret instead of `.env`:
> `firebase functions:secrets:set GROQ_API_KEY` and read it via `defineSecret`.

## 5. OAuth consent screen (test mode) **(you)**

Per spec §8, keep OAuth in **Testing** mode and add your Google account under
**OAuth consent screen → Test users**. No app verification needed for the demo.

## 6. Run

```powershell
# start an emulator (or plug in a device), then:
.\gradlew :app:installDebug
```

Sign in with a **test user** Google account. You should land on the dashboard.

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
