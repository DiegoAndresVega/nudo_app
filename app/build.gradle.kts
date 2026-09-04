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
        versionCode = 5
        versionName = "0.5"

        buildConfigField(
            "String",
            "NUDO_BASE_URL",
            "\"${propiedadesLocales.getProperty("NUDO_BASE_URL", "https://nudo.finkafest.es")}\"",
        )
        // Clave de ALTA, no de acceso: es el `NUDO_CLAVE_ALTA` del servidor y lo
        // único que puede es dar de alta este dispositivo para recibir su token.
        // Sigue viajando en el APK, pero extraerla no muestra ninguna conversación
        // (cada una tiene dueño) ni deja fuera a nadie (no revoca dispositivos).
        buildConfigField(
            "String",
            "NUDO_CLAVE_ARRANQUE",
            "\"${propiedadesLocales.getProperty("NUDO_CLAVE_ARRANQUE", "")}\"",
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
