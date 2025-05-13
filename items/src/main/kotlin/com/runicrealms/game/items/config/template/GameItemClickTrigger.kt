package com.runicrealms.game.items.config.template

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.event.block.Action

data class GameItemClickTrigger(val type: Type, val triggerID: String) {

    enum class Type(val identifier: String, val display: TextComponent) {
        RIGHT_CLICK(
            "right",
            Component.text("RIGHT CLICK", Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)),
        ),
        SHIFT_RIGHT_CLICK(
            "shift-right",
            Component.text(
                "SNEAK + RIGHT CLICK",
                Style.style(NamedTextColor.GOLD, TextDecoration.BOLD),
            ),
        ),
        LEFT_CLICK(
            "left",
            Component.text("LEFT CLICK", Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)),
        ),
        SHIFT_LEFT_CLICK(
            "shift-left",
            Component.text(
                "SNEAK + RIGHT CLICK",
                Style.style(NamedTextColor.GOLD, TextDecoration.BOLD),
            ),
        );

        companion object {
            fun getFromIdentifier(identifier: String): Type? {
                for (trigger in entries) {
                    if (trigger.identifier.equals(identifier, ignoreCase = true)) {
                        return trigger
                    }
                }
                return null
            }

            fun getFromInteractAction(action: Action, player: Player): Type? {
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    if (player.isSneaking) {
                        return SHIFT_RIGHT_CLICK
                    }
                    return RIGHT_CLICK
                } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    if (player.isSneaking) {
                        return SHIFT_LEFT_CLICK
                    }
                    return LEFT_CLICK
                }
                return null
            }
        }
    }
}
