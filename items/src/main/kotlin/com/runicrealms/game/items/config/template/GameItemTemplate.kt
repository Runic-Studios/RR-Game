package com.runicrealms.game.items.config.template

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.runicrealms.trove.generated.api.schema.v1.ItemData
import com.runicrealms.trove.generated.api.schema.v1.StatType
import io.netty.util.internal.ThreadLocalRandom
import net.kyori.adventure.text.TextComponent

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = GameItemGenericTemplate::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GameItemArmorTemplate::class, name = "armor"),
    JsonSubTypes.Type(value = GameItemGemTemplate::class, name = "gem"),
    JsonSubTypes.Type(value = GameItemWeaponTemplate::class, name = "weapon"),
    JsonSubTypes.Type(value = GameItemOffhandTemplate::class, name = "offhand"),
    JsonSubTypes.Type(value = GameItemGenericTemplate::class, name = "generic"), // Also the default
)
sealed class GameItemTemplate(
    val id: String,
    val display: DisplayableItem,
    val tags: List<GameItemTag>,
    val lore: List<TextComponent>,
    triggers: LinkedHashMap<GameItemClickTrigger.Type, String>,
    val extraProperties: Map<String, Any>,
) {

    val triggers = triggers.toTriggers()

    protected open fun buildItemData(): ItemData.Builder {
        val builder = ItemData.newBuilder().setTemplateID(id)
        return builder
    }

    fun generateItemData() = buildItemData().build()

    data class StatRange(val min: Int, val max: Int)

    data class DamageRange(val min: Int, val max: Int)

    protected fun LinkedHashMap<StatType, StatRange>.toRolledStats(): List<ItemData.RolledStat> {
        val stats = mutableListOf<ItemData.RolledStat>()
        for ((statType, _) in this) {
            val stat =
                ItemData.RolledStat.newBuilder()
                    .setType(statType)
                    .setRollPercentage(
                        ThreadLocalRandom.current().nextDouble()
                    ) // Roll random value
                    .build()
            stats.add(stat)
        }
        return stats
    }

    protected fun LinkedHashMap<String, Int>.toPerks(): List<ItemData.Perk> {
        val perks = mutableListOf<ItemData.Perk>()
        for ((perkID, stacks) in this) {
            val perk = ItemData.Perk.newBuilder().setPerkID(perkID).setStacks(stacks).build()
            perks.add(perk)
        }
        return perks
    }

    protected fun LinkedHashMap<GameItemClickTrigger.Type, String>.toTriggers():
        List<GameItemClickTrigger> {
        val triggers = mutableListOf<GameItemClickTrigger>()
        for ((type, triggerID) in this) {
            triggers.add(GameItemClickTrigger(type, triggerID))
        }
        return triggers
    }
}
