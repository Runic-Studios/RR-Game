package com.runicrealms.game.gameplay.mob

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import io.lumine.mythic.bukkit.MythicBukkit
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
 * ("UpdateHealthBar_N") which handles the disguise name tag. For non-MythicMob entities, the custom
 * name is set directly.
 *
 * TODO: Verify that the MythicMobs skill "UpdateHealthBar_N" exists in the MythicMobs skill
 *   configuration (N = 0-10 representing health percentage deciles).
 * TODO: Verify the MythicMobs APIHelper.castSkill API signature for the current MythicMobs version.
 */
@Singleton
class MobMechanicsListener @Inject constructor(private val plugin: Plugin) : Listener {

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
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable {
                    val numBars = calculateNumColours(entity)
                    // TODO: Verify castSkill API for the current MythicMobs version.
                    //   Older versions: MythicBukkit.inst().apiHelper.castSkill(entity,
                    // "UpdateHealthBar_$numBars")
                    MythicBukkit.inst().apiHelper.castSkill(entity, "UpdateHealthBar_$numBars")
                },
                1L,
            )
        } else {
            val healthBar = buildHealthBar(entity, damage)
            entity.customName(healthBar)
            entity.isCustomNameVisible = true
        }
    }

    private fun buildHealthBar(entity: LivingEntity, damage: Int): Component {
        val numBars = calculateNumColours(entity)
        val (firstHalf, secondHalf) = colourBars(numBars)
        val healthStr = (entity.health - damage).toInt().coerceAtLeast(0).toString()
        return Component.text()
            .append(Component.text("[", NamedTextColor.YELLOW))
            .append(firstHalf)
            .append(Component.text(healthStr, NamedTextColor.WHITE))
            .append(secondHalf)
            .append(Component.text("]", NamedTextColor.YELLOW))
            .build()
    }

    private fun calculateNumColours(entity: LivingEntity): Int {
        val maxHealth = entity.maxHealth
        val current = entity.health.coerceAtLeast(0.0)
        val percentage = ((current / maxHealth) * 100.0).toInt()
        return percentage / 10
    }

    private fun colourBars(numBars: Int): Pair<Component, Component> {
        val pipe = "|||||"
        return when (numBars) {
            10 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.GREEN, 0),
                    pipeComponent(pipe, NamedTextColor.GREEN, 0),
                )
            9 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.GREEN, 0),
                    pipeComponent(pipe, NamedTextColor.GREEN, 1),
                )
            8 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.GREEN, 0),
                    pipeComponent(pipe, NamedTextColor.GREEN, 2),
                )
            7 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.YELLOW, 0),
                    pipeComponent(pipe, NamedTextColor.YELLOW, 3),
                )
            6 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.YELLOW, 0),
                    pipeComponent(pipe, NamedTextColor.YELLOW, 4),
                )
            5 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.YELLOW, 0),
                    pipeComponent(pipe, NamedTextColor.DARK_GRAY, 0),
                )
            4 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.YELLOW, 1),
                    pipeComponent(pipe, NamedTextColor.DARK_GRAY, 0),
                )
            3 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.RED, 2),
                    pipeComponent(pipe, NamedTextColor.DARK_GRAY, 0),
                )
            2 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.RED, 3),
                    pipeComponent(pipe, NamedTextColor.DARK_GRAY, 0),
                )
            1 ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.RED, 4),
                    pipeComponent(pipe, NamedTextColor.DARK_GRAY, 0),
                )
            else ->
                Pair(
                    pipeComponent(pipe, NamedTextColor.DARK_GRAY, 0),
                    pipeComponent(pipe, NamedTextColor.DARK_GRAY, 0),
                )
        }
    }

    /** Builds a pipe segment: first N chars in [color], rest in DARK_GRAY. */
    private fun pipeComponent(pipe: String, color: NamedTextColor, grayCount: Int): Component {
        val coloredCount = pipe.length - grayCount
        return if (grayCount == 0) {
            Component.text(pipe, color)
        } else {
            Component.text()
                .append(Component.text(pipe.take(coloredCount), color))
                .append(Component.text(pipe.takeLast(grayCount), NamedTextColor.DARK_GRAY))
                .build()
        }
    }
}
