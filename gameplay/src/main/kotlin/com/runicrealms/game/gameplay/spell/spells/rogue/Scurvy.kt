package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import java.util.HashSet
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/** Passive: first basic attack after Dash applies a disease debuff. */
class Scurvy(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell, PhysicalDamageSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var duration = DURATION
    override var physicalDamage = PHYSICAL_DAMAGE
    override var physicalDamagePerLevel = PHYSICAL_DAMAGE_PER_LEVEL
    override val description: String
        get() =
            "Your first basic attack after casting &aDash &7is laden with disease. " +
                "The target receives nausea for ${duration}s. Against mobs, you deal " +
                "($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) extra physical⚔ damage."

    private val buffed: MutableSet<UUID> = HashSet()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!buffed.contains(event.caster.uniqueId) || !event.isBasicAttack) return

        Bukkit.getPluginManager().callEvent(DebuffEvent(event.caster, event.victim))
        if (event.victim is Player) {
            event.victim.addPotionEffect(
                PotionEffect(
                    PotionEffectType.NAUSEA,
                    (duration * 20).toInt(),
                    3,
                    false,
                    false,
                    false,
                )
            )
            return
        }

        val bonus = physicalDamage + (physicalDamagePerLevel * event.caster.level)
        event.amount += bonus.toInt()
        buffed.remove(event.caster.uniqueId)
        event.caster.world.playSound(event.victim.location, Sound.ENTITY_DROWNED_HURT, 0.5f, 1.0f)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        if (!hasPassive(event.caster.uniqueId, name)) return
        if (event.spell !is Dash) return
        buffed.add(event.caster.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        buffed.remove(event.player.uniqueId)
    }

    companion object {
        const val SPELL_NAME = "Scurvy"
        const val DURATION = 3.0
        const val PHYSICAL_DAMAGE = 12.0
        const val PHYSICAL_DAMAGE_PER_LEVEL = 0.5

        class DebuffEvent(val caster: Player, val victim: LivingEntity) : Event() {
            override fun getHandlers(): HandlerList = handlerList

            companion object {
                @JvmStatic val handlerList = HandlerList()
            }
        }
    }
}
