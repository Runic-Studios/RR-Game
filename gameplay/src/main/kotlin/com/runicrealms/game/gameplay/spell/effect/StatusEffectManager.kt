package com.runicrealms.game.gameplay.spell.effect

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.api.StatusEffectAPI
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.event.StatusEffectEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * Manages hard crowd-control status effects (Silence, Stun, Root, Disarm, Slow, Speed,
 * Invulnerability). Effects are stored as (startTimeMillis, durationSeconds) pairs.
 *
 * Each event that can be blocked by a status effect is intercepted here and cancelled if the
 * affected entity has the relevant status applied.
 */
@Singleton
class StatusEffectManager @Inject constructor(private val plugin: Plugin) :
    StatusEffectAPI, Listener {

    /** Maps UUID -> (RunicStatusEffect -> Pair(startTimeMs, durationSeconds)) */
    private val statusEffectMap =
        ConcurrentHashMap<UUID, ConcurrentHashMap<RunicStatusEffect, Pair<Long, Double>>>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        startRemovalTask()
    }

    // --- StatusEffectAPI ---

    override fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
        applier: LivingEntity?,
    ) {
        val event = StatusEffectEvent(entity, effect, durationInSeconds, displayMessage, applier)
        Bukkit.getPluginManager().callEvent(event)
    }

    override fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
    ) = addStatusEffect(entity, effect, durationInSeconds, displayMessage, null)

    override fun cleanse(uuid: UUID) {
        statusEffectMap[uuid]
            ?.keys
            ?.filter { !it.isBuff }
            ?.forEach { effect -> removeStatusEffect(uuid, effect) }
    }

    override fun purge(uuid: UUID) {
        statusEffectMap[uuid]
            ?.keys
            ?.filter { it.isBuff }
            ?.forEach { effect -> removeStatusEffect(uuid, effect) }
    }

    override fun hasStatusEffect(uuid: UUID, effect: RunicStatusEffect): Boolean =
        statusEffectMap[uuid]?.containsKey(effect) == true

    override fun removeStatusEffect(uuid: UUID, statusEffect: RunicStatusEffect): Boolean {
        val removed = statusEffectMap[uuid]?.remove(statusEffect) != null
        if (removed) {
            val player = Bukkit.getPlayer(uuid) ?: return true
            when (statusEffect) {
                RunicStatusEffect.SLOW_I,
                RunicStatusEffect.SLOW_II,
                RunicStatusEffect.SLOW_III -> player.removePotionEffect(PotionEffectType.SLOWNESS)
                RunicStatusEffect.SPEED_I,
                RunicStatusEffect.SPEED_II,
                RunicStatusEffect.SPEED_III -> player.removePotionEffect(PotionEffectType.SPEED)
                else -> {}
            }
        }
        return removed
    }

    override fun getStatusEffectDuration(uuid: UUID, effect: RunicStatusEffect): Double {
        val entry = statusEffectMap[uuid]?.get(effect) ?: return 0.0
        val (startTime, duration) = entry
        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        return maxOf(0.0, duration - elapsed)
    }

    // --- Event handlers ---

    @EventHandler(priority = EventPriority.HIGH)
    fun onStatusEffect(event: StatusEffectEvent) {
        if (event.isCancelled) return
        val entity = event.entity
        val uuid = entity.uniqueId
        val effect = event.statusEffect
        val duration = event.durationInSeconds

        // Store the effect
        statusEffectMap.getOrPut(uuid) { ConcurrentHashMap() }[effect] =
            Pair(System.currentTimeMillis(), duration)

        if (event.displayMessage && entity is Player) {
            entity.sendMessage(effect.getMessage())
        }
        if (event.playSound) {
            entity.world.playSound(entity.location, effect.sound, 1.0f, 1.0f)
        }

        // Apply vanilla potion effects for SLOW/SPEED
        val durationTicks = (duration * 20).toInt()
        when (effect) {
            RunicStatusEffect.SLOW_I ->
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 0))
            RunicStatusEffect.SLOW_II ->
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1))
            RunicStatusEffect.SLOW_III ->
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 2))
            RunicStatusEffect.SPEED_I ->
                entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, durationTicks, 0))
            RunicStatusEffect.SPEED_II ->
                entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, durationTicks, 1))
            RunicStatusEffect.SPEED_III ->
                entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, durationTicks, 2))
            RunicStatusEffect.ROOT,
            RunicStatusEffect.STUN -> {
                // Freeze movement by setting speed to 0; restore after duration
                val movementAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED) ?: return
                val originalSpeed = movementAttr.value
                movementAttr.baseValue = 0.0
                Bukkit.getScheduler()
                    .runTaskLater(
                        plugin,
                        Runnable {
                            if (entity.isValid) movementAttr.baseValue = originalSpeed
                            removeStatusEffect(uuid, effect)
                        },
                        durationTicks.toLong(),
                    )
            }
            else -> {}
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSpellCast(event: SpellCastEvent) {
        val uuid = event.caster.uniqueId
        if (
            hasStatusEffect(uuid, RunicStatusEffect.SILENCE) ||
                hasStatusEffect(uuid, RunicStatusEffect.STUN)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        val uuid = event.caster.uniqueId
        if (event.isBasicAttack && hasStatusEffect(uuid, RunicStatusEffect.DISARM)) {
            event.isCancelled = true
            return
        }
        if (
            hasStatusEffect(uuid, RunicStatusEffect.STUN) ||
                hasStatusEffect(uuid, RunicStatusEffect.INVULNERABILITY)
        ) {
            event.isCancelled = true
        }
        // ROOT breaks on physical damage
        val victimUuid = event.victim.uniqueId
        if (hasStatusEffect(victimUuid, RunicStatusEffect.ROOT)) {
            removeStatusEffect(victimUuid, RunicStatusEffect.ROOT)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onMagicDamage(event: MagicDamageEvent) {
        val uuid = event.caster.uniqueId
        if (
            hasStatusEffect(uuid, RunicStatusEffect.SILENCE) ||
                hasStatusEffect(uuid, RunicStatusEffect.STUN)
        ) {
            event.isCancelled = true
            return
        }
        val victimUuid = event.victim.uniqueId
        if (hasStatusEffect(victimUuid, RunicStatusEffect.INVULNERABILITY)) {
            event.isCancelled = true
        }
        if (hasStatusEffect(victimUuid, RunicStatusEffect.ROOT)) {
            removeStatusEffect(victimUuid, RunicStatusEffect.ROOT)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onMobDamage(event: MobDamageEvent) {
        val victimUuid = event.victim.uniqueId
        if (hasStatusEffect(victimUuid, RunicStatusEffect.INVULNERABILITY)) {
            event.isCancelled = true
        }
        if (hasStatusEffect(victimUuid, RunicStatusEffect.ROOT)) {
            removeStatusEffect(victimUuid, RunicStatusEffect.ROOT)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSpellHeal(event: SpellHealEvent) {
        val uuid = event.caster.uniqueId
        if (
            hasStatusEffect(uuid, RunicStatusEffect.SILENCE) ||
                hasStatusEffect(uuid, RunicStatusEffect.STUN)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val uuid = event.player.uniqueId
        if (
            hasStatusEffect(uuid, RunicStatusEffect.ROOT) ||
                hasStatusEffect(uuid, RunicStatusEffect.STUN)
        ) {
            // Cancel movement (position change) but allow head rotation
            val from = event.from
            val to = event.to
            if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ) {
                event.isCancelled = true
            }
        }
    }

    // --- Internal ---

    private fun startRemovalTask() {
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                plugin,
                Runnable {
                    val now = System.currentTimeMillis()
                    for ((uuid, effects) in statusEffectMap) {
                        val expired =
                            effects.entries
                                .filter { (_, pair) ->
                                    val (startTime, duration) = pair
                                    (now - startTime) >= (duration * 1000L)
                                }
                                .map { it.key }
                        for (effect in expired) {
                            effects.remove(effect)
                        }
                        if (effects.isEmpty()) statusEffectMap.remove(uuid)
                    }
                },
                0L,
                5L,
            )
    }
}
