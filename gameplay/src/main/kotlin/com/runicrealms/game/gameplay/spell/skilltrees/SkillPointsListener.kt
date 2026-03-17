package com.runicrealms.game.gameplay.spell.skilltrees

import com.google.inject.Inject
import com.runicrealms.game.common.util.colorFormat
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.data.event.GameCharacterLoadEvent
import com.runicrealms.game.gameplay.spell.api.SkillTreeAPI
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLevelChangeEvent
import org.bukkit.plugin.Plugin

private const val SKILL_TREE_UNLOCK_LEVEL = SkillTreeData.FIRST_POINT_LEVEL
private const val REMINDER_INTERVAL_TICKS = 300 * 20L // 5 minutes
private const val REMINDER_DELAY_TICKS = 60 * 20L    // start after 1 minute

/**
 * Notifies players of skill point availability:
 * - On level-up (levels 1–9): how many levels remain until skill trees unlock
 * - On level 10: announces skill tree unlock
 * - On character load: reminds player if they have unspent skill points
 * - Every 5 minutes: reminds all online players with unspent points
 *
 * Mirrors the behaviour of old SkillPointsListener.java.
 */
class SkillPointsListener
@Inject
constructor(
    private val plugin: Plugin,
    private val skillTreeAPI: SkillTreeAPI,
    private val userDataRegistry: UserDataRegistry,
) : Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            Runnable {
                for (character in userDataRegistry.getAllCharacters()) {
                    val uuid = character.bukkitPlayer.uniqueId
                    val points = skillTreeAPI.getAvailableSkillPoints(uuid, 1)
                    if (points > 0) sendReminderMessage(character.bukkitPlayer, points)
                }
            },
            REMINDER_DELAY_TICKS,
            REMINDER_INTERVAL_TICKS,
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onLevelChange(event: PlayerLevelChangeEvent) {
        val uuid = event.player.uniqueId
        // Filter out login-time level-set (level goes from 0 to saved level on character load)
        if (event.oldLevel == 0 && event.newLevel != 1) return
        // Only notify players with a loaded character
        if (userDataRegistry.getCharacter(uuid) == null) return

        val player = event.player
        val newLevel = event.newLevel

        when {
            newLevel == SKILL_TREE_UNLOCK_LEVEL ->
                player.sendMessage(
                    "&c[!] &dYou have unlocked &a&lSKILL TREES&d! Right-click your Ancient Runestone to unlock new perks with your skill points!"
                        .colorFormat()
                )
            newLevel < SKILL_TREE_UNLOCK_LEVEL -> {
                val remaining = SKILL_TREE_UNLOCK_LEVEL - newLevel
                player.sendMessage(
                    "&c[!] &dYou have &f$remaining &dlevel(s) left until you unlock &a&lSKILL TREES&d!"
                        .colorFormat()
                )
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCharacterLoad(event: GameCharacterLoadEvent) {
        val uuid = event.character.bukkitPlayer.uniqueId
        val points = skillTreeAPI.getAvailableSkillPoints(uuid, 1)
        if (points > 0) sendReminderMessage(event.character.bukkitPlayer, points)
    }

    private fun sendReminderMessage(player: org.bukkit.entity.Player, points: Int) {
        player.sendMessage(
            "&c[!] &dYou have &f$points &dskill point(s) to spend! Visit your &a&lSKILL TREE &dto purchase new perks!"
                .colorFormat()
        )
    }
}
