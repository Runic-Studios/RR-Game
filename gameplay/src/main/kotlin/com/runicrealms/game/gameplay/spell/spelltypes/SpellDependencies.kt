package com.runicrealms.game.gameplay.spell.spelltypes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.api.SkillTreeAPI
import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.api.StatusEffectAPI
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import com.runicrealms.game.gameplay.spell.damage.DamageHandler
import com.runicrealms.game.items.generator.ItemStackConverter
import org.bukkit.plugin.Plugin

/**
 * Bundles all shared dependencies that every [Spell] subclass needs. This avoids
 * constructor-parameter explosion across ~80 concrete spell implementations.
 *
 * Injected by Guice as a singleton and passed into each [Spell] constructor by [SpellRegistry].
 */
@Singleton
class SpellDependencies
@Inject
constructor(
    val plugin: Plugin,
    val spellEffectAPI: SpellEffectAPI,
    val statusEffectAPI: StatusEffectAPI,
    val skillTreeAPI: SkillTreeAPI,
    val userDataRegistry: UserDataRegistry,
    val stackTaskRegistry: StackTaskRegistry,
    val damageHandler: DamageHandler,
    val itemStackConverter: ItemStackConverter,
    val combatManager: CombatManager,
)
