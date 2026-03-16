package com.runicrealms.game.gameplay.spell.spelltypes

import org.bukkit.entity.Player

/** Bundles all data about a shielded player. */
data class ShieldPayload(val player: Player, val source: Player, val shield: Shield)
