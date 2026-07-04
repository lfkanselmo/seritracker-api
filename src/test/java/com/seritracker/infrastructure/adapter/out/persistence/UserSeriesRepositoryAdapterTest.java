package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.UserSeriesMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSeriesRepositoryAdapter")
class UserSeriesRepositoryAdapterTest {

    @Mock private JpaUserSeriesRepository jpaRepository;
    @Mock private UserSeriesMapper        mapper;

    @InjectMocks private UserSeriesRepositoryAdapter adapter;

    private UserSeries buildDomain(Long id) {
        return UserSeries.builder()
                .id(id)
                .userId(1L)
                .tmdbId(1396)
                .title("Breaking Bad")
                .status(SeriesStatus.WATCHING)
                .watchedEpisodes(0)
                .totalEpisodes(62)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserSeriesEntity buildEntity(Long id) {
        UserSeriesEntity entity = new UserSeriesEntity();
        entity.setId(id);
        entity.setUserId(1L);
        entity.setTmdbId(1396);
        entity.setTitle("Breaking Bad");
        entity.setStatus("WATCHING");
        entity.setWatchedEpisodes(0);
        entity.setTotalEpisodes(62);
        return entity;
    }

    @Test
    @DisplayName("save should persist and return domain object")
    void save_shouldPersistAndReturnDomain() {
        UserSeries domain   = buildDomain(1L);
        UserSeriesEntity entity = buildEntity(1L);

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        UserSeries result = adapter.save(domain);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(jpaRepository).save(entity);
    }

    @Test
    @DisplayName("findById should return domain when found")
    void findById_shouldReturnDomain_whenFound() {
        UserSeriesEntity entity = buildEntity(1L);
        UserSeries domain = buildDomain(1L);

        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<UserSeries> result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById should return empty when not found")
    void findById_shouldReturnEmpty_whenNotFound() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<UserSeries> result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserId should return mapped page")
    void findAllByUserId_shouldReturnMappedPage() {
        UserSeriesEntity entity = buildEntity(1L);
        UserSeries domain = buildDomain(1L);
        org.springframework.data.domain.PageRequest springPageable =
                org.springframework.data.domain.PageRequest.of(0, 20);

        when(jpaRepository.findAllByUserId(1L, springPageable))
                .thenReturn(new PageImpl<>(List.of(entity), springPageable, 1));
        when(mapper.toDomain(entity)).thenReturn(domain);

        PageResult<UserSeries> result = adapter.findAllByUserId(1L, com.seritracker.domain.model.PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByUserIdAndStatus should filter by status")
    void findByUserIdAndStatus_shouldFilterByStatus() {
        UserSeriesEntity entity = buildEntity(1L);
        UserSeries domain = buildDomain(1L);

        when(jpaRepository.findByUserIdAndStatus(1L, "WATCHING")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<UserSeries> result = adapter.findByUserIdAndStatus(1L, SeriesStatus.WATCHING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(SeriesStatus.WATCHING);
    }

    @Test
    @DisplayName("existsByUserIdAndTmdbId should return true when exists")
    void existsByUserIdAndTmdbId_shouldReturnTrue_whenExists() {
        when(jpaRepository.existsByUserIdAndTmdbId(1L, 1396)).thenReturn(true);

        assertThat(adapter.existsByUserIdAndTmdbId(1L, 1396)).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndTmdbId should return false when not exists")
    void existsByUserIdAndTmdbId_shouldReturnFalse_whenNotExists() {
        when(jpaRepository.existsByUserIdAndTmdbId(1L, 9999)).thenReturn(false);

        assertThat(adapter.existsByUserIdAndTmdbId(1L, 9999)).isFalse();
    }

    @Test
    @DisplayName("deleteById should call repository deleteById")
    void deleteById_shouldCallRepositoryDeleteById() {
        doNothing().when(jpaRepository).deleteById(1L);

        adapter.deleteById(1L);

        verify(jpaRepository).deleteById(1L);
    }
}