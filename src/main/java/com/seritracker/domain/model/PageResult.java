package com.seritracker.domain.model;

import lombok.Value;

import java.util.List;

@Value
public class PageResult<T> {
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
