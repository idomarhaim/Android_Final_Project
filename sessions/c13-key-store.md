---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 54
created: 2026-08-20
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

None technically — different files, different concerns. They are both #48's remainders and can run
in **either order or in parallel**. #48 does not close until both have.

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
