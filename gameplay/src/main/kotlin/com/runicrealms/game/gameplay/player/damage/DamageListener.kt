package com.runicrealms.game.gameplay.player.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin

/**
 * Handles core melee (non-bow, non-staff) damage from players to entities.
 *
 * This listener:
 * 1. Verifies the attacker has the correct weapon type for their class
 * 2. Checks the attack cooldown
 * 3. Checks the player's level meets the weapon's minimum level requirement
 * 4. Fires [BasicAttackEvent] so cooldowns and stat scaling are applied
 * 5. Cancels the vanilla damage and fires the appropriate custom damage event
 * 6. Handles death via [RunicDeathEvent] on fatal hits
 *
 * TODO: Implement this listener once the following are available:
 * - Weapon-class matching logic (ClassType -> allowed WeaponType set)
 * - Item level requirement read via ItemStackConverter / GameItemWeapon
 * - DamageHandler integration for custom damage calculation
 * - RunicDeathEvent wiring for mob kill detection
 * - MythicMobs entity death hook
 */
@Singleton
class DamageListener
@Inject
constructor(
    private val plugin: Plugin,
    private val combatManager: CombatManager,
) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return

        // TODO: Add weapon-class verification
        //   val gameCharacter = userDataRegistry.getCharacter(attacker.uniqueId) ?: return
        //   val classType = gameCharacter.document.character.traits.classType
        //   val weapon = ItemStackConverter.convertToGameItem(attacker.inventory.itemInMainHand)
        //   if (weapon is GameItemWeapon && !classType.allowedWeaponTypes.contains(weapon.weaponType)) {
        //       attacker.sendMessage(RED + "Your class cannot use that weapon!")
        //       event.isCancelled = true
        //       return
        //   }

        // TODO: Add attack cooldown check
        //   val material = attacker.inventory.itemInMainHand.type
        //   if (attacker.getCooldown(material) > 0) {
        //       event.isCancelled = true
        //       return
        //   }

        // TODO: Add level requirement check
        //   if (weapon is GameItemWeapon && weapon.level > gameCharacter.document.character.traits.level) {
        //       attacker.sendMessage(RED + "You must be level ${weapon.level} to use that weapon!")
        //       event.isCancelled = true
        //       return
        //   }

        // TODO: Fire BasicAttackEvent with correct cooldown and damage values from DamageHandler
        //   val material = attacker.inventory.itemInMainHand.type
        //   val attackEvent = BasicAttackEvent(attacker, material, BASE_MELEE_COOLDOWN, BASE_MELEE_COOLDOWN.toDouble(), damage, maxDamage)
        //   Bukkit.getPluginManager().callEvent(attackEvent)
        //   if (attackEvent.isCancelled) return

        // TODO: Cancel vanilla damage and fire PhysicalDamageEvent via DamageHandler
        //   event.isCancelled = true
        //   damageHandler.applyMeleeDamage(attacker, event.entity as? LivingEntity ?: return, attackEvent.damage)

        // TODO: Handle mob death -> RunicDeathEvent / loot drop
    }
}
