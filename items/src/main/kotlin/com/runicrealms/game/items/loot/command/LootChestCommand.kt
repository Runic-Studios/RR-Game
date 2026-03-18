package com.runicrealms.game.items.loot.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.items.loot.LootManager
import com.runicrealms.game.items.loot.chest.RegenerativeLootChest
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("runic|r")
@Subcommand("lootchest|lc")
@CommandPermission("runic.op")
class LootChestCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val lootManager: LootManager,
) : BaseCommand() {

    init {
        commandManager.commandCompletions.registerAsyncCompletion("chest-templates") { context ->
            if (!context.sender.isOp) return@registerAsyncCompletion emptySet()
            lootManager.getLootTables().map { it.identifier }.toSet()
        }

        commandManager.registerCommand(this)
    }

    @Subcommand("create")
    @Syntax("<template-id>")
    @CommandCompletion("@chest-templates")
    fun onCommandCreate(player: Player, args: Array<String>) {
        if (args.isEmpty()) {
            player.sendMessage("$PREFIX&dUsage: /runic lootchest create <template-id>".colorFormat())
            return
        }

        val templateID = args[0]
        if (lootManager.getChestTemplate(templateID) == null) {
            player.sendMessage("$PREFIX&dUnknown chest template '$templateID'.".colorFormat())
            return
        }

        val targetBlock = player.getTargetBlockExact(5)
        if (targetBlock == null) {
            player.sendMessage("$PREFIX&dYou must be looking at a block.".colorFormat())
            return
        }

        val loc = targetBlock.location
        val existing = lootManager.getRegenerativeLootChestAt(
            loc.world?.name ?: return,
            loc.blockX,
            loc.blockY,
            loc.blockZ,
        )

        if (existing != null) {
            player.sendMessage("$PREFIX&dA regenerative loot chest already exists at this location.".colorFormat())
            return
        }

        val chest = RegenerativeLootChest(
            templateID = templateID,
            location = Location(loc.world, loc.blockX.toDouble(), loc.blockY.toDouble(), loc.blockZ.toDouble()),
            respawnTimeSeconds = 300,
            minItemLevel = 1,
            maxItemLevel = 60,
            title = "Loot Chest",
        )

        lootManager.createRegenerativeLootChest(chest)
        player.sendMessage(
            "$PREFIX&dCreated regenerative loot chest with template '$templateID' at ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}.".colorFormat()
        )
    }

    @Subcommand("delete")
    fun onCommandDelete(player: Player) {
        val targetBlock = player.getTargetBlockExact(5)
        if (targetBlock == null) {
            player.sendMessage("$PREFIX&dYou must be looking at a block.".colorFormat())
            return
        }

        val loc = targetBlock.location
        val chest = lootManager.getRegenerativeLootChestAt(
            loc.world?.name ?: return,
            loc.blockX,
            loc.blockY,
            loc.blockZ,
        )

        if (chest == null) {
            player.sendMessage("$PREFIX&dNo regenerative loot chest found at this location.".colorFormat())
            return
        }

        lootManager.deleteRegenerativeLootChest(chest)
        player.sendMessage(
            "$PREFIX&dDeleted regenerative loot chest at ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}.".colorFormat()
        )
    }

    @Subcommand("list")
    fun onCommandList(sender: CommandSender) {
        val chests = lootManager.getRegenerativeLootChests()

        if (chests.isEmpty()) {
            sender.sendMessage("$PREFIX&dNo regenerative loot chests found.".colorFormat())
            return
        }

        sender.sendMessage("$PREFIX&dRegenerative loot chests (${chests.size}):".colorFormat())
        for (chest in chests) {
            val loc = chest.location
            sender.sendMessage(
                "  &7${chest.templateID} &dat ${loc.world?.name}: ${loc.blockX}, ${loc.blockY}, ${loc.blockZ} (respawn: ${chest.respawnTimeSeconds}s)".colorFormat()
            )
        }
    }

    @Subcommand("reload")
    fun onCommandReload(sender: CommandSender) {
        lootManager.reload()
        sender.sendMessage("$PREFIX&dReloaded loot tables and chest templates.".colorFormat())
    }

    @Default
    @CatchUnknown
    fun onCommandHelp(sender: CommandSender) {
        sender.sendMessage("$PREFIX&dLoot Chest Commands:".colorFormat())
        sender.sendMessage("$PREFIX&7/runic lootchest create <template-id>".colorFormat())
        sender.sendMessage("$PREFIX&7/runic lootchest delete".colorFormat())
        sender.sendMessage("$PREFIX&7/runic lootchest list".colorFormat())
        sender.sendMessage("$PREFIX&7/runic lootchest reload".colorFormat())
    }

    companion object {
        private const val PREFIX = "&5[RunicItems] &6» &r"
    }
}
