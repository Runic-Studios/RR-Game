package com.runicrealms.game.items.perk

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import com.runicrealms.game.items.generator.AddedStatsHolder
import com.runicrealms.game.items.generator.GameItem
import kotlin.math.floor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.inventory.ItemStack

/** A class that handles the replacement of a stat that uses percentages */
class DynamicItemPerkPercentStatPlaceholder(
    placeholder: String,
    private val handler: GameItemPerkHandler,
    private val supplier: () -> Double,
    characterEquipmentCacheRegistry: CharacterEquipmentCacheRegistry,
) : DynamicItemPerkTextPlaceholder(placeholder, characterEquipmentCacheRegistry) {

    override fun generateReplacement(
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String {
        val basePercentage = roundToNearestHundredth(supplier() * 100)
        var percentage =
            if (
                this.getEquippedSlot(viewer.player, gameItem, itemStack) != null
            ) { // Item is equipped
                handler.getCurrentStacks(viewer) * basePercentage
            } else {
                val addedStats = (gameItem as AddedStatsHolder).addedStats
                val stacks =
                    addedStats.perks
                        ?.singleOrNull { it.perkID == handler.template.identifier }
                        ?.stacks ?: 0
                stacks * basePercentage
            }
        percentage = roundToNearestHundredth(percentage)

        val component =
            if (percentage != basePercentage) {
                Component.text()
                    .append(
                        Component.text(
                            formatStringPercentage(basePercentage) + "%",
                            Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH),
                        )
                    )
                    .append(
                        Component.text(
                            " " + formatStringPercentage(percentage) + "%",
                            Style.style(NamedTextColor.YELLOW),
                        )
                    )
                    .build()
            } else {
                Component.text(
                    formatStringPercentage(basePercentage) + "%",
                    Style.style(NamedTextColor.YELLOW),
                )
            }
        return LegacyComponentSerializer.legacySection().serialize(component)
    }

    companion object {
        private fun roundToNearestHundredth(number: Double): Double {
            return Math.round(number * 100.0) / 100.0
        }

        /** Turns things like 10.0% into 10% But leaves 10.5% as 10.5% */
        private fun formatStringPercentage(percentage: Double): String {
            return if (floor(percentage) == percentage) percentage.toInt().toString()
            else percentage.toString()
        }
    }
}
