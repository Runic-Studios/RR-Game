package com.runicrealms.game.items.generator

import com.runicrealms.game.items.config.item.GameItemTemplate
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import de.tr7zw.nbtapi.NBT
import java.util.concurrent.ThreadLocalRandom
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta

/**
 * This class (and its subclasses) are meant to be very lightweight wrappers around the
 * protobuf-generated class ItemData.
 *
 * These classes should provide easy conversion from ItemData into ItemStacks (through lore and item
 * generation), as well as useful information like added stats.
 *
 * Additional rule of thumb: Any fields that are only utilized in generation methods or by external
 * classes should always be lazy.
 *
 * We NEED loading of this class to be as fast as possible since it occurs extremely frequently.
 */
sealed class GameItem(protected var data: ItemData, val template: GameItemTemplate) {

    open fun generateItemStack(count: Int, menuDisplay: Boolean = false): ItemStack {
        val itemStack = template.display.generateItem(count)
        val meta = itemStack.itemMeta ?: Bukkit.getItemFactory().getItemMeta(itemStack.type)
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, attributeModifier)
        val lore = generateLore(menuDisplay)

        if (template.tags.isNotEmpty()) {
            for (tag in template.tags) {
                lore.add(tag.display)
            }
        }

        if (template.lore.isNotEmpty()) {
            val extra =
                if (lore.size != 0) {
                    mutableListOf<TextComponent>().apply {
                        addAll(template.lore)
                        add(Component.text(""))
                    }
                } else template.lore
            lore.addAll(0, extra)
        }

        // set other flags
        meta.isUnbreakable = true
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        if (itemStack.type == Material.POTION) {
            (meta as PotionMeta).color = getPotionColor(template)
        }
        meta.lore(lore)
        itemStack.setItemMeta(meta)

        // Modify NBT
        NBT.modify(itemStack) {
            it.setByteArray("data", data.toByteArray()) // TODO determine if needed when menuDisplay
        }
        return itemStack
    }

    protected abstract fun generateLore(menuDisplay: Boolean = false): MutableList<TextComponent>

    fun setCustomData(key: String, value: String) {
        val builder = data.toBuilder()
        builder.customDataMap[key] = value
        data = builder.build()
    }

    fun getCustomData(key: String): String? {
        return data.customDataMap[key]
    }

    protected fun ItemData.RolledStat.getRolledValue(range: GameItemTemplate.StatRange): Int {
        return range.min + (rollPercentage * range.max - range.min + 1).toInt()
    }

    protected fun correctStatRolls(
        rolls: List<ItemData.RolledStat>,
        ranges: Map<StatType, GameItemTemplate.StatRange>,
    ): CorrectedStatRolls {
        val correctedRolls = mutableListOf<ItemData.RolledStat>()
        val calculatedRolls = LinkedHashMap<StatType, Int>()
        var modified = false
        for (roll in rolls) {
            if (!ranges.containsKey(roll.type)) {
                modified = true
                continue
            }
            correctedRolls.add(roll)
            calculatedRolls[roll.type] = roll.getRolledValue(ranges[roll.type] ?: continue)
        }
        for ((statType, statRange) in ranges) {
            if (calculatedRolls.containsKey(statType)) continue
            val roll =
                ItemData.RolledStat.newBuilder()
                    .setType(statType)
                    .setRollPercentage(ThreadLocalRandom.current().nextDouble())
                    .build()
            correctedRolls.add(roll)
            modified = true
            calculatedRolls[statType] = roll.getRolledValue(statRange)
        }
        return CorrectedStatRolls(correctedRolls, calculatedRolls, modified)
    }

    protected data class CorrectedStatRolls(
        val correctedRolls: List<ItemData.RolledStat>,
        val calculatedRolls: LinkedHashMap<StatType, Int>,
        val modified: Boolean,
    )

    protected fun correctPerks(
        existingPerks: List<ItemData.Perk>,
        defaultPerks: Map<String, Int>,
    ): CorrectedPerks {
        val correctedPerks = mutableListOf<ItemData.Perk>()
        var modified = false
        for (existingPerk in existingPerks) {
            val defaultStacks = defaultPerks.getOrDefault(existingPerk.perkID, 0)
            if (defaultStacks > existingPerk.stacks) {
                correctedPerks.add(existingPerk.toBuilder().setStacks(defaultStacks).build())
                modified = true
            } else {
                correctedPerks.add(existingPerk)
            }
        }
        for ((defaultPerkID, defaultPerkStacks) in defaultPerks) {
            if (existingPerks.any { it.perkID == defaultPerkID }) continue
            correctedPerks.add(
                ItemData.Perk.newBuilder()
                    .setPerkID(defaultPerkID)
                    .setStacks(defaultPerkStacks)
                    .build()
            )
            modified = true
        }
        return CorrectedPerks(correctedPerks, modified)
    }

    protected data class CorrectedPerks(
        val correctedPerks: List<ItemData.Perk>,
        val modified: Boolean,
    )

    companion object {
        private val attributeModifier =
            AttributeModifier(
                NamespacedKey("game", "generic.attack_speed"),
                Int.MAX_VALUE.toDouble(),
                AttributeModifier.Operation.ADD_NUMBER,
            )

        private fun getPotionColor(template: GameItemTemplate): Color {
            val property = template.extraProperties["color"] as? String
            val color = property?.lowercase()
            return when (color) {
                "aqua" -> Color.AQUA
                "black" -> Color.BLACK
                "fuchsia" -> Color.FUCHSIA
                "green" -> Color.GREEN
                "lime" -> Color.LIME
                "orange" -> Color.ORANGE
                "red" -> Color.RED
                else -> Color.WHITE
            }
        }
    }
}
