package com.runicrealms.game.items.character

import java.util.UUID

interface CharacterEquipmentCacheRegistry {

    val cachedPlayerStats: Map<UUID, CharacterEquipmentCache>

}