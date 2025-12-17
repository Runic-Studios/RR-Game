package com.runicrealms.game.items.dynamic

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.google.inject.Inject
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.items.generator.GameItem
import com.runicrealms.game.items.generator.ItemStackConverter
import java.util.LinkedList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * NOTE: This class relies on legacy serialization because of a few issues. If we were to use
 * Component#replaceText in adventure:
 * - We would have to a separate replacement for every single dynamic placeholder
 * - This would involve looping over the entire string once for each placeholder
 *
 * Instead, we use an efficient algorithm that is capable of parsing the legacy strings by looking
 * at angle brackets.
 */
class DynamicItemManager
@Inject
constructor(
    plugin: Plugin,
    private val itemStackConverter: ItemStackConverter,
    private val userDataRegistry: UserDataRegistry,
) :
    PacketAdapter(
        plugin,
        ListenerPriority.HIGHEST,
        PacketType.Play.Server.SET_SLOT,
        PacketType.Play.Server.WINDOW_ITEMS,
    ),
    DynamicItemRegistry {
    private val textPlaceholders: MutableMap<String, DynamicItemTextPlaceholder> =
        HashMap() // map between identifier and placeholder

    init {
        ProtocolLibrary.getProtocolManager().asynchronousManager.registerAsyncHandler(this).start()
    }

    override fun registerTextPlaceholder(placeholder: DynamicItemTextPlaceholder) {
        textPlaceholders[placeholder.identifier] = placeholder
    }

    override fun onPacketSending(event: PacketEvent) {
        val character = userDataRegistry.getCharacter(event.player.uniqueId) ?: return
        if (event.packetType === PacketType.Play.Server.SET_SLOT) {
            for (target in event.packet.itemModifier.values) {
                if (target == null || target.type == Material.AIR) continue
                processItem(character, target)
            }
        } else if (event.packetType === PacketType.Play.Server.WINDOW_ITEMS) {
            for (items in event.packet.itemListModifier.values) {
                for (target in items) {
                    if (target == null || target.type == Material.AIR) continue
                    processItem(character, target)
                }
            }

            for (target in event.packet.itemModifier.values) {
                if (target == null || target.type == Material.AIR) continue
                processItem(character, target)
            }
        }
    }

    fun processItem(
        viewer: GameCharacter,
        itemStack: ItemStack,
    ) { // Try and be as efficient as we can!
        val gameItem = itemStackConverter.convertToGameItem(itemStack) ?: return

        val meta = itemStack.itemMeta ?: return

        val displayNameComponent = meta.displayName()
        if (displayNameComponent != null) {
            val displayName =
                LegacyComponentSerializer.legacySection().serialize(displayNameComponent)
            val replacement = generateReplacement(displayName, viewer, gameItem, itemStack)
            if (replacement != null) {
                val newComponent =
                    LegacyComponentSerializer.legacySection().deserialize(replacement)
                // So this is a really really really annoying workaround because the adventure API
                // is sometimes
                // really hard to work with TODO fix
                val noItalics =
                    Component.text()
                        .decoration(TextDecoration.ITALIC, false)
                        .append(newComponent)
                        .build()
                meta.displayName(noItalics)
            }
        }

        if (meta.hasLore()) {
            val lore = meta.lore()!!
            val newLore = LinkedList<Component>()
            var changed = false
            for (component in lore) {
                val line = LegacyComponentSerializer.legacySection().serialize(component)
                val replacement = generateReplacement(line, viewer, gameItem, itemStack)
                if (replacement != null) {
                    changed = true
                    val newComponent: Component =
                        LegacyComponentSerializer.legacySection().deserialize(replacement)
                    // So this is a really really really annoying workaround because the adventure
                    // API is sometimes
                    // really hard to work with TODO fix
                    val noItalics: Component =
                        Component.text()
                            .decoration(TextDecoration.ITALIC, false)
                            .append(newComponent)
                            .build()
                    newLore.add(noItalics)
                } else {
                    newLore.add(component)
                }
            }
            if (changed) meta.lore(newLore)
        }
        itemStack.setItemMeta(meta)
    }

    /**
     * Attempts to generate a replacement of all the placeholders in a given string. If succeeds,
     * returns the new string. If fails (no placeholders), returns null.
     */
    private fun generateReplacement(
        target: String,
        viewer: GameCharacter,
        gameItem: GameItem,
        itemStack: ItemStack,
    ): String? {
        var target = target
        val results = findBracketedTextWithIndices(target)
        var changed = false
        while (!results.isEmpty()) {
            val brackets = results.removeLast() // Reverse order so that indices don't get messed up
            val placeholder = textPlaceholders[brackets.bracketedText]
            if (placeholder != null) {
                val replacement =
                    placeholder.generateReplacement(viewer, gameItem, itemStack) ?: continue
                target =
                    replaceStringSegment(
                        target,
                        brackets.beginIndex,
                        brackets.endIndex,
                        replacement,
                    )
                changed = true
            }
        }
        if (changed) return target
        return null
    }

    private data class BracketedTextInfo(
        val beginIndex: Int,
        val endIndex: Int,
        val bracketedText: String,
    )

    companion object {
        /**
         * Finds all instances of text wrapped in <> and returns a mapping between the indices of
         * the start of the text inside the brackets, and the text without the brackets itself
         *
         * array deque so that we can reverse the order when replacing so that indices don't get
         * messed up
         */
        private fun findBracketedTextWithIndices(input: String): ArrayDeque<BracketedTextInfo> {
            val results: ArrayDeque<BracketedTextInfo> = ArrayDeque()
            var startIndex = -1
            var endIndex: Int

            for (i in input.indices) {
                if (input[i] == '<' && startIndex == -1) {
                    startIndex = i
                } else if (input[i] == '>' && startIndex != -1) {
                    endIndex = i
                    val text = input.substring(startIndex + 1, endIndex)
                    results.add(BracketedTextInfo(startIndex, endIndex, text))
                    startIndex = -1
                }
            }
            return results
        }

        /**
         * Replaces a segment of the string starting from begin index to end index with a given
         * replacement, then returns that string. Begin index is inclusive, endIndex is not
         * inclusive.
         */
        private fun replaceStringSegment(
            original: String,
            beginIndex: Int,
            endIndex: Int,
            replacement: String,
        ): String {
            require(!(beginIndex < 0 || endIndex > original.length || beginIndex > endIndex)) {
                "Invalid indices"
            }
            val before = original.substring(0, beginIndex)
            val after = original.substring(endIndex + 1)
            return before + replacement + after
        }
    }
}
