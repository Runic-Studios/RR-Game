package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Subcommand
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@CommandAlias("character|char")
@CommandPermission("runic.op")
class CharacterCommand
@Inject
constructor(commandManager: PaperCommandManager, private val userDataRegistry: UserDataRegistry, private val plugin: Plugin) :
    BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Conditions("is-op")
    @Subcommand("select")
    fun onSelectCommand(player: Player, args: Array<String?>) {
        val slot = if (args.isEmpty()) null else args[0]?.toIntOrNull()
        val slotString = slot?.toString() ?: "EMPTY"
        player.sendMessage(
            Component.text(
                "Switching to character $slotString",
                Style.style(NamedTextColor.GOLD, TextDecoration.BOLD),
            )
        )
        plugin.launch {
            val success = userDataRegistry.setCharacter(player.uniqueId, slot)
            if (!success) {
                player.sendMessage(
                    Component.text(
                        "Could not switch to character $slotString, check console for details",
                        NamedTextColor.RED,
                    )
                )
            }
        }
    }

    //    @Conditions("is-op")
    //    @Subcommand("create")
    //    fun onCreateCommand(player: Player, args: Array<String?>) {
    //
    //    }

}
