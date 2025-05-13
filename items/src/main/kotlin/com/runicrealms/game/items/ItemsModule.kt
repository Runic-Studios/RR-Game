package com.runicrealms.game.items

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.runicrealms.game.items.character.AddedStats
import com.runicrealms.game.items.character.CharacterEquipmentCache
import com.runicrealms.game.items.character.CharacterEquipmentCacheRegistry
import com.runicrealms.game.items.character.FlatStatsModifier
import com.runicrealms.game.items.config.perk.GameItemPerkTemplateRegistry
import com.runicrealms.game.items.dynamic.DynamicItemManager
import com.runicrealms.game.items.dynamic.DynamicItemRegistry
import com.runicrealms.game.items.generator.GameItemArmor
import com.runicrealms.game.items.generator.GameItemGem
import com.runicrealms.game.items.generator.GameItemGeneric
import com.runicrealms.game.items.generator.GameItemOffhand
import com.runicrealms.game.items.generator.GameItemWeapon
import com.runicrealms.game.items.generator.ItemStackConverter
import kotlin.reflect.KClass

class ItemsModule: AbstractModule() {

    override fun configure() {
        addFactory(AddedStats::class, AddedStats.Factory::class)
        addFactory(CharacterEquipmentCache::class, CharacterEquipmentCache.Factory::class)
        addFactory(FlatStatsModifier::class, FlatStatsModifier.Factory::class)
        addFactory(GameItemArmor::class, GameItemArmor.Factory::class)
        addFactory(GameItemGem::class, GameItemGem.Factory::class)
        addFactory(GameItemGeneric::class, GameItemGeneric.Factory::class)
        addFactory(GameItemOffhand::class, GameItemOffhand.Factory::class)
        addFactory(GameItemWeapon::class, GameItemWeapon.Factory::class)

        bind(DynamicItemManager::class.java).asEagerSingleton()
        bind(DynamicItemRegistry::class.java).to(DynamicItemManager::class.java)

        bind(GameItemManager::class.java).asEagerSingleton()
        bind(GameItemPerkTemplateRegistry::class.java).to(GameItemManager::class.java)
        bind(GameItemPerkTemplateRegistry::class.java).to(GameItemManager::class.java)
        bind(ItemStackConverter::class.java).to(GameItemManager::class.java)

        bind(PlayerItemManager::class.java).asEagerSingleton()
        bind(CharacterEquipmentCacheRegistry::class.java).to(PlayerItemManager::class.java)
    }

    private fun <T : Any, U : Any> addFactory(objectType: KClass<T>, factoryType: KClass<U>) {
        install(FactoryModuleBuilder().implement(objectType.java, objectType.java).build(factoryType.java))
    }

}