package com.runicrealms.game.items

import com.google.inject.Inject
import com.runicrealms.game.common.config.GameYamlLoader
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
import org.bukkit.Material
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
    private val plugin: Plugin,
    private val gameItemArmorFactory: GameItemArmor.Factory,
    private val gameItemGemFactory: GameItemGem.Factory,
    private val gameItemGenericFactory: GameItemGeneric.Factory,
    private val gameItemOffhandFactory: GameItemOffhand.Factory,
    private val gameItemWeaponFactory: GameItemWeapon.Factory,
) : GameItemPerkTemplateRegistry, GameItemTemplateRegistry, ItemStackConverter {

    private val templates = HashMap<String, GameItemTemplate>()
    private val perks = HashMap<String, GameItemPerkTemplate>()

    internal fun readConfig() {
        // NOTE: if this is run when there already exist config files in the
        // maps, it just replaces/adds new ones and never removes them!
        val itemsFolder = File(plugin.dataFolder, "items").also { it.mkdirs() }
        val customFolder = File(itemsFolder, "custom").also { it.mkdirs() }
        val scriptFolder = File(itemsFolder, "script").also { it.mkdirs() }
        val perksFolder = File(itemsFolder, "perk").also { it.mkdirs() }

        val loader = GameYamlLoader()

        val customItems = loader.readYaml<GameItemTemplate>(customFolder).associateBy { it.id }
        val scriptItems = loader.readYaml<GameItemTemplate>(scriptFolder).associateBy { it.id }
        templates.putAll(customItems)
        templates.putAll(scriptItems)

        perks.putAll(
            loader.readYaml<GameItemPerkTemplate>(perksFolder).associateBy { it.identifier }
        )
    }

    init {
        readConfig()
    }

    override fun getItemTemplate(identifier: String): GameItemTemplate? {
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

    override fun getItemTemplates(): Collection<GameItemTemplate> = templates.values

    override fun getPerkTemplate(identifier: String): GameItemPerkTemplate? {
        return perks[identifier]
    }

    override fun getPerkTemplates(): Collection<GameItemPerkTemplate> = perks.values

    override fun convertToGameItem(itemStack: ItemStack): GameItem? {
        return generateGameItem(generateItemData(itemStack) ?: return null)
    }

    override fun generateItemData(itemStack: ItemStack): ItemData? {
        if (itemStack.type == Material.AIR) return null
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
