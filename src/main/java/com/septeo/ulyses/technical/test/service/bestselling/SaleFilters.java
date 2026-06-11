package com.septeo.ulyses.technical.test.service.bestselling;

import com.septeo.ulyses.technical.test.entity.Sales;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.function.Predicate;

@UtilityClass
public final class SaleFilters {

    public static Predicate<Sales> byStartDate(LocalDate startDate) {
        if (startDate == null) {
            return sale -> true;
        }
        return sale -> !sale.getSaleDate().isBefore(startDate);
    }

    public static Predicate<Sales> byEndDate(LocalDate endDate) {
        if (endDate == null) {
            return sale -> true;
        }
        return sale -> !sale.getSaleDate().isAfter(endDate);
    }
}
