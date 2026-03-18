package com.runicrealms.game.items.weaponskin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.model.WeaponData
import com.runicrealms.game.items.config.item.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.game.items.util.ItemDataUpdater
import java.io.File
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("items")

/**
 * Manages weapon skins: loading from configuration, checking eligibility, and applying or removing
 * skins from items.
 *
 * Config format (`weapon-skins.yml`) matches the old Java system exactly:
 * ```yaml
 * <skin-id>:
 *   material: STONE_SWORD
 *   skin-damage: 100
 *   name: "Cool Skin"
 *   class: WARRIOR            # optional
 *   achievement: some-id      # optional
 *   rank:                     # optional
 *     - MVP
 *   permission: some.permission # optional
 * ```
 */
@Singleton
class WeaponSkinManager
@Inject
constructor(
    private val plugin: Plugin,
    private val itemStackConverter: ItemStackConverter,
    private val templateRegistry: GameItemTemplateRegistry,
) {

    private val skinsById = mutableMapOf<String, WeaponSkin>()
    private val skinsByMaterial = mutableMapOf<Material, MutableList<WeaponSkin>>()

    init {
        loadConfig()
    }

    fun loadConfig() {
        skinsById.clear()
        skinsByMaterial.clear()

        val file = File(plugin.dataFolder, "weapon-skins.yml")
        if (!file.exists()) {
            logger.warn("weapon-skins.yml not found in {}", plugin.dataFolder.absolutePath)
            return
        }

        val config = YamlConfiguration.loadConfiguration(file)

        for (skinKey in config.getKeys(false)) {
            val section = config.getConfigurationSection(skinKey) ?: continue

            val materialName = section.getString("material")
            if (materialName == null) {
                logger.warn("Weapon skin '{}' is missing a material", skinKey)
                continue
            }

            val material =
                try {
                    Material.valueOf(materialName.uppercase())
                } catch (exception: IllegalArgumentException) {
                    logger.warn("Weapon skin '{}' has invalid material: {}", skinKey, materialName)
                    continue
                }

            val skin =
                WeaponSkin(
                    id = skinKey,
                    name = section.getString("name"),
                    material = material,
                    damage = section.getInt("skin-damage"),
                    classType = section.getString("class"),
                    permission = section.getString("permission"),
                    rank = section.getStringList("rank").ifEmpty { null },
                    achievementID = section.getString("achievement"),
                )

            skinsById[skinKey] = skin
            skinsByMaterial.getOrPut(material) { mutableListOf() }.add(skin)
        }

        logger.info("Loaded {} weapon skins", skinsById.size)
    }

    fun getSkin(id: String): WeaponSkin? = skinsById[id]

    fun getMaterialSkins(material: Material): List<WeaponSkin> =
        skinsByMaterial[material] ?: emptyList()

    fun canActivateSkin(player: Player, skin: WeaponSkin): Boolean {
        if (skin.permission != null && !player.hasPermission(skin.permission)) return false
        // TODO: Check DonorRank when implemented
        // TODO: Check achievement when implemented
        return true
    }

    fun activateSkin(player: Player, skin: WeaponSkin) {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (item.type != skin.material) continue
            applySkin(item, skin)
            inventory.setItem(slot, item)
        }
    }

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
     * Applies the weapon skin's damage value to the given item stack and records the [skinID] in
     * the item's [WeaponData] CBOR data.
     */
    fun applySkin(itemStack: ItemStack, skin: WeaponSkin) {
        val meta = itemStack.itemMeta as? Damageable ?: return
        meta.damage = skin.damage
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
     * Removes any active weapon skin from the given item stack. Resets damage to the template's
     * base damage value (from [DisplayableItem.damage]), or 0 if the template has none.
     */
    fun removeSkin(itemStack: ItemStack) {
        val meta = itemStack.itemMeta as? Damageable ?: return

        // Look up the template to restore the base display damage
        val itemData = itemStackConverter.generateItemData(itemStack)
        val baseDamage =
            if (itemData != null) {
                templateRegistry.getItemTemplate(itemData.templateID)?.display?.damage?.toInt() ?: 0
            } else {
                0
            }

        meta.damage = baseDamage
        itemStack.itemMeta = meta

        ItemDataUpdater.updateItemData(itemStack) { data ->
            val typeData = data.typeData
            if (typeData is WeaponData) {
                data.copy(typeData = typeData.copy(skinID = null))
            } else {
                data
            }
        }
    }
}
