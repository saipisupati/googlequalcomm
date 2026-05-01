// Versions chosen to be binary-compatible with com.google.ai.edge.litertlm:litertlm-android:0.10.0,
// which was compiled with Kotlin 2.3.x (metadata 2.3.0) and pulls in kotlin-stdlib 2.2.21+ transitively.
// Older Kotlin (≤2.2) cannot read metadata 2.3.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.7" apply false
}
