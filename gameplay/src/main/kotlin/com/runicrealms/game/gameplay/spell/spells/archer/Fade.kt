package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player

class Fade(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.ARCHER, deps), DurationSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var description =
        "You begin to flicker in and out of invisibility for the next ${duration}s! " +
            "While invisible, you cannot cast spells."

    override fun executeSpell(player: Player, type: SpellItemType) {
        var count = 1
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count >= duration) {
                    task.cancel()
                    return@runTaskTimer
                }
                if (count % 2 == 0) {
                    reappear(player)
                } else {
                    disappear(player)
                }
                count += PERIOD
            },
            0L,
            (PERIOD * 20).toLong(),
        )
    }

    private fun disappear(player: Player) {
        addStatusEffect(player, RunicStatusEffect.SILENCE, 1.0, false)
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f)
        player.world.spawnParticle(
            Particle.DUST,
            player.eyeLocation,
            15,
            0.5,
            0.5,
            0.5,
            0.0,
            Particle.DustOptions(Color.BLACK, 1.0f),
        )
        for (loaded in Bukkit.getOnlinePlayers()) {
            if (loaded.uniqueId == player.uniqueId) continue
            loaded.hidePlayer(deps.plugin, player)
        }
    }

    private fun reappear(player: Player) {
        for (loaded in Bukkit.getOnlinePlayers()) {
            if (loaded.uniqueId == player.uniqueId) continue
            loaded.showPlayer(deps.plugin, player)
        }
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f)
        player.world.spawnParticle(
            Particle.DUST,
            player.eyeLocation,
            15,
            0.5,
            0.5,
            0.5,
            0.0,
            Particle.DustOptions(Color.BLACK, 1.0f),
        )
    }

    companion object {
        const val SPELL_NAME = "Fade"
        const val COOLDOWN = 10.0
        const val MANA_COST = 30
        const val DURATION = 6.0
        const val PERIOD = 1
    }
}
