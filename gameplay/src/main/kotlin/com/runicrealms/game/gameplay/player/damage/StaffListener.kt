package com.runicrealms.game.gameplay.player.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.damage.DamageHandler
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.EnemyVerifyEvent
import com.runicrealms.game.gameplay.spell.event.StaffAttackEvent
import com.runicrealms.game.gameplay.spell.spellutil.VectorUtil
import com.runicrealms.game.items.generator.GameItemWeapon
import com.runicrealms.game.items.generator.ItemStackConverter
import java.util.concurrent.ThreadLocalRandom
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Handles the Mage basic (staff) attack by listening to [StaffAttackEvent].
 *
 * [SpellStaffListener] fires [StaffAttackEvent] when a Mage left-clicks; this listener performs the
 * raycast, draws the particle beam, deals damage via [DamageHandler.dealPhysicalDamage], and fires
 * [BasicAttackEvent] to apply the item cooldown.
 */
@Singleton
class StaffListener
@Inject
constructor(
    private val plugin: Plugin,
    private val itemStackConverter: ItemStackConverter,
    private val damageHandler: DamageHandler,
) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onStaffAttack(event: StaffAttackEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        val gameItem = itemStackConverter.convertToGameItem(item) as? GameItemWeapon ?: return

        if (player.getCooldown(item.type) > 0) return

        if (gameItem.weaponTemplate.level > player.level) {
            player.playSound(player.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f)
            player.sendMessage(
                Component.text("Your level is too low to wield this!", NamedTextColor.RED)
            )
            return
        }

        val rayTraceResult =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                event.range.toDouble(),
                RAY_SIZE,
            ) {
                isValidEnemy(player, it)
            }

        player.playSound(player.location, Sound.ENTITY_GHAST_SHOOT, 0.4f, 2.0f)

        if (rayTraceResult?.hitEntity != null) {
            val victim = rayTraceResult.hitEntity as LivingEntity
            VectorUtil.drawLine(player, BEAM_COLOR, player.eyeLocation, victim.eyeLocation, 0.85)
            damageStaff(player, victim, gameItem)
        } else {
            val endLocation = player.getTargetBlock(null, event.range).location
            VectorUtil.drawLine(player, BEAM_COLOR, player.eyeLocation, endLocation, 0.85)
        }

        val minDamage = gameItem.weaponTemplate.damage.min
        val maxDamage = gameItem.weaponTemplate.damage.max
        val attackEvent =
            BasicAttackEvent(
                player,
                item.type,
                BasicAttackEvent.BASE_STAFF_COOLDOWN,
                BasicAttackEvent.BASE_STAFF_COOLDOWN.toDouble(),
                minDamage,
                maxDamage,
            )
        Bukkit.getPluginManager().callEvent(attackEvent)
    }

    private fun damageStaff(player: Player, victim: LivingEntity, gameItem: GameItemWeapon) {
        val minDamage = gameItem.weaponTemplate.damage.min
        val maxDamage = gameItem.weaponTemplate.damage.max
        val randomNum =
            if (maxDamage > minDamage) ThreadLocalRandom.current().nextInt(minDamage, maxDamage + 1)
            else minDamage
        damageHandler.dealPhysicalDamage(
            randomNum,
            victim,
            player,
            isBasicAttack = true,
            isRanged = true,
        )
        player.playSound(player.location, Sound.ENTITY_PLAYER_HURT, 0.5f, 1.0f)
    }

    private fun isValidEnemy(player: Player, entity: Entity): Boolean {
        if (entity !is LivingEntity) return false
        val enemyVerifyEvent = EnemyVerifyEvent(player, entity)
        Bukkit.getPluginManager().callEvent(enemyVerifyEvent)
        return !enemyVerifyEvent.isCancelled
    }

    private companion object {
        const val RAY_SIZE = 0.8
        val BEAM_COLOR: Color = Color.FUCHSIA
    }
}
