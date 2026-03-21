package com.runicrealms.game.gameplay.player.death

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.entity.Dummy
import com.ticxo.modelengine.api.model.ModeledEntity
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancyholograms.api.hologram.Hologram
import java.util.UUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * A physical gravestone spawned at a player's death location that stores their dropped items.
 *
 * The gravestone uses a ModelEngine "boulder" model for its 3D appearance and a FancyHolograms text
 * hologram to display ownership and timer information. Loot priority is controlled by the
 * [priority] flag: when true, only the slain player (and eventually party members) may open it;
 * when false, anyone may loot.
 *
 * @param player the player who died
 * @param deathLocation where the player died
 * @param inventory the items to store (already filtered to droppable items)
 * @param priority true if the slain player has exclusive loot priority
 * @param priorityTime seconds before loot priority expires
 * @param duration seconds before the gravestone despawns entirely
 */
class Gravestone(
    player: Player,
    deathLocation: Location,
    val inventory: Inventory,
    priority: Boolean,
    val priorityTime: Int,
    val duration: Int,
) {
    val uuid: UUID = player.uniqueId
    val startTime: Long = System.currentTimeMillis()
    var priority: Boolean = priority
    private val playerName: String = player.name

    private val hologram: Hologram?
    private val modeledEntity: ModeledEntity?

    init {
        modeledEntity = spawnGravestone(deathLocation, player)
        hologram =
            if (modeledEntity != null) buildHologram(player, modeledEntity)
            else {
                logger.error(
                    "Gravestone model failed to spawn for player $uuid — skipping hologram"
                )
                null
            }
    }

    val location: Location?
        get() = modeledEntity?.base?.location

    /** The entity ID of the model base, used to match player interactions. */
    val baseEntityId: Int?
        get() = modeledEntity?.base?.entityId

    // -----------------------------------------------------------------------------------------
    // Spawn / teardown
    // -----------------------------------------------------------------------------------------

    /**
     * Spawns the ModelEngine "boulder" entity at a nearby valid block.
     *
     * TODO: ModelEngine 4.0.9 API may differ from ME3. [Dummy] and
     *   [ModelEngineAPI.createModeledEntity] signatures should be verified against the actual 4.0.9
     *   jar. If the API has changed, update this method accordingly.
     */
    private fun spawnGravestone(deathLocation: Location, player: Player): ModeledEntity? {
        val spawnLocation =
            findNearestValidBlock(deathLocation, 5)
                ?: run {
                    logger.error("Could not find valid block for gravestone of player $uuid")
                    player.sendMessage(
                        Component.text(
                            "Error: Your gravestone could not be placed. Please contact an admin.",
                            NamedTextColor.RED,
                        )
                    )
                    return null
                }

        // Center in the block
        val centred = spawnLocation.block.location.add(0.5, 0.0, 0.5)
        centred.world = spawnLocation.world

        val dummy = Dummy<Any>()
        dummy.location = centred

        val activeModel = ModelEngineAPI.createActiveModel(MODEL_ID)
        val modelEntity = ModelEngineAPI.createModeledEntity(dummy)

        if (activeModel != null) {
            activeModel.isHitboxVisible = true
            activeModel.setHitboxScale(HITBOX_SCALE)
            modelEntity.addModel(activeModel, true)
        }

        return modelEntity
    }

    private fun buildHologram(player: Player, model: ModeledEntity): Hologram? {
        return try {
            val hologramLocation = model.base.location.clone().add(0.0, 2.0, 0.0)
            val hologramData = TextHologramData("gravestone_${uuid}", hologramLocation)
            hologramData.isPersistent = false
            hologramData.text = buildHologramLines(priorityTime, duration)

            val manager = FancyHologramsPlugin.get().hologramManager
            val holo = manager.create(hologramData)
            manager.addHologram(holo)
            holo
        } catch (exception: Exception) {
            logger.error("Failed to create gravestone hologram for player $uuid", exception)
            null
        }
    }

    /**
     * Removes the gravestone from the world.
     *
     * @param dropItems if true, scatter the remaining inventory items on the ground
     */
    fun collapse(dropItems: Boolean) {
        // Remove hologram
        try {
            if (hologram != null) {
                FancyHologramsPlugin.get().hologramManager.removeHologram(hologram)
            }
        } catch (exception: Exception) {
            logger.error("Failed to remove gravestone hologram for player $uuid", exception)
        }

        // Remove model entity and play effects
        try {
            val loc = modeledEntity?.base?.location
            if (loc != null) {
                loc.world?.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, 0.5f, 0.2f)
                loc.world?.spawnParticle(Particle.CLOUD, loc, 25, 0.75, 1.0, 0.75, 0.0)
            }
            modeledEntity?.destroy()

            if (dropItems && loc != null) {
                for (item: ItemStack? in inventory.contents) {
                    if (item == null) continue
                    loc.world?.dropItem(loc, item)
                }
            }
        } catch (exception: Exception) {
            logger.error("Error despawning gravestone entity for player $uuid", exception)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Hologram updates
    // -----------------------------------------------------------------------------------------

    /**
     * Refreshes the hologram's timer lines with the given remaining seconds. Called by
     * [GravestoneManager] every second.
     */
    fun updateHologramText(remainingPriority: Int, remainingDuration: Int) {
        hologram ?: return
        try {
            val data = hologram.data as? TextHologramData ?: return
            data.text = buildHologramLines(remainingPriority.coerceAtLeast(0), remainingDuration)
            hologram.forceUpdate()
        } catch (exception: Exception) {
            logger.error("Failed to update gravestone hologram for player $uuid", exception)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    /**
     * Searches the column around [origin] for the nearest block whose type is [Material.AIR] or
     * [Material.WATER] (i.e. a block that a gravestone can be placed at).
     *
     * Scans from [origin.y - range] to [origin.y + range], returning the first suitable location.
     */
    private fun findNearestValidBlock(origin: Location, range: Int): Location? {
        val world = origin.world ?: return null
        // Check origin first, then scan outward: +1, -1, +2, -2, ...
        val candidate = origin.clone()
        if (world.getBlockAt(candidate).type.let { it == Material.AIR || it == Material.WATER }) {
            return candidate
        }
        for (dy in 1..range) {
            for (offset in intArrayOf(dy, -dy)) {
                val c = origin.clone().add(0.0, offset.toDouble(), 0.0)
                if (world.getBlockAt(c).type.let { it == Material.AIR || it == Material.WATER }) {
                    return c
                }
            }
        }
        return null
    }

    private fun buildHologramLines(remainingPriority: Int, remainingDuration: Int): List<String> {
        return listOf(
            "<red>$playerName's Gravestone</red>",
            "<yellow>Priority: ${formatTime(remainingPriority)}</yellow>",
            "<gray>Time left: ${formatTime(remainingDuration)}</gray>",
        )
    }

    private fun formatTime(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        return "%dm%ds".format(s / 60, s % 60)
    }

    companion object {
        private const val MODEL_ID = "boulder"
        private const val HITBOX_SCALE = 1.5
    }
}
