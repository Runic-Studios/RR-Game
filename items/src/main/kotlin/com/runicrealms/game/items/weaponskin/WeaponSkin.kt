package com.runicrealms.game.items.weaponskin

import org.bukkit.Material

data class WeaponSkin(
    val id: String,
    val name: String?,
    val material: Material,
    val damage: Int,
    val classType: String?, // TODO: Replace with CharacterClass when implemented
    val permission: String? = null,
    val rank: List<String>? = null, // TODO: Replace with DonorRank when implemented
    val achievementID: String? = null, // TODO: Achievement system not yet implemented
)
