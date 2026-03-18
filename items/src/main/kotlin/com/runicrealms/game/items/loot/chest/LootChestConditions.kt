package com.runicrealms.game.items.loot.chest

import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.game.items.util.ItemInventoryUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Conditions that must all be met for a player to access a loot chest.
 *
 * Conditions are loaded from a `conditions:` section in the chest YAML:
 * ```yaml
 * conditions:
 *   1:
 *     type: item
 *     template-id: my-key-item
 *     count: 1
 *     take-item: true
 * ```
 */
class LootChestConditions(val conditions: List<Condition> = emptyList()) {

    /**
     * Attempts to pass all conditions for the given player. Calls [Condition.onDeny] and returns
     * false on the first failure; calls [Condition.onComplete] (consuming items) on success.
     */
    fun attempt(player: Player): Boolean {
        for (condition in conditions) {
            if (!condition.isFulfilled(player)) {
                condition.onDeny(player)
                return false
            }
        }
        for (condition in conditions) {
            condition.onComplete(player)
        }
        return true
    }

    fun addToConfig(section: ConfigurationSection) {
        conditions.forEachIndexed { index, condition ->
            condition.addToConfig(section.createSection("${index + 1}"))
        }
    }

    /** A single requirement that must be satisfied to open a loot chest. */
    sealed interface Condition {
        fun isFulfilled(player: Player): Boolean

        fun onDeny(player: Player)

        fun onComplete(player: Player)

        fun addToConfig(section: ConfigurationSection)
    }

    /**
     * Requires the player to have a certain number of a specific game item, optionally consuming
     * them on success.
     */
    class ItemCondition(
        private val templateID: String,
        private val displayName: String,
        private val count: Int,
        private val takeItem: Boolean,
        private val converter: ItemStackConverter,
        private val referenceStack: org.bukkit.inventory.ItemStack,
    ) : Condition {

        override fun isFulfilled(player: Player): Boolean {
            return ItemInventoryUtil.hasItem(converter, player, referenceStack, count)
        }

        override fun onDeny(player: Player) {
            player.sendMessage(
                Component.text(
                    "You cannot open this loot chest: missing $displayName",
                    NamedTextColor.RED,
                )
            )
        }

        override fun onComplete(player: Player) {
            if (takeItem) {
                ItemInventoryUtil.takeItem(converter, player, referenceStack, count)
            }
        }

        override fun addToConfig(section: ConfigurationSection) {
            section.set("type", "item")
            section.set("template-id", templateID)
            section.set("count", count)
            section.set("take-item", takeItem)
        }
    }

    companion object {

        fun loadFromConfig(
            section: ConfigurationSection,
            templateRegistry: GameItemTemplateRegistry,
            converter: ItemStackConverter,
        ): LootChestConditions {
            val conditions = mutableListOf<Condition>()
            for (key in section.getKeys(false)) {
                val sub = section.getConfigurationSection(key) ?: continue
                val type = sub.getString("type") ?: continue
                if (!type.equals("item", ignoreCase = true)) {
                    logger.warn("Unknown loot chest condition type '{}', skipping", type)
                    continue
                }
                val templateID = sub.getString("template-id")
                if (templateID == null) {
                    logger.warn(
                        "LootChestConditions item condition missing 'template-id', skipping"
                    )
                    continue
                }
                val template = templateRegistry.getItemTemplate(templateID)
                if (template == null) {
                    logger.warn(
                        "LootChestConditions item condition references unknown template '{}', skipping",
                        templateID,
                    )
                    continue
                }
                val count = sub.getInt("count", 1)
                val takeItem = sub.getBoolean("take-item", true)

                val itemData = template.generateItemData()
                val gameItem = templateRegistry.generateGameItem(itemData)
                val referenceStack = gameItem.generateItemStack(1)

                conditions.add(
                    ItemCondition(
                        templateID = templateID,
                        displayName = template.display.name.content(),
                        count = count,
                        takeItem = takeItem,
                        converter = converter,
                        referenceStack = referenceStack,
                    )
                )
            }
            return LootChestConditions(conditions)
        }
    }
}
