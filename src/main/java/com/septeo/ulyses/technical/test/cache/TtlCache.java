package com.septeo.ulyses.technical.test.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrent cache with per-entry TTL.
 *
 * <p>Concurrency: a single {@link ConcurrentHashMap} stores one
 * {@link CompletableFuture} per key, which holds either the in-flight load or
 * the resolved {@link CacheEntry}. {@link #get(Object, Callable)} uses
 * {@link ConcurrentHashMap#compute compute} only to atomically swap the future
 * reference (a trivial operation), while the expensive loader runs
 * <em>outside</em> any bucket lock. As a result, a slow loader for one key
 * never blocks lookups, inserts or evictions for other keys that happen to
 * hash to the same bucket, and concurrent callers for the same key share the
 * same future and observe its single value.
 */
@Slf4j
public class TtlCache implements Cache {

    private static final String LOG_PREFIX = "[CACHE] ";

    private final String name;
    private final Duration ttl;
    private final ConcurrentHashMap<Object, CompletableFuture<CacheEntry>> store = new ConcurrentHashMap<>();

    public TtlCache(String name, Duration ttl) {
        this.name = name;
        this.ttl = ttl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return store;
    }

    @Override
    public ValueWrapper get(Object key) {
        final CacheEntry entry = readFresh(key);
        return entry == null ? null : new SimpleValueWrapper(entry.value());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        final CacheEntry entry = readFresh(key);
        if (entry == null) {
            return null;
        }
        final Object value = entry.value();
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [%s]: %s".formatted(type.getName(), value));
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        final CompletableFuture<CacheEntry> mine = new CompletableFuture<>();
        // compute() only swaps a reference; the loader runs OUTSIDE the bucket lock.
        final CompletableFuture<CacheEntry> future = store.compute(key, (k, existing) -> isReusable(existing) ? existing : mine);

        if (future != mine) {
            log.debug("{}{} hit/follower key={}", LOG_PREFIX, name, key);
            return (T) awaitValue(future);
        }

        try {
            log.debug("{}{} miss - invoking loader key={}", LOG_PREFIX, name, key);
            final CacheEntry entry = new CacheEntry(valueLoader.call(), Instant.now().plus(ttl));
            mine.complete(entry);
            return (T) entry.value();
        } catch (Exception e) {
            log.error("{}{} loader failed key={}", LOG_PREFIX, name, key, e);
            final ValueRetrievalException wrapped = new ValueRetrievalException(key, valueLoader, e);
            mine.completeExceptionally(wrapped);
            // Remove the failed entry so subsequent calls retry instead of replaying the failure.
            store.remove(key, mine);
            throw wrapped;
        }
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, CompletableFuture.completedFuture(new CacheEntry(value, Instant.now().plus(ttl))));
    }

    @Override
    public void evict(Object key) {
        log.debug("{}{} evict key={}", LOG_PREFIX, name, key);
        store.remove(key);
    }

    @Override
    public void clear() {
        log.debug("{}{} clear", LOG_PREFIX, name);
        store.clear();
    }

    /**
     * A cached future can be reused when it is still loading, or already resolved with a non-expired value.
     *
     * @param existing the cached future to evaluate, may be {@code null}
     * @return {@code true} if the future is still in-flight or holds a valid, non-expired entry;
     *         {@code false} if it is {@code null}, completed exceptionally, or the entry has expired
     */
    private static boolean isReusable(CompletableFuture<CacheEntry> existing) {
        if (existing == null) {
            return false;
        }
        if (!existing.isDone()) {
            return true;
        }
        if (existing.isCompletedExceptionally()) {
            return false;
        }
        final CacheEntry entry = existing.getNow(null);
        return entry != null && !entry.isExpired();
    }

    private CacheEntry readFresh(Object key) {
        final CompletableFuture<CacheEntry> future = store.get(key);
        if (future == null || !future.isDone() || future.isCompletedExceptionally()) {
            return null;
        }
        final CacheEntry entry = future.getNow(null);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            log.debug("{}{} entry expired key={}", LOG_PREFIX, name, key);
            store.remove(key, future);
            return null;
        }
        return entry;
    }

    private static Object awaitValue(CompletableFuture<CacheEntry> running) {
        try {
            return running.join().value();
        } catch (CompletionException ce) {
            // The leader thread already wrapped the original failure into ValueRetrievalException.
            if (ce.getCause() instanceof ValueRetrievalException vre) {
                throw vre;
            }
            throw ce;
        }
    }
}
