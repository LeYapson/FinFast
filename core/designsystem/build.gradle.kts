import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Thème, accessibilité, animations et composants partagés. Aucune dépendance vers `shared` ni vers le réseau.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "fr.mobilune.componentgallery.core.designsystem"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.animation)
            api(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
        }
    }
}

compose.resources {
    // Les icônes et la carte Visa de ce module ont leur propre classe `Res`, distincte de celle de `shared`.
    packageOfResClass = "fr.mobilune.componentgallery.core.designsystem.resources"
}
