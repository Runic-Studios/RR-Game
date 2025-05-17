package com.runicrealms.game.common.config.converter

import com.fasterxml.jackson.databind.util.StdConverter
import org.bukkit.Material

/** Jackson databind compatible converter for turning Strings into Materials */
class MaterialConverter : StdConverter<String, Material?>() {

    override fun convert(value: String): Material {
        return Material.getMaterial(value)
            ?: throw IllegalArgumentException("Unknown material: $value")
    }
}
