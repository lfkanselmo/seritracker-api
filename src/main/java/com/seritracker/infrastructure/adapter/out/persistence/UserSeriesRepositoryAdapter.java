package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.SortDirection;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.out.UserSeriesRepository;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.UserSeriesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserSeriesRepositoryAdapter implements UserSeriesRepository {

    private final JpaUserSeriesRepository jpaRepository;
    private final UserSeriesMapper mapper;

    @Override
    public UserSeries save(UserSeries userSeries) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(userSeries))
        );
    }

    @Override
    public Optional<UserSeries> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<UserSeries> findAllByUserId(Long userId, PageRequest pageRequest) {
        var pageable = toPageable(pageRequest);
        Page<UserSeriesEntity> page = hasSearch(pageRequest)
                ? jpaRepository.findAllByUserIdAndTitleContainingIgnoreCase(userId, pageRequest.getSearch(), pageable)
                : jpaRepository.findAllByUserId(userId, pageable);
        return toPageResult(page);
    }

    @Override
    public List<UserSeries> findByUserIdAndStatus(Long userId, SeriesStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<UserSeries> findByUserIdAndStatus(Long userId, SeriesStatus status, PageRequest pageRequest) {
        var pageable = toPageable(pageRequest);
        Page<UserSeriesEntity> page = hasSearch(pageRequest)
                ? jpaRepository.findByUserIdAndStatusAndTitleContainingIgnoreCase(userId, status.name(), pageRequest.getSearch(), pageable)
                : jpaRepository.findByUserIdAndStatus(userId, status.name(), pageable);
        return toPageResult(page);
    }

    @Override
    public boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId) {
        return jpaRepository.existsByUserIdAndTmdbId(userId, tmdbId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private org.springframework.data.domain.PageRequest toPageable(PageRequest pageRequest) {
        Sort sort = (pageRequest.getSortBy() != null && pageRequest.getSortDirection() != null)
                ? Sort.by(
                        pageRequest.getSortDirection() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                        pageRequest.getSortBy())
                : Sort.unsorted();
        return org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
    }

    private boolean hasSearch(PageRequest pageRequest) {
        return pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank();
    }

    private PageResult<UserSeries> toPageResult(Page<UserSeriesEntity> page) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
