plugins { alias(libs.plugins.shadow) }

tasks.build { dependsOn("shadowJar") }

dependencies {
    implementation(project(":data"))
    implementation(project(":common"))
    implementation(project(":gameplay"))
    implementation(project(":items"))
}

tasks.shadowJar {
    archiveBaseName.set("game-plugin")
    mergeServiceFiles() // Necessary because of something to do with gRPC managed channels
    relocate("com.google.protobuf", "shadow.com.google.protobuf")
    relocate("com.fasterxml.jackson", "shadow.com.fasterxml.jackson")
    relocate("co.aikar.commands", "game.com.aikar.commands")
    relocate("co.aikar.locales", "game.aikar.locales")
}
