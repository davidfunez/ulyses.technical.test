package com.septeo.ulyses.technical.test.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheEntryTest {

    @Test
    @DisplayName("isExpired returns true when expiresAt is in the past")
    void test_isExpired_1() {
        // Given
        final CacheEntry entry = new CacheEntry("value", Instant.now().minusSeconds(1));

        // When
        final boolean expired = entry.isExpired();

        // Then
        assertThat(expired).isTrue();
    }

    @Test
    @DisplayName("isExpired returns false when expiresAt is in the future")
    void test_isExpired_2() {
        // Given
        final CacheEntry entry = new CacheEntry("value", Instant.now().plusSeconds(60));

        // When
        final boolean expired = entry.isExpired();

        // Then
        assertThat(expired).isFalse();
    }
}
