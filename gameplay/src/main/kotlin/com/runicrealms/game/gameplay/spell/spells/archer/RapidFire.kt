package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.RunicBowEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.EntityTrail
import com.runicrealms.game.gameplay.spell.spellutil.particles.HelixParticleFrame
import java.util.UUID
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class RapidFire(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), DurationSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    var percent = PERCENT
    override var description =
        "For ${duration}s, rapid-fire arrows and gain ${(percent * 100).toInt()}% attack speed."

    private val players: MutableMap<UUID, Long> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_CREEPER_PRIMED, 0.5f, 1.0f)
        HelixParticleFrame(1.0, 30.0, 10.0)
            .playParticle(player, Particle.CRIT, player.location, 1.0)
        players[player.uniqueId] = System.currentTimeMillis()
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        percent = config.getDouble("percent", percent * 100.0) / 100.0
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBasicAttack(event: BasicAttackEvent) {
        if (!isUsing(event.player)) return
        val reduction = event.originalCooldownTicks * percent
        event.cooldownTicks =
            maxOf(
                event.cooldownTicks - reduction,
                BasicAttackEvent.MINIMUM_COOLDOWN_TICKS.toDouble(),
            )
    }

    @EventHandler
    fun onRunicBow(event: RunicBowEvent) {
        if (!isUsing(event.player)) return
        event.player.playSound(
            event.player,
            Sound.BLOCK_BAMBOO_PLACE,
            SoundCategory.PLAYERS,
            1f,
            1f,
        )
        EntityTrail.entityTrail(event.arrow, Particle.CRIT, 5000L)
    }

    fun isUsing(player: Player): Boolean {
        val start = players[player.uniqueId] ?: return false
        return start + (duration * 1000.0).toLong() > System.currentTimeMillis()
    }

    companion object {
        const val SPELL_NAME = "Rapid Fire"
        const val COOLDOWN = 18.0
        const val MANA_COST = 40
        const val DURATION = 8.0
        const val PERCENT = 0.30
    }
}
