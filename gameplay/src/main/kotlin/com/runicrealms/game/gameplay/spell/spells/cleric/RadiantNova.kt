package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.cleric.RadiantFireEffect
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.Cone
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player

class RadiantNova(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), HealingSpell, RadiusSpell, WarmupSpell {
    override var healAmount = BASE_HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var radius = BASE_RADIUS
    override var warmupSeconds = BASE_WARMUP
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() = "Charge radiant energy, then heal allies in $radius blocks."

    override fun executeSpell(player: Player, type: SpellItemType) {
        val effectOpt =
            getSpellEffect(player.uniqueId, player.uniqueId, SpellEffectType.RADIANT_FIRE)
        val reachedThreshold =
            if (effectOpt.isPresent) {
                val radiant = effectOpt.get() as? RadiantFireEffect
                val threshold =
                    (spellManager.getSpell(RadiantFire.SPELL_NAME) as? RadiantFire)
                        ?.stackThreshold
                        ?.toInt() ?: Int.MAX_VALUE
                radiant != null && radiant.stacks.get() >= threshold
            } else {
                false
            }

        if (!reachedThreshold) {
            player.world.playSound(player.location, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.0f, 1.0f)
            player.world.playSound(player.location, Sound.ENTITY_TNT_PRIMED, 1.0f, 2.0f)
            Cone.coneEffect(player, Particle.FIREWORK, Color.WHITE, 1.0)
            addStatusEffect(player, RunicStatusEffect.SLOW_III, warmupSeconds, false)
            Bukkit.getScheduler()
                .runTaskLater(
                    deps.plugin,
                    Runnable { executeHeal(player, true) },
                    (warmupSeconds * 20).toLong(),
                )
        } else {
            executeHeal(player, false)
        }
    }

    private fun executeHeal(player: Player, hasWarmup: Boolean) {
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f)
        val location = player.eyeLocation.clone()
        spawnSphere(player, location)
        for (entity in player.world.getNearbyEntities(location, radius, radius, radius)) {
            val ally = entity as? Player ?: continue
            if (!isValidAlly(player, ally)) continue
            healPlayer(player, ally, healAmount, this)
            if (!hasWarmup) {
                deps.statusEffectAPI.cleanse(ally.uniqueId)
            }
        }
    }

    private fun spawnSphere(player: Player, location: Location) {
        var i = 0.0
        while (i <= Math.PI) {
            val ringRadius = Math.sin(i) * radius
            val y = Math.cos(i)
            var angle = 0.0
            while (angle < Math.PI * 2) {
                val x = 0.9 * Math.cos(angle) * ringRadius
                val z = 0.9 * Math.sin(angle) * ringRadius
                location.add(x, y, z)
                player.world.spawnParticle(Particle.INSTANT_EFFECT, location, 1, 0.0, 0.0, 0.0, 0.0)
                location.subtract(x, y, z)
                angle += Math.PI / 12
            }
            i += Math.PI / 12
        }
    }

    companion object {
        const val SPELL_NAME = "Radiant Nova"
        private const val BASE_HEAL = 24.0
        private const val HEAL_PER_LEVEL = 0.5
        private const val BASE_RADIUS = 6.0
        private const val BASE_WARMUP = 1.5
        private const val COOLDOWN = 10.0
        private const val MANA_COST = 24
    }
}
