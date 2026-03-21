package com.runicrealms.game.gameplay.spell.effect

import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancyholograms.api.hologram.Hologram
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * A temporary, non-persistent hologram attached to a [StackEffect].
 *
 * Uses FancyHolograms 2.9.1. The hologram is [persistent = false], so it is never saved to disk. It
 * must be removed explicitly in [SpellEffect.onExpire] via [remove].
 *
 * Hologram name is unique per caster+recipient UUID pair. Name format:
 * "stacks_<effectType>_<casterUuid>_<recipientUuid>"
 */
class StackHologram(
    val effectType: SpellEffectType,
    val initialLocation: Location,
    val hologramName: String,
    val playersToShowTo: Set<Player> = mutableSetOf(),
) {

    private lateinit var hologram: Hologram

    init {
        try {
            val hologramData =
                TextHologramData(hologramName, initialLocation.clone().add(0.0, 1.5, 0.0))
            hologramData.text = listOf(buildDisplayText(0))
            hologramData.billboard = Display.Billboard.CENTER

            val manager = FancyHologramsPlugin.get().hologramManager
            hologram = manager.create(hologramData)
            manager.addHologram(hologram)
        } catch (exception: Exception) {
            logger.error("Failed to create StackHologram '$hologramName'", exception)
        }
    }

    /** Updates the displayed stack count and repositions the hologram to [location]. */
    fun showHologram(location: Location, stacks: Int) {
        try {
            val manager = FancyHologramsPlugin.get().hologramManager
            val hologram = manager.getHologram(hologramName).orElse(null) ?: return
            val data = hologram.data as? TextHologramData ?: return
            data.text = listOf(buildDisplayText(stacks))
            hologram.data.setLocation(location.clone().add(0.0, 1.5, 0.0))
            playersToShowTo.forEach { hologram.showHologram(it) }
            hologram.forceUpdate()
        } catch (exception: Exception) {
            logger.error("Failed to update StackHologram '$hologramName'", exception)
        }
    }

    /** Removes the hologram from the world. Call this in [SpellEffect.onExpire]. */
    fun remove() {
        try {
            FancyHologramsPlugin.get().hologramManager.removeHologram(hologram)
        } catch (exception: Exception) {
            logger.error("Failed to remove StackHologram '$hologramName'", exception)
        }
    }

    private fun buildDisplayText(stacks: Int): String =
        PlainTextComponentSerializer.plainText()
            .serialize(
                Component.text()
                    .append(Component.text(effectType.displayName, Style.style(effectType.colour)))
                    .append(Component.text(" ${effectType.symbol} x$stacks"))
                    .build()
            )
}
