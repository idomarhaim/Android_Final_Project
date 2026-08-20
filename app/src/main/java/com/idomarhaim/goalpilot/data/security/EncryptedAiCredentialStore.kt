package com.idomarhaim.goalpilot.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AiCredentialStore] over Keystore-backed [EncryptedSharedPreferences] — the
 * one place in this app a third-party secret is at rest (#54, #32 §1).
 *
 * ## Its own file, not `AppPreferencesRepositoryImpl`'s
 *
 * The skin, the language and the waking hours live in a plain
 * `SharedPreferences` file, and they should: they are not secrets, and paying
 * Keystore round-trips to read a theme before the first frame would be a
 * regression for nothing. Keeping the key in a **separate encrypted file** also
 * makes the backup exclusion expressible — `res/xml/backup_rules.xml` and
 * `res/xml/data_extraction_rules.xml` exclude one named `sharedpref` path, and
 * they could not exclude a *key inside* a file the rest of the app needs backed
 * up.
 *
 * ## `android:allowBackup` stays `true`, and the key is excluded by name
 *
 * #54 requires the choice to be deliberate and stated, so: **backup remains on
 * for the app, and this file alone is excluded, on both schemes** —
 * `fullBackupContent` for API 30 and below, `dataExtractionRules` for 31+
 * including its `device-transfer` half. `minSdk` here is 26, so both are live.
 *
 * Turning backup off wholesale was the other candidate. It was rejected because
 * it would take the user's theme, language and day schedule with it to protect
 * a file that is excluded either way — a cost paid by settings that are not
 * secrets. **Belt-and-braces rather than the only defence:** the ciphertext is
 * sealed with a Keystore master key that never leaves the device and is itself
 * never backed up, so a restored copy would be undecryptable. That produces a
 * *broken* state rather than a *safe* one, and #32 §1's objection to Firestore
 * was **posture** — a secret at rest in a backed-up store — not reachability.
 * The same objection applies to Google Drive, so the same answer does.
 *
 * ## What happens when the Keystore will not open
 *
 * [EncryptedSharedPreferences.create] can throw on a device whose Keystore
 * entry has been invalidated — a restored backup, a changed lock screen on some
 * OEM builds, a corrupted master key. This class **degrades to the proxy path**
 * rather than crashing or writing anything in the clear: [prefs] is `null`,
 * [read] answers `null`, and the app behaves exactly as it does for a user who
 * never set a key. Losing a key that way costs one re-entry in Settings; the
 * alternatives — crashing at startup, or falling back to plaintext
 * `SharedPreferences` — cost the app or the secret.
 *
 * `Inferred:` the invalidation modes above are the documented ones for
 * Keystore-wrapped keys; `Untested:` none has been reproduced on a device by
 * this session. What *is* exercised is the degradation itself, which the `null`
 * branch guarantees regardless of cause.
 *
 * ## ⚠️ Nothing in this file logs, and that is a requirement
 *
 * #54's first hard rule is that a key never reaches a log, a crash report or an
 * analytics event. This is the one class where an exception object is adjacent
 * to a secret, so it carries no logging statement at all — see [openOrNull].
 */
@Singleton
class EncryptedAiCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : AiCredentialStore {

    /**
     * `null` when the Keystore refused. Lazy so an app whose user never opens
     * the AI section never builds a master key at all — #54's *"should not
     * notice it exists"* is a runtime property too, not only a visual one.
     */
    private val prefs: SharedPreferences? by lazy { openOrNull() }

    override fun read(): AiCredential? {
        val store = prefs ?: return null
        val provider = AiProvider.fromId(store.getString(KEY_PROVIDER, null)) ?: return null
        return AiCredential.of(
            provider = provider,
            model = store.getString(KEY_MODEL, null).orEmpty(),
            key = store.getString(KEY_SECRET, null).orEmpty(),
        )
    }

    override fun write(credential: AiCredential) {
        val store = prefs ?: return
        store.edit()
            .putString(KEY_PROVIDER, credential.provider.id)
            .putString(KEY_MODEL, credential.model)
            .putString(KEY_SECRET, credential.key)
            .apply()
    }

    override fun clear() {
        // commit(), not apply(): a delete the user just asked for should be on
        // disk before this returns. The file holds three entries, so the
        // synchronous write costs a few hundred microseconds — the ANR argument
        // for apply() is about large preference files and does not reach here.
        @Suppress("ApplySharedPref")
        prefs?.edit()?.clear()?.commit()
    }

    private fun openOrNull(): SharedPreferences? = try {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Deliberately swallowed and NOT logged. `e` here can carry the file
        // name and the Keystore alias, and a logging statement in this file is
        // one careless interpolation away from the secret itself. The observable
        // consequence is described in the class KDoc: the AI section reports no
        // key and the proxy answers.
        null
    }

    private companion object {
        /**
         * Becomes `goalpilot_ai_credentials.xml` under `shared_prefs/`, which is
         * the exact path both backup-rule files exclude. **Changing this string
         * silently un-excludes the key from backup** — change both rule files in
         * the same edit.
         */
        const val FILE_NAME = "goalpilot_ai_credentials"

        const val KEY_PROVIDER = "ai_provider"
        const val KEY_MODEL = "ai_model"
        const val KEY_SECRET = "ai_key"
    }
}
