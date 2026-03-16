package com.runicrealms.game.gameplay.spell.spells

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import org.bukkit.entity.Player

/**
 * Manages the cooldown for potion usage with a hotbar display.
 *
 * TODO: Implement onConsumableUse and onPotionConsume once RunicItemGenericTriggerEvent and the
 *   items module are migrated. The current stub does nothing functional.
 *
 * Original Java source: spellapi/spells/Potion.java
 */
class Potion(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.ANY, deps) {

    override var cooldown = POTION_COOLDOWN
    override var manaCost = 0
    override var description = "Internal: manages potion cooldown display."

    init {
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // TODO: Track potionDrinkers set and schedule removal after cooldown
        //       once RunicItemGenericTriggerEvent is available from items module.
    }

    companion object {
        const val SPELL_NAME = "Potion"
        const val POTION_COOLDOWN = 30.0
    }
}
