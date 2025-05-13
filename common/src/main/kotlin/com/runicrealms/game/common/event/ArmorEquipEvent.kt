package com.runicrealms.game.common.event

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack


class ArmorEquipEvent(
    player: Player,
    val equipType: EquipMethod,
    val type: ArmorType,
    val oldArmorPiece: ItemStack?,
    val newArmorPiece: ItemStack?,
    private var isCancelled: Boolean = false
): PlayerEvent(player, false), Cancellable {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS

    /**
     * Gets if this event is cancelled.
     *
     * @return If this event is cancelled
     */
    override fun isCancelled(): Boolean {
        return isCancelled
    }

    /**
     * Sets if this event should be cancelled.
     *
     * @param cancel If this event should be cancelled.
     */
    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    enum class EquipMethod {
        /*
         * When you shift click an armor piece to equip or un-equip
         */
        SHIFT_CLICK,

        /*
         * When you drag and drop the item to equip or un-equip
         */
        DRAG,

        /*
         * When you manually equip or un-equip the item. Used to be DRAG
         */
        PICK_DROP,

        /*
         * When you right-click an armor piece in the hotbar without the inventory open to equip
         */
        HOTBAR,

        /*
         * When you press the hotbar slot number while hovering over the armor slot to equip or un-equip
         */
        HOTBAR_SWAP,

        /**
         * When in range of a dispenser that shoots an armor piece to equip.<br></br>
         * Requires the spigot version to have [org.bukkit.event.block.BlockDispenseArmorEvent] implemented.
         */
        DISPENSER,

        /*
         * When an armor piece is removed due to it losing all durability
         */
        BROKE,

        /*
         * When you die causing all armor to un-equip
         */
        DEATH,
    }

    enum class ArmorType(val slot: Int, val equipmentSlot: EquipmentSlot) {
        HELMET(5, EquipmentSlot.HEAD),
        CHESTPLATE(6, EquipmentSlot.CHEST),
        LEGGINGS(7, EquipmentSlot.LEGS),
        BOOTS(8, EquipmentSlot.FEET),
        OFFHAND(40, EquipmentSlot.OFF_HAND);

        companion object {
            /**
             * Returns the type of armor. If none found, returns 'none'
             *
             * @param itemStack item in hand
             * @return type of runic weapon held
             */
            fun matchType(itemStack: ItemStack?): ArmorType? {
                if (itemStack == null) return null
                return when (itemStack.type) {
                    Material.CHAINMAIL_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.LEATHER_HELMET, Material.IRON_HELMET, Material.CARVED_PUMPKIN, Material.SKELETON_SKULL, Material.WITHER_SKELETON_SKULL, Material.CREEPER_HEAD, Material.DRAGON_HEAD, Material.PLAYER_HEAD, Material.ZOMBIE_HEAD -> HELMET
                    Material.CHAINMAIL_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE -> CHESTPLATE
                    Material.CHAINMAIL_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.LEATHER_LEGGINGS, Material.IRON_LEGGINGS -> LEGGINGS
                    Material.CHAINMAIL_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.LEATHER_BOOTS, Material.IRON_BOOTS -> BOOTS
                    else -> null
                }
            }
        }
    }
}