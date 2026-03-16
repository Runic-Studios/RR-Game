package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spells.Combat
import com.runicrealms.game.gameplay.spell.spells.Potion
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import java.util.HashMap
import java.util.HashSet
import java.util.UUID
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive empowerment while Shrouded:
 * - Dash: cleanse slows, grant Speed III
 * - Twin Fangs: both hits critically strike
 * - Cocoon: teleport behind the victim on a successful hit
 */
class FromTheShadows(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var duration = DURATION
    override var description =
        "While you are &8shrouded&7, your first spell cast is empowered." +
            "\n\n&aDash &7- Cleanse slows and gain Speed III for ${duration}s." +
            "\n\n&aTwin Fangs &7- Both fangs critically strike." +
            "\n\n&aCocoon &7- Landing this spell teleports you behind the target."

    private val buffedTwinFangsHits: MutableMap<UUID, Int> = HashMap()
    private val pendingCocoonTeleport: MutableSet<UUID> = HashSet()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEmpoweredSpell(event: PhysicalDamageEvent) {
        val casterId = event.caster.uniqueId
        if (!hasPassive(casterId, name)) return

        if (event.spell is TwinFangs) {
            val hits = buffedTwinFangsHits[casterId] ?: 0
            if (hits > 0) {
                event.isCritical = true
                if (hits == 1) {
                    buffedTwinFangsHits.remove(casterId)
                } else {
                    buffedTwinFangsHits[casterId] = hits - 1
                }
            }
        }

        if (event.spell is Cocoon && pendingCocoonTeleport.remove(casterId)) {
            val direction = event.victim.location.direction.normalize().multiply(-1.0)
            val behind =
                event.victim.location
                    .clone()
                    .add(direction)
                    .setDirection(event.victim.location.direction)
            event.caster.teleport(behind)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (!hasSpellEffect(event.caster.uniqueId, SpellEffectType.SHROUDED)) return
        if (event.spell is Combat || event.spell is Potion) return

        when (event.spell) {
            is Dash -> {
                removeStatusEffect(event.caster, RunicStatusEffect.SLOW_I)
                removeStatusEffect(event.caster, RunicStatusEffect.SLOW_II)
                removeStatusEffect(event.caster, RunicStatusEffect.SLOW_III)
                addStatusEffect(event.caster, RunicStatusEffect.SPEED_III, duration, true)
            }

            is TwinFangs -> buffedTwinFangsHits[event.caster.uniqueId] = 2
            is Cocoon -> {
                pendingCocoonTeleport.add(event.caster.uniqueId)
                deps.plugin.server.scheduler.runTaskLater(
                    deps.plugin,
                    Runnable { pendingCocoonTeleport.remove(event.caster.uniqueId) },
                    80L,
                )
            }
        }
    }

    companion object {
        const val SPELL_NAME = "From The Shadows"
        const val DURATION = 4.0
    }
}
