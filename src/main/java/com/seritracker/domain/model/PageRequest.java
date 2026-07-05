package com.seritracker.domain.model;

import lombok.Value;

@Value
public class PageRequest {
    int page;
    int size;
    String search;
    String sortBy;
    SortDirection sortDirection;

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, null, null);
    }

    public static PageRequest of(int page, int size, String search, String sortBy, SortDirection sortDirection) {
        return new PageRequest(page, size, search, sortBy, sortDirection);
    }
}
