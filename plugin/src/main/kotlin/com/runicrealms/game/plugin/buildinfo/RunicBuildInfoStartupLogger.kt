package com.runicrealms.game.plugin.buildinfo

import com.google.inject.Inject
import org.slf4j.LoggerFactory

class RunicBuildInfoStartupLogger
@Inject
constructor(private val runicBuildInfoProvider: RunicBuildInfoProvider) {
    init {
        val info = runicBuildInfoProvider.info
        val logger = LoggerFactory.getLogger("plugin")
        logger.info("Server Build Info:")
        logger.info("  Version: {}", info.pluginVersion)
        logger.info("  Branch: {}", info.branch)
        logger.info("  Commit: {} ({})", info.commitShortSha, info.commitSha)
        logger.info("  Message: {}", info.commitMessage)
        logger.info("  Built: {}", info.builtAtUtc)
        logger.info("  Build Source: {} (id: {})", info.buildSource, info.buildId)
    }
}
