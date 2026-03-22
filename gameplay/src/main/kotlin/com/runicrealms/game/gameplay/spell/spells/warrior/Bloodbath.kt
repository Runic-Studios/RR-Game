package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import java.util.UUID
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive for Cleave users:
 * - Heals once per Cleave cast.
 * - Increases damage against bleeding low-health targets.
 */
class Bloodbath(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), AttributeSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var attribute = "strength"
    override var attributeBaseValue = BASE_VALUE
    override var attributeMultiplier = MULTIPLIER
    private val eventMap: MutableMap<UUID, Long> = HashMap()
    private var percent = PERCENT
    private var healthCeiling = HEALTH_CEILING
    override val description: String
        get() =
            "Hitting an enemy with Cleave heals you for " +
                "($attributeBaseValue + &f${attributeMultiplier}x&7 ${attribute.uppercase()}) once per swing. " +
                "You also deal ${(PERCENT * 100).toInt()}% more damage to bleeding enemies under " +
                "${(HEALTH_CEILING * 100).toInt()}% HP."

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        percent = config.getDouble("percent", percent)
        healthCeiling = config.getDouble("health-ceiling", healthCeiling)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return

        eventMap.entries.removeIf { System.currentTimeMillis() - it.value > EXPIRY_DURATION_MS }

        if (event.spell is Cleave && !eventMap.containsKey(event.caster.uniqueId)) {
            eventMap[event.caster.uniqueId] = System.currentTimeMillis()
            val scaledHeal = attributeBaseValue
            healPlayer(event.caster, event.caster, scaledHeal, this)
        }

        if (!hasSpellEffect(event.victim.uniqueId, SpellEffectType.BLEED)) return
        val maxHp = event.victim.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        val healthRatio = event.victim.health / maxHp
        if (healthRatio >= healthCeiling) return
        event.amount = (event.amount * (1 + percent)).toInt()
    }

    companion object {
        const val SPELL_NAME = "Bloodbath"
        const val COOLDOWN = 0.0
        const val MANA_COST = 0
        const val EXPIRY_DURATION_MS = 5000L
        const val BASE_VALUE = 5.0
        const val MULTIPLIER = 1.0
        const val PERCENT = 0.1
        const val HEALTH_CEILING = 0.5
    }
}
