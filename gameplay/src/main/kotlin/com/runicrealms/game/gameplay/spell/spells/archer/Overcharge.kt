package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.archer.ChargedEffect
import com.runicrealms.game.gameplay.spell.effect.archer.StaticEffect
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.LeaveCombatEvent
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class Overcharge(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps), DurationSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var duration = DURATION
    var manaToRestore = MANA_RESTORE
    var percent = PERCENT
    var markedDuration = MARKED_DURATION
    var maxStacks = MAX_STACKS
    var stackDuration = STACK_DURATION
    var stacksPerIncrement = STACKS_PER_INCREMENT
    override val description: String
        get() =
            "Thunder Arrow/Jolt marks enemies with static for ${markedDuration}s. " +
                "Ranged basic attacks against marked enemies consume the mark, restore ${manaToRestore} mana, " +
                "and grant Charged stacks. Charged increases attack speed by ${(percent * 100).toInt()}% per stack."

    init {
        isPassive = true
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell.
    }

    @EventHandler
    fun onExitCombat(event: LeaveCombatEvent) {
        val uuid = event.player.uniqueId
        val charged =
            getSpellEffect(uuid, uuid, SpellEffectType.CHARGED).orElse(null) as? ChargedEffect
                ?: return
        charged.cancel()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        val player = event.caster
        if (!hasPassive(player.uniqueId, name)) return
        if (event.spell !is ThunderArrow && event.spell !is Jolt) return

        val existing =
            getSpellEffect(player.uniqueId, event.victim.uniqueId, SpellEffectType.STATIC)
                .orElse(null) as? StaticEffect
        if (existing != null) {
            existing.cancel()
        }

        StaticEffect(
                player,
                event.victim,
                System.currentTimeMillis(),
                markedDuration,
                deps.spellEffectAPI,
            )
            .initialize()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onRangedDamage(event: PhysicalDamageEvent) {
        if (!event.isRanged) return
        val player = event.caster
        val staticEffect =
            getSpellEffect(player.uniqueId, event.victim.uniqueId, SpellEffectType.STATIC)
                .orElse(null) as? StaticEffect ?: return

        staticEffect.cancel()
        val mana = spellManager.getMana(player.uniqueId)
        spellManager.setMana(player.uniqueId, mana + manaToRestore.toInt())
        addChargedStack(player)
    }

    private fun addChargedStack(player: Player) {
        val charged =
            getSpellEffect(player.uniqueId, player.uniqueId, SpellEffectType.CHARGED).orElse(null)
                as? ChargedEffect

        if (charged != null) {
            repeat(stacksPerIncrement.toInt()) { charged.increment() }
            return
        }

        val newEffect =
            ChargedEffect(
                player,
                maxStacks.toInt(),
                stackDuration,
                System.currentTimeMillis(),
                deps.spellEffectAPI,
            )
        newEffect.initialize()
        repeat(stacksPerIncrement.toInt()) { newEffect.increment() }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBasicAttack(event: BasicAttackEvent) {
        val uuid = event.player.uniqueId
        val charged =
            getSpellEffect(uuid, uuid, SpellEffectType.CHARGED).orElse(null) as? ChargedEffect
                ?: return
        if (!hasPassive(uuid, name)) return

        val stacks = charged.stacks.get()
        val ticksToReduce = (event.originalCooldownTicks * percent) * stacks
        event.cooldownTicks =
            maxOf(
                event.cooldownTicks - ticksToReduce,
                BasicAttackEvent.MINIMUM_COOLDOWN_TICKS.toDouble(),
            )
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        manaToRestore = loadDouble(config, "mana-restore", manaToRestore)
        percent = loadDouble(config, "percent", percent)
        markedDuration = loadDouble(config, "marked-duration", markedDuration)
        maxStacks = loadDouble(config, "max-stacks", maxStacks)
        stackDuration = loadDouble(config, "stack-duration", stackDuration)
    }

    companion object {
        const val SPELL_NAME = "Overcharge"
        const val DURATION = 8.0
        const val MANA_RESTORE = 10.0
        const val PERCENT = 0.03
        const val MARKED_DURATION = 8.0
        const val MAX_STACKS = 10.0
        const val STACK_DURATION = 5.0
        const val STACKS_PER_INCREMENT = 2.0
    }
}
