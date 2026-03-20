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
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.plugin.Plugin

/**
 * Handles custom bow mechanics for the Archer class.
 *
 * Intercepts [EntityShootBowEvent] for class, cooldown, and level verification, tags auto-attack
 * arrows via [autoAttackArrows], and applies [DamageHandler.dealPhysicalDamage] when the arrow
 * hits an entity in [ProjectileHitEvent].
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
    @EventHandler(priority = EventPriority.NORMAL)
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

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBowShoot(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return

        // Cancel vanilla bow shooting for non-Archers or invalid weapons
        val bow = event.bow
        if (bow == null) {
            event.isCancelled = true
            return
        }

        val gameItem = itemStackConverter.convertToGameItem(bow) as? GameItemWeapon
        if (gameItem == null) {
            event.isCancelled = true
            return
        }

        val classType = spellManager.getPlayerClassType(player.uniqueId)
        if (classType != ClassType.ARCHER || gameItem.weaponTemplate.classType != ClassType.ARCHER) {
            event.isCancelled = true
            return
        }

        if (player.getCooldown(bow.type) > 0) {
            event.isCancelled = true
            return
        }

        if (gameItem.weaponTemplate.level > player.level) {
            player.playSound(player.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f)
            player.sendMessage(Component.text("Your level is too low to wield this!", NamedTextColor.RED))
            event.isCancelled = true
            return
        }

        val arrow = event.projectile as? Arrow ?: return

        val bowEvent = RunicBowEvent(player, arrow)
        Bukkit.getPluginManager().callEvent(bowEvent)
        if (bowEvent.isCancelled) {
            event.isCancelled = true
            return
        }

        autoAttackArrows.add(arrow.uniqueId)

        // Boost arrow speed on the following tick so vanilla velocity is set first
        plugin.server.scheduler.runTask(plugin, Runnable { arrow.velocity = arrow.velocity.multiply(1.75) })

        val minDamage = gameItem.weaponTemplate.damage.min
        val maxDamage = gameItem.weaponTemplate.damage.max
        val attackEvent =
            BasicAttackEvent(
                player,
                bow.type,
                BasicAttackEvent.BASE_BOW_COOLDOWN,
                BasicAttackEvent.BASE_BOW_COOLDOWN.toDouble(),
                minDamage,
                maxDamage,
            )
        Bukkit.getPluginManager().callEvent(attackEvent)
    }
}
