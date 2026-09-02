import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.nudo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nudo.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.3"

        buildConfigField(
            "String",
            "NUDO_BASE_URL",
            "\"${propiedadesLocales.getProperty("NUDO_BASE_URL", "https://nudo.finkafest.es")}\"",
        )
        // Clave de ARRANQUE, no de acceso: solo sirve para dar de alta este
        // dispositivo y recibir su token propio. Sigue viajando en el APK, pero
        // extraerla ya no muestra ninguna conversación: cada una tiene dueño.
        buildConfigField(
            "String",
            "NUDO_CLAVE_ARRANQUE",
            "\"${propiedadesLocales.getProperty(
                "NUDO_CLAVE_ARRANQUE",
                propiedadesLocales.getProperty("NUDO_API_KEY", ""),
            )}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Se firma con la clave de debug para poder instalar el APK a mano
            // (la app no se distribuye por Play Store). Mantiene la misma firma
            // que las instalaciones existentes, así se actualiza encima sin
            // desinstalar. Cambiar a una clave propia antes de publicarla.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}
