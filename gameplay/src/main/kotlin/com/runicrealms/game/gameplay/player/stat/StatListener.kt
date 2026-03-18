package com.runicrealms.game.gameplay.player.stat

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.StatType
import com.runicrealms.game.gameplay.player.RegenManager
import com.runicrealms.game.gameplay.player.stat.StatConstants.ABILITY_HASTE
import com.runicrealms.game.gameplay.player.stat.StatConstants.DAMAGE_REDUCTION_CAP
import com.runicrealms.game.gameplay.player.stat.StatConstants.DAMAGE_REDUCTION_MULT
import com.runicrealms.game.gameplay.player.stat.StatConstants.HEALTH_REGEN_MULT
import com.runicrealms.game.gameplay.player.stat.StatConstants.MAGIC_DMG_MULT
import com.runicrealms.game.gameplay.player.stat.StatConstants.MANA_REGEN_MULT
import com.runicrealms.game.gameplay.player.stat.StatConstants.PHYSICAL_DMG_MULT
import com.runicrealms.game.gameplay.player.stat.StatConstants.SPELL_HEALING_MULT
import com.runicrealms.game.gameplay.player.stat.StatConstants.SPELL_SHIELDING_MULT
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.event.SpellShieldEvent
import com.runicrealms.game.gameplay.spell.spells.Potion
import com.runicrealms.game.items.event.GameStatUpdateEvent
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Applies per-stat multipliers to game events using values from [StatManager].
 *
 * Also listens to [GameStatUpdateEvent] to keep max mana in sync when equipment changes.
 */
@Singleton
class StatListener
@Inject
constructor(
    private val plugin: Plugin,
    private val statManager: StatManager,
    private val regenManager: RegenManager,
    private val spellManager: SpellManager,
) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onHealthRegen(event: com.runicrealms.game.gameplay.player.HealthRegenEvent) {
        val uuid = event.player.uniqueId
        val bonus = (event.amount * HEALTH_REGEN_MULT * getStat(uuid, StatType.VITALITY)).toInt()
        event.amount += bonus
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onMagicDamage(event: MagicDamageEvent) {
        val casterUuid = event.caster.uniqueId
        val intelligenceBonus =
            ceil(event.amount * MAGIC_DMG_MULT * getStat(casterUuid, StatType.INTELLIGENCE)).toInt()
        event.amount += intelligenceBonus

        val victim = event.victim
        if (victim is Player) {
            event.amount = applyDamageReduction(event.amount, victim.uniqueId)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onMobDamage(event: MobDamageEvent) {
        val victim = event.victim
        if (victim !is Player) return
        event.amount = applyDamageReduction(event.amount, victim.uniqueId)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        val casterUuid = event.caster.uniqueId
        val strengthBonus =
            ceil(event.amount * PHYSICAL_DMG_MULT * getStat(casterUuid, StatType.STRENGTH)).toInt()
        event.amount += strengthBonus

        val victim = event.victim
        if (victim is Player) {
            event.amount = applyDamageReduction(event.amount, victim.uniqueId)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onManaRegen(event: com.runicrealms.game.gameplay.player.ManaRegenEvent) {
        val uuid = event.player.uniqueId
        val bonus = (event.amount * MANA_REGEN_MULT * getStat(uuid, StatType.INTELLIGENCE)).toInt()
        event.amount += bonus
    }

    /** Reduces spell cooldown via dexterity haste. Skips [Potion] casts (no cooldown scaling). */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onSpellCast(event: SpellCastEvent) {
        if (event.isCancelled) return
        if (event.spell is Potion) return
        val uuid = event.caster.uniqueId
        val dex = getStat(uuid, StatType.DEXTERITY)
        val secondsToReduce = event.spell.cooldown * ABILITY_HASTE * dex
        spellManager.reduceCooldown(event.caster, event.spell.name, secondsToReduce)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onSpellHeal(event: SpellHealEvent) {
        if (event.spell == null) return
        val uuid = event.caster.uniqueId
        val bonus = ceil(event.amount * SPELL_HEALING_MULT * getStat(uuid, StatType.WISDOM)).toInt()
        event.amount += bonus
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onSpellShield(event: SpellShieldEvent) {
        if (event.spell == null) return
        val uuid = event.caster.uniqueId
        val bonus =
            ceil(event.amount * SPELL_SHIELDING_MULT * getStat(uuid, StatType.WISDOM)).toInt()
        event.amount += bonus
    }

    /**
     * Recalculates max mana when a player's equipment changes (e.g. they equip wisdom gear).
     * Mirrors the old StatChangeEvent -> ManaListener.calculateMaxMana() flow.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onStatUpdate(event: GameStatUpdateEvent) {
        regenManager.calculateMaxMana(event.character)
    }

    /**
     * Clamps [amount] down by vitality-based damage reduction (capped at [DAMAGE_REDUCTION_CAP]).
     */
    private fun applyDamageReduction(amount: Int, victimUuid: UUID): Int {
        val vitality = getStat(victimUuid, StatType.VITALITY)
        val reductionPercent = min(DAMAGE_REDUCTION_MULT * vitality, DAMAGE_REDUCTION_CAP)
        val reduced = (amount - ceil(amount * reductionPercent)).toInt()
        return maxOf(1, reduced)
    }

    private fun getStat(uuid: UUID, stat: StatType): Int = statManager.getStat(uuid, stat)
}
