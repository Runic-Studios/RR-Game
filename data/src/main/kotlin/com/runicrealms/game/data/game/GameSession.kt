package com.runicrealms.game.data.game

import com.runicrealms.trove.client.user.UserCharacterData
import com.runicrealms.trove.client.user.UserClaim
import com.runicrealms.trove.client.user.UserPlayerData
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import org.bukkit.entity.Player

/**
 * This class represents any user that has connected to the server, and that we have loaded their
 * player data (but not necessarily character data).
 *
 * This class is internal only to the data module. No interaction with it can occur outside.
 *
 * For modifying player and character data, see GamePlayer and GameCharacter.
 */
internal data class GameSession(
    val claim: UserClaim,
    val playerData: UserPlayerData,
    val bukkitPlayer: Player,
    val saveJob: Job,
) {
    var characterData: UserCharacterData? = null
        internal set

    internal val characterMutex = Mutex()
}
