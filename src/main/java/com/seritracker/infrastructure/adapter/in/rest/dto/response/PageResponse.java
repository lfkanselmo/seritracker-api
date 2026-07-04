package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.PageResult;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.function.Function;

@Value
@Builder
public class PageResponse<T> {

    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;

    public static <D, T> PageResponse<T> from(PageResult<D> pageResult, Function<D, T> mapper) {
        return PageResponse.<T>builder()
                .content(pageResult.getContent().stream().map(mapper).toList())
                .page(pageResult.getPage())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }
}
