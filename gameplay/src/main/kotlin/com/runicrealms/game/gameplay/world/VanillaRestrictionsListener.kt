package com.runicrealms.game.gameplay.world

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Animals
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import org.bukkit.entity.Tameable
import org.bukkit.entity.Wolf
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityEnterLoveModeEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.plugin.Plugin

private val BLOCKED_INTERACT_TYPES = setOf(
    Material.BELL,
    Material.JUKEBOX,
    Material.NOTE_BLOCK,
    Material.LODESTONE,
    Material.TRAPPED_CHEST,
    Material.CHEST,
    Material.ENDER_CHEST,
    Material.DISPENSER,
    Material.DROPPER,
    Material.RESPAWN_ANCHOR,
    Material.REDSTONE_ORE,
)

/**
 * Consolidates all "cancel vanilla feature" restrictions that require no external game logic.
 * Covers block interaction, crafting, inventory actions, creature spawning, and entity feeding.
 */
@Singleton
class VanillaRestrictionsListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    // --- Inventory ---

    /** Prevents number-key hotbar swaps and "collect to cursor" inventory actions. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.click == ClickType.NUMBER_KEY || event.click == ClickType.DOUBLE_CLICK) {
            event.isCancelled = true
        }
    }

    // --- Blocks ---

    /** Prevents non-creative players from placing blocks. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.player.gameMode != GameMode.CREATIVE) event.isCancelled = true
    }

    /** Prevents non-creative players from breaking blocks, and always prevents trapdoor use. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.player.gameMode != GameMode.CREATIVE) event.isCancelled = true
    }

    /** Prevents item frames and paintings from being destroyed by entities. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onHangingBreak(event: HangingBreakByEntityEvent) {
        if (event.remover !is Player) {
            event.isCancelled = true
            return
        }
        val player = event.remover as Player
        if (player.gameMode != GameMode.CREATIVE) event.isCancelled = true
    }

    /**
     * Blocks various player-block interactions:
     * - Farmland trampling (physical action)
     * - Flower pots and potted plants
     * - Blocked block types (bells, jukeboxes, chests, etc.)
     * - Shulker boxes
     * - Trapdoors (right-click)
     * - Pumpkin shearing (shears on pumpkin)
     * - Ender pearl throwing
     * - Campfire cooking (physical action on campfire)
     * - Bed interaction
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock

        // Prevent ender pearl throwing
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            if (player.inventory.itemInMainHand.type == Material.ENDER_PEARL) {
                event.isCancelled = true
                return
            }
        }

        if (block == null) return
        if (player.gameMode == GameMode.CREATIVE) return

        val blockType = block.type

        // Prevent farmland trampling
        if (event.action == Action.PHYSICAL
            && (blockType == Material.FARMLAND || blockType == Material.LEGACY_SOIL)
        ) {
            event.isCancelled = true
            return
        }

        // Prevent campfire cooking
        if (event.action == Action.PHYSICAL && blockType == Material.CAMPFIRE) {
            event.isCancelled = true
            return
        }

        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            // Prevent flower pot interactions
            if (blockType == Material.FLOWER_POT || blockType.toString().startsWith("POTTED_")) {
                event.isCancelled = true
                return
            }

            // Prevent shulker box opening
            if (blockType.toString().lowercase().contains("shulker")) {
                event.isCancelled = true
                return
            }

            // Prevent pumpkin shearing
            if (blockType == Material.PUMPKIN
                && player.inventory.itemInMainHand.type == Material.SHEARS
            ) {
                event.isCancelled = true
                return
            }

            // Prevent trapdoor usage
            if (blockType.toString().lowercase().contains("trapdoor")) {
                event.isCancelled = true
                return
            }

            // Prevent interaction with blocked block types
            if (BLOCKED_INTERACT_TYPES.contains(blockType)) {
                event.isCancelled = true
                return
            }
        }

        // Block PHYSICAL or RIGHT_CLICK on redstone ore
        if (event.action == Action.PHYSICAL || event.action == Action.RIGHT_CLICK_BLOCK) {
            if (blockType == Material.REDSTONE_ORE) {
                event.isCancelled = true
                return
            }
        }
    }

    /** Prevents non-creative players from emptying buckets. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (event.player.gameMode != GameMode.CREATIVE) event.isCancelled = true
    }

    // --- Crafting & Durability ---

    /** Disables vanilla crafting for non-creative players. Custom crafting is used instead. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onCraft(event: CraftItemEvent) {
        if ((event.whoClicked as? Player)?.gameMode != GameMode.CREATIVE) event.isCancelled = true
    }

    /** Disables item durability loss entirely. A custom durability system is used instead. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemDamage(event: PlayerItemDamageEvent) {
        event.isCancelled = true
    }

    // --- Hand & Equipment ---

    /** Prevents the swap-hands action (F key by default). */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onSwapHands(event: PlayerSwapHandItemsEvent) {
        event.isCancelled = true
    }

    /** Prevents non-creative players from manipulating armor stands. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (event.player.gameMode != GameMode.CREATIVE) event.isCancelled = true
    }

    // --- Creature Spawning ---

    /**
     * Prevents natural mob spawning. Also blocks jockey mobs (spider jockeys, chicken jockeys).
     * MythicMobs spawning and plugin spawns are unaffected since they do not use these reasons.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        when (event.spawnReason) {
            CreatureSpawnEvent.SpawnReason.DEFAULT,
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.JOCKEY -> event.isCancelled = true
            else -> {}
        }
    }

    /** Prevents jockey passengers (chicken jockeys, spider jockeys) from being added. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onNoJockeys(event: CreatureSpawnEvent) {
        if (event.spawnReason == CreatureSpawnEvent.SpawnReason.MOUNT) event.isCancelled = true
    }

    // --- Entity Interactions ---

    /**
     * Prevents mobs from trampling farmland and similar blocks.
     * Player physical interactions are handled by [onPlayerInteract].
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onMobTrample(event: EntityInteractEvent) {
        if (event.entity !is Player) event.isCancelled = true
    }

    /**
     * Prevents players from feeding horses (which would heal them) and interacting with item frames
     * via entity interaction.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.player.gameMode == GameMode.CREATIVE) return
        if (event.rightClicked.type == EntityType.ITEM_FRAME) {
            event.isCancelled = true
            return
        }
        if (event.rightClicked is Horse) {
            val item = event.player.inventory.itemInMainHand
            if (item.type == Material.APPLE
                || item.type == Material.GOLDEN_APPLE
                || item.type == Material.ENCHANTED_GOLDEN_APPLE
                || item.type == Material.GOLDEN_CARROT
                || item.type == Material.SUGAR
                || item.type == Material.WHEAT
                || item.type == Material.HAY_BLOCK
            ) {
                event.isCancelled = true
            }
        }
    }

    /**
     * Prevents players from feeding tameable or breedable animals (wolves, cats, llamas, etc.).
     * This blocks both taming attempts and feeding outside creative mode.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onFeedAnimal(event: PlayerInteractEntityEvent) {
        if (event.player.gameMode == GameMode.CREATIVE) return
        val entity = event.rightClicked
        if (entity is Wolf || entity is Tameable || entity is Animals) {
            val item = event.player.inventory.itemInMainHand
            if (item.type != Material.AIR) {
                // Only cancel if the item is a food that would trigger taming/healing/breeding
                // We cancel broadly to prevent all feeding interactions for non-creative players
                if (entity !is Player) event.isCancelled = true
            }
        }
    }

    /** Prevents animals from entering love mode (breeding). */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onLoveMode(event: EntityEnterLoveModeEvent) {
        event.isCancelled = true
    }

    // --- Animal Shearing ---

    /** Prevents sheep from being sheared. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onShear(event: PlayerShearEntityEvent) {
        if (event.entity is Sheep) event.isCancelled = true
    }

    // --- Beds ---

    /** Prevents players from entering beds. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onBedEnter(event: PlayerBedEnterEvent) {
        event.isCancelled = true
    }
}
