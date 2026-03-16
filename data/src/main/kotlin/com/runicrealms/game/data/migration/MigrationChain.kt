package com.runicrealms.game.data.migration

import org.bson.BsonDocument
import org.bson.BsonInt32

/**
 * Holds the ordered list of [Migration] steps and applies them in sequence.
 *
 * Usage:
 * - Start with an empty [migrations] list (version 1, no migrations yet).
 * - When you need to change the schema, create a new [Migration] object and add it here.
 * - [latestVersion] is automatically derived from the highest [Migration.toVersion].
 *
 * Invariants enforced at construction:
 * - Migration steps must be contiguous (no gaps), starting at version 1 -> 2.
 * - Each step's [Migration.fromVersion] must equal the previous step's [Migration.toVersion].
 *
 * The chain mirrors the Trove transformer system.
 */
class MigrationChain(val migrations: List<Migration> = emptyList()) {

    /**
     * The current schema version the application expects. Documents at an older version will be
     * upgraded on first read and re-saved in the new shape.
     */
    val latestVersion: Int = migrations.maxOfOrNull { it.toVersion } ?: 1

    init {
        // Validate that steps are contiguous and start from v1 -> v2
        var expectedFrom = 1
        for (migration in migrations) {
            require(migration.fromVersion == expectedFrom) {
                "Migration chain has a gap: expected fromVersion=$expectedFrom " +
                    "but got fromVersion=${migration.fromVersion}"
            }
            require(migration.toVersion == expectedFrom + 1) {
                "Migration steps must increment by 1: fromVersion=${migration.fromVersion} " +
                    "toVersion=${migration.toVersion}"
            }
            expectedFrom = migration.toVersion
        }
    }

    /**
     * Applies all migration steps from [fromVersion] up to [latestVersion] in order. The
     * `schemaVersion` field in the returned document is updated to [latestVersion].
     *
     * Called by [PlayerRepository] when a document is loaded with an outdated version. The upgraded
     * document is immediately re-saved so future loads are already at the new version.
     */
    fun migrate(doc: BsonDocument, fromVersion: Int): BsonDocument {
        var current = doc
        for (migration in migrations) {
            if (migration.fromVersion < fromVersion) continue
            current = migration.migrate(current)
        }
        // Stamp the upgraded version so the caller can persist it
        current["schemaVersion"] = BsonInt32(latestVersion)
        return current
    }
}
