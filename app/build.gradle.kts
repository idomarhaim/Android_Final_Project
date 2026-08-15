// Required by App Distribution plugin 5.x for a `firebaseAppDistribution { }`
// block inside a buildType. Without it the block still resolves, but through a
// deprecated path that warns on every configuration.
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
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
        versionCode = 4
        versionName = "0.2.2"

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
                releaseNotesFile = "release-notes.txt"
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
