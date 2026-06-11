package com.septeo.ulyses.technical.test.cache;

import java.time.Instant;

record CacheEntry(Object value, Instant expiresAt) {

    boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
