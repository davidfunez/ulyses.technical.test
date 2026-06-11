package com.septeo.ulyses.technical.test.config;

import com.septeo.ulyses.technical.test.cache.TtlCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String BRANDS_CACHE = "brands";
    public static final String BRANDS_ALL_KEY = "all";

    @Bean
    public CacheManager cacheManager(@Value("${app.cache.brands.ttl-seconds:60}") long ttlSeconds) {
        return new TtlCacheManager(Duration.ofSeconds(ttlSeconds));
    }
}
