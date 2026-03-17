package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.rogue.SunderedEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/** Web beam that damages and slows one target, then applies Sundered. */
class Cocoon(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps),
    AttributeSpell,
    DistanceSpell,
    DurationSpell,
    PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var attribute = "dexterity"
    override var attributeBaseValue = BASE_VALUE
    override var attributeMultiplier = MULTIPLIER
    override var distance = DISTANCE
    override var duration = DURATION
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    private var damageCap = DAMAGE_CAP
    private var maxStacks = MAX_STACKS
    private var stackDuration = STACK_DURATION
    override var description =
        "You launch a short-range string of web that deals ($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) " +
            "physical⚔ damage to the first enemy hit within $distance blocks, then slows and applies " +
            "&9sundered &7for ${duration}s." +
            "\n\n&2&lEFFECT &9Sundered" +
            "\n&9Sundered &7enemies suffer an additional ($attributeBaseValue + ${attributeMultiplier}x DEX)% " +
            "physical damage from all sources. Stacks to $MAX_STACKS and each stack expires after $STACK_DURATION s."

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        damageCap = config.getDouble("damage-cap", damageCap)
        maxStacks = config.getDouble("max-stacks", maxStacks)
        stackDuration = config.getDouble("stack-duration", stackDuration)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f)
        val rayTrace =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                BEAM_WIDTH,
            ) { entity ->
                isValidEnemy(player, entity)
            }

        if (rayTrace == null || rayTrace.hitEntity == null) {
            val location = player.getTargetBlock(null, distance.toInt()).location
            VectorUtil.drawLine(player, Material.COBWEB, player.eyeLocation, location, 0.5)
            return
        }

        val target = rayTrace.hitEntity as? LivingEntity ?: return
        VectorUtil.drawLine(player, Material.COBWEB, player.eyeLocation, target.location, 0.5)
        target.world.playSound(target.location, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.25f, 2.0f)
        addStatusEffect(target, RunicStatusEffect.SLOW_III, duration, false)

        val damageEvent =
            PhysicalDamageEvent(physicalDamage.toInt(), target, player, false, false, this)
        Bukkit.getPluginManager().callEvent(damageEvent)
        if (!damageEvent.isCancelled) {
            target.damage(damageEvent.amount.toDouble(), player)
        }
        applySundered(player, target)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!hasSpellEffect(event.victim.uniqueId, SpellEffectType.SUNDERED)) return
        val highestStacks = determineHighestStacks(event.victim.uniqueId, SpellEffectType.SUNDERED)
        val percentAttribute = attributeBaseValue
        val bonusDamage = event.amount * (percentAttribute * highestStacks)
        event.amount = (event.amount + bonusDamage.toInt())
    }

    private fun applySundered(caster: Player, target: LivingEntity) {
        val existing = getSpellEffect(caster.uniqueId, target.uniqueId, SpellEffectType.SUNDERED)
        if (existing.isPresent) {
            (existing.get() as? SunderedEffect)?.increment()
        } else {
            SunderedEffect(
                    caster = caster,
                    recipient = target,
                    maxStacks = maxStacks.toInt(),
                    duration = stackDuration,
                    spellEffectAPI = deps.spellEffectAPI,
                )
                .apply {
                    increment()
                    initialize()
                }
        }
    }

    companion object {
        const val SPELL_NAME = "Cocoon"
        const val COOLDOWN = 8.0
        const val MANA_COST = 20
        const val BEAM_WIDTH = 1.0
        const val BASE_VALUE = 0.05
        const val MULTIPLIER = 0.01
        const val DURATION = 3.0
        const val PHYSICAL_DAMAGE = 20.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 1.0
        const val DISTANCE = 14.0
        const val DAMAGE_CAP = 500.0
        const val MAX_STACKS = 3.0
        const val STACK_DURATION = 20.0
    }
}
