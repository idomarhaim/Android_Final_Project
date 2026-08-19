package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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
    fun `the enum carries identity only, never display copy`() {
        // Replaces `every skin has picker copy`, which asserted the opposite and
        // was green the whole time the picker was untranslatable (issue #51).
        //
        // `label` and `tagline` were constructor arguments here, and a language
        // switch cannot reach a constructor argument — so the words are now in
        // res/ and resolved by ui/components/ComponentStrings.kt. This asserts
        // the SHAPE rather than the two old names, because the failure mode is
        // somebody adding a third string field, not restoring those two.
        //
        // Java reflection rather than kotlin-reflect: the enum constants and
        // $VALUES are static fields, so the instance fields are exactly the
        // declared constructor properties.
        val instanceFields = AppSkin::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }

        assertWithMessage(
            "AppSkin must carry only its persisted id. A String field here is " +
                "display copy a language switch cannot reach — put it in " +
                "res/values/components_strings.xml and map it in ComponentStrings.kt.",
        ).that(instanceFields).containsExactly("id")
    }
}
