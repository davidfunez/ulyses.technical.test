package com.septeo.ulyses.technical.test.validation;

import lombok.experimental.UtilityClass;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Lightweight request-parameter validations.
 *
 * <p>Each method throws {@link ResponseStatusException} with status 400 when the
 * precondition is violated. Kept as a utility class to avoid pulling in any
 * external validation library (as required by the technical test) while keeping
 * controllers free of boilerplate.
 *
 * <p><b>Design note:</b> A declarative approach (Bean Validation with {@code @Min},
 * {@code @Positive}, etc.) would be preferred in a production codebase, but it is
 * intentionally avoided here to respect the "no external libraries" constraint.
 * For the current scope (a handful of endpoints with simple, stateless checks),
 * this utility offers the best trade-off between readability, testability and
 * cost of ownership.
 */
@UtilityClass
public class RequestValidations {

    public static void positive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "%s must be a positive number".formatted(fieldName));
        }
    }

    public static void minimum(int value, int min, String fieldName) {
        if (value < min) {
            throw new ResponseStatusException(BAD_REQUEST, "%s must be >= %d".formatted(fieldName, min));
        }
    }

    public static void dateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate must be before or equal to endDate");
        }
    }
}
