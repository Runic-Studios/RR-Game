package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.google.inject.Inject
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.extension.getClassTypeFromIdentifier
import com.runicrealms.game.gameplay.character.util.CharacterHealthHelper
import com.runicrealms.game.gameplay.character.util.CharacterLevelHelper
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

@CommandAlias("runic|r")
@CommandPermission("runic.op")
class SetClassCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val characterHealthHelper: CharacterHealthHelper,
    private val characterLevelHelper: CharacterLevelHelper,
) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Subcommand("setclass|sc")
    @Syntax("<player> <class>")
    @CommandCompletion("@players @nothing")
    fun onSetClass(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("&cUsage: /runic setclass <player> <class>".colorFormat())
            return
        }
        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("&cPlayer '${args[0]}' is not online.".colorFormat())
            return
        }
        val classType = getClassTypeFromIdentifier(args[1])
        if (classType == null || classType == ClassType.ANY) {
            sender.sendMessage(
                "&c'${args[1]}' is not a valid class. Valid: archer, cleric, mage, rogue, warrior.".colorFormat()
            )
            return
        }
        val character = userDataRegistry.getCharacter(target.uniqueId)
        if (character == null) {
            sender.sendMessage(
                "&c${target.name} does not have an active character loaded.".colorFormat()
            )
            return
        }
        val resetExp = characterLevelHelper.calculateTotalExp(0).toLong().coerceAtLeast(0L)
        character.withSyncCharacterData {
            traits.classType = classType
            traits.level = 0
            traits.exp = resetExp
            traits.subClassType = null
        }
        // Switch to MC main thread to update Bukkit state and recalculate health for the new class
        Bukkit.getScheduler().runTask(plugin, Runnable {
            target.level = 0
            target.exp = 0.0F
            characterHealthHelper.setCharacterMaxHealth(character)
        })
        val displayName = classType.name.lowercase().replaceFirstChar { it.uppercase() }
        sender.sendMessage("&aSet ${target.name}'s class to &f$displayName&a.".colorFormat())
        if (sender != target) {
            target.sendMessage("&aYour class has been set to &f$displayName&a by an admin.".colorFormat())
        }
    }
}
