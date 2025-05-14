package com.runicrealms.game.items.command

import com.google.inject.Inject
import com.runicrealms.game.items.config.template.ClassTypeHolder
import com.runicrealms.game.items.config.template.GameItemArmorTemplate
import com.runicrealms.game.items.config.template.GameItemRarityType
import com.runicrealms.game.items.config.template.GameItemTemplate
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.game.items.config.template.GameItemWeaponTemplate
import com.runicrealms.game.items.config.template.RarityLevelHolder
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.util.EnumMap
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Stream
import org.bukkit.plugin.Plugin

class LootHelper
@Inject
constructor(
    private val plugin: Plugin,
    private val itemTemplateRegistry: GameItemTemplateRegistry,
) {

    companion object {

        private val WEIGHTS =
            mapOf(
                GameItemRarityType.COMMON to 20,
                GameItemRarityType.UNCOMMON to 15,
                GameItemRarityType.RARE to 4,
                GameItemRarityType.EPIC to 1,
            )

        private val RARITY_DROP_TABLE_WEIGHTED =
            ArrayList<GameItemRarityType>(20 + 15 + 4 + 1).apply {
                for (rarity in WEIGHTS.keys) {
                    for (i in 0..<WEIGHTS[rarity]!!) {
                        add(rarity)
                    }
                }
            }
        private val RARITY_ARMOR_WEAPON_DISTRIB_WEIGHTED =
            ArrayList<RarityItemType>(5).apply {
                repeat(4) { add(RarityItemType.ARMOR) }
                add(RarityItemType.WEAPON)
            }
    }

    // Maps level to a mapping between rarities and a list of available templates
    private val armorItems =
        HashMap<Int, MutableMap<GameItemRarityType, MutableList<GameItemTemplate>>>()

    // This includes artifacts because artifact instanceof weapon
    private val weaponItems =
        HashMap<Int, MutableMap<GameItemRarityType, MutableList<GameItemTemplate>>>()
    private val random = ThreadLocalRandom.current()

    /**
     * A method used to easily get an item that meets the following conditions, or null if none meet
     * the conditions
     *
     * @param range the item range, default 0-60
     * @param rarities the rarities allowed, default COMMON, UNCOMMON, RARE, EPIC
     * @param playerClass the usable classes of the item to pick randomly from, default is all
     *   classes
     * @param itemTypes the type of item
     * @param lqm where 1.0 is the default value, values below 1.0 cause your loot to favor more
     *   common items (you get less epics and rares, more commons etc) and above 1.0 favors rarer
     *   items
     * @return a future/promise for an item that meets the following conditions, or null if none
     *   meet the conditions
     */
    fun getItem(
        range: Pair<Int, Int>?,
        rarities: Set<GameItemRarityType>?,
        playerClass: ClassType?,
        itemTypes: Set<ItemType>?,
        lqm: Float?, // TODO use lqm
    ): GameItemTemplate? {
        val rarity: Set<GameItemRarityType?> =
            if (!rarities.isNullOrEmpty()) rarities else setOf(rollRarity())
        val options = mutableListOf<GameItemTemplate>()
        for (template in itemTemplateRegistry.getItemTemplates()) {
            if (template !is RarityLevelHolder) continue
            if (!template.id.startsWith("script")) continue
            if (range != null && range.first <= range.second) {
                if (range.first > template.level && range.second < template.level) continue
            }
            if (!rarity.contains(template.rarity)) continue
            if (playerClass != null && playerClass != ClassType.ANY) {
                if (template !is ClassTypeHolder) continue
                if (template.classType != playerClass) continue
            }
            if (itemTypes != null && !itemTypes.contains(ItemType.getItemType(template))) continue
            options.add(template)
        }
        if (options.isEmpty()) return null
        return options[random.nextInt(0, options.size)]
    }

    fun getTemplatesInLevel(level: Int, rarity: GameItemRarityType): List<GameItemTemplate> {
        if (!armorItems.containsKey(level) && !weaponItems.containsKey(level)) return emptyList()
        if (!armorItems.containsKey(level)) return weaponItems[level]!![rarity]!!
        if (!weaponItems.containsKey(level)) return armorItems[level]!![rarity]!!
        return Stream.concat<GameItemTemplate>(
                armorItems[level]!![rarity]!!.stream(),
                weaponItems[level]!![rarity]!!.stream(),
            )
            .toList()
    }

    /**
     * Gets a random item in level range, INCLUSIVE.
     *
     * @param min - minimum levels
     * @param max - maximum level
     * @return random item
     */
    fun getRandomItemInRange(min: Int, max: Int): GameItemTemplate? {
        var amount = 0
        val rarity = rollRarity()
        val rarityItems =
            when (
                RARITY_ARMOR_WEAPON_DISTRIB_WEIGHTED[
                    random.nextInt(RARITY_ARMOR_WEAPON_DISTRIB_WEIGHTED.size)]
            ) {
                RarityItemType.ARMOR -> armorItems
                RarityItemType.WEAPON -> weaponItems
            }

        for (i in min..max) {
            if (rarityItems.containsKey(i)) {
                if (rarityItems[i]!!.containsKey(rarity)) amount += rarityItems[i]!![rarity]!!.size
            }
        }

        var itemNumber = random.nextInt(amount)
        for (i in min..max) {
            if (!rarityItems.containsKey(i)) continue
            if (!rarityItems[i]!!.containsKey(rarity)) continue
            if (itemNumber >= rarityItems[i]!![rarity]!!.size) {
                itemNumber -= rarityItems[i]!![rarity]!!.size
            } else {
                return rarityItems[i]!![rarity]!![itemNumber]
            }
        }
        return null
    }

    /**
     * A method used to get a random rarity from the given rarity options (only valid options are
     * COMMON, UNCOMMON, RARE, EPIC)
     *
     * @param rarities the given rarity options (only valid options are COMMON, UNCOMMON, RARE,
     *   EPIC)
     * @return a random rarity from the given rarity options
     */
    private fun rollRarity(rarities: Set<GameItemRarityType>): GameItemRarityType {
        val probabilityDistribution = HashMap<GameItemRarityType, Double>()
        probabilityDistribution[GameItemRarityType.EPIC] = 1.0 / 40
        probabilityDistribution[GameItemRarityType.RARE] = 1.0 / 10
        probabilityDistribution[GameItemRarityType.UNCOMMON] = 3.0 / 8
        probabilityDistribution[GameItemRarityType.COMMON] = 1.0 / 2

        for ((key, value) in probabilityDistribution) {
            if (!probabilityDistribution.containsKey(key) || rarities.contains(key)) {
                continue
            }

            probabilityDistribution.remove(key)

            if (probabilityDistribution.isEmpty()) {
                continue
            }

            val prob = value / probabilityDistribution.size
            probabilityDistribution.forEach { (itemRarity: GameItemRarityType, existing: Double) ->
                probabilityDistribution[itemRarity] = existing + prob
            }
        }

        if (probabilityDistribution.isEmpty()) {
            return rollRarity()
        }

        val random = Math.random()
        for ((key, value) in probabilityDistribution) {
            if (random <= value) {
                return key
            }
        }

        // if the players luck was so bad that they didn't even win a common, give them a common
        return GameItemRarityType.COMMON
    }

    private fun rollRarity(): GameItemRarityType {
        return RARITY_DROP_TABLE_WEIGHTED[random.nextInt(RARITY_DROP_TABLE_WEIGHTED.size)]
    }

    fun sortItems(templates: Map<String, GameItemTemplate>) {
        for ((identifier, template) in templates) {
            if (!identifier.startsWith("script")) continue
            if (template !is RarityLevelHolder) continue
            val rarityItems =
                when (template) {
                    is GameItemArmorTemplate -> armorItems
                    is GameItemWeaponTemplate -> weaponItems
                    else -> continue
                }

            if (!rarityItems.containsKey(template.level))
                rarityItems[template.level] = EnumMap(GameItemRarityType::class.java)
            if (!rarityItems[template.level]!!.containsKey(template.rarity)) {
                (rarityItems[template.level])!![template.rarity] = LinkedList<GameItemTemplate>()
            }
            rarityItems[template.level]!![template.rarity]!!.add(template)
        }
    }

    enum class ItemType {
        WEAPON,
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS;

        companion object {
            fun getItemType(item: String): ItemType? {
                return try {
                    valueOf(item.uppercase(Locale.getDefault()))
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            fun getItemType(item: GameItemTemplate): ItemType {
                if (item is GameItemWeaponTemplate) {
                    return WEAPON
                }

                check(item is GameItemArmorTemplate) { "item " + item.id + " not armor or weapon" }

                for (type in entries) {
                    if (item.display.material.name.contains(type.name)) {
                        return type
                    }
                }

                throw IllegalStateException(
                    "item ${item.id} is a type of armor that is not implemented"
                )
            }
        }
    }

    private enum class RarityItemType {
        WEAPON,
        ARMOR,
    }
}
