package com.runicrealms.game.gameplay.player.death

import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.character.util.SaveZoneRegistry
import com.runicrealms.game.gameplay.player.RegenManager
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import com.runicrealms.game.gameplay.spell.event.RunicDeathEvent
import com.runicrealms.game.items.config.item.GameItemTag
import com.runicrealms.game.items.generator.ItemStackConverter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Handles [RunicDeathEvent] at the highest priority to apply all death consequences: item drops,
 * gravestone creation, health/mana/food reset, hearthstone teleport, and leaving combat.
 */
class DeathListener
@Inject
constructor(
    plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val gravestoneManager: GravestoneManager,
    private val spellManager: SpellManager,
    private val regenManager: RegenManager,
    private val combatManager: CombatManager,
    private val saveZoneRegistry: SaveZoneRegistry,
    private val itemStackConverter: ItemStackConverter,
) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    // TODO: replace with DonorRank-based values when DonorRank system is migrated
    companion object {
        private const val PRIORITY_SECONDS = 300 // 5 minutes
        private const val DURATION_SECONDS = 1800 // 30 minutes

        private const val BLINDNESS_TICKS = 80
        private const val HOTBAR_HEARTHSTONE_SLOT = 8
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onRunicDeath(event: RunicDeathEvent) {
        if (event.isCancelled) return
        val victim = event.victim

        broadcastDeathMessage(event)
        playDeathEffects(victim, event)

        // TODO: skip item drops and use dungeon respawn point when dungeon system is migrated
        val dropped = buildDroppedItemsInventory(victim)
        if (dropped != null && !dropped.isEmpty) {
            // PvP kills grant no priority: anyone can loot immediately
            val victimHasPriority = event.killer == null || event.killer !is Player
            // TODO: use DonorRank priority/duration when DonorRank system is migrated
            gravestoneManager.createGravestone(
                victim,
                event.location,
                dropped,
                victimHasPriority,
                PRIORITY_SECONDS,
                DURATION_SECONDS,
            )
        }

        // Remaining steps require the player to be online (combat-log edge case)
        if (!victim.isOnline) return

        resetVitals(victim)
        teleportToHearthstone(victim)
        sendDeathMessage(victim, dropped)

        // Play sounds for the victim at their respawn point (they hear their own death)
        victim.playSound(victim.location, Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f)
        victim.playSound(victim.location, Sound.ENTITY_WITHER_DEATH, 0.25f, 1.0f)

        victim.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_TICKS, 0))
        combatManager.leaveCombat(victim.uniqueId)
    }

    private fun broadcastDeathMessage(event: RunicDeathEvent) {
        val message =
            if (event.killer is Player) {
                Component.text(
                    "${event.victim.name} was killed by ${(event.killer as Player).name}!",
                    NamedTextColor.RED,
                )
            } else {
                Component.text("${event.victim.name} died!", NamedTextColor.RED)
            }
        Bukkit.getOnlinePlayers().forEach { player -> player.sendMessage(message) }
    }

    private fun playDeathEffects(victim: Player, event: RunicDeathEvent) {
        val location = event.location
        val world = location.world ?: return
        world.playSound(location, Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f)
        world.playSound(location, Sound.ENTITY_WITHER_DEATH, 0.25f, 1.0f)
        world.spawnParticle(
            Particle.DUST,
            location,
            25,
            0.5,
            0.5,
            0.5,
            Particle.DustOptions(Color.RED, 3f),
        )
    }

    /**
     * Collects items from backpack slots 9-35, skipping protected tags. Items are removed from the
     * player's inventory and returned in a fresh 36-slot inventory. Returns null if no items were
     * collected (or if the player is in a dungeon — TODO).
     *
     * TODO: call weapon skin disable API on each item when that system is migrated
     */
    private fun buildDroppedItemsInventory(victim: Player): Inventory? {
        val toDrop = mutableListOf<ItemStack>()
        for (slot in 9..35) {
            val item = victim.inventory.getItem(slot) ?: continue
            val gameItem = itemStackConverter.convertToGameItem(item)
            if (gameItem != null) {
                val tags = gameItem.template.tags
                if (
                    tags.contains(GameItemTag.SOULBOUND) ||
                        tags.contains(GameItemTag.QUEST_ITEM) ||
                        tags.contains(GameItemTag.UNTRADEABLE)
                )
                    continue
            }
            toDrop.add(item.clone())
            victim.inventory.setItem(slot, null)
        }
        if (toDrop.isEmpty()) return null

        val inv = Bukkit.createInventory(null, 36)
        for (item in toDrop) {
            val remaining = inv.addItem(item)
            // If the 36-slot inventory was somehow full, drop the remainder at the death location
            remaining.values.forEach { overflow ->
                victim.location.world?.dropItem(victim.location, overflow)
            }
        }
        return inv
    }

    private fun resetVitals(victim: Player) {
        // Reset health to maximum
        val maxHealth = victim.getAttribute(Attribute.MAX_HEALTH)!!.value
        victim.health = maxHealth

        // Reset food
        victim.foodLevel = 20

        // Reset mana to maximum
        val character = userDataRegistry.getCharacter(victim.uniqueId)
        if (character != null) {
            val maxMana = regenManager.calculateMaxMana(character)
            spellManager.setMana(victim.uniqueId, maxMana)
        }
    }

    private fun teleportToHearthstone(victim: Player) {
        val hearthstone = victim.inventory.getItem(HOTBAR_HEARTHSTONE_SLOT)
        val respawnLocation =
            if (hearthstone != null) saveZoneRegistry.getLocationFromItemStack(hearthstone)
            else null
        victim.teleport(respawnLocation ?: saveZoneRegistry.TUTORIAL.location)
    }

    private fun sendDeathMessage(victim: Player, dropped: Inventory?) {
        val gravestone = gravestoneManager.gravestoneMap[victim.uniqueId]
        val gravestoneReminder =
            if (gravestone != null)
                " Your <dark_red><bold>GRAVESTONE</bold></dark_red><red> holds the remainder of" +
                    " your items and will be lootable by others in ${gravestone.priorityTime / 60}m."
            else ""
        victim.sendMessage(
            Component.text(
                "You have died! Your armor and hotbar have been returned. Any soulbound, quest," +
                    " and untradeable items have also been returned.$gravestoneReminder",
                NamedTextColor.RED,
            )
        )
    }
}
