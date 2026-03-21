package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.mage.ArcanumEffect
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HelixParticleFrame
import java.util.UUID
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Instantly shields caster and up to [maxAllies] nearby ally players. Activates or refreshes
 * [ArcanumEffect] on caster. While ArcanumEffect is active, each basic attack grants
 * [shieldPerSlash] shield and [manaPerSlash] mana.
 *
 * Each slash restores [MANA_PER_SLASH] mana through SpellManager's runtime mana map.
 */
class PrimalArcanum(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), DurationSpell, RadiusSpell, ShieldingSpell {

    override var duration = BASE_DURATION
    override var radius = BASE_RADIUS
    override var shieldAmount = BASE_SHIELD
    override var shieldPerLevel = SHIELD_PER_LEVEL
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Shield yourself and up to $MAX_ALLIES allies within $BASE_RADIUS blocks. " +
            "For ${BASE_DURATION}s, basic attacks grant shield and mana."

    companion object {
        const val SPELL_NAME = "Primal Arcanum"
        const val BASE_DURATION = 10.0
        const val BASE_RADIUS = 10.0
        const val BASE_SHIELD = 30.0
        const val SHIELD_PER_LEVEL = 0.5
        const val COOLDOWN = 14.0
        const val MANA_COST = 30
        const val MANA_PER_SLASH = 8
        const val MAX_ALLIES = 3
        const val SHIELD_PER_SLASH = 5.0
        const val SHIELD_PER_SLASH_PER_LEVEL = 0.4
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 2.0f)
        player.world.playSound(player.location, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.0f)
        HelixParticleFrame(3.0, 30.0, 12.0)
            .playParticle(player, Particle.WITCH, player.location, 15.0)

        shieldPlayer(player, player, shieldAmount)

        var count = 1
        for (entity in player.world.getNearbyEntities(player.location, radius, radius, radius)) {
            if (count > MAX_ALLIES) break
            if (entity !is Player || entity == player) continue
            if (!isValidAlly(player, entity)) continue
            shieldPlayer(player, entity, shieldAmount)
            count++
        }

        val existing = getSpellEffect(player.uniqueId, player.uniqueId, SpellEffectType.ARCANUM)
        if (existing.isPresent) {
            // Refresh by removing the old and creating a new one (re-initialize)
            existing.get().cancel()
        }
        ArcanumEffect(caster = player, duration = duration, spellEffectAPI = deps.spellEffectAPI)
            .initialize()
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onArcanumSlash(event: BasicAttackEvent) {
        val player = event.player
        val uuid: UUID = player.uniqueId
        if (!spellManager.isShielded(player)) return
        if (getSpellEffect(uuid, uuid, SpellEffectType.ARCANUM).isEmpty) return
        shieldPlayer(player, player, SHIELD_PER_SLASH)
        spellManager.setMana(uuid, spellManager.getMana(uuid) + MANA_PER_SLASH)
    }
}
