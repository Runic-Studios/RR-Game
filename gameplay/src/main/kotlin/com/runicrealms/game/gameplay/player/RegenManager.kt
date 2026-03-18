package com.runicrealms.game.gameplay.player

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.callSuspendingEvent
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.StatType
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.event.GameCharacterQuitEvent
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.gameplay.player.stat.StatManager
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Class to manage player health and mana. Stores max mana in the player data file, and creates a
 * HashMap to store all current player mana values.
 *
 * @author Skyfallin
 */
class RegenManager
@Inject
constructor(
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val statManager: StatManager,
    private val combatManager: CombatManager,
) : Listener {

    private val currentManaList = ConcurrentHashMap<UUID, Int>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        // regen async to speed up
        plugin.launch {
            withContext(plugin.asyncDispatcher) {
                for (character in userDataRegistry.getAllCharacters()) {
                    regenHealth(character)
                    regenMana(character)
                }
                delay(REGEN_PERIOD * 1000L)
            }
        }
    }

    /** Initialises the player's mana to their max mana. Mirrors old ManaListener.onCharacterLoad. */
    @EventHandler
    fun onCharacterLoad(event: GameCharacterLoadEvent) {
        val character = event.character
        currentManaList[character.bukkitPlayer.uniqueId] = calculateMaxMana(character)
    }

    /** Cleans up stored mana when a character logs out. */
    @EventHandler
    fun onCharacterQuit(event: GameCharacterQuitEvent) {
        currentManaList.remove(event.character.bukkitPlayer.uniqueId)
    }

    /** Returns the player's current mana, or 0 if not yet initialised. */
    fun getCurrentMana(uuid: UUID): Int = currentManaList[uuid] ?: 0

    /**
     * Adds mana to the current pool for the given player. Cannot add above max mana pool
     *
     * @param player to receive mana
     * @param amount of mana to receive
     */
    fun addMana(character: GameCharacter, amount: Int) {
        // Sync context
        val player = character.bukkitPlayer
        val mana = currentManaList[player.uniqueId]!!
        val maxMana: Int = calculateMaxMana(character)
        if (mana < maxMana)
            currentManaList[player.uniqueId] =
                min((mana + amount).toDouble(), maxMana.toDouble()).toInt()
    }

    /**
     * Determines the amount of mana to award per level to the given player based on class
     *
     * @param player to calculate mana for
     * @return the mana per level
     */
    fun getManaPerLv(character: GameCharacter): Double {
        // Sync context
        return character.withSyncCharacterData {
            when (traits.classType) {
                ClassType.ARCHER -> ARCHER_MANA_LV
                ClassType.CLERIC -> CLERIC_MANA_LV
                ClassType.MAGE -> MAGE_MANA_LV
                ClassType.ROGUE -> ROGUE_MANA_LV
                ClassType.WARRIOR -> WARRIOR_MANA_LV
                else -> 0.0
            }
        }
    }

    /** Task to regen health with appropriate modifiers */
    private suspend fun regenHealth(character: GameCharacter) {
        // Sync context
        val player = character.bukkitPlayer
        val regenAmount =
            (HEALTH_REGEN_BASE_VALUE + (HEALTH_REGEN_LEVEL_MULTIPLIER * player.level)).toInt()
        val regenAmountFinal =
            if (!combatManager.isInCombat(player.uniqueId)) regenAmount * OOC_MULTIPLIER
            else regenAmount
        val event = HealthRegenEvent(player, regenAmountFinal)
        Bukkit.getPluginManager().callSuspendingEvent(event, plugin).joinAll()
    }

    /** Periodic task to regenerate mana for all online players */
    private suspend fun regenMana(character: GameCharacter) {
        // Sync context
        val player = character.bukkitPlayer
        val mana =
            if (currentManaList.containsKey(player.uniqueId)) currentManaList[player.uniqueId]!!
            else (BASE_MANA + getManaPerLv(character)).toInt()

        val maxMana: Int = calculateMaxMana(character)
        if (mana >= maxMana) return

        var regenAmt = calculateManaRegen(player.level)

        if (!combatManager.isInCombat(player.uniqueId)) regenAmt *= OOC_MULTIPLIER

        val event = ManaRegenEvent(player, regenAmt)
        Bukkit.getPluginManager().callSuspendingEvent(event, plugin).joinAll()
        if (!event.isCancelled) {
            currentManaList[player.uniqueId] = (mana + event.amount).coerceAtMost(maxMana)
        }
    }

    /**
     * Calculates the total mana for the given player
     *
     * @param player to calculate mana for
     */
    fun calculateMaxMana(character: GameCharacter): Int {
        // Sync context
        val maxMana: Int
        // recalculate max mana based on player level
        val player = character.bukkitPlayer
        val newMaxMana = (BASE_MANA + (getManaPerLv(character) * player.level)).toInt()
        val wisdom = statManager.getStat(player.uniqueId, StatType.WISDOM)
        val wisdomBoost: Double = newMaxMana * (STAT_MAX_MANA_MULT * wisdom)
        maxMana = (newMaxMana + wisdomBoost).toInt()

        // fix current mana if it is now too high
        val currentMana = currentManaList[player.uniqueId] ?: 0
        if (currentMana > maxMana) {
            currentManaList[player.uniqueId] = maxMana
        }
        return maxMana
    }

    companion object {
        private const val HEALTH_REGEN_BASE_VALUE = 5
        private const val HEALTH_REGEN_LEVEL_MULTIPLIER = 0.15
        private const val OOC_MULTIPLIER = 4 // out-of-combat
        private const val REGEN_PERIOD = 4 // seconds

        const val BASE_MANA: Int = 150
        private const val BASE_MANA_REGEN_AMT = 5

        private const val ARCHER_MANA_LV = 1.75
        private const val CLERIC_MANA_LV = 2.25
        private const val MAGE_MANA_LV = 2.75
        private const val ROGUE_MANA_LV = 1.5
        private const val WARRIOR_MANA_LV = 1.5

        private const val STAT_MAX_MANA_MULT = 0.01

        /**
         * Mana regen increases each level
         *
         * @param level of the player
         * @return the mana they should receive each tick
         */
        fun calculateManaRegen(level: Int): Int {
            return (BASE_MANA_REGEN_AMT + (level.toDouble() / 12)).roundToInt()
        }
    }
}
