package com.idomarhaim.goalpilot.ui.components

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.R
import java.io.File
import org.junit.Test

/** The company credit (`C:\Dev\JARVIS\rules\company-branding.md`): form and variant choice. */
class MakerCreditTest {

    @Test
    fun `credit reads exactly Made By, names the company, and is never translated`() {
        val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val english = File(res, "values/components_strings.xml").readText()
        assertThat(english).contains(
            """<string name="components_maker_credit_label" translatable="false">Made By:</string>""",
        )
        assertThat(english).contains(
            """<string name="components_maker_credit_description" translatable="false">Ido Mar-Chaim (IMC)</string>""",
        )
        assertThat(File(res, "values-iw/components_strings.xml").readText())
            .doesNotContain("components_maker_credit")
    }

    @Test
    fun `light surfaces get the navy wordmark`() {
        assertThat(imcLogoFor(Color.White)).isEqualTo(R.drawable.imc_logo_light)
        assertThat(imcLogoFor(Color(0xFFF4F6FA))).isEqualTo(R.drawable.imc_logo_light)
    }

    @Test
    fun `dark surfaces and saturated brand gradients get the light wordmark`() {
        assertThat(imcLogoFor(Color(0xFF101418))).isEqualTo(R.drawable.imc_logo_dark)
        assertThat(imcLogoFor(Color(0xFF3949AB))).isEqualTo(R.drawable.imc_logo_dark)
    }
}
