package com.idomarhaim.goalpilot.resources

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import org.junit.Test
import java.io.File

/**
 * Ties §4.1's vocabulary to the picker's — issue `#53`, the 2026-08-21 comment.
 *
 * ### The defect this exists to prevent recurring
 *
 * §4.1 calls the four materials *glassmorphism · liquid glass · neo · dark neo*.
 * The picker calls two of them **Soft** and **Soft dark**, and until this unit
 * the word "neo" appeared **nowhere in the UI**. Two vocabularies with nothing
 * linking them: a user who had read the spec could not find the control, and a
 * session receiving *"no dark blue neo"* could not match the report to a tile.
 * Both halves happened on `#53` itself.
 *
 * The fix is a **join in two directions** — a mapping table in §4.1, and a
 * caption line on every tile. A join is exactly the kind of thing that is
 * correct the day it is written and rots silently afterwards, because **nothing
 * fails when it drifts**: the doc still renders, the picker still renders, and
 * the two simply stop agreeing. So the table is not documentation of the
 * strings, it is the **input** to this test.
 *
 * ### Why it reads the document rather than restating it
 *
 * A test carrying its own copy of the four names would pass forever while §4.1
 * said something else — it would guard the strings against *itself*, which is
 * the shape of a guard that measures nothing. Parsing the real table is what
 * makes an edit to either side fail here.
 *
 * ### What this cannot see
 *
 * That the caption is actually **rendered**. `MaterialPickerUiTest` asserts that
 * on a device, off the tile's own semantics; a resource can be perfect and never
 * reach a composable.
 */
class MaterialVocabularyTest {

    private val repoRoot: File = listOf(File(".."), File("."))
        .firstOrNull { File(it, "docs/PRODUCT_v0.3.md").isFile }
        ?: error("PRODUCT_v0.3.md not found from ${File(".").absolutePath}")

    private val spec: String = File(repoRoot, "docs/PRODUCT_v0.3.md").readText()

    private val componentsXml: String =
        File(repoRoot, "app/src/main/res/values/components_strings.xml").readText()

    private val strings: Map<String, String> = STRING
        .findAll(componentsXml)
        .associate { it.groupValues[1] to it.groupValues[2].trim() }

    private val stringDeclarations: List<String> =
        DECLARATION.findAll(componentsXml).map { it.value }.toList()

    private val componentStrings: String = File(
        repoRoot,
        "app/src/main/java/com/idomarhaim/goalpilot/ui/components/ComponentStrings.kt",
    ).readText()

    /** One parsed row of §4.1's mapping table. */
    private data class Row(
        val specName: String,
        val enumName: String,
        val pickerLabel: String,
        val specLine: String,
    )

    private val rows: List<Row> by lazy {
        val lines = spec.lines()
        val header = lines.indexOfFirst { it.startsWith("| This section calls it |") }
        assertWithMessage(
            "§4.1's material-vocabulary table is gone. It is the input to this test, not a " +
                "restatement of it — see docs/PRODUCT_v0.3.md §4.1 and issue #53.",
        ).that(header).isAtLeast(0)

        lines.drop(header + 2)
            .takeWhile { it.startsWith("|") }
            .map { line ->
                val cells = line.trim().trim('|').split("|").map { it.plain() }
                assertWithMessage("a table row must carry four cells: $line").that(cells).hasSize(4)
                Row(cells[0], cells[1], cells[2], cells[3])
            }
    }

    @Test
    fun `the table names every material and no others`() {
        assertWithMessage(
            "§4.1's mapping table must have exactly one row per AppMaterial — a material " +
                "missing from it is one nobody can report by name, which is the defect.",
        ).that(rows.map { it.enumName })
            .containsExactlyElementsIn(AppMaterial.entries.map { it.name })
    }

    @Test
    fun `each tile's label is the word the table says it is`() {
        rows.forEach { row ->
            val material = AppMaterial.valueOf(row.enumName)
            assertWithMessage("${row.enumName}: §4.1 says the picker's label is this")
                .that(strings["components_material_${material.id}"])
                .isEqualTo(row.pickerLabel)
        }
    }

    @Test
    fun `each tile's spec line is the name the table says it is`() {
        val frame = strings.getValue("components_material_spec_name")
        rows.forEach { row ->
            val material = AppMaterial.valueOf(row.enumName)
            val name = strings["components_material_${material.id}_spec"]
            assertWithMessage("${row.enumName}: the spec name resource")
                .that(name).isEqualTo(row.specName)
            assertWithMessage("${row.enumName}: the caption a tile actually renders")
                .that(frame.replace("%1\$s", name.orEmpty()))
                .isEqualTo(row.specLine)
        }
    }

    @Test
    fun `ComponentStrings wires each material to its own spec name`() {
        // The one hop the two checks above cannot see: the resources can be
        // perfect and the `when` can hand dark neo neo's key. A swapped pair
        // renders four plausible captions and no test notices.
        AppMaterial.entries.forEach { material ->
            val expected =
                "AppMaterial.${material.name} -> R.string.components_material_${material.id}_spec"
            assertWithMessage("ComponentStrings.specNameRes must contain: $expected")
                .that(componentStrings).contains(expected)
        }
    }

    @Test
    fun `the four spec names are marked untranslatable and the frame is not`() {
        // The decision, guarded rather than only documented. A spec name's job is
        // to be the SAME TOKEN as the design of record, which is English — a
        // Hebrew rendering would name the control after a word appearing in no
        // document, which is this ticket's failure translated rather than a
        // translation of the fix. Losing the attribute would make
        // HebrewLocaleResourceTest demand exactly that.
        AppMaterial.entries.forEach { material ->
            val key = "components_material_${material.id}_spec"
            val declaration = stringDeclarations.first { it.contains("name=\"$key\"") }
            assertWithMessage("$key must stay translatable=\"false\"")
                .that(declaration).contains("translatable=\"false\"")
        }
        val frame =
            stringDeclarations.first { it.contains("name=\"components_material_spec_name\"") }
        assertWithMessage(
            "the frame around the name IS language-dependent and owes a values-iw counterpart",
        ).that(frame).doesNotContain("translatable=\"false\"")
    }

    @Test
    fun `the caption is wired into the picker`() {
        // Weak on purpose, and stated as weak: this is a source grep, not a
        // frame. It catches the caption being deleted from MaterialPicker while
        // every resource assertion above stays green — the exact way a join
        // dies. Seeing it is MaterialPickerUiTest's job.
        val picker = File(
            repoRoot,
            "app/src/main/java/com/idomarhaim/goalpilot/ui/components/MaterialPicker.kt",
        ).readText()
        assertThat(picker).contains("material.specName()")
        assertThat(picker).contains("materialSpecTag(material)")
    }

    private companion object {
        val STRING = Regex(
            """<string\b[^>]*?name="([^"]+)"[^>]*>(.*?)</string>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val DECLARATION = Regex("""<string(?![-\w])[^>]*>""")

        /** A table cell without its markdown emphasis, code ticks or padding. */
        fun String.plain(): String = trim().trim('*', '`', ' ').trim()
    }
}
