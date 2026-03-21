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
import com.runicrealms.game.items.loot.CustomTimedLootManager
import com.runicrealms.game.items.loot.LootManager
import com.runicrealms.game.items.loot.chest.RegenerativeLootChest
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
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
    private val customTimedLootManager: CustomTimedLootManager,
) : BaseCommand() {

    init {
        commandManager.commandCompletions.registerAsyncCompletion("chest-templates") { context ->
            if (!context.sender.isOp) return@registerAsyncCompletion emptySet()
            lootManager.getChestTemplates().map { it.identifier }.toSet()
        }
        commandManager.commandCompletions.registerAsyncCompletion("custom-timed-loot") { context ->
            if (!context.sender.isOp) return@registerAsyncCompletion emptySet()
            lootManager.getCustomTimedLootConfigs().keys
        }

        commandManager.registerCommand(this)
    }

    /**
     * Creates a regenerative loot chest at the block the player is looking at.
     *
     * Args: `<template-id> <min-level> <item-min-level> <item-max-level> [regen-time] [title]`
     *
     * Rarity-based regen-time defaults (seconds): common-chest=600, uncommon-chest=900,
     * rare-chest=1200, epic-chest=2700. All others default to 300s.
     */
    @Subcommand("create")
    @Syntax("<template-id> <min-level> <item-min-level> <item-max-level> [regen-time] [title...]")
    @CommandCompletion("@chest-templates * * * * *")
    fun onCommandCreate(player: Player, args: Array<String>) {
        if (args.size < 4) {
            player.sendMessage(
                "$PREFIX&dUsage: /runic lootchest create <template-id> <min-level> <item-min-level> <item-max-level> [regen-time] [title...]"
                    .colorFormat()
            )
            return
        }

        val templateID = args[0]
        if (lootManager.getChestTemplate(templateID) == null) {
            player.sendMessage("$PREFIX&dUnknown chest template '$templateID'.".colorFormat())
            return
        }

        val minLevel = args[1].toIntOrNull()
        if (minLevel == null || minLevel < 0) {
            player.sendMessage("$PREFIX&d<min-level> must be a non-negative integer.".colorFormat())
            return
        }

        val itemMinLevel = args[2].toIntOrNull()
        if (itemMinLevel == null || itemMinLevel < 1) {
            player.sendMessage(
                "$PREFIX&d<item-min-level> must be a positive integer.".colorFormat()
            )
            return
        }

        val itemMaxLevel = args[3].toIntOrNull()
        if (itemMaxLevel == null || itemMaxLevel < itemMinLevel) {
            player.sendMessage(
                "$PREFIX&d<item-max-level> must be >= <item-min-level>.".colorFormat()
            )
            return
        }

        val regenTime =
            if (args.size > 4) {
                args[4].toIntOrNull()
                    ?: run {
                        player.sendMessage(
                            "$PREFIX&d[regen-time] must be a positive integer (seconds)."
                                .colorFormat()
                        )
                        return
                    }
            } else {
                defaultRegenTime(templateID)
            }

        val title = if (args.size > 5) args.drop(5).joinToString(" ") else "Loot Chest"

        val targetBlock = player.getTargetBlockExact(5)
        if (targetBlock == null) {
            player.sendMessage("$PREFIX&dYou must be looking at a block.".colorFormat())
            return
        }

        val loc = targetBlock.location
        val existing =
            lootManager.getRegenerativeLootChestAt(
                loc.world?.name ?: return,
                loc.blockX,
                loc.blockY,
                loc.blockZ,
            )

        if (existing != null) {
            player.sendMessage(
                "$PREFIX&dA regenerative loot chest already exists at this location.".colorFormat()
            )
            return
        }

        val blockData = targetBlock.blockData
        val direction =
            if (blockData is org.bukkit.block.data.Directional) blockData.facing
            else BlockFace.NORTH

        val chest =
            RegenerativeLootChest(
                templateID = templateID,
                location =
                    Location(
                        loc.world,
                        loc.blockX.toDouble(),
                        loc.blockY.toDouble(),
                        loc.blockZ.toDouble(),
                    ),
                respawnTimeSeconds = regenTime,
                minLevel = minLevel,
                minItemLevel = itemMinLevel,
                maxItemLevel = itemMaxLevel,
                title = title,
                direction = direction,
            )

        lootManager.createRegenerativeLootChest(chest)
        player.sendMessage(
            "$PREFIX&dCreated loot chest '$templateID' at ${loc.blockX}, ${loc.blockY}, ${loc.blockZ} (level $minLevel+, regen ${regenTime}s)."
                .colorFormat()
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
        val chest =
            lootManager.getRegenerativeLootChestAt(
                loc.world?.name ?: return,
                loc.blockX,
                loc.blockY,
                loc.blockZ,
            )

        if (chest == null) {
            player.sendMessage(
                "$PREFIX&dNo regenerative loot chest found at this location.".colorFormat()
            )
            return
        }

        lootManager.deleteRegenerativeLootChest(chest)
        player.sendMessage(
            "$PREFIX&dDeleted regenerative loot chest at ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}."
                .colorFormat()
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
                "  &7${chest.templateID} &dat ${loc.world?.name}: ${loc.blockX}, ${loc.blockY}, ${loc.blockZ} (respawn: ${chest.respawnTimeSeconds}s)"
                    .colorFormat()
            )
        }
    }

    @Subcommand("reload")
    fun onCommandReload(sender: CommandSender) {
        lootManager.reload()
        sender.sendMessage("$PREFIX&dReloaded loot tables and chest templates.".colorFormat())
    }

    @Subcommand("customloot spawn")
    @Syntax("<identifier> <player>")
    @CommandCompletion("@custom-timed-loot @players")
    fun onCommandCustomLootSpawn(sender: CommandSender, identifier: String, targetName: String) {
        val config = lootManager.getCustomTimedLootConfig(identifier)
        if (config == null) {
            sender.sendMessage(
                "$PREFIX&dNo custom timed loot config found for '$identifier'.".colorFormat()
            )
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            sender.sendMessage("$PREFIX&dPlayer '$targetName' is not online.".colorFormat())
            return
        }

        val lootItems = lootManager.generateLootFromTemplate(config.templateID)
        val spawnLocation = config.chestLocation ?: target.location

        val loot = config.createCustomTimedLoot(lootItems, spawnLocation)
        customTimedLootManager.addCustomLoot(loot)
        lootManager.displayTimedLootChest(target, loot)

        sender.sendMessage(
            "$PREFIX&dSpawned custom loot '$identifier' for ${target.name}.".colorFormat()
        )
    }

    @Default
    @CatchUnknown
    fun onCommandHelp(sender: CommandSender) {
        sender.sendMessage("$PREFIX&dLoot Chest Commands:".colorFormat())
        sender.sendMessage(
            "$PREFIX&7/runic lootchest create <template> <min-level> <item-min> <item-max> [regen-time] [title...]"
                .colorFormat()
        )
        sender.sendMessage("$PREFIX&7/runic lootchest delete".colorFormat())
        sender.sendMessage("$PREFIX&7/runic lootchest list".colorFormat())
        sender.sendMessage("$PREFIX&7/runic lootchest reload".colorFormat())
        sender.sendMessage(
            "$PREFIX&7/runic lootchest customloot spawn <identifier> <player>".colorFormat()
        )
    }

    companion object {
        private const val PREFIX = "&5[RunicItems] &6» &r"

        private val REGEN_TIME_DEFAULTS =
            mapOf(
                "common-chest" to 600,
                "uncommon-chest" to 900,
                "rare-chest" to 1200,
                "epic-chest" to 2700,
            )

        private fun defaultRegenTime(templateID: String): Int {
            return REGEN_TIME_DEFAULTS[templateID] ?: 300
        }
    }
}
