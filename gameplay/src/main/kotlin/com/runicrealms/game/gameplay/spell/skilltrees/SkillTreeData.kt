package com.runicrealms.game.gameplay.spell.skilltrees

import com.runicrealms.game.common.SubClassType
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.skilltrees.perks.Perk
import com.runicrealms.game.gameplay.spell.skilltrees.perks.PerkSpell
import com.runicrealms.game.gameplay.spell.skilltrees.util.ArcherTreeUtil
import com.runicrealms.game.gameplay.spell.skilltrees.util.ClericTreeUtil
import com.runicrealms.game.gameplay.spell.skilltrees.util.MageTreeUtil
import com.runicrealms.game.gameplay.spell.skilltrees.util.RogueTreeUtil
import com.runicrealms.game.gameplay.spell.skilltrees.util.WarriorTreeUtil

/**
 * Represents a player's skill tree state for one [SkillTreePosition].
 *
 * [totalAllocatedPoints] is the only value persisted to MongoDB (via
 * [com.runicrealms.game.data.model.CharacterSkills]). The [perks] list is reconstructed at runtime
 * from [totalAllocatedPoints] using [loadPerksFromSubClass].
 *
 * Architecture mirrors the old SkillTreeData.java, with:
 * - `@Transient perks` → runtime-only, not stored
 * - totalAllocatedPoints → stored as positionOneAllocated etc.
 */
class SkillTreeData(val position: SkillTreePosition, var totalAllocatedPoints: Int = 0) {

    /** Runtime-only perk list, reconstructed by [loadPerksFromSubClass]. */
    var perks: List<Perk> = emptyList()
        private set

    /**
     * Reconstructs the perk list from [subClassType] and applies [totalAllocatedPoints]
     * sequentially (each point purchases perks in order).
     */
    fun loadPerksFromSubClass(subClassType: SubClassType) {
        val freshPerks = getSkillTreeBySubClass(subClassType).toMutableList()
        var remaining = totalAllocatedPoints
        for (perk in freshPerks) {
            if (remaining <= 0) break
            while (!perk.isMaxed() && remaining > 0) {
                perk.allocate()
                remaining--
            }
        }
        perks = freshPerks
    }

    /** Returns the default perk list for the given [subClassType] (from the relevant TreeUtil). */
    fun getSkillTreeBySubClass(subClassType: SubClassType): List<Perk> =
        when (subClassType) {
            SubClassType.MARKSMAN -> ArcherTreeUtil.marksmanPerkList()
            SubClassType.STORMSHOT -> ArcherTreeUtil.stormshotPerkList()
            SubClassType.WARDEN -> ArcherTreeUtil.wardenPerkList()
            SubClassType.BARD -> ClericTreeUtil.bardPerkList()
            SubClassType.LIGHTBRINGER -> ClericTreeUtil.lightbringerPerkList()
            SubClassType.STARWEAVER -> ClericTreeUtil.starweaverPerkList()
            SubClassType.CRYOMANCER -> MageTreeUtil.cryomancerPerkList()
            SubClassType.PYROMANCER -> MageTreeUtil.pyromancerPerkList()
            SubClassType.SPELLSWORD -> MageTreeUtil.spellswordPerkList()
            SubClassType.CORSAIR -> RogueTreeUtil.corsairPerkList()
            SubClassType.NIGHTCRAWLER -> RogueTreeUtil.nightcrawlerPerkList()
            SubClassType.WITCH_HUNTER -> RogueTreeUtil.witchHunterPerkList()
            SubClassType.BERSERKER -> WarriorTreeUtil.berserkerPerkList()
            SubClassType.DREADLORD -> WarriorTreeUtil.dreadlordPerkList()
            SubClassType.PALADIN -> WarriorTreeUtil.paladinPerkList()
        }

    /** Returns all purchased PerkSpell names. */
    fun getUnlockedSpellNames(): List<String> =
        perks.filterIsInstance<PerkSpell>().filter { it.isPurchased() }.map { it.spellName }

    /** Returns all passive spell names from purchased PerkSpell perks. */
    fun addPassivesToMap(passiveMap: MutableSet<String>, spellManager: SpellManager) {
        for (spellName in getUnlockedSpellNames()) {
            val spell = spellManager.getSpell(spellName) ?: continue
            if (spell.isPassive) passiveMap.add(spellName.lowercase())
        }
    }

    companion object {
        /** Players start earning skill points at level 10. */
        const val FIRST_POINT_LEVEL = 10
    }
}
