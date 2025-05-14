package com.runicrealms.game.items

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.inject.Inject
import com.runicrealms.game.items.config.perk.GameItemPerkTemplate
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.game.items.config.template.GameItemTemplate
import com.runicrealms.game.items.config.template.GameItemTemplateRegistry
import com.runicrealms.game.items.generator.GameItem
import com.runicrealms.game.items.generator.GameItemArmor
import com.runicrealms.game.items.generator.GameItemGem
import com.runicrealms.game.items.generator.GameItemGeneric
import com.runicrealms.game.items.generator.GameItemOffhand
import com.runicrealms.game.items.generator.GameItemWeapon
import com.runicrealms.game.items.generator.ItemStackConverter
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import de.tr7zw.nbtapi.NBT
import java.io.File
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Loads the YAML files into jackson pojos.
 *
 * Also responsible for mapping between different item data types.
 */
class GameItemManager
@Inject
constructor(
    plugin: Plugin,
    private val gameItemArmorFactory: GameItemArmor.Factory,
    private val gameItemGemFactory: GameItemGem.Factory,
    private val gameItemGenericFactory: GameItemGeneric.Factory,
    private val gameItemOffhandFactory: GameItemOffhand.Factory,
    private val gameItemWeaponFactory: GameItemWeapon.Factory,
) : GameItemPerkTemplateRegistry, GameItemTemplateRegistry, ItemStackConverter {

    private val templates = HashMap<String, GameItemTemplate>()
    private val perks: Map<String, GameItemPerkTemplate>

    init {
        val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val extensions = listOf("yml", "yaml")

        val itemsTemplates =
            File(plugin.dataFolder, "items")
                .walkTopDown()
                .filter { it.extension in extensions }
                .map { file -> yamlMapper.readValue(file, GameItemTemplate::class.java) }
                .associateBy { it.id }
        templates.putAll(itemsTemplates)
        val scriptTemplates =
            File(plugin.dataFolder, "scripts")
                .walkTopDown()
                .filter { it.extension in extensions }
                .map { file -> yamlMapper.readValue(file, GameItemTemplate::class.java) }
                .associateBy { it.id }
        templates.putAll(scriptTemplates)

        perks =
            File(plugin.dataFolder, "itemperks")
                .walkTopDown()
                .filter { it.extension in extensions }
                .map { file -> yamlMapper.readValue(file, GameItemPerkTemplate::class.java) }
                .associateBy { it.identifier }
    }

    override fun getTemplate(identifier: String): GameItemTemplate? {
        return templates[identifier]
    }

    override fun generateGameItem(itemData: ItemData): GameItem {
        return when (itemData.typeDataCase) {
            ItemData.TypeDataCase.ARMOR -> gameItemArmorFactory.create(itemData)
            ItemData.TypeDataCase.GEM -> gameItemGemFactory.create(itemData)
            ItemData.TypeDataCase.OFFHAND -> gameItemOffhandFactory.create(itemData)
            ItemData.TypeDataCase.WEAPON -> gameItemWeaponFactory.create(itemData)
            ItemData.TypeDataCase.TYPEDATA_NOT_SET -> gameItemGenericFactory.create(itemData)
            null -> gameItemGenericFactory.create(itemData)
        }
    }

    override fun getGameItemPerkTemplate(identifier: String): GameItemPerkTemplate? {
        return perks[identifier]
    }

    override fun convertToGameItem(itemStack: ItemStack): GameItem? {
        return generateGameItem(generateItemData(itemStack) ?: return null)
    }

    override fun generateItemData(itemStack: ItemStack): ItemData? {
        val byteData = NBT.readNbt(itemStack).getByteArray("data") ?: return null
        try {
            val itemData = ItemData.parseFrom(byteData)
            return itemData
        } catch (exception: Exception) {
            exception.printStackTrace()
            return null
        }
    }
}
