package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GamePlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

@CommandAlias("character|char")
class CharacterCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val userDataRegistry: UserDataRegistry,
    private val plugin: Plugin,
) : BaseCommand(), Listener {

    companion object {
        const val COOLDOWN_MILLIS = 3000
    }

    init {
        commandManager.registerCommand(this)
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    @Default
    @CatchUnknown
    fun onCommand(player: Player) {
        if (cooldowns[player.uniqueId]?.let { System.currentTimeMillis() < it } == true) {
            player.sendMessage("&cPlease wait before doing this".colorFormat())
            return
        }
        cooldowns[player.uniqueId] = System.currentTimeMillis() + COOLDOWN_MILLIS
        plugin.launch {
            if (userDataRegistry.getCharacter(player.uniqueId) == null) {
                player.sendMessage("&cYou can't use this command right now.".colorFormat())
                return@launch
            }
            player.sendMessage("&aLogging you out...".colorFormat())
            val success = userDataRegistry.setCharacter(player.uniqueId, null)
            if (!success) {
                player.sendMessage(
                    "&cCould not switch your characters! Try again later.".colorFormat()
                )
            }
        }
    }

    @EventHandler
    fun onGamePlayerQuit(event: GamePlayerQuitEvent) {
        cooldowns.remove(event.player.bukkitPlayer.uniqueId)
    }
}
