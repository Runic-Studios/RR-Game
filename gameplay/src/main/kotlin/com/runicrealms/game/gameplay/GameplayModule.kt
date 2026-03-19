package com.runicrealms.game.gameplay

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.runicrealms.game.gameplay.character.CharacterInventoryManager
import com.runicrealms.game.gameplay.character.CharacterTraitsManager
import com.runicrealms.game.gameplay.character.util.CharacterHealthHelper
import com.runicrealms.game.gameplay.character.util.CharacterLevelHelper
import com.runicrealms.game.gameplay.character.util.SaveZoneRegistry
import com.runicrealms.game.gameplay.command.CharacterCommand
import com.runicrealms.game.gameplay.command.SetLevelCommand
import com.runicrealms.game.gameplay.mob.HerbFallDamageListener
import com.runicrealms.game.gameplay.mob.MobCleanupListener
import com.runicrealms.game.gameplay.mob.MobMechanicsListener
import com.runicrealms.game.gameplay.mob.MobTaggerListener
import com.runicrealms.game.gameplay.player.ArmorEquipListener
import com.runicrealms.game.gameplay.player.ArmorEquipRestrictionListener
import com.runicrealms.game.gameplay.player.BossTimedLootDamageListener
import com.runicrealms.game.gameplay.player.HearthstoneListener
import com.runicrealms.game.gameplay.player.PlayerInteractCorrectionListener
import com.runicrealms.game.gameplay.player.PreCommandListener
import com.runicrealms.game.gameplay.player.RegenManager
import com.runicrealms.game.gameplay.player.damage.BasicAttackListener
import com.runicrealms.game.gameplay.player.damage.BowListener
import com.runicrealms.game.gameplay.player.damage.DamageListener
import com.runicrealms.game.gameplay.player.damage.EnvironmentDamageListener
import com.runicrealms.game.gameplay.spell.AllyVerifyListener
import com.runicrealms.game.gameplay.spell.EnemyVerifyListener
import com.runicrealms.game.gameplay.spell.ShieldListener
import com.runicrealms.game.gameplay.world.DaylightCycleManager
import com.runicrealms.game.gameplay.world.VanillaRestrictionsListener
import com.runicrealms.game.gameplay.player.charselect.CharacterAddMenu
import com.runicrealms.game.gameplay.player.charselect.CharacterDeleteMenu
import com.runicrealms.game.gameplay.player.charselect.CharacterSelectHelper
import com.runicrealms.game.gameplay.player.charselect.CharacterSelectManager
import com.runicrealms.game.gameplay.player.charselect.CharacterSelectMenu
import com.runicrealms.game.gameplay.player.death.DeathListener
import com.runicrealms.game.gameplay.player.death.DeathTriggerListener
import com.runicrealms.game.gameplay.player.death.GravestoneManager
import com.runicrealms.game.gameplay.player.stat.StatListener
import com.runicrealms.game.gameplay.player.stat.StatManager
import com.runicrealms.game.gameplay.scoreboard.ScoreboardManager
import com.runicrealms.game.gameplay.spell.SpellManager
import com.runicrealms.game.gameplay.spell.SpellScalingListener
import com.runicrealms.game.gameplay.spell.SpellStaffListener
import com.runicrealms.game.gameplay.spell.SpellUseListener
import com.runicrealms.game.gameplay.spell.api.SkillTreeAPI
import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.api.StatusEffectAPI
import com.runicrealms.game.gameplay.spell.combat.CombatManager
import com.runicrealms.game.gameplay.spell.damage.DamageHandler
import com.runicrealms.game.gameplay.spell.effect.SpellEffectManager
import com.runicrealms.game.gameplay.spell.effect.StatusEffectManager
import com.runicrealms.game.gameplay.spell.skilltrees.SkillPointsListener
import com.runicrealms.game.gameplay.spell.skilltrees.SkillTreeManager
import com.runicrealms.game.gameplay.spell.skilltrees.gui.RuneListener
import com.runicrealms.game.gameplay.spell.skilltrees.gui.RuneMenu
import com.runicrealms.game.gameplay.spell.skilltrees.gui.SkillTreeMenu
import com.runicrealms.game.gameplay.spell.skilltrees.gui.SpellEditorMenu
import com.runicrealms.game.gameplay.spell.skilltrees.gui.SpellMenu
import com.runicrealms.game.gameplay.spell.skilltrees.gui.SubClassMenu
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.StackTaskRegistry
import com.runicrealms.game.gameplay.tips.TipsDataListener
import kotlin.jvm.java
import kotlin.reflect.KClass

class GameplayModule : AbstractModule() {

    override fun configure() {
        bind(TipsDataListener::class.java).asEagerSingleton()

        // --- World / vanilla restrictions ---
        bind(VanillaRestrictionsListener::class.java).asEagerSingleton()
        bind(DaylightCycleManager::class.java).asEagerSingleton()

        // --- Player listeners ---
        bind(PlayerInteractCorrectionListener::class.java).asEagerSingleton()
        bind(PreCommandListener::class.java).asEagerSingleton()
        bind(HearthstoneListener::class.java).asEagerSingleton()
        bind(BossTimedLootDamageListener::class.java).asEagerSingleton()


        // --- Damage system ---
        bind(EnvironmentDamageListener::class.java).asEagerSingleton()
        bind(BasicAttackListener::class.java).asEagerSingleton()
        bind(DamageListener::class.java).asEagerSingleton()
        bind(BowListener::class.java).asEagerSingleton()

        // --- Mob listeners ---
        bind(HerbFallDamageListener::class.java).asEagerSingleton()
        bind(MobMechanicsListener::class.java).asEagerSingleton()
        bind(MobTaggerListener::class.java).asEagerSingleton()
        bind(MobCleanupListener::class.java).asEagerSingleton()

        bind(ArmorEquipListener::class.java).asEagerSingleton()
        bind(ArmorEquipRestrictionListener::class.java).asEagerSingleton()
        bind(ScoreboardManager::class.java).asEagerSingleton()

        bind(GravestoneManager::class.java).asEagerSingleton()
        bind(DeathTriggerListener::class.java).asEagerSingleton()
        bind(DeathListener::class.java).asEagerSingleton()

        bind(RegenManager::class.java).asEagerSingleton()
        bind(StatManager::class.java).asEagerSingleton()
        bind(StatListener::class.java).asEagerSingleton()

        bind(CharacterCommand::class.java).asEagerSingleton()
        bind(SetLevelCommand::class.java).asEagerSingleton()

        bind(CharacterHealthHelper::class.java).asEagerSingleton()
        bind(CharacterLevelHelper::class.java).asEagerSingleton()
        bind(SaveZoneRegistry::class.java).asEagerSingleton()

        bind(CharacterInventoryManager::class.java).asEagerSingleton()
        bind(CharacterTraitsManager::class.java).asEagerSingleton()

        bind(CharacterSelectManager::class.java).asEagerSingleton()
        bind(CharacterSelectHelper::class.java).asEagerSingleton()
        addFactory(CharacterSelectMenu::class, CharacterSelectMenu.Factory::class)
        addFactory(CharacterAddMenu::class, CharacterAddMenu.Factory::class)
        addFactory(CharacterDeleteMenu::class, CharacterDeleteMenu.Factory::class)

        // --- Spell system ---

        // Bind API interfaces to their singleton implementations
        bind(SpellEffectAPI::class.java).to(SpellEffectManager::class.java).asEagerSingleton()
        bind(StatusEffectAPI::class.java).to(StatusEffectManager::class.java).asEagerSingleton()
        // SkillTreeManager implements SkillTreeAPI; binding to the interface ensures the same
        // singleton is returned whether injected as SkillTreeAPI or SkillTreeManager.
        bind(SkillTreeManager::class.java).asEagerSingleton()
        bind(SkillTreeAPI::class.java).to(SkillTreeManager::class.java)

        // Eager singletons that are self-wiring listeners/managers
        bind(SpellDependencies::class.java).asEagerSingleton()
        bind(StackTaskRegistry::class.java).asEagerSingleton()
        bind(DamageHandler::class.java).asEagerSingleton()
        bind(CombatManager::class.java).asEagerSingleton()
        bind(SpellManager::class.java).asEagerSingleton()
        bind(SpellUseListener::class.java).asEagerSingleton()
        bind(SpellStaffListener::class.java).asEagerSingleton()
        bind(SpellScalingListener::class.java).asEagerSingleton()
        bind(RuneListener::class.java).asEagerSingleton()
        bind(SkillPointsListener::class.java).asEagerSingleton()
        bind(ShieldListener::class.java).asEagerSingleton()
        bind(AllyVerifyListener::class.java).asEagerSingleton()
        bind(EnemyVerifyListener::class.java).asEagerSingleton()

        // Skill tree GUI factories (OdalitaMenus @AssistedInject)
        addFactory(RuneMenu::class, RuneMenu.Factory::class)
        addFactory(SubClassMenu::class, SubClassMenu.Factory::class)
        addFactory(SkillTreeMenu::class, SkillTreeMenu.Factory::class)
        addFactory(SpellMenu::class, SpellMenu.Factory::class)
        addFactory(SpellEditorMenu::class, SpellEditorMenu.Factory::class)
    }

    private fun <T : Any, U : Any> addFactory(objectType: KClass<T>, factoryType: KClass<U>) {
        install(
            FactoryModuleBuilder()
                .implement(objectType.java, objectType.java)
                .build(factoryType.java)
        )
    }
}
