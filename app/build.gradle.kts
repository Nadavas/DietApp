plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nadavariel.dietapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nadavariel.dietapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.0" // This is the crucial line to add/update
    }
}

dependencies {

    // Core Android KTX and Compose dependencies
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // BOM should generally be near the top or specifically grouped
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences) // DataStore
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3) // Material 3
    //implementation(libs.androidx.material) // Material 2 (consider if you truly need both)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Firebase dependencies
    implementation(platform(libs.firebase.bom)) // Firebase BOM should generally be near the top or specifically grouped
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.google.firebase.auth.ktx) // Duplication? firebase.auth.ktx and google.firebase.auth.ktx
    implementation(libs.play.services.auth) // Google Play Services Auth

    // For graphics
    implementation(libs.mpandroidchart)

    // Testing dependencies
    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}