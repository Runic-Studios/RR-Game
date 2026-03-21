package com.runicrealms.game.gameplay.spell.effect

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.effect.warrior.BleedEffect
import com.runicrealms.game.gameplay.spell.event.SpellEffectEvent
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import java.util.Optional
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * Manages all active [SpellEffect] instances. Ticks them every [TICK_PERIOD] game ticks. Expired
 * effects have [SpellEffect.onExpire] called and are removed.
 */
@Singleton
class SpellEffectManager @Inject constructor(private val plugin: Plugin) :
    SpellEffectAPI, Listener {

    private val activeSpellEffects: MutableSet<SpellEffect> = mutableSetOf()
    private var globalCounter: Int = 0

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getScheduler().runTaskTimer(plugin, ::tickAll, 0L, TICK_PERIOD)
    }

    /** Reduces healing received when the target has an active [BleedEffect]. */
    @EventHandler
    fun onSpellHeal(event: SpellHealEvent) {
        val recipientUuid = event.recipient.uniqueId
        if (hasSpellEffect(recipientUuid, SpellEffectType.BLEED)) {
            event.amount = (event.amount * (1.0 - BleedEffect.HEALING_REDUCTION)).toInt()
        }
    }

    private fun tickAll() {
        globalCounter += TICK_PERIOD.toInt()
        val toRemove = mutableListOf<SpellEffect>()
        for (effect in activeSpellEffects) {
            try {
                effect.tick(globalCounter)
                if (!effect.isActive()) {
                    toRemove.add(effect)
                }
            } catch (exception: Exception) {
                logger.error("Error ticking SpellEffect ${effect.effectType}", exception)
                toRemove.add(effect)
            }
        }
        for (effect in toRemove) {
            try {
                effect.onExpire()
                if (effect is StackEffect) {
                    effect.stackHologram?.remove()
                }
            } catch (exception: Exception) {
                logger.error("Error expiring SpellEffect ${effect.effectType}", exception)
            }
            activeSpellEffects.remove(effect)
        }
    }

    override fun addSpellEffectToManager(spellEffect: SpellEffect) {
        val event = SpellEffectEvent(spellEffect)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return
        if (spellEffect is StackEffect) {
            spellEffect.initializeNextTick(globalCounter)
        }
        activeSpellEffects.add(spellEffect)
    }

    override fun hasSpellEffect(uuid: UUID, effectType: SpellEffectType): Boolean =
        activeSpellEffects.any { it.effectType == effectType && it.recipient.uniqueId == uuid }

    override fun getSpellEffect(
        casterUuid: UUID,
        recipientUuid: UUID,
        identifier: SpellEffectType,
    ): Optional<SpellEffect> =
        Optional.ofNullable(
            activeSpellEffects.firstOrNull {
                it.effectType == identifier &&
                    it.caster.uniqueId == casterUuid &&
                    it.recipient.uniqueId == recipientUuid
            }
        )

    override fun getSpellEffects(
        recipientId: UUID,
        identifier: SpellEffectType,
    ): List<SpellEffect> =
        activeSpellEffects.filter {
            it.effectType == identifier && it.recipient.uniqueId == recipientId
        }

    override fun determineHighestStacks(recipientId: UUID, identifier: SpellEffectType): Int =
        activeSpellEffects
            .filter { it.effectType == identifier && it.recipient.uniqueId == recipientId }
            .filterIsInstance<StackEffect>()
            .maxOfOrNull { it.stacks.get() } ?: 0

    fun getGlobalCounter(): Int = globalCounter

    companion object {
        const val TICK_PERIOD = 5L
    }
}
