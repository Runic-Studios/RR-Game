package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell that applies a shield. Scaling is applied by SpellScalingListener. */
interface ShieldingSpell {
    var shieldAmount: Double
    var shieldPerLevel: Double

    fun loadShieldingData(config: FileConfiguration) {
        shieldAmount = config.getDouble("shield", shieldAmount)
        shieldPerLevel = config.getDouble("shield-per-level", shieldPerLevel)
    }
}
