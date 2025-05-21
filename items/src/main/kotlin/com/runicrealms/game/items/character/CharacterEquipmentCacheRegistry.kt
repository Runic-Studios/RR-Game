package com.runicrealms.game.items.character

import com.runicrealms.game.data.game.GameCharacter
import java.util.UUID

interface CharacterEquipmentCacheRegistry {

    val cachedCharacterStats: Map<UUID, CharacterEquipmentCache>

    fun getAddedCharacterStats(gameCharacter: GameCharacter): AddedStats? {
        return cachedCharacterStats[gameCharacter.bukkitPlayer.uniqueId]?.totalStats
    }
}
