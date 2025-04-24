package com.runicrealms.game.gameplay.data

import com.google.inject.Inject
import com.google.protobuf.Timestamp
import com.runicrealms.game.data.event.GamePlayerDataLoadEvent
import java.time.Instant
import org.bukkit.event.EventHandler
import org.bukkit.plugin.Plugin

class PlayerTraitDataManager @Inject constructor(plugin: Plugin) {

    @EventHandler
    fun onGamePlayerCreate(event: GamePlayerDataLoadEvent) {
        with(event.playerData) {
            traits.data.setLastLogin(
                Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build()
            )
            stageChanges(traits)
        }
    }
}
