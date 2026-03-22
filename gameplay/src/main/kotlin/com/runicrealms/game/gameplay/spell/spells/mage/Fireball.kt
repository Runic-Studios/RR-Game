package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.metadata.FixedMetadataValue

/**
 * Launches a SmallFireball projectile. On ProjectileHitEvent, deals magic damage to the hit entity.
 */
class Fireball(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.MAGE, deps), MagicDamageSpell {

    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override val description: String
        get() = "Launch a fireball that detonates on impact, dealing $magicDamage magic damage."

    private val playerFireballs: MutableMap<UUID, SmallFireball> = HashMap()

    override fun executeSpell(player: Player, type: SpellItemType) {
        val dir = player.location.direction.normalize().multiply(FIREBALL_SPEED)
        val fireball = player.launchProjectile(SmallFireball::class.java, dir)
        fireball.shooter = player
        fireball.setIsIncendiary(false)
        fireball.setMetadata(
            "fireball_caster",
            FixedMetadataValue(deps.plugin, player.uniqueId.toString()),
        )
        playerFireballs[player.uniqueId] = fireball
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (projectile !is SmallFireball) return
        val casterUuidStr =
            projectile.getMetadata("fireball_caster").firstOrNull()?.asString() ?: return
        val casterUuid = UUID.fromString(casterUuidStr)
        val caster = Bukkit.getPlayer(casterUuid) ?: return
        val hitEntity = event.hitEntity ?: return
        if (hitEntity !is LivingEntity) return
        if (!isValidEnemy(caster, hitEntity)) return

        deps.damageHandler.dealMagicDamage(magicDamage.toInt(), hitEntity, caster, this)
        projectile.remove()
        playerFireballs.remove(casterUuid)
    }

    companion object {
        const val SPELL_NAME = "Fireball"
        // TODO: Load from config once config system is migrated.
        const val BASE_DAMAGE = 30.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val COOLDOWN = 6.0
        const val MANA_COST = 20
        const val FIREBALL_SPEED = 2.0
    }
}
