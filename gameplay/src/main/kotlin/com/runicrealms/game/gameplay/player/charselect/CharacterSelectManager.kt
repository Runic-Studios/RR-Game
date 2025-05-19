package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.common.WORLD_NAME
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterPreLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.event.GamePlayerJoinEvent
import com.runicrealms.game.data.event.GamePlayerQuitEvent
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import nl.odalitadevelopments.menus.OdalitaMenus
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin

class CharacterSelectManager
@Inject
constructor(
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val odalitaMenus: OdalitaMenus,
    private val characterSelectMenuFactory: CharacterSelectMenu.Factory,
) : Listener {

    companion object {
        private val SPAWN_BOX = Location(Bukkit.getWorld(WORLD_NAME), -2271.5, 1.0, 2289.5)
    }

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    // This is basically a map of all the players that have pressed the "create" character button,
    // and are currently waiting for their character to be created.
    // Necessary to use GameCharacterPreLoadEvent to set the character's class
    val creationCharacterTypes = ConcurrentHashMap<UUID, ClassType>()

    private fun sendToSelection(player: Player) {
        player.inventory.clear()
        player.isInvulnerable = true
        player.level = 0
        player.exp = 0F
        player.foodLevel = 20
        player.teleport(SPAWN_BOX)
        player.gameMode = GameMode.SURVIVAL
        // TODO display loading characters
        player.sendMessage("&aLoading your character information...".colorFormat())
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Before any player data is loaded
        event.joinMessage(Component.text(""))
        sendToSelection(event.player)
    }

    @EventHandler
    fun onGameCharacterQuit(event: GameCharacterQuitEvent) {
        // When a player quits, or tries to select a new character
        sendToSelection(event.character.bukkitPlayer)
    }

    @EventHandler
    fun onGamePlayerJoin(event: GamePlayerJoinEvent) {
        // After we have loaded player data
        plugin.launch {
            val traits =
                userDataRegistry.loadUserCharactersTraits(event.player.bukkitPlayer.uniqueId)
            if (traits == null) {
                event.player.bukkitPlayer.sendMessage(
                    "&cFailed to load your characters traits!".colorFormat()
                )
                return@launch
            }
            if (!event.player.bukkitPlayer.isOnline) return@launch
            odalitaMenus.openMenu(
                characterSelectMenuFactory.create(traits),
                event.player.bukkitPlayer,
            )
        }
    }

    @EventHandler
    fun onGamePlayerQuit(event: GamePlayerQuitEvent) {
        // When a player quits
        creationCharacterTypes.remove(event.player.bukkitPlayer.uniqueId)
    }

    // For players creating characters
    @EventHandler
    fun onGameCharacterPreLoad(event: GameCharacterPreLoadEvent) {
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
