package com.runicrealms.game.gameplay.player.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import com.runicrealms.game.gameplay.spell.event.RunicBowEvent
import org.bukkit.entity.Arrow
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
 * This listener:
 * 1. Intercepts [EntityShootBowEvent] for players and fires [RunicBowEvent]
 * 2. Removes arrows on hit ([ProjectileHitEvent]) to prevent arrow stacking on mobs
 * 3. Cancels vanilla arrow pickup ([PlayerPickupArrowEvent])
 * 4. Applies bow attack cooldown
 * 5. Enforces class (Archer only) and level requirements
 *
 * TODO: Implement this listener once the following are available:
 * - Class check: player must be Archer (ClassType.ARCHER) to fire bow attacks
 * - Item level requirement read via ItemStackConverter / GameItemWeapon
 * - DamageHandler integration for arrow hit PhysicalDamageEvent (RangedDamageEvent)
 * - BasicAttackEvent firing with bow-specific cooldown (BASE_BOW_COOLDOWN)
 * - EntityTargetEvent handling to prevent mobs targeting arrow source
 */
@Singleton
class BowListener
@Inject
constructor(
    private val plugin: Plugin,
    private val combatManager: CombatManager,
) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /** Cancels vanilla arrow pickup so arrows do not clutter the player's inventory. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onArrowPickup(event: PlayerPickupArrowEvent) {
        event.isCancelled = true
    }

    /** Removes arrows immediately on projectile hit to prevent them sticking in mobs. */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (projectile !is Arrow) return
        if (projectile.shooter !is Player) return
        plugin.server.scheduler.runTaskLater(plugin, Runnable { projectile.remove() }, 1L)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBowShoot(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return

        // TODO: Verify player is Archer class
        //   val gameCharacter = userDataRegistry.getCharacter(player.uniqueId) ?: return
        //   if (gameCharacter.document.character.traits.classType != ClassType.ARCHER) return

        // TODO: Check bow level requirement
        //   val weapon = ItemStackConverter.convertToGameItem(player.inventory.itemInMainHand)
        //   if (weapon is GameItemWeapon && weapon.level > traits.level) {
        //       player.sendMessage(RED + "You must be level ${weapon.level} to use that weapon!")
        //       event.isCancelled = true
        //       return
        //   }

        // TODO: Check attack cooldown and fire BasicAttackEvent / RunicBowEvent
        //   val arrow = event.projectile as? Arrow ?: return
        //   val bowEvent = RunicBowEvent(player, arrow)
        //   Bukkit.getPluginManager().callEvent(bowEvent)
        //   if (bowEvent.isCancelled) { event.isCancelled = true; return }
        //   Apply bow cooldown via BasicAttackEvent
    }
}
