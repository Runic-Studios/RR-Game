package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.warrior.HolyFervorEffect
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import com.runicrealms.game.gameplay.spell.spellutil.KnockbackUtil
import com.runicrealms.game.gameplay.spell.spellutil.particles.SlashEffect
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.roundToInt
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Activates Holy Fervor: grants speed/shield buffs and transforms basic attacks into light sweeps
 * with knockback and ally shielding.
 */
class SacredWings(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), DurationSpell, RadiusSpell, ShieldingSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var radius = RADIUS
    override var shieldAmount = SHIELD
    override var shieldPerLevel = SHIELD_PER_LEVEL
    override val description: String
        get() =
            "Conjure wings of light for ${duration}s, granting speed to nearby allies and shielding yourself. " +
                "During holy fervor, your basic attacks become radiant sweeps."

    private var allyShield = ALLY_SHIELD
    private var allyShieldPerLevel = ALLY_SHIELD_PER_LEVEL
    private var knockback = KNOCKBACK
    private var sweepCooldown = SWEEP_COOLDOWN
    private val sweepCooldownMap: MutableMap<UUID, MutableMap<UUID, Long>> = HashMap()

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        allyShield = config.getDouble("ally-shield", allyShield)
        allyShieldPerLevel = config.getDouble("ally-shield-per-level", allyShieldPerLevel)
        knockback = config.getDouble("knockback", knockback)
        sweepCooldown = config.getDouble("sweep-cooldown", sweepCooldown)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f)
        player.world.playSound(player.location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.0f)
        player.world.strikeLightningEffect(player.location)

        addStatusEffect(player, RunicStatusEffect.SPEED_I, duration, false)
        for (entity in
            player.world.getNearbyEntities(player.location, radius, radius, radius) { target ->
                isValidAlly(player, target)
            }) {
            val ally = entity as? LivingEntity ?: continue
            addStatusEffect(ally, RunicStatusEffect.SPEED_I, duration, false)
        }

        shieldPlayer(player, player, shieldAmount, this)
        HolyFervorEffect(caster = player, duration = duration, spellEffectAPI = deps.spellEffectAPI)
            .initialize()
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (event.isRanged) return
        if (event.spell !is SacredWings) return
        if (!hasSpellEffect(event.caster.uniqueId, SpellEffectType.HOLY_FERVOR)) return

        val casterId = event.caster.uniqueId
        val victimId = event.victim.uniqueId
        val victimCooldowns = sweepCooldownMap.getOrPut(casterId) { HashMap() }
        val lastHitTime = victimCooldowns[victimId]
        if (
            lastHitTime != null &&
                (System.currentTimeMillis() - lastHitTime) < (sweepCooldown * 1000.0)
        )
            return

        victimCooldowns[victimId] = System.currentTimeMillis()
        sweepTarget(event.caster, event.victim)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBasicAttack(event: BasicAttackEvent) {
        if (!hasSpellEffect(event.player.uniqueId, SpellEffectType.HOLY_FERVOR)) return
        event.isCancelled = true
        sweepEffect(
            player = event.player,
            material = event.material,
            minDamage = event.damage,
            maxDamage = event.maxDamage,
            cooldownTicks = event.cooldownTicks.roundToInt(),
        )
    }

    private fun sweepEffect(
        player: Player,
        material: Material,
        minDamage: Int,
        maxDamage: Int,
        cooldownTicks: Int,
    ) {
        val randomDamage = ThreadLocalRandom.current().nextInt(minDamage, maxDamage + 1)
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2.0f)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f)

        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                SWEEP_DISTANCE,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        SlashEffect.slashHorizontal(player, Particle.ANGRY_VILLAGER, player.location)

        val primary = rayTrace?.hitEntity as? LivingEntity
        if (primary != null) {
            primary.world.playSound(primary.location, Sound.ENTITY_PLAYER_HURT, 0.5f, 2.0f)
            val targets =
                player.world.getNearbyEntities(
                    primary.location,
                    BEAM_WIDTH,
                    BEAM_WIDTH,
                    BEAM_WIDTH,
                ) { target ->
                    isValidEnemy(player, target)
                }
            for (target in targets) {
                val victim = target as? LivingEntity ?: continue
                val damageEvent =
                    PhysicalDamageEvent(randomDamage, victim, player, true, false, this)
                Bukkit.getPluginManager().callEvent(damageEvent)
                if (!damageEvent.isCancelled) {
                    victim.damage(damageEvent.amount.toDouble(), player)
                }
            }
        }

        player.setCooldown(material, cooldownTicks)
    }

    private fun sweepTarget(player: Player, victim: LivingEntity) {
        victim.world.spawnParticle(Particle.CLOUD, victim.location, 25, 0.75, 1.0, 0.75, 0.0)
        val direction = victim.location.toVector().subtract(player.location.toVector()).normalize()
        KnockbackUtil.knockBackCustom(victim, direction, knockback)

        for (entity in
            victim.world.getNearbyEntities(victim.location, radius, radius, radius) { target ->
                isValidAlly(player, target)
            }) {
            if (entity.uniqueId == player.uniqueId) continue
            val ally = entity as? Player ?: continue
            shieldPlayer(player, ally, allyShield, this)
        }
    }

    companion object {
        const val SPELL_NAME = "Sacred Wings"
        const val COOLDOWN = 16.0
        const val MANA_COST = 35
        const val DURATION = 8.0
        const val RADIUS = 8.0
        const val SHIELD = 30.0
        const val SHIELD_PER_LEVEL = 1.5
        const val ALLY_SHIELD = 20.0
        const val ALLY_SHIELD_PER_LEVEL = 1.0
        const val KNOCKBACK = 1.0
        const val SWEEP_COOLDOWN = 2.0
        const val BEAM_WIDTH = 2.0
        const val SWEEP_DISTANCE = 2.0
    }
}
