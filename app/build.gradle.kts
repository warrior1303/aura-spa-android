plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

// Nota: al tener un proyecto multi-módulo estándar, este archivo raíz coordina los plugins.
// El módulo app se configura con el bloque siguiente si usas un único archivo de compilación simplificado
// o con settings.gradle.kts + build.gradle.kts en la raíz y app/build.gradle.kts.
