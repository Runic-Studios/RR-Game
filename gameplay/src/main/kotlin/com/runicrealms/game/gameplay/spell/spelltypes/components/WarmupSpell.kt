package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell with a cast time / channel delay before its effect fires (in seconds). */
interface WarmupSpell {
    var warmupSeconds: Double

    fun loadWarmupData(config: FileConfiguration) {
        warmupSeconds = config.getDouble("warmup", warmupSeconds)
    }
}
