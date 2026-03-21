package com.runicrealms.game.gameplay.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.google.inject.Inject
import com.runicrealms.game.gameplay.spell.event.MobDamageEvent
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.command.CommandSender

/**
 * Legacy compatibility command used by MythicMobs content.
 *
 * Expected format: /runicdamage <caster.uuid> <target.name> <amount>
 */
@CommandAlias("runicdamage")
@CommandPermission("runic.op")
class RunicDamageCommand @Inject constructor(commandManager: PaperCommandManager) : BaseCommand() {

    init {
        commandManager.registerCommand(this)
    }

    @Default
    @CatchUnknown
    fun onCommand(sender: CommandSender, args: Array<String>) {
        if (!sender.isOp) return
        if (args.size < 3) {
            logImproperConfig()
            return
        }

        val casterId = args[0].toUuidOrNull() ?: return
        val caster = Bukkit.getEntity(casterId) ?: return
        val target = Bukkit.getPlayer(args[1]) ?: return
        val amount = args[2].toIntOrNull() ?: return
        if (target.gameMode == GameMode.CREATIVE) return

        val mobDamageEvent = MobDamageEvent(target, amount, caster, false)
        Bukkit.getPluginManager().callEvent(mobDamageEvent)
        if (mobDamageEvent.isCancelled) return

        target.noDamageTicks = 0
        target.damage(mobDamageEvent.amount.toDouble(), caster)
    }

    private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

    private fun logImproperConfig() {
        Bukkit.getServer().logger.info("RunicDamage improperly configured. Please check logs!")
    }
}
