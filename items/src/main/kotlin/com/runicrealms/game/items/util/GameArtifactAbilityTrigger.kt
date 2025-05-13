package com.runicrealms.game.items.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

enum class GameArtifactAbilityTrigger(val identifier: String, val display: TextComponent) {
    ON_CAST(
        "on-cast",
        Component.text("ON CAST", Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)),
    ),
    ON_HIT(
        "on-hit",
        Component.text("ON HIT", Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)),
    ),
    ON_KILL(
        "on-kill",
        Component.text("ON KILL", Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)),
    );

    companion object {
        fun getFromIdentifier(identifier: String?): GameArtifactAbilityTrigger? {
            for (trigger in entries) {
                if (trigger.identifier.equals(identifier, ignoreCase = true)) {
                    return trigger
                }
            }
            return null
        }
    }
}
