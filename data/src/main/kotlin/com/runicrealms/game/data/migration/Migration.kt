package com.runicrealms.game.data.migration

import org.bson.BsonDocument

/**
 * A single schema migration step that transforms a raw BSON document from one schema version to the
 * next. Migrations operate on raw [BsonDocument] so they can reshape fields freely without needing
 * to deserialise into a Kotlin class first.
 *
 * Implementation guide:
 * - [fromVersion] and [toVersion] must be consecutive integers.
 * - The function must be pure: it receives a copy of the document and should return a new or
 *   mutated BsonDocument in the new shape.
 * - Do NOT update the `schemaVersion` field inside [migrate]; [MigrationChain] does that.
 *
 * Example: renaming `profession.level` to `profession.progress`:
 *
 *     object ProfessionLevelToProgressMigration : Migration {
 *         override val fromVersion = 1
 *         override val toVersion = 2
 *         override fun migrate(doc: BsonDocument): BsonDocument {
 *             val profession = doc.getDocument("profession") ?: return doc
 *             val level = profession.getInt32("level")?.value ?: 1
 *             val xp = profession.getInt64("xp")?.value   ?: 0L
 *             profession.remove("level")
 *             profession.remove("xp")
 *             profession["progress"] = BsonDocument()
 *                 .append("level", BsonInt32(level))
 *                 .append("xp", BsonInt64(xp))
 *                 .append("prestige", BsonInt32(0))
 *             doc["profession"] = profession
 *             return doc
 *         }
 *     }
 */
interface Migration {
    val fromVersion: Int
    val toVersion: Int

    fun migrate(doc: BsonDocument): BsonDocument
}
