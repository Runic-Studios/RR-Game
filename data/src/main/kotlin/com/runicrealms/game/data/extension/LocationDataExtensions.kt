package com.runicrealms.game.data.extension

import com.runicrealms.game.common.WorldType
import com.runicrealms.game.common.util.ALTERRA_NAME
import com.runicrealms.game.common.util.DUNGEONS_NAME
import com.runicrealms.game.data.model.LocationData
import org.bukkit.Bukkit
import org.bukkit.Location

fun LocationData.toBukkit(): Location {
    return Location(Bukkit.getWorld(world.toWorldName()), x, y, z, yaw, pitch)
}

fun WorldType.toWorldName(): String {
    return when (this) {
        WorldType.ALTERRA -> ALTERRA_NAME
        WorldType.DUNGEONS -> DUNGEONS_NAME
    }
}

fun String.toWorldType(): WorldType {
    return when (this) {
        ALTERRA_NAME -> WorldType.ALTERRA
        DUNGEONS_NAME -> WorldType.DUNGEONS
        else -> throw IllegalArgumentException("Unrecognized world name: $this")
    }
}

fun Location.toLocationData(): LocationData {
    return LocationData(
        world = (world?.name ?: ALTERRA_NAME).toWorldType(),
        x = x,
        y = y,
        z = z,
        pitch = pitch,
        yaw = yaw,
    )
}
