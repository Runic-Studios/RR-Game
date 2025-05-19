package com.runicrealms.game.common

import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Returns a skinned head
 *
 * @param value from minecraft-heads.com
 * @return head item stack for use in menus
 */
fun getHead(value: String): ItemStack {
    val skull = ItemStack(Material.PLAYER_HEAD)
    val hashAsId = UUID(value.hashCode().toLong(), value.hashCode().toLong())
    @Suppress("deprecation")
    return Bukkit.getUnsafe()
        .modifyItemStack(
            skull,
            "{SkullOwner:{Id:\"$hashAsId\",Properties:{textures:[{Value:\"$value\"}]}}}",
        )
}
