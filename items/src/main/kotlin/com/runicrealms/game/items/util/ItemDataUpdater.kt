package com.runicrealms.game.items.util

import com.runicrealms.game.data.model.ItemData
import de.tr7zw.nbtapi.NBT
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Utility for reading and modifying the CBOR-encoded [ItemData] stored in an ItemStack's NBT.
 *
 * Used by any system that needs to mutate item data at runtime (gem socketing, gold pouches, etc.).
 */
@OptIn(ExperimentalSerializationApi::class)
object ItemDataUpdater {

    /**
     * Reads the [ItemData] from an ItemStack's NBT "data" tag.
     *
     * @return the decoded [ItemData], or null if the item has no game data
     */
    fun readItemData(itemStack: ItemStack): ItemData? {
        if (itemStack.type == Material.AIR) return null
        val compound = NBT.readNbt(itemStack)
        if (!compound.hasTag("data")) return null
        val bytes = compound.getByteArray("data") ?: return null
        return try {
            Cbor.decodeFromByteArray<ItemData>(bytes)
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Applies a transformation to the [ItemData] stored in an ItemStack's NBT, then writes the
     * result back.
     *
     * @param itemStack the item to modify (mutated in-place)
     * @param transform a function that receives the current [ItemData] and returns the new one
     * @return true if the update succeeded, false if the item had no game data
     */
    fun updateItemData(itemStack: ItemStack, transform: (ItemData) -> ItemData): Boolean {
        val currentData = readItemData(itemStack) ?: return false
        val newData = transform(currentData)
        NBT.modify(itemStack) { compound ->
            compound.setByteArray("data", Cbor.encodeToByteArray(newData))
        }
        return true
    }

    /**
     * Updates a single key in the item's customData map.
     *
     * @return true if the update succeeded, false if the item had no game data
     */
    fun updateCustomData(itemStack: ItemStack, key: String, value: String): Boolean {
        return updateItemData(itemStack) { data ->
            data.copy(customData = data.customData + (key to value))
        }
    }
}
