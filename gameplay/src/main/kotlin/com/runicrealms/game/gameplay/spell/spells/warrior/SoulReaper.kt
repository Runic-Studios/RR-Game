package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.StackTask
import com.runicrealms.game.gameplay.spell.spellutil.particles.RotatingParticleEffect
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Passive soul system:
 * - Devour / Umbral Grasp grant soul stacks
 * - Souls reduce incoming damage
 * - Stacks expire after [duration] seconds and refresh on new gains
 */
class SoulReaper(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.WARRIOR, deps) {
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Landing Devour or Umbral Grasp grants souls that reduce incoming damage."

    var duration = BASE_DURATION
    var maxStacks = BASE_MAX_STACKS
    var percent = BASE_PERCENT

    init {
        isPassive = true
        displayCastMessage = false
        startParticleTask()
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell.
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        val victim = event.victim as? Player ?: return
        if (!hasPassive(victim.uniqueId, name)) return
        event.amount = reducedDamage(victim, event.amount)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!hasPassive(event.victim.uniqueId, name)) return
        val victim = event.victim as? Player ?: return
        event.amount = reducedDamage(victim, event.amount)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        val victim = event.victim as? Player
        if (victim != null && hasPassive(victim.uniqueId, name)) {
            event.amount = reducedDamage(victim, event.amount)
        }

        val spell = event.spell ?: return
        if (spell !is Devour && spell !is UmbralGrasp) return

        val caster = event.caster
        if (!hasPassive(caster.uniqueId, name)) return
        gainSoul(caster)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clearPlayerData(event.player.uniqueId)
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        duration = config.getDouble("duration", duration)
        maxStacks = config.getInt("max-stacks", maxStacks).coerceAtLeast(1)
        percent = config.getDouble("percent", percent).coerceAtLeast(0.0)
    }

    fun getSoulCount(uuid: UUID): Int = reaperTaskMap[uuid]?.stacks?.get() ?: 0

    private fun reducedDamage(victim: Player, amount: Int): Int {
        val stacks = getSoulCount(victim.uniqueId)
        if (stacks <= 0) return amount
        val reducedAmount = (amount * (stacks * percent)).toInt()
        return (amount - reducedAmount).coerceAtLeast(0)
    }

    private fun gainSoul(player: Player) {
        val uuid = player.uniqueId
        val existing = reaperTaskMap[uuid]
        if (existing == null) {
            val task =
                deps.plugin.server.scheduler.runTaskLater(
                    deps.plugin,
                    Runnable { cleanupTask(uuid, "Soul Reaper has expired.") },
                    (duration * 20.0).toLong(),
                )
            val stackTask =
                StackTask(
                    caster = player,
                    spell = this,
                    stacks = AtomicInteger(1),
                    bukkitTask = task,
                    registry = deps.stackTaskRegistry,
                )
            reaperTaskMap[uuid] = stackTask
        } else {
            if (existing.stacks.get() < maxStacks) {
                existing.stacks.incrementAndGet()
            }
            existing.reset(
                (duration * 20.0).toLong(),
                Runnable { cleanupTask(uuid, "Soul Reaper has expired.") },
            )
        }
        player.sendMessage("Souls: ${getSoulCount(uuid)}")
    }

    private fun startParticleTask() {
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            Runnable {
                val iterator = reaperTaskMap.entries.iterator()
                while (iterator.hasNext()) {
                    val (uuid, stackTask) = iterator.next()
                    val player = stackTask.caster
                    if (!player.isOnline) {
                        stackTask.cancelTask()
                        iterator.remove()
                        particleEffects.remove(uuid)
                        continue
                    }

                    val stacks = stackTask.stacks.get().coerceAtLeast(1)
                    val particleEffect =
                        particleEffects.computeIfAbsent(uuid) {
                            RotatingParticleEffect(1.0, stacks.toDouble())
                        }
                    particleEffect.show(
                        player,
                        Particle.DUST,
                        player.location.clone().add(0.0, 1.0, 0.0),
                        Color.fromRGB(185, 251, 185),
                    )
                }
            },
            0L,
            1L,
        )
    }

    private fun clearPlayerData(uuid: UUID) {
        reaperTaskMap.remove(uuid)?.cancelTask()
        particleEffects.remove(uuid)
    }

    private fun cleanupTask(uuid: UUID, text: String) {
        clearPlayerData(uuid)
        deps.plugin.server.getPlayer(uuid)?.sendMessage(text)
    }

    companion object {
        const val SPELL_NAME = "Soul Reaper"
        private const val BASE_DURATION = 8.0
        private const val BASE_MAX_STACKS = 5
        private const val BASE_PERCENT = 0.03

        val reaperTaskMap: ConcurrentHashMap<UUID, StackTask> = ConcurrentHashMap()
        private val particleEffects: ConcurrentHashMap<UUID, RotatingParticleEffect> =
            ConcurrentHashMap()
    }
}
