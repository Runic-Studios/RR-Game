package com.runicrealms.game.gameplay.player.stat

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.StatType
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeData
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreePosition
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * Manages per-player base stats derived from skill tree [PerkBaseStat] perks. The total stat value
 * is base (skill tree) + item bonus (from [CharacterEquipmentCacheRegistry]).
 *
 * Stat multipliers are applied to game events by [StatListener].
 *
 * Base stats are loaded asynchronously on [GameCharacterLoadEvent] by reconstructing perk lists
 * directly from [SkillTreeData] (independent of [com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager]'s
 * own async load, avoiding ordering and circular-dependency issues).
 */
@Singleton
class StatManager
@Inject
constructor(
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val equipmentCacheRegistry: CharacterEquipmentCacheRegistry,
) : Listener {

    /** UUID -> (StatType -> base value from skill tree perks) */
    private val baseStatMap: ConcurrentHashMap<UUID, ConcurrentHashMap<StatType, Int>> =
        ConcurrentHashMap()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    /**
     * Returns the total stat value for the given player: skill tree base + item bonus. Returns 0
     * if the player is not currently loaded.
     */
    fun getStat(uuid: UUID, stat: StatType): Int {
        val base = baseStatMap[uuid]?.get(stat) ?: 0
        val itemBonus =
            equipmentCacheRegistry.cachedCharacterStats[uuid]?.totalStats?.stats?.get(stat) ?: 0
        return base + itemBonus
    }

    /**
     * Adds [amount] to the base stat [stat] for [uuid]. Called by
     * [com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager] when a [PerkBaseStat] perk
     * is successfully purchased.
     */
    fun addBaseStatBonus(uuid: UUID, stat: StatType, amount: Int) {
        baseStatMap[uuid]?.merge(stat, amount, Int::plus)
    }

    @EventHandler
    fun onCharacterLoad(event: GameCharacterLoadEvent) {
        val character = event.character
        val uuid = character.bukkitPlayer.uniqueId
        val statMap = ConcurrentHashMap<StatType, Int>()
        baseStatMap[uuid] = statMap

        plugin.launch {
            try {
                val (skills, subClassType) =
                    character.withCharacterData { Pair(skills, traits.subClassType) }

                val subClass = subClassType ?: return@launch

                val pointsPerPosition =
                    mapOf(
                        SkillTreePosition.FIRST to skills.positionOneAllocated,
                        SkillTreePosition.SECOND to skills.positionTwoAllocated,
                        SkillTreePosition.THIRD to skills.positionThreeAllocated,
                    )

                for ((position, allocatedPoints) in pointsPerPosition) {
                    val treeData = SkillTreeData(position, allocatedPoints)
                    treeData.loadPerksFromSubClass(subClass)
                    for (perk in treeData.perks) {
                        if (perk !is PerkBaseStat) continue
                        if (perk.currentlyAllocatedPoints < perk.cost) continue
                        val amount = perk.bonusAmount * perk.currentlyAllocatedPoints
                        statMap.merge(perk.stat, amount, Int::plus)
                    }
                }
            } catch (exception: Exception) {
                logger.error("Failed to load base stats for $uuid", exception)
            }
        }
    }

    @EventHandler
    fun onCharacterQuit(event: GameCharacterQuitEvent) {
        baseStatMap.remove(event.character.bukkitPlayer.uniqueId)
    }
}
