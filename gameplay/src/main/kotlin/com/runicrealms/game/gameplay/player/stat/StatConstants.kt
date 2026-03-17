package com.runicrealms.game.gameplay.player.stat

/** Multiplier constants applied by [StatListener] to game events based on character stats. */
object StatConstants {
    /** Dexterity: fraction of a spell's cooldown removed per stat point (0.4% per point). */
    const val ABILITY_HASTE = 0.004

    /** Intelligence: fraction of mana regen added per stat point (1.5% per point). */
    const val MANA_REGEN_MULT = 0.015

    /** Intelligence: fraction of magic damage added per stat point (1% per point). */
    const val MAGIC_DMG_MULT = 0.01

    /** Strength: fraction of physical damage added per stat point (0.75% per point). */
    const val PHYSICAL_DMG_MULT = 0.0075

    /** Wisdom: fraction of max mana added per stat point (1% per point). */
    const val MAX_MANA_MULT = 0.01

    /** Wisdom: fraction of spell healing added per stat point (1.2% per point). */
    const val SPELL_HEALING_MULT = 0.012

    /** Wisdom: fraction of spell shielding added per stat point (1.2% per point). */
    const val SPELL_SHIELDING_MULT = 0.012

    /** Vitality: fraction of incoming damage reduced per stat point (0.3% per point). */
    const val DAMAGE_REDUCTION_MULT = 0.003

    /** Maximum fraction of damage that can be mitigated by vitality (40%). */
    const val DAMAGE_REDUCTION_CAP = 0.40

    /** Vitality: fraction of health regen added per stat point (1% per point). */
    const val HEALTH_REGEN_MULT = 0.01
}
