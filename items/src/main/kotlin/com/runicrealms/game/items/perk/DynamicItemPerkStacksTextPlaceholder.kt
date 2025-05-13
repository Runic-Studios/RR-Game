package com.runicrealms.game.items.perk

import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import com.runicrealms.game.items.generator.GameItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.inventory.ItemStack

/** Used to replace the ? in [?/4] +X item perks */
class DynamicItemPerkStacksTextPlaceholder(
    private val handler: GameItemPerkHandler,
    characterEquipmentCacheRegistry: CharacterEquipmentCacheRegistry,
) :
    DynamicItemPerkTextPlaceholder(
        handler.template.identifier + "-equipped",
        characterEquipmentCacheRegistry,
    ) {

    override fun generateReplacement(
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String {
        val stacks: Int = handler.getCurrentUncappedStacks(viewer)
        val component =
            if (getEquippedSlot(viewer.player, gameItem, itemStack) != null) {
                val stacksColor =
                    if (stacks > handler.template.maxStacks) NamedTextColor.RED
                    else NamedTextColor.WHITE
                Component.text()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(stacks.toString(), stacksColor))
                    .append(Component.text("/${handler.template.maxStacks}]", NamedTextColor.GRAY))
                    .build()
            } else {
                Component.text("[${handler.template.maxStacks}]", Style.style(NamedTextColor.GRAY))
            }
        return LegacyComponentSerializer.legacySection().serialize(component)
    }
}
