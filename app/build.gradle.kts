import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// The single source of the app version. /commit-tag-push rewrites this line when it cuts a release.
val appVersion = "0.7.0"

// versionCode is derived from the version: 1.2.3 -> 10203, so it rises with every release.
val appVersionCode = appVersion.split('.').map(String::toInt).let { (major, minor, patch) ->
    major * 10_000 + minor * 100 + patch
}

val gitCommit: String = providers.exec {
    commandLine("git", "rev-parse", "--short=8", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.get().trim().ifEmpty { "unknown" }

val buildTime: String = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"))

// Release signing comes from environment variables; see DEPLOYMENT.md. Without them the release APK is unsigned.
val signingKeystore: String? = System.getenv("SIGNING_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

// Optional debug signing, set by scripts/run-debug.* from scripts/.env; see BUILDING.md. Without it the
// debug APK is signed with the default Android debug key.
val debugSigningKeystore: String? = System.getenv("DEBUG_SIGNING_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

android {
    namespace = "pl.cukrzycowy.labelbite"
    compileSdk = 37

    defaultConfig {
        applicationId = "pl.cukrzycowy.labelbite"
        minSdk = 31
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion

        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
    }

    signingConfigs {
        if (signingKeystore != null) create("release") {
            storeFile = file(signingKeystore)
            storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
        if (debugSigningKeystore != null) create("localDebug") {
            storeFile = file(debugSigningKeystore)
            storePassword = System.getenv("DEBUG_SIGNING_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("DEBUG_SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("DEBUG_SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            if (debugSigningKeystore != null) signingConfig = signingConfigs.getByName("localDebug")
        }
        release {
            if (signingKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
}
