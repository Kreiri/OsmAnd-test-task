plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.osmandtesttask"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.osmandtesttask"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.simplexml)

    implementation(libs.kotlinx.coroutines.android)


    testImplementation(libs.bundles.unitTestSuite)
    testImplementation(libs.bundles.androidTestSuite)
}