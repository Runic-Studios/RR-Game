package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell that heals players. Scaling is applied by SpellScalingListener. */
interface HealingSpell {
    var healAmount: Double
    var healPerLevel: Double

    fun loadHealingData(config: FileConfiguration) {
        healAmount = config.getDouble("heal", healAmount)
        healPerLevel = config.getDouble("heal-per-level", healPerLevel)
    }
}
