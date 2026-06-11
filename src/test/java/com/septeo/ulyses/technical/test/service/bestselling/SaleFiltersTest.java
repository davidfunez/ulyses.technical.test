package com.septeo.ulyses.technical.test.service.bestselling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.function.Predicate;

import com.septeo.ulyses.technical.test.entity.Sales;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaleFiltersTest {

    @Test
    @DisplayName("byStartDate returns always-true predicate when startDate is null")
    void test_byStartDate_1() {
        // Given
        final Sales sale = saleOn(LocalDate.of(1900, 1, 1));

        // When
        final Predicate<Sales> predicate = SaleFilters.byStartDate(null);

        // Then
        assertThat(predicate.test(sale)).isTrue();
    }

    @Test
    @DisplayName("byStartDate accepts sales on or after the start date")
    void test_byStartDate_2() {
        // Given
        final LocalDate startDate = LocalDate.of(2024, 6, 1);
        final Predicate<Sales> predicate = SaleFilters.byStartDate(startDate);

        // When & Then
        assertThat(predicate.test(saleOn(startDate))).isTrue();
        assertThat(predicate.test(saleOn(startDate.plusDays(1)))).isTrue();
        assertThat(predicate.test(saleOn(startDate.minusDays(1)))).isFalse();
    }

    @Test
    @DisplayName("byEndDate returns always-true predicate when endDate is null")
    void test_byEndDate_1() {
        // Given
        final Sales sale = saleOn(LocalDate.of(9999, 12, 31));

        // When
        final Predicate<Sales> predicate = SaleFilters.byEndDate(null);

        // Then
        assertThat(predicate.test(sale)).isTrue();
    }

    @Test
    @DisplayName("byEndDate accepts sales on or before the end date")
    void test_byEndDate_2() {
        // Given
        final LocalDate endDate = LocalDate.of(2024, 6, 1);
        final Predicate<Sales> predicate = SaleFilters.byEndDate(endDate);

        // When & Then
        assertThat(predicate.test(saleOn(endDate))).isTrue();
        assertThat(predicate.test(saleOn(endDate.minusDays(1)))).isTrue();
        assertThat(predicate.test(saleOn(endDate.plusDays(1)))).isFalse();
    }

    private static Sales saleOn(final LocalDate date) {
        final Sales sale = new Sales();
        sale.setSaleDate(date);
        return sale;
    }
}
