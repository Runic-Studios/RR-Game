package com.runicrealms.game.items.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Default
import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.items.weaponskin.WeaponSkinManager
import com.runicrealms.game.items.weaponskin.ui.WeaponAppearancesUI
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Command handler for `/weaponry`. Opens the weapon appearances UI
 * if the player is holding a weapon.
 */
@CommandAlias("weaponry")
class WeaponryCommand
@Inject
constructor(
    commandManager: PaperCommandManager,
    private val weaponSkinManager: WeaponSkinManager,
    private val weaponAppearancesUI: WeaponAppearancesUI,
) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Default
    @Conditions("is-player")
    fun onCommand(player: Player) {
        val heldItem = player.inventory.itemInMainHand
        if (heldItem.type == Material.AIR) {
            player.sendMessage(PREFIX.plus("&dYou must be holding a weapon to use this command!").colorFormat())
            return
        }

        val skins = weaponSkinManager.getMaterialSkins(heldItem.type)
        if (skins.isEmpty()) {
            player.sendMessage(PREFIX.plus("&dThere are no skins available for this weapon type.").colorFormat())
            return
        }

        weaponAppearancesUI.open(player)
    }

    companion object {
        const val PREFIX: String = "&5[WeaponSkins] &6>> &r"
    }
}
