import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystoreProperties = Properties()
val releaseKeystorePropertiesFile = rootProject.file("release-keystore.properties")
if (releaseKeystorePropertiesFile.exists()) {
    releaseKeystorePropertiesFile.inputStream().use(releaseKeystoreProperties::load)
}
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { key -> releaseKeystoreProperties.getProperty(key)?.isNotBlank() == true }

android {
    namespace = "com.alfa.shakegroan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alfa.shakegroan"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("storeFile"))
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    val metricsEndpoint = providers.gradleProperty("fallOuchMetricsEndpoint")
        .orElse("http://89.125.81.120:8787/v1/events")
    val metricsWriteKey = providers.gradleProperty("fallOuchMetricsWriteKey")
        .orElse("e37c64c213d27cc598647558aa459ef4d1d5210c4173ebf4")
    defaultConfig {
        buildConfigField("String", "FALL_OUCH_METRICS_ENDPOINT", "\"${metricsEndpoint.get()}\"")
        buildConfigField("String", "FALL_OUCH_METRICS_WRITE_KEY", "\"${metricsWriteKey.get()}\"")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("com.google.android.material:material:1.13.0")
    implementation(composeBom)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
