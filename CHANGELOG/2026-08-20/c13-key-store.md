# `c13-key-store` — 2026-08-20

> **Summary:** `C13`'s **encrypted key store** ships, and with it §4.9's fifth and last Settings section — so **#48's AI section exists**. Three pieces, all of them: a Keystore-backed `EncryptedSharedPreferences` store that is the one place in this app a third-party secret sits at rest; a **provider abstraction** whose four adapters live server-side in `functions/`, so the app gains *no outbound path to any model provider* and there is exactly one copy of every prompt and every `C11b` schema; and a **permanent status line** naming which provider answered, which is the honesty half — without it the app has two credentials and three rungs and the user cannot tell which one paid. **The proxy remains the default and works untouched with no key set**: the callable payload with no credential is byte-identical to what this app sent before `C13`. Four defects were found *by the checks rather than by review* and all four are fixed: an **off-screen Save button** in the key editor (a tap landed on the scrim and silently discarded the typed key), a **stale status line** claiming the free model was chosen over a key that did not yet exist, a JSON **array passing an object check** in the adapters, and a **secrecy guard that could never fire** because its own regex had been corrupted into a control character.

**Session:** `c13-key-store` · **Date:** 2026-08-20 (finished after midnight on 08-21) · **Mode:** `AUTO MODE` · **Brief:** [`sessions/c13-key-store.md`](../../sessions/c13-key-store.md) · **Issue:** [#54](https://github.com/idomarhaim/Android_Final_Project/issues/54), decided in [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32)

---

## What shipped, against the brief

| # | Piece | State |
|---|---|---|
| 1 | **The key store** — `androidx.security:security-crypto` → `EncryptedSharedPreferences` | ✅ `data/security/EncryptedAiCredentialStore.kt`, declared in `libs.versions.toml` + `app/build.gradle.kts` |
| 2 | **A provider abstraction**, proxy stays the default | ✅ `domain/model/AiCallEnvelope.kt` (wire), `data/security/DefaultAiProviderRepository.kt` (decisions), `functions/src/providers.ts` (four adapters) |
| 3 | **A status line naming which provider answered** | ✅ `feature/settings/AiStatusLine.kt`, rendered permanently by `AiCard` |
| 4 | The AI section's three controls in `SettingsScreen.kt` | ✅ `feature/settings/AiCard.kt` — provider · model · key |
| — | **The Cloud Function deploy** | ⛔ **NOT done — outward action, Ido's to run.** See *What is owed* |
| — | Test-call the key once on entry (#32 *Also settled*) | ⛔ **Not built** — out of #54's scope; see *What is owed* |

**All three exit directions are tested**, against the shipping repository rather than a fake:
`no key → proxy` · `key set → provider` · `key deleted → proxy again`.

---

## The four decisions worth reading

### 1. The Keystore is behind a three-method port, so the routing is testable

`AiCredentialStore` has `read` / `write` / `clear` and nothing else.
`DefaultAiProviderRepository` holds every decision `C13` makes — the ladder, §5's dead-key latch,
the routing — and needs no `Context`, no Keystore and no device.

The alternative was a hand-written fake repository, and it fails in a specific way: #54's exit
criterion would then be asserted against **a second implementation of the latch** rather than the
one that ships. Here the only thing substituted is a map.

It is deliberately **not** a general "secure storage" abstraction. One credential, one file, no
keys-by-name — a wider interface invites a second secret into a store whose backup exclusion names
exactly one path.

### 2. `allowBackup` stays `true`, and the key alone is excluded — on both schemes

#54 requires the choice to be deliberate and stated. It is: `res/xml/backup_rules.xml`
(`fullBackupContent`, API ≤ 30) and `res/xml/data_extraction_rules.xml` (`dataExtractionRules`,
31+) each exclude `goalpilot_ai_credentials.xml`, and the modern file excludes it from
**`cloud-backup` and `device-transfer` both** — a phone-to-phone copy is a separate channel with
its own opt-out, and excluding only the cloud half would leave the key riding a device transfer,
which is precisely the *re-entered on a new device* behaviour #32 §1 chose on purpose.

Turning backup off wholesale was the other candidate and was rejected: it would take the theme,
the language and the day schedule with it to protect a file excluded either way.

`Observed:` the exclusion **rules and the manifest attributes** are asserted by
`AiCredentialSecrecyTest`. `Untested:` no backup/restore cycle was run, so what is verified is
that the exclusion is *declared*, not that Android honoured it. `bmgr backupnow` + a restore is
what would settle it, and it is not in #54's scope.

### 3. An absent `answeredBy` means the free model answered — and that is the true reading

`AiCallEnvelope.answeredBy` is four lines and carries the property the whole ticket turns on.
A response with **no** echo is read as *"the free model answered"*, and that is not a defensive
default: a deployed function that predates `C13` ignores the credential fields entirely and calls
GROQ on `process.env.GROQ_API_KEY`, so the free model really did answer.

Reading it the other way — *we sent a key, so the key must have answered* — would put a provider's
name on screen for a call that provider never saw.

**This is not hypothetical, and it was watched happening.** `functions/` deploys separately from
the APK and the deploy is Ido's to run, so right now every install is a `C13` client talking to a
pre-`C13` function. On the device, with a key stored, the row read *"GoalPilot's free model
answered, not your OpenAI key"* — correct, against a live un-deployed function.

The three rungs are therefore all expressible on the wire, and `"none"` is deliberately **distinct
from the field being absent**: absent is an old deployment that really answered, `"none"` is a
current deployment reporting that nothing did. Collapsing them would make an outage read as *"the
free model answered"*.

### 4. Errors carry a class and a status, and there is nowhere to put a body

#32 filed a spec line after finding `callGroqJson` throwing `GROQ HTTP ${status}: ${body}` into
`logger.error` — harmless while the only key is the project's own, and a third-party provider's
error text in Google's logs the moment a user key is in play.

It is enforced **structurally** rather than by a rule someone remembers: `ProviderError` has no
body field, and `callUserProvider` never reads the response body at all. `providers.test.mjs`
asserts the body is not read, by flipping a flag if anything calls `.text()` or `.json()` on a
failed response.

`callGroqJson` still interpolates its body, deliberately: that key is GoalPilot's, its error text
is GoalPilot's, and a user's key never reaches that function.

---

## 🧪 Tests

Every layer this project has, all green.

| Layer | Result | Notes |
|---|---|---|
| **Cloud Functions unit** (`node --test`) | **56 / 56** | `functions/test/providers.test.mjs` is new — 19 of them |
| **JVM unit** (`:app:testDebugUnitTest`) | **609 / 609**, 0 skipped | 48 new across 4 suites |
| **Instrumented** (`am instrument`, `emulator-5554`, API 35) | **174 / 174** | `AiSectionUiTest` is new — 13 of them |
| **Firestore rules** (`firestore-tests/`) | **not run** — the layer exists and `C13` does not reach it: no rule changed, and nothing `C13` stores goes to Firestore at all (#32 §1 rejected exactly that, on posture) |
| **Emulator triggers** | **not run** — no trigger, no projection and no callable *contract* changed; the response gains two optional fields and every existing field is untouched |

New suites:

- `AiProviderRoutingTest` (13) — the three exit directions, plus §5's latch in both directions.
- `AiCallEnvelopeTest` (12) — the wire contract, and the absent-echo rule.
- `AiCredentialSecrecyTest` (10) — #54's hard requirements, asserted.
- `AiStatusLineTest` (13) — all eight reachable status states.
- `AiSectionUiTest` (13) — the masked field, the replace action, the delete.
- `providers.test.mjs` (19) — the four adapters and #32 §5's failure table.

**Instrumented tests were run via `install -r` + `am instrument`, not
`connectedDebugAndroidTest`** — the sign-in on the emulator was preserved throughout and is intact.

### Negative controls — every new guard was checked against the accident it exists for

A guard that cannot fail is worse than no guard, so each was made to fail on purpose and then
restored:

| Guard | Injected accident | Fired? |
|---|---|---|
| `no key-bearing file contains a logging statement` | `Log.e("Rec", "call failed for $credential", e)` in `RecommendationRepositoryImpl` | ✅ |
| `no key-bearing file interpolates a whole credential` | `val debugA = "cred=$credential"` | ✅ **only after a repair — see below** |
| `only the store the envelope and the editor read the key itself` | `val debugB = credential.key.length` | ✅ **only after a repair — see below** |
| `theSaveButtonIsOnScreenAsSoonAsTheEditorOpens` | the pre-fix layout | ✅ (that *is* how the defect was found) |

---

## Four defects, all found by the checks rather than by reading

### A · The editor's Save button was off the bottom of the screen

`Observed:` 2026-08-20, `sdk_gphone64_x86_64`, 1344×2992. The sheet opened *partially expanded*
with the form scrolling inside it, which put **Save at `y=3033px` on a `2992px` screen**. A tap
there does not miss harmlessly — it lands on the **scrim**, so the sheet closes and the key the
user just typed is discarded, with no exception anywhere.

It reads perfectly in a screenshot of the top of the sheet, and no JVM test can see geometry.
Fixed by opening the sheet fully expanded **and** pinning the action row outside the scrolling
`Column`, so the row that commits the form can never scroll away — including with the keyboard up,
which is the case that would defeat the first fix alone. `theSaveButtonIsOnScreenAsSoonAsTheEditorOpens`
is the regression test.

### B · The status line claimed the free model was chosen over a key that did not exist yet

`Observed:` on the device. The dashboard makes a feed call at launch; adding a key afterwards then
rendered *"GoalPilot's free model answered, not your OpenAI key"* — true word by word, and it reads
as though the key had been tried and passed over, about a call made before the key was typed.

`save()` now clears `lastAnswer`. `clear()` deliberately does **not**, and the asymmetry is the
point: after a clear there is no credential for the line to be about, so the null-credential branch
answers and the stale value is never rendered. After a save it is.

### C · A JSON array passed an object check

`typeof [] === "object"`, so `extractJson` returned an array to callers that immediately read
`json?.points` / `json?.recommendations` off it and got `undefined` — a *the model answered* path
that silently produces the fallback values. Caught by `providers.test.mjs` on its first run.

### D · ⚠️ A secrecy guard that could never fire — and how it got that way

The two new checks in `AiCredentialSecrecyTest` passed with violations injected. Their regexes
contained a literal **backspace byte (`0x08`)** where `\b` was intended, so they matched nothing.

**The cause is this environment, not a typo.** A `Bash` heredoc here collapses `\\` to `\` before
Python sees it, so `s.replace('\x08', '\\b')` arrives as *replace a backspace with a backspace*.
Self-test: `len(b'\\b')` returns **1**, not 2. It had already produced two literal newlines inside
Kotlin string literals earlier in the session — those failed the compiler loudly; this one failed
**silently and in the flattering direction**, because a regex that matches nothing reports a clean
codebase.

Repaired by building the bytes from codepoints (`bytes([92]) + b'b'`), never from an escape. A
sweep for stray control characters across all 34 files being committed found this one file and no
other. `Inferred:` other `\\` collapses in this session were harmless — the dangerous ones are
exactly the recognised escapes (`\b \n \t \r \f \v \a \0 \x`), which the control-character sweep
covers by construction.

---

## Can the key appear anywhere it should not — checked, and here is what was checked

#54 asks for a **stated, checked answer**, naming what was grepped rather than that someone looked.

**In the source, mechanically, on every future commit** — `AiCredentialSecrecyTest`:

1. `AiCredential.toString()` is overridden, so the `data class` cannot print the key; `"$credential"`
   is asserted safe. The override is a safety property, not tidiness.
2. **No logging call at all** in any of the nine files that hold, read, store or transmit the key.
3. **No whole-credential interpolation** in those files (a property read like
   `${credential.provider.displayName}` is allowed — the first draft fired on it, and a guard that
   fires on the safe case gets relaxed by whoever hits it next).
4. **Only the store, the envelope and the key editor may read `.key` at all.** This is what makes
   narrowing check 3 cost nothing.

**In the committed diff** — all 34 files, tracked *and untracked*, swept for
`sk-…` / `gsk_…` / `AIza…` / `sk-ant-…` shapes: **none**, and every credential-ish literal is
`FAKE-KEY-NOT-A-REAL-KEY-*`.

⚠️ **The first run of that sweep was `git diff HEAD`, which excludes untracked files** — so it
passed vacuously over most of the new code. Re-run over `git status --porcelain` with directories
expanded. A search run at the wrong width does not fail, it *passes*.

**On the device, with a real key stored** — `emulator-5554`, via `run-as`:

- `shared_prefs/goalpilot_ai_credentials.xml` — **pref names *and* values are both ciphertext**
  (AES256-SIV on the names, AES256-GCM on the values). The strings `ai_provider`, `ai_model` and
  `ai_key` do not appear.
- `grep -rl 'FAKE-KEY-NOT-A-REAL' .` across the **whole app data directory** — no hit.
- The whole `logcat` buffer — **0** hits.
- Even the model id `gpt-4o-mini` appears nowhere in plaintext.

**Deleting is a real delete, verified at the filesystem level**: after *Remove*, the file holds
**two** entries — Tink's own keysets, which `EncryptedSharedPreferences` writes itself — and none
of the three credential entries. The fixture key was removed from the emulator before this commit;
the device is clean and still signed in.

---

## Seen

The AI section, the editor, the masked row and the delete were all driven by hand on
`emulator-5554` and read on screen — not inferred from a passing test. Screenshots are in the
session scratchpad (not committed). What the render pass established that the suites could not:
defect **A** and defect **B**, both above.

---

## What is deliberately **not** built

### The deploy — and the client is honest without it

`functions/src/providers.ts` and the ladder in `index.ts` are written, typechecked and unit-tested,
and **not deployed**. A Firebase deploy is an outward action against a live cloud environment, so
it is always-ask in both modes.

**Nothing is broken meanwhile, and that is by construction rather than by luck.** The deployed
pre-`C13` function ignores the credential fields and answers on the project key; the absent-echo
rule reads that as *the free model answered*, which is exactly what happened. A user who sets a key
before the deploy sees a status line that truthfully says the free model is still answering.

### Test-calling the key on entry

#32's *Also settled* decided the key is test-called once when entered, so a typo surfaces in
Settings rather than in the feed. It is **not** in #54's scope list or its exit criteria, and it
needs a fifth callable — so it is not built. §5's `401/403` message still fires, one surface later.

### The `#30` per-feature veto

#32 §4 hands it to [`#30`](https://github.com/idomarhaim/Android_Final_Project/issues/30)
explicitly, so that no second control gets built here. `C13` contributes **one switch and no more**.

---

## What is owed

| Owed | Whose move |
|---|---|
| `firebase deploy --only functions` | **Ido's** — outward action. Until then the free model answers and the status line says so |
| #54 closed | this session, after the commit |
| #48 closed | **now possible** — `C12` #53 landed in `05ec6aa` and this is its other half |

---

## Files

**New (16):** `domain/model/AiProvider.kt` · `AiCredential.kt` · `AiAnswer.kt` · `AiCallEnvelope.kt` ·
`domain/repository/AiProviderRepository.kt` · `data/security/AiCredentialStore.kt` ·
`EncryptedAiCredentialStore.kt` · `DefaultAiProviderRepository.kt` ·
`feature/settings/AiCard.kt` · `AiStatusLine.kt` · `res/xml/backup_rules.xml` ·
`res/xml/data_extraction_rules.xml` · `functions/src/providers.ts` ·
`functions/test/providers.test.mjs` · 4 test suites.

**Modified (15):** `gradle/libs.versions.toml` · `app/build.gradle.kts` · `AndroidManifest.xml` ·
`di/RepositoryModule.kt` · `data/remote/RecommendationRepositoryImpl.kt` ·
`feature/settings/SettingsScreen.kt` · `SettingsViewModel.kt` ·
`feature/dashboard/DashboardScreen.kt` · `res/values/strings.xml` · `res/values-iw/strings.xml` ·
`functions/src/index.ts` · 4 test files.

Two of those are worth naming because they are **copy that my change made false**, not refactors:
`SettingsScreen`'s scope line now names the key as the one thing here that outlives sign-out (§0.4
forbids silence about that), and the dashboard sheet's Settings row enumerated four sections and
now enumerates five — leaving it at four would have made the door to the new section invisible from
the one place it is opened.

`MaterialPickerUiTest.kt` and `MaterialRenderPass.kt` (`c12`'s, released) call `SettingsContent`
and were updated for its four new parameters. Those parameters were deliberately given **no
defaults**: a default would let a real screen forget them and render an AI section that silently
does nothing, which is the one thing that section must not be.

---

## KB — 5 candidates, drained in full

Ingested into the central bundle (`C:/Dev/JARVIS/kb`) in `0045b86`; released there in `e3fa2c5`.
The journal entry `kb/log/2026-08-21.md` names this repo and its candidate file, which is the only
tie that survives a cross-repo drain — the pages and the candidate cannot ride one commit.

- **Ingested:** a Bash heredoc collapses a doubled backslash before Python sees it →
  `kb/dev/escapes-die-in-transit.md` §7
- **Ingested:** a bottom sheet's primary action can open below the fold, and the miss lands on the
  scrim → `kb/dev/sheet-action-below-the-fold.md` *(new page)*
- **Ingested:** a tree sweep that reads one window, and a `git diff` sweep that reads two-thirds of
  the files → `kb/dev/look-at-your-own-output.md` §4k
- **Ingested:** `install -r` + `am instrument`, fifth run — plus the `applicationIdSuffix` on the
  instrumentation component and the build-exit-code gate →
  `kb/dev/android-device-verification.md` §8c

**One of the five was already a page, and the bundle check is the only reason it did not become a
second one.** The candidate proposed a new page for the heredoc collapse;
`escapes-die-in-transit.md` has existed since **2026-08-10**, from **this repo**, on the same
mechanism — a JavaScript template literal that time. What this instance genuinely added is the
worse failure mode: there the corruption hit the *subject*, here it hit the **check**, and a gutted
matcher in a guard reports *success*.

`Check-KbLinks`: **CLEAN** — 100 pages, no broken links, no orphans, no wikilinks.

The candidate file was fully promoted and is therefore deleted rather than rewritten down to
survivors (`rules/derivable-decision.md` §1). It had not yet been committed, so this entry is where
that record lives.

---

# Round 2 — 2026-08-21: deployed, and the ladder verified end to end

> Ido authorised the deploy explicitly (*"why is it yours? if you can, run it"*), which is the gate
> `outward-action-governance.md` puts on it. **The honest correction to r1: it was never a
> capability limit.** `firebase-tools` 15.27.0 is installed and logged in as `name.iddo@gmail.com`
> on `goalpilot-56e30`; r1 could have run it and did not, because AUTO MODE does not extend to
> outward actions. Saying *"yours to run"* without saying *"and I am able to"* understated what was
> being withheld.

## The deploy

`firebase deploy --only functions` → **all five functions updated**, `us-central1`, nodejs22:
`getRecommendations` · `classifyTask` · `scoreTask` · `projectPoints` · `projectChallengeScore`.
The two projection triggers were redeployed unchanged (`projection.ts` was not touched).

⚠️ **It failed on the first attempt, and the failure names nothing useful:**

```
Error: User code failed to load. Cannot determine backend specification. Timeout after 10000.
```

That reads as *your new module is broken*. It is not: `node -e "require('./lib/index.js')"` loaded
the compiled entry point in **202 ms** and listed all five exports. The analyzer's discovery step
had simply exceeded its 10 s budget on this machine. `FUNCTIONS_DISCOVERY_TIMEOUT=120` and the
identical command succeeded. **Check that the module loads before believing the message** — the
diagnosis and the refutation are one command apart.

## What the deploy made testable — and it needs no real API key

The `#54` brief could not verify *key set → provider* without a paid credential. It does not need
one: a **deliberately invalid** key exercises the entire ladder, because rung 1 has to be *reached*
before it can fail.

`Observed:` 2026-08-21, `emulator-5554`, key `gsk_DELIBERATELY_INVALID_…`, cold launch so the
dashboard fires `getRecommendations`. The status row read:

> **Your GROQ key was rejected — GoalPilot's free model answered instead. Check the key, or remove it.**

That one sentence is only reachable if **every** link worked:

| Link | Proven by the sentence |
|---|---|
| client put `provider · model · key` in the payload | the function had a credential to try |
| the deployed adapter called GROQ's real endpoint | a real `401` came back |
| `classifyStatus` mapped it | `dead`, not `quota` or `transient` |
| rung 2 ran | the free model answered, so the feed still populated |
| the function echoed | `answeredBy: "proxy"` + `keyError.class: "dead"` |
| the client rendered §5's table | the *rejected* wording, and only for `dead` |

**And the regression check first:** with **no** key, against the same new deployment, the row read
*"GoalPilot's own free model answers. You have not added a key"* — identical to pre-`C13`.

## Secrecy, re-checked against the live system

- **Device logcat, whole buffer, after the failing call: 0 hits** for the invalid key.
- **The key is gone from the device** — removed through the app's own *Remove*; the encrypted file
  is back to two entries (Tink's own keysets), and a `grep -rl` over the whole app data directory
  finds nothing.
- **No provider error body reaches a logger — `Observed:` in round 4 below.** This bullet said
  `Inferred:` / `Untested:` when r2 shipped, on the grounds that `functions:log` could not be made
  to show the call. That was the wrong instrument, not a dead end; see *Round 4*.

⚠️ **The first version of that last check was reported as three clean zero-counts, and all three
were vacuous** — they were greps over a file containing only the fetch error. Caught by asking for
the denominator (`lines fetched: 15`), which is precisely the §4k habit this session had put in the
KB an hour earlier. Recorded because the same session wrote the rule and then broke it.

## What this closes

`#54`'s held item is gone: the capability is **live**, not merely written. Both `#54` and `#48` are
closed on this round. `#53` remains open on `C12` §4.4's `.tag` collapse, which `#48` never owned.

---

# Round 4 — 2026-08-21: the one `unverified` item is closed, by observing it

r2 shipped with one open item — *no provider error body reaches Google's logs* was `Inferred:` from
the code shape plus a unit test, never watched. Ido asked why it was still open. **The honest answer
is that two failed log fetches were treated as a dead end after one attempt each**, which is not the
same thing as *no way to verify it*.

## Why the log route genuinely does not work here — and why that was never the blocker

`firebase functions:log` is unreliable on this project, and it fails in a way that invites the wrong
conclusion: it returns **15 lines whatever you ask for**, and the slice **moves**. `--only
getRecommendations` gave entries ending `2026-08-20T09:11Z`; the same command with `-n 200` came
back with entries from **`2026-08-06`**, older still. A bare `firebase functions:log` returns
`Error: Failed to list log entries`. `gcloud` is not installed here, so there is no second reader.

**The claim never needed the log.** It is a claim about *what a logger is handed*, and that is
observable directly — run the **deployed artifact** against the real endpoint and serialise the
error with **every own property**, next to the thing that would have leaked, as a control:

```
--- what the provider actually says on a bad key (status 401) ---
{"error":{"message":"Invalid API Key","type":"invalid_request_error","code":"invalid_api_key"}}

--- what callUserProvider throws for the same call ---
{"stack":"ProviderError: groq HTTP 401 (dead)
    at callUserProvider (.../functions/lib/providers.js:301:15)...",
 "message":"groq HTTP 401 (dead)","provider":"groq","status":401,"failureClass":"dead","name":"ProviderError"}

contains the KEY?             : false
contains the provider's BODY? : false
contains "invalid_api_key"?   : false
```

`Observed:` 2026-08-21 — real network call to `api.groq.com`, real `401`. **It is the deployed
artifact and not the source**: the stack trace names `functions/lib/providers.js`, the compiled file
that was uploaded.

## Why this is better evidence than the log would have been

A clean log line proves *this one call logged nothing bad*. This proves **there is nothing bad to
log**: that error object is the only value any logger on the path receives, and it carries four
fields, none of which is the body. `index.ts` narrows it further — `logger.warn` is handed a
hand-built `{provider, status, class}`, not the error at all.

**What still reaches a log, deliberately:** `callGroqJson`'s error, which does interpolate a body —
GoalPilot's **own** key's error text on GoalPilot's own project. A user's key never reaches that
function, and `LadderError` carries the user-side failure as a class and a status while its `cause`
is always the proxy's error. #32's spec line is scoped to user-key calls, and it holds.

## The method, stated so it is reusable

To verify *"X never reaches the log"*: **serialise what the logger actually receives, with all own
properties, and put the real X beside it as a control.** Without the control the check is
unfalsifiable — an empty dump proves nothing if you never established what a leak would look like.

## And the process point, which is the part worth keeping

The item should not have been reported open. Two failed fetches of **one** tool is a fact about that
tool, not about the claim — and a better instrument was available the whole time.
`open because: no way to verify it here` was simply false; the accurate line would have been
`not attempted yet`. The status block's `open because:` field is meant to be the thing that stops
this, and it only works if the reason is the real one.

---

# Round 5 — 2026-08-21: cutting `v0.3.0`, so the build reaches Ido's phone

Ido asked for the app on his phone. No phone is attached to this machine (only
`emulator-5554`), so the route is Firebase App Distribution — which this repo already has,
end to end, from `5316782` on 2026-08-06.

## ⚠️ The release signing key is GONE from this machine — and it does not matter

`app/goalpilot-release.jks` does not exist, `local.properties` carries no `RELEASE_*`, and a
`find` over the whole user profile turns up **no `.jks` at all**. It was created on the machine
that was replaced, and it did not come across. That is exactly the loss `docs/RELEASING.md` §2.1
warns is *"painful to undo"* — every future update must carry the same signature or testers must
uninstall and lose their local state.

**It survives in GitHub Actions secrets.** `gh secret list` shows all six from 2026-08-06,
including `RELEASE_KEYSTORE_BASE64`. So CI can still produce correctly-signed builds even though
no human on this machine can. **The tag route is not merely convenient here, it is the only route**
— a local `assembleRelease` falls back to the *debug* key, and `app/build.gradle.kts` refuses to
distribute that on purpose.

> 📌 **Worth Ido's attention regardless:** the key now exists in exactly **one** place, a GitHub
> secret that cannot be read back. If that repo or that secret goes, no future update can ever
> install over the current one. Backing it up is a decision only he can take, and it is not
> something this session could do for him — the secret is write-only.

## The last release attempt failed, and it was not the code

`v0.2.2`'s run failed after 15 m with *"The job was not acquired by Runner of type hosted even
after multiple attempts"* — a runner-allocation failure, nothing to do with the build. `v0.2.1`
had succeeded in 6 m 49 s. So **`versionCode = 4` was never distributed**, and the newest build
any tester has is `v0.2.1`.

## R8 was the real risk, and it was tested rather than assumed

The release build runs `isMinifyEnabled = true` + `isShrinkResources = true`, and **none of
`C13`'s code had ever been through it**. `EncryptedSharedPreferences` is Tink underneath, which is
reflection- and protobuf-heavy and a classic R8 casualty — and `app/proguard-rules.pro` carries
**no Tink or `androidx.security` keep rules at all**.

That failure would have been invisible in the worst way: `EncryptedAiCredentialStore.openOrNull()`
catches and returns `null`, so a stripped Tink shows up as *"you have not added a key"* — the app
looks fine and the feature is silently dead in exactly the builds real users get.

`Observed:` built `assembleRelease`, installed the minified APK, stored a key, **force-stopped the
process**, relaunched, and the row still read `••••••••5555` with the provider and model intact.
Zero Tink or `GeneralSecurityException` lines in logcat across the whole cycle. `security-crypto`
ships its own consumer ProGuard rules, so no keep rules were needed — **but that is now a
measurement rather than a hope**, and it is the kind of thing that changes silently on a
dependency bump.

The release build also confirmed §4.9's signed-out entry point: Settings opens from the sign-in
screen with no account, the AI section renders, and Account reads *"Not signed in"*.

## What shipped

| | |
|---|---|
| `versionCode` | **4 → 5** (4 was never distributed) |
| `versionName` | `0.2.2` → **`0.3.0`** — this carries #48's whole Settings surface, #53's material contract and #54's AI section |
| `release-notes.txt` | **created** — it did not exist, and `app/build.gradle.kts` names it as `releaseNotesFile`. Written for a tester, not a developer: what changed on screen, and that nothing needs an API key |
| tag | `v0.3.0`, which is what `.github/workflows/release.yml` triggers on |

The local release APK was uninstalled from the emulator afterwards so nothing debug-signed under
the release `applicationId` is left lying around.

---

# Round 6 — 2026-08-21: main's CI had been red for ten hours, and it was not `C13`

Ido forwarded a *"Run failed: Instrumented tests (cloud emulator)"* email for `20f3b7e` — this
session's version bump. **The commit is innocent and so is `C13`.** `gh run list` shows the same
workflow failing on **every** push since **12:57**, across three sessions' commits
(`8-notifications`, `c12-material-contract`, and both of this session's), with the last green run
at **12:03**.

## The cause: two tests, one ungranted permission

172 of 174 pass. The two that do not are both `NotificationObservedFireTest`, failing at
`requirePermission()` — `POST_NOTIFICATIONS` is a **runtime** permission from API 33, the CI
emulator is API 34, and `.github/workflows/instrumented-tests.yml` has **no grant step**.

The suite's own KDoc said why, and it is worth quoting because it is what made the break
invisible to the session that caused it:

> *The permission is granted from **outside**, by `adb shell pm grant` … it must be run through
> `adb shell am instrument` rather than `connectedDebugAndroidTest`.*

That is a **correct description of a human's run and a false one of CI's**, and nothing connects
the two: the suite passes locally forever, because a developer's emulator was granted the
permission once, months ago, and never revoked.

## The fix, and why it is a grant rather than a skip

An `@Before` that calls `uiAutomation.grantRuntimePermission`, guarded on `SDK_INT >= 33`.

**`assumeTrue` was the tempting one-liner and it is the wrong fix.** A skip turns *"nothing was
posted"* into a **green** run, which is the exact failure this suite was written to catch — its
first line is *"a notification you cannot see is a notification you have not built"*. Granting
keeps `requirePermission()`'s assertion untouched: the grant is real, and if it does not take, the
suite still fails as loudly as it did this morning.

The other half of the original note is left standing, because it is still true: a **human**
collecting evidence should use `am instrument`, since `connectedDebugAndroidTest` uninstalls the
app and takes the posted notifications with it. CI never reads the shade, so that concern does not
reach it.

## Verified by reproducing CI's condition, not by hoping

A local run proves nothing here — this machine's emulator has had the permission since `#8` was
built. So it was **revoked first**:

```
adb shell pm revoke com.idomarhaim.goalpilot.debug android.permission.POST_NOTIFICATIONS
```

`Observed:` with the permission revoked, `NotificationObservedFireTest` **6/6**, and the full
suite **174/174**. Before the fix that same state is what CI failed on, four times.

**Scope note:** this is `#8`'s file and `#8` is closed. It is touched anyway because a red `main`
is nobody's ticket and everybody's problem, and because the change is confined to making the
suite's own stated precondition true rather than altering what it asserts.

---

# Round 7 — 2026-08-21: the signing key is recovered, verified and back on the machine

Ido asked for §2.1a's procedure to be *run*, not just written. It was, end to end.

## First, the search r2 got wrong

r2 reported the keystore missing after `find /c/Users/namei …`. **That search never looked at
`C:\Dev`**, which is precisely where Ido said he kept it — the conclusion was right and the
evidence for it was not. Re-run across `C:\Dev` **and** the profile: no `.jks` anywhere but
Android's own `debug.keystore` and some unrelated OneDrive logs. Ido's account is confirmed: it sat
in the Dev folder **outside the repository**, so git never carried it.

That is the second wrong-width search in this session, after `git diff HEAD`. Both passed.

## The workflow had to grow before it was useful

`backup-signing-key.yml` as written at r6 recovered **the `.jks` alone** — and that is not enough
to use it. `signingConfigs` needs the store password, the alias and the key password, and those are
three more unreadable secrets. Recovering the file without them leaves you exactly as stuck, one
step later. It now bundles a ready-to-paste `local.properties` fragment beside the keystore, and
**proves the stored password opens it** before shipping, so a corrupted secret is caught while a
second copy still exists.

## What was actually run

| Step | Result |
|---|---|
| 48-char random passphrase, set as `BACKUP_PASSPHRASE` | never printed, never written to the repo |
| `gh workflow run "Back up the signing key"` | success in **11 s** |
| the job's own report | `Alias name: goalpilot` · valid to **2053** · SHA-1 `E7:D5:53:4C:…:90:62` |
| downloaded, `gpg --decrypt`, untarred | keystore + credentials + `keytool -list -v` output |
| installed to `app/goalpilot-release.jks` + `local.properties` | both confirmed **git-ignored** before anything else |
| `assembleRelease`, then `apksigner verify --print-certs` | `CN=Ido Marhaim, OU=GoalPilot` · SHA-1 `e7d5534c…9062` |
| `BACKUP_PASSPHRASE` deleted, scratchpad wiped | the passphrase was transient by design and never needed saving |

**The APK check is the one that matters.** The whole failure mode here is that `build.gradle.kts`
silently falls back to the **debug** key when credentials are absent, so a build that "succeeds"
proves nothing. This machine's debug SHA-1 is `44:8D:0D:94:…:B3:EC`; the APK's is
`e7d5534c…9062`, matching both the restored keystore and the certificate registered with Firebase.
Three independent sources, one fingerprint.

## Two `keytool` runs printed nothing, and the pipe hid why

`keytool -list …| grep …` produced empty output twice and was very nearly read as *"the keystore is
odd"*. It was `keytool: command not found` — `JAVA_HOME` was exported in **Windows form**
(`C:/Program Files/…`) and prepended to a Git Bash `PATH`, which wants `/c/Program Files/…`.
**`CLAUDE.md` documents this exact trap**, and it still cost two cycles because `grep` swallowed the
error. Gradle was unaffected throughout — it reads `org.gradle.java.home` from `gradle.properties`
— which is what made the failure look selective rather than environmental.

## A false alarm, checked before it was raised

This machine's debug key (`44:8D:…`) is not the one `OPERATIONS.md` records (`F1:D0:96:…`), which
looked like *"fresh Google Sign-In is broken in debug builds here"*. It is not:
`apps:android:sha:list` on the **debug** app id returns both `f1d0964d…` and `448d0d94…`. Someone
already registered this machine's key. Recorded because the near-miss is the point — the
observation was real and the conclusion would have been wrong.

## What is now written down

`docs/RELEASING.md` §2.1a is updated from *"missing"* to *"recovered"*, with the history kept, and
gains **§2.1b — what actually needs backing up**: exactly three files
(`app/goalpilot-release.jks`, `local.properties`, `functions/.env`), because
`app/google-services.json` is tracked and everything else is regenerable. It answers Ido's
backup-repo question directly: **a private repo is fine and secrets go in encrypted**, since git
never forgets a key committed in the clear — and *"this machine plus a GitHub secret"* is not two
places, because losing the laptop is the scenario that already happened.
