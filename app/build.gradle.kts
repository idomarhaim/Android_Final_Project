// Required by App Distribution plugin 5.x for a `firebaseAppDistribution { }`
// block inside a buildType. Without it the block still resolves, but through a
// deprecated path that warns on every configuration.
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import org.gradle.api.tasks.testing.Test
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

// Read developer-specific config from local.properties (git-ignored) so no
// secrets are committed. GOOGLE_WEB_CLIENT_ID is the OAuth 2.0 *Web* client id
// from the Firebase / Google Cloud console; it is needed for Google Sign-In and
// to request Google Tasks scopes. See docs/SETUP.md.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val googleWebClientId: String =
    localProps.getProperty("GOOGLE_WEB_CLIENT_ID") ?: "REPLACE_WITH_WEB_CLIENT_ID"
val functionsRegion: String =
    localProps.getProperty("FUNCTIONS_REGION") ?: "us-central1"

/**
 * Release-signing credentials. local.properties on a developer machine (written
 * by `scripts/new-release-keystore.ps1`), environment variables on CI — neither
 * is ever committed. See docs/RELEASING.md.
 */
fun secret(key: String): String? =
    localProps.getProperty(key) ?: System.getenv(key)

val releaseStoreFile: String? = secret("RELEASE_STORE_FILE")
val releaseStorePassword: String? = secret("RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = secret("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = secret("RELEASE_KEY_PASSWORD")

// Resolved once so both signingConfigs and the sanity check below agree on it.
val releaseKeystore = releaseStoreFile?.let { rootProject.file(it) }
val hasReleaseKey = releaseKeystore?.exists() == true &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.idomarhaim.goalpilot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.idomarhaim.goalpilot"
        minSdk = 26
        targetSdk = 35
        // BUMP BOTH ON EVERY RELEASE — versionCode strictly upward. Nothing
        // detects "an update is available" without it: App Distribution
        // compares versionCode, and Android refuses to install a build whose
        // versionCode is lower than the one already on the device. The release
        // checklist in docs/RELEASING.md exists because forgetting this is
        // silent — the build succeeds and testers are simply never prompted.
        versionCode = 14
        versionName = "0.5.3"

        testInstrumentationRunner = "com.idomarhaim.goalpilot.HiltTestRunner"
        vectorDrawables { useSupportLibrary = true }

        // Exposed to code as R.string.gp_web_client_id and BuildConfig.FUNCTIONS_REGION.
        resValue("string", "gp_web_client_id", googleWebClientId)
        buildConfigField("String", "FUNCTIONS_REGION", "\"$functionsRegion\"")
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // The real key when its credentials are present, the debug key
            // otherwise — a fresh clone can still `assembleRelease` and get an
            // installable APK. The fallback is for *local* builds only: the
            // release workflow fails the build outright when the real key is
            // missing (see the check below), because a debug-signed APK handed
            // to a tester can never be updated by a properly signed one.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")

            firebaseAppDistribution {
                // Testers are managed in the Firebase console. The group alias
                // must exist there or the upload fails with a 404 that reads
                // like an auth problem.
                groups = "testers"
                artifactType = "APK"

                // ⚠️ REPO-ROOT-RELATIVE, and the "app/" is load-bearing. This read
                // `"release-notes.txt"` until 2026-08-24 on the belief — written into
                // ReleaseNotesGuardTest's KDoc and docs/RELEASING.md — that the plugin
                // resolves the property against the app MODULE. It does not.
                //
                // `Observed:` `./gradlew :app:appDistributionUploadRelease` on
                // 2026-08-24 failed with
                //   Failed to read file "C:/Dev/Android_Final_Project/release-notes.txt"
                // naming the REPO ROOT, with app/release-notes.txt present the whole
                // time. The stray root copy deleted on 2026-08-22 was the file the
                // plugin had been reading all along, so deleting it killed the local
                // upload route, and nothing noticed for two days because nobody ran it.
                //
                // Writing the path with its directory in it makes the property say what
                // it does under either reading, which is the only form that cannot rot
                // back. It also now literally matches release.yml's
                // `--release-notes-file app/release-notes.txt`.
                releaseNotesFile = "app/release-notes.txt"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
    }
}

// ImeSettleSweepTest (issue #58) is a JVM test that READS the instrumented
// sources as text. Gradle has no way to know that: `testDebugUnitTest`'s tracked
// inputs are the main and unit-test source sets, so a commit that touches only
// `src/androidTest/` leaves the task UP-TO-DATE and the sweep reports its
// PREVIOUS result — green — without running.
//
// That is the one case the guard exists for, so it would have been silent
// exactly when it mattered. `Observed:` 2026-08-21 — a raw `performTextInput`
// deliberately reintroduced into SilentFilingUiTest.kt passed with no output,
// and failed correctly only under `--rerun-tasks`. Declaring the directory as an
// input costs one re-run per androidTest commit and closes it.
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/androidTest"))
        .withPropertyName("androidTestSourcesReadByImeSettleSweepTest")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    // ── Compose ────────────────────────────────────────────────────
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ── AndroidX core / lifecycle ─────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── Hilt (DI) ─────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── Coroutines ────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Firebase (Auth + Firestore + Storage + Functions) ─────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.play.services.auth)

    // ── Firebase App Distribution (in-app update prompt) ──────────
    // `-api` is a no-op stub compiled into every variant; the real
    // implementation is release-only, so debug builds carry no updater and
    // AppUpdateChecker quietly does nothing there. That asymmetry is the
    // whole point of the split — do not "simplify" it to one dependency.
    implementation(libs.firebase.appdistribution.api)
    releaseImplementation(libs.firebase.appdistribution)

    // ── Encrypted key store (C13 #54, decided in #32 §1) ──────────
    // The ONE place a third-party secret is at rest in this app. Keystore-backed
    // EncryptedSharedPreferences, never Firestore: #32 §1 rejected the cheaper
    // Firestore option on posture, not on reachability. See
    // data/security/EncryptedAiProviderRepository and the two backup-exclusion
    // rule files it forces into the manifest.
    implementation(libs.androidx.security.crypto)

    // ── Media / serialization ─────────────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    // ── Health Connect (fitness & sleep, spec §5 nice-to-have) ────
    implementation(libs.androidx.health.connect)

    // ── Glance (home-screen widget pack, spec §4.5) ───────────────
    // Compose-shaped, but it does not render Compose: the composable tree is
    // compiled to a RemoteViews that the *launcher* inflates in its own process.
    // Nothing from ui/components/ crosses that line — no Canvas, no
    // Modifier.blur, no animation — which is why ui/widget/ draws its charts
    // into bitmaps instead of reusing them.
    implementation(libs.androidx.glance.appwidget)

    // ── WorkManager (#8's local scheduling, spec §2.5) ─────────────
    // §2.7 establishes there is no credential for a background sync and cannot
    // be one, so every reminder in this app is scheduled locally or not at all.
    // WorkManager rather than AlarmManager: see notifications/ReminderScheduler.
    implementation(libs.androidx.work.runtime)

    // ── Unit tests ────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    // ── Instrumented tests ────────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}

/**
 * The localization guards read files off disk, and Gradle cannot see that.
 *
 * `HebrewLocaleResourceTest`, `AnalyticsLiteralSweepTest`,
 * `WidgetHebrewResourceTest` and `WidgetPaletteResourceTest` do not exercise
 * code — they open `src/main/res` and `src/main/java` with `java.io.File` and
 * assert on the text. Nothing in that is
 * visible to Gradle's up-to-date check, whose declared inputs for a unit-test
 * task are the test classes and the runtime classpath. **Editing the *value* of
 * an existing string changes neither**: `R.jar` is keyed on the resource *names*,
 * so a translated-to-English `values-iw/` string leaves every declared input
 * byte-identical, `:app:testDebugUnitTest` reports UP-TO-DATE, and the guard that
 * exists to catch exactly that edit never executes a single assertion.
 *
 * It fails in the flattering direction, and it fails precisely when it matters:
 * a resource-only change is what a localization sweep *is*, so the guard is off
 * on the one commit shape it was written for. `Observed:` 2026-08-16 by session
 * `51c-analytics-render` and reproduced here before this block was added —
 * `gp_widget_level` set to `"Level"` in `values-iw/` still built green.
 *
 * Declaring the two trees as inputs is the fix rather than `outputs.upToDateWhen
 * { false }`, which would also work and would cost the whole suite on every
 * unrelated build. `--rerun-tasks` is the manual version of the same thing and
 * depends on someone remembering, which is what made this a defect.
 *
 * `withPathSensitivity(RELATIVE)` is there for the build cache, which is on in
 * this repo: `Test` is `@CacheableTask`, and an `inputs.dir` normalizes on
 * ABSOLUTE paths unless told otherwise, which would key every cache entry to
 * this checkout directory. `Observed:` 2026-08-16 — deleting these two lines and
 * re-running under `--warning-mode=all` produces **no warning of any kind**, so
 * nothing in the build will tell you if they are dropped, which is why the
 * reason is written here rather than left to be rediscovered. `Inferred:` the
 * cache-reuse consequence itself, from Gradle's documented default
 * normalization; not measured here.
 */
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/res"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("fileScanningGuardResources")
    inputs.dir(layout.projectDirectory.dir("src/main/java"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("fileScanningGuardSources")

    // The same problem one directory further out. `DerivedStateFixtureTest` reads
    // ../shared-fixtures/derived-state.json — outside this module entirely, because
    // functions/test/projection.test.mjs reads the identical file and neither layer may
    // own it (docs/PRODUCT_v0.3.md §5.2). Gradle cannot infer that, so without this line
    // editing the fixture leaves the task UP-TO-DATE and the suite reports green on the
    // previous run's numbers. `Observed:` 2026-08-20 during the negative control for
    // `C20`'s build half — a deliberately broken fixture went red under an explicit
    // --tests filter and then "passed" in 1 s on the next full run.
    inputs.file(rootProject.layout.projectDirectory.file("shared-fixtures/derived-state.json"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("sharedDerivedStateFixture")

    // `ReleaseNotesGuardTest` reads the release notes and the RELEASE WORKFLOW, and both are
    // outside anything Gradle associates with a test. Without these two lines, changing either
    // leaves the task UP-TO-DATE and the guard reports green on the previous run.
    //
    // `Observed:` 2026-08-22, in the mutation check written to prove that guard is not vacuous —
    // the workflow was edited to name a different notes file and `testDebugUnitTest` answered
    // UP-TO-DATE in 2 s. The mutation would have been recorded as "the guard did not catch it",
    // which is the opposite of what happened: the guard never ran. Same class as the
    // shared-fixture line above, and it is the second instance in three days.
    inputs.file(layout.projectDirectory.file("release-notes.txt"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("releaseNotesReadByGuard")
    inputs.file(rootProject.layout.projectDirectory.file(".github/workflows/release.yml"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("releaseWorkflowReadByGuard")

    // THIS FILE is the guard's third input, and it was missing from the day the guard was
    // written -- the comment above says "if this class grows a third file, declare that one
    // too" without noticing the class already had one. A build script is not an input to a
    // test task, so editing `releaseNotesFile` above leaves testDebugUnitTest cacheable.
    //
    // `Observed:` 2026-08-24, mutating that property back to its broken value and re-running
    // this class gave BUILD SUCCESSFUL in 9 s having executed nothing. With --rerun-tasks the
    // same mutation fails three of the four assertions. Without this line the guard is green
    // on exactly the edit it exists to catch.
    inputs.file(layout.projectDirectory.file("build.gradle.kts"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("moduleBuildFileReadByGuard")

    // `DocsCurrencyTest` reads the documentation, the functions entry point and the JDK pin, and
    // Gradle associates a test task with none of them. Same class as all three cases above; this
    // is the third instance in three days, which is why the guard's own KDoc says so out loud.
    //
    // The two Kotlin sources it parses (`Constants.kt`, `Destinations.kt`) need NO line here --
    // `inputs.dir("src/main/java")` above already covers them as text. Only these four are outside
    // every tracked tree.
    inputs.dir(rootProject.layout.projectDirectory.dir("docs"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("docsReadByCurrencyGuard")
    inputs.file(rootProject.layout.projectDirectory.file("README.md"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("readmeReadByCurrencyGuard")
    inputs.file(rootProject.layout.projectDirectory.file("functions/src/index.ts"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("functionsIndexReadByCurrencyGuard")
    inputs.file(rootProject.layout.projectDirectory.file("gradle.properties"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("gradlePropertiesReadByCurrencyGuard")
}

/**
 * A debug-signed APK is a dead end: the moment a tester installs one, the
 * properly signed successor can no longer update it — Android rejects a
 * signature change and the only way out is uninstall-and-lose-your-data. Local
 * `assembleRelease` may still fall back to the debug key (handy, and it never
 * leaves the machine), but *distributing* one is unrecoverable, so the upload
 * task refuses rather than warns.
 */
tasks.matching { it.name.startsWith("appDistributionUpload") }.configureEach {
    doFirst {
        check(hasReleaseKey) {
            "Refusing to distribute: no release signing key. RELEASE_STORE_FILE / " +
                "RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD must " +
                "resolve from local.properties or the environment, and the keystore must " +
                "exist. Run scripts/new-release-keystore.ps1 — see docs/RELEASING.md."
        }
    }
}
