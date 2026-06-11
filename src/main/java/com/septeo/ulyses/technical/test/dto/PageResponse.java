package com.septeo.ulyses.technical.test.dto;

import java.util.List;

public record PageResponse<T>(List<T> data, PaginationInfo pagination) {

    public static <T> PageResponse<T> of(List<T> data, int page, int pageSize, boolean hasMore) {
        return new PageResponse<>(data, new PaginationInfo(hasMore, page, pageSize));
    }
}
