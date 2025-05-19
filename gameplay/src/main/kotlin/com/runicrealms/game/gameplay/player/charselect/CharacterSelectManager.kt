package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.common.WORLD_NAME
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterPreLoadEvent
import com.runicrealms.game.data.event.GamePlayerJoinEvent
import com.runicrealms.game.data.event.GamePlayerQuitEvent
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin

class CharacterSelectManager
@Inject
constructor(
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val characterSelectMenuFactory: CharacterSelectMenu.Factory,
) : Listener {

    companion object {
        private val SPAWN_BOX = Location(Bukkit.getWorld(WORLD_NAME), -2271.5, 2.0, 2289.5)
    }

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    // This is basically a map of all the players that have pressed the "create" character button,
    // and are currently waiting for their character to be created.
    // Necessary to use GameCharacterPreLoadEvent to set the character's class
    val creationCharacterTypes = ConcurrentHashMap<UUID, ClassType>()

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        // Runs before any data has been loaded!
        event.joinMessage(Component.text(""))
        event.player.inventory.clear()
        event.player.isInvulnerable = true
        event.player.level = 0
        event.player.exp = 0F
        event.player.foodLevel = 20
        event.player.teleport(SPAWN_BOX)
        // TODO display loading characters
        event.player.sendMessage("&aLoading your character information...".colorFormat())
    }

    @EventHandler
    fun onPlayerJoin(event: GamePlayerJoinEvent) {
        plugin.launch {
            val traits =
                userDataRegistry.loadUserCharactersTraits(event.player.bukkitPlayer.uniqueId)
            if (traits == null) {
                event.player.bukkitPlayer.sendMessage(
                    "&cFailed to load your characters traits!".colorFormat()
                )
                return@launch
            }
            characterSelectMenuFactory.create(event.player.bukkitPlayer, traits).openSelect()
        }
    }

    @EventHandler
    fun onQuit(event: GamePlayerQuitEvent) {
        creationCharacterTypes.remove(event.player.bukkitPlayer.uniqueId)
    }

    // For players creating characters
    @EventHandler
    fun onCharacterPreLoad(event: GameCharacterPreLoadEvent) {
        with(event.characterData) {
            if (!empty) return
            val classType = creationCharacterTypes.remove(event.user) ?: return
            traits.data.classType = classType
            traits.data.exp = 0
            traits.data.level = 0
            stageChanges(traits)
        }
    }
}
