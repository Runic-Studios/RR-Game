package com.runicrealms.game.items.weaponskin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.model.WeaponData
import com.runicrealms.game.items.util.ItemDataUpdater
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Manages weapon skins: loading from configuration, checking eligibility,
 * and applying or removing skins from items.
 */
@Singleton
class WeaponSkinManager
@Inject
constructor(
    private val plugin: Plugin,
) {

    private val logger = LoggerFactory.getLogger("items")

    private val skinsById = mutableMapOf<String, WeaponSkin>()
    private val skinsByMaterial = mutableMapOf<Material, MutableList<WeaponSkin>>()

    init {
        loadConfig()
    }

    /**
     * Loads weapon skins from the `weapon-skins.yml` configuration file.
     */
    fun loadConfig() {
        skinsById.clear()
        skinsByMaterial.clear()

        val file = File(plugin.dataFolder, "weapon-skins.yml")
        if (!file.exists()) {
            logger.warn("weapon-skins.yml not found in {}", plugin.dataFolder.absolutePath)
            return
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val skinsSection = config.getConfigurationSection("skins") ?: run {
            logger.warn("No 'skins' section found in weapon-skins.yml")
            return
        }

        for (skinKey in skinsSection.getKeys(false)) {
            val section = skinsSection.getConfigurationSection(skinKey) ?: continue

            val materialName = section.getString("material") ?: run {
                logger.warn("Weapon skin '{}' is missing a material", skinKey)
                continue
            }

            val material = try {
                Material.valueOf(materialName.uppercase())
            } catch (exception: IllegalArgumentException) {
                logger.warn("Weapon skin '{}' has invalid material: {}", skinKey, materialName)
                continue
            }

            val skin = WeaponSkin(
                id = skinKey,
                material = material,
                permission = section.getString("permission"),
                donorRank = section.getStringList("donor-rank").ifEmpty { null },
                achievementID = section.getString("achievement-id"),
                customModelData = section.getInt("custom-model-data", 0),
            )

            skinsById[skinKey] = skin
            skinsByMaterial.getOrPut(material) { mutableListOf() }.add(skin)
        }

        logger.info("Loaded {} weapon skins", skinsById.size)
    }

    /**
     * Returns the weapon skin with the given ID, or null if not found.
     */
    fun getSkin(id: String): WeaponSkin? = skinsById[id]

    /**
     * Returns all weapon skins available for the given material.
     */
    fun getMaterialSkins(material: Material): List<WeaponSkin> =
        skinsByMaterial[material] ?: emptyList()

    /**
     * Checks whether a player can activate the given weapon skin.
     */
    fun canActivateSkin(player: Player, skin: WeaponSkin): Boolean {
        // Check permission if one is specified
        if (skin.permission != null && !player.hasPermission(skin.permission)) {
            return false
        }

        // TODO: Check DonorRank when implemented
        // if (skin.donorRank != null) { ... }

        // TODO: Check achievement when implemented
        // if (skin.achievementID != null) { ... }

        return true
    }

    /**
     * Applies the given skin to all matching weapons in the player's inventory.
     */
    fun activateSkin(player: Player, skin: WeaponSkin) {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (item.type != skin.material) continue
            applySkin(item, skin)
            inventory.setItem(slot, item)
        }
    }

    /**
     * Removes the given skin from all matching weapons in the player's inventory.
     */
    fun deactivateSkin(player: Player, skin: WeaponSkin) {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (item.type != skin.material) continue
            removeSkin(item)
            inventory.setItem(slot, item)
        }
    }

    /**
     * Applies the weapon skin's custom model data to the given item stack and
     * updates the skinID in the item's [WeaponData].
     */
    fun applySkin(itemStack: ItemStack, skin: WeaponSkin) {
        val meta = itemStack.itemMeta ?: return
        meta.setCustomModelData(skin.customModelData)
        itemStack.itemMeta = meta

        ItemDataUpdater.updateItemData(itemStack) { itemData ->
            val typeData = itemData.typeData
            if (typeData is WeaponData) {
                itemData.copy(typeData = typeData.copy(skinID = skin.id))
            } else {
                itemData
            }
        }
    }

    /**
     * Removes custom model data from the given item stack and clears the skinID
     * in the item's [WeaponData].
     */
    fun removeSkin(itemStack: ItemStack) {
        val meta = itemStack.itemMeta ?: return
        meta.setCustomModelData(null)
        itemStack.itemMeta = meta

        ItemDataUpdater.updateItemData(itemStack) { itemData ->
            val typeData = itemData.typeData
            if (typeData is WeaponData) {
                itemData.copy(typeData = typeData.copy(skinID = null))
            } else {
                itemData
            }
        }
    }
}
