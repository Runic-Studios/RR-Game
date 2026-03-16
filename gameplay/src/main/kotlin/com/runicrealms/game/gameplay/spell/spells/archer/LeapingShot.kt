package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.EntityTrail
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

class LeapingShot(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), DurationSpell, PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override var duration = DURATION
    var launchMultiplier = LAUNCH_MULTIPLIER
    var verticalPower = VERTICAL_POWER
    override var description =
        "Fire 4 arrows in quick succession while leaping backwards. " +
            "Each arrow deals (${physicalDamage} + ${physicalDamagePerLevel}x lvl) physical damage. " +
            "Gain fall damage immunity for ${duration}s."

    private val hasBeenHit: MutableMap<UUID, UUID> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        leap(player)
        fireArrow(player, false)
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { fireArrow(player, false) },
            5L,
        )
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { fireArrow(player, false) },
            10L,
        )
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { fireArrow(player, true) },
            15L,
        )
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        launchMultiplier = config.getDouble("launch-multiplier", launchMultiplier)
        verticalPower = config.getDouble("vertical-power", verticalPower)
    }

    private fun fireArrow(player: Player, last: Boolean) {
        val arrow = player.launchProjectile(Arrow::class.java)
        arrow.velocity = player.eyeLocation.direction.normalize().multiply(2.0)
        arrow.shooter = player
        arrow.isCustomNameVisible = false
        leapArrows[arrow.uniqueId] = last
        arrow.setBounce(false)
        player.world.playSound(
            player.location,
            Sound.ENTITY_ARROW_HIT,
            3.0f,
            if (last) 3.0f else 1.0f,
        )
        EntityTrail.entityTrail(arrow, Particle.CRIT, 5000L)
    }

    private fun leap(player: Player) {
        val look = player.location.direction
        val launchPath =
            Vector(-look.x, verticalPower, -look.z).normalize().multiply(launchMultiplier)
        launchPath.y = minOf(launchPath.y, 0.95)
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.2f)
        player.world.spawnParticle(
            Particle.DUST,
            player.location,
            25,
            0.5,
            0.5,
            0.5,
            0.0,
            Particle.DustOptions(Color.fromRGB(210, 180, 140), 1.0f),
        )
        player.velocity = launchPath
        val immunityTask =
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable { leapTasks.remove(player.uniqueId) },
                (duration * 20.0).toLong(),
            )
        leapTasks[player.uniqueId] = immunityTask
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onFallDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!leapTasks.containsKey(player.uniqueId)) return
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPhysicalDamage(event: EntityDamageByEntityEvent) {
        if (event.isCancelled) return
        val arrow = event.damager as? Arrow ?: return
        val player = arrow.shooter as? Player ?: return
        val last = leapArrows.remove(arrow.uniqueId) ?: return

        if (hasBeenHit.containsKey(player.uniqueId)) {
            event.isCancelled = true
            return
        }

        val victim = event.entity as? LivingEntity ?: return
        event.isCancelled = true
        Bukkit.getPluginManager().callEvent(ArrowHitEvent(player, victim, last))

        val dmgEvent =
            PhysicalDamageEvent(physicalDamage.toInt(), victim, player, false, true, this)
        Bukkit.getPluginManager().callEvent(dmgEvent)
        if (!dmgEvent.isCancelled) {
            victim.damage(dmgEvent.amount.toDouble(), player)
        }
        hasBeenHit[player.uniqueId] = victim.uniqueId
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { hasBeenHit.remove(player.uniqueId) },
            8L,
        )
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        leapTasks.remove(event.player.uniqueId)?.cancel()
        hasBeenHit.remove(event.player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Leaping Shot"
        const val COOLDOWN = 10.0
        const val MANA_COST = 25
        const val PHYSICAL_DAMAGE = 12.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val DURATION = 3.0
        const val LAUNCH_MULTIPLIER = 1.8
        const val VERTICAL_POWER = 0.45

        private val leapArrows: MutableMap<UUID, Boolean> = HashMap()
        private val leapTasks: MutableMap<UUID, BukkitTask> = HashMap()

        class ArrowHitEvent(val caster: Player, val victim: LivingEntity, val isLast: Boolean) :
            Event() {
            override fun getHandlers(): HandlerList = handlerList

            companion object {
                @JvmStatic val handlerList = HandlerList()
            }
        }
    }
}
