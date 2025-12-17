package com.runicrealms.game.items.dynamic.placeholder

import com.google.inject.Inject
import com.runicrealms.game.common.util.TextIcons
import com.runicrealms.game.data.event.GameCharacterPreLoadEvent
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.config.item.ClassTypeRequirementHolder
import com.runicrealms.game.items.dynamic.DynamicItemRegistry
import com.runicrealms.game.items.dynamic.DynamicItemTextPlaceholder
import com.runicrealms.game.items.generator.GameItem
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/** Replaces \<class> on items with an X or a check mark */
class DynamicItemClassRequirementTextPlaceholder
@Inject
constructor(plugin: Plugin, dynamicItemRegistry: DynamicItemRegistry) :
    DynamicItemTextPlaceholder("class"), Listener {

    // Note that we cache classes because we want to be able to access them safely
    // Dynamic generateReplacement is called ASYNC: GameCharacter data can only be accessed sync
    private val cachedClasses = HashMap<UUID, ClassType>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        dynamicItemRegistry.registerTextPlaceholder(this)
    }

    @EventHandler(
        priority = EventPriority.HIGHEST
    ) // Run after we have set player's class (if new player)
    fun onCharacterSelect(
        event: GameCharacterPreLoadEvent
    ) { // Run on character pre-load so that we are prepared to modify items on character load
        cachedClasses[event.user] = event.characterData.traits.data.classType
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cachedClasses.remove(event.player.uniqueId)
    }

    override fun generateReplacement(
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String? {
        val template = gameItem.template
        if (template !is ClassTypeRequirementHolder) return null
        val classType = cachedClasses[viewer.bukkitPlayer.uniqueId] ?: return null

        return if (template.classType == classType) {
            TextIcons.CHECKMARK_ICON
        } else {
            TextIcons.X_ICON
        }
    }
}
