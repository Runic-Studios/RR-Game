package com.runicrealms.game.items.weaponskin

import org.bukkit.Material

data class WeaponSkin(
    val id: String,
    val material: Material,
    val permission: String? = null,
    val donorRank: List<String>? = null, // TODO: Replace with DonorRank when implemented
    val achievementID: String? = null, // TODO: Achievement system not yet implemented
    val customModelData: Int = 0,
)
