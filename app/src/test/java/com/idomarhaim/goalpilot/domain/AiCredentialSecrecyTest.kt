package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiProvider
import org.junit.Test
import java.io.File

/**
 * #54's hard requirements about the one unit in the plan that handles a secret,
 * asserted rather than reviewed.
 *
 * > *A key never reaches a log, a crash report, an analytics event, or a
 * > changelog. Before you commit, `git diff` and look for it with your own
 * > eyes.*
 *
 * Looking with your own eyes is what a person does **once**, on the commit that
 * introduces the feature. This file is what does it on every commit afterwards
 * — the same argument `AnalyticsLiteralSweepTest` makes about the Hebrew sweep:
 * *a sweep is not a state, it is an event*, and the drift is invisible to the
 * compiler, to the English render and to a reviewer who is not looking for it.
 *
 * Two halves, and they catch different things:
 *
 * 1. **[toString] cannot print the key.** A `data class` prints every property,
 *    and `Log.d(TAG, "cred=$credential")` is the single likeliest route from an
 *    encrypted store to a log line. The override makes the unsafe rendering
 *    unreachable rather than forbidden.
 * 2. **No logging statement exists in the files that hold a key.** A file-content
 *    sweep, like the localization guards next door, and for the same reason: the
 *    day somebody adds `Log.e(TAG, "call failed", e)` to
 *    `RecommendationRepositoryImpl` while debugging, everything compiles and
 *    every other test passes.
 */
class AiCredentialSecrecyTest {

    // Obviously fake and clearly marked, per #54's last hard requirement.
    private val fakeKey = "FAKE-KEY-NOT-A-REAL-KEY-1234"
    private val credential = AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey)

    // ── 1 · the key has no safe rendering ──────────────────────────

    @Test
    fun `toString never contains the key`() {
        assertThat(credential.toString()).doesNotContain(fakeKey)
        // Not even the tail-bearing mask should reconstruct it, so check the
        // whole string rather than trusting the override's shape.
        assertThat(credential.toString()).contains("openai")
        assertThat(credential.toString()).contains("gpt-4o-mini")
    }

    @Test
    fun `string interpolation of a credential is safe`() {
        // The actual failure shape: nobody writes `credential.toString()`, they
        // write "$credential" inside a message.
        assertThat("sending $credential").doesNotContain(fakeKey)
    }

    @Test
    fun `the mask shows at most the last four characters`() {
        assertThat(credential.maskedKey).endsWith("1234")
        assertThat(credential.maskedKey).doesNotContain("FAKE")
        assertThat(credential.maskedKey.length).isEqualTo(12)
    }

    @Test
    fun `a very short key is masked entirely`() {
        // On a string of four characters or fewer, "the last four" IS the whole
        // secret. Nothing of it survives.
        val short = AiCredential(AiProvider.GROQ, "", "abcd")
        assertThat(short.maskedKey).doesNotContain("abcd")
        assertThat(short.maskedKey.toSet()).containsExactly('•')
    }

    @Test
    fun `a pasted trailing newline never becomes part of the key`() {
        // A key with one fails as a 401 the user cannot see the cause of, which
        // §5 then classes as "dead, revoked or mistyped" — the worst possible
        // first experience of the feature.
        val c = AiCredential.of(AiProvider.GROQ, " some-model ", "  $fakeKey\n")
        assertThat(c?.key).isEqualTo(fakeKey)
        assertThat(c?.model).isEqualTo("some-model")
    }

    @Test
    fun `an empty key is no credential`() {
        assertThat(AiCredential.of(AiProvider.GROQ, "m", "   ")).isNull()
    }

    // ── 2 · no file that holds a key may log ───────────────────────

    private val sourceRoot = listOf(
        File("src/main/java/com/idomarhaim/goalpilot"),
        File("app/src/main/java/com/idomarhaim/goalpilot"),
    ).firstOrNull { it.isDirectory }
        ?: error("source root not found from ${File(".").absolutePath}")

    /**
     * The files that hold, read, store or transmit the key.
     *
     * A closed list rather than a whole-package sweep, because the property is
     * about **these** call sites: `data/security/` is where it is at rest and
     * `RecommendationRepositoryImpl` is where it goes on the wire. A file added
     * to the key's path and not to this list is missed — which is why the list
     * is short, named, and sits next to the KDoc in each of those files saying
     * the same thing.
     */
    private val keyBearingFiles = listOf(
        // Where it is at rest.
        "data/security/EncryptedAiCredentialStore.kt",
        "data/security/DefaultAiProviderRepository.kt",
        // Where it goes on the wire.
        "data/remote/RecommendationRepositoryImpl.kt",
        "domain/model/AiCredential.kt",
        "domain/model/AiCallEnvelope.kt",
        // Everything that HOLDS one, whether or not it reads `.key`. The three
        // below were missed on the first pass of this list and added in the
        // pre-commit review: none of them touches `.key`, which is exactly the
        // reasoning that leaves them out and exactly why it is wrong — an
        // `AiCredential` in scope is all `"$credential"` needs, and the whole
        // point of the second test here is that the safe rendering is one
        // deleted override away.
        "feature/settings/AiCard.kt",
        "feature/settings/AiStatusLine.kt",
        "feature/settings/SettingsScreen.kt",
        "feature/settings/SettingsViewModel.kt",
    )

    @Test
    fun `no key-bearing file contains a logging statement`() {
        // Matches the CALL, not the word: `android.util.Log`, Timber, and
        // `println`. Comments and KDoc discussing logging are not calls, and
        // several of these files discuss it at length on purpose — so the
        // pattern requires an open parenthesis and the lines are stripped of
        // comments first.
        val logCall = Regex("""\b(Log\.[dviewa]|Timber\.[dviewa]|println|print)\s*\(""")

        for (relative in keyBearingFiles) {
            val file = File(sourceRoot, relative)
            assertWithMessage("$relative is listed as key-bearing but does not exist")
                .that(file.isFile).isTrue()

            val offenders = file.readLines()
                .withIndex()
                .filterNot { (_, line) ->
                    val t = line.trimStart()
                    t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")
                }
                .filter { (_, line) -> logCall.containsMatchIn(line) }
                .map { (i, line) -> "${relative}:${i + 1}  ${line.trim()}" }

            assertWithMessage(
                "#54: a key never reaches a log. These files carry the user's " +
                    "API key and must contain no logging call at all.\n" +
                    offenders.joinToString("\n"),
            ).that(offenders).isEmpty()
        }
    }

    @Test
    fun `no key-bearing file interpolates a whole credential`() {
        // The second shape, and the one `toString()` alone cannot stop: a message
        // built with the credential in it is safe today ONLY because of the
        // override, so catching it here means the property survives someone
        // deleting that override.
        //
        // ⚠️ **The bare object, not any mention of it.** A first draft matched
        // `\$\{?(credential|cred)\b` and fired on
        // `"…If \${credential.provider.displayName} fails…"` — a display name, in
        // a sentence written for the user. A guard that fires on the safe case
        // gets relaxed by whoever hits it next, and the relaxation is what
        // actually costs you. So this matches `$credential` and `${credential}`
        // and lets a property read through; `.key` has its own test below, which
        // is what stops the narrowing from opening a hole.
        val bareObject = Regex(
            """\$\{\s*(credential|cred|aiCredential)\s*}|\$(credential|cred|aiCredential)\b(?![.\w])""",
        )
        assertNoMatch(bareObject) {
            "A whole credential interpolated into a string is one deleted " +
                "toString() override away from a leaked key."
        }
    }

    /**
     * The files allowed to touch `.key` at all.
     *
     * Everything else may hold an [AiCredential] and read its provider, model or
     * mask — that is what the screen is for — but the secret itself has exactly
     * three legitimate readers: the store that writes it, the envelope that puts
     * it on the wire, and the editor that carries a replacement into the save.
     */
    private val mayReadTheKey = setOf(
        "data/security/EncryptedAiCredentialStore.kt",
        "domain/model/AiCallEnvelope.kt",
        "domain/model/AiCredential.kt",
        "feature/settings/AiCard.kt",
    )

    @Test
    fun `only the store the envelope and the editor read the key itself`() {
        // The complement of the test above, and the reason narrowing that regex
        // to the bare object costs nothing: a property read is allowed there
        // precisely because THIS names which property is the secret and keeps
        // every other file away from it.
        val readsKey = Regex("""\.key\b""")
        for (relative in keyBearingFiles) {
            if (relative in mayReadTheKey) continue
            val offenders = codeLines(relative).filter { (_, line) -> readsKey.containsMatchIn(line) }
                .map { (i, line) -> "$relative:${i + 1}  ${line.trim()}" }
            assertWithMessage(
                "#54: only the store, the envelope and the key editor may read " +
                    "AiCredential.key. Everything else uses the provider, the " +
                    "model or the mask.\n" + offenders.joinToString("\n"),
            ).that(offenders).isEmpty()
        }
    }

    /** Source lines with comment lines removed — KDoc here discusses logging on purpose. */
    private fun codeLines(relative: String): List<IndexedValue<String>> =
        File(sourceRoot, relative).readLines()
            .withIndex()
            .filterNot { (_, line) ->
                val t = line.trimStart()
                t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")
            }

    private fun assertNoMatch(pattern: Regex, message: () -> String) {
        for (relative in keyBearingFiles) {
            val offenders = codeLines(relative)
                .filter { (_, line) -> pattern.containsMatchIn(line) }
                .map { (i, line) -> "$relative:${i + 1}  ${line.trim()}" }
            assertWithMessage(message() + "\n" + offenders.joinToString("\n"))
                .that(offenders).isEmpty()
        }
    }

    // ── 3 · the key is excluded from both backup schemes ───────────

    @Test
    fun `the encrypted preferences file is excluded from backup and device transfer`() {
        // #54 requires `android:allowBackup` to be decided DELIBERATELY and
        // stated. The decision is: backup stays on, and this one file is
        // excluded on both schemes — so the assertion is that both rule files
        // name the exact path `EncryptedAiCredentialStore.FILE_NAME` produces.
        // A rename there without a matching edit here silently un-excludes the
        // key, which is a change no build or render would report.
        val resRoot = listOf(File("src/main/res"), File("app/src/main/res"))
            .firstOrNull { it.isDirectory }
            ?: error("res/ not found from ${File(".").absolutePath}")

        val path = "goalpilot_ai_credentials.xml"

        val legacy = File(resRoot, "xml/backup_rules.xml").readText()
        assertWithMessage("API 30 and below: full-backup exclusion")
            .that(legacy).contains("""<exclude domain="sharedpref" path="$path" />""")

        val modern = File(resRoot, "xml/data_extraction_rules.xml").readText()
        // BOTH sections. `device-transfer` is a separate channel with its own
        // opt-out, and excluding only the cloud half would leave the key riding
        // a phone-to-phone copy — the exact behaviour #32 §1 chose against.
        assertThat(modern).contains("<cloud-backup>")
        assertThat(modern).contains("<device-transfer>")
        assertThat(Regex(Regex.escape("""path="$path"""")).findAll(modern).count())
            .isEqualTo(2)

        val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml"))
            .first { it.isFile }.readText()
        assertThat(manifest).contains("""android:fullBackupContent="@xml/backup_rules"""")
        assertThat(manifest).contains("""android:dataExtractionRules="@xml/data_extraction_rules"""")
    }
}
