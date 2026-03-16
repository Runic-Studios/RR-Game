package com.runicrealms.game.plugin

import com.google.inject.Inject
import java.util.Properties
import org.slf4j.LoggerFactory

data class RunicBuildInfo(
    val branch: String,
    val commitSha: String,
    val commitShortSha: String,
    val commitMessage: String,
    val buildId: String,
    val buildSource: String,
    val builtAtUtc: String,
    val pluginVersion: String,
)

class RunicBuildInfoProvider @Inject constructor(private val plugin: GamePlugin) {
    private val logger = LoggerFactory.getLogger("plugin")

    val info: RunicBuildInfo by lazy { load() }

    private fun load(): RunicBuildInfo {
        val properties = Properties()

        plugin.getResource("runic-build-info.properties")?.use { stream -> properties.load(stream) }
            ?: logger.warn("runic-build-info.properties was not found in the plugin jar.")

        fun propertyOrUnknown(name: String): String =
            properties.getProperty(name)?.trim().orEmpty().ifEmpty { "unknown" }

        return RunicBuildInfo(
            branch = propertyOrUnknown("branch"),
            commitSha = propertyOrUnknown("commitSha"),
            commitShortSha = propertyOrUnknown("commitShortSha"),
            commitMessage = propertyOrUnknown("commitMessage"),
            buildId = propertyOrUnknown("buildId"),
            buildSource = propertyOrUnknown("buildSource"),
            builtAtUtc = propertyOrUnknown("builtAtUtc"),
            pluginVersion = propertyOrUnknown("pluginVersion"),
        )
    }
}
