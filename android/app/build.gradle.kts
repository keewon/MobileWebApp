plugins {
    id("com.android.application")
    id("com.google.gms.google-services") apply false
}

android {
    namespace = "app.keewon.mobilewebapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.keewon.mobilewebapp"
        minSdk = 24
        targetSdk = 36
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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.webkit:webkit:1.17.1")

    // Ad SDKs
    implementation("com.tapjoy:tapjoy-android-sdk:14.8.0")
    implementation("com.unity3d.ads:unity-ads:4.21.0")
    implementation("com.google.android.gms:play-services-ads:25.5.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:35.0.2"))
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
