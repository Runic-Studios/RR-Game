package com.runicrealms.game.data.model

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.ProfessionType
import com.runicrealms.game.common.StatType
import com.runicrealms.game.common.WorldType
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonDiscriminator

// Top-level player document: one document per player in MongoDB.
// The `id` field maps to the MongoDB `_id` field via @BsonId.

data class PlayerDocument(
    val id: java.util.UUID,
    var schemaVersion: Int = CURRENT_VERSION,
    var updatedAt: Instant = Instant.now(),
    /** True only on the very first ever insert for this player. */
    var isNewPlayer: Boolean = false,
    var player: PlayerData,
    /** All of the player's characters, keyed by slot index (0-based). */
    var characters: Map<String, CharacterData>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

// Player-scoped data (shared across all characters)

data class PlayerData(
    var traits: PlayerTraits,
    var achievements: PlayerAchievements,
    var bank: PlayerBank,
    var gathering: PlayerGathering,
    var mounts: PlayerMounts,
    var settings: PlayerSettings,
)

data class PlayerTraits(
    var lastLogin: Instant,
    /** Stored as milliseconds in BSON (see BsonCodecs). */
    var playTimeMillis: Long,
)

data class PlayerAchievements(var achievements: Map<String, AchievementStatus>)

data class AchievementStatus(var progress: Long, var unlocked: Boolean)

data class PlayerBank(var pages: Map<String, BankPage>, var maxPageIndex: Int)

data class BankPage(var items: Map<String, ItemDataStack>)

data class PlayerGathering(
    var cookingExp: Long,
    var farmingExp: Long,
    var fishingExp: Long,
    var harvestingExp: Long,
    var miningExp: Long,
    var woodcuttingExp: Long,
)

/**
 * TODO: Mount IDs are stored as plain strings. When mount code is added, register a custom
 *   Codec<Mount> that serialises as the mount's string ID and deserialises by looking up the
 *   MountRegistry.
 */
data class PlayerMounts(
    var unlockedMounts: List<String>,
    var ridingLicense: Int,
    var favourite: String?,
)

data class PlayerSettings(var tips: Boolean, var chatChannels: Map<String, ChatChannelSettings>)

data class ChatChannelSettings(var muted: Boolean, var spy: Boolean)

// Character-scoped data (one entry per character slot)

data class CharacterData(
    /** True only on the very first creation of this character slot. */
    var isNewCharacter: Boolean = false,
    var traits: CharacterTraits,
    var inventory: CharacterInventory,
    var profession: CharacterProfession,
    var quests: CharacterQuests,
    var skills: CharacterSkills,
    var spells: CharacterSpells,
)

data class CharacterTraits(
    var slot: Int,
    var classType: ClassType,
    var exp: Long,
    var level: Int,
    var health: Int,
    var hunger: Int,
    var location: LocationData,
    var outlaw: Boolean,
    var lastLogin: Instant,
    /** Stored as milliseconds in BSON (see BsonCodecs). */
    var playTimeMillis: Long,
)

data class CharacterInventory(
    /** Minecraft inventory slot index -> item stack. */
    var items: Map<String, ItemDataStack>
)

data class CharacterProfession(var profession: ProfessionType, var level: Int, var exp: Long)

data class CharacterQuests(
    /** Quest ID -> quest state. */
    var quests: Map<String, QuestData>
)

data class CharacterSkills(
    var positionOneAllocated: Int,
    var positionTwoAllocated: Int,
    var positionThreeAllocated: Int,
)

data class CharacterSpells(
    var spellOneID: String,
    var spellTwoID: String,
    var spellThreeID: String,
    var spellFourID: String,
)

// Shared domain types (used by both player and character data)

data class LocationData(
    var world: WorldType,
    var x: Double,
    var y: Double,
    var z: Double,
    var pitch: Float,
    var yaw: Float,
)

@Serializable data class ItemDataStack(val data: ItemData, val count: Int)

@Serializable
data class ItemData(
    val templateID: String,
    /** Arbitrary string metadata attached to an item. */
    val customData: Map<String, String>,
    /**
     * Null for generic/non-stat items. The concrete subtype is stored in MongoDB with a `_type`
     * discriminator field (see BsonCodecs).
     */
    val typeData: ItemTypeData?,
)

/**
 * Sealed hierarchy for item-type-specific data. A `_type` discriminator field in BSON distinguishes
 * the concrete subtype at runtime (see BsonCodecs for the codec setup).
 */
@BsonDiscriminator(key = "_type") @Serializable sealed class ItemTypeData

@Serializable
@SerialName("armor")
@BsonDiscriminator("armor")
data class ArmorData(
    val stats: List<RolledStat>,
    val gemBonuses: List<GemBonus>,
    val perks: List<Perk>,
) : ItemTypeData()

@Serializable
@SerialName("weapon")
@BsonDiscriminator("weapon")
data class WeaponData(val stats: List<RolledStat>, val perks: List<Perk>, val skinID: String?) :
    ItemTypeData()

@Serializable
@SerialName("gem")
@BsonDiscriminator("gem")
data class GemData(val bonus: GemBonus) : ItemTypeData()

@Serializable
@SerialName("offhand")
@BsonDiscriminator("offhand")
data class OffhandData(val stats: List<RolledStat>, val perks: List<Perk>) : ItemTypeData()

@Serializable data class RolledStat(val type: StatType, val rollPercentage: Double)

@Serializable data class StaticStat(val type: StatType, val amount: Int)

@Serializable
data class GemBonus(
    val stats: List<StaticStat>,
    val health: Int,
    val mainStat: StatType,
    val tier: Int,
)

@Serializable data class Perk(val perkID: String, val stacks: Int)

data class QuestData(
    var started: Boolean,
    var completed: Boolean,
    var completionTimeEpochMillis: Long?,
    /** Objective ID -> state. */
    var objectives: Map<String, ObjectiveState>,
)

data class ObjectiveState(var completed: Boolean, var completionTimeEpochMillis: Long?)
