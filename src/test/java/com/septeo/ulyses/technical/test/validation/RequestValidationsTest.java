package com.septeo.ulyses.technical.test.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RequestValidationsTest {

    @Test
    @DisplayName("positive succeeds with a positive value")
    void test_positive_1() {
        // Given, When & Then
        assertThatCode(() -> RequestValidations.positive(1L, "id")).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provide_positive_invalid_cases")
    @DisplayName("positive throws 400 for null or non-positive values")
    void test_positive_2(final Long value) {
        // Given, When & Then
        assertThatThrownBy(() -> RequestValidations.positive(value, "id"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    final ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("id must be a positive number");
                });
    }

    private static Stream<Arguments> provide_positive_invalid_cases() {
        return Stream.of(
                Arguments.of(Named.of("null value", (Long) null)),
                Arguments.of(Named.of("zero value", 0L)),
                Arguments.of(Named.of("negative value", -5L)));
    }

    @Test
    @DisplayName("minimum succeeds when value equals the minimum")
    void test_minimum_1() {
        // Given, When & Then
        assertThatCode(() -> RequestValidations.minimum(1, 1, "page")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("minimum succeeds when value is greater than the minimum")
    void test_minimum_2() {
        // Given, When & Then
        assertThatCode(() -> RequestValidations.minimum(10, 1, "page")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("minimum throws 400 when value is below the minimum")
    void test_minimum_3() {
        // Given, When & Then
        assertThatThrownBy(() -> RequestValidations.minimum(0, 1, "page"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    final ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("page must be >= 1");
                });
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provide_dateRange_valid_cases")
    @DisplayName("dateRange accepts null bounds and start <= end")
    void test_dateRange_1(final LocalDate startDate, final LocalDate endDate) {
        // Given, When & Then
        assertThatCode(() -> RequestValidations.dateRange(startDate, endDate)).doesNotThrowAnyException();
    }

    private static Stream<Arguments> provide_dateRange_valid_cases() {
        return Stream.of(
                Arguments.of(Named.of("both null", null), null),
                Arguments.of(Named.of("only start", LocalDate.of(2024, 1, 1)), null),
                Arguments.of(Named.of("only end", null), LocalDate.of(2024, 1, 1)),
                Arguments.of(Named.of("start before end", LocalDate.of(2024, 1, 1)), LocalDate.of(2024, 12, 31)),
                Arguments.of(Named.of("start equals end", LocalDate.of(2024, 6, 1)), LocalDate.of(2024, 6, 1)));
    }

    @Test
    @DisplayName("dateRange throws 400 when start is after end")
    void test_dateRange_2() {
        // Given
        final LocalDate startDate = LocalDate.of(2024, 12, 31);
        final LocalDate endDate = LocalDate.of(2024, 1, 1);

        // When & Then
        assertThatThrownBy(() -> RequestValidations.dateRange(startDate, endDate))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    final ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("startDate must be before or equal to endDate");
                });
    }
}
