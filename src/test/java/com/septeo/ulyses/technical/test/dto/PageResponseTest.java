package com.septeo.ulyses.technical.test.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    @DisplayName("of builds a PageResponse with the provided data and pagination info")
    void test_of_1() {
        // Given
        final List<String> data = List.of("a", "b", "c");
        final int page = 2;
        final int pageSize = 3;
        final boolean hasMore = true;

        // When
        final PageResponse<String> response = PageResponse.of(data, page, pageSize, hasMore);

        // Then
        assertThat(response.data()).isSameAs(data);
        assertThat(response.pagination()).isEqualTo(new PaginationInfo(true, page, pageSize));
    }

    @Test
    @DisplayName("of supports empty data and hasMore=false")
    void test_of_2() {
        // Given
        final List<String> data = List.of();

        // When
        final PageResponse<String> response = PageResponse.of(data, 1, 10, false);

        // Then
        assertThat(response.data()).isEmpty();
        assertThat(response.pagination()).isEqualTo(new PaginationInfo(false, 1, 10));
    }
}
