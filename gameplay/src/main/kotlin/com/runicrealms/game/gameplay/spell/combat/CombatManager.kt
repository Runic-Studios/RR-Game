package com.runicrealms.game.gameplay.spell.combat

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.CombatType
import com.runicrealms.game.gameplay.spell.event.EnterCombatEvent
import com.runicrealms.game.gameplay.spell.event.LeaveCombatEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

@Singleton
class CombatManager @Inject constructor(private val plugin: Plugin) {

    private val combatMap: ConcurrentHashMap<UUID, CombatEntry> = ConcurrentHashMap()

    init {
        plugin.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val expired =
                    combatMap.entries
                        .filter { (_, entry) ->
                            now >= entry.lastRefreshMs + (entry.combatType.durationSeconds * 1000L)
                        }
                        .map { it.key }
                for (uuid in expired) {
                    leaveCombat(uuid)
                }
            }
        }
    }

    fun enterCombat(uuid: UUID, combatType: CombatType) {
        val player = Bukkit.getPlayer(uuid) ?: return
        val current = combatMap[uuid]
        val nextType =
            when {
                current == null -> combatType
                current.combatType == CombatType.PVP -> CombatType.PVP
                combatType == CombatType.PVP -> CombatType.PVP
                else -> CombatType.PVE
            }

        if (current == null) {
            val enterEvent = EnterCombatEvent(player, nextType)
            Bukkit.getPluginManager().callEvent(enterEvent)
            if (enterEvent.isCancelled) return
        }
        combatMap[uuid] = CombatEntry(nextType, System.currentTimeMillis())
    }

    fun leaveCombat(uuid: UUID) {
        combatMap.remove(uuid) ?: return
        val player = Bukkit.getPlayer(uuid) ?: return
        Bukkit.getPluginManager().callEvent(LeaveCombatEvent(player))
    }

    fun isInCombat(uuid: UUID): Boolean = combatMap.containsKey(uuid)

    fun getCombatType(uuid: UUID): CombatType? = combatMap[uuid]?.combatType

    private data class CombatEntry(val combatType: CombatType, val lastRefreshMs: Long)
}
