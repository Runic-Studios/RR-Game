package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.mage.IncendiaryEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.StaffAttackEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive. Whenever player deals magic damage with DragonsBreath/Erupt/Meteor, gains
 * IncendiaryEffect. First staff attack while incendiary fires a forward fire wave up to [distance]
 * blocks, dealing magic damage (no repeat hits per cast).
 */
class Incendiary(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), MagicDamageSpell, DistanceSpell, DurationSpell {

    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var distance = BASE_DISTANCE
    override var duration = BASE_DURATION
    var count = 5
    var durationOfDamage = 4.0
    var period = 2.0
    override var cooldown = 0.0
    override var manaCost = 0
    override val description: String
        get() = "Passive: After a fire spell, your next staff attack fires a fire wave."

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onMagicDamage(event: MagicDamageEvent) {
        if (event.spell !is DragonsBreath && event.spell !is Erupt && event.spell !is Meteor) return
        val player = event.caster
        if (!hasPassive(player.uniqueId, SPELL_NAME)) return
        IncendiaryEffect(player, duration = duration, spellEffectAPI = deps.spellEffectAPI)
            .initialize()
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onStaffAttack(event: StaffAttackEvent) {
        val player = event.player
        if (!hasPassive(player.uniqueId, SPELL_NAME)) return
        if (!hasSpellEffect(player.uniqueId, SpellEffectType.INCENDIARY)) return

        // Consume incendiary
        deps.spellEffectAPI.getSpellEffects(player.uniqueId, SpellEffectType.INCENDIARY).forEach {
            it.cancel()
        }

        fireWave(player)
    }

    private fun fireWave(player: Player) {
        val origin = player.location.add(0.0, 1.0, 0.0)
        val dir = origin.direction.normalize()
        val hitThisWave = mutableSetOf<LivingEntity>()
        var count = 1

        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count > distance.toInt()) {
                    task.cancel()
                    return@runTaskTimer
                }

                val point = origin.clone().add(dir.clone().multiply(count))

                if (!point.block.type.isAir) {
                    task.cancel()
                    return@runTaskTimer
                }

                HorizontalCircleFrame(BEAM_RADIUS, semiCircle = true)
                    .playParticle(player, Particle.FLAME, point, 0.8)
                point.world.playSound(point, Sound.ITEM_FIRECHARGE_USE, 0.25f, 2.0f)

                for (entity in
                    point.world.getNearbyEntities(point, BEAM_RADIUS, BEAM_RADIUS, BEAM_RADIUS)) {
                    if (entity !is LivingEntity || entity == player || entity in hitThisWave)
                        continue
                    if (!isValidEnemy(player, entity)) continue
                    hitThisWave.add(entity)
                    deps.damageHandler.dealMagicDamage(
                        magicDamage.toInt(),
                        entity,
                        player,
                        this@Incendiary,
                    )
                }

                count++
            },
            0L,
            1L,
        )
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        count = loadInt(config, "count", count)
        durationOfDamage = loadDouble(config, "duration-of-damage", durationOfDamage)
        period = loadDouble(config, "period", period)
    }

    companion object {
        const val SPELL_NAME = "Incendiary"
        const val BASE_DAMAGE = 25.0
        const val DAMAGE_PER_LEVEL = 0.75
        const val BASE_DISTANCE = 12.0
        const val BASE_DURATION = 8.0
        const val BEAM_RADIUS = 1.0
    }
}
