package com.septeo.ulyses.technical.test.cache;

import jakarta.annotation.Nonnull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class TtlCacheManager implements CacheManager {

    private final Duration ttl;
    private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();

    public TtlCacheManager(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, key -> new TtlCache(key, ttl));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }
}
