package com.runicrealms.game.gameplay.spell.spelltypes

import org.bukkit.Location

/** Metadata attached to a spell-spawned projectile entity. */
data class EntityData(val firedFrom: Location, val range: Int, val damage: Double)
