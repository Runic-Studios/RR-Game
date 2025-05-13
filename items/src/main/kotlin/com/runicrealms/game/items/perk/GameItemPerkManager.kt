package com.runicrealms.game.items.perk

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.items.config.perk.GameItemPerkTemplate
import com.runicrealms.game.items.event.ActiveItemPerksChangeEvent
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class GameItemPerkManager @Inject constructor(
    private val plugin: Plugin
): Listener, GameItemPerkHandlerRegistry {

    private val handlers = HashMap<GameItemPerkTemplate, GameItemPerkHandler>()
    private val types = HashMap<String, GameItemPerkTemplate>()

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    override fun registerItemPerk(handler: GameItemPerkHandler) {
        handlers[handler.template] = handler
        types[handler.template.identifier] = handler.template
    }

    @EventHandler
    suspend fun onActiveItemPerksChange(event: ActiveItemPerksChangeEvent) {
        val oldPerks = event.oldItemPerks.associateBy { it.perkID }
        val newPerks = event.newItemPerks.associateBy { it.perkID }

        var activated = false
        var deactivated = false

        for (handlerType in handlers.keys) {
            val oldCount = oldPerks[handlerType.identifier]?.stacks ?: 0
            val newCount = newPerks[handlerType.identifier]?.stacks ?: 0
            if (oldCount != newCount) {
                handlers[handlerType]?.updateActive(event.character, newCount)
                if (newCount > oldCount) {
                    activated = true
                } else {
                    deactivated = true
                }
            }
        }

        if (event.playSounds) {
            with (event.character.player) {
                if (activated && !deactivated) {
                    playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f)
                } else if (!activated && deactivated) {
                    playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 2.0f)
                } else if (activated) { // both activated and deactivated
                    playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 2.0f)
                    plugin.launch {
                        delay(500)
                        playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f)
                    }
                }
            }
        }
    }

    override fun getGameItemPerkHandler(template: GameItemPerkTemplate): GameItemPerkHandler? {
        return handlers[template]
    }

}