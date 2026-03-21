package com.runicrealms.game.gameplay.character

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.extension.toBukkit
import com.runicrealms.game.data.extension.toLocationData
import com.runicrealms.game.gameplay.character.util.CharacterHealthHelper
import com.runicrealms.game.gameplay.character.util.CharacterLevelHelper
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLevelChangeEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin

class CharacterTraitsManager
@Inject
constructor(
    plugin: Plugin,
    private val characterLevelHelper: CharacterLevelHelper,
    private val characterHealthHelper: CharacterHealthHelper,
    private val userDataRegistry: UserDataRegistry,
) : Listener {

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    @EventHandler
    fun onGameCharacterLoad(event: GameCharacterLoadEvent) {
        val player = event.character.bukkitPlayer
        event.character.withSyncCharacterData {
            // Set level and exp bar from persisted traits
            player.level = traits.level
            val totalExpAtLevel = characterLevelHelper.calculateTotalExp(traits.level)
            val totalExpToLevel = characterLevelHelper.calculateTotalExp(traits.level + 1)
            var proportion =
                (traits.exp - totalExpAtLevel) / (totalExpToLevel - totalExpAtLevel).toFloat()
            if (traits.level >= CharacterLevelHelper.MAX_LEVEL) player.exp = 0F
            if (proportion < 0) proportion = 0F
            if (proportion >= 1) proportion = 0.99F
            player.exp = proportion

            // Teleport to last known location
            player.teleport(traits.location.toBukkit(), PlayerTeleportEvent.TeleportCause.PLUGIN)
        }
        // Level is now set; apply max health from level + equipment
        characterHealthHelper.setCharacterMaxHealth(event.character)
    }

    @EventHandler
    fun onLevelUp(event: PlayerLevelChangeEvent) {
        val character = userDataRegistry.getCharacter(event.player.uniqueId) ?: return
        characterHealthHelper.setCharacterMaxHealth(character)
    }

    @EventHandler
    fun onGameCharacterQuit(event: GameCharacterQuitEvent) {
        // Persist current location back into the in-memory document.
        // The periodic save loop (or final save on logout) will write it to MongoDB.
        event.character.withSyncCharacterData {
            traits.location = event.character.bukkitPlayer.location.toLocationData()
        }
    }
}
