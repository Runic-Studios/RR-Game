package com.runicrealms.game.gameplay.player.charselect

import SubClassType
import com.google.inject.Inject
import com.runicrealms.game.common.breakLines
import com.runicrealms.game.common.colorFormat
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import java.util.HashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class CharacterSelectHelper @Inject constructor() {

    val characterCreateItem: ItemStack = ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1)
    val onlyKnightCreateItem: ItemStack
    val onlyHeroCreateItem: ItemStack
    val onlyChampionCreateItem: ItemStack
    val goBackItem: ItemStack
    val confirmDeleteItem: ItemStack
    val exitGameItem: ItemStack

    val classIcons = HashMap<ClassType, ItemStack>()

    init {
        val creationMeta = checkNotNull(characterCreateItem.itemMeta)
        creationMeta.isUnbreakable = true
        creationMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        creationMeta.displayName(
            Component.text("Create a Class", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD))
        )
        characterCreateItem.setItemMeta(creationMeta)

        onlyKnightCreateItem = ItemStack(Material.BARRIER, 1)
        val knightMeta = checkNotNull(onlyKnightCreateItem.itemMeta)
        knightMeta.isUnbreakable = true
        knightMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        knightMeta.displayName(
            Component.text()
                .append(
                    Component.text(
                        "You need ",
                        Style.style(NamedTextColor.RED, TextDecoration.BOLD),
                    )
                )
                .append(Component.text("Knight", Style.style(NamedTextColor.AQUA, TextDecoration.BOLD)))
                .append(Component.text(" rank to use this slot", Style.style(NamedTextColor.RED, TextDecoration.BOLD)))
                .build()
        )
        onlyKnightCreateItem.setItemMeta(knightMeta)

        onlyHeroCreateItem = ItemStack(Material.BARRIER, 1)
        val heroMeta = checkNotNull(onlyHeroCreateItem.itemMeta)
        heroMeta.isUnbreakable = true
        heroMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        heroMeta.displayName(
            Component.text()
                .append(
                    Component.text(
                        "You need ",
                        Style.style(NamedTextColor.RED, TextDecoration.BOLD),
                    )
                )
                .append(Component.text("Hero", Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)))
                .append(Component.text(" rank to use this slot", Style.style(NamedTextColor.RED, TextDecoration.BOLD)))
                .build()
        )
        onlyHeroCreateItem.setItemMeta(heroMeta)

        onlyChampionCreateItem = ItemStack(Material.BARRIER, 1)
        val championMeta = checkNotNull(onlyChampionCreateItem.itemMeta)
        championMeta.isUnbreakable = true
        championMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        championMeta.displayName(
            Component.text()
                .append(
                    Component.text(
                        "You need ",
                        Style.style(NamedTextColor.RED, TextDecoration.BOLD),
                    )
                )
                .append(
                    Component.text(
                        "Champion",
                        Style.style(NamedTextColor.DARK_PURPLE, TextDecoration.BOLD),
                    )
                )
                .append(
                    Component.text(
                        " rank to use this slot",
                        Style.style(NamedTextColor.RED, TextDecoration.BOLD),
                    )
                )
                .build()
        )
        onlyChampionCreateItem.setItemMeta(championMeta)

        goBackItem = ItemStack(Material.BARRIER)
        val goBackMeta = checkNotNull(goBackItem.itemMeta)
        goBackMeta.isUnbreakable = true
        goBackMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        goBackMeta.displayName(
            Component.text("Cancel", Style.style(NamedTextColor.RED, TextDecoration.BOLD))
        )
        goBackItem.setItemMeta(goBackMeta)

        confirmDeleteItem = ItemStack(Material.SLIME_BALL)
        val confirmDeletionMeta = checkNotNull(confirmDeleteItem.itemMeta)
        confirmDeletionMeta.isUnbreakable = true
        confirmDeletionMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        confirmDeletionMeta.displayName(
            Component.text("Confirm Deletion", Style.style(NamedTextColor.RED, TextDecoration.BOLD))
        )
        confirmDeletionMeta.lore(
            listOf(
                Component.text(
                    "WARNING: There is no going back!",
                    Style.style(NamedTextColor.DARK_RED),
                )
            )
        )
        confirmDeleteItem.setItemMeta(confirmDeletionMeta)

        val archerItem = ClassIcon.ARCHER_ICON.itemStack.clone()
        val archerMeta = checkNotNull(archerItem.itemMeta)
        archerMeta.displayName(
            Component.text("Archer ⚔", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD))
        )

        val archerLore = mutableListOf<Component>()

        archerLore += Component.text("", NamedTextColor.GRAY)
        archerLore += Component.text("● Long-range", NamedTextColor.GOLD)
        archerLore += Component.text("● Bowman", NamedTextColor.GOLD)
        archerLore += Component.text("● Single Target", NamedTextColor.GOLD)
        archerLore += Component.text("", NamedTextColor.GRAY)

        // Assuming ChatUtils.formattedText returns a List<String>, we map it to Components
        archerLore +=
            "The archer features a diverse array of damage, mobility, and utility spells, a master of natural terrain and single combat!"
                .breakLines()
                .map { Component.text(it, Style.style(NamedTextColor.GRAY)) }

        archerLore += Component.text("") // blank line
        archerLore +=
            Component.text(
                "Subclasses: ",
                Style.style(NamedTextColor.DARK_AQUA, TextDecoration.BOLD),
            )

        SubClassType.ARCHER_SUBCLASSES.forEach { subClass ->
            archerLore += Component.text("● ${subClass.text}", NamedTextColor.AQUA)
        }

        archerMeta.lore(archerLore)
        archerItem.setItemMeta(archerMeta)
        classIcons[ClassType.ARCHER] = archerItem

        val clericItem = ClassIcon.CLERIC_ICON.itemStack.clone()
        val clericMeta = checkNotNull(clericItem.itemMeta)
        clericMeta.isUnbreakable = true
        clericMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES)
        clericMeta.displayName(
            Component.text("Cleric ✦", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD))
        )

        val clericLore = mutableListOf<Component>()
        clericLore += Component.empty()
        clericLore += Component.text("● Close-range", Style.style(NamedTextColor.GOLD))
        clericLore += Component.text("● Healer", Style.style(NamedTextColor.GOLD))
        clericLore += Component.text("● Area-of-effect", Style.style(NamedTextColor.GOLD))
        clericLore += Component.empty()
        clericLore +=
            "The cleric enjoys a range of crowd control, healing, and utility spells, bolstering any party!"
                .breakLines()
                .map { Component.text(it, Style.style(NamedTextColor.GRAY)) }
        clericLore += Component.empty()
        clericLore +=
            Component.text(
                "Subclasses: ",
                Style.style(NamedTextColor.DARK_AQUA, TextDecoration.BOLD),
            )
        SubClassType.CLERIC_SUBCLASSES.forEach { subClass ->
            clericLore += Component.text("● ${subClass.text}", Style.style(NamedTextColor.AQUA))
        }
        clericMeta.lore(clericLore)
        clericItem.setItemMeta(clericMeta)
        classIcons[ClassType.CLERIC] = clericItem

        val mageItem = ClassIcon.MAGE_ICON.itemStack.clone()
        val mageMeta = checkNotNull(mageItem.itemMeta)
        mageMeta.displayName(
            Component.text("Mage ʔ", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD))
        )

        val mageLore = mutableListOf<Component>()
        mageLore += Component.empty()
        mageLore += Component.text("● Medium-range", Style.style(NamedTextColor.GOLD))
        mageLore += Component.text("● Caster", Style.style(NamedTextColor.GOLD))
        mageLore += Component.text("● Area-of-effect", Style.style(NamedTextColor.GOLD))
        mageLore += Component.empty()
        mageLore +=
            "The mage is a master of widespread damage, controlling the flow of battle and deadly if left unchecked in the back lines!"
                .breakLines()
                .map { Component.text(it, Style.style(NamedTextColor.GRAY)) }
        mageLore += Component.empty()
        mageLore +=
            Component.text(
                "Subclasses: ",
                Style.style(NamedTextColor.DARK_AQUA, TextDecoration.BOLD),
            )
        SubClassType.MAGE_SUBCLASSES.forEach { subClass ->
            mageLore += Component.text("● ${subClass.text}", Style.style(NamedTextColor.AQUA))
        }

        mageMeta.lore(mageLore)
        mageItem.setItemMeta(mageMeta)
        classIcons[ClassType.MAGE] = mageItem

        val rogueItem = ClassIcon.ROGUE_ICON.itemStack.clone()
        val rogueMeta = checkNotNull(rogueItem.itemMeta)
        rogueMeta.displayName(
            Component.text("Rogue ⚔", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD))
        )

        val rogueLore = mutableListOf<Component>()
        rogueLore += Component.empty()
        rogueLore += Component.text("● Close-range", Style.style(NamedTextColor.GOLD))
        rogueLore += Component.text("● Fighter", Style.style(NamedTextColor.GOLD))
        rogueLore += Component.text("● Single Target", Style.style(NamedTextColor.GOLD))
        rogueLore += Component.empty()
        rogueLore +=
            "The rogue does not play fair, arming itself with a set of stealth and mobility to engage any foe!"
                .breakLines()
                .map { Component.text(it, Style.style(NamedTextColor.GRAY)) }
        rogueLore += Component.empty()
        rogueLore +=
            Component.text(
                "Subclasses: ",
                Style.style(NamedTextColor.DARK_AQUA, TextDecoration.BOLD),
            )
        SubClassType.ROGUE_SUBCLASSES.forEach { subClass ->
            rogueLore += Component.text("● ${subClass.text}", Style.style(NamedTextColor.AQUA))
        }
        rogueMeta.lore(rogueLore)
        rogueItem.setItemMeta(rogueMeta)
        classIcons[ClassType.ROGUE] = rogueItem

        val warriorItem = ClassIcon.WARRIOR_ICON.itemStack.clone()
        val warriorMeta = checkNotNull(warriorItem.itemMeta)
        warriorMeta.displayName(
            Component.text("Warrior ■", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD))
        )

        val warriorLore = mutableListOf<Component>()
        warriorLore += Component.empty()
        warriorLore += Component.text("● Close-range", Style.style(NamedTextColor.GOLD))
        warriorLore += Component.text("● Tank", Style.style(NamedTextColor.GOLD))
        warriorLore += Component.text("● Single Target", Style.style(NamedTextColor.GOLD))
        warriorLore += Component.empty()
        warriorLore +=
            "The warrior is a force to be reckoned with, featuring both offensive and defensive spells to charge into the front lines!"
                .breakLines()
                .map { Component.text(it, Style.style(NamedTextColor.GRAY)) }
        warriorLore += Component.empty()
        warriorLore +=
            Component.text(
                "Subclasses: ",
                Style.style(NamedTextColor.DARK_AQUA, TextDecoration.BOLD),
            )
        SubClassType.WARRIOR_SUBCLASSES.forEach { subClass ->
            warriorLore += Component.text("● ${subClass.text}", Style.style(NamedTextColor.AQUA))
        }

        warriorMeta.lore(warriorLore)
        warriorItem.setItemMeta(warriorMeta)
        classIcons[ClassType.WARRIOR] = warriorItem

        exitGameItem = ItemStack(Material.OAK_DOOR, 1)
        val exitGameItemMeta = checkNotNull(exitGameItem.itemMeta)
        exitGameItemMeta.displayName("&r&cLeave the Realm".colorFormat())
        exitGameItem.setItemMeta(exitGameItemMeta)
    }

    enum class ClassIcon(
        private val material: Material,
        private val damage: Int,
        private val classType: ClassType,
    ) {
        ARCHER_ICON(Material.BOW, 11, ClassType.ARCHER),
        CLERIC_ICON(Material.STONE_SHOVEL, 1, ClassType.CLERIC),
        WARRIOR_ICON(Material.STONE_AXE, 1, ClassType.WARRIOR),
        MAGE_ICON(Material.STONE_HOE, 1, ClassType.MAGE),
        ROGUE_ICON(Material.STONE_SWORD, 1, ClassType.ROGUE);

        val itemStack = ItemStack(material)

        init {
            val meta = itemStack.itemMeta
            if (meta is Damageable) {
                meta.damage = damage
            }
            meta.isUnbreakable = true
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES)
            itemStack.setItemMeta(meta)
        }

        companion object {
            fun fromClassType(classType: ClassType): ClassIcon {
                return entries.singleOrNull { it.classType == classType }
                    ?: throw IllegalArgumentException(
                        "Couldn't find class icon for class type $classType"
                    )
            }
        }
    }
}
