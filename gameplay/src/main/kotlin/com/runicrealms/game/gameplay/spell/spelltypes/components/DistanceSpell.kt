package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell that travels or applies its effect up to a defined distance (in blocks). */
interface DistanceSpell {
    var distance: Double

    fun loadDistanceData(config: FileConfiguration) {
        distance = config.getDouble("distance", distance)
    }
}
