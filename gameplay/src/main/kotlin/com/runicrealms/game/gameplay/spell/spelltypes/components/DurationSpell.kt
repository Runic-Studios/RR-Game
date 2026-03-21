package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell effect that lasts for a defined duration (in seconds). */
interface DurationSpell {
    var duration: Double

    fun loadDurationData(config: FileConfiguration) {
        duration = config.getDouble("duration", duration)
    }
}
