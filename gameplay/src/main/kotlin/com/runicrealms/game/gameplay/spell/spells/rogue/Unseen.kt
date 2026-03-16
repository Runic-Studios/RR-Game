package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.rogue.ShroudedEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import java.util.UUID
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Grants Shrouded for a duration. Shrouded grants stealth and mob damage immunity. Taking or
 * dealing player damage breaks stealth.
 */
class Unseen(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var description =
        "For ${duration}s, you vanish completely and gain &8shrouded&7. " +
            "Dealing or taking damage from players ends the effect early. " +
            "While shrouded, you are immune to monster damage."

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f)
        player.world.spawnParticle(
            Particle.DUST,
            player.eyeLocation,
            15,
            0.5,
            0.5,
            0.5,
            Particle.DustOptions(Color.BLACK, 1.0f),
        )

        ShroudedEffect(caster = player, duration = duration, spellEffectAPI = deps.spellEffectAPI)
            .initialize()
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        val victim = event.victim as? Player ?: return
        val effect = getSpellEffect(victim.uniqueId, victim.uniqueId, SpellEffectType.SHROUDED)
        if (effect.isPresent) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        breakShrouded(event.caster.uniqueId)
        val victim = event.victim as? Player ?: return
        breakShrouded(victim.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        breakShrouded(event.caster.uniqueId)
        val victim = event.victim as? Player ?: return
        breakShrouded(victim.uniqueId)
    }

    private fun breakShrouded(uuid: UUID) {
        val effect = getSpellEffect(uuid, uuid, SpellEffectType.SHROUDED)
        val shrouded = effect.orElse(null) as? ShroudedEffect ?: return
        shrouded.cancel()
    }

    companion object {
        const val SPELL_NAME = "Unseen"
        const val COOLDOWN = 18.0
        const val MANA_COST = 25
        const val DURATION = 8.0
    }
}
