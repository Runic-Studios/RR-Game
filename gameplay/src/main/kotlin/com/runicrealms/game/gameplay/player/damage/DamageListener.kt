package com.runicrealms.game.gameplay.player.damage

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import com.runicrealms.game.gameplay.spell.damage.DamageHandler
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.EnemyVerifyEvent
import com.runicrealms.game.items.generator.GameItemWeapon
import com.runicrealms.game.items.generator.ItemStackConverter
import java.util.concurrent.ThreadLocalRandom
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin

/**
 * Handles core melee (non-bow, non-staff) damage from players to entities.
 *
 * Warrior, Rogue, and Cleric use direct melee attacks via [EntityDamageByEntityEvent]. Mage (staff)
 * attacks are handled by [StaffListener] via [StaffAttackEvent]. Archer (bow) attacks are handled
 * by [BowListener] via [EntityShootBowEvent].
 */
@Singleton
class DamageListener
@Inject
constructor(
    private val plugin: Plugin,
    private val combatManager: CombatManager,
    private val spellManager: SpellManager,
    private val itemStackConverter: ItemStackConverter,
    private val damageHandler: DamageHandler,
) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        // DamageHandler.dealPhysicalDamage calls victim.damage() which re-fires this event.
        // Let that re-trigger through so the HP change actually applies.
        if (damageHandler.isDealing) return

        val attacker = event.damager as? Player ?: return

        // Always cancel vanilla damage; Runic replaces it with PhysicalDamageEvent
        event.isCancelled = true

        val item = attacker.inventory.itemInMainHand
        val gameItem = itemStackConverter.convertToGameItem(item) as? GameItemWeapon ?: return

        val classType = spellManager.getPlayerClassType(attacker.uniqueId)

        // Weapon must belong to this player's class
        if (gameItem.weaponTemplate.classType != classType) return

        // Staff (Mage) and bow (Archer) attacks are handled by their own listeners
        if (classType == ClassType.MAGE || classType == ClassType.ARCHER) return

        val material = item.type
        if (attacker.getCooldown(material) > 0) return

        if (gameItem.weaponTemplate.level > attacker.level) {
            attacker.playSound(attacker.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f)
            attacker.sendMessage(
                Component.text("Your level is too low to wield this!", NamedTextColor.RED)
            )
            return
        }

        val victim = event.entity as? LivingEntity ?: return

        val enemyVerifyEvent = EnemyVerifyEvent(attacker, victim)
        Bukkit.getPluginManager().callEvent(enemyVerifyEvent)
        if (enemyVerifyEvent.isCancelled) return

        val minDamage = gameItem.weaponTemplate.damage.min
        val maxDamage = gameItem.weaponTemplate.damage.max
        val randomNum =
            if (maxDamage > minDamage) ThreadLocalRandom.current().nextInt(minDamage, maxDamage + 1)
            else minDamage

        val attackEvent =
            BasicAttackEvent(
                attacker,
                material,
                BasicAttackEvent.BASE_MELEE_COOLDOWN,
                BasicAttackEvent.BASE_MELEE_COOLDOWN.toDouble(),
                minDamage,
                maxDamage,
            )
        Bukkit.getPluginManager().callEvent(attackEvent)
        if (attackEvent.isCancelled) return

        damageHandler.dealPhysicalDamage(
            randomNum,
            victim,
            attacker,
            isBasicAttack = true,
            isRanged = false,
        )
    }
}
