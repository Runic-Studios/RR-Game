package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Subcommand
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

@CommandAlias("character|char")
@CommandPermission("runic.op")
class CharacterCommand @Inject constructor(private val userDataRegistry: UserDataRegistry) :
    BaseCommand() {

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
        val success = runBlocking { userDataRegistry.setCharacter(player.uniqueId, slot) }
        if (!success) {
            player.sendMessage(
                Component.text(
                    "Could not switch to character $slotString, check console for details",
                    NamedTextColor.RED,
                )
            )
        }
    }

    //    @Conditions("is-op")
    //    @Subcommand("create")
    //    fun onCreateCommand(player: Player, args: Array<String?>) {
    //
    //    }

}
