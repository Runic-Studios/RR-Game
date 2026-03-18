package com.runicrealms.game.items.dynamic.placeholder

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.dynamic.DynamicItemRegistry
import com.runicrealms.game.items.dynamic.DynamicItemTextPlaceholder
import com.runicrealms.game.items.generator.GameItem
import org.bukkit.inventory.ItemStack

/**
 * A [DynamicItemTextPlaceholder] that reads its replacement value directly from the item's
 * [com.runicrealms.game.data.model.ItemData.customData] map.
 *
 * This replaces the old RunicItemDynamic pattern: instead of a dedicated item type with a mutable
 * integer field, any generic item can store arbitrary values in customData and display them via
 * dynamic lore placeholders.
 *
 * Example: a gold pouch template has `<coins>` in its lore. Inject a [Factory] and call
 * `factory.create("coins", "coins")` to register a placeholder that replaces `<coins>` with the
 * value stored in `customData["coins"]`.
 *
 * @param identifier the placeholder identifier (without angle brackets)
 * @param key the key to look up in [com.runicrealms.game.data.model.ItemData.customData]
 */
class DynamicCustomDataTextPlaceholder
@AssistedInject
constructor(
    dynamicItemRegistry: DynamicItemRegistry,
    @Assisted identifier: String,
    @Assisted private val key: String,
) : DynamicItemTextPlaceholder(identifier) {

    interface Factory {
        fun create(identifier: String, key: String): DynamicCustomDataTextPlaceholder
    }

    init {
        dynamicItemRegistry.registerTextPlaceholder(this)
    }

    override fun generateReplacement(
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String {
        return gameItem.getCustomData(key) ?: "0"
    }
}
