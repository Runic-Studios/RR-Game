package com.runicrealms.game.gameplay.player

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.items.config.template.GameItemTag
import com.runicrealms.game.items.generator.ItemStackConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.plugin.Plugin

class HungerManager
@Inject
constructor(
    private val plugin: Plugin,
    private val itemStackConverter: ItemStackConverter,
    private val userDataRegistry: UserDataRegistry,
) : Listener {
    init {
        Bukkit.getPluginManager().registerSuspendingEvents(this, plugin)
        plugin.launch {
            withContext(plugin.minecraftDispatcher) {
                delay(HUNGER_TASK_DELAY_MILLIS)
                while (true) {
                    tickAllOnlinePlayersHunger()
                    delay(PLAYER_HUNGER_TIME_MILLIS)
                }
            }
        }
    }

    /** Prevent eating items which are not consumables */
    @EventHandler(priority = EventPriority.LOWEST) // first
    fun onFoodInteract(event: PlayerItemConsumeEvent) {
        val runicItem = itemStackConverter.convertToGameItem(event.item) ?: return
        val isConsumable = runicItem.template.tags.contains(GameItemTag.CONSUMABLE)
        if (!isConsumable) {
            event.isCancelled = true
            event.player.sendMessage(
                Component.text("I can't eat that!", Style.style(NamedTextColor.RED))
            )
        }
    }

    /** Prevents normal decay of hunger */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        if (event.foodLevel <= player.foodLevel) event.isCancelled = true
    }

    /** Reduces regen for players below half hunger */
    @EventHandler
    fun onHealthRegen(event: HealthRegenEvent) {
        val foodLevel: Int = event.player.foodLevel
        if (foodLevel <= INVIGORATED_HUNGER_THRESHOLD) {
            event.isCancelled = true
        } else if (foodLevel <= 10) {
            event.amount = (event.amount * HALF_HUNGER_REGEN_MULTIPLIER).toInt()
        }
    }

    private fun restoreHunger(player: Player) {
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f)
        player.sendMessage(
            Component.text(
                "You feel rested in the city! Your hunger has been sated.",
                Style.style(NamedTextColor.GREEN),
            )
        )
        player.foodLevel = 20
    }

    /**
     * Manually reduce player hunger. Either reduces player saturation if it exists, or reduces
     * player hunger value if there is no saturation
     */
    private fun tickAllOnlinePlayersHunger() {
        for (character in userDataRegistry.getAllCharacters()) {
            if (
                true
            ) { // TODO RunicCore.getRegionAPI().isSafezone(player.location)) { // prevent hunger
                // loss in capital cities
                if (character.bukkitPlayer.foodLevel < 20) {
                    restoreHunger(character.bukkitPlayer)
                }
                continue
            }
            if (character.bukkitPlayer.foodLevel <= STARVATION_HUNGER_LEVEL) continue
            if (character.bukkitPlayer.saturation > 0) continue
            character.bukkitPlayer.foodLevel -= 1
        }
    }

    companion object {
        private const val HUNGER_TASK_DELAY_MILLIS = 60000L
        private const val PLAYER_HUNGER_TIME_MILLIS = 60000L
        private const val INVIGORATED_HUNGER_THRESHOLD = 6 // hunger level to receive regen
        private const val STARVATION_HUNGER_LEVEL = 1
        private const val HALF_HUNGER_REGEN_MULTIPLIER = 0.5
    }
}
