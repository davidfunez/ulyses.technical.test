package com.septeo.ulyses.technical.test.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.septeo.ulyses.technical.test.config.CacheConfig;
import com.septeo.ulyses.technical.test.controller.BrandController;
import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.it.slice.controller.BrandControllerSlice;
import com.septeo.ulyses.technical.test.it.slice.infra.CacheConfigSlice;
import com.septeo.ulyses.technical.test.it.slice.infra.JpaSlice;
import com.septeo.ulyses.technical.test.it.slice.infra.SecuritySlice;
import com.septeo.ulyses.technical.test.it.slice.repository.BrandRepositorySlice;
import com.septeo.ulyses.technical.test.it.slice.service.BrandServiceSlice;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DB restore: @Transactional rolls back every test method, so writes against the H2 instance never
// persist beyond the test boundary and data.sql state is reset for the next one.
@ActiveProfiles("test")
@WebMvcTest(controllers = BrandController.class)
@ContextConfiguration(classes = {
        BrandControllerSlice.class,
        BrandServiceSlice.class,
        BrandRepositorySlice.class,
        CacheConfigSlice.class,
        SecuritySlice.class,
        JpaSlice.class
})
@Transactional
@DisplayName("BrandController + cache IT - controller → service (cache) → repository → H2 with security filters enabled")
class BrandCacheIT {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc api;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CacheManager cacheManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Cache brandsCache;

    @BeforeEach
    void cleanCache() {
        brandsCache = cacheManager.getCache(CacheConfig.BRANDS_CACHE);
        brandsCache.clear();
    }

    @Test
    @DisplayName("test_getAllBrands_1: returns the 3 brands seeded by data.sql and populates cache")
    void test_getAllBrands_1() throws Exception {
        // When
        api.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3)));

        // Then
        assertThat(brandsCache.get(CacheConfig.BRANDS_ALL_KEY)).isNotNull();
    }

    @Test
    @DisplayName("test_getAllBrands_2: second call is served from cache (repository not used)")
    void test_getAllBrands_2() throws Exception {
        // Given
        api.perform(get("/api/brands")).andExpect(status().isOk());
        @SuppressWarnings("unchecked")
        List<Brand> cached = (List<Brand>) brandsCache.get(CacheConfig.BRANDS_ALL_KEY).get();
        Brand sentinel = new Brand(999L, "SENTINEL", "from-cache", List.of());
        brandsCache.put(CacheConfig.BRANDS_ALL_KEY, List.of(sentinel));

        // When & Then
        api.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("SENTINEL")));

        // Restore
        brandsCache.put(CacheConfig.BRANDS_ALL_KEY, cached);
    }

    @Test
    @DisplayName("test_getBrandById_1: caches the entry under its id key")
    void test_getBrandById_1() throws Exception {
        // When
        api.perform(get("/api/brands/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        // Then
        assertThat(brandsCache.get(1L)).isNotNull();
    }

    @Test
    @DisplayName("test_getBrandById_2: unknown id returns 404 (Optional.empty bypasses cache write)")
    void test_getBrandById_2() throws Exception {
        // When & Then
        api.perform(get("/api/brands/{id}", 9_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("test_createBrand_1: persists to DB and evicts the 'all' cache entry")
    void test_createBrand_1() throws Exception {
        // Given
        api.perform(get("/api/brands")).andExpect(status().isOk());
        assertThat(brandsCache.get(CacheConfig.BRANDS_ALL_KEY)).isNotNull();
        Brand toCreate = new Brand(null, "Peugeot", "French manufacturer", List.of());

        // When
        api.perform(post("/api/brands")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(toCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Peugeot")));

        // Then
        entityManager.flush();
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM brands WHERE name = ?", Integer.class, "Peugeot");
        assertThat(rows).isEqualTo(1);
        assertThat(brandsCache.get(CacheConfig.BRANDS_ALL_KEY)).isNull();
        // Restore: @Transactional rolls back the inserted row.
    }

    @Test
    @DisplayName("test_updateBrand_1: updates DB row and evicts both 'all' and id keys")
    void test_updateBrand_1() throws Exception {
        // Given
        String created = api.perform(post("/api/brands")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(new Brand(null, "Skoda", "new", List.of()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long target = JSON.readTree(created).get("id").asLong();
        api.perform(get("/api/brands")).andExpect(status().isOk());
        api.perform(get("/api/brands/{id}", target)).andExpect(status().isOk());
        assertThat(brandsCache.get(CacheConfig.BRANDS_ALL_KEY)).isNotNull();
        assertThat(brandsCache.get(target)).isNotNull();
        Brand update = new Brand(null, "Skoda-Updated", "edited", List.of());

        // When
        api.perform(put("/api/brands/{id}", target)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Skoda-Updated")));

        // Then
        entityManager.flush();
        String name = jdbc.queryForObject(
                "SELECT name FROM brands WHERE id = ?", String.class, target);
        assertThat(name).isEqualTo("Skoda-Updated");
        assertThat(brandsCache.get(CacheConfig.BRANDS_ALL_KEY)).isNull();
        assertThat(brandsCache.get(target)).isNull();
        // Restore: @Transactional rolls back the insert and the update.
    }

    @Test
    @DisplayName("test_updateBrand_2: unknown id returns 404 and does not touch DB")
    void test_updateBrand_2() throws Exception {
        // Given
        Brand update = new Brand(null, "x", "x", List.of());

        // When & Then
        api.perform(put("/api/brands/{id}", 9_999L)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("test_deleteBrand_1: removes row, evicts caches and subsequent GET returns 404")
    void test_deleteBrand_1() throws Exception {
        // Given
        String created = api.perform(post("/api/brands")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(new Brand(null, "Cupra", "new", List.of()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = JSON.readTree(created);
        Long target = node.get("id").asLong();
        api.perform(get("/api/brands")).andExpect(status().isOk());
        api.perform(get("/api/brands/{id}", target)).andExpect(status().isOk());

        // When
        api.perform(delete("/api/brands/{id}", target)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());

        // Then
        entityManager.flush();
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM brands WHERE id = ?", Integer.class, target);
        assertThat(rows).isZero();
        assertThat(brandsCache.get(CacheConfig.BRANDS_ALL_KEY)).isNull();
        assertThat(brandsCache.get(target)).isNull();
        // Restore: @Transactional rolls back the insert and the delete.
    }

    @Test
    @DisplayName("test_deleteBrand_2: unknown id returns 404")
    void test_deleteBrand_2() throws Exception {
        // When & Then
        api.perform(delete("/api/brands/{id}", 9_999L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("test_security_1: write without credentials returns 401")
    void test_security_1() throws Exception {
        // When & Then
        api.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(new Brand(null, "Anon", "x", List.of()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("test_security_2: write with USER role returns 403")
    void test_security_2() throws Exception {
        // When & Then
        api.perform(delete("/api/brands/{id}", 1L)
                        .with(httpBasic("user", "user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("test_security_3: bad credentials return 401")
    void test_security_3() throws Exception {
        // When & Then
        api.perform(put("/api/brands/{id}", 1L)
                        .with(httpBasic("admin", "wrong"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(new Brand(null, "x", "x", List.of()))))
                .andExpect(status().isUnauthorized());
    }
}
