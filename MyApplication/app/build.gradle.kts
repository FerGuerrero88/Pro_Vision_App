plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")



    // ML Kit: Text Recognition y Traducción
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:translate:17.0.1")

    // Glide para carga de imágenes desde IP
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.language.id.common)
    implementation(libs.firebase.database)
    implementation(libs.swiperefreshlayout)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.translate)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}