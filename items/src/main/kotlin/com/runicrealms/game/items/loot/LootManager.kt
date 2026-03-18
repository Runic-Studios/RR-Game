package com.runicrealms.game.items.loot

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.loot.chest.LootChestTableEntry
import com.runicrealms.game.items.loot.chest.LootChestTemplate
import com.runicrealms.game.items.loot.chest.RegenerativeLootChest
import com.runicrealms.game.items.loot.chest.TimedLootChest
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Central manager for the loot system. Loads and provides access to loot tables,
 * chest templates, and regenerative loot chests. Also handles displaying timed loot chests.
 */
@Singleton
class LootManager
@Inject
constructor(
    private val plugin: Plugin,
    private val templateRegistry: GameItemTemplateRegistry,
) {

    private val lootTables = HashMap<String, LootTable>()
    private val chestTemplates = HashMap<String, LootChestTemplate>()
    private val regenerativeChests = HashMap<String, RegenerativeLootChest>()

    init {
        reload()
    }

    /**
     * Reloads all loot tables, chest templates, and regenerative chests from disk.
     */
    fun reload() {
        lootTables.clear()
        chestTemplates.clear()
        regenerativeChests.clear()

        loadLootTables()
        loadChestTemplates()
        loadRegenerativeChests()

        logger.info(
            "Loaded ${lootTables.size} loot tables, ${chestTemplates.size} chest templates, " +
                "${regenerativeChests.size} regenerative chests"
        )
    }

    fun getLootTable(id: String): LootTable? = lootTables[id]

    fun getChestTemplate(id: String): LootChestTemplate? = chestTemplates[id]

    fun getRegenerativeLootChests(): Collection<RegenerativeLootChest> = regenerativeChests.values

    fun getLootTables(): Collection<LootTable> = lootTables.values

    /**
     * Creates and persists a new regenerative loot chest.
     */
    fun createRegenerativeLootChest(chest: RegenerativeLootChest) {
        val key = chest.locationKey()
        regenerativeChests[key] = chest
        saveRegenerativeChests()
    }

    /**
     * Deletes a regenerative loot chest and persists the change.
     */
    fun deleteRegenerativeLootChest(chest: RegenerativeLootChest) {
        val key = chest.locationKey()
        regenerativeChests.remove(key)
        saveRegenerativeChests()
    }

    /**
     * Finds a regenerative loot chest at the given block location.
     */
    fun getRegenerativeLootChestAt(
        world: String,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
    ): RegenerativeLootChest? {
        val key = "${world}_${blockX}_${blockY}_${blockZ}"
        return regenerativeChests[key]
    }

    /**
     * Displays a timed loot chest to a player by opening a chest inventory
     * populated with generated loot.
     */
    fun displayTimedLootChest(player: Player, chest: TimedLootChest) {
        val inventory = Bukkit.createInventory(
            null,
            LootChestTemplate.CHEST_SLOTS,
            net.kyori.adventure.text.Component.text("Loot Chest"),
        )

        for (item in chest.lootItems) {
            inventory.addItem(item)
        }

        player.openInventory(inventory)
    }

    /**
     * Generates loot items from a chest template using the associated loot tables.
     */
    fun generateLootFromTemplate(templateID: String): List<ItemStack> {
        val template = chestTemplates[templateID]
        if (template == null) {
            logger.warn("Unknown chest template '$templateID'")
            return emptyList()
        }

        val generatedItems = mutableListOf<ItemStack>()
        val random = ThreadLocalRandom.current()

        for (entry in template.tableEntries) {
            val table = lootTables[entry.tableID]
            if (table == null) {
                logger.warn("Chest template '${template.identifier}' references unknown table '${entry.tableID}'")
                continue
            }

            val count = if (entry.minCount >= entry.maxCount) {
                entry.minCount
            } else {
                random.nextInt(entry.minCount, entry.maxCount + 1)
            }

            repeat(count) {
                val item = table.generateLoot(templateRegistry) { tableID -> lootTables[tableID] }
                if (item != null) {
                    generatedItems.add(item)
                }
            }
        }

        // Cap at chest size
        return generatedItems.take(LootChestTemplate.CHEST_SLOTS)
    }

    // -- Loading --

    private fun loadLootTables() {
        val folder = File(plugin.dataFolder, "loot-tables").also { it.mkdirs() }
        val rawTables = HashMap<String, RawLootTableData>()

        for (file in folder.listFiles { _, name -> name.endsWith(".yml") } ?: emptyArray()) {
            val config = YamlConfiguration.loadConfiguration(file)
            for (key in config.getKeys(false)) {
                val section = config.getConfigurationSection(key) ?: continue
                val items = mutableListOf<LootItem>()
                val subTableRefs = mutableListOf<LootTableReference>()
                val dependsOn = mutableListOf<String>()

                val itemsSection = section.getConfigurationSection("items")
                if (itemsSection != null) {
                    for (itemKey in itemsSection.getKeys(false)) {
                        val itemSection = itemsSection.getConfigurationSection(itemKey) ?: continue
                        items.add(
                            LootItem(
                                templateID = itemSection.getString("template-id") ?: continue,
                                weight = itemSection.getDouble("weight", 1.0),
                                minStackSize = itemSection.getInt("min-stack-size", 1),
                                maxStackSize = itemSection.getInt("max-stack-size", 1),
                            )
                        )
                    }
                }

                val subTablesSection = section.getConfigurationSection("sub-tables")
                if (subTablesSection != null) {
                    for (subKey in subTablesSection.getKeys(false)) {
                        val subSection = subTablesSection.getConfigurationSection(subKey) ?: continue
                        val tableID = subSection.getString("table-id") ?: continue
                        val weight = subSection.getDouble("weight", 1.0)
                        subTableRefs.add(LootTableReference(tableID, weight))
                        dependsOn.add(tableID)
                    }
                }

                rawTables[key] = RawLootTableData(key, items, subTableRefs, dependsOn)
            }
        }

        // Topological sort so sub-tables are loaded before their parents
        val sorted = topologicalSort(rawTables)
        for (raw in sorted) {
            lootTables[raw.identifier] = LootTable(
                identifier = raw.identifier,
                items = raw.items,
                subTableReferences = raw.subTableReferences,
            )
        }
    }

    private fun loadChestTemplates() {
        val folder = File(plugin.dataFolder, "chest-templates").also { it.mkdirs() }

        for (file in folder.listFiles { _, name -> name.endsWith(".yml") } ?: emptyArray()) {
            val config = YamlConfiguration.loadConfiguration(file)
            for (key in config.getKeys(false)) {
                val section = config.getConfigurationSection(key) ?: continue
                val entries = mutableListOf<LootChestTableEntry>()

                val tablesSection = section.getConfigurationSection("tables")
                if (tablesSection != null) {
                    for (tableKey in tablesSection.getKeys(false)) {
                        val tableSection = tablesSection.getConfigurationSection(tableKey) ?: continue
                        entries.add(
                            LootChestTableEntry(
                                tableID = tableSection.getString("table-id") ?: continue,
                                minCount = tableSection.getInt("min-count", 1),
                                maxCount = tableSection.getInt("max-count", 1),
                            )
                        )
                    }
                }

                chestTemplates[key] = LootChestTemplate(
                    identifier = key,
                    tableEntries = entries,
                )
            }
        }
    }

    private fun loadRegenerativeChests() {
        val file = File(plugin.dataFolder, "regenerative-chests.yml")
        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)
        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key) ?: continue
            val chest = RegenerativeLootChest.loadFrom(section)
            if (chest == null) {
                logger.warn("Failed to load regenerative chest '$key'")
                continue
            }
            regenerativeChests[chest.locationKey()] = chest
        }
    }

    private fun saveRegenerativeChests() {
        val file = File(plugin.dataFolder, "regenerative-chests.yml")
        val config = YamlConfiguration()

        var index = 0
        for (chest in regenerativeChests.values) {
            val section = config.createSection("chest-$index")
            chest.saveTo(section)
            index++
        }

        config.save(file)
    }

    // -- Topological Sort --

    private data class RawLootTableData(
        val identifier: String,
        val items: List<LootItem>,
        val subTableReferences: List<LootTableReference>,
        val dependsOn: List<String>,
    )

    /**
     * Sorts raw loot table data topologically so that sub-tables appear before
     * the tables that reference them.
     */
    private fun topologicalSort(tables: Map<String, RawLootTableData>): List<RawLootTableData> {
        val sorted = mutableListOf<RawLootTableData>()
        val visited = HashSet<String>()
        val visiting = HashSet<String>()

        fun visit(id: String) {
            if (id in visited) return
            if (id in visiting) {
                logger.warn("Circular dependency detected in loot tables involving '$id'")
                return
            }
            val data = tables[id] ?: return
            visiting.add(id)
            for (dependency in data.dependsOn) {
                visit(dependency)
            }
            visiting.remove(id)
            visited.add(id)
            sorted.add(data)
        }

        for (id in tables.keys) {
            visit(id)
        }

        return sorted
    }
}
