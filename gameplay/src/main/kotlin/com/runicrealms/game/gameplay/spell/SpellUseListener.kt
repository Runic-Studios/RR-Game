package com.runicrealms.game.gameplay.spell

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.event.SpellTriggerEvent
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.SpellSlot
import com.runicrealms.game.gameplay.spell.spelltypes.SpellTriggerType
import java.util.UUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Manages the two-step spell cast activation UI:
 * 1. First trigger (right-click / left-click / swap hands): opens the cast menu title and starts a
 *    [SPELL_TIMEOUT]-second window.
 * 2. Second trigger within the window: fires [SpellCastEvent] which calls [Spell.execute].
 *
 * Current stubs (see SPELL_MIGRATION.md):
 * - Settings check (`SettingsManager.getCastMenuEnabled()`) not yet migrated — always enabled.
 * - Weapon-type class validation (`DamageListener.matchClass()`) not yet migrated — skipped.
 * - [BasicAttackEvent] is fired from an un-migrated DamageListener — stubbed.
 */
@Singleton
class SpellUseListener
@Inject
constructor(private val plugin: Plugin, private val spellManager: SpellManager) : Listener {

    private val casters: MutableMap<UUID, BukkitTask> = HashMap()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onSpellTrigger(event: SpellTriggerEvent) {
        if (event.isCancelled || !event.willExecute) return
        val player = event.player

        if (casters.containsKey(player.uniqueId)) {
            // Second trigger: attempt to cast the spell for this slot
            casters.remove(player.uniqueId)?.cancel()
            castSpell(player, event.spellSlot)
        } else {
            // First trigger: start the cast window
            val isArcher = event.spellTriggerType == SpellTriggerType.ARCHER
            val isValidFirstSlot =
                if (isArcher) {
                    event.spellSlot == SpellSlot.RIGHT_CLICK
                } else {
                    event.spellSlot == SpellSlot.RIGHT_CLICK
                }
            if (!isValidFirstSlot) return

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f)
            showCastMenu(player)

            val task =
                Bukkit.getScheduler()
                    .runTaskLater(
                        plugin,
                        Runnable {
                            casters.remove(player.uniqueId)
                            player.sendActionBar(
                                Component.text("Spell cancelled.", NamedTextColor.GRAY)
                            )
                        },
                        SPELL_TIMEOUT * 20L,
                    )
            casters[player.uniqueId] = task
        }
    }

    @EventHandler
    fun onSpellCast(event: SpellCastEvent) {
        if (event.isCancelled || !event.willExecute) return
        event.spell.execute(event.caster, SpellItemType.ARTIFACT)
    }

    // --- Input listeners ---

    @EventHandler(priority = EventPriority.HIGH)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        if (event.newSlot == 0) {
            fireSpellTrigger(event.player, SpellSlot.HOT_BAR_ONE)
        }
        // Cancel spell if slot changes while casting
        if (casters.containsKey(event.player.uniqueId) && event.newSlot != 0) {
            casters.remove(event.player.uniqueId)?.cancel()
            event.player.sendActionBar(Component.text("Spell cancelled.", NamedTextColor.GRAY))
        }
    }

    @EventHandler
    fun onSwapHands(event: PlayerSwapHandItemsEvent) {
        fireSpellTrigger(event.player, SpellSlot.SWAP_HANDS)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (event.hand != EquipmentSlot.HAND) return
        // Slot 0 is the rune slot; right-clicking it opens the RuneMenu, not a spell cast window.
        if (player.inventory.heldItemSlot == RUNE_SLOT) return

        // TODO: Add weapon-type check (DamageListener.matchClass) once migrated.
        // For now, allow any click with item in hand.

        when (event.action) {
            Action.LEFT_CLICK_AIR,
            Action.LEFT_CLICK_BLOCK -> {
                fireSpellTrigger(player, SpellSlot.LEFT_CLICK)
            }
            Action.RIGHT_CLICK_AIR,
            Action.RIGHT_CLICK_BLOCK -> {
                if (event.clickedBlock != null) event.isCancelled = true
                fireSpellTrigger(player, SpellSlot.RIGHT_CLICK)
            }
            else -> {}
        }
    }

    // --- Helpers ---

    private fun fireSpellTrigger(player: Player, slot: SpellSlot) {
        // TODO: Determine Archer vs DEFAULT via GameCharacter classType once wired.
        val triggerType = SpellTriggerType.DEFAULT
        val triggerEvent = SpellTriggerEvent(player, slot, triggerType)
        Bukkit.getPluginManager().callEvent(triggerEvent)
    }

    private fun castSpell(player: Player, slot: SpellSlot) {
        val spell = spellManager.getPlayerSpell(player, slot)
        if (spell == null) {
            player.sendActionBar(
                Component.text("No spell assigned to that slot.", NamedTextColor.GRAY)
            )
            return
        }
        val castEvent = SpellCastEvent(player, spell)
        Bukkit.getPluginManager().callEvent(castEvent)
    }

    private fun showCastMenu(player: Player) {
        player.sendActionBar(
            Component.text()
                .append(Component.text("[L] ", NamedTextColor.AQUA))
                .append(Component.text("Left  ", NamedTextColor.WHITE))
                .append(Component.text("[R] ", NamedTextColor.GREEN))
                .append(Component.text("Right  ", NamedTextColor.WHITE))
                .append(Component.text("[F] ", NamedTextColor.GOLD))
                .append(Component.text("Swap", NamedTextColor.WHITE))
                .build()
        )
    }

    companion object {
        private const val SPELL_TIMEOUT = 5

        /** Slot 0 is the Ancient Runestone. Spell triggers are suppressed on this slot. */
        private const val RUNE_SLOT = 0
    }
}
