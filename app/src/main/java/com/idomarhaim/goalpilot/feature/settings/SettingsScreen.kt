package com.idomarhaim.goalpilot.feature.settings

import android.content.res.Resources
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.model.displayName
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.LanguagePicker
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
 * ## What is here, and what is deliberately not
 *
 * §4.9 lists five sections. Four are built here — Appearance, Language &
 * region, Your day, Account. **Two controls it names are not, and neither is an
 * oversight:**
 *
 * | Missing | Why | Whose ticket |
 * |---|---|---|
 * | Appearance's **material (4 tiles)** | §4.1's four-material contract does not exist in this codebase — no `AppMaterial`, no palette transform, and no open issue scheduling one. A picker over materials nothing renders is a control that changes nothing, which is §0.3's defect installed in the screen built to prevent it | `C12` §4.1 |
 * | the whole **AI** section | its three controls are `C13`'s — an `EncryptedSharedPreferences` key store (`androidx.security:security-crypto`, not a dependency here), a provider abstraction, and a status line naming which provider answered. Every model call goes through the Cloud Function proxy today and the client holds no key at all | `C13`, designed in #32, unbuilt |
 *
 * Both are recorded in this session's changelog rather than shown as a disabled
 * row. §4.9 rules that **a lock is a word, never a dimming** — and the honest
 * word for a section whose subsystem does not exist belongs in the backlog, not
 * on the user's screen.
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
    val language by viewModel.language.collectAsStateWithLifecycle()
    val region by viewModel.region.collectAsStateWithLifecycle()
    val schedule by viewModel.daySchedule.collectAsStateWithLifecycle()

    SettingsContent(
        skin = skin,
        onSkin = viewModel::setSkin,
        brightness = brightness,
        onBrightness = viewModel::setBrightness,
        language = language,
        onLanguage = viewModel::setLanguage,
        region = region,
        onRegion = viewModel::setRegion,
        schedule = schedule,
        onWakingHours = viewModel::setWakingHours,
        onPlanningOverrideMinutes = viewModel::setPlanningOverrideMinutes,
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
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    region: AppRegion,
    onRegion: (AppRegion) -> Unit,
    schedule: DaySchedule,
    onWakingHours: (WakingHours) -> Unit,
    onPlanningOverrideMinutes: (Int?) -> Unit,
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
        topBar = {
            TopAppBar(
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
 * §0.4 forbids the app to be silent about what outlives sign-out. §4.9's own
 * example of that is `C13`'s encrypted third-party key, which is not built — so
 * this line states the scope that **is** true today rather than announcing a
 * secret the app does not hold. It is a sentence to rewrite the day `C13`
 * lands, not one to delete.
 */
@Composable
private fun ScopeLine() {
    Text(
        text = "Everything here belongs to this phone, not to your account. " +
            "It stays exactly as it is when you sign out.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag(TAG_SCOPE_LINE),
    )
}

@Composable
private fun AppearanceCard(
    brightness: AppBrightness,
    onBrightness: (AppBrightness) -> Unit,
    skin: AppSkin,
    onSkin: (AppSkin) -> Unit,
) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingLabel("Brightness")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppBrightness.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == brightness,
                        onClick = { onBrightness(option) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppBrightness.entries.size,
                        ),
                    ) {
                        Text(brightnessLabel(option))
                    }
                }
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
private fun SettingDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** A tappable row showing the current value of a setting edited in a window. */
@Composable
private fun ValueRow(
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
