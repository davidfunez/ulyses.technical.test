package com.septeo.ulyses.technical.test.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.septeo.ulyses.technical.test.dto.BestSellingVehicleResponse;
import com.septeo.ulyses.technical.test.dto.PageResponse;
import com.septeo.ulyses.technical.test.dto.PaginationInfo;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.service.SalesService;
import com.septeo.ulyses.technical.test.validation.RequestValidations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SalesControllerTest {

    private static final int PAGE_SIZE = 10;
    private static final int MIN_PAGE = 1;

    @Mock
    private SalesService salesService;

    @InjectMocks
    private SalesController subject;

    @Test
    @DisplayName("getAllSales validates page minimum and returns the service page")
    void test_getAllSales_1() {
        // Given
        final PageResponse<Sales> page = new PageResponse<>(List.of(new Sales()), new PaginationInfo(false, 3, PAGE_SIZE));
        when(this.salesService.getSalesPage(3, PAGE_SIZE)).thenReturn(page);

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<PageResponse<Sales>> response = this.subject.getAllSales(3);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(page);
            validations.verify(() -> RequestValidations.minimum(3, MIN_PAGE, "page"));
        }
    }

    @Test
    @DisplayName("getAllSales propagates the 400 raised by the page validator")
    void test_getAllSales_2() {
        // Given
        final ResponseStatusException failure = new ResponseStatusException(HttpStatus.BAD_REQUEST, "boom");

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            validations.when(() -> RequestValidations.minimum(0, MIN_PAGE, "page")).thenThrow(failure);

            // When & Then
            assertThatThrownBy(() -> this.subject.getAllSales(0)).isSameAs(failure);
            verifyNoInteractions(this.salesService);
        }
    }

    @Test
    @DisplayName("getSalesById validates id, returns 200 when present")
    void test_getSalesById_1() {
        // Given
        final Sales sale = new Sales();
        when(this.salesService.getSalesById(5L)).thenReturn(Optional.of(sale));

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<Sales> response = this.subject.getSalesById(5L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(sale);
            validations.verify(() -> RequestValidations.positive(5L, "id"));
        }
    }

    @Test
    @DisplayName("getSalesById returns 404 when the sale is not found")
    void test_getSalesById_2() {
        // Given
        when(this.salesService.getSalesById(99L)).thenReturn(Optional.empty());

        try (MockedStatic<RequestValidations> ignored = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<Sales> response = this.subject.getSalesById(99L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }

    @Test
    @DisplayName("getSalesById propagates the validation error and skips the service")
    void test_getSalesById_3() {
        // Given
        final ResponseStatusException failure = new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad");

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            validations.when(() -> RequestValidations.positive(-1L, "id")).thenThrow(failure);

            // When & Then
            assertThatThrownBy(() -> this.subject.getSalesById(-1L)).isSameAs(failure);
            verifyNoInteractions(this.salesService);
        }
    }

    @Test
    @DisplayName("getSalesByBrand validates brandId and returns the sales list")
    void test_getSalesByBrand_1() {
        // Given
        final List<Sales> expected = List.of(new Sales());
        when(this.salesService.getSalesByBrand(7L)).thenReturn(expected);

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<List<Sales>> response = this.subject.getSalesByBrand(7L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            validations.verify(() -> RequestValidations.positive(7L, "brandId"));
        }
    }

    @Test
    @DisplayName("getSalesByBrand propagates validation error and skips the service")
    void test_getSalesByBrand_2() {
        // Given
        final ResponseStatusException failure = new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad");

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            validations.when(() -> RequestValidations.positive(0L, "brandId")).thenThrow(failure);

            // When & Then
            assertThatThrownBy(() -> this.subject.getSalesByBrand(0L)).isSameAs(failure);
            verifyNoInteractions(this.salesService);
        }
    }

    @Test
    @DisplayName("getSalesByVehicle validates vehicleId and returns the sales list")
    void test_getSalesByVehicle_1() {
        // Given
        final List<Sales> expected = List.of(new Sales(), new Sales());
        when(this.salesService.getSalesByVehicle(4L)).thenReturn(expected);

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<List<Sales>> response = this.subject.getSalesByVehicle(4L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            validations.verify(() -> RequestValidations.positive(4L, "vehicleId"));
        }
    }

    @Test
    @DisplayName("getSalesByVehicle propagates validation error and skips the service")
    void test_getSalesByVehicle_2() {
        // Given
        final ResponseStatusException failure = new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad");

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            validations.when(() -> RequestValidations.positive(0L, "vehicleId")).thenThrow(failure);

            // When & Then
            assertThatThrownBy(() -> this.subject.getSalesByVehicle(0L)).isSameAs(failure);
            verifyNoInteractions(this.salesService);
        }
    }

    @Test
    @DisplayName("getBestSellingVehicles validates the date range and returns the service result")
    void test_getBestSellingVehicles_1() {
        // Given
        final LocalDate startDate = LocalDate.of(2024, 1, 1);
        final LocalDate endDate = LocalDate.of(2024, 12, 31);
        final List<BestSellingVehicleResponse> expected =
                List.of(new BestSellingVehicleResponse(1L, "Brand", "Model", "2024", 10L));
        when(this.salesService.getBestSellingVehicles(startDate, endDate)).thenReturn(expected);

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<List<BestSellingVehicleResponse>> response =
                    this.subject.getBestSellingVehicles(startDate, endDate);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            validations.verify(() -> RequestValidations.dateRange(startDate, endDate));
        }
    }

    @Test
    @DisplayName("getBestSellingVehicles accepts null date filters")
    void test_getBestSellingVehicles_2() {
        // Given
        when(this.salesService.getBestSellingVehicles(null, null)).thenReturn(List.of());

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<List<BestSellingVehicleResponse>> response =
                    this.subject.getBestSellingVehicles(null, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
            validations.verify(() -> RequestValidations.dateRange(null, null));
        }
    }

    @Test
    @DisplayName("getBestSellingVehicles propagates the 400 raised by the date-range validator")
    void test_getBestSellingVehicles_3() {
        // Given
        final LocalDate startDate = LocalDate.of(2024, 12, 31);
        final LocalDate endDate = LocalDate.of(2024, 1, 1);
        final ResponseStatusException failure = new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad");

        try (MockedStatic<RequestValidations> validations = mockStatic(RequestValidations.class)) {
            validations.when(() -> RequestValidations.dateRange(startDate, endDate)).thenThrow(failure);

            // When & Then
            assertThatThrownBy(() -> this.subject.getBestSellingVehicles(startDate, endDate)).isSameAs(failure);
            verifyNoInteractions(this.salesService);
        }
    }

    @Test
    @DisplayName("getSalesByBrand returns an empty list when there are no sales")
    void test_getSalesByBrand_3() {
        // Given
        when(this.salesService.getSalesByBrand(1L)).thenReturn(List.of());

        try (MockedStatic<RequestValidations> ignored = mockStatic(RequestValidations.class)) {
            // When
            final ResponseEntity<List<Sales>> response = this.subject.getSalesByBrand(1L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
            verify(this.salesService).getSalesByBrand(1L);
        }
    }
}
