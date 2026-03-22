package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask

class Sear(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, MagicDamageSpell, RadiusSpell {
    override var duration = BASE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() =
            "Fire a beam of light. The first enemy hit is atoned and detonates when damaged by another source."

    private val atoningEntitiesMap: MutableMap<UUID, BukkitTask> = ConcurrentHashMap()
    private val atonementCasters: MutableMap<UUID, UUID> = ConcurrentHashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f)
        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                MAX_DIST.toDouble(),
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        if (rayTrace == null) {
            val location = player.getTargetBlockExact(MAX_DIST)?.location ?: player.location
            VectorUtil.drawLine(player, Particle.CLOUD, player.eyeLocation, location, 0.5)
            player.world.spawnParticle(Particle.ANGRY_VILLAGER, location, 8, 0.5, 0.5, 0.5, 0.0)
            return
        }
        val hit = rayTrace.hitEntity as? LivingEntity ?: return
        VectorUtil.drawLine(player, Particle.CLOUD, player.eyeLocation, hit.eyeLocation, 0.75)
        hit.world.playSound(hit.location, Sound.ENTITY_GENERIC_EXPLODE, 0.25f, 2.0f)
        hit.world.spawnParticle(Particle.ANGRY_VILLAGER, hit.eyeLocation, 8, 0.8, 0.5, 0.8, 0.0)
        applyAtonement(player, hit)
        val event = MagicDamageEvent(magicDamage.toInt(), hit, player, this)
        Bukkit.getPluginManager().callEvent(event)
        if (!event.isCancelled) {
            hit.damage(event.amount.toDouble(), player)
        }
    }

    @EventHandler
    fun onMagicDamage(event: MagicDamageEvent) {
        if (event.spell is Sear) return
        detonateIfAtoned(event.victim, event.caster)
    }

    @EventHandler
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        detonateIfAtoned(event.victim, event.caster)
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        clearAtonement(event.entity.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clearAtonement(event.player.uniqueId)
    }

    private fun applyAtonement(caster: Player, victim: LivingEntity) {
        clearAtonement(victim.uniqueId)
        atonementCasters[victim.uniqueId] = caster.uniqueId
        var count = 0
        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    if (count >= duration.toInt()) {
                        task.cancel()
                        clearAtonement(victim.uniqueId)
                        return@Runnable
                    }
                    count += 1
                    HorizontalCircleFrame(0.5, false)
                        .playParticle(caster, Particle.FLAME, victim.eyeLocation, 0.3, Color.YELLOW)
                },
                0L,
                20L,
            )
        atoningEntitiesMap[victim.uniqueId] = task
    }

    private fun detonateIfAtoned(victim: LivingEntity, fallbackCaster: Player) {
        if (!atoningEntitiesMap.containsKey(victim.uniqueId)) return
        clearAtonement(victim.uniqueId)
        val caster =
            deps.plugin.server.getPlayer(
                atonementCasters.remove(victim.uniqueId) ?: fallbackCaster.uniqueId
            ) ?: fallbackCaster
        for (entity in victim.world.getNearbyEntities(victim.location, radius, radius, radius)) {
            val target = entity as? LivingEntity ?: continue
            if (!isValidEnemy(caster, target)) continue
            val event = MagicDamageEvent(magicDamage.toInt(), target, caster, this)
            Bukkit.getPluginManager().callEvent(event)
            if (!event.isCancelled) {
                target.damage(event.amount.toDouble(), caster)
            }
        }
    }

    private fun clearAtonement(victimId: UUID) {
        atoningEntitiesMap.remove(victimId)?.cancel()
        atonementCasters.remove(victimId)
    }

    companion object {
        const val SPELL_NAME = "Sear"
        private const val COOLDOWN = 6.0
        private const val MANA_COST = 14
        private const val BASE_DURATION = 4.0
        private const val BASE_DAMAGE = 14.0
        private const val DAMAGE_PER_LEVEL = 0.35
        private const val BASE_RADIUS = 4.0
        private const val MAX_DIST = 10
        private const val BEAM_WIDTH = 1.5
    }
}
