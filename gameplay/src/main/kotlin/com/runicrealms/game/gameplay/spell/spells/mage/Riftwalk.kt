package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive. After [Blink], blasts all enemies within [radius] blocks for magic damage. If any enemy
 * is hit, reduces Blink's cooldown by [duration] seconds.
 */
class Riftwalk(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), DurationSpell, MagicDamageSpell, RadiusSpell {

    override var duration = COOLDOWN_REDUCTION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Passive: After Blink, deal ($BASE_DAMAGE + ${DAMAGE_PER_LEVEL}x lvl) " +
            "magic damage within $BASE_RADIUS blocks. If any enemy is hit, reduce Blink cooldown by ${COOLDOWN_REDUCTION}s."

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBlinkCast(event: SpellCastEvent) {
        if (event.isCancelled) return
        if (event.spell !is Blink) return
        if (!hasPassive(event.caster.uniqueId, SPELL_NAME)) return
        val caster = event.caster

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                var foundEnemy = false
                caster.world.playSound(caster.location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.2f)
                for (entity in
                    caster.world.getNearbyEntities(caster.location, radius, radius, radius)) {
                    if (entity !is LivingEntity || entity == caster) continue
                    if (!isValidEnemy(caster, entity)) continue
                    if (
                        deps.damageHandler.dealMagicDamage(
                            magicDamage.toInt(),
                            entity,
                            caster,
                            this,
                        ) > 0
                    ) {
                        foundEnemy = true
                    }
                }
                if (foundEnemy) {
                    spellManager.reduceCooldown(caster, Blink.SPELL_NAME, duration)
                }
            },
            1L,
        )
    }

    companion object {
        const val SPELL_NAME = "Riftwalk"
        const val BASE_DAMAGE = 20.0
        const val DAMAGE_PER_LEVEL = 0.5
        const val BASE_RADIUS = 5.0
        const val COOLDOWN_REDUCTION = 3.0
    }
}
