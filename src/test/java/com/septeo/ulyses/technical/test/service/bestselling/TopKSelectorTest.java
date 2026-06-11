package com.septeo.ulyses.technical.test.service.bestselling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TopKSelectorTest {

    private static final ToLongFunction<Long> VALUE_EXTRACTOR = value -> value;

    @Test
    @DisplayName("of returns top K elements in descending order")
    void test_of_1() {
        // Given
        final List<Long> input = List.of(3L, 1L, 5L, 2L, 4L, 7L, 6L);

        // When
        final List<Long> result = TopKSelector.of(input, 3, VALUE_EXTRACTOR);

        // Then
        assertThat(result).containsExactly(7L, 6L, 5L);
    }

    @Test
    @DisplayName("of returns all elements when K is greater than input size")
    void test_of_2() {
        // Given
        final List<Long> input = List.of(2L, 1L, 3L);

        // When
        final List<Long> result = TopKSelector.of(input, 10, VALUE_EXTRACTOR);

        // Then
        assertThat(result).containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("of preserves first-seen order when values tie")
    void test_of_3() {
        // Given
        final Long first = 5L;
        final Long second = 5L;
        final Long third = 5L;
        final List<Long> input = List.of(first, second, third);

        // When
        final List<Long> result = TopKSelector.of(input, 2, VALUE_EXTRACTOR);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(first);
        assertThat(result.get(1)).isSameAs(second);
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provide_of_edge_cases")
    @DisplayName("of returns empty list for edge cases (null, empty or non-positive K)")
    void test_of_4(final List<Long> input, final int k) {
        // Given & When
        final List<Long> result = TopKSelector.of(input, k, VALUE_EXTRACTOR);

        // Then
        assertThat(result).isEmpty();
    }

    private static Stream<Arguments> provide_of_edge_cases() {
        return Stream.of(
                Arguments.of(Named.of("null input", null), 5),
                Arguments.of(Named.of("empty input", List.of()), 5),
                Arguments.of(Named.of("K = 0", List.of(1L, 2L)), 0),
                Arguments.of(Named.of("K negative", List.of(1L, 2L)), -3));
    }

    @Test
    @DisplayName("of handles single-element input")
    void test_of_5() {
        // Given
        final List<Long> input = List.of(42L);

        // When
        final List<Long> result = TopKSelector.of(input, 5, VALUE_EXTRACTOR);

        // Then
        assertThat(result).containsExactly(42L);
    }

    @Test
    @DisplayName("of inserts in the middle when value falls between existing values")
    void test_of_6() {
        // Given - candidate goes between first and last while buffer is full
        final List<Long> input = List.of(10L, 1L, 5L, 7L);

        // When
        final List<Long> result = TopKSelector.of(input, 3, VALUE_EXTRACTOR);

        // Then
        assertThat(result).containsExactly(10L, 7L, 5L);
    }

    @Test
    @DisplayName("of rejects values that do not beat the current threshold once buffer is full")
    void test_of_7() {
        // Given
        final List<Long> input = List.of(10L, 9L, 8L, 1L, 2L, 3L);

        // When
        final List<Long> result = TopKSelector.of(input, 3, VALUE_EXTRACTOR);

        // Then
        assertThat(result).containsExactly(10L, 9L, 8L);
    }

    @Test
    @DisplayName("of works with negative values")
    void test_of_8() {
        // Given
        final List<Long> input = List.of(-5L, -10L, -1L, -7L);

        // When
        final List<Long> result = TopKSelector.of(input, 2, VALUE_EXTRACTOR);

        // Then
        assertThat(result).containsExactly(-1L, -5L);
    }
}
