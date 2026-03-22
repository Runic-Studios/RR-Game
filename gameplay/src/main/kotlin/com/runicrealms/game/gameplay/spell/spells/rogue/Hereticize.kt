package com.runicrealms.game.gameplay.spell.spells.rogue

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive: branded enemies trigger cooldown reduction when casting, and take extra magic damage
 * when hit by the passive owner's basic attacks.
 */
class Hereticize(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ROGUE, deps), DurationSpell, MagicDamageSpell {
    override var cooldown = 0.0
    override var manaCost = 0
    override var duration = DURATION
    override var magicDamage = MAGIC_DAMAGE
    override var magicDamagePerLevel = MAGIC_DAMAGE_PER_LEVEL
    override val description: String
        get() =
            "Anytime a &7&obranded &7enemy uses an ability, your active ability cooldowns are " +
                "reduced by ${duration}s. &7&oBranded &7enemies also take an additional " +
                "($magicDamage + &f${magicDamagePerLevel}x&7 lvl) magicʔ damage when hit by your basic attacks."

    // Internal map-based state (SPELL_MIGRATION.md): caster -> branded victim
    private val brandedEnemies: ConcurrentHashMap<UUID, UUID> = SilverBolt.getBrandedEnemiesMap()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onCast(event: SpellCastEvent) {
        if (brandedEnemies.isEmpty()) return

        for ((witchHunterUuid, brandedUuid) in brandedEnemies) {
            if (brandedUuid != event.caster.uniqueId) continue

            val witchHunter = Bukkit.getPlayer(witchHunterUuid) ?: continue
            if (!hasPassive(witchHunter.uniqueId, name)) continue
            reduceKnownCooldowns(witchHunter)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        if (brandedEnemies.isEmpty()) return
        if (!hasPassive(event.caster.uniqueId, name)) return
        applyHereticDamage(event.victim)
    }

    private fun applyHereticDamage(victim: LivingEntity) {
        for ((casterUuid, brandedUuid) in brandedEnemies) {
            if (brandedUuid != victim.uniqueId) continue
            val caster = Bukkit.getPlayer(casterUuid) ?: continue
            if (!hasPassive(caster.uniqueId, name)) continue
            victim.world.playSound(victim.location, Sound.ENTITY_WITCH_HURT, 0.25f, 2.0f)
            val damageEvent = MagicDamageEvent(magicDamage.toInt(), victim, caster, this)
            Bukkit.getPluginManager().callEvent(damageEvent)
            if (!damageEvent.isCancelled) {
                victim.damage(damageEvent.amount.toDouble(), caster)
            }
        }
    }

    private fun reduceKnownCooldowns(player: Player) {
        // SpellManagerBridge does not expose cooldown iteration yet, so reduce known active rogue
        // cooldowns.
        for (spellName in COOLDOWN_TARGETS) {
            if (spellManager.isOnCooldown(player, spellName)) {
                spellManager.reduceCooldown(player, spellName, duration)
            }
        }
    }

    companion object {
        const val SPELL_NAME = "Hereticize"
        const val DURATION = 1.0
        const val MAGIC_DAMAGE = 12.0
        const val MAGIC_DAMAGE_PER_LEVEL = 0.5

        private val COOLDOWN_TARGETS =
            listOf(
                "Harpoon",
                "Dash",
                "Whirlpool",
                "Silver Bolt",
                "Twin Fangs",
                "Cocoon",
                "Flay",
                "Cannonfire",
                "Unseen",
                "Warding Glyph",
            )
    }
}
