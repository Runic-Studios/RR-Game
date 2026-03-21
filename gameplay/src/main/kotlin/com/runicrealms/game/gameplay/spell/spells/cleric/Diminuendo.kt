package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.HorizontalCircleFrame
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import java.util.UUID
import kotlin.math.max
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask

class Diminuendo(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), DurationSpell, RadiusSpell {
    override var duration = BASE_DURATION
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Place a jukebox for $duration seconds. Enemies inside $radius blocks attack slower and mobs deal reduced damage."

    var attackSpeedReduction = BASE_ATTACK_SPEED_REDUCTION
    var mobDamageReduction = BASE_MOB_DAMAGE_REDUCTION

    private val affectedEnemiesMap: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf()
    private val activeTasks: MutableMap<UUID, BukkitTask> = mutableMapOf()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val location = player.location
        val center =
            Location(
                location.world,
                location.blockX + 0.5,
                location.blockY.toDouble(),
                location.blockZ + 0.5,
            )
        val world = center.world ?: return
        val oldMaterial = center.block.type
        center.block.setType(Material.JUKEBOX, false)

        val hologramName = "diminuendo_${player.uniqueId}_${System.currentTimeMillis()}"
        val hologramData = TextHologramData(hologramName, center.clone().add(0.5, 2.5, 0.5))
        hologramData.isPersistent = false
        hologramData.text = listOf("${player.name}'s Jukebox")
        val manager = FancyHologramsPlugin.get().hologramManager
        val hologram = manager.create(hologramData)
        manager.addHologram(hologram)

        var count = 0
        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    affectedEnemiesMap.remove(player.uniqueId)
                    if (count >= duration.toInt()) {
                        task.cancel()
                        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.0f)
                        world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f)
                        manager.removeHologram(hologram)
                        center.block.type = oldMaterial
                        activeTasks.remove(player.uniqueId)
                        return@Runnable
                    }
                    count += 1
                    world.playSound(center, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.0f)
                    HorizontalCircleFrame(radius, false)
                        .playParticle(player, Particle.CRIT, center, 0.3, Color.GREEN)
                    val affected = mutableSetOf<UUID>()
                    for (entity in world.getNearbyEntities(center, radius, radius, radius)) {
                        if (isValidEnemy(player, entity)) {
                            affected.add(entity.uniqueId)
                        }
                    }
                    affectedEnemiesMap[player.uniqueId] = affected
                },
                0L,
                20L,
            )
        activeTasks[player.uniqueId] = task

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable {
                manager.removeHologram(hologram)
                if (center.block.type == Material.JUKEBOX) {
                    center.block.type = oldMaterial
                }
            },
            max(1L, (duration * 20).toLong()),
        )
    }

    @EventHandler
    fun onBasicAttack(event: BasicAttackEvent) {
        if (!isAffected(event.player.uniqueId)) return
        val addedTicks = event.originalCooldownTicks * attackSpeedReduction
        event.cooldownTicks += addedTicks
    }

    @EventHandler
    fun onMobDamage(event: MobDamageEvent) {
        if (!isAffected(event.mob.uniqueId)) return
        val reduction = (event.amount * mobDamageReduction).toInt()
        event.amount = (event.amount - reduction).coerceAtLeast(0)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        activeTasks.remove(event.player.uniqueId)?.cancel()
        affectedEnemiesMap.remove(event.player.uniqueId)
    }

    private fun isAffected(entityId: UUID): Boolean {
        return affectedEnemiesMap.values.any { it.contains(entityId) }
    }

    companion object {
        const val SPELL_NAME = "Diminuendo"
        private const val COOLDOWN = 12.0
        private const val MANA_COST = 25
        private const val BASE_DURATION = 8.0
        private const val BASE_RADIUS = 7.0
        private const val BASE_ATTACK_SPEED_REDUCTION = 0.3
        private const val BASE_MOB_DAMAGE_REDUCTION = 0.2
    }
}
