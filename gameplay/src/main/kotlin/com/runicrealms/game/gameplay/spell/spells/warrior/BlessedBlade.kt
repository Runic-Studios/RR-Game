package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.warrior.BlessedBladeEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spells.Combat
import com.runicrealms.game.gameplay.spell.spells.Potion
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive: casting a spell grants Blessed Blade charges. Basic attacks consume a charge to deal
 * bonus magic damage and heal the caster plus nearby allies.
 */
class BlessedBlade(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps),
    DurationSpell,
    HealingSpell,
    MagicDamageSpell,
    RadiusSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var healAmount = BASE_HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    private var maxCharges = MAX_CHARGES
    private var maxTargets = MAX_TARGETS
    override var description =
        "Each spell cast empowers your next ${MAX_CHARGES} basic attacks for ${duration}s. " +
            "Empowered attacks deal ($magicDamage + &f${magicDamagePerLevel}x&7 lvl) magicʔ damage, " +
            "then heal you and up to ${MAX_TARGETS.toInt()} allies within $radius blocks " +
            "for ($healAmount + &f${healPerLevel}x&7 lvl)."
    private val chargeMap: MutableMap<UUID, Int> = HashMap()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive spell
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        maxCharges = config.getInt("max-charges", maxCharges)
        maxTargets = config.getDouble("max-targets", maxTargets)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBasicAttack(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        if (!hasPassive(event.caster.uniqueId, name)) return

        val caster = event.caster
        val charges = chargeMap[caster.uniqueId] ?: return
        if (charges <= 0) return

        chargeMap[caster.uniqueId] = charges - 1

        val blessedBladeEffect =
            getSpellEffect(caster.uniqueId, caster.uniqueId, SpellEffectType.BLESSED_BLADE)
                .orElse(null) as? BlessedBladeEffect
        blessedBladeEffect?.decrement(caster.eyeLocation, (charges - 1).coerceAtLeast(0))

        val victim = event.victim as? LivingEntity ?: return
        val dmgEvent = MagicDamageEvent(magicDamage.toInt(), victim, caster, this)
        Bukkit.getPluginManager().callEvent(dmgEvent)
        if (!dmgEvent.isCancelled) {
            victim.damage(dmgEvent.amount.toDouble(), caster)
        }

        healPlayer(caster, caster, healAmount, this)
        var alliesHealed = 0
        for (entity in
            caster.world.getNearbyEntities(caster.location, radius, radius, radius) { target ->
                isValidAlly(caster, target)
            }) {
            if (entity.uniqueId == caster.uniqueId) continue
            val ally = entity as? Player ?: continue
            healPlayer(caster, ally, healAmount, this)
            alliesHealed++
            if (alliesHealed >= maxTargets.toInt()) break
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (event.spell is Combat || event.spell is Potion) return
        if (event.spell.name == name) return

        chargeMap[event.caster.uniqueId] = maxCharges
        getSpellEffect(event.caster.uniqueId, event.caster.uniqueId, SpellEffectType.BLESSED_BLADE)
            .ifPresent { it.cancel() }
        BlessedBladeEffect(
                caster = event.caster,
                duration = duration,
                stackDuration = duration,
                spellEffectAPI = deps.spellEffectAPI,
            )
            .apply {
                increment(event.caster.eyeLocation, maxCharges)
                initialize()
            }
    }

    companion object {
        const val SPELL_NAME = "Blessed Blade"
        const val COOLDOWN = 0.0
        const val MANA_COST = 0
        const val DURATION = 8.0
        const val BASE_HEAL = 10.0
        const val HEAL_PER_LEVEL = 0.5
        const val BASE_DAMAGE = 14.0
        const val DAMAGE_PER_LEVEL = 0.6
        const val RADIUS = 8.0
        const val MAX_CHARGES = 3
        const val MAX_TARGETS = 2.0
    }
}
