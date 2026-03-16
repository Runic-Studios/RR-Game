package com.runicrealms.game.data.game

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A reentrant coroutine-based lock for protecting a single player's data.
 *
 * Unlike a plain [Mutex], this lock is re-entrant within the same coroutine. A coroutine that
 * already holds the lock (detected via a [CoroutineContext] element) may call [withLock] again
 * without deadlocking, enabling nested data accessor calls (e.g. calling
 * [com.runicrealms.game.data.game.GameCharacter.withSyncCharacterData] from inside a
 * [com.runicrealms.game.data.game.GameCharacter.withCharacterData] block).
 *
 * Usage is exclusively through [withLock]. The lock is acquired on entry, the [action] runs with
 * the caller's data as receiver, and the lock is released on exit: whether normally or via an
 * exception. Callers never acquire or release the lock manually.
 */
internal class PlayerDataLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(action: suspend () -> T): T {
        val existing = coroutineContext[LockOwnerKey]
        if (existing != null) {
            // This coroutine already holds the lock: re-enter without re-acquiring the mutex.
            existing.depth++
            return try {
                action()
            } finally {
                existing.depth--
            }
        }
        // First acquisition: take the mutex and install a LockOwner into the context so nested
        // calls in this coroutine can detect re-entry.
        return mutex.withLock {
            val owner = LockOwner()
            withContext(coroutineContext + owner) { action() }
        }
    }
}

private class LockOwner : AbstractCoroutineContextElement(LockOwnerKey) {
    var depth = 0
}

private object LockOwnerKey : CoroutineContext.Key<LockOwner>
