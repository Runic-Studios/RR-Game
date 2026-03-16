package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import java.util.UUID
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask

class AstralBlessing(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, ShieldingSpell {
    override var duration = BASE_DURATION
    override var shieldAmount = BASE_SHIELD
    override var shieldPerLevel = SHIELD_PER_LEVEL
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Enemies hit by Starlight are marked for $duration seconds. Allied basic attacks consume the mark and grant a shield."

    private val markMap: MutableMap<UUID, AstralMark> = mutableMapOf()

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive - handled through event listeners.
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (event.spell !is Starlight) return
        applyMark(event.caster, event.victim)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        val mark = markMap[event.victim.uniqueId] ?: return
        val markCaster = deps.plugin.server.getPlayer(mark.casterUuid) ?: return
        if (!isValidAlly(markCaster, event.caster)) return
        mark.task.cancel()
        markMap.remove(event.victim.uniqueId)
        shieldPlayer(event.caster, event.caster, shieldAmount, this)
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        clearMark(event.entity.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clearMark(event.player.uniqueId)
        val iterator = markMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.casterUuid == event.player.uniqueId) {
                entry.value.task.cancel()
                iterator.remove()
            }
        }
    }

    private fun applyMark(caster: Player, victim: LivingEntity) {
        clearMark(victim.uniqueId)
        var elapsed = 0
        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    if (elapsed >= duration.toInt()) {
                        task.cancel()
                        markMap.remove(victim.uniqueId)
                        return@Runnable
                    }
                    elapsed += 1
                    HorizontalCircleFrame(0.5, false)
                        .playParticle(caster, Particle.DUST, victim.eyeLocation, 0.3, Color.YELLOW)
                },
                0L,
                20L,
            )
        markMap[victim.uniqueId] = AstralMark(caster.uniqueId, task)
    }

    private fun clearMark(victimId: UUID) {
        val mark = markMap.remove(victimId) ?: return
        mark.task.cancel()
    }

    companion object {
        const val SPELL_NAME = "Astral Blessing"
        private const val BASE_DURATION = 3.0
        private const val BASE_SHIELD = 20.0
        private const val SHIELD_PER_LEVEL = 0.5
    }

    private data class AstralMark(val casterUuid: UUID, val task: BukkitTask)
}
