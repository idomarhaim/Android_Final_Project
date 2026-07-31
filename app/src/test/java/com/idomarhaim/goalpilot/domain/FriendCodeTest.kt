package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FriendCode
import org.junit.Test
import kotlin.random.Random

/** Domain (unit) tests for the short friend code used to add friends (spec §7). */
class FriendCodeTest {

    @Test
    fun `generate produces a valid code of the expected length`() {
        repeat(200) {
            val code = FriendCode.generate()
            assertThat(code).hasLength(FriendCode.LENGTH)
            assertThat(FriendCode.isValid(code)).isTrue()
        }
    }

    @Test
    fun `generated codes never contain visually ambiguous characters`() {
        val ambiguous = setOf('I', 'O', '0', '1')
        repeat(200) {
            assertThat(FriendCode.generate().none { it in ambiguous }).isTrue()
        }
    }

    @Test
    fun `generate is deterministic for a seeded random`() {
        assertThat(FriendCode.generate(Random(42))).isEqualTo(FriendCode.generate(Random(42)))
    }

    @Test
    fun `normalize uppercases and strips separators`() {
        assertThat(FriendCode.normalize("  7kq 4-rd ")).isEqualTo("7KQ4RD")
    }

    @Test
    fun `normalize drops characters outside the alphabet`() {
        // O/0 and I/1 are not in the alphabet, so they are discarded rather than
        // silently matched against a different code.
        assertThat(FriendCode.normalize("A0B1C!")).isEqualTo("ABC")
    }

    @Test
    fun `isValid rejects wrong lengths and stray characters`() {
        assertThat(FriendCode.isValid("7KQ4R")).isFalse()
        assertThat(FriendCode.isValid("7KQ4RDX")).isFalse()
        assertThat(FriendCode.isValid("7kq4rd")).isFalse()
        assertThat(FriendCode.isValid("7KQ4R0")).isFalse()
        assertThat(FriendCode.isValid("")).isFalse()
    }
}
