package com.runicrealms.game.gameplay.character

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.extension.toBukkit
import com.runicrealms.game.data.extension.toTrove
import com.runicrealms.game.gameplay.character.util.CharacterLevelHelper
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin

class CharacterTraitsManager
@Inject
constructor(plugin: Plugin, private val characterLevelHelper: CharacterLevelHelper) : Listener {

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    fun onGameCharacterLoad(event: GameCharacterLoadEvent) {
        val player = event.character.bukkitPlayer
        event.character.withSyncCharacterData {
            // Set level and exp
            player.level = traits.data.level
            val totalExpAtLevel = characterLevelHelper.calculateTotalExp(traits.data.level)
            val totalExpToLevel = characterLevelHelper.calculateTotalExp(traits.data.level + 1)
            var proportion =
                (traits.data.exp - totalExpAtLevel) / (totalExpToLevel - totalExpAtLevel).toFloat()
            if (traits.data.level >= CharacterLevelHelper.MAX_LEVEL) player.exp = 0F
            if (proportion < 0) proportion = 0F
            if (proportion >= 1) proportion = 0.99F
            player.exp = proportion

            // Set location
            player.teleport(
                traits.data.location.toBukkit(),
                PlayerTeleportEvent.TeleportCause.PLUGIN,
            )
        }
    }

    @EventHandler
    fun onGameCharacterQuit(event: GameCharacterQuitEvent) {
        // TODO store level/exp information

        // Store location
        event.character.withSyncCharacterData {
            traits.data.location = event.character.bukkitPlayer.location.toTrove()
            stageChanges(traits)
        }
    }
}
