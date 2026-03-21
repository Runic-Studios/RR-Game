package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta

class SacredSpring(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps), HealingSpell, MagicDamageSpell, RadiusSpell {
    override var healAmount = BASE_HEAL
    override var healPerLevel = HEAL_PER_LEVEL
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Throw a magical vial. Allies are healed and enemies are damaged in a $radius block radius."

    override fun executeSpell(player: Player, type: SpellItemType) {
        val item = ItemStack(Material.SPLASH_POTION)
        val meta = item.itemMeta as PotionMeta
        meta.color = Color.AQUA
        item.itemMeta = meta

        val thrownPotion = player.launchProjectile(ThrownPotion::class.java)
        thrownPotionIds.add(thrownPotion.uniqueId)
        thrownPotion.item = item
        thrownPotion.velocity = player.location.direction.normalize().multiply(POTION_SPEED_MULT)
        thrownPotion.shooter = player
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPotionBreak(event: PotionSplashEvent) {
        if (!thrownPotionIds.contains(event.potion.uniqueId)) return
        val player = event.potion.shooter as? Player ?: return

        thrownPotionIds.remove(event.potion.uniqueId)
        event.isCancelled = true

        val expiredBomb = event.potion
        val loc = expiredBomb.location
        expiredBomb.world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f)
        expiredBomb.world.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f)
        expiredBomb.world.playSound(loc, Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 0.5f, 1.0f)

        for (entity in player.world.getNearbyEntities(loc, radius, radius, radius)) {
            if (entity is Player && isValidAlly(player, entity)) {
                healPlayer(player, entity, healAmount, this)
            }
            if (entity is LivingEntity && isValidEnemy(player, entity)) {
                val dmgEvent = MagicDamageEvent(magicDamage.toInt(), entity, player, this)
                Bukkit.getPluginManager().callEvent(dmgEvent)
                if (!dmgEvent.isCancelled) {
                    entity.damage(dmgEvent.amount.toDouble(), player)
                }
            }
        }
    }

    companion object {
        const val SPELL_NAME = "Sacred Spring"
        private const val POTION_SPEED_MULT = 1.25
        private const val BASE_HEAL = 16.0
        private const val HEAL_PER_LEVEL = 0.4
        private const val BASE_DAMAGE = 14.0
        private const val DAMAGE_PER_LEVEL = 0.35
        private const val BASE_RADIUS = 4.0
        private const val COOLDOWN = 8.0
        private const val MANA_COST = 16
        val thrownPotionIds: MutableSet<UUID> = mutableSetOf()
    }
}
