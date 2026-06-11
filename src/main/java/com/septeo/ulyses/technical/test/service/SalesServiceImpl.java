package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.dto.BestSellingVehicleResponse;
import com.septeo.ulyses.technical.test.dto.PageResponse;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.entity.Vehicle;
import com.septeo.ulyses.technical.test.repository.SalesRepository;
import com.septeo.ulyses.technical.test.service.bestselling.SaleFilters;
import com.septeo.ulyses.technical.test.service.bestselling.TopKSelector;
import com.septeo.ulyses.technical.test.service.bestselling.VehicleSalesCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Implementation of the SalesService interface.
 * This class provides the implementation for all sales-related operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SalesServiceImpl implements SalesService {

    private static final String LOG_PREFIX = "[SALES] ";
    private static final int TOP_K_BEST_SELLING = 5;

    private final SalesRepository salesRepository;

    @Override
    public PageResponse<Sales> getSalesPage(int page, int pageSize) {
        log.debug("{}getSalesPage page={} pageSize={}", LOG_PREFIX, page, pageSize);
        final List<Sales> rows = salesRepository.findPage(page, pageSize);
        final boolean hasMore = rows.size() > pageSize;
        final List<Sales> data = hasMore ? rows.subList(0, pageSize) : rows;

        return PageResponse.of(data, page, pageSize, hasMore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Sales> getSalesById(Long id) {
        log.debug("{}getSalesById id={}", LOG_PREFIX, id);
        return salesRepository.findById(id);
    }

    @Override
    public List<Sales> getSalesByBrand(Long brandId) {
        log.debug("{}getSalesByBrand brandId={}", LOG_PREFIX, brandId);
        return salesRepository.findByBrandId(brandId);
    }

    @Override
    public List<Sales> getSalesByVehicle(Long vehicleId) {
        log.debug("{}getSalesByVehicle vehicleId={}", LOG_PREFIX, vehicleId);
        return salesRepository.findByVehicleId(vehicleId);
    }

    @Override
    public List<BestSellingVehicleResponse> getBestSellingVehicles(LocalDate startDate, LocalDate endDate) {
        log.info("{}getBestSellingVehicles startDate={} endDate={}", LOG_PREFIX, startDate, endDate);
        final Predicate<Sales> filter = SaleFilters.byStartDate(startDate)
                .and(SaleFilters.byEndDate(endDate));

        final List<Sales> all = salesRepository.findAll();
        final List<VehicleSalesCount> counts = countSalesByVehicle(all, filter);
        final List<VehicleSalesCount> top = TopKSelector.of(counts, TOP_K_BEST_SELLING, VehicleSalesCount::totalSales);
        log.debug("{}getBestSellingVehicles scanned={} distinctVehicles={} returned={}",
                LOG_PREFIX, all.size(), counts.size(), top.size());
        return top.stream().map(this::toResponse).toList();
    }

    private List<VehicleSalesCount> countSalesByVehicle(List<Sales> sales, Predicate<Sales> filter) {
        // LinkedHashMap keeps first-seen vehicle order, which is the tie-break contract for top-K.
        final Map<Long, VehicleSalesCount> byVehicle = new LinkedHashMap<>();
        for (Sales sale : sales) {
            if (!filter.test(sale)) {
                continue;
            }
            final Vehicle vehicle = sale.getVehicle();
            byVehicle.merge(
                    vehicle.getId(),
                    new VehicleSalesCount(vehicle, 1L),
                    (existing, fresh) -> new VehicleSalesCount(existing.vehicle(), existing.totalSales() + 1L));
        }
        return List.copyOf(byVehicle.values());
    }

    private BestSellingVehicleResponse toResponse(VehicleSalesCount count) {
        final Vehicle vehicle = count.vehicle();
        return new BestSellingVehicleResponse(
                vehicle.getId(),
                vehicle.getBrand().getName(),
                vehicle.getModel(),
                vehicle.getYear(),
                count.totalSales());
    }
}
