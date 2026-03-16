package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.RunicBowEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent

class SteadyAim(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), AttributeSpell, DurationSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var attribute = ATTRIBUTE
    override var attributeBaseValue = ATTRIBUTE_BASE_VALUE
    override var attributeMultiplier = ATTRIBUTE_MULTIPLIER
    override var duration = DURATION
    override var description =
        "Every ${duration}s you do not basic attack, gain a stack (up to $MAX_STACKS) " +
            "that adds bonus physical damage to your next basic attack. " +
            "The final Leaping Shot arrow grants maximum stacks."

    private val stacks: MutableMap<UUID, SpellPayload> = HashMap()

    init {
        isPassive = true
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            Runnable {
                val now = System.currentTimeMillis()
                for ((uuid, payload) in stacks) {
                    if (now - payload.lastTimeShot < duration * 1000.0) continue
                    if (now - payload.lastStackTime < duration * 1000.0) continue
                    val previous = payload.stacks
                    payload.stacks = (payload.stacks + 1).coerceIn(0, MAX_STACKS)
                    payload.lastStackTime = now
                    if (previous < MAX_STACKS && payload.stacks >= MAX_STACKS) {
                        val player = Bukkit.getPlayer(uuid) ?: continue
                        player.playSound(
                            player,
                            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                            SoundCategory.NEUTRAL,
                            1f,
                            1f,
                        )
                        player.sendMessage("§a§lSteady Aim is fully charged")
                    }
                }
            },
            0L,
            20L,
        )
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onRunicBow(event: RunicBowEvent) {
        if (!hasPassive(event.player.uniqueId, name)) return
        val payload = stacks.getOrPut(event.player.uniqueId) { SpellPayload() }
        payload.lastTimeShot = System.currentTimeMillis()
        payload.lastStackTime = System.currentTimeMillis()
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        val payload = stacks[event.caster.uniqueId] ?: return
        if (payload.stacks <= 0) return

        // TODO: StatAPI pending (SPELL_MIGRATION.md #3)
        val amount = attributeBaseValue.toInt()
        event.amount += amount * payload.stacks
        payload.stacks = 0
    }

    @EventHandler
    fun onLeapingShotArrowHit(event: LeapingShot.Companion.ArrowHitEvent) {
        if (!event.isLast) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        val payload = stacks[event.caster.uniqueId] ?: return
        payload.stacks = MAX_STACKS
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        stacks.remove(event.player.uniqueId)
    }

    private class SpellPayload {
        var lastTimeShot: Long = System.currentTimeMillis()
        var lastStackTime: Long = System.currentTimeMillis()
        var stacks: Int = 0
    }

    companion object {
        const val SPELL_NAME = "Steady Aim"
        const val ATTRIBUTE = "dexterity"
        const val ATTRIBUTE_BASE_VALUE = 6.0
        const val ATTRIBUTE_MULTIPLIER = 0.0
        const val DURATION = 2.0
        const val MAX_STACKS = 4
    }
}
