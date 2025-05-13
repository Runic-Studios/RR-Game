package com.runicrealms.game.items.util

import java.util.LinkedList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent

class ItemLoreBuilder {
    private val lore: MutableList<TextComponent> = LinkedList()

    constructor()

    constructor(initial: TextComponent) {
        lore.add(initial)
    }

    constructor(initial: Array<TextComponent>) {
        lore.addAll(listOf(*initial))
    }

    constructor(initial: List<TextComponent>) {
        lore.addAll(initial)
    }

    fun appendLines(vararg lines: TextComponent): ItemLoreBuilder {
        lore.addAll(listOf(*lines))
        return this
    }

    fun appendLines(lines: List<TextComponent>): ItemLoreBuilder {
        lore.addAll(lines)
        return this
    }

    fun appendLinesIf(condition: Boolean, vararg lines: TextComponent): ItemLoreBuilder {
        if (condition) lore.addAll(listOf(*lines))
        return this
    }

    fun appendLinesIf(condition: Boolean, lines: List<TextComponent>): ItemLoreBuilder {
        if (condition) lore.addAll(lines)
        return this
    }

    fun appendLinesIf(condition: Boolean, lineSupplier: () -> Array<TextComponent>): ItemLoreBuilder {
        if (condition) lore.addAll(listOf(*lineSupplier()))
        return this
    }

    fun newLine(): ItemLoreBuilder {
        lore.add(Component.text(""))
        return this
    }

    fun newLineIf(condition: Boolean): ItemLoreBuilder {
        if (condition) lore.add(Component.text(""))
        return this
    }

    fun build(): MutableList<TextComponent> {
        return lore
    }
}