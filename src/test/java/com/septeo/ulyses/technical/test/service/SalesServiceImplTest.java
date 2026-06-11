package com.septeo.ulyses.technical.test.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.septeo.ulyses.technical.test.dto.BestSellingVehicleResponse;
import com.septeo.ulyses.technical.test.dto.PageResponse;
import com.septeo.ulyses.technical.test.dto.PaginationInfo;
import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.entity.Vehicle;
import com.septeo.ulyses.technical.test.repository.SalesRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesServiceImplTest {

    private static final int PAGE_SIZE = 3;

    @Mock
    private SalesRepository salesRepository;

    @InjectMocks
    private SalesServiceImpl subject;

    @Test
    @DisplayName("getSalesPage returns hasMore=true and drops the lookahead row when repo returns pageSize+1")
    void test_getSalesPage_1() {
        // Given
        final List<Sales> repoRows = List.of(sale(1L), sale(2L), sale(3L), sale(4L));
        when(this.salesRepository.findPage(1, PAGE_SIZE)).thenReturn(repoRows);

        // When
        final PageResponse<Sales> response = this.subject.getSalesPage(1, PAGE_SIZE);

        // Then
        assertThat(response.data()).hasSize(PAGE_SIZE);
        assertThat(response.data()).containsExactly(repoRows.get(0), repoRows.get(1), repoRows.get(2));
        assertThat(response.pagination()).isEqualTo(new PaginationInfo(true, 1, PAGE_SIZE));
    }

    @Test
    @DisplayName("getSalesPage returns hasMore=false when repo returns fewer rows than pageSize")
    void test_getSalesPage_2() {
        // Given
        final List<Sales> repoRows = List.of(sale(1L), sale(2L));
        when(this.salesRepository.findPage(2, PAGE_SIZE)).thenReturn(repoRows);

        // When
        final PageResponse<Sales> response = this.subject.getSalesPage(2, PAGE_SIZE);

        // Then
        assertThat(response.data()).isSameAs(repoRows);
        assertThat(response.pagination()).isEqualTo(new PaginationInfo(false, 2, PAGE_SIZE));
    }

    @Test
    @DisplayName("getSalesPage returns hasMore=false when repo returns exactly pageSize")
    void test_getSalesPage_3() {
        // Given
        final List<Sales> repoRows = List.of(sale(1L), sale(2L), sale(3L));
        when(this.salesRepository.findPage(1, PAGE_SIZE)).thenReturn(repoRows);

        // When
        final PageResponse<Sales> response = this.subject.getSalesPage(1, PAGE_SIZE);

        // Then
        assertThat(response.data()).isSameAs(repoRows);
        assertThat(response.pagination()).isEqualTo(new PaginationInfo(false, 1, PAGE_SIZE));
    }

    @Test
    @DisplayName("getSalesPage returns empty page when repo returns empty list")
    void test_getSalesPage_4() {
        // Given
        when(this.salesRepository.findPage(5, PAGE_SIZE)).thenReturn(List.of());

        // When
        final PageResponse<Sales> response = this.subject.getSalesPage(5, PAGE_SIZE);

        // Then
        assertThat(response.data()).isEmpty();
        assertThat(response.pagination()).isEqualTo(new PaginationInfo(false, 5, PAGE_SIZE));
    }

    @Test
    @DisplayName("getSalesById returns the sale when found")
    void test_getSalesById_1() {
        // Given
        final Sales sale = sale(42L);
        when(this.salesRepository.findById(42L)).thenReturn(Optional.of(sale));

        // When
        final Optional<Sales> result = this.subject.getSalesById(42L);

        // Then
        assertThat(result).contains(sale);
    }

    @Test
    @DisplayName("getSalesById returns empty when not found")
    void test_getSalesById_2() {
        // Given
        when(this.salesRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        final Optional<Sales> result = this.subject.getSalesById(99L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getSalesByBrand delegates to the repository and returns its result")
    void test_getSalesByBrand_1() {
        // Given
        final List<Sales> expected = List.of(sale(1L), sale(2L));
        when(this.salesRepository.findByBrandId(7L)).thenReturn(expected);

        // When
        final List<Sales> result = this.subject.getSalesByBrand(7L);

        // Then
        assertThat(result).isSameAs(expected);
        verify(this.salesRepository).findByBrandId(7L);
        verifyNoMoreInteractions(this.salesRepository);
    }

    @Test
    @DisplayName("getSalesByVehicle delegates to the repository and returns its result")
    void test_getSalesByVehicle_1() {
        // Given
        final List<Sales> expected = List.of(sale(1L));
        when(this.salesRepository.findByVehicleId(3L)).thenReturn(expected);

        // When
        final List<Sales> result = this.subject.getSalesByVehicle(3L);

        // Then
        assertThat(result).isSameAs(expected);
        verify(this.salesRepository).findByVehicleId(3L);
        verifyNoMoreInteractions(this.salesRepository);
    }

    @Test
    @DisplayName("getBestSellingVehicles returns top 5 ordered by sales count when no date filter is applied")
    void test_getBestSellingVehicles_1() {
        // Given
        final Vehicle v1 = vehicle(1L, "ModelA", "2020");
        final Vehicle v2 = vehicle(2L, "ModelB", "2021");
        final Vehicle v3 = vehicle(3L, "ModelC", "2022");
        final Vehicle v4 = vehicle(4L, "ModelD", "2023");
        final Vehicle v5 = vehicle(5L, "ModelE", "2024");
        final Vehicle v6 = vehicle(6L, "ModelF", "2025");
        // Counts: v1=4, v2=3, v3=2, v4=1, v5=1, v6=1 -> top 5: v1, v2, v3, v4|v5|v6 (ties keep insertion order)
        final List<Sales> all = List.of(
                saleOf(v1), saleOf(v1), saleOf(v1), saleOf(v1),
                saleOf(v2), saleOf(v2), saleOf(v2),
                saleOf(v3), saleOf(v3),
                saleOf(v4),
                saleOf(v5),
                saleOf(v6));
        when(this.salesRepository.findAll()).thenReturn(all);

        // When
        final List<BestSellingVehicleResponse> response = this.subject.getBestSellingVehicles(null, null);

        // Then
        assertThat(response).hasSize(5);
        assertThat(response.get(0).vehicleId()).isEqualTo(1L);
        assertThat(response.get(0).totalSales()).isEqualTo(4L);
        assertThat(response.get(0).brand()).isEqualTo("BrandX");
        assertThat(response.get(0).model()).isEqualTo("ModelA");
        assertThat(response.get(0).year()).isEqualTo("2020");
        assertThat(response.get(1).vehicleId()).isEqualTo(2L);
        assertThat(response.get(1).totalSales()).isEqualTo(3L);
        assertThat(response.get(2).vehicleId()).isEqualTo(3L);
        assertThat(response.get(2).totalSales()).isEqualTo(2L);
        // Ties at totalSales=1 -> first-seen order in the input is v4, v5, v6.
        assertThat(response.get(3).vehicleId()).isEqualTo(4L);
        assertThat(response.get(3).totalSales()).isEqualTo(1L);
        assertThat(response.get(4).vehicleId()).isEqualTo(5L);
        assertThat(response.get(4).totalSales()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getBestSellingVehicles filters sales by start and end date inclusively")
    void test_getBestSellingVehicles_2() {
        // Given
        final Vehicle v1 = vehicle(1L, "ModelA", "2020");
        final Vehicle v2 = vehicle(2L, "ModelB", "2021");
        final LocalDate start = LocalDate.of(2024, 1, 10);
        final LocalDate end = LocalDate.of(2024, 1, 20);
        final List<Sales> all = List.of(
                saleOf(v1, LocalDate.of(2024, 1, 9)),   // out (before start)
                saleOf(v1, LocalDate.of(2024, 1, 10)),  // in
                saleOf(v1, LocalDate.of(2024, 1, 15)),  // in
                saleOf(v2, LocalDate.of(2024, 1, 20)),  // in
                saleOf(v2, LocalDate.of(2024, 1, 21))); // out (after end)
        when(this.salesRepository.findAll()).thenReturn(all);

        // When
        final List<BestSellingVehicleResponse> response = this.subject.getBestSellingVehicles(start, end);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).vehicleId()).isEqualTo(1L);
        assertThat(response.get(0).totalSales()).isEqualTo(2L);
        assertThat(response.get(1).vehicleId()).isEqualTo(2L);
        assertThat(response.get(1).totalSales()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getBestSellingVehicles returns empty when no sales exist")
    void test_getBestSellingVehicles_3() {
        // Given
        when(this.salesRepository.findAll()).thenReturn(List.of());

        // When
        final List<BestSellingVehicleResponse> response = this.subject.getBestSellingVehicles(null, null);

        // Then
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("getBestSellingVehicles returns empty when all sales are filtered out by date")
    void test_getBestSellingVehicles_4() {
        // Given
        final Vehicle v1 = vehicle(1L, "ModelA", "2020");
        final List<Sales> all = List.of(saleOf(v1, LocalDate.of(2020, 1, 1)));
        when(this.salesRepository.findAll()).thenReturn(all);

        // When
        final List<BestSellingVehicleResponse> response = this.subject.getBestSellingVehicles(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        // Then
        assertThat(response).isEmpty();
        verifyNoMoreInteractions(this.salesRepository);
    }

    @Test
    @DisplayName("getBestSellingVehicles breaks ties using first-seen order of each vehicle")
    void test_getBestSellingVehicles_6() {
        // Given - all vehicles tie at 2 sales; expected order = first-seen order in the input.
        final Vehicle v10 = vehicle(10L, "ModelJ", "2020");
        final Vehicle v20 = vehicle(20L, "ModelT", "2021");
        final Vehicle v30 = vehicle(30L, "ModelD", "2022");
        final Vehicle v40 = vehicle(40L, "ModelF", "2023");
        final List<Sales> all = List.of(
                saleOf(v30), saleOf(v30),
                saleOf(v10), saleOf(v10),
                saleOf(v40), saleOf(v40),
                saleOf(v20), saleOf(v20));
        when(this.salesRepository.findAll()).thenReturn(all);

        // When
        final List<BestSellingVehicleResponse> response = this.subject.getBestSellingVehicles(null, null);

        // Then
        assertThat(response).extracting(BestSellingVehicleResponse::vehicleId)
                .containsExactly(30L, 10L, 40L, 20L);
        assertThat(response).extracting(BestSellingVehicleResponse::totalSales)
                .containsOnly(2L);
    }

    private static Sales sale(final Long id) {
        final Sales sale = new Sales();
        sale.setId(id);
        return sale;
    }

    private static Sales saleOf(final Vehicle vehicle) {
        return saleOf(vehicle, LocalDate.of(2024, 6, 1));
    }

    private static Sales saleOf(final Vehicle vehicle, final LocalDate saleDate) {
        final Sales sale = new Sales();
        sale.setVehicle(vehicle);
        sale.setSaleDate(saleDate);
        sale.setPrice(BigDecimal.ONE);
        return sale;
    }

    private static Vehicle vehicle(final Long id, final String model, final String year) {
        final Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("BrandX");
        final Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setColor("RED");
        return vehicle;
    }

    @Test
    @DisplayName("getSalesPage does not interact with anything other than the repository")
    void test_getSalesPage_5() {
        // Given
        when(this.salesRepository.findPage(1, PAGE_SIZE)).thenReturn(List.of());

        // When
        this.subject.getSalesPage(1, PAGE_SIZE);

        // Then
        verifyNoMoreInteractions(this.salesRepository);
    }

    @Test
    @DisplayName("getSalesById does not interact with anything other than findById")
    void test_getSalesById_3() {
        // Given
        when(this.salesRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        this.subject.getSalesById(1L);

        // Then
        verifyNoMoreInteractions(this.salesRepository);
    }

    @Test
    @DisplayName("getBestSellingVehicles does not trigger any other repository call")
    void test_getBestSellingVehicles_5() {
        // Given
        when(this.salesRepository.findAll()).thenReturn(List.of());

        // When
        this.subject.getBestSellingVehicles(null, null);

        // Then
        verify(this.salesRepository).findAll();
        verifyNoMoreInteractions(this.salesRepository);
    }

    @Test
    @DisplayName("getSalesByBrand returning empty list does not trigger extra interactions")
    void test_getSalesByBrand_2() {
        // Given
        when(this.salesRepository.findByBrandId(1L)).thenReturn(List.of());

        // When
        final List<Sales> result = this.subject.getSalesByBrand(1L);

        // Then
        assertThat(result).isEmpty();
        verifyNoMoreInteractions(this.salesRepository);
    }

    @Test
    @DisplayName("getBestSellingVehicles applies only startDate when endDate is null")
    void test_getBestSellingVehicles_7() {
        // Given
        final Vehicle v1 = vehicle(1L, "ModelA", "2020");
        final List<Sales> all = List.of(
                saleOf(v1, LocalDate.of(2023, 12, 31)), // out (before start)
                saleOf(v1, LocalDate.of(2024, 1, 1)),   // in (equal to start)
                saleOf(v1, LocalDate.of(2099, 1, 1)));  // in (no upper bound)
        when(this.salesRepository.findAll()).thenReturn(all);

        // When
        final List<BestSellingVehicleResponse> response =
                this.subject.getBestSellingVehicles(LocalDate.of(2024, 1, 1), null);

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).vehicleId()).isEqualTo(1L);
        assertThat(response.get(0).totalSales()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getBestSellingVehicles applies only endDate when startDate is null")
    void test_getBestSellingVehicles_8() {
        // Given
        final Vehicle v1 = vehicle(1L, "ModelA", "2020");
        final List<Sales> all = List.of(
                saleOf(v1, LocalDate.of(1999, 1, 1)),   // in (no lower bound)
                saleOf(v1, LocalDate.of(2024, 1, 1)),   // in (equal to end)
                saleOf(v1, LocalDate.of(2024, 1, 2))); // out (after end)
        when(this.salesRepository.findAll()).thenReturn(all);

        // When
        final List<BestSellingVehicleResponse> response =
                this.subject.getBestSellingVehicles(null, LocalDate.of(2024, 1, 1));

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).vehicleId()).isEqualTo(1L);
        assertThat(response.get(0).totalSales()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getBestSellingVehicles returns every distinct vehicle when there are fewer than K")
    void test_getBestSellingVehicles_9() {
        // Given
        final Vehicle v1 = vehicle(1L, "ModelA", "2020");
        final Vehicle v2 = vehicle(2L, "ModelB", "2021");
        when(this.salesRepository.findAll()).thenReturn(List.of(saleOf(v1), saleOf(v2), saleOf(v1)));

        // When
        final List<BestSellingVehicleResponse> response = this.subject.getBestSellingVehicles(null, null);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).vehicleId()).isEqualTo(1L);
        assertThat(response.get(0).totalSales()).isEqualTo(2L);
        assertThat(response.get(1).vehicleId()).isEqualTo(2L);
        assertThat(response.get(1).totalSales()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getBestSellingVehicles returns all vehicles when distinct count equals K")
    void test_getBestSellingVehicles_10() {
        // Given - 5 distinct vehicles, all entered into the buffer without rejections.
        final Vehicle v1 = vehicle(1L, "ModelA", "2020");
        final Vehicle v2 = vehicle(2L, "ModelB", "2021");
        final Vehicle v3 = vehicle(3L, "ModelC", "2022");
        final Vehicle v4 = vehicle(4L, "ModelD", "2023");
        final Vehicle v5 = vehicle(5L, "ModelE", "2024");
        when(this.salesRepository.findAll()).thenReturn(List.of(
                saleOf(v1), saleOf(v1), saleOf(v1), saleOf(v1), saleOf(v1),
                saleOf(v2), saleOf(v2), saleOf(v2), saleOf(v2),
                saleOf(v3), saleOf(v3), saleOf(v3),
                saleOf(v4), saleOf(v4),
                saleOf(v5)));

        // When
        final List<BestSellingVehicleResponse> response = this.subject.getBestSellingVehicles(null, null);

        // Then
        assertThat(response).hasSize(5);
        assertThat(response).extracting(BestSellingVehicleResponse::vehicleId)
                .containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(response).extracting(BestSellingVehicleResponse::totalSales)
                .containsExactly(5L, 4L, 3L, 2L, 1L);
    }

}
