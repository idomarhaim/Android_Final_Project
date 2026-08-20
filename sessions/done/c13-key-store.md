---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 54
created: 2026-08-20
result: shipped 2026-08-20 in `c4a700c` (claim `5d84251`) — #54 left OPEN on the deploy, see below
---

# `C13` build half — the encrypted key store, so #48's AI section can exist

**The second hole in [#48](https://github.com/idomarhaim/Android_Final_Project/issues/48).**
Independent of `C20`. Needs the **Gradle daemon**; a device for the render pass.

> ⚠️ **Runs AFTER `c12-material-contract`, and never beside it. Verified 2026-08-20.** Both edit
> `feature/settings/SettingsScreen.kt` (`:103` here, `AppearanceCard` at `:275` there). `c12` first,
> because the material contract decides how this section renders.

## The issue is filed

[**#54**](https://github.com/idomarhaim/Android_Final_Project/issues/54) *(2026-08-20)*, and
[#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) — the closed `C13` decision —
now carries a comment pointing at it.

## Why it exists

`48-settings-surface` built four of five §4.9 sections and left the AI section out on purpose.
Its words, `feature/settings/SettingsScreen.kt:86`:

> its three controls are `C13`'s — an `EncryptedSharedPreferences` key store
> (`androidx.security:security-crypto`, **not a dependency here**), a provider abstraction, and a
> status line naming which provider answered. **Every model call goes through the Cloud Function
> proxy today and the client holds no key at all.**

That last sentence is the important one: **there is no key handling to retrofit.** You are adding a
capability, not migrating one.

## What ships — three pieces

1. **The key store.** `androidx.security:security-crypto` → `EncryptedSharedPreferences`. Add the
   dependency to `gradle/libs.versions.toml` and `app/build.gradle.kts` the way every other one is
   declared there.
2. **A provider abstraction** — so a bring-your-own key routes somewhere, and the Cloud Function
   proxy stays the default. **The proxy must remain the default and must keep working with no key
   set**; a user who never opens this section should not notice it exists.
3. **A status line naming which provider answered.** This is the honesty half — without it the app
   has two paths and the user cannot tell which one ran, which is §0.3's *second number that quietly
   disagrees* wearing a different hat.

Then the AI section's three controls in `SettingsScreen.kt`, which already has the surface.

## ⚠️ This is the one unit in the plan that handles a secret

Everything below is a hard requirement, not a checklist to tick:

- **A key never reaches a log, a crash report, an analytics event, or a changelog.** Before you
  commit, `git diff` and look for it with your own eyes.
- **The key never leaves the device.** It is not synced to Firestore, not put in a DTO, not backed
  up. Set `android:allowBackup` deliberately and say what you chose.
- **The UI shows a masked value and a *replace* action, never the stored key.** "Reveal" is a
  feature request, not a default.
- **Deleting a key is a real delete**, and the app returns to the proxy path cleanly.
- Any test fixture key is obviously fake and clearly marked.

If a design decision here trades security for convenience, **stop and ask Ido** — that is not a
derivable decision.

## The relationship to `C12`

They are both #48's remainders, and **#48 does not close until both have landed.**

> ⛔ **CORRECTED 2026-08-20 — this section said *"None technically — different files, different
> concerns … can run in either order or in parallel."* That is FALSE, and it is false in the
> flattering direction.**
>
> **They collide on `feature/settings/SettingsScreen.kt`.** `c12-material-contract` claims that
> exact path on the board; this brief's own *What ships* section ends *"then the AI section's three
> controls in `SettingsScreen.kt`"*. Both write the same file.
>
> Two further reasons, either of which alone would settle it:
> - This unit edits **`gradle/libs.versions.toml` and `app/build.gradle.kts`** to add
>   `androidx.security:security-crypto`. A dependency change invalidates the build for **any**
>   concurrent session.
> - Both need the **Gradle daemon** and a **device**, which are singletons on this board.
>
> **Either order is still fine. In parallel is not.** The original claim was made at the wrong
> granularity — *different concerns* is not *different files* — which is the same failure
> `9-duration-box` and `11-fill-buttons` hit on 2026-08-20, costing a whole round that built
> nothing. General finding: `kb/dev/agent-topology-and-routing.md` § *The disjointness check has to
> be run at the granularity the claim is made in*.

## Exit

- JVM unit for the provider selection: no key → proxy; key set → provider; key deleted → proxy
  again. **All three directions**, because the one you skip is the one that breaks.
- Instrumented for the masked field and the replace action.
- **A stated, checked answer to "can the key appear anywhere it should not"** — name what you
  grepped, not just that you looked.
- **Seen** on a device.
- `CHANGELOG/<today>/c13-key-store.md` · board row released · brief closed to `sessions/done/` with
  `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading. If `c12-material-contract` has also landed, say that **#48 is now
closable** and name posting the close comment as the next step.

---

## Result — 2026-08-20 (finished after midnight on 08-21)

**Shipped in `c4a700c`.** All three pieces plus §4.9's AI section and its three controls.
Full account: [`CHANGELOG/2026-08-20/c13-key-store.md`](../CHANGELOG/2026-08-20/c13-key-store.md).

**Tests, every layer this project has:** functions **56/56** · JVM unit **609/609** (48 new,
0 skipped) · instrumented **174/174** (13 new, API 35) · `firestore-tests/` not run, and `C13`
does not reach it — nothing it stores goes to Firestore at all (#32 §1 rejected exactly that).

**Four defects found by the checks rather than by reading, all fixed** — an off-screen Save button
that discarded the typed key on a scrim tap, a status line claiming the free model was chosen over
a key that did not yet exist, a JSON array passing an object check, and a secrecy guard whose regex
had been corrupted into a control character so it could never fire. Each new guard now has a
recorded negative control.

### ⛔ `#54` is left OPEN, and the held item is named

**`firebase deploy --only functions` has not been run.** It is an outward action against a live
cloud environment, so it is always-ask in both modes and it is Ido's to run. Until it does, a user
who sets a key has that key travel to a function that ignores it — so *bring-your-own key* is
written and tested but **not live**, which is more than a formality and is why the ticket stays
open rather than closing with a footnote.

**Nothing is broken meanwhile, by construction rather than by luck.** The pre-`C13` deployment
answers on the project key, and `AiCallEnvelope.answeredBy` reads an absent echo as *the free model
answered* — which is the truth. `Observed:` on `emulator-5554`, with a key stored, the status row
read *"GoalPilot's free model answered, not your OpenAI key"*.

### Also not built, deliberately

**#32's *test-call the key once on entry*.** It is in that ticket's *Also settled*, and it is in
neither `#54`'s scope list nor its exit criteria — it needs a fifth callable. §5's `401/403`
message still fires, one surface later.

### `#48`

Both remainders have now landed as code: `C12` #53's material contract in `05ec6aa`, and this.
`#53` stays open on `C12` §4.4's `.tag` collapse, which is a different §4.1 item that `#48` never
owned — so it does not hold `#48`.
