import org.gradle.api.tasks.Sync

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.astral.unwm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.astral.unwm"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            // debug build is installable without signing
            signingConfig = signingConfigs.getByName("debug")
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")

    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.webkit:webkit:1.8.0")
    implementation("io.coil-kt:coil-compose:2.5.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.hamcrest:hamcrest:2.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val watermarkInputDir = rootProject.layout.projectDirectory.dir("watermark")
val watermarkAssetsOutput = layout.buildDirectory.dir("generated/watermark-assets/web/watermark-samples")

val generateWatermarkAssets by tasks.registering(Sync::class) {
    from(watermarkInputDir)
    include("**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.webp")
    into(watermarkAssetsOutput.get().asFile)
    onlyIf { watermarkInputDir.asFile.exists() }
}

androidComponents {
    val objects = project.objects
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            generateWatermarkAssets
        ) { task -> objects.directoryProperty().apply { set(task.destinationDir) } }
    }
}
