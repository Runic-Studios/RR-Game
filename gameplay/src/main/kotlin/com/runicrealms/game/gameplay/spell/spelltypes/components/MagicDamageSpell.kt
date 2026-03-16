package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell that deals magic (spell) damage. Scaling is applied by SpellScalingListener. */
interface MagicDamageSpell {
    var magicDamage: Double
    var magicDamagePerLevel: Double

    fun loadMagicData(config: FileConfiguration) {
        magicDamage = config.getDouble("magic-damage", magicDamage)
        magicDamagePerLevel = config.getDouble("magic-damage-per-level", magicDamagePerLevel)
    }
}
