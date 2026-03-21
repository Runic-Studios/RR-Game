package com.runicrealms.game.gameplay.spell.spellutil

import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/** Static vector and line-drawing utilities for spell effects. */
object VectorUtil {

    /** Rotates [vector] around the Y axis by [degrees]. */
    fun rotateVectorAroundY(vector: Vector, degrees: Double): Vector {
        val clone = vector.clone()
        val rad = Math.toRadians(degrees)
        val cosA = cos(rad)
        val sinA = sin(rad)
        val x = vector.x
        val z = vector.z
        clone.x = cosA * x - sinA * z
        clone.z = sinA * x + cosA * z
        return clone
    }

    /**
     * Draws a line of [particle] from [start] to [end].
     *
     * @param spacing distance between each particle point
     */
    fun drawLine(
        player: Player,
        particle: Particle,
        start: Location,
        end: Location,
        spacing: Double,
    ) {
        val direction = end.toVector().subtract(start.toVector())
        val length = direction.length()
        if (length == 0.0) return
        direction.normalize().multiply(spacing)
        val current = start.clone()
        var travelled = 0.0
        while (travelled < length) {
            player.world.spawnParticle(particle, current, 1, 0.0, 0.0, 0.0, 0.0)
            current.add(direction)
            travelled += spacing
        }
    }

    /** Draws a coloured REDSTONE line from [start] to [end]. */
    fun drawLine(player: Player, color: Color, start: Location, end: Location, spacing: Double) {
        val direction = end.toVector().subtract(start.toVector())
        val length = direction.length()
        if (length == 0.0) return
        direction.normalize().multiply(spacing)
        val current = start.clone()
        var travelled = 0.0
        val dustOptions = Particle.DustOptions(color, 1.0f)
        while (travelled < length) {
            player.world.spawnParticle(Particle.DUST, current, 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
            current.add(direction)
            travelled += spacing
        }
    }

    /** Draws a BLOCK_CRACK line of [material] from [start] to [end]. */
    fun drawLine(
        player: Player,
        material: Material,
        start: Location,
        end: Location,
        spacing: Double,
    ) {
        val direction = end.toVector().subtract(start.toVector())
        val length = direction.length()
        if (length == 0.0) return
        direction.normalize().multiply(spacing)
        val current = start.clone()
        var travelled = 0.0
        val blockData = material.createBlockData()
        while (travelled < length) {
            player.world.spawnParticle(
                Particle.BLOCK_CRUMBLE,
                current,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                blockData,
            )
            current.add(direction)
            travelled += spacing
        }
    }
}
