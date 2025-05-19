package com.runicrealms.game.common

import java.net.URI
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta


/**
 * Returns a skinned head
 *
 * @param value from minecraft-heads.com
 * @return head item stack for use in menus
 */
fun getHead(value: String): ItemStack {
    val head = ItemStack(Material.PLAYER_HEAD)
    val skullMeta = head.itemMeta as SkullMeta
    val profile = Bukkit.createProfile(UUID.randomUUID())
    profile.textures.skin = URI.create(value).toURL()
    skullMeta.playerProfile = profile
    head.setItemMeta(skullMeta)
    return head
}
