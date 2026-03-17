package com.runicrealms.game.gameplay.spell.skilltrees

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.gameplay.player.stat.StatManager
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.api.SkillTreeAPI
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkBaseStat
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * Manages the player skill tree state in memory, backed by [CharacterSkills] and [CharacterSpells]
 * in PlayerDocument.
 *
 * On [GameCharacterLoadEvent] the three [SkillTreeData] objects are reconstructed from the
 * persisted totalAllocatedPoints. The perk lists are runtime-only (not stored in MongoDB).
 *
 * On [GameCharacterQuitEvent] the skill tree data is serialised back into [CharacterSkills] and the
 * active passives map is cleared.
 */
@Singleton
class SkillTreeManager
@Inject
constructor(
    private val plugin: Plugin,
    private val spellManager: SpellManager,
    private val userDataRegistry: UserDataRegistry,
    private val statManagerProvider: Provider<StatManager>,
) : SkillTreeAPI, Listener {

    /** UUID -> (slot -> Map<SkillTreePosition, SkillTreeData>) */
    private val skillTreeMap: ConcurrentHashMap<UUID, Map<SkillTreePosition, SkillTreeData>> =
        ConcurrentHashMap()

    /** UUID -> Set<passiveSpellNameLowercase> */
    private val passiveMap: ConcurrentHashMap<UUID, MutableSet<String>> = ConcurrentHashMap()

    /** UUID -> SpellData (runtime mirror of CharacterSpells) */
    private val spellDataMap: ConcurrentHashMap<UUID, SpellData> = ConcurrentHashMap()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    // --- Events ---

    @EventHandler
    fun onCharacterLoad(event: GameCharacterLoadEvent) {
        val character = event.character
        val uuid = character.bukkitPlayer.uniqueId
        plugin.launch {
            try {
                val (charSkills, charSpells, subClassType) =
                    character.withCharacterData { Triple(skills, spells, traits.subClassType) }

                val trees =
                    mapOf(
                        SkillTreePosition.FIRST to
                            SkillTreeData(SkillTreePosition.FIRST, charSkills.positionOneAllocated),
                        SkillTreePosition.SECOND to
                            SkillTreeData(
                                SkillTreePosition.SECOND,
                                charSkills.positionTwoAllocated,
                            ),
                        SkillTreePosition.THIRD to
                            SkillTreeData(
                                SkillTreePosition.THIRD,
                                charSkills.positionThreeAllocated,
                            ),
                    )

                // Reconstruct perk lists if subclass is known
                if (subClassType != null) {
                    for ((_, tree) in trees) {
                        tree.loadPerksFromSubClass(subClassType)
                    }
                }

                skillTreeMap[uuid] = trees
                spellDataMap[uuid] = SpellData.fromCharacterSpells(charSpells)

                // Rebuild passives map
                val passives: MutableSet<String> = mutableSetOf()
                for ((_, tree) in trees) {
                    tree.addPassivesToMap(passives, spellManager)
                }
                passiveMap[uuid] = passives
            } catch (exception: Exception) {
                logger.error("Failed to load skill trees for $uuid", exception)
            }
        }
    }

    @EventHandler
    fun onCharacterQuit(event: GameCharacterQuitEvent) {
        val uuid = event.character.bukkitPlayer.uniqueId
        val trees = skillTreeMap.remove(uuid) ?: return
        passiveMap.remove(uuid)
        val spellData = spellDataMap.remove(uuid)

        plugin.launch {
            try {
                event.character.withCharacterData {
                    skills.positionOneAllocated =
                        trees[SkillTreePosition.FIRST]?.totalAllocatedPoints ?: 0
                    skills.positionTwoAllocated =
                        trees[SkillTreePosition.SECOND]?.totalAllocatedPoints ?: 0
                    skills.positionThreeAllocated =
                        trees[SkillTreePosition.THIRD]?.totalAllocatedPoints ?: 0
                    if (spellData != null) {
                        spells.spellOneID = spellData.spellHotbarOne
                        spells.spellTwoID = spellData.spellLeftClick
                        spells.spellThreeID = spellData.spellRightClick
                        spells.spellFourID = spellData.spellSwapHands
                    }
                }
            } catch (exception: Exception) {
                logger.error("Failed to save skill trees for $uuid", exception)
            }
        }
    }

    // --- SkillTreeAPI ---

    override fun getAvailableSkillPoints(uuid: UUID, slot: Int): Int {
        val level =
            userDataRegistry.getCharacter(uuid)?.withSyncCharacterData { traits.level } ?: return 0
        val totalEarned = maxOf(0, level - SkillTreeData.FIRST_POINT_LEVEL + 1)
        return totalEarned - getSpentPoints(uuid, slot)
    }

    override fun getPassives(uuid: UUID): Set<String> = passiveMap[uuid] ?: emptySet()

    override fun getSpentPoints(uuid: UUID, slot: Int): Int {
        val pos = SkillTreePosition.fromValue(slot)
        return skillTreeMap[uuid]?.get(pos)?.totalAllocatedPoints ?: 0
    }

    override fun hasPassiveFromSkillTree(uuid: UUID, passive: String): Boolean =
        passiveMap[uuid]?.contains(passive.lowercase()) == true

    override fun getPlayerSpellName(uuid: UUID, slot: Int, spellSlotIndex: Int): String? =
        spellDataMap[uuid]?.getSpellForSlotIndex(spellSlotIndex)

    /** Returns the full skill tree map for a player (all 3 positions). */
    fun getSkillTreeDataMap(uuid: UUID): Map<SkillTreePosition, SkillTreeData>? = skillTreeMap[uuid]

    /** Returns the spell data for a player. */
    fun getSpellData(uuid: UUID): SpellData? = spellDataMap[uuid]

    /**
     * Attempts to purchase [perk] in the [SkillTreePosition] for [uuid]. Validates skill points and
     * prerequisites.
     */
    fun attemptToPurchasePerk(uuid: UUID, position: SkillTreePosition, perk: Perk): Boolean {
        val trees = skillTreeMap[uuid] ?: return false
        val tree = trees[position] ?: return false
        if (getAvailableSkillPoints(uuid, position.value) < perk.cost) return false
        if (!perk.allocate()) return false
        tree.totalAllocatedPoints += perk.cost

        // Apply base stat bonus if this perk grants stats
        if (perk is PerkBaseStat) {
            val amount = perk.bonusAmount * perk.currentlyAllocatedPoints
            statManagerProvider.get().addBaseStatBonus(uuid, perk.stat, amount)
        }

        // Rebuild passives
        val player = Bukkit.getPlayer(uuid) ?: return true
        val passives: MutableSet<String> = mutableSetOf()
        for ((_, t) in trees) {
            t.addPassivesToMap(passives, spellManager)
        }
        passiveMap[uuid] = passives
        return true
    }
}
