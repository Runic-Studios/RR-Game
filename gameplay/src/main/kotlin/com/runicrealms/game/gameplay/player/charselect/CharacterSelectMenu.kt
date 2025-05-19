package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.addOnClick
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.extension.getInfo
import com.runicrealms.game.data.util.MAX_CHARACTERS
import com.runicrealms.trove.client.user.UserCharactersTraits
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import de.tr7zw.nbtapi.NBT
import java.util.ArrayList
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem
import xyz.xenondevs.invui.window.Window

class CharacterSelectMenu
@AssistedInject
constructor(
    private val userDataRegistry: UserDataRegistry,
    private val plugin: Plugin,
    private val characterSelectManager: CharacterSelectManager,
    private val characterSelectHelper: CharacterSelectHelper,
    @Assisted private val player: Player,
    @Assisted private val charactersTraits: UserCharactersTraits,
) {

    interface Factory {
        fun create(player: Player, charactersTraits: UserCharactersTraits): CharacterSelectMenu
    }

    @Volatile private var hasSelected = false

    fun openSelect() {
        val gui =
            Gui.normal().setStructure(". . 0 1 2 3 4 . .", ". . 5 6 7 8 9 . .", "# # # # e # # # #")

        val slots = 5 // TODO DonorRank.getDonorRank(player).getClassSlots()
        var addedSlots = 0
        for (i in 0 until MAX_CHARACTERS) {
            val ingredientChar = i.toString().first()
            val traits = charactersTraits.characters[i]
            if (traits != null) {
                gui.addIngredient(
                    ingredientChar,
                    createSelectIcon(i).addOnClick { _, _, event ->
                        closeForAllViewers()
                        if (hasSelected) return@addOnClick
                        hasSelected = true
                        val slot =
                            NBT.readNbt(event.currentItem).getInteger("slot") ?: return@addOnClick
                        plugin.launch { userDataRegistry.setCharacter(player.uniqueId, slot) }
                    },
                )
                addedSlots++
            } else {
                if (addedSlots < slots) {
                    gui.addIngredient(
                        ingredientChar,
                        characterSelectHelper.characterCreateItem.addOnClick { _, _, _ -> },
                    )
                } else {
                    val icon =
                        when {
                            i >= 8 -> characterSelectHelper.onlyChampionCreateItem
                            i >= 6 -> characterSelectHelper.onlyHeroCreateItem
                            i == 5 -> characterSelectHelper.onlyKnightCreateItem
                            else -> null
                        } ?: continue
                    gui.addIngredient(ingredientChar, icon)
                }
            }
        }

        gui.addIngredient(
            'e',
            characterSelectHelper.exitGameItem.addOnClick { _, _, _ ->
                closeForAllViewers()
                player.kick()
            },
        )
        Window.single().setGui(gui).build(player).open()
    }

    fun openDelete(slot: Int) {
        val gui =
            Gui.normal()
                .setStructure(". . b . . . c . .")
                .addIngredient(
                    'b',
                    characterSelectHelper.goBackItem.addOnClick { _, _, _ -> openSelect() },
                )
                .addIngredient(
                    'c',
                    characterSelectHelper.confirmDeleteItem.addOnClick { _, _, _ ->
                        closeForAllViewers()
                        // TODO display deleting character title
                        plugin.launch { deleteCharacter(slot) }
                    },
                )
        Window.single().setGui(gui).build(player).open()
    }

    fun openAdd(slot: Int) {
        val gui =
            Gui.normal()
                .setStructure("b . a c m r w . .")
                .addIngredient(
                    'b',
                    characterSelectHelper.goBackItem.addOnClick { _, _, _ -> openSelect() },
                )
                .addIngredient('a', createChooseClassIcon(ClassType.ARCHER, slot))
                .addIngredient('c', createChooseClassIcon(ClassType.CLERIC, slot))
                .addIngredient('m', createChooseClassIcon(ClassType.MAGE, slot))
                .addIngredient('r', createChooseClassIcon(ClassType.ROGUE, slot))
                .addIngredient('w', createChooseClassIcon(ClassType.WARRIOR, slot))
        Window.single().setGui(gui).build(player).open()
    }

    private suspend fun deleteCharacter(slot: Int) {
        // TODO actually implement this with trove
        withContext(plugin.asyncDispatcher) {
            player.sendMessage(
                Component.text(
                    "Deleting characters is not currently supported",
                    Style.style(NamedTextColor.RED),
                )
            )
            delay(2500)
            openSelect()
        }
    }

    private fun createSelectIcon(slot: Int): ItemStack {
        val characterData =
            charactersTraits.characters[slot]?.data ?: return ItemStack(Material.AIR)
        val item =
            CharacterSelectHelper.ClassIcon.fromClassType(characterData.classType).itemStack.clone()
        val meta = item.itemMeta!!
        val classTypeInfo = characterData.classType.getInfo()
        meta.displayName(
            Component.text(
                classTypeInfo.name,
                Style.style(NamedTextColor.GREEN, TextDecoration.BOLD),
            )
        )
        meta.isUnbreakable = true
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        val lore = ArrayList<TextComponent>(3)
        lore.add(
            Component.text()
                .append(Component.text("Level: ", Style.style(NamedTextColor.GRAY)))
                .append(Component.text(characterData.level, Style.style(NamedTextColor.GREEN)))
                .build()
        )
        lore.add(
            Component.text()
                .append(Component.text("Exp: ", Style.style(NamedTextColor.GRAY)))
                .append(Component.text(characterData.exp, Style.style(NamedTextColor.GREEN)))
                .build()
        )
        lore.add(Component.text("[Right click] to delete", Style.style(NamedTextColor.GREEN)))
        meta.lore(lore)
        item.setItemMeta(meta)
        NBT.modify(item) { it.setInteger("slot", slot) }
        return item
    }

    private fun createChooseClassIcon(classType: ClassType, slot: Int): ControlItem<Gui> {
        return characterSelectHelper.classIcons[classType]!!.addOnClick { _, _, _ ->
            characterSelectManager.creationCharacterTypes[player.uniqueId] = classType
            plugin.launch { userDataRegistry.setCharacter(player.uniqueId, slot) }
        }
    }
}
