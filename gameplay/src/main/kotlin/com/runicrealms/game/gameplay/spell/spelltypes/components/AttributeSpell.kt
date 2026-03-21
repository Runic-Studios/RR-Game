package com.runicrealms.game.gameplay.spell.spelltypes.components

import org.bukkit.configuration.file.FileConfiguration

/** A spell that scales off a player stat attribute (e.g. intelligence). */
interface AttributeSpell {
    /** The stat identifier (e.g. "intelligence"). */
    var attribute: String
    /** Base value before stat scaling. */
    var attributeBaseValue: Double
    /** Multiplied by the stat value and added to the base. */
    var attributeMultiplier: Double

    fun loadAttributeData(config: FileConfiguration) {
        attribute = config.getString("attribute", attribute) ?: attribute
        attributeBaseValue = config.getDouble("attribute-base-value", attributeBaseValue)
        attributeMultiplier = config.getDouble("attribute-multiplier", attributeMultiplier)
    }
}
