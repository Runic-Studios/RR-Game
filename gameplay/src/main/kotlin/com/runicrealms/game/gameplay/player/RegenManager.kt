package com.runicrealms.game.gameplay.player

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
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Manages player health and mana regeneration.
 *
 * [SpellManager] is the single source of truth for current mana — spells deduct from it via
 * [SpellManager.getMana]/[SpellManager.setMana], and this class regens into it every
 * [REGEN_PERIOD] seconds. This avoids the two-map split that existed when [RegenManager] kept its
 * own `currentManaList` separate from [SpellManager.manaMap].
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
    private val spellManager: SpellManager,
) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        // Runs on the MCCoroutine main dispatcher (like the old BukkitRunnable), so sync events
        // can be fired safely. delay() suspends without blocking the main thread.
        plugin.launch {
            while (true) {
                delay(REGEN_PERIOD * 1000L)
                for (character in userDataRegistry.getAllCharacters()) {
                    regenHealth(character)
                    regenMana(character)
                }
            }
        }
    }

    /** Initialises the player's mana to their max mana on character load. */
    @EventHandler
    fun onCharacterLoad(event: GameCharacterLoadEvent) {
        val character = event.character
        spellManager.setMana(character.bukkitPlayer.uniqueId, calculateMaxMana(character))
    }

    /** Cleans up mana state when a character logs out. */
    @EventHandler
    fun onCharacterQuit(event: GameCharacterQuitEvent) {
        spellManager.setMana(event.character.bukkitPlayer.uniqueId, 0)
    }

    /** Returns the player's current mana via [SpellManager] (the single mana store). */
    fun getCurrentMana(uuid: UUID): Int = spellManager.getMana(uuid)

    /**
     * Adds mana to the current pool for the given player. Cannot exceed max mana.
     *
     * @param character to receive mana
     * @param amount of mana to add
     */
    fun addMana(character: GameCharacter, amount: Int) {
        val uuid = character.bukkitPlayer.uniqueId
        val mana = spellManager.getMana(uuid)
        val maxMana: Int = calculateMaxMana(character)
        if (mana < maxMana)
            spellManager.setMana(uuid, min((mana + amount).toDouble(), maxMana.toDouble()).toInt())
    }

    /**
     * Returns the mana-per-level multiplier for the given character based on their class.
     *
     * @param character to calculate mana for
     * @return the mana awarded per level
     */
    fun getManaPerLv(character: GameCharacter): Double {
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

    /** Periodic task: regenerates health for one character. */
    private suspend fun regenHealth(character: GameCharacter) {
        val player = character.bukkitPlayer
        val regenAmount =
            (HEALTH_REGEN_BASE_VALUE + (HEALTH_REGEN_LEVEL_MULTIPLIER * player.level)).toInt()
        val regenAmountFinal =
            if (!combatManager.isInCombat(player.uniqueId)) regenAmount * OOC_MULTIPLIER
            else regenAmount
        val event = HealthRegenEvent(player, regenAmountFinal)
        Bukkit.getPluginManager().callSuspendingEvent(event, plugin).joinAll()
    }

    /** Periodic task: regenerates mana for one character. */
    private suspend fun regenMana(character: GameCharacter) {
        val player = character.bukkitPlayer
        val uuid = player.uniqueId
        val mana = spellManager.getMana(uuid)
        val maxMana: Int = calculateMaxMana(character)
        if (mana >= maxMana) return

        var regenAmt = calculateManaRegen(player.level)
        if (!combatManager.isInCombat(uuid)) regenAmt *= OOC_MULTIPLIER

        val event = ManaRegenEvent(player, regenAmt)
        Bukkit.getPluginManager().callSuspendingEvent(event, plugin).joinAll()
        if (!event.isCancelled) {
            spellManager.setMana(uuid, (mana + event.amount).coerceAtMost(maxMana))
        }
    }

    /**
     * Calculates the character's total max mana from base, class-per-level scaling, and wisdom.
     * Also clamps current mana down to the new max if it exceeds it (e.g. after equipment swap).
     */
    fun calculateMaxMana(character: GameCharacter): Int {
        val player = character.bukkitPlayer
        val uuid = player.uniqueId
        val newMaxMana = (BASE_MANA + (getManaPerLv(character) * player.level)).toInt()
        val wisdom = statManager.getStat(uuid, StatType.WISDOM)
        val wisdomBoost: Double = newMaxMana * (STAT_MAX_MANA_MULT * wisdom)
        val maxMana = (newMaxMana + wisdomBoost).toInt()

        // Clamp current mana if equipment change lowered the cap
        val currentMana = spellManager.getMana(uuid)
        if (currentMana > maxMana) {
            spellManager.setMana(uuid, maxMana)
        }
        return maxMana
    }

    companion object {
        private const val HEALTH_REGEN_BASE_VALUE = 5
        private const val HEALTH_REGEN_LEVEL_MULTIPLIER = 0.15
        private const val OOC_MULTIPLIER = 4 // out-of-combat regen multiplier
        private const val REGEN_PERIOD = 4L // seconds

        const val BASE_MANA: Int = 150
        private const val BASE_MANA_REGEN_AMT = 5

        private const val ARCHER_MANA_LV = 1.75
        private const val CLERIC_MANA_LV = 2.25
        private const val MAGE_MANA_LV = 2.75
        private const val ROGUE_MANA_LV = 1.5
        private const val WARRIOR_MANA_LV = 1.5

        private const val STAT_MAX_MANA_MULT = 0.01

        /**
         * Mana regen amount increases slightly each level.
         *
         * @param level of the player
         * @return mana awarded per regen tick
         */
        fun calculateManaRegen(level: Int): Int {
            return (BASE_MANA_REGEN_AMT + (level.toDouble() / 12)).roundToInt()
        }
    }
}
