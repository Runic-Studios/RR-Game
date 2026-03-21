package com.runicrealms.game.tools.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.google.inject.Inject
import com.runicrealms.game.tools.WARP_PREFIX
import com.runicrealms.game.tools.WarpManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent

@CommandAlias("runic|r")
@CommandPermission("runic.warp")
class WarpCommand
@Inject
constructor(commandManager: PaperCommandManager, private val warpManager: WarpManager) :
    BaseCommand() {

    init {
        commandManager.commandCompletions.registerAsyncCompletion("warps") { context ->
            warpManager.getWarps().filter { it.startsWith(context.input) }
        }
        commandManager.registerCommand(this)
    }

    @Subcommand("warp")
    @Conditions("is-player")
    @CommandCompletion("@warps @nothing")
    @Syntax("<warp name>")
    fun onWarp(player: Player, args: Array<String>) {
        if (args.size != 1) {
            player.sendMessage(
                WARP_PREFIX.append(Component.text("/runic warp <warp name>", NamedTextColor.RED))
            )
            return
        }

        val location = warpManager.getWarp(args[0])

        if (location == null || !location.isWorldLoaded) {
            player.sendMessage(
                WARP_PREFIX.append(
                    Component.text("You have entered an invalid warp!", NamedTextColor.RED)
                )
            )
            return
        }

        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
        player.sendMessage(
            WARP_PREFIX.append(
                Component.text("You have warped to ${args[0]}!", NamedTextColor.GREEN)
            )
        )
    }

    @Subcommand("setwarp add")
    @Conditions("is-player")
    @CommandCompletion("@nothing")
    @Syntax("<warp name>")
    fun onSetWarpAdd(player: Player, args: Array<String>) {
        if (args.size != 1) {
            player.sendMessage(
                WARP_PREFIX.append(
                    Component.text("/runic setwarp add <warp name>", NamedTextColor.RED)
                )
            )
            return
        }

        if (warpManager.addWarp(args[0], player.location)) {
            player.sendMessage(
                WARP_PREFIX.append(
                    Component.text("Successfully added the ${args[0]} warp!", NamedTextColor.GREEN)
                )
            )
        } else {
            player.sendMessage(
                WARP_PREFIX.append(
                    Component.text("The ${args[0]} warp already exists!", NamedTextColor.RED)
                )
            )
        }
    }

    @Subcommand("setwarp remove")
    @CommandCompletion("@warps @nothing")
    @Syntax("<warp name>")
    fun onSetWarpRemove(sender: CommandSender, args: Array<String>) {
        if (args.size != 1) {
            sender.sendMessage(
                WARP_PREFIX.append(
                    Component.text("/runic setwarp remove <warp name>", NamedTextColor.RED)
                )
            )
            return
        }

        if (warpManager.removeWarp(args[0])) {
            sender.sendMessage(
                WARP_PREFIX.append(
                    Component.text(
                        "Successfully removed the ${args[0]} warp!",
                        NamedTextColor.GREEN,
                    )
                )
            )
        } else {
            sender.sendMessage(
                WARP_PREFIX.append(
                    Component.text("The ${args[0]} warp does not exist!", NamedTextColor.RED)
                )
            )
        }
    }

    @Subcommand("warps")
    fun onWarps(sender: CommandSender) {
        val builder =
            Component.text()
                .append(WARP_PREFIX)
                .append(Component.text("Available warps:", NamedTextColor.GREEN))

        val iterator = warpManager.getWarps().iterator()

        while (iterator.hasNext()) {
            val warp = iterator.next()
            builder.append(
                Component.text(" $warp${if (iterator.hasNext()) "," else ""}", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.suggestCommand("/runic warp $warp"))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text("Click here to warp to $warp!", NamedTextColor.GREEN)
                        )
                    )
            )
        }

        sender.sendMessage(builder.build())
    }
}
