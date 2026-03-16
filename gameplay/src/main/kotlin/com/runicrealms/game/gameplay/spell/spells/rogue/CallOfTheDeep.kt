package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import java.util.HashMap
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerQuitEvent

/** Passive synergy spell for Harpoon, Dash, Scurvy, and Whirlpool. */
class CallOfTheDeep(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), WarmupSpell, PhysicalDamageSpell, DurationSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var warmupSeconds = WARMUP
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override var duration = STUN_DURATION
    private var dashBuff = DASH_BUFF
    private var dashBuffDuration = DASH_BUFF_DURATION
    override var description =
        "After landing &aHarpoon&7 on an enemy, refund half of Harpoon's current cooldown. " +
            "Basic attacks against that enemy within $DASH_BUFF_DURATION s reduce &aDash&7 cooldown by $DASH_BUFF s. " +
            "Applying &aScurvy&7 on enemies inside &aWhirlpool&7 summons a creature from the depths " +
            "after $WARMUP s, dealing ($PHYSICAL_DAMAGE + &f${PHYSICAL_DAMAGE_PER_LEVEL}x&7 lvl) " +
            "physical⚔ damage and stunning for $STUN_DURATION s."
    private val harpooned: MutableMap<UUID, Long> = HashMap()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        dashBuff = config.getDouble("dash-buff", dashBuff)
        dashBuffDuration = config.getDouble("dash-buff-duration", dashBuffDuration)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onHarpoonHit(event: Harpoon.Companion.HarpoonHitEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        val manager = spellManager as? SpellManager ?: return
        val remaining = manager.getRemainingCooldown(event.caster, Harpoon.SPELL_NAME)
        spellManager.reduceCooldown(event.caster, Harpoon.SPELL_NAME, remaining / 2.0)
        harpooned[event.victim.uniqueId] = System.currentTimeMillis()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        val timestamp = harpooned[event.victim.uniqueId] ?: return
        if (System.currentTimeMillis() > timestamp + (dashBuffDuration * 1000L).toLong()) return
        spellManager.reduceCooldown(event.caster, Dash.SPELL_NAME, dashBuff)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onScurvyDebuff(event: Scurvy.Companion.DebuffEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        val whirlpool = spellManager.getSpell(Whirlpool.SPELL_NAME) as? Whirlpool ?: return
        if (!whirlpool.isInWhirlPool(event.victim)) return

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                val damageEvent =
                    PhysicalDamageEvent(
                        physicalDamage.toInt(),
                        event.victim,
                        event.caster,
                        false,
                        false,
                        this,
                    )
                Bukkit.getPluginManager().callEvent(damageEvent)
                if (!damageEvent.isCancelled) {
                    event.victim.damage(damageEvent.amount.toDouble(), event.caster)
                }
                addStatusEffect(event.victim, RunicStatusEffect.STUN, duration, true)
            },
            (warmupSeconds * 20).toLong(),
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) {
        harpooned.remove(event.entity.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        harpooned.remove(event.player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Call Of The Deep"
        const val WARMUP = 2.0
        const val PHYSICAL_DAMAGE = 20.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val STUN_DURATION = 1.5
        const val DASH_BUFF = 0.5
        const val DASH_BUFF_DURATION = 3.0
    }
}
