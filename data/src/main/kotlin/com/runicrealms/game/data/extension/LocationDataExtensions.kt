package com.runicrealms.game.data.extension

import com.runicrealms.game.common.util.ALTERRA_NAME
import com.runicrealms.game.common.util.DUNGEONS_NAME
import com.runicrealms.trove.generated.api.schema.v1.LocationData
import org.bukkit.Bukkit
import org.bukkit.Location

fun LocationData.toBukkit(): Location {
    return Location(Bukkit.getWorld(world.toName()), x, y, z, yaw, pitch)
}

fun LocationData.World.toName(): String {
    return when (this) {
        LocationData.World.ALTERRA -> ALTERRA_NAME
        LocationData.World.DUNGEONS -> DUNGEONS_NAME
        LocationData.World.UNRECOGNIZED -> throw IllegalArgumentException("Unrecognized world")
    }
}

fun String.toLocationDataWorld(): LocationData.World {
    return when (this) {
        ALTERRA_NAME -> LocationData.World.ALTERRA
        DUNGEONS_NAME -> LocationData.World.DUNGEONS
        else -> throw IllegalArgumentException("Unrecognized world")
    }
}

fun Location.toTrove(): LocationData {
    return LocationData.newBuilder()
        .setWorld(world.name.toLocationDataWorld())
        .setX(x)
        .setY(y)
        .setZ(z)
        .setYaw(yaw)
        .setPitch(pitch)
        .build()
}
