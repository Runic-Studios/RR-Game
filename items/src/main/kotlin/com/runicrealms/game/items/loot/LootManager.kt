package com.runicrealms.game.items.loot

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.game.items.loot.chest.BossTimedLoot
import com.runicrealms.game.items.loot.chest.CustomTimedLoot
import com.runicrealms.game.items.loot.chest.LootChestConditions
import com.runicrealms.game.items.loot.chest.LootChestTableEntry
import com.runicrealms.game.items.loot.chest.LootChestTemplate
import com.runicrealms.game.items.loot.chest.RegenerativeLootChest
import com.runicrealms.game.items.loot.chest.TimedLootChest
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Central manager for the loot system. Loads and provides access to loot tables, chest templates,
 * regenerative loot chests, and timed loot chest configs.
 *
 * Config file locations (relative to plugin data folder), matching the old Java system:
 * - Loot tables: `loot/loot-tables/<name>.yml` (one table per file)
 * - Chest templates: `loot/chest-types/<name>.yml` (one template per file)
 * - Regenerative chests: `loot/regenerative-chests.yml`
 * - Timed loot: `loot/timed-loot/<name>.yml` (one entry per file)
 *
 * ### Loot table format (`loot/loot-tables/my-table.yml`):
 * ```yaml
 * identifier: my-table
 * items:
 *   some-template-id:
 *     weight: 10
 *     stack-size:
 *       min: 1
 *       max: 3
 *   script:          # special key: generates a random item in chest's level range (not yet implemented)
 *     weight: 5
 * subtables:
 *   - other-table-id
 * ```
 *
 * ### Chest template format (`loot/chest-types/my-chest.yml`):
 * ```yaml
 * identifier: my-chest-type
 * loot-tables:
 *   my-table:
 *     count:
 *       min: 3
 *       max: 5
 * ```
 *
 * ### Regenerative chest format (`loot/regenerative-chests.yml`):
 * ```yaml
 * next-id: 1
 * chests:
 *   0:
 *     location:
 *       world: world
 *       x: 100
 *       y: 64
 *       z: 200
 *       direction: NORTH
 *     template: my-chest-type
 *     regeneration-time: 300
 *     min-level: 1
 *     item-level:
 *       min: 1
 *       max: 60
 *     title: "&6Loot Chest"
 *     model: NORMAL   # optional
 *     conditions:     # optional
 *       1:
 *         type: item
 *         template-id: my-key
 *         count: 1
 *         take-item: true
 * ```
 */
@Singleton
class LootManager
@Inject
constructor(
    private val plugin: Plugin,
    private val templateRegistry: GameItemTemplateRegistry,
    private val itemStackConverter: ItemStackConverter,
) {

    private val lootTables = HashMap<String, LootTable>()
    private val chestTemplates = HashMap<String, LootChestTemplate>()
    private val regenerativeChests = HashMap<String, RegenerativeLootChest>()

    /**
     * Stores timed loot template configs keyed by boss ID (for boss type) or custom identifier (for
     * custom type). These are templates - actual instances are created when the boss dies or the
     * command is run.
     */
    private val bossTimedLootConfigs = HashMap<String, BossTimedLootConfig>()
    private val customTimedLootConfigs = HashMap<String, CustomTimedLootConfig>()

    private val lootFolder
        get() = File(plugin.dataFolder, "loot")

    private var nextRegenChestID = 0

    init {
        reload()
    }

    fun reload() {
        lootTables.clear()
        chestTemplates.clear()
        regenerativeChests.clear()
        bossTimedLootConfigs.clear()
        customTimedLootConfigs.clear()
        nextRegenChestID = 0

        loadLootTables()
        loadChestTemplates()
        loadRegenerativeChests()
        loadTimedLoot()

        logger.info(
            "Loaded ${lootTables.size} loot tables, ${chestTemplates.size} chest templates, " +
                "${regenerativeChests.size} regenerative chests, " +
                "${bossTimedLootConfigs.size} boss timed loot configs, " +
                "${customTimedLootConfigs.size} custom timed loot configs"
        )
    }

    fun getLootTable(id: String): LootTable? = lootTables[id]

    fun getChestTemplate(id: String): LootChestTemplate? = chestTemplates[id]

    fun getRegenerativeLootChests(): Collection<RegenerativeLootChest> = regenerativeChests.values

    fun getLootTables(): Collection<LootTable> = lootTables.values

    fun getChestTemplates(): Collection<LootChestTemplate> = chestTemplates.values

    fun getBossTimedLootConfig(bossID: String): BossTimedLootConfig? = bossTimedLootConfigs[bossID]

    fun getCustomTimedLootConfig(customID: String): CustomTimedLootConfig? =
        customTimedLootConfigs[customID]

    fun getCustomTimedLootConfigs(): Map<String, CustomTimedLootConfig> = customTimedLootConfigs

    fun createRegenerativeLootChest(chest: RegenerativeLootChest) {
        regenerativeChests[chest.locationKey()] = chest
        saveRegenerativeChestsAsync()
    }

    fun deleteRegenerativeLootChest(chest: RegenerativeLootChest) {
        regenerativeChests.remove(chest.locationKey())
        saveRegenerativeChestsAsync()
    }

    fun getRegenerativeLootChestAt(
        world: String,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
    ): RegenerativeLootChest? = regenerativeChests["${world}_${blockX}_${blockY}_${blockZ}"]

    fun displayTimedLootChest(player: Player, chest: TimedLootChest) {
        val inventory =
            Bukkit.createInventory(null, LootChestTemplate.CHEST_SLOTS, Component.text(chest.title))
        placeItemsInInventory(inventory, chest.lootItems)
        player.openInventory(inventory)
    }

    fun generateLootFromTemplate(templateID: String): List<ItemStack> {
        val template =
            chestTemplates[templateID]
                ?: run {
                    logger.warn("Unknown chest template '{}'", templateID)
                    return emptyList()
                }

        val generatedItems = mutableListOf<ItemStack>()
        val random = ThreadLocalRandom.current()

        for (entry in template.tableEntries) {
            val table = lootTables[entry.tableID]
            if (table == null) {
                logger.warn(
                    "Chest template '{}' references unknown table '{}'",
                    template.identifier,
                    entry.tableID,
                )
                continue
            }

            val count =
                if (entry.minCount >= entry.maxCount) entry.minCount
                else random.nextInt(entry.minCount, entry.maxCount + 1)

            repeat(count) {
                val item = table.generateLoot(templateRegistry) { id -> lootTables[id] }
                if (item != null) generatedItems.add(item)
            }
        }

        return generatedItems.take(LootChestTemplate.CHEST_SLOTS)
    }

    /** Places items into an inventory in randomised slots (not sequentially). */
    fun placeItemsInInventory(inventory: org.bukkit.inventory.Inventory, items: List<ItemStack>) {
        val slots = (0 until LootChestTemplate.CHEST_SLOTS).toMutableList()
        slots.shuffle(ThreadLocalRandom.current())
        val slotIterator = slots.iterator()
        for (item in items) {
            if (!slotIterator.hasNext()) break
            inventory.setItem(slotIterator.next(), item)
        }
    }

    // -- Loading --

    private fun loadLootTables() {
        val folder = File(lootFolder, "loot-tables").also { it.mkdirs() }
        val rawTables = HashMap<String, RawLootTableData>()

        for (file in
            folder.listFiles { _, name -> name.endsWith(".yml") || name.endsWith(".yaml") }
                ?: emptyArray()) {
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                val identifier = config.getString("identifier")
                if (identifier == null) {
                    logger.warn("Loot table file '{}' is missing 'identifier'", file.name)
                    continue
                }

                val items = mutableListOf<LootItem>()
                val dependsOn = mutableListOf<String>()

                val itemsSection = config.getConfigurationSection("items")
                if (itemsSection != null) {
                    for (templateID in itemsSection.getKeys(false)) {
                        val section = itemsSection.getConfigurationSection(templateID) ?: continue
                        val weight = section.getInt("weight")
                        if (weight == 0) {
                            logger.warn(
                                "Loot table '{}': item '{}' has zero weight, skipping",
                                identifier,
                                templateID,
                            )
                            continue
                        }
                        if (templateID.equals("script", ignoreCase = true)) {
                            // TODO: script items generate level-appropriate loot; not yet supported
                            logger.debug(
                                "Skipping 'script' loot entry in table '{}' (not yet implemented)",
                                identifier,
                            )
                        } else {
                            val minStack = section.getInt("stack-size.min", 1)
                            val maxStack = section.getInt("stack-size.max", 1)
                            items.add(
                                LootItem(
                                    templateID = templateID,
                                    weight = weight.toDouble(),
                                    minStackSize = minStack,
                                    maxStackSize = maxStack,
                                )
                            )
                        }
                    }
                }

                val subtableList = config.getStringList("subtables")
                dependsOn.addAll(subtableList)

                rawTables[identifier] = RawLootTableData(identifier, items, subtableList, dependsOn)
            } catch (exception: Exception) {
                logger.error("Error loading loot table file '{}': {}", file.name, exception.message)
            }
        }

        val sorted = topologicalSort(rawTables)
        for (raw in sorted) {
            val allItems = mutableListOf<LootItem>()
            allItems.addAll(raw.items)
            for (subtableID in raw.subtableIDs) {
                val sub = lootTables[subtableID]
                if (sub == null) {
                    logger.warn(
                        "Loot table '{}' references unknown subtable '{}'",
                        raw.identifier,
                        subtableID,
                    )
                    continue
                }
                allItems.addAll(sub.items)
            }
            lootTables[raw.identifier] = LootTable(identifier = raw.identifier, items = allItems)
        }
    }

    private fun loadChestTemplates() {
        val folder = File(lootFolder, "chest-types").also { it.mkdirs() }

        for (file in
            folder.listFiles { _, name -> name.endsWith(".yml") || name.endsWith(".yaml") }
                ?: emptyArray()) {
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                val identifier = config.getString("identifier")
                if (identifier == null) {
                    logger.warn("Chest template file '{}' is missing 'identifier'", file.name)
                    continue
                }

                val lootTablesSection = config.getConfigurationSection("loot-tables")
                if (lootTablesSection == null) {
                    logger.warn("Chest template '{}' is missing 'loot-tables' section", identifier)
                    continue
                }

                val entries = mutableListOf<LootChestTableEntry>()
                for (tableID in lootTablesSection.getKeys(false)) {
                    val minCount = lootTablesSection.getInt("$tableID.count.min")
                    val maxCount = lootTablesSection.getInt("$tableID.count.max")
                    if (minCount == 0 || maxCount == 0) {
                        logger.warn(
                            "Chest template '{}': table '{}' has missing count.min or count.max",
                            identifier,
                            tableID,
                        )
                        continue
                    }
                    entries.add(
                        LootChestTableEntry(
                            tableID = tableID,
                            minCount = minCount,
                            maxCount = maxCount,
                        )
                    )
                }

                chestTemplates[identifier] =
                    LootChestTemplate(identifier = identifier, tableEntries = entries)
            } catch (exception: Exception) {
                logger.error(
                    "Error loading chest template file '{}': {}",
                    file.name,
                    exception.message,
                )
            }
        }
    }

    private fun loadRegenerativeChests() {
        val file = File(lootFolder, "regenerative-chests.yml")
        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)
        nextRegenChestID = config.getInt("next-id", 0)

        val chestsSection = config.getConfigurationSection("chests") ?: return
        for (chestID in chestsSection.getKeys(false)) {
            val section = chestsSection.getConfigurationSection(chestID) ?: continue
            var chest = RegenerativeLootChest.loadFrom(section)
            if (chest == null) {
                logger.warn("Failed to load regenerative chest '{}'", chestID)
                continue
            }
            // Load conditions separately (they need the template registry and converter)
            val conditionsSection = section.getConfigurationSection("conditions")
            if (conditionsSection != null) {
                val conditions =
                    LootChestConditions.loadFromConfig(
                        conditionsSection,
                        templateRegistry,
                        itemStackConverter,
                    )
                chest = chest.copy(conditions = conditions)
            }
            regenerativeChests[chest.locationKey()] = chest
        }
    }

    private fun loadTimedLoot() {
        val folder = File(lootFolder, "timed-loot").also { it.mkdirs() }

        for (file in
            folder.listFiles { _, name -> name.endsWith(".yml") || name.endsWith(".yaml") }
                ?: emptyArray()) {
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                val type = config.getString("type")
                if (type == null) {
                    logger.warn("Timed loot file '{}' is missing 'type'", file.name)
                    continue
                }

                when (type.lowercase()) {
                    "boss" -> parseBossTimedLoot(config, file.name)
                    "custom" -> parseCustomTimedLoot(config, file.name)
                    else ->
                        logger.warn(
                            "Timed loot file '{}' has unknown type '{}', expected 'boss' or 'custom'",
                            file.name,
                            type,
                        )
                }
            } catch (exception: Exception) {
                logger.error("Error loading timed loot file '{}': {}", file.name, exception.message)
            }
        }
    }

    private fun parseBossTimedLoot(config: YamlConfiguration, fileName: String) {
        val chestSection = config.getConfigurationSection("chest")
        if (chestSection == null) {
            logger.warn("Boss timed loot file '{}' is missing 'chest' section", fileName)
            return
        }
        val bossSection = config.getConfigurationSection("boss")
        if (bossSection == null) {
            logger.warn("Boss timed loot file '{}' is missing 'boss' section", fileName)
            return
        }

        val templateID = chestSection.getString("template")
        if (templateID == null) {
            logger.warn("Boss timed loot '{}': missing 'chest.template'", fileName)
            return
        }
        val duration = chestSection.getInt("duration", 300)
        val title = chestSection.getString("title", "Loot Chest") ?: "Loot Chest"
        val minLevel = chestSection.getInt("min-level", 0)
        val minItemLevel = chestSection.getInt("item-level.min", 1)
        val maxItemLevel = chestSection.getInt("item-level.max", 60)
        val chestLocation = parseLocation(chestSection.getConfigurationSection("location"))

        val hologramOffset = parseHologramOffset(config.getConfigurationSection("hologram"))
        val hologramLines = config.getStringList("hologram.lines").takeIf { it.isNotEmpty() }

        val mmID = bossSection.getString("mm-id")
        if (mmID == null) {
            logger.warn("Boss timed loot '{}': missing 'boss.mm-id'", fileName)
            return
        }
        val lootDamageThreshold = bossSection.getDouble("loot-damage-threshold", 0.0)
        val lootRange = bossSection.getInt("loot-range", 1024)
        val completeLocation = parseLocation(bossSection.getConfigurationSection("location"))

        bossTimedLootConfigs[mmID] =
            BossTimedLootConfig(
                mmID = mmID,
                templateID = templateID,
                duration = duration,
                title = title,
                minLevel = minLevel,
                minItemLevel = minItemLevel,
                maxItemLevel = maxItemLevel,
                chestLocation = chestLocation,
                hologramOffset = hologramOffset,
                hologramLines = hologramLines,
                lootDamageThreshold = lootDamageThreshold,
                lootRange = lootRange,
                completeLocation = completeLocation,
            )
    }

    private fun parseCustomTimedLoot(config: YamlConfiguration, fileName: String) {
        val chestSection = config.getConfigurationSection("chest")
        if (chestSection == null) {
            logger.warn("Custom timed loot file '{}' is missing 'chest' section", fileName)
            return
        }
        val customSection = config.getConfigurationSection("custom")
        if (customSection == null) {
            logger.warn("Custom timed loot file '{}' is missing 'custom' section", fileName)
            return
        }

        val templateID = chestSection.getString("template")
        if (templateID == null) {
            logger.warn("Custom timed loot '{}': missing 'chest.template'", fileName)
            return
        }
        val duration = chestSection.getInt("duration", 300)
        val title = chestSection.getString("title", "Loot Chest") ?: "Loot Chest"
        val minLevel = chestSection.getInt("min-level", 0)
        val minItemLevel = chestSection.getInt("item-level.min", 1)
        val maxItemLevel = chestSection.getInt("item-level.max", 60)
        val chestLocation = parseLocation(chestSection.getConfigurationSection("location"))

        val hologramOffset = parseHologramOffset(config.getConfigurationSection("hologram"))
        val hologramLines = config.getStringList("hologram.lines").takeIf { it.isNotEmpty() }

        val identifier = customSection.getString("identifier")
        if (identifier == null) {
            logger.warn("Custom timed loot '{}': missing 'custom.identifier'", fileName)
            return
        }

        customTimedLootConfigs[identifier] =
            CustomTimedLootConfig(
                identifier = identifier,
                templateID = templateID,
                duration = duration,
                title = title,
                minLevel = minLevel,
                minItemLevel = minItemLevel,
                maxItemLevel = maxItemLevel,
                chestLocation = chestLocation,
                hologramOffset = hologramOffset,
                hologramLines = hologramLines,
            )
    }

    private fun parseLocation(section: org.bukkit.configuration.ConfigurationSection?): Location? {
        if (section == null) return null
        val worldName = section.getString("world") ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null
        val x = section.getDouble("x")
        val y = section.getDouble("y")
        val z = section.getDouble("z")
        val yaw = section.getDouble("yaw", 0.0).toFloat()
        val pitch = section.getDouble("pitch", 0.0).toFloat()
        return Location(world, x, y, z, yaw, pitch)
    }

    private fun parseHologramOffset(
        section: org.bukkit.configuration.ConfigurationSection?
    ): Vector? {
        if (section == null) return null
        val locSection = section.getConfigurationSection("location") ?: return null
        // If a full absolute location is given, compute offset relative to chest location.
        // Return null to signal the caller should use the absolute location instead.
        // For simplicity, we return null here so absolute hologram locations from the config are
        // handled by TimedLootChest using the default offset - a full per-instance override would
        // require passing the absolute location through.
        return null
    }

    // -- Saving --

    /**
     * Saves the regenerative chests file asynchronously. Must be called from the Minecraft main
     * thread.
     */
    private fun saveRegenerativeChestsAsync() {
        plugin.launch { withContext(plugin.asyncDispatcher) { saveRegenerativeChests() } }
    }

    private fun saveRegenerativeChests() {
        val file = File(lootFolder, "regenerative-chests.yml").also { it.parentFile.mkdirs() }
        val config = YamlConfiguration()

        // Persist the counter as-is (only increases, never resets to size)
        config.set("next-id", nextRegenChestID)

        var index = 0
        for (chest in regenerativeChests.values) {
            chest.saveTo(config.createSection("chests.$index"))
            index++
        }

        config.save(file)
    }

    // -- Topological Sort --

    private data class RawLootTableData(
        val identifier: String,
        val items: List<LootItem>,
        val subtableIDs: List<String>,
        val dependsOn: List<String>,
    )

    private fun topologicalSort(tables: Map<String, RawLootTableData>): List<RawLootTableData> {
        val sorted = mutableListOf<RawLootTableData>()
        val visited = HashSet<String>()
        val visiting = HashSet<String>()

        fun visit(id: String) {
            if (id in visited) return
            if (id in visiting) {
                logger.warn("Circular dependency detected in loot tables involving '{}'", id)
                return
            }
            val data = tables[id] ?: return
            visiting.add(id)
            for (dep in data.dependsOn) visit(dep)
            visiting.remove(id)
            visited.add(id)
            sorted.add(data)
        }

        for (id in tables.keys) visit(id)
        return sorted
    }
}

/** Stores the loaded config for a boss-triggered timed loot chest. */
data class BossTimedLootConfig(
    val mmID: String,
    val templateID: String,
    val duration: Int,
    val title: String,
    val minLevel: Int,
    val minItemLevel: Int,
    val maxItemLevel: Int,
    val chestLocation: Location?,
    val hologramOffset: Vector?,
    val hologramLines: List<String>?,
    val lootDamageThreshold: Double,
    val lootRange: Int,
    val completeLocation: Location?,
) {
    fun createBossTimedLoot(lootItems: List<ItemStack>, spawnLocation: Location): BossTimedLoot {
        return BossTimedLoot(
            templateID = templateID,
            location = chestLocation ?: spawnLocation,
            durationSeconds = duration,
            lootItems = lootItems,
            title = title,
            hologramOffset = hologramOffset,
            hologramLines = hologramLines,
            bossID = mmID,
            lootDamageThreshold = lootDamageThreshold,
            lootRange = lootRange,
            completeLocation = completeLocation,
            minLevel = minLevel,
        )
    }
}

/** Stores the loaded config for a custom identifier timed loot chest. */
data class CustomTimedLootConfig(
    val identifier: String,
    val templateID: String,
    val duration: Int,
    val title: String,
    val minLevel: Int,
    val minItemLevel: Int,
    val maxItemLevel: Int,
    val chestLocation: Location?,
    val hologramOffset: Vector?,
    val hologramLines: List<String>?,
) {
    fun createCustomTimedLoot(
        lootItems: List<ItemStack>,
        spawnLocation: Location,
    ): CustomTimedLoot {
        return CustomTimedLoot(
            templateID = templateID,
            location = chestLocation ?: spawnLocation,
            durationSeconds = duration,
            lootItems = lootItems,
            title = title,
            hologramOffset = hologramOffset,
            hologramLines = hologramLines,
            customID = identifier,
            minLevel = minLevel,
        )
    }
}
