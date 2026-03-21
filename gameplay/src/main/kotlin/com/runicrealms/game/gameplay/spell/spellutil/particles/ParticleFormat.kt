package com.runicrealms.game.gameplay.spell.spellutil.particles

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player

/** Functional interface for particle shape generators. */
fun interface ParticleFormat {
    /**
     * Plays a particle effect.
     *
     * @param color Optional color for REDSTONE/DUST particles.
     */
    fun playParticle(
        player: Player,
        particle: Particle,
        location: Location,
        particleSpacing: Double,
        vararg color: Color,
    )
}
