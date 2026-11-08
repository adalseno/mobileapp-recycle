import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
}

// Release signing. Values come from Gradle properties so no secret is ever in the repo:
// locally from ~/.gradle/gradle.properties, on CI from ORG_GRADLE_PROJECT_* environment variables.
// Without them, the release APK is built unsigned.
val releaseStoreFile = providers.gradleProperty("releaseStoreFile").orNull

android {
    namespace = "io.github.adalseno.bottlechecker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Branding values come from gradle.properties so a rebrand is a config change.
        applicationId = providers.gradleProperty("app.applicationId").get()
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("app.versionCode").get().toInt()
        versionName = providers.gradleProperty("app.versionName").get()
        resValue("string", "app_name", providers.gradleProperty("app.name").get())
    }
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("releaseStorePassword").get()
                keyAlias = providers.gradleProperty("releaseKeyAlias").get()
                keyPassword = providers.gradleProperty("releaseKeyPassword").get()
            }
        }
    }
    buildFeatures {
        resValues = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
