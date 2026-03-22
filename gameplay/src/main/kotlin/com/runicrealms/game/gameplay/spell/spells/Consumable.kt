package com.runicrealms.game.gameplay.spell.spells

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import org.bukkit.entity.Player

/**
 * Stub for Consumable spell handling.
 *
 * The original Java source (spellapi/spells/Consumable.java) was entirely commented out and
 * contained dead code. No logic was migrated.
 *
 * TODO: Implement if/when Consumable mechanic is redesigned.
 */
class Consumable(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.ANY, deps) {

    override var cooldown = 0.0
    override var manaCost = 0
    override val description: String
        get() = "Internal: consumable cooldown stub."

    init {
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // TODO: Implement once Consumable mechanic is decided.
    }

    companion object {
        const val SPELL_NAME = "Consumable"
    }
}
