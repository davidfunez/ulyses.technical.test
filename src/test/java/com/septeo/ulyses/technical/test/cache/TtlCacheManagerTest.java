package com.septeo.ulyses.technical.test.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class TtlCacheManagerTest {

    @Test
    @DisplayName("getCache creates a TtlCache on the first request")
    void test_getCache_1() {
        // Given
        final TtlCacheManager manager = new TtlCacheManager(Duration.ofSeconds(30));

        // When
        final Cache cache = manager.getCache("brands");

        // Then
        assertThat(cache).isInstanceOf(TtlCache.class);
        assertThat(cache.getName()).isEqualTo("brands");
    }

    @Test
    @DisplayName("getCache returns the same instance for repeated lookups")
    void test_getCache_2() {
        // Given
        final TtlCacheManager manager = new TtlCacheManager(Duration.ofSeconds(30));

        // When
        final Cache first = manager.getCache("brands");
        final Cache second = manager.getCache("brands");

        // Then
        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("getCacheNames is empty until a cache is requested")
    void test_getCacheNames_1() {
        // Given
        final TtlCacheManager manager = new TtlCacheManager(Duration.ofSeconds(30));

        // When
        final var names = manager.getCacheNames();

        // Then
        assertThat(names).isEmpty();
    }

    @Test
    @DisplayName("getCacheNames returns every cache that has been requested")
    void test_getCacheNames_2() {
        // Given
        final TtlCacheManager manager = new TtlCacheManager(Duration.ofSeconds(30));
        manager.getCache("brands");
        manager.getCache("vehicles");

        // When
        final var names = manager.getCacheNames();

        // Then
        assertThat(names).containsExactlyInAnyOrder("brands", "vehicles");
    }
}
