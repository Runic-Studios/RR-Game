package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.common.ALTERRA_NAME
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterJoinEvent
import com.runicrealms.game.data.event.GameCharacterPreLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.event.GamePlayerJoinEvent
import com.runicrealms.game.data.event.GamePlayerQuitEvent
import com.runicrealms.game.data.extension.toTrove
import com.runicrealms.game.gameplay.character.util.SaveZoneRegistry
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
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
    private val saveZoneRegistry: SaveZoneRegistry,
) : Listener {

    companion object {
        private val SPAWN_BOX = Location(Bukkit.getWorld(ALTERRA_NAME), -2271.5, 1.0, 2289.5)
    }

    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    // This is basically a map of all the players that have pressed the "create" character button,
    // and are currently waiting for their character to be created.
    // Necessary to use GameCharacterPreLoadEvent to set the character's class
    val creationCharacterTypes = ConcurrentHashMap<UUID, ClassType>()

    private val isLoading = HashSet<UUID>()

    fun setLoading(player: Player, loading: Boolean) {
        if (loading) {
            if (isLoading.contains(player.uniqueId)) return
            isLoading.add(player.uniqueId)
            plugin.launch {
                var dots = 0
                while (isLoading.contains(player.uniqueId)) {
                    val dotsText = ".".repeat(dots)
                    player.showTitle(
                        Title.title(
                            "&aLoading$dotsText".colorFormat(),
                            Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(10), Duration.ZERO),
                        )
                    )
                    dots++
                    dots %= 4
                    delay(200)
                }
            }
        } else {
            if (isLoading.remove(player.uniqueId)) player.clearTitle()
        }
    }

    private fun sendToSelection(player: Player) {
        player.inventory.clear()
        player.isInvulnerable = true
        player.foodLevel = 20
        player.teleport(SPAWN_BOX)
        player.gameMode = GameMode.SURVIVAL
        setLoading(player, true)
    }

    private fun openSelectionMenu(player: Player) {
        plugin.launch {
            val traits = userDataRegistry.loadUserCharactersTraits(player.uniqueId)
            if (traits == null) {
                player.sendMessage("&cFailed to load your characters traits!".colorFormat())
                return@launch
            }
            if (!player.isOnline) return@launch
            odalitaMenus.openMenu(characterSelectMenuFactory.create(traits), player)
            setLoading(player, false)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Before any player data is loaded
        sendToSelection(event.player)
    }

    @EventHandler
    fun onGameCharacterQuit(event: GameCharacterQuitEvent) {
        // When a player quits, or tries to select a new character
        if (!event.isOnLogout) {
            sendToSelection(event.character.bukkitPlayer)
            openSelectionMenu(event.character.bukkitPlayer)
        }
    }

    @EventHandler
    fun onGamePlayerJoin(event: GamePlayerJoinEvent) {
        // After we have loaded player data
        openSelectionMenu(event.player.bukkitPlayer)
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

            // Set initial traits
            traits.data.classType = classType
            traits.data.exp = 0
            traits.data.level = 0
            traits.data.location = saveZoneRegistry.TUTORIAL.location.toTrove()

            stageChanges(traits)
        }
    }

    @EventHandler
    fun onGameCharacterJoin(event: GameCharacterJoinEvent) {
        event.character.bukkitPlayer.isInvulnerable = false
    }
}
