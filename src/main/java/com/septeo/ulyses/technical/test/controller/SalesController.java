package com.septeo.ulyses.technical.test.controller;

import com.septeo.ulyses.technical.test.dto.BestSellingVehicleResponse;
import com.septeo.ulyses.technical.test.dto.PageResponse;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.service.SalesService;
import com.septeo.ulyses.technical.test.validation.RequestValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Slf4j
public class SalesController {

    private static final String LOG_PREFIX = "[SALES-API] ";
    private static final int PAGE_SIZE = 10;
    private static final int MIN_PAGE = 1;

    private final SalesService salesService;

    @GetMapping
    public ResponseEntity<PageResponse<Sales>> getAllSales(@RequestParam(defaultValue = "1") int page) {
        log.debug("{}GET /api/sales page={}", LOG_PREFIX, page);
        RequestValidations.minimum(page, MIN_PAGE, "page");
        return ResponseEntity.ok(salesService.getSalesPage(page, PAGE_SIZE));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sales> getSalesById(@PathVariable Long id) {
        log.debug("{}GET /api/sales/{}", LOG_PREFIX, id);
        RequestValidations.positive(id, "id");

        return salesService.getSalesById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/brands/{brandId}")
    public ResponseEntity<List<Sales>> getSalesByBrand(@PathVariable Long brandId) {
        log.debug("{}GET /api/sales/brands/{}", LOG_PREFIX, brandId);
        RequestValidations.positive(brandId, "brandId");

        return ResponseEntity.ok(salesService.getSalesByBrand(brandId));
    }

    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<List<Sales>> getSalesByVehicle(@PathVariable Long vehicleId) {
        log.debug("{}GET /api/sales/vehicles/{}", LOG_PREFIX, vehicleId);
        RequestValidations.positive(vehicleId, "vehicleId");
        return ResponseEntity.ok(salesService.getSalesByVehicle(vehicleId));
    }

    @GetMapping("/vehicles/bestSelling")
    public ResponseEntity<List<BestSellingVehicleResponse>> getBestSellingVehicles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("{}GET /api/sales/vehicles/bestSelling startDate={} endDate={}", LOG_PREFIX, startDate, endDate);
        RequestValidations.dateRange(startDate, endDate);
        return ResponseEntity.ok(salesService.getBestSellingVehicles(startDate, endDate));
    }
}
