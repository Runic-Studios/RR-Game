package com.runicrealms.game.gameplay.spell.effect

import org.bukkit.Sound

/**
 * Crowd-control and buff status effects. These are hard status effects managed by
 * [StatusEffectManager] (as opposed to the softer proc effects in [SpellEffectType]).
 */
enum class RunicStatusEffect(
    val displayName: String,
    val description: String,
    val isBuff: Boolean,
    private val effectMessage: String,
    val sound: Sound,
) {
    DISARM(
        "Disarm",
        "Enemy cannot use basic attacks!",
        false,
        "disarmed!",
        Sound.ENTITY_ITEM_BREAK,
    ),
    INVULNERABILITY(
        "Invulnerability",
        "You cannot take damage!",
        true,
        "invulnerability!",
        Sound.ITEM_TOTEM_USE,
    ),
    ROOT(
        "Root",
        "Enemy cannot move! Effect broken by damage.",
        false,
        "rooted!",
        Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,
    ),
    SILENCE("Silence", "Enemy cannot cast spells!", false, "silenced!", Sound.ENTITY_CHICKEN_DEATH),
    SLOW_I("Slow", "Enemy movement speed reduced!", false, "slowed!", Sound.BLOCK_SOUL_SAND_BREAK),
    SLOW_II("Slow", "Enemy movement speed reduced!", false, "slowed!", Sound.BLOCK_SOUL_SAND_BREAK),
    SLOW_III(
        "Slow",
        "Enemy movement speed greatly reduced!",
        false,
        "slowed!",
        Sound.BLOCK_SOUL_SAND_BREAK,
    ),
    SPEED_I(
        "Speed",
        "Movement speed increased!",
        true,
        "speed!",
        Sound.ENTITY_FIREWORK_ROCKET_BLAST,
    ),
    SPEED_II(
        "Speed",
        "Movement speed increased!",
        true,
        "speed!",
        Sound.ENTITY_FIREWORK_ROCKET_BLAST,
    ),
    SPEED_III(
        "Speed",
        "Movement speed greatly increased!",
        true,
        "speed!",
        Sound.ENTITY_FIREWORK_ROCKET_BLAST,
    ),
    STUN(
        "Stun",
        "Enemy cannot cast spells, deal damage, or move!",
        false,
        "stunned!",
        Sound.BLOCK_GLASS_BREAK,
    );

    /** Returns the chat message shown to the affected entity. */
    fun getMessage(): String =
        if (isBuff) "You have gained $effectMessage" else "You have been $effectMessage"
}
