package com.runicrealms.game.gameplay.player

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.plugin.Plugin

class HungerManager @Inject constructor(
    private val plugin: Plugin
) : Listener {
    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            RunicCore.getInstance(),
            Runnable { this.tickAllOnlinePlayersHunger() }, HUNGER_TICK_TASK_DELAY * 20L, PLAYER_HUNGER_TIME * 20L
        )
    }

    /**
     * Prevent eating items which are not consumables
     */
    @EventHandler(priority = EventPriority.LOWEST) // first
    fun onFoodInteract(event: PlayerItemConsumeEvent) {
        val runicItem: RunicItem = RunicItemsAPI.getRunicItemFromItemStack(event.item) ?: return
        val isConsumable: Boolean = runicItem.getTags().contains(RunicItemTag.CONSUMABLE)
        if (!isConsumable) {
            event.isCancelled = true
            event.player.sendMessage(ChatColor.RED.toString() + "I can't eat that!")
        }
    }

    /**
     * Prevents normal decay of hunger
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        if (event.foodLevel <= player.foodLevel) event.isCancelled = true
    }

    /**
     * Reduces regen for players below half hunger
     */
    @EventHandler
    fun onHealthRegen(event: HealthRegenEvent) {
        val foodLevel: Int = event.getPlayer().getFoodLevel()
        if (foodLevel <= INVIGORATED_HUNGER_THRESHOLD) {
            event.isCancelled = true
        } else if (foodLevel <= 10) {
            event.setAmount((event.getAmount() * HALF_HUNGER_REGEN_MULTIPLIER) as Int)
        }
    }

    private fun restoreHunger(player: Player) {
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f)
        player.sendMessage(ChatColor.GREEN.toString() + "You feel rested in the city! Your hunger has been sated.")
        player.foodLevel = 20
    }

    /**
     * Manually reduce player hunger. Either reduces player saturation if it exists,
     * or reduces player hunger value if there is no saturation
     */
    private fun tickAllOnlinePlayersHunger() {
        for (uuid in RunicDatabase.getAPI().getCharacterAPI().getLoadedCharacters()) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            if (RunicCore.getRegionAPI().isSafezone(player.location)) { // prevent hunger loss in capital cities
                if (player.foodLevel < 20) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(RunicCore.getInstance()) { restoreHunger(player) }
                }
                continue
            }
            if (player.foodLevel <= STARVATION_HUNGER_LEVEL) continue
            if (player.saturation > 0) continue
            player.foodLevel = player.foodLevel - 1
        }
    }

    companion object {
        private const val HUNGER_TICK_TASK_DELAY = 60 // seconds
        private const val PLAYER_HUNGER_TIME = 60 // tick time in seconds
        private const val INVIGORATED_HUNGER_THRESHOLD = 6 // hunger level to receive regen
        private const val STARVATION_HUNGER_LEVEL = 1
        private const val HALF_HUNGER_REGEN_MULTIPLIER = 0.5
    }
}
