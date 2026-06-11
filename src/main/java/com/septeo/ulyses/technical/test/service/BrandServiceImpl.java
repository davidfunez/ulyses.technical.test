package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.config.CacheConfig;
import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the BrandService interface.
 * This class provides the implementation for all brand-related operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BrandServiceImpl implements BrandService {

    private static final String LOG_PREFIX = "[BRAND] ";

    private final BrandRepository brandRepository;

    @Override
    @Cacheable(cacheNames = CacheConfig.BRANDS_CACHE, key = "'" + CacheConfig.BRANDS_ALL_KEY + "'", sync = true)
    public List<Brand> getAllBrands() {
        log.debug("{}cache miss - loading all brands from repository", LOG_PREFIX);
        return brandRepository.findAll();
    }

    @Override
    @Cacheable(
            cacheNames = CacheConfig.BRANDS_CACHE,
            key = "#id",
            condition = "#id != null",
            unless = "#result == null")
    public Optional<Brand> getBrandById(Long id) {
        log.debug("{}cache miss - loading brand id={} from repository", LOG_PREFIX, id);
        return brandRepository.findById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.BRANDS_CACHE, key = "'" + CacheConfig.BRANDS_ALL_KEY + "'"),
            @CacheEvict(cacheNames = CacheConfig.BRANDS_CACHE, key = "#brand.id", condition = "#brand.id != null")
    })
    public Brand saveBrand(Brand brand) {
        log.info("{}saveBrand id={} - evicting cache entries", LOG_PREFIX, brand.getId());
        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.BRANDS_CACHE, key = "'" + CacheConfig.BRANDS_ALL_KEY + "'"),
            @CacheEvict(cacheNames = CacheConfig.BRANDS_CACHE, key = "#id")
    })
    public void deleteBrand(Long id) {
        log.info("{}deleteBrand id={} - evicting cache entries", LOG_PREFIX, id);
        brandRepository.deleteById(id);
    }
}
