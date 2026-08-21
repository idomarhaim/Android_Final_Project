package com.idomarhaim.goalpilot.feature.settings

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.model.displayName
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.LanguagePicker
import com.idomarhaim.goalpilot.ui.components.MaterialPicker
import com.idomarhaim.goalpilot.ui.components.label
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.components.SkinPicker
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet
import com.idomarhaim.goalpilot.ui.locale.AppTimePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Spec §4.9's Settings surface.
 *
 * > **Profile is the account, Settings is the device, and sign-out is the
 * > test.**
 *
 * ## What is here
 *
 * §4.9 lists five sections and **all five are built**: Appearance, Language &
 * region, Your day, AI, Account.
 *
 * ⚠️ **Two of them were entries in a *missing* table here until 2026-08-20**,
 * and the table is deleted rather than hedged, because both of its reasons have
 * been removed by the tickets that owned them:
 *
 * | Was missing | Its stated reason | Closed by |
 * |---|---|---|
 * | the whole **AI** section | *"its three controls are `C13`'s — an `EncryptedSharedPreferences` key store (`androidx.security:security-crypto`, not a dependency here), a provider abstraction, and a status line naming which provider answered"* | `C13` [#54](https://github.com/idomarhaim/Android_Final_Project/issues/54) — all three exist; see [AiCard] |
 * | Appearance's **material tiles** | *"§4.1's four-material contract does not exist in this codebase"* | `C12` [#53](https://github.com/idomarhaim/Android_Final_Project/issues/53) |
 *
 * Both were right when written, and both were recorded in the changelog rather
 * than shown as disabled rows — §4.9 rules that **a lock is a word, never a
 * dimming**, and the honest word for a section whose subsystem does not exist
 * belongs in the backlog and not on the user's screen. That principle survives
 * the table it produced: [AiCard] applies it *inside* the AI section, where an
 * install with no key gets one action rather than three controls that would
 * change nothing.
 *
 * ⚠️ **The material tiles' own note, kept because it says what `C12` changed:**
 * they were the second entry in that table until
 * `C12` #53 — *"§4.1's four-material contract does not exist in this
 * codebase — no `AppMaterial`, no palette transform, and no open issue
 * scheduling one. A picker over materials nothing renders is a control that
 * changes nothing."* That was the right call and #53 removed its reason:
 * `AppMaterial`, the palette transforms and the `GpMaterialSpec` contract all
 * exist now, and the tiles below are a picker over materials the whole app
 * renders.
 *
 * ## Why it takes no `AuthRepository`
 *
 * It is reachable **from the sign-in screen with no account at all**, which is
 * §4.9's proof of the split. See [SettingsViewModel].
 *
 * @param onOpenProfile `null` when there is no account — the Account section
 *   then states the boundary without offering a door to a screen this user has
 *   no way into.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenProfile: (() -> Unit)?,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val skin by viewModel.skin.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val material by viewModel.material.collectAsStateWithLifecycle()
    val background by viewModel.background.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val region by viewModel.region.collectAsStateWithLifecycle()
    val schedule by viewModel.daySchedule.collectAsStateWithLifecycle()
    val aiCredential by viewModel.aiCredential.collectAsStateWithLifecycle()
    val aiLastAnswer by viewModel.aiLastAnswer.collectAsStateWithLifecycle()

    SettingsContent(
        skin = skin,
        onSkin = viewModel::setSkin,
        brightness = brightness,
        onBrightness = viewModel::setBrightness,
        material = material,
        onMaterial = viewModel::setMaterial,
        background = background,
        onBackground = viewModel::setBackground,
        language = language,
        onLanguage = viewModel::setLanguage,
        region = region,
        onRegion = viewModel::setRegion,
        schedule = schedule,
        onWakingHours = viewModel::setWakingHours,
        onPlanningOverrideMinutes = viewModel::setPlanningOverrideMinutes,
        aiCredential = aiCredential,
        aiLastAnswer = aiLastAnswer,
        onAiCredential = viewModel::setAiCredential,
        onClearAiCredential = viewModel::clearAiCredential,
        onBack = onBack,
        onOpenProfile = onOpenProfile,
    )
}

/**
 * The screen with its state hoisted out — every value in, every edit out.
 *
 * Split from [SettingsScreen] so the whole surface can be driven from a
 * `createComposeRule()` test with no Hilt graph, no Firebase and no
 * `AppPreferencesRepository`. That matters more here than on most screens:
 * §4.9's consequence lines are **arithmetic rendered as a sentence**, and the
 * only way to catch one that silently stops moving with its setting is to move
 * the setting and read the sentence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    skin: AppSkin,
    onSkin: (AppSkin) -> Unit,
    brightness: AppBrightness,
    onBrightness: (AppBrightness) -> Unit,
    material: AppMaterial,
    onMaterial: (AppMaterial) -> Unit,
    background: AppBackground,
    onBackground: (AppBackground) -> Unit,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    region: AppRegion,
    onRegion: (AppRegion) -> Unit,
    schedule: DaySchedule,
    onWakingHours: (WakingHours) -> Unit,
    onPlanningOverrideMinutes: (Int?) -> Unit,
    aiCredential: AiCredential?,
    aiLastAnswer: AiAnswer?,
    onAiCredential: (AiCredential) -> Unit,
    onClearAiCredential: () -> Unit,
    onBack: () -> Unit,
    onOpenProfile: (() -> Unit)?,
) {
    // The *device* locale, read off the framework's own configuration rather
    // than LocalContext's: AppLocale overrides the composition's context with
    // the app's language, so reading it there would make "follow the device"
    // report whatever the app was last set to.
    val deviceLocale = remember { Resources.getSystem().configuration.locales[0] }
    val formattingLocale = region.formattingLocale(language, deviceLocale)

    var regionSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<TimeField?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScopeLine()

            SectionHeader(stringResource(R.string.settings_appearance_title))
            AppearanceCard(
                material = material,
                onMaterial = onMaterial,
                background = background,
                onBackground = onBackground,
                brightness = brightness,
                onBrightness = onBrightness,
                skin = skin,
                onSkin = onSkin,
            )

            SectionHeader("Language & region")
            LanguageRegionCard(
                language = language,
                onLanguage = onLanguage,
                region = region,
                deviceLocale = deviceLocale,
                formattingLocale = formattingLocale,
                onOpenRegionPicker = { regionSheetOpen = true },
            )

            SectionHeader("Your day")
            YourDayCard(
                schedule = schedule,
                formattingLocale = formattingLocale,
                onEdit = { editingTime = it },
                onFollowWakingHours = { onPlanningOverrideMinutes(null) },
            )

            SectionHeader(stringResource(R.string.settings_ai_title))
            AiCard(
                credential = aiCredential,
                lastAnswer = aiLastAnswer,
                onSave = onAiCredential,
                onClear = onClearAiCredential,
            )

            SectionHeader("Account")
            AccountCard(onOpenProfile = onOpenProfile)

            Spacer(Modifier.height(24.dp))
        }
    }

    if (regionSheetOpen) {
        RegionSheet(
            selected = region,
            displayIn = formattingLocale,
            deviceLocale = deviceLocale,
            onSelect = {
                onRegion(it)
                regionSheetOpen = false
            },
            onDismiss = { regionSheetOpen = false },
        )
    }

    editingTime?.let { field ->
        AppTimePickerDialog(
            initialMinutesOfDay = field.currentMinutes(schedule),
            confirmLabel = "Set",
            dismissLabel = "Cancel",
            title = { Text(field.label) },
            onDismissRequest = { editingTime = null },
            onConfirm = { minutes ->
                field.applyTo(schedule, minutes, onWakingHours, onPlanningOverrideMinutes)
                editingTime = null
            },
        )
    }
}

/**
 * §4.9: *"the screen opens with a **scope line**, not a title alone"*.
 *
 * §0.4 forbids the app to be silent about what outlives sign-out, and §4.9's own
 * example of that is `C13`'s encrypted third-party key.
 *
 * ⚠️ **Rewritten by `C13` #54, exactly as this KDoc said it would be.** It used
 * to end at *"it stays exactly as it is when you sign out"* and carried a note
 * saying the key was not built, so the line must not announce a secret the app
 * does not hold. The app holds one now, so the second sentence names it: the
 * key is the one thing here a user might *want* gone when they sign out, and
 * §0.4's whole point is that the app may not be silent about that. The word
 * **removed** is doing work — it says the door exists and where it is, which is
 * what stops *"stays on this phone"* reading as *"you are stuck with it"*.
 */
@Composable
private fun ScopeLine() {
    Text(
        text = "Everything here belongs to this phone, not to your account. " +
            "It stays exactly as it is when you sign out — including your own " +
            "API key, if you add one, until you remove it below.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag(TAG_SCOPE_LINE),
    )
}

/**
 * §4.9's Appearance section, in §4.9's own order: **material · colour ·
 * brightness**.
 *
 * ## Why the material comes first
 *
 * It is the only one of the three that changes what the other two *mean*. The
 * skin is a palette and brightness is an end of it; the material decides how
 * both are rendered — and, for dark neo, whether brightness can move at all. A
 * user who picks brightness first and material second has had their first
 * choice silently overruled, which is the shape §0.3 is about.
 *
 * ## The lock, said twice on purpose
 *
 * §4.1 requires the picker to **say** dark neo is brightness-locked, and §4.9
 * rules that **a lock is a word, never a dimming**. So it is stated in two
 * places that cannot drift, because both read
 * [AppMaterial.isBrightnessLocked]: the word `Dark only` on the tile, and the
 * struck-through, captioned brightness control here. The segments are *also*
 * disabled — the rule forbids a dimming **instead of** a word, not alongside
 * one, and a control that is legible, captioned and still inert is worse than
 * one you cannot press.
 *
 * **The stored brightness is not overwritten by the lock.** That is what the
 * consequence line's second sentence is for: the setting is remembered and
 * takes effect again the moment another material is chosen, so the lock is a
 * suspension rather than a silent write.
 */
@Composable
private fun AppearanceCard(
    material: AppMaterial,
    onMaterial: (AppMaterial) -> Unit,
    background: AppBackground,
    onBackground: (AppBackground) -> Unit,
    brightness: AppBrightness,
    onBrightness: (AppBrightness) -> Unit,
    skin: AppSkin,
    onSkin: (AppSkin) -> Unit,
) {
    val brightnessIsDark = brightness.isDark(isSystemInDarkTheme())
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingLabel("Material")
            MaterialPicker(
                selected = material,
                skin = skin,
                brightnessIsDark = brightnessIsDark,
                background = background,
                onSelect = onMaterial,
                modifier = Modifier.testTag(TAG_MATERIAL_PICKER),
            )
            // §4.9's table: Material's consequence line states "that dark neo is
            // brightness-locked". Live, so it reads as a fact about the current
            // choice rather than a warning about someone else's.
            ConsequenceLine(
                text = materialConsequence(material),
                modifier = Modifier.testTag(TAG_MATERIAL_CONSEQUENCE),
            )

            Spacer(Modifier.height(20.dp))

            // #57 b. Directly under Material and above Brightness, because it is
            // the axis that COMBINES with the one above it -- Ido's own words
            // were "combinations between the backgrounds and the blocks", and a
            // combination read as one thing is what puts the two controls
            // adjacent. It is the fourth control in a card that already had
            // three, which is a real cost; the alternative shape (AiCard's
            // summary row opening an editor) was rejected because a background
            // is judged by LOOKING at it, and a control you must open to see is
            // one that cannot be compared against the material tiles above it.
            SettingLabel("Background")
            BackgroundPicker(
                selected = background,
                material = material,
                skin = skin,
                brightnessIsDark = brightnessIsDark,
                onSelect = onBackground,
                modifier = Modifier.testTag(TAG_BACKGROUND_PICKER),
            )
            ConsequenceLine(
                text = backgroundConsequence(background, material),
                modifier = Modifier.testTag(TAG_BACKGROUND_CONSEQUENCE),
            )

            Spacer(Modifier.height(20.dp))

            SettingLabel("Brightness")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppBrightness.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == brightness,
                        onClick = { onBrightness(option) },
                        enabled = !material.isBrightnessLocked,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppBrightness.entries.size,
                        ),
                    ) {
                        Text(
                            text = brightnessLabel(option),
                            textDecoration = if (material.isBrightnessLocked) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            if (material.isBrightnessLocked) {
                ConsequenceLine(
                    text = brightnessLockConsequence(material),
                    modifier = Modifier.testTag(TAG_BRIGHTNESS_LOCK),
                )
            }

            Spacer(Modifier.height(20.dp))

            SettingLabel("Colour")
            // Reused rather than re-authored: this string is already swept and
            // already translated, and #48 only had to delete the half of it
            // ("light and dark still follow your system setting") that the
            // Brightness control above made false.
            SettingDescription(stringResource(R.string.settings_appearance_description))
            SkinPicker(selected = skin, onSelect = onSkin)
        }
    }
}

/**
 * What choosing this material costs, in the app's own words.
 *
 * Two sentences and not one: the first is true of the *current* selection, the
 * second names the material that is about to take a control away. A line that
 * only fired once dark neo was already selected would be a report, not a
 * consequence.
 */
@Composable
private fun materialConsequence(material: AppMaterial): String {
    val locked = AppMaterial.entries.filter { it.isBrightnessLocked }
    if (locked.isEmpty()) return "Every material renders in light and dark."
    // map, not joinToString: joinToString's transform is not inline, and a
    // @Composable call cannot cross a non-inline lambda boundary.
    val names = locked.map { it.label() }.joinToString(" and ")
    return if (material.isBrightnessLocked) {
        names + " has no light scheme, so brightness below is fixed to dark."
    } else {
        material.label() + " renders in light and dark. " + names +
            " has no light scheme, so choosing it fixes brightness to dark."
    }
}

/** The struck-through control's caption — why it is inert, and what happens to the value. */
@Composable
private fun brightnessLockConsequence(material: AppMaterial): String =
    material.label() + " is dark only, so this has no effect right now. " +
        "Your choice is remembered and applies again as soon as you pick another material."

private fun brightnessLabel(brightness: AppBrightness): String = when (brightness) {
    AppBrightness.SYSTEM -> "Device"
    AppBrightness.LIGHT -> "Light"
    AppBrightness.DARK -> "Dark"
}

@Composable
private fun LanguageRegionCard(
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    region: AppRegion,
    deviceLocale: Locale,
    formattingLocale: Locale,
    onOpenRegionPicker: () -> Unit,
) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingLabel(stringResource(R.string.settings_language_title))
            SettingDescription(stringResource(R.string.settings_language_description))
            LanguagePicker(selected = language, onSelect = onLanguage)

            Spacer(Modifier.height(16.dp))

            SettingLabel("Region")
            ValueRow(
                value = regionLabel(region, formattingLocale, deviceLocale),
                onClick = onOpenRegionPicker,
                testTag = TAG_REGION_ROW,
            )
            // §4.9's table: Region's consequence line states "which day the week
            // starts, and how a date reads". Both are computed from the setting
            // rather than described, so they move the instant it does.
            ConsequenceLine(
                text = "Week starts on " +
                    region.firstDayOfWeek(deviceLocale)
                        .getDisplayName(TextStyle.FULL, formattingLocale) +
                    " · today reads " +
                    region.sampleDate(language, deviceLocale, LocalDate.now()),
                modifier = Modifier.testTag(TAG_REGION_CONSEQUENCE),
            )
        }
    }
}

private fun regionLabel(region: AppRegion, displayIn: Locale, deviceLocale: Locale): String =
    if (region.countryCode == null) {
        val name = AppRegion(deviceLocale.country.takeIf { it.isNotBlank() }).displayName(displayIn)
        if (name.isBlank()) "Follow the device" else "Follow the device — " + name
    } else {
        region.displayName(displayIn)
    }

@Composable
private fun YourDayCard(
    schedule: DaySchedule,
    formattingLocale: Locale,
    onEdit: (TimeField) -> Unit,
    onFollowWakingHours: () -> Unit,
) {
    val timeFormat = remember(formattingLocale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(formattingLocale)
    }
    val waking = schedule.waking
    val start = timeFormat.format(waking.start)
    val end = timeFormat.format(waking.end)
    val planning = timeFormat.format(schedule.planningTime)

    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            DayTrack(
                schedule = schedule,
                contentDescription = "Awake from $start to $end. Planning at $planning.",
            )

            Spacer(Modifier.height(18.dp))

            SettingLabel("Awake between")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ValueRow(
                    value = start,
                    onClick = { onEdit(TimeField.WakingStart) },
                    testTag = TAG_WAKING_START,
                    modifier = Modifier.weight(1f),
                )
                ValueRow(
                    value = end,
                    onClick = { onEdit(TimeField.WakingEnd) },
                    testTag = TAG_WAKING_END,
                    modifier = Modifier.weight(1f),
                )
            }
            ConsequenceLine(
                text = wakingConsequence(waking),
                modifier = Modifier.testTag(TAG_WAKING_CONSEQUENCE),
            )

            Spacer(Modifier.height(18.dp))

            SettingLabel("Plan tomorrow at")
            ValueRow(
                value = planning,
                onClick = { onEdit(TimeField.Planning) },
                testTag = TAG_PLANNING,
            )
            ConsequenceLine(
                text = if (schedule.planningFollowsWaking) {
                    "Follows your waking hours — one hour before they end, at $end. " +
                        "Move them and this moves too."
                } else {
                    "Pinned to $planning. It no longer follows your waking hours."
                },
                modifier = Modifier.testTag(TAG_PLANNING_CONSEQUENCE),
            )
            if (!schedule.planningFollowsWaking) {
                TextButton(
                    onClick = onFollowWakingHours,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("Follow my waking hours again")
                }
            }
        }
    }
}

/**
 * §4.9's table: *Awake between* states "how many hours that is, and where
 * `C9b`'s load bar reddens".
 *
 * The zero case is spelled out rather than rendered as "0 h", because start ==
 * end is a state the picker can reach and a bare zero reads as a bug in the
 * screen rather than as a description of what was just chosen.
 */
private fun wakingConsequence(waking: WakingHours): String {
    if (waking.lengthMinutes == 0) {
        return "No awake hours — the start and the end are the same time."
    }
    val span = spanOf(waking.lengthMinutes)
    val red = spanOf(waking.loadBarRedMinutes)
    return "$span awake — your day's load bar reddens past $red of planned work."
}

private fun spanOf(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "$hours h" else "$hours h $rest m"
}

@Composable
private fun AccountCard(onOpenProfile: (() -> Unit)?) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            if (onOpenProfile != null) {
                ListItem(
                    headlineContent = {
                        Text("Your profile", style = MaterialTheme.typography.titleSmall)
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable(onClick = onOpenProfile)
                        .testTag(TAG_PROFILE_ROW),
                )
            } else {
                ListItem(
                    headlineContent = {
                        Text("Not signed in", style = MaterialTheme.typography.titleSmall)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            // §4.9's table: "Your profile — that nothing on this screen belongs
            // to the account". Stated on both branches, because the signed-out
            // one is the branch that proves it.
            ConsequenceLine(
                text = "Your friend code, level, points and streak live there and leave " +
                    "with the account. Nothing on this screen does.",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(TAG_ACCOUNT_CONSEQUENCE),
            )
        }
    }
}

/**
 * The one-line explanation under a control's label — chrome, not a consequence.
 *
 * Deliberately a different component from [ConsequenceLine]: a description says
 * what the control is for and is true whatever it is set to, while a
 * consequence line states arithmetic and moves. Sharing one component would
 * have let a description quietly take a consequence's place, which is the
 * failure §4.9's table exists to prevent.
 */
@Composable
internal fun SettingDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
internal fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** A tappable row showing the current value of a setting edited in a window. */
@Composable
internal fun ValueRow(
    value: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    GpCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The whole ISO country list, in a sheet with a lazy list.
 *
 * A sheet rather than a dropdown menu because `DropdownMenu` composes every
 * item eagerly into a `Column` and this list is ~250 rows. It is not curated,
 * for the reason [AppRegion.offered] gives: a shortlist is an enumeration
 * somebody has to be in, and the person missing from it has no way to say so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionSheet(
    selected: AppRegion,
    displayIn: Locale,
    deviceLocale: Locale,
    onSelect: (AppRegion) -> Unit,
    onDismiss: () -> Unit,
) {
    val regions = remember(displayIn) { AppRegion.offered(displayIn) }
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Region",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_REGION_SHEET),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                RegionRow(
                    label = regionLabel(AppRegion.SYSTEM, displayIn, deviceLocale),
                    isSelected = selected.countryCode == null,
                    onClick = { onSelect(AppRegion.SYSTEM) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            items(regions, key = { it.id }) { candidate ->
                RegionRow(
                    label = candidate.displayName(displayIn),
                    isSelected = candidate == selected,
                    onClick = { onSelect(candidate) },
                )
            }
        }
    }
}

@Composable
private fun RegionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        trailingContent = if (isSelected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/**
 * Which clock the open time picker is editing.
 *
 * A type rather than three booleans, so the dialog reads its initial value and
 * writes its result through the same case — a pair that drifts apart in every
 * screen that stores "which field" separately from "what to do with it".
 */
private sealed interface TimeField {
    val label: String

    fun currentMinutes(schedule: DaySchedule): Int

    fun applyTo(
        schedule: DaySchedule,
        minutes: Int,
        onWakingHours: (WakingHours) -> Unit,
        onPlanningOverrideMinutes: (Int?) -> Unit,
    )

    data object WakingStart : TimeField {
        override val label = "Awake from"
        override fun currentMinutes(schedule: DaySchedule) = schedule.waking.startMinutes
        override fun applyTo(
            schedule: DaySchedule,
            minutes: Int,
            onWakingHours: (WakingHours) -> Unit,
            onPlanningOverrideMinutes: (Int?) -> Unit,
        ) = onWakingHours(schedule.waking.copy(startMinutes = minutes))
    }

    data object WakingEnd : TimeField {
        override val label = "Awake until"
        override fun currentMinutes(schedule: DaySchedule) = schedule.waking.endMinutes
        override fun applyTo(
            schedule: DaySchedule,
            minutes: Int,
            onWakingHours: (WakingHours) -> Unit,
            onPlanningOverrideMinutes: (Int?) -> Unit,
        ) = onWakingHours(schedule.waking.copy(endMinutes = minutes))
    }

    data object Planning : TimeField {
        override val label = "Plan tomorrow at"
        override fun currentMinutes(schedule: DaySchedule) = schedule.planningMinutes
        override fun applyTo(
            schedule: DaySchedule,
            minutes: Int,
            onWakingHours: (WakingHours) -> Unit,
            onPlanningOverrideMinutes: (Int?) -> Unit,
        ) = onPlanningOverrideMinutes(minutes)
    }
}

const val TAG_SCOPE_LINE = "settings_scope_line"
const val TAG_MATERIAL_PICKER = "settings_material_picker"
const val TAG_MATERIAL_CONSEQUENCE = "settings_material_consequence"
const val TAG_BACKGROUND_PICKER = "settings_background_picker"
const val TAG_BACKGROUND_CONSEQUENCE = "settings_background_consequence"
const val TAG_BRIGHTNESS_LOCK = "settings_brightness_lock"
const val TAG_REGION_ROW = "settings_region_row"
const val TAG_REGION_SHEET = "settings_region_sheet"
const val TAG_REGION_CONSEQUENCE = "settings_region_consequence"
const val TAG_WAKING_START = "settings_waking_start"
const val TAG_WAKING_END = "settings_waking_end"
const val TAG_WAKING_CONSEQUENCE = "settings_waking_consequence"
const val TAG_PLANNING = "settings_planning"
const val TAG_PLANNING_CONSEQUENCE = "settings_planning_consequence"
const val TAG_PROFILE_ROW = "settings_profile_row"
const val TAG_ACCOUNT_CONSEQUENCE = "settings_account_consequence"
