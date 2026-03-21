package com.runicrealms.game.gameplay.spell.spelltypes

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import java.util.Optional
import java.util.UUID
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** Public contract for all spells. */
interface ISpell {
    fun hasSpellEffect(uuid: UUID, identifier: SpellEffectType): Boolean

    fun getSpellEffect(
        casterUuid: UUID,
        recipientUuid: UUID,
        identifier: SpellEffectType,
    ): Optional<SpellEffect>

    fun getSpellEffects(recipientId: UUID, identifier: SpellEffectType): List<SpellEffect>

    fun determineHighestStacks(recipientId: UUID, identifier: SpellEffectType): Int

    fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
        applier: LivingEntity? = null,
    )

    fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
    )

    /**
     * Executes this spell for the given player. Returns true if the spell was successfully cast.
     *
     * TODO (SPELL_MIGRATION.md #1): Decide on the execute() signature. Options: (a) Pass
     * GameCharacter as a third parameter (b) Look up GameCharacter from UserDataRegistry inside
     * execute() [current approach] (c) Keep Player-only and use withSyncCharacterData
     */
    fun execute(player: Player, type: SpellItemType): Boolean

    val cooldown: Double
    val description: String
    val manaCost: Int
    val name: String
    val reqClass: ClassType

    fun hasPassive(uuid: UUID, passive: String): Boolean

    fun hasStatusEffect(uuid: UUID, effect: RunicStatusEffect): Boolean

    fun healPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell? = null)

    fun isOnCooldown(player: Player): Boolean

    fun isValidAlly(caster: Player, recipient: Entity): Boolean

    fun isValidEnemy(caster: Player, victim: Entity): Boolean

    fun percentMaxHealth(entity: LivingEntity, percent: Double): Int

    fun percentMissingHealth(entity: LivingEntity, percent: Double, cap: Int): Int

    fun removeStatusEffect(entity: Entity, effect: RunicStatusEffect): Boolean

    fun shieldPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell? = null)
}
