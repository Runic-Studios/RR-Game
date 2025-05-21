package com.runicrealms.game.items.perk

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import com.runicrealms.game.items.config.perk.GameItemPerkTemplate
import com.runicrealms.game.items.dynamic.DynamicItemRegistry
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

abstract class GameItemPerkHandler
protected constructor(
    val template: GameItemPerkTemplate,
    plugin: Plugin,
    dynamicItemRegistry: DynamicItemRegistry,
    private val equipmentRegistry: CharacterEquipmentCacheRegistry,
) : Listener {

    private val active: MutableSet<UUID> = Collections.newSetFromMap(ConcurrentHashMap())
    private val dynamicItemPerksStacksTextPlaceholder =
        DynamicItemPerkStacksTextPlaceholder(this, equipmentRegistry)

    init {
        dynamicItemRegistry.registerTextPlaceholder(
            this.dynamicItemPerksStacksTextPlaceholder
        ) // used to handle lore
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
    }

    /** Returns the display name for this perk to be used on item lore */
    open fun getName(): TextComponent {
        return template.name
    }

    /**
     * Gets the lore for this item perk. This is the portion of the lore that appears on the item
     * for the item perk, NOT INCLUDING the header
     *
     * Header: [?/4] +X Perk Name
     *
     * Lore: explanation lorum ipsum 50% dolor sit amet etc etc etc
     *
     * Null indicates no lore
     */
    open fun getLoreSection(): List<TextComponent> {
        return template.lore
    }

    /**
     * Called when the number of stacks of this item perk changes. Fires async. Override this method
     * to track changes to a player's stacks.
     *
     * @param stacks Number of stacks, 0 indicates no stacks (item perk deactivated).
     */
    open fun onChange(character: GameCharacter?, stacks: Int) {}

    /**
     * Updates the status of a player's item perk stacks. Used internally, cannot override.
     *
     * @param character Character who has the item perk equipped/de-equipped
     * @param stacks How many item perk stacks the player has (0 if unequipped)
     */
    fun updateActive(character: GameCharacter, stacks: Int) {
        if (stacks > 0) {
            active.add(character.bukkitPlayer.uniqueId)
        } else {
            active.remove(character.bukkitPlayer.uniqueId)
        }
        onChange(character, stacks)
    }

    /**
     * Returns the current amount of stacks of this perk that a player has equipped. This is
     * automatically capped by the maximum.
     */
    fun getCurrentStacks(character: GameCharacter): Int {
        val cache =
            equipmentRegistry.cachedCharacterStats[character.bukkitPlayer.uniqueId] ?: return 0
        val activePerks = cache.totalStats.perks
        if (activePerks != null) {
            for (perk in activePerks) {
                if (perk.perkID == template.identifier) {
                    return perk.stacks
                }
            }
        }
        return 0
    }

    /**
     * Returns the current amount of stacks of this perk that a player has equipped; This is not
     * capped by the maximum number of stacks for this perk.
     */
    fun getCurrentUncappedStacks(character: GameCharacter): Int {
        val cache =
            equipmentRegistry.cachedCharacterStats[character.bukkitPlayer.uniqueId] ?: return 0
        val activePerks = cache.itemPerksExceedingMax
        val uncappedStacks = activePerks.getOrDefault(template, 0)
        if (uncappedStacks != 0) return uncappedStacks
        return getCurrentStacks(character)
    }

    /**
     * Gets whether a given player has this item perk equipped. Use getCurrentStacks(Player) to get
     * the number of stacks.
     *
     * @param player Player to check
     * @return If they have this perk equipped
     */
    fun isActive(player: Player): Boolean {
        return active.contains(player.uniqueId)
    }

    /**
     * Gets the current set of active players who have this item perk equipped. Do not modify this
     * set.
     *
     * @return Active players' UUIDs
     */
    fun getActive(): Set<UUID> {
        return this.active
    }

    fun getDynamicItemPerksStacksTextPlaceholder(): DynamicItemPerkStacksTextPlaceholder {
        return this.dynamicItemPerksStacksTextPlaceholder
    }

    @EventHandler
    private fun onCharacterQuit(event: GameCharacterQuitEvent) {
        active.remove(event.character.bukkitPlayer.uniqueId)
    }
}
