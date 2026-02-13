plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") apply false
}

android {
    namespace = "com.example.mobilewebapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mobilewebapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.webkit:webkit:1.8.0")

    // Ad SDKs
    implementation("com.tapjoy:tapjoy-android-sdk:14.6.0")
    implementation("com.unity3d.ads:unity-ads:4.16.6")
    implementation("com.google.android.gms:play-services-ads:24.9.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-analytics")
}

// Copy common/ web assets into android assets before build
tasks.register<Copy>("syncCommonAssets") {
    from("${rootProject.projectDir}/../common")
    into("src/main/assets/common")
}

tasks.named("preBuild") {
    dependsOn("syncCommonAssets")
}

// Apply google-services plugin only if google-services.json exists
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
