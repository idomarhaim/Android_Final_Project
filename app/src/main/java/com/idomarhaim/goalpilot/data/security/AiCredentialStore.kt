package com.idomarhaim.goalpilot.data.security

import com.idomarhaim.goalpilot.domain.model.AiCredential

/**
 * Where the one third-party secret in this app is at rest — three methods, and
 * nothing else (#54, #32 §1).
 *
 * ## Why the port exists
 *
 * The real implementation is [EncryptedAiCredentialStore], which needs a
 * `Context`, an Android Keystore and a device. Splitting it out leaves
 * [DefaultAiProviderRepository] — where every decision `C13` actually makes
 * lives: the ladder, §5's dead-key latch, and the routing the whole ticket
 * turns on — as plain Kotlin that a JVM test can drive.
 *
 * That matters for one specific reason. #54's exit criterion is *"provider
 * selection tested in **all three directions**: no key → proxy; key set →
 * provider; key deleted → proxy again"*, and the alternative to this seam is a
 * hand-written fake repository — which would mean the test asserts against a
 * second implementation of the latch rather than the one that ships. Here the
 * only thing substituted is a map.
 *
 * It is deliberately **not** a general "secure storage" abstraction. One
 * credential, one file, no keys-by-name: a wider interface would invite a
 * second secret into a store whose backup exclusion names exactly one path.
 */
interface AiCredentialStore {

    /** The stored credential, or `null` when none is set — the default state. */
    fun read(): AiCredential?

    /** Overwrites whatever is there. */
    fun write(credential: AiCredential)

    /**
     * A **real delete** (#54): the bytes go, they are not blanked. Implementations
     * write synchronously — a delete the user just asked for should be on disk
     * before this returns.
     */
    fun clear()
}
