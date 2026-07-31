package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AppSkin
import org.junit.Test

class AppSkinTest {

    @Test
    fun `default skin is Aurora`() {
        assertThat(AppSkin.DEFAULT).isEqualTo(AppSkin.AURORA)
    }

    @Test
    fun `every skin round-trips through its persisted id`() {
        AppSkin.entries.forEach { skin ->
            assertThat(AppSkin.fromId(skin.id)).isEqualTo(skin)
        }
    }

    @Test
    fun `ids are unique and stable-looking`() {
        val ids = AppSkin.entries.map { it.id }
        assertThat(ids).containsNoDuplicates()
        // Persisted verbatim in SharedPreferences, so they must not be display copy.
        ids.forEach { assertThat(it).matches("[a-z][a-z0-9_]*") }
    }

    @Test
    fun `unknown or missing id falls back to the default`() {
        assertThat(AppSkin.fromId(null)).isEqualTo(AppSkin.DEFAULT)
        assertThat(AppSkin.fromId("")).isEqualTo(AppSkin.DEFAULT)
        assertThat(AppSkin.fromId("midnight")).isEqualTo(AppSkin.DEFAULT)
    }

    @Test
    fun `id lookup is case-insensitive`() {
        assertThat(AppSkin.fromId("BLOSSOM")).isEqualTo(AppSkin.BLOSSOM)
        assertThat(AppSkin.fromId("Aurora")).isEqualTo(AppSkin.AURORA)
    }

    @Test
    fun `every skin has picker copy`() {
        AppSkin.entries.forEach { skin ->
            assertThat(skin.label).isNotEmpty()
            assertThat(skin.tagline).isNotEmpty()
        }
    }
}
