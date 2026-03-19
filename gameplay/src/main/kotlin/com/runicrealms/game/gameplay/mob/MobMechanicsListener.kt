package com.runicrealms.game.gameplay.mob

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import io.lumine.mythic.bukkit.MythicBukkit
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.ChatColor
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.plugin.Plugin

/**
 * Handles custom mob mechanics:
 * - Prevents all entities from catching fire (vanilla fire damage is replaced by the custom system)
 * - Displays a colour-coded health bar as the entity's name tag when they take damage
 * - Updates the health bar on health regen (for MythicMobs with disguises)
 *
 * For MythicMobs entities, the health bar is updated via the MythicMobs skill system
 * ("UpdateHealthBar_N") which handles the disguise name tag. For non-MythicMob entities,
 * the custom name is set directly.
 *
 * TODO: Verify that the MythicMobs skill "UpdateHealthBar_N" exists in the MythicMobs
 *   skill configuration (N = 0-10 representing health percentage deciles).
 * TODO: Verify the MythicMobs APIHelper.castSkill API signature for the current MythicMobs version.
 */
@Singleton
class MobMechanicsListener
@Inject
constructor(private val plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /** Prevents all entities from catching fire. The custom damage system handles fire damage. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityCombust(event: EntityCombustEvent) {
        event.isCancelled = true
    }

    /**
     * Updates mob health bar on regen. Only applies to entities that have passengers (disguise
     * indicator for LibsDisguises-style MythicMobs), skipping armor stands and horses.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onMobRegainHealth(event: EntityRegainHealthEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (entity is ArmorStand) return
        if (entity is Horse) return
        // Skip players - they have their own health display
        if (entity is Player) return
        // Only update display for entities that have passengers (disguise indicator)
        if (entity.passengers.isEmpty()) return
        updateDisplayName(entity, 0)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (event.isCancelled) return
        val entity = event.victim as? LivingEntity ?: return
        if (entity is Player) return
        updateDisplayName(entity, event.amount)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onMagicDamage(event: MagicDamageEvent) {
        if (event.isCancelled) return
        val entity = event.victim as? LivingEntity ?: return
        if (entity is Player) return
        updateDisplayName(entity, event.amount)
    }

    private fun updateDisplayName(entity: LivingEntity, damage: Int) {
        val optional = MythicBukkit.inst().mobManager.getActiveMob(entity.uniqueId)
        if (optional.isPresent) {
            // Delay by 1 tick so health value reflects the post-damage state
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val numBars = calculateNumColours(entity)
                // TODO: Verify castSkill API for the current MythicMobs version.
                //   Older versions: MythicBukkit.inst().apiHelper.castSkill(entity, "UpdateHealthBar_$numBars")
                MythicBukkit.inst().apiHelper.castSkill(entity, "UpdateHealthBar_$numBars")
            }, 1L)
        } else {
            val healthBar = buildHealthBar(entity, damage)
            entity.customName(LegacyComponentSerializer.legacySection().deserialize(healthBar))
            entity.isCustomNameVisible = true
        }
    }

    private fun buildHealthBar(entity: LivingEntity, damage: Int): String {
        val numBars = calculateNumColours(entity)
        val (firstHalf, secondHalf) = colourBars(numBars)
        val healthStr = "${ChatColor.WHITE}${(entity.health - damage).toInt().coerceAtLeast(0)}"
        return "${ChatColor.YELLOW}[$firstHalf$healthStr$secondHalf${ChatColor.YELLOW}]"
    }

    private fun calculateNumColours(entity: LivingEntity): Int {
        val maxHealth = entity.maxHealth
        val current = entity.health.coerceAtLeast(0.0)
        val percentage = ((current / maxHealth) * 100.0).toInt()
        return percentage / 10
    }

    private fun colourBars(numBars: Int): Pair<String, String> {
        val green = ChatColor.GREEN.toString()
        val yellow = ChatColor.YELLOW.toString()
        val red = ChatColor.RED.toString()
        val gray = ChatColor.DARK_GRAY.toString()
        val pipe = "|||||"
        return when (numBars) {
            10 -> Pair("$green$pipe", "$green$pipe")
            9  -> Pair("$green$pipe", "${green}||||${gray}|")
            8  -> Pair("$green$pipe", "${green}|||${gray}||")
            7  -> Pair("$yellow$pipe", "${yellow}||${gray}|||")
            6  -> Pair("$yellow$pipe", "${yellow}|${gray}||||")
            5  -> Pair("$yellow$pipe", "$gray$pipe")
            4  -> Pair("${yellow}||||${gray}|", "$gray$pipe")
            3  -> Pair("${red}|||${gray}||", "$gray$pipe")
            2  -> Pair("${red}||${gray}|||", "$gray$pipe")
            1  -> Pair("${red}|${gray}||||", "$gray$pipe")
            else -> Pair("$gray$pipe", "$gray$pipe")
        }
    }
}
