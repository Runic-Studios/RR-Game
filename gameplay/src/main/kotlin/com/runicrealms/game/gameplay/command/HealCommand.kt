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
import com.runicrealms.game.gameplay.player.RegenManager
import com.runicrealms.game.gameplay.spell.SpellManager
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("runic|r")
@CommandPermission("runic.op")
class HealCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val userDataRegistry: UserDataRegistry,
    private val regenManager: RegenManager,
    private val spellManager: SpellManager,
) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Subcommand("heal")
    @Syntax("[player]")
    @CommandCompletion("@players")
    fun onHeal(sender: CommandSender, args: Array<String>) {
        val target: Player =
            if (args.isEmpty()) {
                if (sender is Player) {
                    sender
                } else {
                    sender.sendMessage("&cYou must specify a player name.".colorFormat())
                    return
                }
            } else {
                val found = Bukkit.getPlayer(args[0])
                if (found == null) {
                    sender.sendMessage("&cPlayer '${args[0]}' is not online.".colorFormat())
                    return
                }
                found
            }

        val maxHealth = target.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        target.health = maxHealth

        val character = userDataRegistry.getCharacter(target.uniqueId)
        if (character != null) {
            spellManager.setMana(target.uniqueId, regenManager.calculateMaxMana(character))
        }

        target.sendMessage("&aYou have been restored to full health and mana.".colorFormat())
        if (sender != target) {
            sender.sendMessage("&aRestored ${target.name} to full health and mana.".colorFormat())
        }
    }
}
