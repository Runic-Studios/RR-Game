package com.runicrealms.game.gameplay.player.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import com.runicrealms.game.gameplay.spell.damage.DamageHandler
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.EnemyVerifyEvent
import com.runicrealms.game.gameplay.spell.event.RunicBowEvent
import com.runicrealms.game.items.generator.GameItemWeapon
import com.runicrealms.game.items.generator.ItemStackConverter
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin

/**
 * Handles custom bow mechanics for the Archer class.
 *
 * Intercepts [PlayerInteractEvent] on right-click to verify class, cooldown, and level, then
 * manually launches an arrow via [Player.launchProjectile]. This matches the original instant-fire
 * mechanic (no draw-and-release required, no arrows needed in inventory).
 *
 * Arrow UUIDs are tracked in [autoAttackArrows] so [ProjectileHitEvent] can apply
 * [DamageHandler.dealPhysicalDamage] when the arrow hits an entity.
 */
@Singleton
class BowListener
@Inject
constructor(
    private val plugin: Plugin,
    private val combatManager: CombatManager,
    private val spellManager: SpellManager,
    private val itemStackConverter: ItemStackConverter,
    private val damageHandler: DamageHandler,
) : Listener {

    // Tracks UUIDs of arrows fired as auto-attacks so onProjectileHit can apply damage
    private val autoAttackArrows = HashSet<UUID>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /** Cancels vanilla arrow pickup so arrows do not clutter the player's inventory. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onArrowPickup(event: PlayerPickupArrowEvent) {
        event.isCancelled = true
    }

    /** Applies damage when an auto-attack arrow hits an entity, then removes the arrow. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (projectile !is Arrow) return
        val shooter = projectile.shooter as? Player ?: return

        if (autoAttackArrows.remove(projectile.uniqueId)) {
            val victim = event.hitEntity as? LivingEntity
            if (victim != null) {
                val enemyVerifyEvent = EnemyVerifyEvent(shooter, victim)
                Bukkit.getPluginManager().callEvent(enemyVerifyEvent)
                if (!enemyVerifyEvent.isCancelled) {
                    val item = shooter.inventory.itemInMainHand
                    val gameItem = itemStackConverter.convertToGameItem(item) as? GameItemWeapon
                    if (gameItem != null) {
                        val minDamage = gameItem.weaponTemplate.damage.min
                        val maxDamage = gameItem.weaponTemplate.damage.max
                        val randomNum =
                            if (maxDamage > minDamage)
                                ThreadLocalRandom.current().nextInt(minDamage, maxDamage + 1)
                            else minDamage
                        damageHandler.dealPhysicalDamage(
                            randomNum,
                            victim,
                            shooter,
                            isBasicAttack = true,
                            isRanged = true,
                        )
                    }
                }
            }
        }

        plugin.server.scheduler.runTaskLater(plugin, Runnable { projectile.remove() }, 1L)
    }

    /**
     * Fires a custom arrow on right-click with a Runic bow, bypassing the vanilla draw mechanic.
     * The vanilla interaction is always cancelled to prevent the bow-drawing animation and ensure
     * Runic cooldowns govern fire rate.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBowShoot(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK)
            return

        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type != Material.BOW) return

        // Cancel the vanilla interaction so the bow draw animation never starts
        event.isCancelled = true

        val gameItem = itemStackConverter.convertToGameItem(item) as? GameItemWeapon ?: return

        val classType = spellManager.getPlayerClassType(player.uniqueId)
        if (classType != ClassType.ARCHER || gameItem.weaponTemplate.classType != ClassType.ARCHER)
            return

        if (player.getCooldown(Material.BOW) > 0) return

        if (gameItem.weaponTemplate.level > player.level) {
            player.playSound(player.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f)
            player.sendMessage(
                Component.text("Your level is too low to wield this!", NamedTextColor.RED)
            )
            return
        }

        val arrow = player.launchProjectile(Arrow::class.java)
        arrow.velocity = arrow.velocity.multiply(ARROW_SPEED_MULTIPLIER)
        arrow.shooter = player

        val bowEvent = RunicBowEvent(player, arrow)
        Bukkit.getPluginManager().callEvent(bowEvent)
        if (bowEvent.isCancelled) {
            arrow.remove()
            return
        }

        autoAttackArrows.add(arrow.uniqueId)

        player.playSound(player.location, Sound.ENTITY_ARROW_SHOOT, 0.25f, 1.0f)

        val minDamage = gameItem.weaponTemplate.damage.min
        val maxDamage = gameItem.weaponTemplate.damage.max
        val attackEvent =
            BasicAttackEvent(
                player,
                Material.BOW,
                BasicAttackEvent.BASE_BOW_COOLDOWN,
                BasicAttackEvent.BASE_BOW_COOLDOWN.toDouble(),
                minDamage,
                maxDamage,
            )
        Bukkit.getPluginManager().callEvent(attackEvent)
    }

    private companion object {
        const val ARROW_SPEED_MULTIPLIER = 1.75
    }
}
