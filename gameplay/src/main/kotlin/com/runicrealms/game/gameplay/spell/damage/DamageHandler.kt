package com.runicrealms.game.gameplay.spell.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@Singleton
class DamageHandler @Inject constructor(private val plugin: Plugin) {

    /**
     * True while [dealMagicDamage] or [dealPhysicalDamage] is executing [LivingEntity.damage].
     * Bukkit re-fires [EntityDamageByEntityEvent] during that call; listeners check this flag
     * to avoid intercepting the re-trigger.
     */
    var isDealing: Boolean = false
        private set

    fun dealMagicDamage(
        amount: Int,
        victim: LivingEntity,
        caster: Player,
        spell: Spell? = null,
    ): Int {
        val event = MagicDamageEvent(amount, victim, caster, spell)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return 0

        val finalAmount =
            if (event.isCritical) (event.amount * CRITICAL_MULTIPLIER).toInt() else event.amount
        victim.noDamageTicks = 0
        isDealing = true
        victim.damage(finalAmount.toDouble(), caster)
        isDealing = false
        spawnDamageHologram(victim.location, finalAmount, event.isCritical, MAGIC_COLOR_TAG)
        return finalAmount
    }

    fun dealPhysicalDamage(
        amount: Int,
        victim: LivingEntity,
        caster: Player,
        isBasicAttack: Boolean = false,
        isRanged: Boolean = false,
        spell: Spell? = null,
    ): Int {
        val event = PhysicalDamageEvent(amount, victim, caster, isBasicAttack, isRanged, spell)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return 0

        val finalAmount =
            if (event.isCritical) (event.amount * CRITICAL_MULTIPLIER).toInt() else event.amount
        victim.noDamageTicks = 0
        isDealing = true
        victim.damage(finalAmount.toDouble(), caster)
        isDealing = false
        spawnDamageHologram(victim.location, finalAmount, event.isCritical, PHYSICAL_COLOR_TAG)
        return finalAmount
    }

    private fun spawnDamageHologram(
        location: Location,
        amount: Int,
        isCritical: Boolean,
        normalColorTag: String,
    ) {
        val hologramName = "dmg_${System.nanoTime()}"
        val hologramData = TextHologramData(hologramName, location.clone().add(0.0, 1.4, 0.0))
        hologramData.isPersistent = false
        val color = if (isCritical) "<gold>" else "<$normalColorTag>"
        hologramData.text = listOf("$color-$amount")

        val hologramManager = FancyHologramsPlugin.get().hologramManager
        val hologram = hologramManager.create(hologramData)
        hologramManager.addHologram(hologram)

        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable { hologramManager.removeHologram(hologram) },
            HOLOGRAM_DURATION_TICKS,
        )
    }

    private companion object {
        const val CRITICAL_MULTIPLIER = 1.5
        const val HOLOGRAM_DURATION_TICKS = 20L
        const val MAGIC_COLOR_TAG = "dark_aqua"
        const val PHYSICAL_COLOR_TAG = "red"
    }
}
