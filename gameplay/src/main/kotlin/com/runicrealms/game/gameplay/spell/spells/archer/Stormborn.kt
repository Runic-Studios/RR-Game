package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.RunicBowEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spells.Combat
import com.runicrealms.game.gameplay.spell.spells.Potion
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import com.runicrealms.game.gameplay.spell.spellutil.particles.EntityTrail
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.metadata.FixedMetadataValue

class Stormborn(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), MagicDamageSpell, RadiusSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var magicDamage = MAGIC_DAMAGE
    override var magicDamagePerLevel = MAGIC_DAMAGE_PER_LEVEL
    override var radius = RADIUS
    var maxTargets = MAX_TARGETS
    override var description =
        "After casting an ability, your next 3 basic attacks are storm-infused, " +
            "ricocheting to up to ${maxTargets} additional targets in ${radius} blocks " +
            "for bonus magic damage."

    private val stormPlayers: MutableMap<UUID, Int> = HashMap()
    private val hasAlreadyHit: MutableMap<UUID, Long> = HashMap()

    init {
        isPassive = true
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell.
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        radius = config.getDouble("radius", radius)
        maxTargets = config.getDouble("max-targets", maxTargets)
        magicDamage = config.getDouble("magic-damage", magicDamage)
        magicDamagePerLevel = config.getDouble("magic-damage-per-level", magicDamagePerLevel)
    }

    private fun fireArrow(player: Player): Arrow {
        val arrow = player.launchProjectile(Arrow::class.java)
        arrow.velocity = player.eyeLocation.direction.normalize().multiply(2.0)
        arrow.shooter = player
        arrow.isCustomNameVisible = false
        arrow.customName(Component.text("autoAttack"))
        arrow.setMetadata(ARROW_META_KEY, FixedMetadataValue(deps.plugin, ARROW_META_VALUE))
        EntityTrail.entityTrail(arrow, Particle.CRIT, 5000L)
        return arrow
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onStormArrowDamage(event: EntityDamageByEntityEvent) {
        val arrow = event.damager as? Arrow ?: return
        val player = arrow.shooter as? Player ?: return
        if (!arrow.hasMetadata(ARROW_META_KEY)) return
        if (!arrow.getMetadata(ARROW_META_KEY)[0].asString().equals(ARROW_META_VALUE, true)) return
        val victim = event.entity as? LivingEntity ?: return
        if (!isValidEnemy(player, victim)) return

        val lastHit = hasAlreadyHit[player.uniqueId]
        if (lastHit != null && lastHit + 400 > System.currentTimeMillis()) return
        event.isCancelled = true

        val hitEvent = ArrowHitEvent(player, victim)
        Bukkit.getPluginManager().callEvent(hitEvent)
        if (hitEvent.isCancelled) return

        val dmgEvent = MagicDamageEvent(magicDamage.toInt(), victim, player, this)
        Bukkit.getPluginManager().callEvent(dmgEvent)
        if (!dmgEvent.isCancelled) {
            victim.damage(dmgEvent.amount.toDouble(), player)
        }
        hasAlreadyHit[player.uniqueId] = System.currentTimeMillis()
        ricochetEffect(player, victim)
    }

    private fun ricochetEffect(caster: Player, victim: LivingEntity) {
        var enemiesHit = 0
        for (entity in
            victim.world.getNearbyEntities(victim.location, radius, radius, radius) {
                isValidEnemy(caster, it)
            }) {
            if (enemiesHit >= maxTargets) break
            val target = entity as? LivingEntity ?: continue
            if (target == victim) continue
            target.world.playSound(target.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.25f, 1.0f)
            VectorUtil.drawLine(caster, Color.BLUE, victim.eyeLocation, target.eyeLocation, 0.5)
            val dmgEvent = MagicDamageEvent(magicDamage.toInt(), target, caster, this)
            Bukkit.getPluginManager().callEvent(dmgEvent)
            if (!dmgEvent.isCancelled) {
                target.damage(dmgEvent.amount.toDouble(), caster)
            }
            enemiesHit++
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onRunicBowEvent(event: RunicBowEvent) {
        val remaining = stormPlayers[event.player.uniqueId] ?: return
        event.isCancelled = true
        event.player.world.playSound(
            event.player.location,
            Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
            0.25f,
            0.75f,
        )
        fireArrow(event.player)
        event.player.world.playSound(
            event.player.location,
            Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
            0.25f,
            0.75f,
        )
        val newValue = remaining - 1
        if (newValue <= 0) {
            stormPlayers.remove(event.player.uniqueId)
        } else {
            stormPlayers[event.player.uniqueId] = newValue
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (event.spell is Potion) return
        if (event.spell is Combat) return
        stormPlayers[event.caster.uniqueId] = 3
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        stormPlayers.remove(event.player.uniqueId)
        hasAlreadyHit.remove(event.player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Stormborn"
        const val MAGIC_DAMAGE = 12.0
        const val MAGIC_DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 5.0
        const val MAX_TARGETS = 3.0
        const val ARROW_META_KEY = "data"
        const val ARROW_META_VALUE = "storm shot"

        class ArrowHitEvent(val caster: Player, val victim: LivingEntity) : Event(), Cancellable {
            private var cancelled = false

            override fun isCancelled(): Boolean = cancelled

            override fun setCancelled(cancel: Boolean) {
                cancelled = cancel
            }

            override fun getHandlers(): HandlerList = handlerList

            companion object {
                @JvmStatic val handlerList = HandlerList()
            }
        }
    }
}
