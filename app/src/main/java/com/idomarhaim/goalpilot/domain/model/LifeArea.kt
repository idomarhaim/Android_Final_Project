package com.idomarhaim.goalpilot.domain.model

/**
 * A user-defined **area of life** — "Health", "Studies", "Career",
 * "Relationships" — that goals are filed under (spec §1: goals belong to life
 * areas).
 *
 * This is deliberately *not* [GoalCategory]. The category is a fixed taxonomy the
 * LLM classifies against and the palette is built around; a life area is whatever
 * the user says their life is divided into, and it is the unit the time-allocation
 * chart reports on. A goal carries both: `category` for colour/icon/LLM context,
 * `lifeAreaId` for "which part of my life is this".
 *
 * [googleListId] is set when the area was synced from a Google Tasks list, which
 * is what lets a re-sync recognise its own earlier writes instead of creating a
 * second "בריאות". It is the same identity problem the Google Tasks *task* import
 * has to solve by title — here we have a real handle, so we use it.
 */
data class LifeArea(
    val id: String = "",
    val name: String = "",
    val colorHex: String = LifeAreaPalette.DEFAULT_HEX,
    val iconKey: String = "flag",
    /** Manual ordering; ties fall back to name so the list never jitters. */
    val sortOrder: Int = 0,
    /** Google Tasks list this area mirrors, or null for a hand-made area. */
    val googleListId: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
) {
    val isLinkedToGoogleTasks: Boolean get() = !googleListId.isNullOrBlank()
}

/**
 * Colours and icons for life areas.
 *
 * The hexes are the same categorical set the [GoalCategory] palette uses — ten
 * hues spread around the wheel, each clearing 4.5:1 on a light surface — because
 * they end up side by side as slices of one pie, where two neighbouring greens
 * are indistinguishable. `String.toGoalAccent()` lifts them for dark surfaces.
 */
object LifeAreaPalette {

    val hexes: List<String> = listOf(
        "#CF3636", // red
        "#0F6FCB", // blue
        "#26804A", // green
        "#B85107", // amber
        "#8B39C4", // violet
        "#D6246E", // pink
        "#0B7285", // teal
        "#3B3BA8", // indigo
        "#7C4A21", // brown
        "#64748B", // slate
    )

    const val DEFAULT_HEX = "#64748B"

    /** Icon keys offered by the editor; resolved to Material icons in the UI layer. */
    val iconKeys: List<String> = listOf(
        "favorite", "fitness", "sleep", "nutrition", "people",
        "work", "project", "school", "finance", "home", "spa", "flag",
    )

    /**
     * The palette entry least used by [existing] areas, preferring one that is
     * unused entirely. Sync creates several areas at once, so "just take the next
     * colour" would hand two lists the same hue as soon as the user has more than
     * ten areas — this degrades to *evenly* reused instead.
     */
    fun nextHex(existing: List<String>): String {
        val used = existing.map { it.uppercase() }
        return hexes.minByOrNull { hex -> used.count { it == hex.uppercase() } } ?: DEFAULT_HEX
    }

    /**
     * Guesses an icon from an area's name.
     *
     * Bilingual on purpose: the Google Tasks lists this feature syncs from are
     * the user's own, and in this project they are written in Hebrew (בריאות,
     * לימודים, קריירה, זוגיות). Matching only English keywords would give every
     * synced area the same generic flag, which is exactly the "it didn't
     * understand me" feeling the sync is meant to avoid. Falls back to "flag",
     * which is never wrong, only unhelpful.
     */
    fun iconKeyFor(name: String): String {
        val n = name.trim().lowercase()
        if (n.isEmpty()) return "flag"
        return KEYWORDS.firstOrNull { (words, _) -> words.any { n.contains(it) } }?.second ?: "flag"
    }

    /**
     * Ordered because the first match wins: "בריאות" (health) must be tested
     * before the broader body/fitness words, and "לימודים" before "work".
     */
    private val KEYWORDS: List<Pair<List<String>, String>> = listOf(
        listOf("health", "בריאות", "רפואה") to "favorite",
        listOf("sleep", "rest", "שינה", "מנוחה") to "sleep",
        listOf("nutrition", "food", "diet", "eat", "תזונה", "אוכל", "דיאטה") to "nutrition",
        listOf("fitness", "gym", "sport", "run", "train", "כושר", "ספורט", "ריצה", "אימון") to "fitness",
        listOf(
            "relationship", "family", "friends", "partner", "couple", "social",
            "זוגיות", "משפחה", "חברים", "חברתי",
        ) to "people",
        listOf("career", "work", "job", "business", "קריירה", "עבודה", "עסק") to "work",
        listOf("study", "studies", "learn", "school", "course", "לימודים", "לימוד", "קורס", "אקדמיה") to "school",
        listOf("project", "side", "build", "פרויקט", "פרוייקט", "פרויקטים") to "project",
        listOf("finance", "money", "budget", "saving", "כספים", "כסף", "תקציב", "חיסכון") to "finance",
        listOf("home", "house", "chores", "בית", "משק בית") to "home",
        listOf("spirit", "mind", "calm", "meditat", "רוח", "נפש", "מדיטציה", "רוחניות") to "spa",
    )
}
