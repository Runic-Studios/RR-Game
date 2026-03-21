package com.runicrealms.game.tools

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

val WARP_PREFIX: Component =
    Component.text("[", NamedTextColor.LIGHT_PURPLE)
        .append(Component.text("RunicWarp", NamedTextColor.DARK_PURPLE))
        .append(Component.text("] > ", NamedTextColor.LIGHT_PURPLE))
