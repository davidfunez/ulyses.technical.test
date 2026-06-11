package com.septeo.ulyses.technical.test.it;

import com.septeo.ulyses.technical.test.controller.SalesController;
import com.septeo.ulyses.technical.test.it.slice.controller.SalesControllerSlice;
import com.septeo.ulyses.technical.test.it.slice.infra.JpaSlice;
import com.septeo.ulyses.technical.test.it.slice.infra.SecuritySlice;
import com.septeo.ulyses.technical.test.it.slice.repository.SalesRepositorySlice;
import com.septeo.ulyses.technical.test.it.slice.service.SalesServiceSlice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DB restore: @Transactional rolls back every test method, leaving data.sql state untouched between tests.
@ActiveProfiles("test")
@WebMvcTest(controllers = SalesController.class)
@ContextConfiguration(classes = {
        SalesControllerSlice.class,
        SalesServiceSlice.class,
        SalesRepositorySlice.class,
        SecuritySlice.class,
        JpaSlice.class
})
@Transactional
@DisplayName("SalesController IT - real DB through controller → service → repository slice with security filters enabled")
class SalesControllerIT {

    @Autowired
    private MockMvc api;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("test_getAllSales_1: first page returns 10 sales and hasMore=true")
    void test_getAllSales_1() throws Exception {
        // When & Then
        api.perform(get("/api/sales").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(10)))
                .andExpect(jsonPath("$.pagination.page", is(1)))
                .andExpect(jsonPath("$.pagination.pageSize", is(10)))
                .andExpect(jsonPath("$.pagination.hasMore", is(true)));
    }

    @Test
    @DisplayName("test_getAllSales_2: invalid page <1 returns 400")
    void test_getAllSales_2() throws Exception {
        // When & Then
        api.perform(get("/api/sales").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test_getAllSales_3: default page parameter is 1")
    void test_getAllSales_3() throws Exception {
        // When & Then
        api.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page", is(1)));
    }

    @Test
    @DisplayName("test_getSalesById_1: existing id returns 200 and the entity")
    void test_getSalesById_1() throws Exception {
        // Given
        Long anyId = jdbc.queryForObject("SELECT MIN(id) FROM sales", Long.class);

        // When & Then
        api.perform(get("/api/sales/{id}", anyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(anyId.intValue())));
    }

    @Test
    @DisplayName("test_getSalesById_2: unknown id returns 404")
    void test_getSalesById_2() throws Exception {
        // When & Then
        api.perform(get("/api/sales/{id}", 9_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("test_getSalesById_3: non-positive id returns 400")
    void test_getSalesById_3() throws Exception {
        // When & Then
        api.perform(get("/api/sales/{id}", 0L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test_getSalesByBrand_1: existing brand returns matching sales count")
    void test_getSalesByBrand_1() throws Exception {
        // Given
        Integer expected = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales WHERE brand_id = ?", Integer.class, 1L);

        // When & Then
        api.perform(get("/api/sales/brands/{brandId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(expected)));
    }

    @Test
    @DisplayName("test_getSalesByBrand_2: non-positive brandId returns 400")
    void test_getSalesByBrand_2() throws Exception {
        // When & Then
        api.perform(get("/api/sales/brands/{brandId}", -3L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test_getSalesByVehicle_1: vehicle with sales returns full list")
    void test_getSalesByVehicle_1() throws Exception {
        // Given
        Integer expected = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales WHERE vehicle_id = ?", Integer.class, 1L);

        // When & Then
        api.perform(get("/api/sales/vehicles/{vehicleId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(expected)));
    }

    @Test
    @DisplayName("test_getSalesByVehicle_2: non-positive vehicleId returns 400")
    void test_getSalesByVehicle_2() throws Exception {
        // When & Then
        api.perform(get("/api/sales/vehicles/{vehicleId}", 0L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test_getBestSellingVehicles_1: without date range returns up to 5 entries")
    void test_getBestSellingVehicles_1() throws Exception {
        // When & Then
        api.perform(get("/api/sales/vehicles/bestSelling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(5)))
                .andExpect(jsonPath("$[0].totalSales", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("test_getBestSellingVehicles_2: inverted date range returns 400")
    void test_getBestSellingVehicles_2() throws Exception {
        // When & Then
        api.perform(get("/api/sales/vehicles/bestSelling")
                        .param("startDate", "2025-02-01")
                        .param("endDate", "2025-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test_getBestSellingVehicles_3: filtering by a narrow range yields a subset")
    void test_getBestSellingVehicles_3() throws Exception {
        // When & Then
        api.perform(get("/api/sales/vehicles/bestSelling")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }
}
