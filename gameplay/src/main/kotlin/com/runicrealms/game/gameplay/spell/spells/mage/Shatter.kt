package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.StatType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.mage.ChilledEffect
import com.runicrealms.game.gameplay.spell.effect.mage.IceBarrierEffect
import com.runicrealms.game.gameplay.spell.event.LeaveCombatEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import java.util.UUID
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive (Cryomancer first passive).
 *
 * On basic attack against a Chilled enemy: consumes Chilled, deals magic damage, and
 * grants/increments an [IceBarrierEffect] stack on the caster.
 *
 * IceBarrier reduces physical and mob damage taken by (baseValue + multiplier * statValue)% per
 * stack, up to [maxStacks] stacks. Stacks expire after [stackDuration]s and are cleared on leaving
 * combat.
 */
class Shatter(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), AttributeSpell, MagicDamageSpell, ShieldingSpell {

    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var shieldAmount = 0.0
    override var shieldPerLevel = 0.0
    override var attributeBaseValue = BASE_VALUE
    override var attributeMultiplier = MULTIPLIER
    override var attribute = STAT_NAME
    var maxStacks = MAX_STACKS
    var stackDuration = STACK_DURATION
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Passive: Basic attacks on Chilled enemies shatter ice, dealing " +
            "($BASE_DAMAGE + ${DAMAGE_PER_LEVEL}x lvl) magic damage and gaining an Ice Barrier stack."

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return

        val victimId: UUID = event.victim.uniqueId

        // Reduce damage taken by victim if they have IceBarrier stacks
        val victimBarrier = getSpellEffect(victimId, victimId, SpellEffectType.ICE_BARRIER)
        if (victimBarrier.isPresent) {
            val statValue = deps.statManager.getStat(victimId, StatType.INTELLIGENCE)
            val reductionPercent = attributeBaseValue + attributeMultiplier * statValue
            val damageToReduce = (reductionPercent / 100.0) * event.amount
            event.amount = (event.amount - damageToReduce).toInt().coerceAtLeast(0)
        }

        if (!hasPassive(event.caster.uniqueId, SPELL_NAME)) return

        val uuid: UUID = event.caster.uniqueId
        val chilledOpt = getSpellEffect(uuid, victimId, SpellEffectType.CHILLED)
        if (chilledOpt.isEmpty) return

        val chilledEffect = chilledOpt.get() as ChilledEffect
        chilledEffect.cancel()

        deps.damageHandler.dealMagicDamage(magicDamage.toInt(), event.victim, event.caster, this)

        val iceBarrierOpt = getSpellEffect(uuid, uuid, SpellEffectType.ICE_BARRIER)
        if (iceBarrierOpt.isPresent) {
            (iceBarrierOpt.get() as IceBarrierEffect).addStack()
        } else {
            IceBarrierEffect(
                    caster = event.caster,
                    duration = stackDuration,
                    maxStacks = maxStacks,
                    spellEffectAPI = deps.spellEffectAPI,
                )
                .initialize()
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onMobDamage(event: MobDamageEvent) {
        val victimId: UUID = event.victim.uniqueId
        val victimBarrier = getSpellEffect(victimId, victimId, SpellEffectType.ICE_BARRIER)
        if (victimBarrier.isEmpty) return

        val statValue = deps.statManager.getStat(victimId, StatType.INTELLIGENCE)
        val reductionPercent = attributeBaseValue + attributeMultiplier * statValue
        val damageToReduce = (reductionPercent / 100.0) * event.amount
        event.amount = (event.amount - damageToReduce).toInt().coerceAtLeast(0)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onLeaveCombat(event: LeaveCombatEvent) {
        val uuid: UUID = event.player.uniqueId
        val iceBarrierOpt = getSpellEffect(uuid, uuid, SpellEffectType.ICE_BARRIER)
        if (iceBarrierOpt.isPresent) {
            (iceBarrierOpt.get() as IceBarrierEffect).clearStacks()
        }
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        // Config has a typo: "max-atacks" instead of "max-attacks"
        maxStacks = loadInt(config, "max-atacks", maxStacks)
        stackDuration = loadDouble(config, "stack-duration", stackDuration)
    }

    companion object {
        const val SPELL_NAME = "Shatter"
        const val BASE_DAMAGE = 20.0
        const val DAMAGE_PER_LEVEL = 0.5
        const val BASE_VALUE = 5.0
        const val MULTIPLIER = 0.1
        const val STAT_NAME = "intelligence"
        const val MAX_STACKS = 3
        const val STACK_DURATION = 20.0
    }
}
