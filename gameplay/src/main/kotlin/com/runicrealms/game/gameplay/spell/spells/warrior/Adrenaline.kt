package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HelixParticleFrame
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * New ultimate spell for Berserker. On cast: grants Speed II for [duration]s. Each basic attack
 * against a bleeding enemy grants a stack of rage (up to [maxStacks]), each adding [percent]%
 * physical damage. Reaching max stacks cleanses debuffs, resets Speed II, and marks the caster as
 * enraged.
 */
class Adrenaline(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), DurationSpell {

    override var duration = BASE_DURATION
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    private var maxStacks = DEFAULT_MAX_STACKS
    private var percent = DEFAULT_PERCENT
    override val description: String
        get() =
            "For the next ${duration}s, gain Speed II! " +
                "Each basic attack against bleeding enemies grants a stack of rage, " +
                "dealing ${(DEFAULT_PERCENT * 100).toInt()}% more physical damage per stack, " +
                "up to ${DEFAULT_MAX_STACKS} stacks. " +
                "Reaching max rage cleanses all debuffs and resets the speed bonus!"

    private val rageMap: MutableMap<UUID, RagePayload> = HashMap()

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        maxStacks = config.getInt("max-stacks", maxStacks)
        percent = config.getDouble("percent", percent)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f)
        player.world.playSound(player.location, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.0f)
        HelixParticleFrame(2.0, 0.5, 1.0)
            .playParticle(player, Particle.DUST, player.location, 1.0, Color.RED)
        rageMap[player.uniqueId] = RagePayload()
        addStatusEffect(player, RunicStatusEffect.SPEED_II, duration, false)
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                rageMap.remove(player.uniqueId)
                player.playSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f)
            },
            (duration * 20).toLong(),
        )
    }

    private fun addStack(caster: Player) {
        val ragePayload = rageMap[caster.uniqueId] ?: return
        val currentStacks = ragePayload.stacks.get()
        val newStacks = minOf(currentStacks + 1, maxStacks)
        ragePayload.stacks.set(newStacks)
        caster.sendMessage(
            Component.text("You have $newStacks stacks of rage!", NamedTextColor.GREEN)
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        val playerId = event.caster.uniqueId
        if (!rageMap.containsKey(playerId) || !event.isBasicAttack) return

        val ragePayload = rageMap[playerId] ?: return
        var stacks = ragePayload.stacks.get()

        if (hasSpellEffect(event.victim.uniqueId, SpellEffectType.BLEED) && stacks < maxStacks) {
            addStack(event.caster)
            stacks = ragePayload.stacks.get()

            if (!ragePayload.isEnraged && stacks >= maxStacks) {
                ragePayload.isEnraged = true
                event.caster.sendMessage(
                    Component.text("You are ", NamedTextColor.GREEN)
                        .append(Component.text("ENRAGED", NamedTextColor.RED))
                        .append(Component.text("!", NamedTextColor.GREEN))
                )
                deps.statusEffectAPI.cleanse(playerId)
                addStatusEffect(event.caster, RunicStatusEffect.SPEED_II, duration, false)
            }
        }

        // Apply damage multiplier
        val multiplier = 1.0 + (percent * stacks)
        event.amount = (event.amount * multiplier).toInt()
    }

    private inner class RagePayload {
        val stacks = AtomicInteger(0)
        var isEnraged = false
    }

    companion object {
        const val SPELL_NAME = "Adrenaline"
        const val BASE_DURATION = 8.0
        const val COOLDOWN = 20.0
        const val MANA_COST = 40
        const val DEFAULT_MAX_STACKS = 5
        const val DEFAULT_PERCENT = 0.03
    }
}
