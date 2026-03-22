package com.runicrealms.game.gameplay.spell.spells.warrior

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.warrior.BleedEffect
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.scheduler.BukkitTask

/**
 * Throws a spinning axe projectile. On first enemy hit, deals physical damage and applies Bleed. If
 * the target is already bleeding, refreshes Bleed stacks and applies a slow.
 */
class AxeToss(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.WARRIOR, deps), DurationSpell, PhysicalDamageSpell {
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var duration = SLOW_DURATION
    override var physicalDamage = BASE_DAMAGE
    override var physicalDamagePerLevel = DAMAGE_PER_LEVEL
    override val description: String
        get() =
            "Throw your weapon, dealing ($physicalDamage + &f${physicalDamagePerLevel}x&7 lvl) physical⚔ damage " +
                "and applying &cbleed&7. If the enemy is already bleeding, they are slowed for ${duration}s."

    private val hasBeenHit: MutableMap<UUID, UUID> = HashMap()

    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        duration = config.getDouble("slow-duration", duration)
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        val artifact = player.inventory.itemInMainHand
        if (artifact.type == Material.AIR) return

        val durability = (artifact.itemMeta as? Damageable)?.damage ?: 0
        val thrownStack = ItemStack(artifact.type)
        val thrownMeta = thrownStack.itemMeta as? Damageable
        if (thrownMeta != null) {
            thrownMeta.damage = durability
            thrownStack.itemMeta = thrownMeta
        }

        val path = player.eyeLocation.direction.normalize().multiply(1.5)
        player.world.playSound(player.location, Sound.ENTITY_SHULKER_SHOOT, 0.5f, 1.0f)
        val projectile = player.world.dropItem(player.eyeLocation, thrownStack)
        projectile.pickupDelay = Int.MAX_VALUE
        projectile.velocity = path
        projectile.ticksLived = 1

        lateinit var task: BukkitTask
        task =
            deps.plugin.server.scheduler.runTaskTimer(
                deps.plugin,
                Runnable {
                    if (projectile.isOnGround || projectile.isDead) {
                        if (projectile.isOnGround) projectile.remove()
                        task.cancel()
                        return@Runnable
                    }

                    val loc = projectile.location
                    projectile.world.spawnParticle(Particle.CRIT, loc, 1, 0.0, 0.0, 0.0, 0.0)

                    for (entity in
                        projectile.world.getNearbyEntities(loc, 1.5, 1.5, 1.5) { target ->
                            isValidEnemy(player, target)
                        }) {
                        if (hasBeenHit[player.uniqueId] == entity.uniqueId) continue
                        hasBeenHit[player.uniqueId] = entity.uniqueId
                        val victim = entity as? LivingEntity ?: continue

                        val bleedOpt =
                            getSpellEffect(player.uniqueId, victim.uniqueId, SpellEffectType.BLEED)
                        if (bleedOpt.isEmpty) {
                            BleedEffect(
                                    caster = player,
                                    recipient = victim,
                                    duration = BLEED_DURATION,
                                    spellEffectAPI = deps.spellEffectAPI,
                                    damageHandler = deps.damageHandler,
                                )
                                .initialize()
                        } else {
                            (bleedOpt.get() as? BleedEffect)?.let { existing ->
                                // Mirror Java behaviour: reset stacks/timer by re-initializing a
                                // fresh effect.
                                existing.cancel()
                                BleedEffect(
                                        caster = player,
                                        recipient = victim,
                                        duration = BLEED_DURATION,
                                        spellEffectAPI = deps.spellEffectAPI,
                                        damageHandler = deps.damageHandler,
                                    )
                                    .initialize()
                            }
                            addStatusEffect(victim, RunicStatusEffect.SLOW_III, duration, true)
                            victim.world.spawnParticle(
                                Particle.ANGRY_VILLAGER,
                                victim.location,
                                5,
                                0.5,
                                0.5,
                                0.5,
                                0.0,
                            )
                        }

                        victim.world.playSound(
                            victim.location,
                            Sound.ENTITY_BLAZE_SHOOT,
                            0.5f,
                            0.2f,
                        )
                        dealPhysicalDamage(player, victim, physicalDamage.toInt())
                        projectile.remove()
                        task.cancel()
                        return@Runnable
                    }
                },
                0L,
                1L,
            )

        deps.plugin.server.scheduler.runTaskLater(
            deps.plugin,
            Runnable { hasBeenHit.clear() },
            (duration * 20.0).toLong(),
        )
    }

    companion object {
        const val SPELL_NAME = "Axe Toss"
        const val COOLDOWN = 8.0
        const val MANA_COST = 20
        const val BASE_DAMAGE = 20.0
        const val DAMAGE_PER_LEVEL = 1.0
        const val SLOW_DURATION = 2.5
        const val BLEED_DURATION = 6.0
    }

    private fun dealPhysicalDamage(caster: Player, victim: LivingEntity, amount: Int) {
        val event = PhysicalDamageEvent(amount, victim, caster, false, false, this)
        Bukkit.getPluginManager().callEvent(event)
        if (!event.isCancelled) {
            victim.damage(event.amount.toDouble(), caster)
        }
    }
}
