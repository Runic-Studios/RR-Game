package com.runicrealms.game.items.weaponskin.ui

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.util.colorFormat
import org.bukkit.entity.Player

/**
 * Stub UI for weapon skin selection.
 *
 * TODO: Implement with OdalitaMenus once the menu library API is available.
 */
@Singleton
class WeaponAppearancesUI
@Inject
constructor() {

    fun open(player: Player) {
        // TODO: Replace with OdalitaMenus-based UI implementation
        player.sendMessage(
            "&5[WeaponSkins] &6>> &dThe weapon appearances UI is not yet implemented (pending OdalitaMenus integration)."
                .colorFormat()
        )
    }
}
