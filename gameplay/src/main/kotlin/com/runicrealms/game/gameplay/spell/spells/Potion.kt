package com.runicrealms.game.gameplay.spell.spells

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.items.config.item.GameItemTag
import com.runicrealms.game.items.event.GameItemGenericTriggerEvent
import java.util.UUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Manages the cooldown for potion usage with a hotbar display.
 *
 * Original Java source: spellapi/spells/Potion.java
 */
class Potion(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.ANY, deps) {

    private val potionDrinkers: MutableSet<UUID> = HashSet()

    override var cooldown = POTION_COOLDOWN
    override var manaCost = 0
    override val description: String
        get() = "Internal: manages potion cooldown display."

    init {
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        potionDrinkers.add(player.uniqueId)
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { potionDrinkers.remove(player.uniqueId) },
            (cooldown * 20L).toLong(),
        )
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPotionConsume(event: GameItemGenericTriggerEvent) {
        if (!potionDrinkers.contains(event.player.uniqueId)) return
        if (!event.item.template.tags.contains(GameItemTag.POTION)) return
        event.isCancelled = true
        event.player.sendMessage(
            Component.text("Use of potions is on cooldown!", NamedTextColor.RED)
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onConsumableUse(event: GameItemGenericTriggerEvent) {
        if (event.isCancelled) return
        if (!event.item.template.tags.contains(GameItemTag.POTION)) return
        Bukkit.getPluginManager().callEvent(SpellCastEvent(event.player, this))
    }

    companion object {
        const val SPELL_NAME = "Potion"
        const val POTION_COOLDOWN = 30.0
    }
}
