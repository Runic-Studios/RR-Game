package com.runicrealms.game.items.character

import com.github.shynixn.mccoroutine.bukkit.callSuspendingEvent
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.google.common.collect.Sets
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.config.perk.GameItemPerkTemplate
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.game.items.event.ActiveItemPerksChangeEvent
import com.runicrealms.game.items.event.GameStatUpdateEvent
import com.runicrealms.game.items.generator.GameItem
import com.runicrealms.game.items.generator.GameItemArmor
import com.runicrealms.game.items.generator.GameItemOffhand
import com.runicrealms.game.items.generator.GameItemWeapon
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import java.util.EnumMap
import kotlin.concurrent.Volatile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

/** A simple container which caches the player's stats and updates their armor stats */
class CharacterEquipmentCache
@AssistedInject
constructor(
    @Assisted val character: GameCharacter,
    private val addedStatsFactory: AddedStats.Factory,
    private val perkTemplateRegistry: GameItemPerkTemplateRegistry,
    private val itemStackConverter: ItemStackConverter,
    private val plugin: Plugin,
) {

    private val logger = LoggerFactory.getLogger("items")

    interface Factory {
        fun create(player: Player): CharacterEquipmentCache
    }

    /*

    TODO this class desperately needs a rewrite that uses coroutines
    also someone gotta check if this is actually okay to do on the main thread i have no clue

    =========================================== PSA ===========================================
    if you plan to change this class please consult with excel
    everything has a purpose
    so many things are specifically set up to produce intended behavior within and outside of this class

    EXCEL FROM POST KOTLIN MIGRATION: i have no idea whats going on, oh no

    */

    // ItemPerks that exceed the max, and how much they would've been
    val itemPerksExceedingMax = HashMap<GameItemPerkTemplate, Int>()

    @Volatile private var helmet: GameItemArmor? = null

    @Volatile private var chestplate: GameItemArmor? = null

    @Volatile private var leggings: GameItemArmor? = null

    @Volatile private var boots: GameItemArmor? = null

    @Volatile private var offhand: GameItemOffhand? = null

    @Volatile private var weapon: GameItemWeapon? = null

    var totalStats: AddedStats
        private set

    private val statsModifiers = HashSet<StatsModifier>()

    /*
    - we store the "most recently used weapon" for a player
    - if the player has no recently used weapon (just logged in?) and they swap to one, they hear the beacon activate noise and gain item perks
    - if the player is holding their weapon and then swaps to a potion, they lose the item perk but no beacon noise plays
    - if the player is holding a potion and they swap to their recently used weapon, they regain the item perk but no beacon noise plays
    - if the player swaps to a weapon that is not their "recently used weapon" then they lose all weapon perks, beacon deactivate sound plays,
        and an internal countdown immediately begins until they can use weapon perks again.
        Upon the ending of that countdown, their "most recently used weapon" is reset (as if they just logged in)
    - Note that the cooldown will still activate even if the weapon they swapped to had no actual perks on it
     */
    private var recentWeapon: RecentWeapon? = null

    private var lastCooldown = 0L

    private fun isOnCooldown(): Boolean {
        return System.currentTimeMillis() - lastCooldown < WEAPON_PERKS_COOLDOWN_MILLIS
    }

    init {
        totalStats = addedStatsFactory.create(EnumMap(StatType::class.java), null, 0)
        plugin.launch { withContext(plugin.minecraftDispatcher) { updateAllItems(true, false) } }
    }

    // MUST ALWAYS RUN WITH SYNC CONTEXT
    fun updateTotalStats(onLogin: Boolean, weaponSwitched: Boolean) {
        var oldPerks = totalStats.perks
        totalStats = addedStatsFactory.create(mutableMapOf(), mutableSetOf(), 0)
        if (helmet != null) totalStats.combine(helmet!!.addedStats)
        if (chestplate != null) totalStats.combine(chestplate!!.addedStats)
        if (leggings != null) totalStats.combine(leggings!!.addedStats)
        if (boots != null) totalStats.combine(boots!!.addedStats)
        if (offhand != null) totalStats.combine(offhand!!.addedStats)
        val modifierStats = addedStatsFactory.create(mutableMapOf(), mutableSetOf(), 0)
        for (modifier in statsModifiers) {
            modifierStats.combine(modifier.getChanges(totalStats))
        }
        totalStats.combine(modifierStats)

        var beaconNoise: Boolean? =
            null // Null indicates default behavior, true indicates yes, false indicates no

        if (weapon == null || canUseWeapon(character, weapon!!)) {
            if (weapon != null && !weaponSwitched && !isOnCooldown()) {
                totalStats.combine(weapon!!.addedStats) // Default behavior
            } else if (weapon != null && weaponSwitched) {
                // Add just the stats no perks

                val weaponStats = weapon!!.addedStats
                totalStats.combine(
                    addedStatsFactory.create(weaponStats.stats, null, weaponStats.health)
                )

                // Because this logic is very confusing, I will try to outline the thought process
                // behind each statement
                if (!isOnCooldown()) { // if we are on cooldown we do nothing
                    if (recentWeapon != null && !recentWeapon!!.matchesItem(weapon)) {
                        // We had a weapon equipped, and we just swapped to a different weapon
                        // If the previous weapon had perks, activate cooldown, beacon deactivate,
                        // don't add any new perks.
                        // Else apply perks normally.
                        if (recentWeapon!!.hasItemPerks()) {
                            // Both the old weapon and the new one have perks
                            beaconNoise = true // Play beacon deactivate
                            lastCooldown = System.currentTimeMillis()
                            plugin.launch {
                                withContext(plugin.minecraftDispatcher) {
                                    delay(WEAPON_PERKS_COOLDOWN_MILLIS)
                                    recentWeapon =
                                        null // Reset our recent weapon, so we can get weapon perks
                                    // like normal
                                    updateWeaponAndTotal(false)
                                }
                            }
                        } else if (weapon!!.addedStats.hasItemPerks()) {
                            // Only the new one has perks, the old one didn't
                            // ... we should apply the perks normally
                            totalStats.combine(
                                addedStatsFactory.create(
                                    EMPTY_MUTABLE_STAT_MAP,
                                    weaponStats.perks,
                                    0,
                                )
                            )
                        }
                    } else {
                        // Either we didn't have a previous weapon (cooldown ended/login) or we
                        // swapped back to our previous weapon
                        // Does matter, just reapply stats as normal
                        totalStats.combine(
                            addedStatsFactory.create(EMPTY_MUTABLE_STAT_MAP, weaponStats.perks, 0)
                        )
                        if (recentWeapon != null) {
                            // This implies that we just equipped the same weapon as the last weapon
                            // (work through the logic)
                            // Suppress beacon noises:
                            beaconNoise = false
                        }
                    }
                }
            } else if (weapon == null && weaponSwitched && recentWeapon != null) {
                beaconNoise =
                    false // We de-equipped a weapon, suppress noise because we didn't swap to a
                // perks weapon
            }
        } else if (weapon != null) { // We equipped a weapon but we can't use it
            beaconNoise = false // Suppress noise
        }

        val perks = totalStats.perks
        itemPerksExceedingMax.clear()
        if (perks != null) {
            val newPerks =
                perks
                    .stream()
                    .map { perk: ItemData.Perk ->
                        val perkTemplate =
                            perkTemplateRegistry.getGameItemPerkTemplate(perk.perkID)!!
                        if (perk.stacks > perkTemplate.maxStacks) {
                            itemPerksExceedingMax[perkTemplate] = perk.stacks
                            return@map ItemData.Perk.newBuilder()
                                .setPerkID(perk.perkID)
                                .setStacks(perkTemplate.maxStacks)
                                .build()
                        }
                        perk
                    }
                    .toList()
            totalStats = addedStatsFactory.create(totalStats.stats, newPerks, totalStats.health)
        }

        if (oldPerks == null) oldPerks = EMPTY_MUTABLE_SET
        var newPerks = totalStats.perks
        if (newPerks == null) newPerks = EMPTY_MUTABLE_SET
        if (Sets.intersection(oldPerks, newPerks) != Sets.union(oldPerks, newPerks)) {
            if (!weaponSwitched)
                beaconNoise = null // We didn't switch weapons, disregard funky logic

            val playSounds = if (beaconNoise == null) !onLogin else !onLogin && beaconNoise
            val event = ActiveItemPerksChangeEvent(character, oldPerks, newPerks, playSounds)
            Bukkit.getPluginManager().callSuspendingEvent(event, plugin)
        } else {
            if (beaconNoise != null && beaconNoise) {
                // This is for the rare case where a player switches from a perks weapon, to a
                // non-weapon item, to a new weapon
                // Here, the total perks would not be changing for the final swap but we still play
                // the deactivation noise with cooldown
                character.player.playSound(
                    character.player.location,
                    Sound.BLOCK_BEACON_DEACTIVATE,
                    1.0f,
                    2.0f,
                )
            }
        }
    }

    fun updateAllItems(onLogin: Boolean, callEvent: Boolean) {
        check(!(!onLogin && Bukkit.isPrimaryThread())) { "Cannot run update stats on main thread!" }
        updateHelmet()
        updateChestplate()
        updateLeggings()
        updateBoots()
        updateOffhand()
        updateWeaponAndTotal(onLogin) // also updates total
        if (callEvent)
            Bukkit.getPluginManager()
                .callSuspendingEvent(GameStatUpdateEvent(character, this), plugin)
    }

    fun updateItems(onLogin: Boolean, vararg types: StatHolderType) {
        check(!(!onLogin && Bukkit.isPrimaryThread())) { "Cannot run update stats on main thread!" }
        var hasUpdatedTotal = false
        for (type in types) {
            when (type) {
                StatHolderType.HELMET -> updateHelmet()
                StatHolderType.CHESTPLATE -> updateChestplate()
                StatHolderType.LEGGINGS -> updateLeggings()
                StatHolderType.BOOTS -> updateBoots()
                StatHolderType.WEAPON -> {
                    updateWeaponAndTotal(onLogin)
                    hasUpdatedTotal = true
                }

                StatHolderType.OFFHAND -> updateOffhand()
            }
        }
        if (!hasUpdatedTotal) updateTotalStats(onLogin, false)
    }

    private fun updateHelmet() {
        val itemStack = character.player.inventory.helmet
        try {
            helmet =
                if (itemStack == null) null
                else itemStackConverter.convertToGameItem(itemStack) as? GameItemArmor
        } catch (exception: Exception) {
            logger.error("Error loading player ${character.player.name} helmet!")
            exception.printStackTrace()
            helmet = null
        }
        if (helmet?.addedStats?.hasItemPerks() == true)
            character.player.updateInventory() // Update dynamic lore
    }

    private fun updateChestplate() {
        val itemStack = character.player.inventory.chestplate
        try {
            chestplate =
                if (itemStack == null) null
                else itemStackConverter.convertToGameItem(itemStack) as? GameItemArmor
        } catch (exception: Exception) {
            logger.error("Error loading player ${character.player.name} chestplate!")
            exception.printStackTrace()
            chestplate = null
        }
        if (chestplate?.addedStats?.hasItemPerks() == true)
            character.player.updateInventory() // Update dynamic lore
    }

    private fun updateLeggings() {
        val itemStack = character.player.inventory.leggings
        try {
            leggings =
                if (itemStack == null) null
                else itemStackConverter.convertToGameItem(itemStack) as? GameItemArmor
        } catch (exception: Exception) {
            logger.error("Error loading player ${character.player.name} leggings!")
            exception.printStackTrace()
            leggings = null
        }
        if (leggings?.addedStats?.hasItemPerks() == true)
            character.player.updateInventory() // Update dynamic lore
    }

    private fun updateBoots() {
        val itemStack = character.player.inventory.boots
        try {
            boots =
                if (itemStack == null) null
                else itemStackConverter.convertToGameItem(itemStack) as? GameItemArmor
        } catch (exception: Exception) {
            logger.error("Error loading player ${character.player.name} boots!")
            exception.printStackTrace()
            boots = null
        }
        if (boots?.addedStats?.hasItemPerks() == true)
            character.player.updateInventory() // Update dynamic lore
    }

    // This one also updates the total because we need to update total before we change the recent
    // weapon to the current one
    private fun updateWeaponAndTotal(onLogin: Boolean) {
        val itemStack = character.player.inventory.itemInMainHand
        try {
            weapon = itemStackConverter.convertToGameItem(itemStack) as? GameItemWeapon
        } catch (exception: Exception) {
            logger.error("Error loading player ${character.player.name} weapon!")
            exception.printStackTrace()
            weapon = null
        }

        updateTotalStats(onLogin, true)

        if (weapon?.addedStats?.hasItemPerks() == true) {
            // Experimental change that didn't work to force update the weapon
            //            PacketContainer container = new
            // PacketContainer(PacketType.Play.Server.SET_SLOT);
            //            container.getBytes().write(-2, (byte) 0); // Window ID: -2 means ignore
            // state ID
            //            container.getIntegers().write(0, 0); // State ID: bogus value 0
            //            container.getShorts().write(0, (short)
            // player.getInventory().getHeldItemSlot()); // Slot number
            //            container.getItemModifier().write(0,
            // player.getInventory().getItemInMainHand()); // ItemStack
            //            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
            character.player.updateInventory() // Update dynamic lore
        }

        // For item perks warmup
        if (weapon != null && !isOnCooldown()) {
            if (recentWeapon?.matchesItem(weapon) == true)
                return // avoid constructing a new object if we can

            if (!canUseWeapon(character, weapon!!)) return
            recentWeapon = RecentWeapon(weapon!!)
        }
    }

    private fun updateOffhand() {
        val itemStack = character.player.inventory.itemInOffHand
        try {
            offhand = itemStackConverter.convertToGameItem(itemStack) as? GameItemOffhand
        } catch (exception: Exception) {
            logger.error("Error loading player ${character.player.name} offhand!")
            exception.printStackTrace()
            offhand = null
        }

        if (offhand?.addedStats?.hasItemPerks() == true)
            character.player.updateInventory() // Update dynamic lore
    }

    fun getHelmet(): GameItemArmor? {
        return helmet
    }

    fun getChestplate(): GameItemArmor? {
        return chestplate
    }

    fun getLeggings(): GameItemArmor? {
        return leggings
    }

    fun getBoots(): GameItemArmor? {
        return boots
    }

    fun getWeapon(): GameItemWeapon? {
        return if (weapon == null) null
        else (if (canUseWeapon(character, weapon!!)) weapon else null)
    }

    fun getOffhand(): GameItemOffhand? {
        return offhand
    }

    fun addModifier(modifier: StatsModifier) {
        statsModifiers.add(modifier)
        updateTotalStats(false, false)
    }

    fun removeModifier(modifier: StatsModifier) {
        statsModifiers.remove(modifier)
        updateTotalStats(false, false)
    }

    enum class StatHolderType {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS,
        WEAPON,
        OFFHAND,
    }

    private class RecentWeapon(val templateID: String, val itemPerks: Set<ItemData.Perk>?) {
        constructor(weapon: GameItemWeapon) : this(weapon.template.id, weapon.addedStats.perks)

        fun matchesItem(item: GameItem?): Boolean {
            if (item !is GameItemWeapon) return false
            return item.template.equals(templateID) && itemPerks == item.addedStats.perks
        }

        fun hasItemPerks(): Boolean {
            return !itemPerks.isNullOrEmpty()
        }
    }

    companion object {

        private const val WEAPON_PERKS_COOLDOWN_MILLIS = 5000L

        // Save for memory/performance reasons
        private val EMPTY_MUTABLE_SET = mutableSetOf<ItemData.Perk>()
        private val EMPTY_MUTABLE_STAT_MAP = mutableMapOf<StatType, Int>()

        private fun canUseWeapon(character: GameCharacter, weapon: GameItemWeapon): Boolean {
            return runBlocking {
                val type = character.withCharacterData { traits.data.classType }
                return@runBlocking weapon.weaponTemplate.level <= character.player.level &&
                    type == weapon.weaponTemplate.classType
            }
        }
    }
}
