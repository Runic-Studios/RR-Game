package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.ShieldBreakEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class TwilightResurgence(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, MagicDamageSpell, RadiusSpell {
    override var duration = BASE_COOLDOWN_REDUCTION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "When your shield breaks from damage, reduce Cosmic Prism cooldown and release a blind pulse."

    var blindDuration = BASE_BLIND_DURATION
    var effectCooldown = BASE_EFFECT_COOLDOWN
    private val cooldownPlayerSet: MutableSet<UUID> = mutableSetOf()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive - handled via shield break events.
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onShieldBreak(event: ShieldBreakEvent) {
        if (event.breakReason != ShieldBreakEvent.BreakReason.DAMAGE) return
        val payload = event.shieldPayload
        if (cooldownPlayerSet.contains(payload.source.uniqueId)) return
        if (!hasPassive(payload.source.uniqueId, name)) return

        cooldownPlayerSet.add(payload.source.uniqueId)
        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { cooldownPlayerSet.remove(payload.source.uniqueId) },
            (effectCooldown * 20).toLong(),
        )

        spellManager.reduceCooldown(payload.source, CosmicPrism.SPELL_NAME, duration)

        val centerPlayer = payload.player
        for (entity in
            centerPlayer.world.getNearbyEntities(centerPlayer.location, radius, radius, radius)) {
            val target = entity as? LivingEntity ?: continue
            if (!isValidEnemy(centerPlayer, target)) continue
            target.world.playSound(target.location, Sound.ENTITY_EVOKER_CAST_SPELL, 0.5f, 1.0f)
            val eventDamage = MagicDamageEvent(magicDamage.toInt(), target, payload.source, this)
            Bukkit.getPluginManager().callEvent(eventDamage)
            if (!eventDamage.isCancelled) {
                target.damage(eventDamage.amount.toDouble(), payload.source)
            }
            target.addPotionEffect(
                PotionEffect(PotionEffectType.BLINDNESS, (blindDuration * 20).toInt(), 2)
            )
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cooldownPlayerSet.remove(event.player.uniqueId)
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        blindDuration = config.getDouble("blind-duration", blindDuration)
        duration = config.getDouble("duration", duration)
        effectCooldown = config.getDouble("effect-cooldown", effectCooldown)
    }

    companion object {
        const val SPELL_NAME = "Twilight Resurgence"
        private const val BASE_COOLDOWN_REDUCTION = 3.0
        private const val BASE_DAMAGE = 12.0
        private const val DAMAGE_PER_LEVEL = 0.3
        private const val BASE_RADIUS = 5.0
        private const val BASE_BLIND_DURATION = 2.0
        private const val BASE_EFFECT_COOLDOWN = 6.0
    }
}
