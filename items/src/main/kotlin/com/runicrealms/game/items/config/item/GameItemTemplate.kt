package com.runicrealms.game.items.config.item

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.runicrealms.game.common.StatType
import com.runicrealms.game.data.model.ItemData
import com.runicrealms.game.data.model.Perk
import com.runicrealms.game.data.model.RolledStat
import java.util.concurrent.ThreadLocalRandom
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

    protected open fun buildItemData(): ItemData {
        return ItemData(templateID = id, customData = emptyMap(), typeData = null)
    }

    fun generateItemData() = buildItemData()

    data class StatRange(val min: Int, val max: Int)

    data class DamageRange(val min: Int, val max: Int)

    protected fun LinkedHashMap<StatType, StatRange>.toRolledStats(): List<RolledStat> {
        return map { (statType, _) ->
            RolledStat(type = statType, rollPercentage = ThreadLocalRandom.current().nextDouble())
        }
    }

    protected fun LinkedHashMap<String, Int>.toPerks(): List<Perk> {
        return map { (perkID, stacks) -> Perk(perkID = perkID, stacks = stacks) }
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
