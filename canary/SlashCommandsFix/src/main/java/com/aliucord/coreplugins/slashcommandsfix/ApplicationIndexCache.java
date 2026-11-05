/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.coreplugins.slashcommandsfix;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache of application indexes, keyed by their API endpoint.
 * <p>
 * It is accessed from the store dispatcher (fetching), the UI (permission checks)
 * and the gateway thread (invalidation), so all state lives in concurrent maps.
 * <p>
 * Invalidation does not drop entries; it bumps a per-key generation so that
 * stale entries can still be read without network access (for permission checks)
 * while the next browse/query refetches them. An entry fetched before an
 * invalidation stays stale even if the fetch finishes afterwards.
 */
final class ApplicationIndexCache {
    /** Entries older than this are refetched; covers changes that have no gateway event (e.g. user-installed apps). */
    static final long MAX_AGE_MS = 10 * 60 * 1000;

    private static final class Entry {
        final ApplicationIndex index;
        final long generation;
        final long fetchedAt;
        final long maxAgeMs;

        Entry(ApplicationIndex index, long generation, long fetchedAt, long maxAgeMs) {
            this.index = index;
            this.generation = generation;
            this.fetchedAt = fetchedAt;
            this.maxAgeMs = maxAgeMs;
        }
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, Long> generations = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    /** Per-endpoint time (ms) before which no request may be made, after a rate limit or failure. */
    private final Map<String, Long> blockedUntil = new ConcurrentHashMap<>();
    private volatile long globalBlockedUntil = 0;

    long generation(String key) {
        return generations.getOrDefault(key, 0L);
    }

    /** Lock object used to make sure only one request per index is in flight. */
    Object lockFor(String key) {
        return locks.computeIfAbsent(key, k -> new Object());
    }

    /** Returns the cached index only if it hasn't been invalidated and isn't too old. */
    Optional<ApplicationIndex> getFresh(String key) {
        var entry = entries.get(key);
        if (entry == null
            || entry.generation != generation(key)
            || System.currentTimeMillis() - entry.fetchedAt > entry.maxAgeMs) {
            return Optional.empty();
        }
        return Optional.of(entry.index);
    }

    /** Returns the cached index even if it is stale. Never triggers a request. */
    Optional<ApplicationIndex> getAny(String key) {
        var entry = entries.get(key);
        return entry == null ? Optional.empty() : Optional.of(entry.index);
    }

    /** Stores an index fetched while the key was at {@code generationAtRequest}, considered fresh for {@code maxAgeMs}. */
    void put(String key, ApplicationIndex index, long generationAtRequest, long maxAgeMs) {
        entries.put(key, new Entry(index, generationAtRequest, System.currentTimeMillis(), maxAgeMs));
    }

    /** Milliseconds until requests to {@code key} are allowed again (0 if allowed now). */
    long blockedForMs(String key) {
        var until = Math.max(blockedUntil.getOrDefault(key, 0L), globalBlockedUntil);
        return Math.max(0, until - System.currentTimeMillis());
    }

    /** Disallows requests to {@code key} (or to every index, if {@code global}) for {@code ms}. */
    void block(String key, long ms, boolean global) {
        var until = System.currentTimeMillis() + ms;
        if (global) {
            globalBlockedUntil = Math.max(globalBlockedUntil, until);
        } else {
            blockedUntil.merge(key, until, Math::max);
        }
    }

    void invalidate(String key) {
        generations.merge(key, 1L, Long::sum);
    }
}
