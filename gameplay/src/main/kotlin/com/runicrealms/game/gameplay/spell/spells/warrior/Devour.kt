package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.ThreatUtil
import com.runicrealms.game.gameplay.spell.spellutil.particles.SlashEffect
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Cleaves in front of the caster, damaging enemies in a small area and reducing their outgoing
 * damage for a short duration.
 */
class Devour(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), DurationSpell, MagicDamageSpell, RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    private var percent = REDUCTION_PERCENT
    override var description =
        "Cleave in front of you, dealing ($magicDamage + &f${magicDamagePerLevel}x&7 lvl) magicʔ damage " +
            "and reducing affected enemies' outgoing damage by ${(REDUCTION_PERCENT * 100).toInt()}% for ${duration}s."

    private val debuffedEntities: MutableSet<UUID> = HashSet()

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.25f)
        player.world.playSound(player.location, Sound.ENTITY_WITHER_HURT, 0.5f, 1.0f)

        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                radius,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        SlashEffect.slashHorizontal(
            player,
            Particle.DUST,
            player.location,
            Color.fromRGB(185, 251, 185),
        )
        SlashEffect.slashHorizontal(
            player,
            Particle.DUST,
            player.location.clone().add(0.0, 0.5, 0.0),
            Color.fromRGB(185, 251, 185),
        )

        val hit = rayTrace?.hitEntity as? LivingEntity ?: return
        hit.world.playSound(hit.location, Sound.ENTITY_PLAYER_HURT, 0.5f, 2.0f)
        for (entity in
            player.world.getNearbyEntities(hit.location, BEAM_WIDTH, BEAM_WIDTH, BEAM_WIDTH) {
                target ->
                isValidEnemy(player, target)
            }) {
            val victim = entity as? LivingEntity ?: continue
            debuffedEntities.add(victim.uniqueId)
            deps.plugin.server.scheduler.runTaskLater(
                deps.plugin,
                Runnable { debuffedEntities.remove(victim.uniqueId) },
                (duration * 20.0).toLong(),
            )

            val dmgEvent = MagicDamageEvent(magicDamage.toInt(), victim, player, this)
            Bukkit.getPluginManager().callEvent(dmgEvent)
            if (!dmgEvent.isCancelled) {
                victim.damage(dmgEvent.amount.toDouble(), player)
            }
            ThreatUtil.generateThreat(player, victim)
        }
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        percent = config.getDouble("percent", percent)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        if (!debuffedEntities.contains(event.mob.uniqueId)) return
        val reducedAmount = event.amount * percent
        event.amount = (event.amount - reducedAmount).toInt()
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        if (!debuffedEntities.contains(event.victim.uniqueId)) return
        val reducedAmount = event.amount * percent
        event.amount = (event.amount - reducedAmount).toInt()
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!debuffedEntities.contains(event.victim.uniqueId)) return
        val reducedAmount = event.amount * percent
        event.amount = (event.amount - reducedAmount).toInt()
    }

    companion object {
        const val SPELL_NAME = "Devour"
        const val COOLDOWN = 10.0
        const val MANA_COST = 20
        const val DURATION = 6.0
        const val BASE_DAMAGE = 16.0
        const val DAMAGE_PER_LEVEL = 0.8
        const val RADIUS = 8.0
        const val REDUCTION_PERCENT = 0.2
        const val BEAM_WIDTH = 2.0
    }
}
