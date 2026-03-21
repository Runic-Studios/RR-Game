package com.runicrealms.game.gameplay.player.ui

import com.google.inject.assistedinject.AssistedInject
import com.runicrealms.game.common.StatType
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.common.util.toLoreComponents
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.character.util.CharacterHealthHelper
import com.runicrealms.game.gameplay.character.util.CharacterLevelHelper
import com.runicrealms.game.gameplay.player.stat.StatConstants
import com.runicrealms.game.gameplay.player.stat.StatManager
import java.text.DecimalFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import nl.odalitadevelopments.menus.annotations.Menu
import nl.odalitadevelopments.menus.contents.MenuContents
import nl.odalitadevelopments.menus.items.ClickableItem
import nl.odalitadevelopments.menus.items.DisplayItem
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider
import nl.odalitadevelopments.menus.menu.type.MenuType
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@Menu(title = "Character Stats", type = MenuType.CHEST_6_ROW)
class StatsMenu
@AssistedInject
constructor(
    private val statManager: StatManager,
    private val userDataRegistry: UserDataRegistry,
    private val characterLevelHelper: CharacterLevelHelper,
) : PlayerMenuProvider {

    interface Factory {
        fun create(): StatsMenu
    }

    companion object {
        private val STAT_ITEM_SLOTS = intArrayOf(20, 22, 24, 30, 32)
        private val DECIMAL_FORMAT = DecimalFormat("0.00")

        private val STAT_MATERIALS =
            mapOf(
                StatType.DEXTERITY to Material.QUARTZ,
                StatType.INTELLIGENCE to Material.LAPIS_LAZULI,
                StatType.STRENGTH to Material.REDSTONE,
                StatType.VITALITY to Material.DIAMOND,
                StatType.WISDOM to Material.EMERALD,
            )

        private val STAT_ICONS =
            mapOf(
                StatType.DEXTERITY to "✦",
                StatType.INTELLIGENCE to "ʔ",
                StatType.STRENGTH to "⚔",
                StatType.VITALITY to "■",
                StatType.WISDOM to "✸",
            )

        private val STAT_PREFIXES =
            mapOf(
                StatType.DEXTERITY to "DEX",
                StatType.INTELLIGENCE to "INT",
                StatType.STRENGTH to "STR",
                StatType.VITALITY to "VIT",
                StatType.WISDOM to "WIS",
            )

        private val STAT_COLORS =
            mapOf(
                StatType.DEXTERITY to "&e",
                StatType.INTELLIGENCE to "&3",
                StatType.STRENGTH to "&c",
                StatType.VITALITY to "&f",
                StatType.WISDOM to "&a",
            )
    }

    override fun onLoad(player: Player, menuContents: MenuContents) {
        val uuid = player.uniqueId
        val dexterity = statManager.getStat(uuid, StatType.DEXTERITY)
        val intelligence = statManager.getStat(uuid, StatType.INTELLIGENCE)
        val strength = statManager.getStat(uuid, StatType.STRENGTH)
        val vitality = statManager.getStat(uuid, StatType.VITALITY)
        val wisdom = statManager.getStat(uuid, StatType.WISDOM)

        fillBorders(menuContents)
        menuContents.set(0, 0, ClickableItem.of(buildCloseButton()) { player.closeInventory() })
        menuContents.set(
            0,
            4,
            DisplayItem.of(
                buildStatInfoItem(player, dexterity, intelligence, strength, vitality, wisdom)
            ),
        )

        val statItems =
            listOf(
                buildDexterityItem(dexterity),
                buildIntelligenceItem(intelligence),
                buildStrengthItem(strength),
                buildWisdomItem(wisdom),
                buildVitalityItem(vitality),
            )
        for (i in STAT_ITEM_SLOTS.indices) {
            val slot = STAT_ITEM_SLOTS[i]
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(statItems[i]))
        }
    }

    private fun buildCloseButton(): ItemStack =
        ItemStack(Material.BARRIER).apply {
            editMeta { meta ->
                meta.displayName("&cClose".colorFormat().decoration(TextDecoration.ITALIC, false))
            }
        }

    private fun buildStatInfoItem(
        player: Player,
        dexterity: Int,
        intelligence: Int,
        strength: Int,
        vitality: Int,
        wisdom: Int,
    ): ItemStack {
        val character = userDataRegistry.getCharacter(player.uniqueId)
        val classType = character?.withSyncCharacterData { traits.classType }
        val baseHealth =
            if (classType != null)
                characterLevelHelper.calculateHealthAtLevel(player.level, classType)
            else CharacterHealthHelper.BASE_HEALTH
        val healthBonus =
            (player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0).toInt() - baseHealth
        val lore =
            "\n&7Your character stats improve your potency in battle!" +
                "\n&7Earn them from your &5Skill Tree &7or from items!" +
                "\n&c❤ (Health): ${statPrefix(healthBonus)}$healthBonus" +
                "\n${formattedStat(StatType.DEXTERITY, dexterity)}" +
                "\n${formattedStat(StatType.INTELLIGENCE, intelligence)}" +
                "\n${formattedStat(StatType.STRENGTH, strength)}" +
                "\n${formattedStat(StatType.VITALITY, vitality)}" +
                "\n${formattedStat(StatType.WISDOM, wisdom)}"
        return ItemStack(Material.PAPER).apply {
            editMeta { meta ->
                meta.displayName(
                    "&eCharacter Stats".colorFormat().decoration(TextDecoration.ITALIC, false)
                )
                meta.lore(lore.toLoreComponents())
            }
        }
    }

    private fun formattedStat(stat: StatType, value: Int): String {
        val color = STAT_COLORS.getValue(stat)
        val icon = STAT_ICONS.getValue(stat)
        val prefix = STAT_PREFIXES.getValue(stat)
        return "$color$icon ($prefix): ${statPrefix(value)}$value"
    }

    private fun statPrefix(value: Int): String = if (value > 0) "&a+" else "&7+"

    private fun buildDexterityItem(dexterity: Int): ItemStack {
        val abilityHaste = StatConstants.ABILITY_HASTE * 100 * dexterity
        val abilityHasteString = if (abilityHaste > 0) DECIMAL_FORMAT.format(abilityHaste) else "0"
        val lore =
            "\n&7Dexterity (DEX) grants &ospell haste&7, decreasing your spell cooldowns!" +
                "\n\n&2&lCombat Bonuses:" +
                "\n&7Spell Haste: ${statPrefix(dexterity)}$abilityHasteString%"
        return buildStatItem(StatType.DEXTERITY, dexterity, lore)
    }

    private fun buildIntelligenceItem(intelligence: Int): ItemStack {
        val manaRegenPercent = StatConstants.MANA_REGEN_MULT * 100 * intelligence
        val spellDamagePercent = StatConstants.MAGIC_DMG_MULT * 100 * intelligence
        val manaRegenString =
            if (manaRegenPercent > 0) DECIMAL_FORMAT.format(manaRegenPercent) else "0"
        val spellDamageString =
            if (spellDamagePercent > 0) DECIMAL_FORMAT.format(spellDamagePercent) else "0"
        val lore =
            "\n&7Intelligence (INT) grants additional &3magic &3ʔ&7 damage and mana regeneration!" +
                "\n\n&2&lCombat Bonuses:" +
                "\n&7Magic Damage: ${statPrefix(intelligence)}$spellDamageString%" +
                "\n&7Mana Regen: ${statPrefix(intelligence)}$manaRegenString%"
        return buildStatItem(StatType.INTELLIGENCE, intelligence, lore)
    }

    private fun buildStrengthItem(strength: Int): ItemStack {
        val physicalDamagePercent = StatConstants.PHYSICAL_DMG_MULT * 100 * strength
        val physicalDamageString =
            if (physicalDamagePercent > 0) DECIMAL_FORMAT.format(physicalDamagePercent) else "0"
        val lore =
            "\n&7Strength (STR) grants additional &cphysical &c⚔&7 damage!" +
                "\n&2&lCombat Bonuses:" +
                "\n&7Physical Damage: ${statPrefix(strength)}$physicalDamageString%"
        return buildStatItem(StatType.STRENGTH, strength, lore)
    }

    private fun buildWisdomItem(wisdom: Int): ItemStack {
        val maxManaPercent = StatConstants.MAX_MANA_MULT * 100 * wisdom
        val spellHealingPercent = StatConstants.SPELL_HEALING_MULT * 100 * wisdom
        val spellShieldingPercent = StatConstants.SPELL_SHIELDING_MULT * 100 * wisdom
        val maxManaString = if (maxManaPercent > 0) DECIMAL_FORMAT.format(maxManaPercent) else "0"
        val spellHealingString =
            if (spellHealingPercent > 0) DECIMAL_FORMAT.format(spellHealingPercent) else "0"
        val spellShieldingString =
            if (spellShieldingPercent > 0) DECIMAL_FORMAT.format(spellShieldingPercent) else "0"
        val lore =
            "\n&7Wisdom (WIS) grants additional mana, outgoing &aspell healing✸&7, " +
                "and outgoing &espell shielding&7!" +
                "\n\n&2&lCombat Bonuses:" +
                "\n&7Max Mana: ${statPrefix(wisdom)}$maxManaString%" +
                "\n&7Spell Healing: ${statPrefix(wisdom)}$spellHealingString%" +
                "\n&7Spell Shielding: ${statPrefix(wisdom)}$spellShieldingString%"
        return buildStatItem(StatType.WISDOM, wisdom, lore)
    }

    private fun buildVitalityItem(vitality: Int): ItemStack {
        var defensePercent = StatConstants.DAMAGE_REDUCTION_MULT * 100 * vitality
        val defenseCap = StatConstants.DAMAGE_REDUCTION_CAP * 100
        if (defensePercent > defenseCap) defensePercent = defenseCap
        val healthRegenPercent = StatConstants.HEALTH_REGEN_MULT * 100 * vitality
        val defenseString = if (defensePercent > 0) DECIMAL_FORMAT.format(defensePercent) else "0"
        val healthRegenString =
            if (healthRegenPercent > 0) DECIMAL_FORMAT.format(healthRegenPercent) else "0"
        val capSuffix = if (defensePercent >= defenseCap) " (Cap Reached)" else ""
        val lore =
            "\n&7Vitality (VIT) grants additional &odefense&7, " +
                "reducing damage taken from players and monsters, " +
                "as well as additional health regeneration!" +
                "\n\n&9Defense is capped at ${defenseCap.toInt()}%" +
                "\n\n&2&lCombat Bonuses:" +
                "\n&7Defense: ${statPrefix(vitality)}$defenseString%$capSuffix" +
                "\n&7Health Regen: ${statPrefix(vitality)}$healthRegenString%"
        return buildStatItem(StatType.VITALITY, vitality, lore)
    }

    private fun buildStatItem(stat: StatType, value: Int, lore: String): ItemStack {
        val icon = STAT_ICONS.getValue(stat)
        val material = STAT_MATERIALS.getValue(stat)
        val name = stat.name.lowercase().replaceFirstChar { it.uppercase() }
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(
                    "&e$name$icon: $value".colorFormat().decoration(TextDecoration.ITALIC, false)
                )
                meta.lore(lore.toLoreComponents())
            }
        }
    }

    private fun fillBorders(menuContents: MenuContents) {
        val glass =
            ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
                editMeta { meta -> meta.displayName(Component.empty()) }
            }
        val borderSlots =
            intArrayOf(
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                17,
                18,
                26,
                27,
                35,
                36,
                44,
                45,
                46,
                47,
                48,
                49,
                50,
                51,
                52,
                53,
            )
        for (slot in borderSlots) {
            menuContents.set(slot / 9, slot % 9, DisplayItem.of(glass.clone()))
        }
    }
}
