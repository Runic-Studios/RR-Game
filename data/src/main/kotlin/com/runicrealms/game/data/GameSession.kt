package com.runicrealms.game.data

import com.runicrealms.trove.client.user.UserCharacterData
import com.runicrealms.trove.client.user.UserClaim
import com.runicrealms.trove.client.user.UserPlayerData
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import org.bukkit.entity.Player

data class GameSession(
    val claim: UserClaim,
    val playerData: UserPlayerData,
    val bukkitPlayer: Player,
    val saveJob: Job,
) {
    var characterData: UserCharacterData? = null
        internal set

    val characterMutex = Mutex()
}
