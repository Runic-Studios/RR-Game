package com.runicrealms.game.gameplay.player.charselect

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.extension.getInfo
import com.runicrealms.game.data.util.MAX_CHARACTERS
import com.runicrealms.trove.client.user.UserCharactersTraits
import de.tr7zw.nbtapi.NBT
import java.time.Duration
import java.util.ArrayList
import java.util.function.Supplier
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import nl.odalitadevelopments.menus.OdalitaMenus
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.contents.action.MenuCloseResult
import nl.odalitadevelopments.menus.contents.pos.SlotPos
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.items.DisplayItem
import nl.odalitadevelopments.menus.items.buttons.OpenMenuItem
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

@Menu(title = "Select Your Character", type = MenuType.CHEST_3_ROW)
class CharacterSelectMenu
@AssistedInject
constructor(
    private val userDataRegistry: UserDataRegistry,
    private val plugin: Plugin,
    private val characterSelectHelper: CharacterSelectHelper,
    private val odalitaMenus: OdalitaMenus,
    @Assisted private val userCharactersTraits: UserCharactersTraits,
) : PlayerMenuProvider {

    interface Factory {
        fun create(userCharactersTraits: UserCharactersTraits): CharacterSelectMenu
    }

    @Inject private lateinit var characterAddMenuFactory: CharacterAddMenu.Factory

    @Inject private lateinit var characterDeleteMenuFactory: CharacterDeleteMenu.Factory

    @Inject private lateinit var characterSelectManager: CharacterSelectManager

    @Volatile private var hasSelected = false

    private fun createSelectIcon(slot: Int): ItemStack {
        val characterData =
            userCharactersTraits.characters[slot]?.data ?: return ItemStack(Material.AIR)
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

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val slots = 5 // TODO DonorRank.getDonorRank(player).getClassSlots()
        var addedSlots = 0
        for (i in 0 until MAX_CHARACTERS) {
            val slotPos = SlotPos.of(i / 5, i % 5 + 2)
            val traits = userCharactersTraits.characters[i]
            if (traits != null) {
                menuContents.set(
                    slotPos,
                    ClickableItem.of(createSelectIcon(i)) { event ->
                        if (!event.isRightClick) {
                            if (hasSelected) return@of
                            hasSelected = true
                            event.whoClicked.closeInventory()
                            val slot =
                                NBT.readNbt(event.currentItem).getInteger("slot") ?: return@of
                            characterSelectManager.setLoading(player, true)
                            plugin.launch {
                                userDataRegistry.setCharacter(player.uniqueId, slot)
                                characterSelectManager.setLoading(player, false)
                                player.showTitle(
                                    Title.title(
                                        "&6Welcome to the Realm!".colorFormat(),
                                        Component.empty(),
                                        Title.Times.times(
                                            Duration.ZERO,
                                            Duration.ofMillis(1500),
                                            Duration.ofMillis(500),
                                        ),
                                    )
                                )
                            }
                        } else {
                            odalitaMenus.openMenu(
                                characterDeleteMenuFactory.create(i, userCharactersTraits),
                                event.whoClicked as Player,
                            )
                        }
                    },
                )
                addedSlots++
            } else {
                if (addedSlots < slots) {
                    menuContents.set(
                        slotPos,
                        OpenMenuItem.of(
                            characterSelectHelper.characterCreateItem,
                            characterAddMenuFactory.create(i, userCharactersTraits),
                        ),
                    )
                    addedSlots++
                } else {
                    val icon =
                        when {
                            i >= 8 -> characterSelectHelper.onlyChampionCreateItem
                            i >= 6 -> characterSelectHelper.onlyHeroCreateItem
                            i == 5 -> characterSelectHelper.onlyKnightCreateItem
                            else -> null
                        } ?: continue
                    menuContents.set(slotPos, DisplayItem.of(icon))
                }
            }
        }

        menuContents.set(
            SlotPos.of(2, 4),
            ClickableItem.of(characterSelectHelper.exitGameItem) { event ->
                event.whoClicked.closeInventory()
                player.kick("&aGoodbye!".colorFormat())
            },
        )

        menuContents
            .events()
            .onClose(
                Supplier<MenuCloseResult> {
                    if (hasSelected) MenuCloseResult.CLOSE else MenuCloseResult.KEEP_OPEN
                }
            )
    }
}
