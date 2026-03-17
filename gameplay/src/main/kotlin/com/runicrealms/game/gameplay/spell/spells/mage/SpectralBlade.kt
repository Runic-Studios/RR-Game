package com.runicrealms.game.gameplay.spell.spells.mage

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.event.StaffAttackEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spellutil.particles.SlashEffect
import com.runicrealms.game.items.generator.GameItemWeapon
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * Passive (Pyromancer/Arcanist passive).
 *
 * While shielded, basic attacks and staff attacks use the [ArcaneSlash] hitbox and deal magic
 * damage instead of physical damage. The damage is scaled by weapon damage + STR % bonus, then
 * passed through to INT scaling via [MagicDamageEvent].
 *
 * Weapon damage is read from the held weapon template when available.
 *
 */
class SpectralBlade(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.MAGE, deps) {

    override var cooldown = 0.0
    override var manaCost = 0
    override var description =
        "Passive: While shielded, basic attacks use Arcane Slash's hitbox and deal magic damage."

    init {
        isPassive = true
        displayCastMessage = false
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!event.isBasicAttack) return
        if (!spellManager.isShielded(event.caster)) return
        if (!hasPassive(event.caster.uniqueId, SPELL_NAME)) return
        event.setCancelled(true)
        bladeAttack(event.caster, resolveWeaponDamage(event.caster))
    }

    @EventHandler(ignoreCancelled = true)
    fun onStaffAttack(event: StaffAttackEvent) {
        if (!spellManager.isShielded(event.player)) return
        if (!hasPassive(event.player.uniqueId, SPELL_NAME)) return
        event.setCancelled(true)
        bladeAttack(event.player, resolveWeaponDamage(event.player))
    }

    private fun bladeAttack(player: Player, weaponDamage: Int) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2.0f)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f)

        val distance =
            (spellManager.getSpell(ArcaneSlash.SPELL_NAME) as? DistanceSpell)?.distance
                ?: ArcaneSlash.BASE_DISTANCE
        val beamWidth = ArcaneSlash.BEAM_WIDTH

        val rayTraceResult =
            player.world.rayTraceEntities(
                player.location,
                player.location.direction,
                distance,
                beamWidth,
            ) { entity ->
                entity != player && isValidEnemy(player, entity)
            }

        if (rayTraceResult?.hitEntity == null) {
            SlashEffect.slashHorizontal(player, Particle.WITCH, player.location)
            return
        }

        val hitEntity = rayTraceResult.hitEntity as? LivingEntity ?: return
        SlashEffect.slashHorizontal(player, Particle.WITCH, player.location)
        hitEntity.world.playSound(hitEntity.location, Sound.ENTITY_PLAYER_HURT, 0.5f, 2.0f)

        // Gather all enemies in the beam width around the hit entity (AoE like ArcaneSlash)
        val targets =
            player.world.getNearbyEntities(hitEntity.location, beamWidth, beamWidth, beamWidth) {
                entity ->
                entity != player && isValidEnemy(player, entity)
            }

        val finalDamage = weaponDamage.toDouble()

        for (target in targets) {
            if (target !is LivingEntity) continue
            deps.damageHandler.dealMagicDamage(finalDamage.toInt(), target, player, this)
        }
    }

    private fun resolveWeaponDamage(player: Player): Int {
        val gameItem = deps.itemStackConverter.convertToGameItem(player.inventory.itemInMainHand)
        val weapon = gameItem as? GameItemWeapon ?: return BASE_DAMAGE.toInt()
        val range = weapon.weaponTemplate.damage
        return (range.min..range.max).random()
    }

    companion object {
        const val SPELL_NAME = "Spectral Blade"
        const val BASE_DAMAGE = 15.0
    }
}
