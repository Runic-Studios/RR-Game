package com.runicrealms.game.plugin

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class GamePluginLoader : PluginLoader {

    override fun classloader(pluginClasspathBuilder: PluginClasspathBuilder) {
        // This is necessary for InvUI on paper, see the docs
        val resolver = MavenLibraryResolver()
        resolver.addRepository(
            RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases/")
                .build()
        )
        // Note: The version must match the version in libs.versions.toml
        resolver.addDependency(
            Dependency(DefaultArtifact("xyz.xenondevs.invui:invui:pom:1.45"), null)
        )
        pluginClasspathBuilder.addLibrary(resolver)
    }
}
