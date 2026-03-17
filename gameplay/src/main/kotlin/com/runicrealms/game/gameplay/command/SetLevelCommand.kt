package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.character.util.CharacterLevelHelper
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@CommandAlias("runic|r")
@CommandPermission("runic.op")
class SetLevelCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val characterLevelHelper: CharacterLevelHelper,
) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Subcommand("setlevel|sl")
    @Syntax("<player> <level>")
    @CommandCompletion("@players @nothing")
    fun onSetLevel(sender: CommandSender, target: Player, level: Int) {
        val clamped = level.coerceIn(0, CharacterLevelHelper.MAX_LEVEL)
        val character = userDataRegistry.getCharacter(target.uniqueId)
        if (character == null) {
            sender.sendMessage("&c${target.name} does not have an active character loaded.".colorFormat())
            return
        }
        val newExp = characterLevelHelper.calculateTotalExp(clamped).toLong().coerceAtLeast(0L)
        character.withSyncCharacterData {
            traits.level = clamped
            traits.exp = newExp
        }
        Bukkit.getScheduler().runTask(plugin, Runnable { target.level = clamped })
        sender.sendMessage("&aSet ${target.name}'s level to &f$clamped&a.".colorFormat())
        if (sender != target) {
            target.sendMessage("&aYour level has been set to &f$clamped&a by an admin.".colorFormat())
        }
    }
}
