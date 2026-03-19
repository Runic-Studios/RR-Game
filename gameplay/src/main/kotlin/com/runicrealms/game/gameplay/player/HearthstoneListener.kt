package com.runicrealms.game.gameplay.player

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

private const val HEARTHSTONE_SLOT = 8
private const val TELEPORT_TIME_SECONDS = 5
private const val MOVE_CONSTANT = 0.6

/**
 * Manages the server hearthstone - a utility item in hotbar slot 8 that teleports the player
 * to their saved home location after a 5-second channel time.
 *
 * Teleportation is cancelled if the player moves or enters combat during the channel.
 *
 * TODO: Implement fully once the following are available:
 * - Hearthstone item template ID in GameItemTemplateRegistry
 * - SafeZoneRegistry for resolving the saved location identifier
 * - Character data field for storing the saved hearthstone location
 *   (requires Schema Migration per AGENTS.md)
 * - "Give hearthstone on character load" requires reading location from character data
 */
@Singleton
class HearthstoneListener
@Inject
constructor(
    private val plugin: Plugin,
    private val combatManager: CombatManager,
    private val userDataRegistry: UserDataRegistry,
) : Listener {

    private val currentlyUsing = HashMap<UUID, BukkitTask>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onHearthstoneUse(event: PlayerInteractEvent) {
        if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) return
        val player = event.player

        if (player.inventory.itemInMainHand.type == Material.AIR) return
        if (player.gameMode == GameMode.CREATIVE) return
        if (player.inventory.heldItemSlot != HEARTHSTONE_SLOT) return
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val hearthstone = player.inventory.getItem(HEARTHSTONE_SLOT) ?: return

        // TODO: Verify hearthstone is the correct game item (check templateID via ItemStackConverter)
        //   val gameItem = ItemStackConverter.convertToGameItem(hearthstone) ?: return
        //   if (gameItem.templateID != HEARTHSTONE_TEMPLATE_ID) return

        // TODO: Read saved location from character data
        //   val gameCharacter = userDataRegistry.getCharacter(player.uniqueId) ?: return
        //   val locationIdentifier = gameCharacter.document.character.traits.hearthstoneLocation ?: return
        //   val destination = SafeZoneRegistry.getLocationFromIdentifier(locationIdentifier) ?: return

        if (combatManager.isInCombat(player.uniqueId)) {
            player.sendMessage(Component.text("You can't use that in combat!", NamedTextColor.RED))
            return
        }

        if (currentlyUsing.containsKey(player.uniqueId)) return

        player.world.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.0f)

        // TODO: Pass actual destination location once SafeZoneRegistry is available
        //   currentlyUsing[player.uniqueId] = beginTeleportation(player, destination)
    }

    private fun beginTeleportation(player: Player, destination: Location): BukkitTask {
        val initX = Math.round(player.location.x * MOVE_CONSTANT)
        val initY = Math.round(player.location.y * MOVE_CONSTANT)
        val initZ = Math.round(player.location.z * MOVE_CONSTANT)

        var count = 0
        return plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val current = player.location

            // Cancel if player moved
            if (Math.round(current.x * MOVE_CONSTANT) != initX
                || Math.round(current.y * MOVE_CONSTANT) != initY
                || Math.round(current.z * MOVE_CONSTANT) != initZ
            ) {
                currentlyUsing.remove(player.uniqueId)?.cancel()
                player.sendMessage(Component.text("Teleportation cancelled due to movement!", NamedTextColor.RED))
                return@Runnable
            }

            // Cancel if in combat
            if (combatManager.isInCombat(player.uniqueId)) {
                currentlyUsing.remove(player.uniqueId)?.cancel()
                player.sendMessage(Component.text("Teleportation cancelled due to combat!", NamedTextColor.RED))
                return@Runnable
            }

            if (count >= TELEPORT_TIME_SECONDS) {
                currentlyUsing.remove(player.uniqueId)?.cancel()
                player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 60, 2))
                player.teleport(destination)
                player.world.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.0f)
                player.sendMessage(Component.text("You arrive at your location.", NamedTextColor.AQUA))
                return@Runnable
            }

            player.world.spawnParticle(
                Particle.DUST,
                player.location.add(0.0, 1.0, 0.0),
                10, 0.5, 0.5, 0.5,
                Particle.DustOptions(Color.fromRGB(0, 200, 255), 3f),
            )
            player.sendMessage(
                Component.text("Teleporting... ", NamedTextColor.AQUA)
                    .append(Component.text("${TELEPORT_TIME_SECONDS - count}s", NamedTextColor.WHITE)),
            )
            count++
        }, 0L, 20L)
    }

    /** Prevents moving the hearthstone out of slot 8. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (player.gameMode == GameMode.CREATIVE) return
        if (event.clickedInventory?.type != InventoryType.PLAYER) return
        if (event.slot == HEARTHSTONE_SLOT) event.isCancelled = true
    }
}
