package com.runicrealms.game.gameplay.player.ui

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.common.util.toLoreComponents
import com.runicrealms.game.data.UserDataRegistry
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import net.kyori.adventure.text.format.TextDecoration
import nl.odalitadevelopments.menus.OdalitaMenus
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin

/**
 * Manages the five crafting-grid icons displayed via ProtocolLib packets. Only the stats icon
 * (slot 2) is functional; the remaining icons are stubs pending future migration.
 */
@Singleton
class PlayerMenuManager
@Inject
constructor(
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val odalitaMenus: OdalitaMenus,
    private val statsMenuFactory: StatsMenu.Factory,
) : Listener {

    private companion object {
        const val PLAYER_CRAFT_INV_SIZE = 5
        val PLAYER_CRAFTING_SLOTS: Set<Int> = setOf(1, 2, 3, 4)

        // 10 ticks * 50 ms/tick
        const val REFRESH_DELAY_MS = 500L
    }

    private val craftingSlotPackets: ConcurrentHashMap<UUID, Set<PacketContainer>> =
        ConcurrentHashMap()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        // Runs on the MC main dispatcher via plugin.launch; delay() suspends without blocking.
        plugin.launch {
            while (true) {
                delay(REFRESH_DELAY_MS)
                for (character in userDataRegistry.getAllCharacters()) {
                    val player = character.bukkitPlayer
                    val view = player.openInventory
                    if (!isPlayerCraftingInv(view)) continue
                    val packets = craftingSlotPackets[player.uniqueId] ?: continue
                    for (packet in packets) {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
                    }
                }
            }
        }
    }

    private fun isPlayerCraftingInv(view: InventoryView): Boolean =
        view.topInventory.size == PLAYER_CRAFT_INV_SIZE

    private fun constructCraftingSlotPacket(slot: Int, item: ItemStack): PacketContainer {
        val packet =
            ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SET_SLOT)
        packet.integers.write(0, 0) // window ID
        packet.integers.write(2, slot) // slot ID
        packet.itemModifier.write(0, item)
        return packet
    }

    private fun clearPlayerCraftingSlots(view: InventoryView) {
        view.setItem(0, null)
        view.setItem(1, null)
        view.setItem(2, null)
        view.setItem(3, null)
        view.setItem(4, null)
    }

    private fun buildIconItem(
        material: Material,
        name: String,
        description: String,
        player: Player? = null,
    ): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                if (material == Material.PLAYER_HEAD && player != null) {
                    (meta as SkullMeta).owningPlayer = player
                }
                meta.displayName(name.colorFormat().decoration(TextDecoration.ITALIC, false))
                meta.lore(description.toLoreComponents())
                meta.isUnbreakable = true
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            }
        }

    private fun mountMenuIcon(): ItemStack =
        buildIconItem(
            Material.SADDLE,
            "&eMount Menu",
            "\n&6&lCLICK\n&7To view your mount appearances\n&7and riding achievements!",
        )

    private fun profileIcon(player: Player): ItemStack =
        buildIconItem(
            Material.PLAYER_HEAD,
            "&e${player.name}'s Profile",
            "\n&6&lCLICK\n&7To view your settings\n&7and achievements!",
            player,
        )

    private fun statsMenuIcon(): ItemStack =
        buildIconItem(
            Material.REDSTONE,
            "&eCharacter Stats",
            "\n&6&lCLICK\n&7To view your character stats!",
        )

    private fun gatheringLevelIcon(): ItemStack =
        buildIconItem(
            Material.IRON_PICKAXE,
            "&eGathering Skills",
            "\n&6&lCLICK\n&7To view your gathering skills!\n&7They are account-wide!",
        ).apply {
            // HIDE_ATTRIBUTES often does not survive ProtocolLib SET_SLOT for tools; hide defaults explicitly.
            setData(
                DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay()
                    .addHiddenComponents(
                        DataComponentTypes.ATTRIBUTE_MODIFIERS,
                        DataComponentTypes.TOOL,
                        DataComponentTypes.WEAPON,
                    )
                    .build(),
            )
        }

    private fun donorPerksIcon(): ItemStack =
        buildIconItem(
            Material.EXPERIENCE_BOTTLE,
            "&eDonor Perks",
            "\n&6&lCLICK\n&7To view and activate donor perks!",
        )

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val view = player.openInventory
        if (isPlayerCraftingInv(view)) {
            clearPlayerCraftingSlots(view)
        }
        craftingSlotPackets[player.uniqueId] =
            setOf(
                constructCraftingSlotPacket(0, mountMenuIcon()),
                constructCraftingSlotPacket(1, profileIcon(player)),
                constructCraftingSlotPacket(2, statsMenuIcon()),
                constructCraftingSlotPacket(3, gatheringLevelIcon()),
                constructCraftingSlotPacket(4, donorPerksIcon()),
            )
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val view = player.openInventory
        if (isPlayerCraftingInv(view)) {
            clearPlayerCraftingSlots(view)
        }
        craftingSlotPackets.remove(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val view = event.view
        if (!isPlayerCraftingInv(view)) return
        view.setItem(1, null)
        view.setItem(2, null)
        view.setItem(3, null)
        view.setItem(4, null)
        view.topInventory.clear()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory ?: return
        if (clickedInventory.type != InventoryType.CRAFTING) return
        if (player.gameMode == GameMode.CREATIVE) return
        if (clickedInventory == event.view.bottomInventory) return
        if (!PLAYER_CRAFTING_SLOTS.contains(event.slot)) return
        event.isCancelled = true
        player.updateInventory()
        if (event.cursor.type != Material.AIR) return
        if (event.slot == 2) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f)
            odalitaMenus.openMenu(statsMenuFactory.create(), player)
        }
        // Slots 1, 3, 4: not yet migrated (profile, gathering, donor)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.inventory.type != InventoryType.CRAFTING) return
        if (player.gameMode == GameMode.CREATIVE) return
        if (event.inventory == event.view.bottomInventory) return
        if (event.inventorySlots.any { it in PLAYER_CRAFTING_SLOTS }) {
            event.isCancelled = true
        }
    }
}
