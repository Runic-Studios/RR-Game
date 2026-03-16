package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.effect.cleric.SongOfWarEffect
import com.runicrealms.game.gameplay.spell.event.MagicDamageEvent
import com.runicrealms.game.gameplay.spell.event.PhysicalDamageEvent
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spellutil.KnockbackUtil
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class Battlecry(deps: SpellDependencies) :
    Spell(SPELL_NAME, ClassType.CLERIC, deps),
    AttributeSpell,
    DurationSpell,
    MagicDamageSpell,
    RadiusSpell,
    Tempo.Influenced {
    override var attribute = "intelligence"
    override var attributeBaseValue = BASE_ATTRIBUTE
    override var attributeMultiplier = BASE_MULTIPLIER
    override var duration = BASE_DURATION
    override var magicDamage = BASE_DAMAGE
    override var magicDamagePerLevel = DAMAGE_PER_LEVEL
    override var radius = BASE_RADIUS
    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Shout in a $radius block radius, damaging enemies and empowering allies with Song of War."

    var knockback = BASE_KNOCKBACK

    override fun executeSpell(player: Player, type: SpellItemType) {
        val buffDuration = effectiveDuration(player)
        applySongOfWar(player, player)

        for (entity in player.getNearbyEntities(radius, radius, radius)) {
            val living = entity as? LivingEntity ?: continue
            if (entity is Player && isValidAlly(player, entity)) {
                applySongOfWar(player, entity, buffDuration)
                continue
            }
            if (!isValidEnemy(player, living)) continue
            val event = MagicDamageEvent(magicDamage.toInt(), living, player, this)
            Bukkit.getPluginManager().callEvent(event)
            if (!event.isCancelled) {
                living.damage(event.amount.toDouble(), player)
                val direction = living.location.toVector().subtract(player.location.toVector())
                KnockbackUtil.knockBackCustom(living, direction, knockback)
            }
        }

        player.world.playSound(
            player.location,
            Sound.ENTITY_ENDER_DRAGON_GROWL,
            SoundCategory.AMBIENT,
            0.5f,
            1.0f,
        )
        removeExtraDuration(player)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPhysicalDamage(event: PhysicalDamageEvent) {
        if (!hasSongOfWar(event.caster)) return
        val bonus = (event.amount * percentAttribute(event.caster)).toInt()
        event.amount += bonus
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMagicDamage(event: MagicDamageEvent) {
        if (!hasSongOfWar(event.caster)) return
        val bonus = (event.amount * percentAttribute(event.caster)).toInt()
        event.amount += bonus
    }

    private fun applySongOfWar(
        caster: Player,
        recipient: Player,
        buffDuration: Double = effectiveDuration(caster),
    ) {
        caster.world.spawnParticle(
            Particle.NOTE,
            recipient.eyeLocation,
            8,
            Math.random() * 2.0,
            Math.random(),
            Math.random() * 2.0,
        )
        if (
            getSpellEffect(caster.uniqueId, recipient.uniqueId, SpellEffectType.SONG_OF_WAR)
                .isPresent
        ) {
            return
        }
        SongOfWarEffect(
                caster,
                recipient,
                duration = buffDuration,
                spellEffectAPI = deps.spellEffectAPI,
            )
            .initialize()
    }

    private fun hasSongOfWar(player: Player): Boolean {
        return getSpellEffect(player.uniqueId, player.uniqueId, SpellEffectType.SONG_OF_WAR)
            .isPresent
    }

    private fun percentAttribute(player: Player): Double {
        // TODO: include StatAPI scaling (attributeMultiplier * playerStat) when StatAPI is
        // migrated.
        return (attributeBaseValue / 100.0).coerceAtLeast(0.0)
    }

    companion object {
        const val SPELL_NAME = "Battlecry"
        private const val BASE_DURATION = 6.0
        private const val BASE_ATTRIBUTE = 10.0
        private const val BASE_MULTIPLIER = 0.0
        private const val BASE_DAMAGE = 20.0
        private const val DAMAGE_PER_LEVEL = 0.5
        private const val BASE_RADIUS = 6.0
        private const val BASE_KNOCKBACK = 2.0
        private const val COOLDOWN = 8.0
        private const val MANA_COST = 20
    }
}
