package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell that deals physical damage. Scaling is applied by SpellScalingListener. */
interface PhysicalDamageSpell {
    var physicalDamage: Double
    var physicalDamagePerLevel: Double

    fun loadPhysicalData(config: FileConfiguration) {
        physicalDamage = config.getDouble("physical-damage", physicalDamage)
        physicalDamagePerLevel =
            config.getDouble("physical-damage-per-level", physicalDamagePerLevel)
    }
}
