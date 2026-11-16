import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("androidx.navigation.safeargs")
    alias(libs.plugins.google.gms.google.services)
}

//API Key de Google Maps: se lee de local.properties (MAPS_API_KEY=...) para no subirla a git
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "pe.edu.usat.incidentapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pe.edu.usat.incidentapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //Se usa en el AndroidManifest como ${MAPS_API_KEY}
        manifestPlaceholders["MAPS_API_KEY"] = propiedadesLocales.getProperty("MAPS_API_KEY", "")
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    //Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    //ViewModel y LiveData
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    //Room (persistencia local / modo offline)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    //SwipeRefreshLayout
    implementation(libs.swiperefreshlayout)

    //Glide (carga de imagenes desde internet / API)
    implementation(libs.glide)

    //Lectura de QR (ZXing)
    implementation(libs.zxing.android.embedded)

    //Firebase Cloud Messaging (notificaciones push)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    //Google Maps y ubicación del técnico
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
