plugins { alias(libs.plugins.kotlin.serialization) }

dependencies {
    implementation(project(":common"))
    implementation(rootProject.libs.kotlinx.serialization.core)
    implementation(rootProject.libs.kotlinx.serialization.cbor)
}
