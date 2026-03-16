package com.runicrealms.game.gameplay.spell.api

import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import java.util.Optional
import java.util.UUID

/** Public API for querying and registering [SpellEffect] instances. */
interface SpellEffectAPI {
    fun addSpellEffectToManager(spellEffect: SpellEffect)

    fun hasSpellEffect(uuid: UUID, effectType: SpellEffectType): Boolean

    fun getSpellEffect(
        casterUuid: UUID,
        recipientUuid: UUID,
        identifier: SpellEffectType,
    ): Optional<SpellEffect>

    fun getSpellEffects(recipientId: UUID, identifier: SpellEffectType): List<SpellEffect>

    /**
     * Returns the highest stack count across all active [SpellEffect]s of the given type on the
     * recipient.
     */
    fun determineHighestStacks(recipientId: UUID, identifier: SpellEffectType): Int
}
