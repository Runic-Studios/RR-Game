package com.runicrealms.game.gameplay.character.util

import com.google.inject.Inject
import com.runicrealms.game.items.config.template.GameItemTemplate
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.ItemStackConverter
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.ItemStack

class SaveZoneRegistry
@Inject
constructor(
    private val templateRegistry: GameItemTemplateRegistry,
    private val itemStackConverter: ItemStackConverter,
) {
    // TODO make configurable

    val saveZones = mutableSetOf<SaveZoneLocation>()

    inner class SaveZoneLocation
    constructor(
        val identifier: String,
        val display: String,
        val location: Location,
        val template: GameItemTemplate?,
        val isCity: Boolean = true,
    ) {
        val itemStack: ItemStack? =
            if (template != null) templateRegistry.generateGameItem(template).generateItemStack(1)
            else null

        init {
            saveZones.add(this)
        }
    }

    val TUTORIAL =
        SaveZoneLocation(
            "tutorial",
            "Tutorial",
            Location(Bukkit.getWorld("Alterra"), -2277.5, 23.0, 1676.5, -250f, 0f),
            templateRegistry.getItemTemplate("hearthstone")!!,
        )

    val AZANA =
        SaveZoneLocation(
            "azana",
            "Azana",
            Location(Bukkit.getWorld("Alterra"), -764.5, 40.0, 206.5, 180f, 0f),
            templateRegistry.getItemTemplate("hearthstone-azana")!!,
        )

    val KOLDORE =
        SaveZoneLocation(
            "koldore",
            "Koldore",
            Location(Bukkit.getWorld("Alterra"), -1661.5, 35.0, 206.5, 270f, 0f),
            templateRegistry.getItemTemplate("hearthstone-koldore")!!,
        )

    val WHALETOWN =
        SaveZoneLocation(
            "whaletown",
            "Whaletown",
            Location(Bukkit.getWorld("Alterra"), -1834.5, 32.0, -654.5),
            templateRegistry.getItemTemplate("hearthstone-whaletown")!!,
        )

    val HILSTEAD =
        SaveZoneLocation(
            "hilstead",
            "Hilstead",
            Location(Bukkit.getWorld("Alterra"), -1649.5, 44.0, -2053.5, 270f, 0f),
            templateRegistry.getItemTemplate("hearthstone-hilstead")!!,
        )

    val WINTERVALE =
        SaveZoneLocation(
            "wintervale",
            "Wintervale",
            Location(Bukkit.getWorld("Alterra"), -1672.5, 37.0, -2639.5, 90f, 0f),
            templateRegistry.getItemTemplate("hearthstone-wintervale")!!,
        )
    val DAWNSHIRE_INN =
        SaveZoneLocation(
            "dawnshire_inn",
            "Dawnshire Inn",
            Location(Bukkit.getWorld("Alterra"), -306.5, 57.0, -408.5, 90f, 0f),
            templateRegistry.getItemTemplate("hearthstone-dawnshire-inn")!!,
        )

    val DEAD_MANS_REST =
        SaveZoneLocation(
            "dead_mans_rest",
            "Dead Man's Rest",
            Location(Bukkit.getWorld("Alterra"), -24.5, 32.0, -475.5, 90f, 0f),
            templateRegistry.getItemTemplate("hearthstone-dead-mans-rest")!!,
        )
    val ISFODAR =
        SaveZoneLocation(
            "isfodar",
            "Isfodar",
            Location(Bukkit.getWorld("Alterra"), 754.5, 113.0, -93.5, 0f, 0f),
            templateRegistry.getItemTemplate("hearthstone-isfodar")!!,
        )
    val TIRENEAS =
        SaveZoneLocation(
            "tireneas",
            "Tireneas",
            Location(Bukkit.getWorld("Alterra"), 887.5, 43.0, 547.5, 270f, 0f),
            templateRegistry.getItemTemplate("hearthstone-tireneas")!!,
        )

    val ZENYTH =
        SaveZoneLocation(
            "zenyth",
            "Zenyth",
            Location(Bukkit.getWorld("Alterra"), 1564.5, 38.0, -158.5, 180f, 0f),
            templateRegistry.getItemTemplate("hearthstone-zenyth")!!,
        )

    val NAHEEN =
        SaveZoneLocation(
            "naheen",
            "Naheen",
            Location(Bukkit.getWorld("Alterra"), 1981.5, 41.0, 239.5, 270f, 0f),
            templateRegistry.getItemTemplate("hearthstone-naheen")!!,
        )

    val NAZMORA =
        SaveZoneLocation(
            "nazmora",
            "Naz'mora",
            Location(Bukkit.getWorld("Alterra"), 2587.5, 33.0, 979.5, 270f, 0f),
            templateRegistry.getItemTemplate("hearthstone-nazmora")!!,
        )

    val ORC_OUTPOST =
        SaveZoneLocation(
            "orc_outpost",
            "Orc Outpost",
            Location(Bukkit.getWorld("Alterra"), 2587.5, 33.0, 979.5, 270f, 0f),
            null,
            false,
        )

    val STONEHAVEN =
        SaveZoneLocation(
            "stonehaven",
            "Stonehaven",
            Location(Bukkit.getWorld("Alterra"), -788.5, 37.0, 749.5, 90f, 0f),
            templateRegistry.getItemTemplate("hearthstone-stonehaven")!!,
        )

    val FROSTS_END =
        SaveZoneLocation(
            "frosts_end",
            "Frost's End",
            Location(Bukkit.getWorld("Alterra"), -788.5, 37.0, 749.5, 90f, 0f),
            null,
            false,
        )

    /**
     * Returns an enum based on a string identifier
     *
     * @param identifier the location of the hearthstone
     * @return an enum
     */
    fun getFromIdentifier(identifier: String): SaveZoneLocation {
        for (safeZoneLocation in saveZones) {
            if (safeZoneLocation.identifier == identifier) return safeZoneLocation
        }
        return TUTORIAL
    }

    /**
     * Returns the location of hearthstone based on its string identifier
     *
     * @param identifier the value that goes under 'location' in the yaml
     * @return a Location object
     */
    private fun getLocationFromIdentifier(identifier: String): Location {
        for (safeZoneLocation in saveZones) {
            if (!safeZoneLocation.isCity)
                continue // Ignore quest hubs which don't have hearthstones

            if (safeZoneLocation.identifier == identifier) return safeZoneLocation.location
        }
        return TUTORIAL.location
    }

    /**
     * Returns the location of a hearthstone based on the identifier of the item stack
     *
     * @param hearthstone is the player's hearthstone
     * @return a Location object
     */
    fun getLocationFromItemStack(hearthstone: ItemStack): Location? {
        val identifier =
            itemStackConverter
                .convertToGameItem(hearthstone)
                ?.template
                ?.extraProperties
                ?.get("location") as? String ?: return null
        return getLocationFromIdentifier(identifier)
    }
}
