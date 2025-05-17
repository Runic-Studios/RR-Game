package com.runicrealms.game.items.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.google.inject.Inject
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.common.toLegacy
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.extension.getClassTypeFromIdentifier
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.game.items.config.template.ClassTypeHolder
import com.runicrealms.game.items.config.template.GameItemRarityType
import com.runicrealms.game.items.config.template.GameItemTemplate
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.AddedStatsHolder
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import java.util.Locale
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("runic|r")
@Subcommand("item|i")
@CommandPermission("runic.op")
class ItemCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val itemTemplateRegistry: GameItemTemplateRegistry,
    private val perkTemplateRegistry: GameItemPerkTemplateRegistry,
    private val userDataRegistry: UserDataRegistry,
    private val itemStackConverter: ItemStackConverter,
    private val lootHelper: LootHelper,
    private val inventoryHelper: InventoryHelper,
) : BaseCommand() {
    init {
        val templateIdentifiers = itemTemplateRegistry.getItemTemplates().map { it.id }

        commandManager.commandCompletions.registerAsyncCompletion("item-ids") { context ->
            return@registerAsyncCompletion if (!context.sender.isOp) templateIdentifiers
            else emptySet()
        }

        commandManager.commandCompletions.registerAsyncCompletion("flag-parameter") { context ->
            val args = context.getContextValue(Array<String>::class.java)
            val list = mutableListOf<String>()

            for (flag in GetRandomFlag.entries) {
                var contains = false
                for (arg in args) {
                    if (
                        !arg.startsWith("-") ||
                            !flag.text.equals(arg.substring(1), ignoreCase = true)
                    )
                        continue
                    contains = true
                    break
                }
                if (!contains) {
                    list.add("-" + flag.text)
                }
            }
            list
        }

        commandManager.commandCompletions.registerAsyncCompletion("value-parameter") { context ->
            val index = context.config.toInt()
            val args = context.getContextValue(Array<String>::class.java)
            val rawFlag = args[index - 1]
            if (!rawFlag.startsWith("-")) {
                return@registerAsyncCompletion emptyList()
            }
            val flag =
                GetRandomFlag.getFlag(rawFlag.substring(1))
                    ?: return@registerAsyncCompletion emptyList()
            flag.complete(context.input)
        }

        val perkIdentifiers = perkTemplateRegistry.getPerkTemplates().map { it.identifier }

        commandManager.commandCompletions.registerAsyncCompletion("perk-ids") { context ->
            return@registerAsyncCompletion if (context.sender.isOp) perkIdentifiers else emptySet()
        }

        // TODO
        //        commandManager.commandCompletions.registerAsyncCompletion("loot-tables") { context
        // ->
        //            if (!context.sender.isOp) return@registerAsyncCompletion emptySet()
        //            RunicItems.getLootAPI().getLootTables().stream().map(LootTable::getIdentifier)
        //                .collect(Collectors.toSet<T>())
        //        }

        commandManager.registerCommand(this)
    }

    @Subcommand("clear|c")
    @Conditions("is-op")
    @Syntax("<player> [item] [amount]")
    @CommandCompletion("@players @item-ids @nothing")
    fun onCommandClear(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }
        val target = Bukkit.getPlayerExact(args[0])
        if (target == null) {
            sender.sendMessage("$PREFIX&dThat is not a valid player!".colorFormat())
            return
        }
        var template: GameItemTemplate? = null
        if (args.size >= 2) {
            template = itemTemplateRegistry.getItemTemplate(args[1])
            if (template == null) {
                sender.sendMessage("$PREFIX&dThat item ID does not exist!".colorFormat())
                return
            }
        }
        var amount = -1
        if (args.size >= 3) {
            if (!isInt(args[2])) {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
            amount = args[2].toInt()
            if (amount < 1) {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
        }

        inventoryHelper.clearInventory(target.inventory, amount, template, sender)

        sender.sendMessage("$PREFIX&dCleared items from player's inventory!".colorFormat())
    }

    @Subcommand("drop")
    @Conditions("is-op")
    @Syntax("<item> <location> [amount]")
    @CommandCompletion("@item-ids @nothing @nothing")
    fun onCommandDrop(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }
        val template = itemTemplateRegistry.getItemTemplate(args[0])
        var count = 1
        if (template == null) {
            sender.sendMessage("$PREFIX&dThat item ID does not exist!".colorFormat())
            return
        }
        val splitLocation =
            args[1].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val location =
            Location(
                Bukkit.getWorld(splitLocation[0]),
                splitLocation[1].toDouble(),
                splitLocation[2].toDouble(),
                splitLocation[3].toDouble(),
            )
        if (args.size >= 3) {
            if (isInt(args[2])) {
                count = args[2].toInt()
            } else {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
        }
        val item = itemTemplateRegistry.generateGameItem(template).generateItemStack(count)
        location.getWorld().dropItem(location, item)
    }

    @Subcommand("drop-range")
    @Conditions("is-op")
    @Syntax("<min-level> <max-level> <location> [amount]")
    @CommandCompletion("@range:0-60 @range:0-60 @nothing @nothing")
    fun onCommandDropRange(sender: CommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }
        if (!isInt(args[0]) || !isInt(args[1])) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Level min and level max must be integers!".colorFormat()
            )
            return
        }
        val template = lootHelper.getRandomItemInRange(args[0].toInt(), args[1].toInt())
        var count = 1
        if (template == null) {
            sender.sendMessage("$PREFIX&dThat item ID does not exist!".colorFormat())
            return
        }
        val splitLocation =
            args[2].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val location =
            Location(
                Bukkit.getWorld(splitLocation[0]),
                splitLocation[1].toDouble(),
                splitLocation[2].toDouble(),
                splitLocation[3].toDouble(),
            )
        if (args.size >= 4) {
            if (isInt(args[3])) {
                count = args[3].toInt()
            } else {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
        }
        val item = itemTemplateRegistry.generateGameItem(template).generateItemStack(count)
        location.getWorld().dropItem(location, item)
    }

    // TODO
    //    @Subcommand("drop-lt")
    //    @Conditions("is-op")
    //    @Syntax("<loot-table> <min-level> <max-level> <location>")
    //    @CommandCompletion("@loot-tables @nothing")
    //    fun onCommandDropLootTable(sender: CommandSender, args: Array<String>) {
    //        if (args.size != 4) {
    //            sender.sendMessage("$PREFIX&dInvalid syntax! Please check &7/runicitem
    // help".colorFormat())
    //            return
    //        }
    //        val table: LootTable? = RunicItems.getLootAPI().getLootTable(args[0])
    //        if (table == null) {
    //            sender.sendMessage("$PREFIX&dThat loot table does not exist!".colorFormat())
    //            return
    //        }
    //
    //        val minLevel = args[1].toInt()
    //        val maxLevel = args[2].toInt()
    //
    //        val splitLocation = args[3].split(",".toRegex()).dropLastWhile { it.isEmpty()
    // }.toTypedArray()
    //        val location = Location(
    //            Bukkit.getWorld(splitLocation[0]),
    //            splitLocation[1].toDouble(),
    //            splitLocation[2].toDouble(),
    //            splitLocation[3].toDouble()
    //        )
    //        val item: ItemStack = table.generateLoot(GenericLootHolder(minLevel, maxLevel))
    //        location.getWorld().dropItem(location, item)
    //    }

    @Subcommand("dupe-item")
    @Conditions("is-op")
    fun onCommandDupeItem(player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            player.sendMessage("$PREFIX&dYou are not holding an item!".colorFormat())
            return
        }
        var slot = -1
        for (i in 0..34) {
            val slotItem = player.inventory.getItem(i)
            if (slotItem == null || slotItem.type == Material.AIR) {
                slot = i
                break
            }
        }
        if (slot != -1) {
            player.inventory.setItem(slot, item)
            player.sendMessage("$PREFIX&dAdded duped item to your inventory!".colorFormat())
        } else {
            player.sendMessage("$PREFIX&dYou do not have space in your inventory!".colorFormat())
        }
    }

    @Subcommand("get")
    @Conditions("is-player|is-op")
    @Syntax("<item> [amount]")
    @CommandCompletion("@item-ids @nothing")
    fun onCommandGet(player: Player, args: Array<String>) {
        if (args.size == 0) {
            player.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }
        val template = itemTemplateRegistry.getItemTemplate(args[0])
        var count = 1
        if (template == null) {
            player.sendMessage("$PREFIX&dThat item ID does not exist!".colorFormat())
            return
        }
        if (args.size >= 2) {
            if (isInt(args[1])) {
                count = args[1].toInt()
            } else {
                player.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
        }
        val item = itemTemplateRegistry.generateGameItem(template).generateItemStack(count)
        player.inventory.addItem(item)
        // TODO check works
        //        RunicItemsAPI.addItem(player.inventory, item)
    }

    @Subcommand("get-range")
    @Conditions("is-player|is-op")
    @Syntax("<level-min> <level-max> [amount]")
    @CommandCompletion("@range:0-60 @range:0-60 @nothing")
    fun onCommandGetRange(player: Player, args: Array<String>) {
        if (args.size == 1) {
            player.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }
        if (!isInt(args[0]) || !isInt(args[1])) {
            player.sendMessage(
                "$PREFIX&dInvalid syntax! Level min and level max must be integers!".colorFormat()
            )
            return
        }
        val template = lootHelper.getRandomItemInRange(args[0].toInt(), args[1].toInt())
        var count = 1
        if (template == null) {
            player.sendMessage("$PREFIX&dThat item ID does not exist!".colorFormat())
            return
        }
        if (args.size >= 3) {
            if (isInt(args[2])) {
                count = args[2].toInt()
            } else {
                player.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
        }
        val item = itemTemplateRegistry.generateGameItem(template)
        // TODO check works
        player.inventory.addItem(item.generateItemStack(count))
        player.sendMessage(
            "$PREFIX&dGiven you &5${count}x &r${item.template.display.name.toLegacy()}"
                .colorFormat()
        )
    }

    @Subcommand("give")
    @Conditions("is-op")
    @Syntax("<player> <item> [amount]")
    @CommandCompletion("@players @item-ids @nothing")
    fun onCommandGive(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }
        val target = Bukkit.getPlayerExact(args[0])
        if (target == null) {
            sender.sendMessage("$PREFIX&dThat is not a valid player!".colorFormat())
            return
        }
        val template = itemTemplateRegistry.getItemTemplate(args[1])
        var count = 1
        if (template == null) {
            sender.sendMessage("$PREFIX&dThat item ID does not exist!".colorFormat())
            return
        }
        if (args.size >= 3) {
            if (!isInt(args[2])) {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
            count = args[2].toInt()
            if (count < 1) {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
        }

        val item = itemTemplateRegistry.generateGameItem(template)
        if (args.size == 4) {
            item.setCustomData("dynamic", args[3])
        }
        // TODO check works
        //        RunicItemsAPI.addItem(target.inventory, item.generateItemStack(count))
        target.inventory.addItem(item.generateItemStack(count))
    }

    @Subcommand("give-range")
    @Conditions("is-op")
    @Syntax("<player> <level-min> <level-max> [amount]")
    @CommandCompletion("@players @range:0-60 @range:0-60 @nothing")
    fun onCommandGiveRange(sender: CommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }
        val target = Bukkit.getPlayerExact(args[0])
        if (target == null) {
            sender.sendMessage("$PREFIX&dThat is not a valid player!".colorFormat())
            return
        }
        if (!isInt(args[1]) || !isInt(args[2])) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Level min and level max must be integers!".colorFormat()
            )
            return
        }
        val template = lootHelper.getRandomItemInRange(args[1].toInt(), args[2].toInt())
        var count = 1
        if (template == null) {
            sender.sendMessage("$PREFIX&dThat item ID does not exist!".colorFormat())
            return
        }
        if (args.size >= 4) {
            if (!isInt(args[3])) {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
            count = args[3].toInt()
            if (count < 1) {
                sender.sendMessage("$PREFIX&dThat is not a valid amount!".colorFormat())
                return
            }
        }
        val item = itemTemplateRegistry.generateGameItem(template)
        // TODO check works
        target.inventory.addItem(item.generateItemStack(count))
        //        RunicItemsAPI.addItem(target.inventory, item.generateItemStack(count))
    }

    @Default
    @CatchUnknown
    @Conditions("is-op")
    @Subcommand("help|h")
    fun onCommandHelp(sender: CommandSender) {
        sender.sendMessage("$PREFIX&dAvailable commands: ".colorFormat())
        sender.sendMessage("$PREFIX&7/runicitem help".colorFormat())
        sender.sendMessage("$PREFIX&7/runicitem get <item> [amount]".colorFormat())
        sender.sendMessage("$PREFIX&7/runicitem give <player> <item> [amount]".colorFormat())
        sender.sendMessage("$PREFIX&7/runicitem clear <player> [item] [amount]".colorFormat())
        sender.sendMessage(
            "$PREFIX&7/runicitem toggle-database &dWARNING - don't use if you don't know what this does!"
                .colorFormat()
        )
    }

    @Subcommand("picker")
    @Conditions("is-player|is-op")
    @Syntax("<player> <item> <item> <item> <item> <item>")
    @CommandCompletion("@online @item-ids @item-ids item-ids item-ids item-ids")
    fun onCommandPicker(sender: CommandSender, args: Array<String?>) {
        if (args.size != 6) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }

        val target = Bukkit.getPlayerExact(args[0]!!)
        if (target == null) {
            sender.sendMessage(
                "$PREFIX&dInvalid syntax! Please check &7/runicitem help".colorFormat()
            )
            return
        }

        val classType = runBlocking {
            userDataRegistry.getCharacter(target.uniqueId)?.withCharacterData {
                traits.data.classType
            }
        }

        if (classType == null) {
            sender.sendMessage(
                "$PREFIX&dCannot run picker because $target is not a character".colorFormat()
            )
            return
        }

        for (i in 1..5) {
            val template = itemTemplateRegistry.getItemTemplate(args[i] ?: continue) ?: continue

            var itemClass = (template as? ClassTypeHolder)?.classType
            if (itemClass == null || itemClass != classType) continue
            // TODO check works
            target.inventory.addItem(
                itemTemplateRegistry.generateGameItem(template).generateItemStack(1)
            )
            //            RunicItemsAPI.addItem(
            //                target.inventory,
            //                itemTemplateRegistry.generateGameItem(template,
            // 1).generateItemStack(),
            //                target.location
            //            )
            return
        }
    }

    @Subcommand("set-perk")
    @Conditions("is-player|is-op")
    @CommandCompletion("@perk-ids")
    private fun onSetPerk(player: Player, args: Array<String>) {
        if (args.size != 2) {
            player.sendMessage(
                "$PREFIX&dYou must specify a perk type and the amount of stacks!".colorFormat()
            )
            return
        }

        val type = perkTemplateRegistry.getPerkTemplate(args[0])

        if (type == null) {
            player.sendMessage("$PREFIX&d${args[0]} is not a valid perk type!".colorFormat())
            return
        }

        val heldItem = player.inventory.itemInMainHand
        if (heldItem.type == Material.AIR) {
            player.sendMessage(
                "$PREFIX&dYou must be holding an item to add a perk to it!".colorFormat()
            )
            return
        }

        val count = heldItem.amount
        val item = itemStackConverter.convertToGameItem(heldItem)
        if (item !is AddedStatsHolder) {
            player.sendMessage(
                "$PREFIX&dYou must be holding armor, weapon or offhand to add a perk to it."
                    .colorFormat()
            )
            return
        }

        if (!isInt(args[1]) || args[1].toInt() < 0) {
            player.sendMessage("$PREFIX&dThe number of stacks must be an integer!".colorFormat())
            return
        }

        val stacks = args[1].toInt()

        // inefficient i know but i don't care it only runs on command and is async
        val perks = item.addedStats.perks ?: mutableSetOf()
        val newPerks = LinkedHashSet<ItemData.Perk>()

        for (perk in perks) {
            if (perk.perkID != type.identifier) {
                newPerks.add(perk)
            }
        }
        if (stacks > 0) {
            newPerks.add(
                ItemData.Perk.newBuilder().setPerkID(type.identifier).setStacks(stacks).build()
            )
        }

        item.addedStats.perks?.clear()
        newPerks.forEach { item.addedStats.addPerk(it) }

        player.inventory.setItemInMainHand(item.generateItemStack(count))

        player.sendMessage("$PREFIX&dSet ${args[0]} to ${stacks}x on your held item.".colorFormat())
    }

    @Subcommand("get-random")
    @Conditions("is-player|is-op")
    @CommandCompletion(
        "@flag-parameter:0 @value-parameter:1 @flag-parameter:2 @value-parameter:3 @flag-parameter:4 @value-parameter:5 @flag-parameter:6 @value-parameter:7 @flag-parameter:8 @value-parameter:9 @nothing"
    )
    private fun onGetRandom(player: Player, args: Array<String>) {
        val parameters = HashMap<GetRandomFlag, String?>()

        var i = 0
        while (i < args.size) {
            val arg = args[i]

            if (!arg.startsWith("-")) {
                i++
                continue
            }

            val parameter = arg.substring(1)

            val flag = GetRandomFlag.getFlag(parameter)

            if (flag == null || parameters.containsKey(flag)) {
                i++
                continue
            }

            var value: String? = null

            // If the next argument doesn't start with "-", it's the value of this flag.
            if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                value = args[i + 1]
                i++ // Skip next arg since it's this flag's value.
            }

            parameters[flag] = value
            i++
        }

        val rawRange = parameters[GetRandomFlag.RANGE]
        val parsedRange =
            rawRange?.split(",".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        if (
            parsedRange != null &&
                (parsedRange.size != 2 ||
                    !isInt(parsedRange[0].trim { it <= ' ' }) ||
                    !isInt(parsedRange[1].trim { it <= ' ' }))
        ) {
            player.sendMessage("$PREFIX&cInvalid range syntax: lower-higher".colorFormat())
        }

        val range =
            if (
                parsedRange != null &&
                    parsedRange.size == 2 &&
                    isInt(parsedRange[0].trim { it <= ' ' }) &&
                    isInt(parsedRange[1].trim { it <= ' ' })
            )
                Pair(
                    parsedRange[0].trim { it <= ' ' }.toInt(),
                    parsedRange[1].trim { it <= ' ' }.toInt(),
                )
            else null

        val rarities by lazy { mutableSetOf<GameItemRarityType>() }

        val rarity = parameters[GetRandomFlag.RARITY]
        val splitRarities =
            rarity?.split(",".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
                ?: emptyArray()
        for (rawRarity in splitRarities) {
            val parsed = GameItemRarityType.getFromIdentifier(rawRarity.trim())

            if (parsed == null) {
                player.sendMessage("$PREFIX&cInvalid rarity of $rawRarity entered!".colorFormat())
                continue
            }

            rarities.add(parsed)
        }

        val playerClass = parameters[GetRandomFlag.CLASS]
        val clazz = getClassTypeFromIdentifier(playerClass?.trim() ?: "")

        if (clazz == null && playerClass != null) {
            player.sendMessage(
                "$PREFIX&cInvalid class of ${playerClass.trim()} entered!".colorFormat()
            )
        }

        val types = parameters[GetRandomFlag.ITEMS]
        var items: MutableSet<LootHelper.ItemType>? = null

        val splitTypes =
            types?.split(",".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
                ?: emptyArray()

        for (rawType in splitTypes) {
            val parsed = LootHelper.ItemType.getItemType(rawType.trim())
            if (parsed == null) {
                player.sendMessage(
                    "$PREFIX&cInvalid item type of ${rawType.trim()} entered!".colorFormat()
                )
                continue
            }
            if (items == null) {
                items = HashSet()
            }
            items.add(parsed)
        }
        val lqm =
            try {
                parameters[GetRandomFlag.LQM]!!.toFloat()
            } catch (e: NumberFormatException) {
                null
            } catch (e: NullPointerException) {
                null
            }

        // iterating and picking item is async, slight delay and item is given on main thread after
        // async task is complete
        val template = lootHelper.getItem(range, rarities, clazz, items, lqm)

        if (template == null) {
            player.sendMessage(
                "$PREFIX&cThere are no item templates that match your conditions!".colorFormat()
            )
            return
        }

        val item = itemTemplateRegistry.generateGameItem(template).generateItemStack(1)
        // TODO check works
        player.inventory.addItem(item)
        //        RunicItemsAPI.addItem(player.inventory, item.generateItemStack())
    }

    /** An enum to keep track of parameters for the get-random subcommand */
    private enum class GetRandomFlag(val text: String, val complete: (String) -> List<String>) {
        RANGE(
            "range",
            { input ->
                Stream.of("10,20", "20,30", "30,40", "40,50", "50,60", "60,60")
                    .filter { element: String -> element.startsWith(input) }
                    .toList()
            },
        ),
        RARITY(
            "rarity",
            { input ->
                Stream.of("common", "uncommon", "rare", "epic", "common,uncommon")
                    .filter { element: String -> element.startsWith(input) }
                    .toList()
            },
        ),
        CLASS(
            "class",
            { input ->
                Stream.of("mage", "mage,rogue", "mage,rogue,warrior")
                    .filter { element: String -> element.startsWith(input) }
                    .toList()
            },
        ),
        ITEMS(
            "items",
            { input ->
                Stream.of(
                        "helmet",
                        "chestplate",
                        "leggings",
                        "boots",
                        "weapon",
                        "helmet,chestplate,leggings,boots",
                    )
                    .filter { element: String -> element.startsWith(input) }
                    .toList()
            },
        ),
        LQM(
            "lqm",
            { input ->
                Stream.of("1", "1.5", "0.5")
                    .filter { element: String -> element.startsWith(input) }
                    .toList()
            },
        );

        companion object {
            fun getFlag(value: String): GetRandomFlag? {
                return try {
                    valueOf(value.uppercase(Locale.getDefault()))
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    companion object {
        const val PREFIX: String = "&5[RunicItems] &6» &r"

        private fun isInt(number: String): Boolean {
            try {
                number.toInt()
            } catch (exception: Exception) {
                return false
            }
            return true
        }

        private fun isBoolean(bool: String): Boolean {
            return ("true".equals(bool, ignoreCase = true) ||
                "false".equals(bool, ignoreCase = true))
        }
    }
}
