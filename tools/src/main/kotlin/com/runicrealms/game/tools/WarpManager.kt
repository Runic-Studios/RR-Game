package com.runicrealms.game.tools

import com.google.inject.Inject
import java.io.File
import java.io.IOException
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory

class WarpManager @Inject constructor(private val plugin: JavaPlugin) {

    private val logger = LoggerFactory.getLogger("tools")
    private val warps: LinkedHashMap<String, Location> = linkedMapOf()

    init {
        read()
    }

    fun getWarp(warp: String): Location? = warps[warp]

    fun getWarps(): Set<String> = warps.keys

    fun addWarp(warp: String, location: Location): Boolean {
        if (warps.containsKey(warp)) return false
        warps[warp] = location
        write()
        return true
    }

    fun removeWarp(warp: String): Boolean {
        val removed = warps.remove(warp) != null
        if (removed) write()
        return removed
    }

    private fun read() {
        val config =
            try {
                YamlConfiguration.loadConfiguration(configFile())
            } catch (e: IOException) {
                logger.error("Failed to load warps config", e)
                return
            }

        warps.clear()

        val warpsSection = config.getConfigurationSection("warps") ?: return

        for (name in warpsSection.getKeys(false)) {
            val warp = warpsSection.getConfigurationSection(name) ?: continue

            val worldName = warp.getString("world")
            val x = if (warp.isDouble("x")) warp.getDouble("x") else null
            val y = if (warp.isDouble("y")) warp.getDouble("y") else null
            val z = if (warp.isDouble("z")) warp.getDouble("z") else null
            val yaw = if (warp.isDouble("yaw")) warp.getDouble("yaw") else null
            val pitch = if (warp.isDouble("pitch")) warp.getDouble("pitch") else null

            if (worldName == null || x == null || y == null || z == null || yaw == null || pitch == null) {
                continue
            }

            val world = Bukkit.getWorld(worldName)
            if (world == null) {
                logger.error("World '{}' from warps.yml does not exist", worldName)
                continue
            }

            warps[name] = Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
        }
    }

    private fun write() {
        val config = YamlConfiguration()
        val warpsSection = config.createSection("warps")

        for ((name, location) in warps) {
            if (!location.isWorldLoaded) continue
            val warp = warpsSection.createSection(name)
            warp.set("world", location.world!!.name)
            warp.set("x", location.x)
            warp.set("y", location.y)
            warp.set("z", location.z)
            warp.set("yaw", location.yaw)
            warp.set("pitch", location.pitch)
        }

        try {
            config.save(configFile())
        } catch (e: IOException) {
            logger.error("Failed to save warps config", e)
        }
    }

    private fun configFile(): File {
        val home = plugin.dataFolder
        if (!home.exists()) home.mkdir()
        val file = File(home, "warps.yml")
        if (!file.exists()) file.createNewFile()
        return file
    }
}
