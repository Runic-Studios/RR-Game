package com.runicrealms.game.items.config.item

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.runicrealms.game.common.config.converter.MaterialConverter
import com.runicrealms.game.common.config.converter.TextComponentConverter
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/** Jackson dataformat yaml pojo: Used as the "display" section in an item template */
data class DisplayableItem(
    @JsonDeserialize(converter = TextComponentConverter::class) val name: TextComponent,
    @JsonDeserialize(converter = MaterialConverter::class) val material: Material,
    val damage: Short? = null,
) {

    fun generateItem(count: Int): ItemStack {
        val item = ItemStack(material)
        var meta = item.itemMeta
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.type)
        }
        if (meta is Damageable && damage != null) {
            meta.damage = damage.toInt()
        }
        meta!!.itemName(name)
        item.setItemMeta(meta)
        item.amount = count
        return item
    }
}
