package com.runicrealms.game.data.lock

import java.time.Instant
import java.util.UUID

/**
 * BSON document shape stored in the `locks` MongoDB collection.
 *
 * A TTL index on [expiresAt] (created at startup by [PlayerLockRepository]) automatically deletes
 * stale lock documents once their expiry time passes. This is cleanup-only: the actual enforcement
 * of "who holds the lock right now" happens in [PlayerLockRepository] via atomic findOneAndUpdate
 * operations.
 */
data class PlayerLock(
    /** The player UUID: used as the `_id` field in MongoDB for O(1) lookup. */
    val id: UUID,
    /** Identifies which game-server instance holds this lock (e.g. "paper-abc1234"). */
    val serverId: String,
    /**
     * Absolute timestamp after which the lock is considered expired and can be taken over by
     * another server. Also the field on which the TTL index runs.
     */
    val expiresAt: Instant,
)
