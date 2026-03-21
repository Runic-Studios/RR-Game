package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell with an area-of-effect radius (in blocks). */
interface RadiusSpell {
    var radius: Double

    fun loadRadiusData(config: FileConfiguration) {
        radius = config.getDouble("radius", radius)
    }
}
