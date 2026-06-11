package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.dto.BestSellingVehicleResponse;
import com.septeo.ulyses.technical.test.dto.PageResponse;
import com.septeo.ulyses.technical.test.entity.Sales;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Sales operations.
 */
public interface SalesService {

    PageResponse<Sales> getSalesPage(int page, int pageSize);

    /**
     * Get a sales by its ID.
     *
     * @param id the ID of the sales to find
     * @return an Optional containing the sales if found, or empty if not found
     */
    Optional<Sales> getSalesById(Long id);

    List<Sales> getSalesByBrand(Long brandId);

    List<Sales> getSalesByVehicle(Long vehicleId);

    List<BestSellingVehicleResponse> getBestSellingVehicles(LocalDate startDate, LocalDate endDate);
}
