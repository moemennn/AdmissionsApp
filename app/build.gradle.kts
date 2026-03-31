plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "edu.fandm.mibrahi1.admissionsapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "edu.fandm.mibrahi1.admissionsapp"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
//    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.0.0")
}