package com.runicrealms.game.gameplay.scoreboard

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.data.game.GameCharacter
import com.runicrealms.game.gameplay.player.RegenManager
import com.runicrealms.game.items.event.GameStatUpdateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLevelChangeEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard

/**
 * Creates and maintains the sidebar scoreboard displayed to each player.
 *
 * Layout (top -> bottom, sidebar order is score descending):
 * ```
 *   §1                    ← invisible spacer (score 9)
 *   §e§lPlayerName        (score 8)
 *   §aClassName §elv. N   (score 7)
 *   §eProf: §aNone        (score 6) TODO: wire when Professions plugin migrates
 *   §eGuild: §aNone       (score 5) TODO: wire when Guilds plugin migrates
 *   §eStatus: §aLawful    (score 4) TODO: wire when PvP plugin migrates
 *   §2                    ← invisible spacer (score 3)
 *   §4❤ §cN §e/ §cN (Health)  (score 2)
 *   §3✸ N §e/ §3N (Mana)      (score 1)
 * ```
 *
 * Each player owns their own [Scoreboard] instance, so team names are unique per player with no
 * need for the player-index prefix used in the old Java implementation.
 *
 * Health and mana are refreshed every 200 ms via an async coroutine polling loop. Class/level is
 * refreshed on [PlayerLevelChangeEvent] and [GameStatUpdateEvent].
 */
class ScoreboardManager
@Inject
constructor(
    private val plugin: Plugin,
    private val userDataRegistry: UserDataRegistry,
    private val regenManager: RegenManager,
) : Listener {

    companion object {
        // Team names: simple short strings; safe because scoreboards are per-player instances
        private const val TEAM_CLASS = "class"
        private const val TEAM_PROF = "prof"
        private const val TEAM_GUILD = "guild"
        private const val TEAM_OUTLAW = "outlaw"
        private const val TEAM_HEALTH = "health"
        private const val TEAM_MANA = "mana"

        // Invisible anchor entries: unique colour-code pairs that render as empty strings in chat
        // (§ = \u00a7; codes: 0=black, 2=dark_green, a=green, 6=gold, 4=dark_red, c=red, b=aqua)
        private const val S = '\u00a7'
        private val ENTRY_CLASS = "${S}0${S}2"
        private val ENTRY_PROF = "${S}0${S}a"
        private val ENTRY_GUILD = "${S}0${S}6"
        private val ENTRY_OUTLAW = "${S}0${S}4"
        private val ENTRY_HEALTH = "${S}0${S}c"
        private val ENTRY_MANA = "${S}0${S}b"

        private const val HEALTH_MANA_UPDATE_MS = 200L
    }

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        startHealthManaLoop()
    }

    // Scoreboard setup

    /** Called at LOWEST priority so the scoreboard is ready before other load handlers run. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onCharacterLoad(event: GameCharacterLoadEvent) {
        setupScoreboard(event.character)
    }

    private fun setupScoreboard(character: GameCharacter) {
        val player = character.bukkitPlayer
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val title =
            Component.text("     Runic Realms")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
        val obj = scoreboard.registerNewObjective("ServerName", "dummy", title)
        obj.displaySlot = DisplaySlot.SIDEBAR

        // Spacer + player name header (static scores, set directly on the objective)
        obj.getScore("${S}1").score = 9
        val playerNameEntry =
            LegacyComponentSerializer.legacySection()
                .serialize(
                    Component.text(player.name)
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                )
        obj.getScore(playerNameEntry).score = 8

        // Static info rows — registered as teams so the prefix can be updated without flicker
        scoreboard.registerNewTeam(TEAM_CLASS).also { team ->
            team.addEntry(ENTRY_CLASS)
            obj.getScore(ENTRY_CLASS).score = 7
            team.prefix(classComponent(character))
        }
        scoreboard.registerNewTeam(TEAM_PROF).also { team ->
            team.addEntry(ENTRY_PROF)
            obj.getScore(ENTRY_PROF).score = 6
            // TODO: replace with real profession data when Professions plugin is migrated
            team.prefix(
                Component.text("Prof: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("None").color(NamedTextColor.GREEN))
            )
        }
        scoreboard.registerNewTeam(TEAM_GUILD).also { team ->
            team.addEntry(ENTRY_GUILD)
            obj.getScore(ENTRY_GUILD).score = 5
            // TODO: replace with real guild data when Guilds plugin is migrated
            team.prefix(
                Component.text("Guild: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("None").color(NamedTextColor.GREEN))
            )
        }
        scoreboard.registerNewTeam(TEAM_OUTLAW).also { team ->
            team.addEntry(ENTRY_OUTLAW)
            obj.getScore(ENTRY_OUTLAW).score = 4
            // TODO: replace with real outlaw status when PvP plugin is migrated
            team.prefix(
                Component.text("Status: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("Lawful").color(NamedTextColor.GREEN))
            )
        }

        // Second spacer
        obj.getScore("${S}2").score = 3

        // Dynamic combat rows — populated immediately and kept up-to-date by the polling loop
        scoreboard.registerNewTeam(TEAM_HEALTH).also { team ->
            team.addEntry(ENTRY_HEALTH)
            obj.getScore(ENTRY_HEALTH).score = 2
        }
        scoreboard.registerNewTeam(TEAM_MANA).also { team ->
            team.addEntry(ENTRY_MANA)
            obj.getScore(ENTRY_MANA).score = 1
        }

        updateHealthManaDisplay(character, scoreboard)
        player.scoreboard = scoreboard
    }

    // Static info updates (class / level)

    @EventHandler
    fun onLevelUp(event: PlayerLevelChangeEvent) {
        val character = userDataRegistry.getCharacter(event.player.uniqueId) ?: return
        updateStaticInfo(character)
    }

    @EventHandler
    fun onStatUpdate(event: GameStatUpdateEvent) {
        updateStaticInfo(event.character)
    }

    private fun updateStaticInfo(character: GameCharacter) {
        character.bukkitPlayer.scoreboard.getTeam(TEAM_CLASS)?.prefix(classComponent(character))
    }

    // Health / mana polling loop (200 ms)

    private fun startHealthManaLoop() {
        plugin.launch {
            withContext(plugin.asyncDispatcher) {
                while (true) {
                    delay(HEALTH_MANA_UPDATE_MS)
                    for (character in userDataRegistry.getAllCharacters()) {
                        updateHealthManaDisplay(character, character.bukkitPlayer.scoreboard)
                    }
                }
            }
        }
    }

    private fun updateHealthManaDisplay(character: GameCharacter, scoreboard: Scoreboard) {
        try {
            scoreboard.getTeam(TEAM_HEALTH)?.prefix(healthComponent(character.bukkitPlayer))
            scoreboard.getTeam(TEAM_MANA)?.prefix(manaComponent(character))
        } catch (_: Exception) {
            // Scoreboard may not yet be fully initialised; will be retried on the next poll
        }
    }

    // Display string helpers

    private fun classComponent(character: GameCharacter): Component {
        val classType = character.withSyncCharacterData { traits.classType }
        return if (classType == ClassType.ANY) {
            Component.text("Class: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("None").color(NamedTextColor.GREEN))
        } else {
            val name = classType.name.lowercase().replaceFirstChar { it.titlecase() }
            Component.text("$name ")
                .color(NamedTextColor.GREEN)
                .append(Component.text("lv. ").color(NamedTextColor.YELLOW))
                .append(
                    Component.text("${character.bukkitPlayer.level}").color(NamedTextColor.GREEN)
                )
        }
    }

    private fun healthComponent(player: Player): Component {
        val hp = player.health.toInt()
        val maxHp = player.getAttribute(Attribute.MAX_HEALTH)!!.value.toInt()
        return Component.text("❤ ")
            .color(NamedTextColor.DARK_RED)
            .append(Component.text("$hp ").color(NamedTextColor.RED))
            .append(Component.text("/ ").color(NamedTextColor.YELLOW))
            .append(Component.text("$maxHp (Health)").color(NamedTextColor.RED))
    }

    private fun manaComponent(character: GameCharacter): Component {
        val mana = regenManager.getCurrentMana(character.bukkitPlayer.uniqueId)
        val maxMana = regenManager.calculateMaxMana(character)
        return Component.text("✸ $mana ")
            .color(NamedTextColor.DARK_AQUA)
            .append(Component.text("/ ").color(NamedTextColor.YELLOW))
            .append(Component.text("$maxMana (Mana)").color(NamedTextColor.DARK_AQUA))
    }
}
