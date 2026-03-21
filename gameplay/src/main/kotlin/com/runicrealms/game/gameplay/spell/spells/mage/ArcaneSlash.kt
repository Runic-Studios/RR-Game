package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.SlashEffect
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Slashes in a line up to [distance] blocks, dealing magic damage to all enemies hit. If at least
 * one enemy is hit, grants the caster a shield.
 */
class ArcaneSlash(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), DistanceSpell, MagicDamageSpell, ShieldingSpell {

    override var distance = BASE_DISTANCE
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var shieldAmount = BASE_SHIELD
    override var shieldPerLevel = SHIELD_PER_LEVEL
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Slash in a line dealing $magicDamage magic damage. If an enemy is hit, gain a shield."

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2.0f)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f)
        SlashEffect.slashHorizontal(player, Particle.ENCHANT, player.location)
        val origin = player.location.add(0.0, 1.0, 0.0)
        val dir = origin.direction.normalize()
        var hitAny = false

        for (i in 1..distance.toInt()) {
            val point = origin.clone().add(dir.clone().multiply(i))
            point.world.spawnParticle(Particle.ENCHANT, point, 3, BEAM_WIDTH, 0.2, BEAM_WIDTH)

            for (entity in point.world.getNearbyEntities(point, BEAM_WIDTH, 1.0, BEAM_WIDTH)) {
                if (entity !is LivingEntity || entity == player) continue
                if (!isValidEnemy(player, entity)) continue
                hitAny = true
                deps.damageHandler.dealMagicDamage(magicDamage.toInt(), entity, player, this)
            }
        }

        if (hitAny) {
            shieldPlayer(player, player, shieldAmount)
        }
    }

    companion object {
        const val SPELL_NAME = "Arcane Slash"
        const val BASE_DISTANCE = 8.0
        const val BASE_DAMAGE = 20.0
        const val DAMAGE_PER_LEVEL = 0.5
        const val BASE_SHIELD = 25.0
        const val SHIELD_PER_LEVEL = 0.5
        const val COOLDOWN = 5.0
        const val MANA_COST = 15
        const val BEAM_WIDTH = 2.0
    }
}
