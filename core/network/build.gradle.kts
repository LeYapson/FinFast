import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Client Ktor, session (jeton d'accès) et APIs du serveur. Aucune dépendance vers l'interface ni vers `shared`.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "fr.mobilune.componentgallery.core.network"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            // `api` : HttpClient fait partie de l'interface publique (createHttpClient, constructeurs des APIs).
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
