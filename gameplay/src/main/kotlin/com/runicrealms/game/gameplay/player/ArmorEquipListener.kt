package com.runicrealms.game.gameplay.player

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.runicrealms.game.common.event.ArmorEquipEvent
import com.runicrealms.game.common.event.ArmorEquipEvent.ArmorType
import com.runicrealms.game.common.event.ArmorEquipEvent.EquipMethod
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Call our custom ArmorEquipEvent to listen for player gear changes Most events run last to wait
 * for other plugins Handles off-hands now as well
 */
class ArmorEquipListener @Inject constructor(private val plugin: Plugin) : Listener {
    /** Handles all methods of equipping armor from the inventory screen (shifting, etc.) */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun inventoryClick(e: InventoryClickEvent) {
        if (e.isCancelled) return
        if (e.action == InventoryAction.NOTHING) return
        var shift = false
        var numberKey = false
        if (e.click == ClickType.SHIFT_LEFT || e.click == ClickType.SHIFT_RIGHT) {
            shift = true
        }
        if (e.click == ClickType.NUMBER_KEY) {
            numberKey = true
        }
        if (
            e.slotType != InventoryType.SlotType.ARMOR &&
                e.slotType != InventoryType.SlotType.QUICKBAR &&
                e.slotType != InventoryType.SlotType.CONTAINER
        )
            return
        if (e.clickedInventory != null && e.clickedInventory!!.type != InventoryType.PLAYER) return
        if (e.inventory.type != InventoryType.CRAFTING && e.inventory.type != InventoryType.PLAYER)
            return
        if (e.whoClicked !is Player) return
        var newArmorType = ArmorType.matchType(if (shift) e.currentItem else e.cursor)
        if (!shift && newArmorType != null && e.rawSlot != newArmorType.slot) {
            // Used for drag and drop checking to make sure you aren't trying to place a helmet in
            // the boots slot.
            return
        }
        if (shift) {
            newArmorType = ArmorType.matchType(e.currentItem)
            if (newArmorType != null) {
                val equipping = e.rawSlot != newArmorType.slot
                if (
                    newArmorType == ArmorType.HELMET &&
                        (equipping == isAirOrNull(e.whoClicked.inventory.helmet)) ||
                        newArmorType == ArmorType.CHESTPLATE &&
                            (equipping == isAirOrNull(e.whoClicked.inventory.chestplate)) ||
                        newArmorType == ArmorType.LEGGINGS &&
                            (equipping == isAirOrNull(e.whoClicked.inventory.leggings)) ||
                        newArmorType == ArmorType.BOOTS &&
                            (equipping == isAirOrNull(e.whoClicked.inventory.boots))
                ) {
                    val armorEquipEvent =
                        ArmorEquipEvent(
                            e.whoClicked as Player,
                            EquipMethod.SHIFT_CLICK,
                            newArmorType,
                            (if (equipping) null else e.currentItem),
                            (if (equipping) e.currentItem else null),
                        )
                    Bukkit.getServer().pluginManager.callEvent(armorEquipEvent)
                    if (armorEquipEvent.isCancelled()) {
                        e.isCancelled = true
                    }
                }
            }
        } else {
            var newArmorPiece = e.cursor
            var oldArmorPiece = e.currentItem
            if (numberKey) {
                if (
                    e.clickedInventory!!.type == InventoryType.PLAYER
                ) { // Prevents shit in the 2by2 crafting
                    // e.getClickedInventory() == The players inventory
                    // e.getHotBarButton() == key people are pressing to equip or unequip the item
                    // to or from.
                    // e.getRawSlot() == The slot the item is going to.
                    // e.getSlot() == Armor slot, can't use e.getRawSlot() as that gives a hotbar
                    // slot ;-;
                    val hotbarItem = e.clickedInventory!!.getItem(e.hotbarButton)
                    if (!isAirOrNull(hotbarItem)) { // Equipping
                        newArmorType = ArmorType.matchType(hotbarItem)
                        newArmorPiece = hotbarItem!!
                        oldArmorPiece = e.clickedInventory!!.getItem(e.slot)
                    } else { // un-equipping
                        newArmorType =
                            ArmorType.matchType(
                                if (!isAirOrNull(e.currentItem)) e.currentItem else e.cursor
                            )
                    }
                }
            } else {
                if (
                    isAirOrNull(e.cursor) && !isAirOrNull(e.currentItem)
                ) { // un-equip with no new item going into the slot.
                    newArmorType = ArmorType.matchType(e.currentItem)
                }
                // e.getCurrentItem() == Unequip
                // e.getCursor() == Equip
                // newArmorType = ArmorType.matchType(!isAirOrNull(e.getCurrentItem()) ?
                // e.getCurrentItem() : e.getCursor());
            }
            if (newArmorType != null && e.rawSlot == newArmorType.slot) {
                var method = EquipMethod.PICK_DROP
                if (e.action == InventoryAction.HOTBAR_SWAP || numberKey)
                    method = EquipMethod.HOTBAR_SWAP
                val armorEquipEvent =
                    ArmorEquipEvent(
                        e.whoClicked as Player,
                        method,
                        newArmorType,
                        oldArmorPiece!!,
                        newArmorPiece,
                    )
                Bukkit.getServer().pluginManager.callEvent(armorEquipEvent)
                if (armorEquipEvent.isCancelled()) {
                    e.isCancelled = true
                }
            }
        }
    }

    /** Handles clicking and dragging items to equip them */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun inventoryDrag(event: InventoryDragEvent) {
        if (event.rawSlots.isEmpty()) return
        /*
        getType() seems to always be even.
        old Cursor gives the item you are equipping
        raw slot is the ArmorType slot
        can't replace armor using this method making getCursor() useless.
         */
        val type = ArmorType.matchType(event.oldCursor)
        if (type != null && type.slot == event.rawSlots.stream().findFirst().orElse(0)) {
            val armorEquipEvent =
                ArmorEquipEvent(
                    event.whoClicked as Player,
                    EquipMethod.DRAG,
                    type,
                    null,
                    event.oldCursor,
                )
            Bukkit.getServer().pluginManager.callEvent(armorEquipEvent)
            if (armorEquipEvent.isCancelled()) {
                event.result = Event.Result.DENY
                event.isCancelled = true
            }
        }
    }

    /**
     * Fixes a bug that causes inventory to be out-of-sync (probably due to all our dupe or
     * inventory checks)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onArmorEquip(e: InventoryClickEvent) {
        if (!(e.click == ClickType.SHIFT_LEFT || e.click == ClickType.SHIFT_RIGHT)) return
        if (e.whoClicked !is Player) return
        plugin.launch { (e.whoClicked as Player).updateInventory() }
    }

    /** Handles right-clicking an armor piece to equip it */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun playerInteractEvent(event: PlayerInteractEvent) {
        if (event.useItemInHand() == Event.Result.DENY) return
        if (event.action == Action.PHYSICAL) return
        if (!(event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK))
            return
        val player = event.player
        if (event.useInteractedBlock() != Event.Result.DENY) {
            if (
                event.clickedBlock != null &&
                    event.action == Action.RIGHT_CLICK_BLOCK &&
                    !player.isSneaking
            ) {
                // Some blocks have actions when you right-click them which stop the client from
                // equipping the armor in hand
                val mat = event.clickedBlock!!.type
                if (BLOCKED_MATERIALS.contains(mat)) return
            }
        }

        val newArmorType = ArmorType.matchType(event.item) ?: return

        if (
            newArmorType == ArmorType.HELMET ||
                newArmorType == ArmorType.CHESTPLATE ||
                newArmorType == ArmorType.LEGGINGS ||
                newArmorType == ArmorType.BOOTS
        ) {
            val armorEquipEvent =
                ArmorEquipEvent(
                    event.player,
                    EquipMethod.HOTBAR,
                    newArmorType,
                    event.player.inventory.getItem(newArmorType.equipmentSlot),
                    event.item!!,
                )
            Bukkit.getServer().pluginManager.callEvent(armorEquipEvent)
            if (armorEquipEvent.isCancelled()) {
                event.isCancelled = true
                player.updateInventory()
            }
        }
    }

    companion object {
        private val BLOCKED_MATERIALS =
            HashSet<Material>().apply {
                add(Material.CAMPFIRE)
                add(Material.FURNACE)
                add(Material.CHEST)
                add(Material.TRAPPED_CHEST)
                add(Material.BEACON)
                add(Material.DISPENSER)
                add(Material.DROPPER)
                add(Material.HOPPER)
                add(Material.CRAFTING_TABLE)
                add(Material.ENCHANTING_TABLE)
                add(Material.ENDER_CHEST)
                add(Material.ANVIL)
                addAll(Material.entries.filter { it.name.contains("_BED") })
                addAll(Material.entries.filter { it.name.contains("_FENCE_GATE") })
                addAll(Material.entries.filter { it.name.contains("_DOOR") })
                addAll(Material.entries.filter { it.name.contains("_BUTTON") })
                addAll(Material.entries.filter { it.name.contains("_TRAPDOOR") })
                addAll(Material.entries.filter { it.name.contains("_FENCE") })
                add(Material.REPEATER)
                add(Material.COMPARATOR)
                add(Material.BREWING_STAND)
                add(Material.CAULDRON)
                addAll(Material.entries.filter { it.name.contains("_SIGN") })
                add(Material.LEVER)
                addAll(Material.entries.filter { it.name.contains("_SHULKER_BOX") })
            }

        /** A utility method to support versions that use null or air ItemStacks. */
        fun isAirOrNull(item: ItemStack?): Boolean {
            return item == null || item.type == Material.AIR
        }
    }
}
