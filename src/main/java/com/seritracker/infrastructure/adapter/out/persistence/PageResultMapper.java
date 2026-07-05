package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.PageResult;
import org.springframework.data.domain.Page;

import java.util.function.Function;

final class PageResultMapper {

    private PageResultMapper() {
    }

    static <E, T> PageResult<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
