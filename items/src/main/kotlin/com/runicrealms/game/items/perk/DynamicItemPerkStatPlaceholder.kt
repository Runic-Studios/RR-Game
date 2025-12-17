package com.runicrealms.game.items.perk

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import com.runicrealms.game.items.generator.AddedStatsHolder
import com.runicrealms.game.items.generator.GameItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.inventory.ItemStack

/** A class that handles the replacement of a stat that stacks */
open class DynamicItemPerkStatPlaceholder(
    placeholder: String,
    private val handler: GameItemPerkHandler,
    private val supplier: () -> Int,
    characterEquipmentCacheRegistry: CharacterEquipmentCacheRegistry,
) : DynamicItemPerkTextPlaceholder(placeholder, characterEquipmentCacheRegistry) {

    override fun generateReplacement(
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String {
        val base: Int = supplier()
        val value =
            if (
                getEquippedSlot(viewer.bukkitPlayer, gameItem, itemStack) != null
            ) { // Item is equipped
                handler.getCurrentStacks(viewer) * base
            } else {
                val addedStats = (gameItem as AddedStatsHolder).addedStats
                val stacks =
                    addedStats.perks
                        ?.singleOrNull { it.perkID == handler.template.identifier }
                        ?.stacks ?: 0
                stacks * base
            }

        val component =
            if (value != base) {
                Component.text()
                    .append(
                        Component.text(
                            base,
                            Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH),
                        )
                    )
                    .append(Component.text(" $value", Style.style(NamedTextColor.YELLOW)))
                    .build()
            } else {
                Component.text(base, Style.style(NamedTextColor.YELLOW))
            }
        return LegacyComponentSerializer.legacySection().serialize(component)
    }
}
