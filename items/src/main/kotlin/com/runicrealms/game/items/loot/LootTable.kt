package com.runicrealms.game.items.loot

import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.inventory.ItemStack
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Represents a single weighted item entry in a loot table.
 */
data class LootItem(
    val templateID: String,
    val weight: Double,
    val minStackSize: Int = 1,
    val maxStackSize: Int = 1,
)

/**
 * Represents a weighted entry for custom script-based loot handling.
 */
data class LootScriptItem(val weight: Double)

/**
 * A reference to a sub-table that can be included in another loot table.
 */
data class LootTableReference(
    val tableID: String,
    val weight: Double,
)

/**
 * A loot table with weighted items that can reference sub-tables.
 * Items are selected based on weighted random selection.
 */
class LootTable(
    val identifier: String,
    val items: List<LootItem>,
    val subTableReferences: List<LootTableReference> = emptyList(),
) {

    /**
     * Generates a random loot item from this table, considering sub-table references.
     * Uses weighted random selection across all entries.
     *
     * @param templateRegistry the registry used to generate items from template IDs
     * @param tableResolver a function that resolves sub-table IDs to their LootTable instances
     * @return the generated ItemStack, or null if the table is empty or generation fails
     */
    fun generateLoot(
        templateRegistry: GameItemTemplateRegistry,
        tableResolver: (String) -> LootTable?,
    ): ItemStack? {
        val allEntries = mutableListOf<WeightedEntry>()

        for (item in items) {
            allEntries.add(WeightedEntry.ItemEntry(item))
        }
        for (reference in subTableReferences) {
            allEntries.add(WeightedEntry.SubTableEntry(reference))
        }

        if (allEntries.isEmpty()) {
            logger.warn("Loot table '$identifier' has no entries")
            return null
        }

        val totalWeight = allEntries.sumOf { it.weight }
        if (totalWeight <= 0.0) {
            logger.warn("Loot table '$identifier' has zero or negative total weight")
            return null
        }

        val roll = ThreadLocalRandom.current().nextDouble() * totalWeight
        var accumulated = 0.0

        for (entry in allEntries) {
            accumulated += entry.weight
            if (roll < accumulated) {
                return when (entry) {
                    is WeightedEntry.ItemEntry -> generateItemFromEntry(entry.item, templateRegistry)
                    is WeightedEntry.SubTableEntry -> {
                        val subTable = tableResolver(entry.reference.tableID)
                        if (subTable == null) {
                            logger.warn(
                                "Loot table '$identifier' references unknown sub-table '${entry.reference.tableID}'"
                            )
                            null
                        } else {
                            subTable.generateLoot(templateRegistry, tableResolver)
                        }
                    }
                }
            }
        }

        // Fallback (should not be reached due to floating point)
        return when (val lastEntry = allEntries.last()) {
            is WeightedEntry.ItemEntry -> generateItemFromEntry(lastEntry.item, templateRegistry)
            is WeightedEntry.SubTableEntry -> {
                val subTable = tableResolver(lastEntry.reference.tableID)
                subTable?.generateLoot(templateRegistry, tableResolver)
            }
        }
    }

    private fun generateItemFromEntry(
        item: LootItem,
        templateRegistry: GameItemTemplateRegistry,
    ): ItemStack? {
        val template = templateRegistry.getItemTemplate(item.templateID)
        if (template == null) {
            logger.warn("Loot table '$identifier' references unknown template '${item.templateID}'")
            return null
        }
        val stackSize = if (item.minStackSize >= item.maxStackSize) {
            item.minStackSize
        } else {
            ThreadLocalRandom.current().nextInt(item.minStackSize, item.maxStackSize + 1)
        }
        return templateRegistry.generateGameItem(template).generateItemStack(stackSize)
    }

    private sealed class WeightedEntry(val weight: Double) {
        class ItemEntry(val item: LootItem) : WeightedEntry(item.weight)
        class SubTableEntry(val reference: LootTableReference) : WeightedEntry(reference.weight)
    }
}
