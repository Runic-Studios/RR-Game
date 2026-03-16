package com.runicrealms.game.data.lock

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoCollection
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.bson.Document
import org.slf4j.LoggerFactory

/**
 * Manages distributed player locks using a dedicated `locks` MongoDB collection.
 *
 * Lock semantics mirror the old Trove lease system:
 * - A lock is acquired on player login.
 * - It is renewed every 30 seconds (same cadence as the periodic save).
 * - It is released on player logout.
 * - If a server crashes, the TTL index ensures the lock expires after [LOCK_TTL_MILLIS]
 *   milliseconds, allowing another server to take over.
 *
 * All operations are fully async (suspend functions). Failures are returned as [Result.failure]
 *
 * Thread-safety: all methods are coroutine-safe. The actual locking guarantee comes from MongoDB's
 * atomic findOneAndUpdate with appropriate filters, not from in-process locking.
 */
class PlayerLockRepository(private val collection: MongoCollection<Document>) {

    private val logger = LoggerFactory.getLogger("data")

    companion object {
        /** How long a lock remains valid without renewal (1 minute). */
        const val LOCK_TTL_MILLIS = 60_000L

        private const val FIELD_ID = "_id"
        private const val FIELD_SERVER_ID = "serverId"
        private const val FIELD_EXPIRES_AT = "expiresAt"
    }

    /**
     * Ensures the `locks` collection has a TTL index on the `expiresAt` field.
     *
     * MongoDB will automatically delete documents whose [expiresAt] has passed. This is cleanup
     * only: expiry enforcement happens in [acquireOrRenew] via the filter.
     *
     * Must be called once at startup (via [MongoModule]).
     */
    suspend fun ensureTtlIndex() {
        val indexKeys = Document(FIELD_EXPIRES_AT, 1)
        val indexOptions = IndexOptions().expireAfter(0, TimeUnit.SECONDS)
        collection.createIndex(indexKeys, indexOptions)
        logger.info("Ensured TTL index on locks.expiresAt")
    }

    /**
     * Acquires the lock for [playerId] on behalf of [serverId], or renews it if this server already
     * holds it, or steals it if the existing lock has expired.
     *
     * The filter allows the update only if: a) No lock document exists for this player (upsert), OR
     * b) The existing lock belongs to [serverId] (renewal), OR c) The existing lock has expired
     * (takeover after crash).
     *
     * Returns [Result.success] on success, [Result.failure] if the lock is held by a different live
     * server.
     *
     * Must be called on an async dispatcher: never call from the MC main thread.
     */
    suspend fun acquireOrRenew(playerId: UUID, serverId: String): Result<Unit> {
        val newExpiry = Instant.now().plusMillis(LOCK_TTL_MILLIS)

        // Allow the update if: no doc exists (upsert covers this), OR we own it, OR it's expired
        val filter =
            and(
                eq(FIELD_ID, playerId),
                or(eq(FIELD_SERVER_ID, serverId), lt(FIELD_EXPIRES_AT, Instant.now())),
            )

        val update = combine(set(FIELD_SERVER_ID, serverId), set(FIELD_EXPIRES_AT, newExpiry))

        val options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)

        return try {
            // findOneAndUpdate with upsert: succeeds if no doc exists OR filter matches
            collection.findOneAndUpdate(filter, update, options)
            Result.success(Unit)
        } catch (exception: Exception) {
            // If the filter did not match (another server holds a live lock), findOneAndUpdate
            // with upsert would fail with a duplicate-key error on the _id field.
            Result.failure(
                IllegalStateException(
                    "Failed to acquire/renew lock for player $playerId on server $serverId: " +
                        "another server may hold a live lock. Cause: ${exception.message}",
                    exception,
                )
            )
        }
    }

    /**
     * Releases the lock for [playerId] only if [serverId] currently holds it.
     *
     * A failed release is non-fatal at logout (the TTL index will clean it up), but must be logged
     * at ERROR level.
     *
     * Must be called on an async dispatcher: never call from the MC main thread.
     */
    suspend fun release(playerId: UUID, serverId: String): Result<Unit> {
        return try {
            // Only delete if this server is the current owner
            val filter = and(eq(FIELD_ID, playerId), eq(FIELD_SERVER_ID, serverId))
            collection.deleteOne(filter)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                IllegalStateException(
                    "Failed to release lock for player $playerId on server $serverId. " +
                        "Stale lock will expire via TTL. Cause: ${exception.message}",
                    exception,
                )
            )
        }
    }
}
