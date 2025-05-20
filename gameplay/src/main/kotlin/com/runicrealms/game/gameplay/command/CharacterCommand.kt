package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@CommandAlias("runic|r")
@Subcommand("character|char")
class CharacterCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val userDataRegistry: UserDataRegistry,
    private val plugin: Plugin,
) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Default
    @CatchUnknown
    fun onCommand(player: Player) {
        player.sendMessage("&aLogging you out...".colorFormat())
        plugin.launch {
            val success = userDataRegistry.setCharacter(player.uniqueId, null)
            if (!success) {
                player.sendMessage("&cCould not switch your characters! Try again later.".colorFormat())
            }
        }
    }

}
