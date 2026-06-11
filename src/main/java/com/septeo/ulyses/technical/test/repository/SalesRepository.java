package com.septeo.ulyses.technical.test.repository;

import com.septeo.ulyses.technical.test.entity.Sales;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Sales entity.
 */
@Repository
public interface SalesRepository {
    /**
     * Find all sales.
     *
     * @return a list of all sales
     */
    List<Sales> findAll();

    /**
     * Find a sale by its ID.
     *
     * @param id the ID of the sale to find
     * @return an Optional containing the sale if found, or empty if not found
     */
    Optional<Sales> findById(Long id);

    /**
     * Page-based slice of sales. Implementations should fetch {@code pageSize + 1}
     * rows to compute {@code hasMore} without a separate {@code COUNT} query.
     *
     * @param page     1-based page number
     * @param pageSize items per page
     * @return at most {@code pageSize + 1} sales (the last one only signals there is more data)
     */
    List<Sales> findPage(int page, int pageSize);

    List<Sales> findByBrandId(Long brandId);

    List<Sales> findByVehicleId(Long vehicleId);
}
