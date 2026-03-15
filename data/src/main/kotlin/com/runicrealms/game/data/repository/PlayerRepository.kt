package com.runicrealms.game.data.repository

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.ProfessionType
import com.runicrealms.game.common.WorldType
import com.runicrealms.game.data.migration.MigrationChain
import com.runicrealms.game.data.model.CharacterData
import com.runicrealms.game.data.model.CharacterInventory
import com.runicrealms.game.data.model.CharacterProfession
import com.runicrealms.game.data.model.CharacterQuests
import com.runicrealms.game.data.model.CharacterSkills
import com.runicrealms.game.data.model.CharacterSpells
import com.runicrealms.game.data.model.CharacterTraits
import com.runicrealms.game.data.model.LocationData
import com.runicrealms.game.data.model.PlayerAchievements
import com.runicrealms.game.data.model.PlayerBank
import com.runicrealms.game.data.model.PlayerData
import com.runicrealms.game.data.model.PlayerDocument
import com.runicrealms.game.data.model.PlayerGathering
import com.runicrealms.game.data.model.PlayerMounts
import com.runicrealms.game.data.model.PlayerSettings
import com.runicrealms.game.data.model.PlayerTraits
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.slf4j.LoggerFactory

/**
 * Handles all persistence for [PlayerDocument] objects.
 *
 * Each player is stored as a single MongoDB document (one document = one player aggregate
 * containing all characters). This class is the only place that touches the `players` collection.
 *
 * Two collection handles are maintained:
 * - [rawCollection] ([BsonDocument]): used during load so we can inspect `schemaVersion` and run
 *   [MigrationChain] on the raw BSON before deserialising.
 * - [typedCollection] ([PlayerDocument]): used for inserts and saves where the document is already
 *   at the current version.
 *
 * All methods are suspend functions: they must be called on an async dispatcher, never on the
 * Minecraft main thread.
 */
class PlayerRepository(
    private val rawCollection: MongoCollection<BsonDocument>,
    private val typedCollection: MongoCollection<PlayerDocument>,
    private val migrationChain: MigrationChain,
    private val codecRegistry: CodecRegistry,
) {

    private val logger = LoggerFactory.getLogger("data")

    /**
     * Loads the player document for [playerId], running any pending schema migrations if the stored
     * version is older than [MigrationChain.latestVersion].
     *
     * If no document exists yet (first login), inserts a fresh default document with
     * [PlayerDocument.isNewPlayer] = true.
     *
     * The upgraded document is immediately written back to the database so future loads don't need
     * to migrate again.
     *
     * Returns [Result.failure] on any MongoDB error: the caller must kick the player.
     */
    suspend fun loadOrCreate(playerId: UUID): Result<PlayerDocument> {
        return try {
            val raw = rawCollection.find(eq("_id", playerId)).firstOrNull()

            if (raw == null) {
                // First-ever login: create a default document
                val newDoc = defaultDocument(playerId)
                typedCollection.insertOne(newDoc)
                logger.info("Created new player document for $playerId")
                Result.success(newDoc)
            } else {
                val storedVersion = raw.getInt32("schemaVersion")?.value ?: 1
                val migratedRaw =
                    if (storedVersion < migrationChain.latestVersion) {
                        logger.info(
                            "Migrating player $playerId from schema v$storedVersion " +
                                "to v${migrationChain.latestVersion}"
                        )
                        val upgraded = migrationChain.migrate(raw, storedVersion)
                        // Write-back the upgraded document immediately so future loads are fast
                        val upgradedDoc = decodeDocument(upgraded)
                        typedCollection.replaceOne(
                            eq("_id", playerId),
                            upgradedDoc,
                            ReplaceOptions().upsert(false),
                        )
                        upgraded
                    } else {
                        raw
                    }

                Result.success(decodeDocument(migratedRaw))
            }
        } catch (exception: Exception) {
            Result.failure(
                IllegalStateException(
                    "Failed to load/create player document for $playerId",
                    exception,
                )
            )
        }
    }

    /**
     * Persists the in-memory [PlayerDocument] to MongoDB, replacing the existing document
     * atomically. Uses upsert so it works even if somehow the document was deleted.
     *
     * Stamps [PlayerDocument.updatedAt] to now and [PlayerDocument.schemaVersion] to
     * [MigrationChain.latestVersion] before writing.
     *
     * Returns [Result.failure] on any MongoDB error: the caller must kick the player (if online) or
     * log at ERROR level (if called during logout).
     */
    suspend fun save(document: PlayerDocument): Result<Unit> {
        return try {
            val toWrite =
                document.copy(
                    updatedAt = Instant.now(),
                    schemaVersion = migrationChain.latestVersion,
                    // isNewPlayer is only meaningful on first insert; clear it on every save
                    isNewPlayer = false,
                )
            typedCollection.replaceOne(
                eq("_id", document.id),
                toWrite,
                ReplaceOptions().upsert(true),
            )
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                IllegalStateException(
                    "Failed to save player document for ${document.id}",
                    exception,
                )
            )
        }
    }

    // Helpers

    /**
     * Decodes a raw [BsonDocument] into a [PlayerDocument] using the shared [codecRegistry]. This
     * is the mechanism that allows [loadOrCreate] to work with the raw collection for migration
     * inspection while still producing a typed result.
     */
    private fun decodeDocument(raw: BsonDocument): PlayerDocument {
        val codec = codecRegistry.get(PlayerDocument::class.java)
        return codec.decode(BsonDocumentReader(raw), DecoderContext.builder().build())
    }

    /**
     * Returns a fresh [PlayerDocument] with safe default values for a brand-new player. The
     * [PlayerDocument.isNewPlayer] flag is true; downstream listeners ([GamePlayerPreLoadEvent]
     * handlers) use this to perform first-time initialisation (e.g. applying default settings,
     * awarding starter items, etc.).
     */
    private fun defaultDocument(playerId: UUID): PlayerDocument =
        PlayerDocument(
            id = playerId,
            isNewPlayer = true,
            player =
                PlayerData(
                    traits = PlayerTraits(lastLogin = Instant.now(), playTimeMillis = 0L),
                    achievements = PlayerAchievements(achievements = emptyMap()),
                    bank = PlayerBank(pages = emptyMap(), maxPageIndex = 0),
                    gathering =
                        PlayerGathering(
                            cookingExp = 0L,
                            farmingExp = 0L,
                            fishingExp = 0L,
                            harvestingExp = 0L,
                            miningExp = 0L,
                            woodcuttingExp = 0L,
                        ),
                    mounts =
                        PlayerMounts(
                            unlockedMounts = emptyList(),
                            ridingLicense = 0,
                            favourite = null,
                        ),
                    settings = PlayerSettings(tips = true, chatChannels = emptyMap()),
                ),
            characters = emptyMap(),
        )

    /**
     * Returns a fresh [CharacterData] for a newly created character slot. The
     * [CharacterData.isNewCharacter] flag is true; downstream listeners
     * ([GameCharacterPreLoadEvent] handlers) use this to apply class selection and other first-time
     * character defaults.
     */
    fun defaultCharacterData(slot: Int): CharacterData =
        CharacterData(
            isNewCharacter = true,
            traits =
                CharacterTraits(
                    slot = slot,
                    classType = ClassType.ANY,
                    exp = 0L,
                    level = 0,
                    health = 200,
                    hunger = 20,
                    location =
                        LocationData(
                            world = WorldType.ALTERRA,
                            x = 0.0,
                            y = 64.0,
                            z = 0.0,
                            pitch = 0f,
                            yaw = 0f,
                        ), // TODO fix
                    outlaw = false,
                    lastLogin = Instant.now(),
                    playTimeMillis = 0L,
                ),
            inventory = CharacterInventory(items = emptyMap()),
            profession = CharacterProfession(profession = ProfessionType.NONE, level = 0, exp = 0L),
            quests = CharacterQuests(quests = emptyMap()),
            skills =
                CharacterSkills(
                    positionOneAllocated = 0,
                    positionTwoAllocated = 0,
                    positionThreeAllocated = 0,
                ),
            spells =
                CharacterSpells(
                    spellOneID = "",
                    spellTwoID = "",
                    spellThreeID = "",
                    spellFourID = "",
                ),
        )
}
