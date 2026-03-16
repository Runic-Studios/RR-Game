package com.runicrealms.game.gameplay.spell.spells.archer

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.data.Bisected
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SacredGrove(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.ARCHER, deps),
    DurationSpell,
    HealingSpell,
    RadiusSpell,
    WarmupSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = DURATION
    override var healAmount = HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var radius = RADIUS
    override var warmupSeconds = WARMUP
    override var description =
        "Place an enchanted flower that grows for ${warmupSeconds}s, then heals allies " +
            "within ${radius} blocks for (${healAmount} + ${healPerLevel}x lvl) each second for ${duration}s."

    override fun executeSpell(player: Player, type: SpellItemType) {
        val location = player.location.block.location.clone().add(0.5, 0.0, 0.5)
        groveLocationMap[player.uniqueId] = location
        spawnFlower(player, location)

        var count = 0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count >= warmupSeconds) {
                    task.cancel()
                    player.world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.0f)
                    player.world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f)
                    createHealingRunnable(player, location)
                    return@runTaskTimer
                }

                count += 1
                player.world.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.0f)
                HorizontalCircleFrame(radius - warmupSeconds + count, false)
                    .playParticle(player, Particle.CRIT, location, 10.0, Color.GREEN)
            },
            0L,
            20L,
        )
    }

    private fun createHealingRunnable(player: Player, location: Location) {
        var count = 0
        deps.plugin.server.scheduler.runTaskTimer(
            deps.plugin,
            { task ->
                if (count >= duration) {
                    Bukkit.getPluginManager().callEvent(GroveExpiryEvent(player))
                    task.cancel()
                    groveLocationMap.remove(player.uniqueId)
                    return@runTaskTimer
                }
                count += 1
                HorizontalCircleFrame(radius, false)
                    .playParticle(player, Particle.HAPPY_VILLAGER, location, 5.0, Color.GREEN)
                for (entity in
                    player.world.getNearbyEntities(location, radius, radius, radius) {
                        isValidAlly(player, it)
                    }) {
                    val ally = entity as? Player ?: continue
                    healPlayer(player, ally, healAmount, this)
                }
            },
            0L,
            20L,
        )
    }

    private fun spawnFlower(player: Player, location: Location) {
        val oldMaterial = location.block.type
        location.block.type = Material.DANDELION

        val hologramName = "sacred_grove_${player.uniqueId}_${System.currentTimeMillis()}"
        val hologramData = TextHologramData(hologramName, location.clone().add(0.5, 2.5, 0.5))
        hologramData.text = listOf("${player.name}'s Seedling")
        hologramData.isPersistent = false

        val manager = FancyHologramsPlugin.get().hologramManager
        val hologram = manager.create(hologramData)
        manager.addHologram(hologram)

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                (hologram.data as? TextHologramData)?.text = listOf("${player.name}'s Wild Growth")
                hologram.forceUpdate()

                location.block.type = Material.SUNFLOWER
                val bottom = location.block.blockData as? Bisected
                bottom?.half = Bisected.Half.BOTTOM
                if (bottom != null) location.block.blockData = bottom

                val higher = location.clone().add(0.0, 1.0, 0.0)
                val oldMaterialHigher = higher.block.type
                higher.block.type = Material.SUNFLOWER
                val top = higher.block.blockData as? Bisected
                top?.half = Bisected.Half.TOP
                if (top != null) higher.block.blockData = top

                deps.plugin.server.scheduler.runTaskLater(
                    deps.plugin,
                    Runnable {
                        manager.removeHologram(hologram)
                        location.block.type = oldMaterial
                        higher.block.type = oldMaterialHigher
                    },
                    (duration * 20.0).toLong(),
                )
            },
            (warmupSeconds * 20.0).toLong(),
        )
    }

    companion object {
        const val SPELL_NAME = "Sacred Grove"
        const val COOLDOWN = 22.0
        const val MANA_COST = 45
        const val DURATION = 6.0
        const val HEAL = 10.0
        const val HEAL_PER_LEVEL = 1.0
        const val RADIUS = 6.0
        const val WARMUP = 3.0

        val groveLocationMap: MutableMap<UUID, Location> = HashMap()

        class GroveExpiryEvent(val caster: Player) : Event(), Cancellable {
            private var cancelled = false

            override fun isCancelled(): Boolean = cancelled

            override fun setCancelled(cancel: Boolean) {
                cancelled = cancel
            }

            override fun getHandlers(): HandlerList = handlerList

            companion object {
                @JvmStatic val handlerList = HandlerList()
            }
        }
    }
}
