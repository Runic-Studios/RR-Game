package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.scheduler.BukkitTask

/**
 * Places a temporary bell that charges holy power over time. Allies can ring it to trigger AoE
 * damage + shielding, and optional ally teleport.
 */
class Salvation(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps),
    DistanceSpell,
    DurationSpell,
    MagicDamageSpell,
    RadiusSpell,
    ShieldingSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var distance = DISTANCE
    override var duration = DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = RADIUS
    override var shieldAmount = BASE_SHIELD
    override var shieldPerLevel = SHIELD_PER_LEVEL
    override var description =
        "Conjure a bell up to $distance blocks away. It charges for ${duration}s. " +
            "An ally can right-click the bell to detonate it, dealing AoE magic damage and granting shields."

    private val blockMap: MutableMap<Block, BellTask> = HashMap()
    private var maxDistance = MAX_DISTANCE

    override fun executeSpell(player: Player, type: SpellItemType) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f)
        val targetLocation =
            player.getTargetBlockExact(distance.toInt())?.location
                ?: player.location.clone().add(player.location.direction.multiply(distance))
        val adjusted = targetLocation.clone()
        if (adjusted.block.type != Material.AIR) {
            adjusted.add(0.0, 1.0, 0.0)
        }
        VectorUtil.drawLine(player, Color.WHITE, player.eyeLocation, adjusted, 0.5)
        player.world.spawnParticle(Particle.HAPPY_VILLAGER, adjusted, 8, 0.5, 0.5, 0.5, 0.0)
        spawnBell(player, adjusted)
    }

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        maxDistance = config.getDouble("max-distance", maxDistance)
    }

    @EventHandler
    fun onBellRung(event: PlayerInteractEvent) {
        if (blockMap.isEmpty()) return
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clicked = event.clickedBlock ?: return
        if (clicked.type != Material.BELL) return

        val bellTask = blockMap[clicked] ?: return
        val caster = Bukkit.getPlayer(bellTask.casterUUID) ?: return
        val clicker = event.player
        if (!isValidAlly(caster, clicker)) return

        clicker.world.playSound(clicked.location, Sound.BLOCK_BELL_USE, 0.5f, 2.0f)
        val count = bellTask.count.get()
        bellTask.task.cancel()
        bellTask.cleanup()
        blockMap.remove(clicked)

        clicked.world.playSound(clicked.location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 2.0f)
        clicked.world.spawnParticle(Particle.FIREWORK, clicked.location, 15, 2.0, 2.0, 2.0, 0.0)

        for (entity in
            clicked.world.getNearbyEntities(clicked.location, radius, radius, radius) { target ->
                isValidEnemy(caster, target)
            }) {
            val victim = entity as? LivingEntity ?: continue
            val damageEvent = MagicDamageEvent((magicDamage * count).toInt(), victim, caster, this)
            Bukkit.getPluginManager().callEvent(damageEvent)
            if (!damageEvent.isCancelled) {
                victim.damage(damageEvent.amount.toDouble(), caster)
            }
        }

        shieldPlayer(caster, clicker, shieldAmount * count, this)

        if (clicker.uniqueId != bellTask.casterUUID) {
            var canTeleport = false
            if (clicker.world == caster.world) {
                val distanceSquared = clicker.location.distanceSquared(caster.location)
                if (distanceSquared <= (maxDistance * maxDistance)) {
                    canTeleport = true
                }
            }
            if (canTeleport) {
                clicker.teleport(caster)
                clicker.world.playSound(
                    clicker.location,
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    0.5f,
                    1.0f,
                )
            } else {
                clicker.sendMessage(
                    Component.text(
                        "Could not teleport you to the caster! Target is too far away!",
                        NamedTextColor.RED,
                    )
                )
                clicker.world.playSound(clicker.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f)
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val quitterId = event.player.uniqueId
        val entries = blockMap.entries.filter { it.value.casterUUID == quitterId }
        entries.forEach { (block, bellTask) ->
            bellTask.task.cancel()
            bellTask.cleanup()
            blockMap.remove(block)
        }
    }

    private fun spawnBell(caster: Player, location: Location) {
        val bestLocation =
            findNearestValidBellLocation(location, 3)
                ?: run {
                    caster.sendMessage(
                        Component.text("A valid location could not be found!", NamedTextColor.RED)
                    )
                    caster.playSound(
                        caster.location,
                        Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
                        0.5f,
                        1.0f,
                    )
                    return
                }
        val oldMaterial = bestLocation.block.type
        bestLocation.block.setType(Material.BELL, false)

        val hologramName = "salvation_bell_${caster.uniqueId}_${System.nanoTime()}"
        val hologramData = TextHologramData(hologramName, bestLocation.clone().add(0.5, 2.5, 0.5))
        hologramData.text =
            listOf(
                "<white>${caster.name}'s </white><gray>Bell</gray>",
                "<green><bold>CLICK ME!</bold></green>",
                determineHologramString(0),
            )
        val manager = FancyHologramsPlugin.get().hologramManager
        val hologram = manager.create(hologramData)
        manager.addHologram(hologram)

        caster.world.playSound(location, Sound.BLOCK_BELL_USE, 0.5f, 1.0f)
        val bellTask =
            BellTask(caster.uniqueId, hologramName, bestLocation, oldMaterial, duration, deps)
        blockMap[bestLocation.block] = bellTask
    }

    private fun findNearestValidBellLocation(origin: Location, searchRadius: Int): Location? {
        val world = origin.world ?: return null
        val base = origin.block.location
        for (yOffset in -1..1) {
            for (radiusStep in 0..searchRadius) {
                for (x in -radiusStep..radiusStep) {
                    for (z in -radiusStep..radiusStep) {
                        val candidate =
                            base.clone().add(x.toDouble(), yOffset.toDouble(), z.toDouble())
                        val block = world.getBlockAt(candidate)
                        val below = world.getBlockAt(candidate.clone().add(0.0, -1.0, 0.0))
                        if (
                            (block.type == Material.AIR || block.type == Material.WATER) &&
                                below.type.isSolid
                        ) {
                            return candidate
                        }
                    }
                }
            }
        }
        return null
    }

    companion object {
        const val SPELL_NAME = "Salvation"
        const val COOLDOWN = 20.0
        const val MANA_COST = 45
        const val DISTANCE = 10.0
        const val DURATION = 4.0
        const val MAX_DISTANCE = 30.0
        const val BASE_DAMAGE = 10.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val RADIUS = 5.0
        const val BASE_SHIELD = 10.0
        const val SHIELD_PER_LEVEL = 1.0

        private fun determineHologramString(count: Int): String {
            return when (count) {
                2 -> "<gold>★★</gold><white>☆☆</white>"
                3 -> "<gold>★★★</gold><white>☆</white>"
                4 -> "<gold>★★★★</gold>"
                else -> "<gold>★</gold><white>☆☆☆</white>"
            }
        }
    }

    private class BellTask(
        val casterUUID: UUID,
        private val hologramName: String,
        private val location: Location,
        private val oldMaterial: Material,
        duration: Double,
        deps: SpellDependencies,
    ) {
        val count = AtomicInteger(1)
        val task: BukkitTask

        init {
            lateinit var repeatingTask: BukkitTask
            repeatingTask =
                deps.plugin.server.scheduler.runTaskTimer(
                    deps.plugin,
                    Runnable {
                        if (count.get() >= duration.toInt()) {
                            repeatingTask.cancel()
                            cleanup()
                        } else {
                            count.incrementAndGet()
                            val manager = FancyHologramsPlugin.get().hologramManager
                            val hologram = manager.getHologram(hologramName).orElse(null)
                            val data = hologram?.data as? TextHologramData
                            if (data != null) {
                                data.text =
                                    listOf(
                                        data.text.getOrNull(0) ?: "",
                                        data.text.getOrNull(1) ?: "",
                                        determineHologramString(count.get()),
                                    )
                                hologram.forceUpdate()
                            }
                        }
                    },
                    20L,
                    20L,
                )
            task = repeatingTask
        }

        fun cleanup() {
            val manager = FancyHologramsPlugin.get().hologramManager
            manager.getHologram(hologramName).ifPresent { manager.removeHologram(it) }
            location.block.setType(oldMaterial, false)
        }
    }
}
