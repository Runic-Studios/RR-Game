package com.runicrealms.game.gameplay.spell.spellutil

import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.PolarBear
import org.bukkit.entity.Wolf

/**
 * Generates threat towards a player from nearby hostile mobs. Uses MythicMobs 5.6.1 API for
 * MythicMob threat tracking; falls back to vanilla targeting for non-MythicMob entities.
 */
object ThreatUtil {

    /**
     * Forces [target] (if it is a Monster, Wolf, or PolarBear) to aggro [player]. For MythicMob
     * entities, also adds threat via the MythicMobs API.
     */
    fun generateThreat(player: Player, target: Entity, amount: Double = 100.0) {
        if (target !is Monster && target !is Wolf && target !is PolarBear) return

        // MythicMobs 5.6.1 API
        MythicBukkit.inst().mobManager.getActiveMob(target.uniqueId).ifPresent { activeMob ->
            MythicBukkit.inst().apiHelper.addThreat(activeMob.entity.bukkitEntity, player, amount)
        }

        // Vanilla targeting fallback
        if (target is Monster) {
            target.target = player
        }
    }
}
