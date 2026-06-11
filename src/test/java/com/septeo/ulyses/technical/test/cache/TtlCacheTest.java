package com.septeo.ulyses.technical.test.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;

class TtlCacheTest {

    private static final String CACHE_NAME = "test-cache";
    private static final Duration LONG_TTL = Duration.ofMinutes(5);
    private static final Duration EXPIRED_TTL = Duration.ofMillis(-1);

    @Test
    @DisplayName("getName returns the cache name")
    void test_getName_1() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);

        // When
        final String name = cache.getName();

        // Then
        assertThat(name).isEqualTo(CACHE_NAME);
    }

    @Test
    @DisplayName("getNativeCache returns the underlying concurrent map")
    void test_getNativeCache_1() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);

        // When
        final Object nativeCache = cache.getNativeCache();

        // Then
        assertThat(nativeCache).isNotNull();
    }

    @Test
    @DisplayName("get(key) returns null when the entry is not present")
    void test_get_key_1() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);

        // When
        final Cache.ValueWrapper wrapper = cache.get("missing");

        // Then
        assertThat(wrapper).isNull();
    }

    @Test
    @DisplayName("get(key) returns the value wrapped after a put")
    void test_get_key_2() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        cache.put("k", "value");

        // When
        final Cache.ValueWrapper wrapper = cache.get("k");

        // Then
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.get()).isEqualTo("value");
    }

    @Test
    @DisplayName("get(key) returns null and evicts expired entries")
    void test_get_key_3() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, EXPIRED_TTL);
        cache.put("k", "value");

        // When
        final Cache.ValueWrapper wrapper = cache.get("k");

        // Then
        assertThat(wrapper).isNull();
    }

    @Test
    @DisplayName("get(key, type) returns null for missing entry")
    void test_get_key_type_1() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);

        // When
        final String value = cache.get("missing", String.class);

        // Then
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("get(key, type) returns the cached value when type matches")
    void test_get_key_type_2() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        cache.put("k", "hello");

        // When
        final String value = cache.get("k", String.class);

        // Then
        assertThat(value).isEqualTo("hello");
    }

    @Test
    @DisplayName("get(key, type) returns the value when type is null")
    void test_get_key_type_3() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        cache.put("k", "value");

        // When
        final Object value = cache.get("k", (Class<Object>) null);

        // Then
        assertThat(value).isEqualTo("value");
    }

    @Test
    @DisplayName("get(key, type) returns null when value is null even with a type")
    void test_get_key_type_4() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        cache.put("k", null);

        // When
        final String value = cache.get("k", String.class);

        // Then
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("get(key, type) throws when the cached value does not match the requested type")
    void test_get_key_type_5() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        cache.put("k", 42);

        // When & Then
        assertThatThrownBy(() -> cache.get("k", String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.class.getName());
    }

    @Test
    @DisplayName("get(key, Callable) loads and caches the value on first access")
    void test_get_key_callable_1() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        final AtomicInteger loaderCalls = new AtomicInteger();
        final Callable<String> loader = () -> {
            loaderCalls.incrementAndGet();
            return "loaded";
        };

        // When
        final String first = cache.get("k", loader);
        final String second = cache.get("k", loader);

        // Then
        assertThat(first).isEqualTo("loaded");
        assertThat(second).isEqualTo("loaded");
        assertThat(loaderCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("get(key, Callable) reloads when the entry is expired")
    void test_get_key_callable_2() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, EXPIRED_TTL);
        final AtomicInteger loaderCalls = new AtomicInteger();
        final Callable<String> loader = () -> "value-" + loaderCalls.incrementAndGet();

        // When
        final String first = cache.get("k", loader);
        final String second = cache.get("k", loader);

        // Then
        assertThat(first).isEqualTo("value-1");
        assertThat(second).isEqualTo("value-2");
    }

    @Test
    @DisplayName("get(key, Callable) wraps loader exceptions into ValueRetrievalException")
    void test_get_key_callable_3() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        final RuntimeException cause = new RuntimeException("boom");
        final Callable<String> loader = () -> {
            throw cause;
        };

        // When & Then
        assertThatThrownBy(() -> cache.get("k", loader))
                .isInstanceOf(ValueRetrievalException.class)
                .hasCause(cause);
    }

    @Test
    @DisplayName("get(key, Callable) only runs the loader once under concurrent access")
    void test_get_key_callable_4() throws Exception {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        final int threads = 16;
        final AtomicInteger loaderCalls = new AtomicInteger();
        final CyclicBarrier barrier = new CyclicBarrier(threads);
        final CountDownLatch finished = new CountDownLatch(threads);
        final Callable<String> loader = () -> {
            loaderCalls.incrementAndGet();
            Thread.sleep(20);
            return "single";
        };
        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        // When
        final List<Future<String>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    barrier.await();
                    return cache.get("k", loader);
                } finally {
                    finished.countDown();
                }
            }));
        }
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();

        // Then
        for (Future<String> f : futures) {
            assertThat(f.get()).isEqualTo("single");
        }
        assertThat(loaderCalls.get()).isEqualTo(1);
        pool.shutdownNow();
    }

    @Test
    @DisplayName("get(key, Callable) does not block operations on other keys while a slow loader runs")
    void test_get_key_callable_5() throws Exception {
        // Given - a slow loader on key "slow" must not freeze a concurrent put/get on key "fast"
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        final CountDownLatch loaderStarted = new CountDownLatch(1);
        final CountDownLatch otherKeyDone = new CountDownLatch(1);
        final Callable<String> slowLoader = () -> {
            loaderStarted.countDown();
            // Wait for the other key to complete; if the bucket were locked this would deadlock.
            if (!otherKeyDone.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("other key was blocked by the slow loader");
            }
            return "slow-value";
        };
        final ExecutorService pool = Executors.newFixedThreadPool(2);

        // When
        final Future<String> slow = pool.submit(() -> cache.get("slow", slowLoader));
        assertThat(loaderStarted.await(1, TimeUnit.SECONDS)).isTrue();
        cache.put("fast", "fast-value");
        final Cache.ValueWrapper fast = cache.get("fast");
        otherKeyDone.countDown();

        // Then
        assertThat(fast).isNotNull();
        assertThat(fast.get()).isEqualTo("fast-value");
        assertThat(slow.get(2, TimeUnit.SECONDS)).isEqualTo("slow-value");
        pool.shutdownNow();
    }

    @Test
    @DisplayName("get(key, Callable) propagates ValueRetrievalException to concurrent waiters when the loader fails")
    void test_get_key_callable_6() throws Exception {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        final CountDownLatch loaderStarted = new CountDownLatch(1);
        final CountDownLatch followerReady = new CountDownLatch(1);
        final RuntimeException boom = new RuntimeException("boom");
        final Callable<String> failing = () -> {
            loaderStarted.countDown();
            followerReady.await();
            throw boom;
        };
        final ExecutorService pool = Executors.newFixedThreadPool(2);

        // When
        final Future<String> leader = pool.submit(() -> cache.get("k", failing));
        assertThat(loaderStarted.await(1, TimeUnit.SECONDS)).isTrue();
        final Future<String> follower = pool.submit(() -> cache.get("k", failing));
        // Give the follower a moment to register on the in-flight future before releasing the loader.
        Thread.sleep(50);
        followerReady.countDown();

        // Then
        assertThatThrownBy(leader::get)
                .hasCauseInstanceOf(Cache.ValueRetrievalException.class);
        assertThatThrownBy(follower::get)
                .hasCauseInstanceOf(Cache.ValueRetrievalException.class);
        pool.shutdownNow();
    }

    @Test
    @DisplayName("evict removes the cached entry")
    void test_evict_1() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        cache.put("k", "value");

        // When
        cache.evict("k");

        // Then
        assertThat(cache.get("k")).isNull();
    }

    @Test
    @DisplayName("evict on missing key does not throw")
    void test_evict_2() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);

        // When & Then
        assertThatCode(() -> cache.evict("missing")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("clear removes all cached entries")
    void test_clear_1() {
        // Given
        final TtlCache cache = new TtlCache(CACHE_NAME, LONG_TTL);
        cache.put("a", 1);
        cache.put("b", 2);

        // When
        cache.clear();

        // Then
        assertThat(cache.get("a")).isNull();
        assertThat(cache.get("b")).isNull();
    }
}
