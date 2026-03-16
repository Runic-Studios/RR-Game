package com.runicrealms.game.data.config

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.runicrealms.game.data.lock.PlayerLockRepository
import com.runicrealms.game.data.migration.MigrationChain
import com.runicrealms.game.data.model.BsonCodecs
import com.runicrealms.game.data.model.ItemTypeData
import com.runicrealms.game.data.model.PlayerDocument
import com.runicrealms.game.data.repository.PlayerRepository
import org.bson.BsonDocument
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistry

/**
 * Guice module that wires up all MongoDB-related bindings.
 *
 * @param connectionString Full MongoDB connection string, read from `mongodb.secret` in the server
 *   root directory (e.g. `mongodb+srv://user:pass@cluster0.example.mongodb.net/...`).
 * @param databaseName Name of the database to connect to, read from `config.yml` (e.g. `"realm"`
 *   for live, `"realm-dev"` for development).
 * @param serverId Unique identifier for this game server process, used as the distributed lock
 *   owner ID.
 *
 * Provides:
 * - [MongoClient]: the connection pool (one per JVM process)
 * - [MongoDatabase]: the specific database handle
 * - [CodecRegistry]: custom codecs for all model types (enums, UUID, etc.)
 * - [PlayerRepository]: typed+raw collection handles + migration chain
 * - [PlayerLockRepository]: locks collection handle
 * - [MigrationChain]: empty for now (version 1, no migrations TODO)
 * - [String] annotated with @Named("serverId"): unique ID for this server process
 *
 * TTL index on the locks collection is created eagerly at startup.
 */
class MongoModule(
    private val connectionString: String,
    private val databaseName: String,
    private val serverId: String,
) : AbstractModule() {

    override fun configure() {
        bind(String::class.java)
            .annotatedWith(com.google.inject.name.Names.named("serverId"))
            .toInstance(serverId)
    }

    @Provides
    @Singleton
    fun provideMongoClient(codecRegistry: CodecRegistry): MongoClient {
        val settings =
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .codecRegistry(codecRegistry)
                .build()
        return MongoClient.create(settings)
    }

    @Provides
    @Singleton
    fun provideCodecRegistry(): CodecRegistry {
        val registry = BsonCodecs.buildRegistry()
        // Fail fast at startup if polymorphic item codecs are misconfigured.
        registry.get(PlayerDocument::class.java)
        registry.get(ItemTypeData::class.java)
        return registry
    }

    @Provides
    @Singleton
    fun provideMongoDatabase(client: MongoClient): MongoDatabase = client.getDatabase(databaseName)

    @Provides
    @Singleton
    fun provideMigrationChain(): MigrationChain =
        MigrationChain(migrations = emptyList()) // Add migrations here as needed

    @Provides
    @Singleton
    fun providePlayerRepository(
        database: MongoDatabase,
        migrationChain: MigrationChain,
        codecRegistry: CodecRegistry,
    ): PlayerRepository {
        val rawCollection = database.getCollection<BsonDocument>("players")
        return PlayerRepository(rawCollection, migrationChain, codecRegistry)
    }

    @Provides
    @Singleton
    fun providePlayerLockRepository(database: MongoDatabase): PlayerLockRepository {
        val collection = database.getCollection<org.bson.Document>("locks")
        return PlayerLockRepository(collection)
    }
}
