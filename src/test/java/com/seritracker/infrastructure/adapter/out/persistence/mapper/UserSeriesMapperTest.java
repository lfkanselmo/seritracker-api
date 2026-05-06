package com.seritracker.infrastructure.adapter.out.persistence.mapper;

import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserSeriesMapper")
class UserSeriesMapperTest {

    private UserSeriesMapper mapper;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        mapper = new UserSeriesMapper();
        now    = LocalDateTime.now();
    }

    private UserSeriesEntity buildEntity() {
        UserSeriesEntity entity = new UserSeriesEntity();
        entity.setId(1L);
        entity.setUserId(2L);
        entity.setTmdbId(1396);
        entity.setTitle("Breaking Bad");
        entity.setPosterUrl("https://image.tmdb.org/t/p/w300/poster.jpg");
        entity.setStatus("WATCHING");
        entity.setRating(9);
        entity.setWatchedEpisodes(10);
        entity.setTotalEpisodes(62);
        entity.setNetwork("AMC");
        entity.setNotes("Great show");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private UserSeries buildDomain() {
        return UserSeries.builder()
                .id(1L)
                .userId(2L)
                .tmdbId(1396)
                .title("Breaking Bad")
                .posterUrl("https://image.tmdb.org/t/p/w300/poster.jpg")
                .status(SeriesStatus.WATCHING)
                .rating(9)
                .watchedEpisodes(10)
                .totalEpisodes(62)
                .network("AMC")
                .notes("Great show")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("should map entity to domain correctly")
    void shouldMapEntityToDomain_correctly() {
        UserSeriesEntity entity = buildEntity();
        UserSeries domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getUserId()).isEqualTo(entity.getUserId());
        assertThat(domain.getTmdbId()).isEqualTo(entity.getTmdbId());
        assertThat(domain.getTitle()).isEqualTo(entity.getTitle());
        assertThat(domain.getPosterUrl()).isEqualTo(entity.getPosterUrl());
        assertThat(domain.getStatus()).isEqualTo(SeriesStatus.WATCHING);
        assertThat(domain.getRating()).isEqualTo(entity.getRating());
        assertThat(domain.getWatchedEpisodes()).isEqualTo(entity.getWatchedEpisodes());
        assertThat(domain.getTotalEpisodes()).isEqualTo(entity.getTotalEpisodes());
        assertThat(domain.getNetwork()).isEqualTo(entity.getNetwork());
        assertThat(domain.getNotes()).isEqualTo(entity.getNotes());
    }

    @Test
    @DisplayName("should map domain to entity correctly")
    void shouldMapDomainToEntity_correctly() {
        UserSeries domain = buildDomain();
        UserSeriesEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getUserId()).isEqualTo(domain.getUserId());
        assertThat(entity.getTmdbId()).isEqualTo(domain.getTmdbId());
        assertThat(entity.getTitle()).isEqualTo(domain.getTitle());
        assertThat(entity.getPosterUrl()).isEqualTo(domain.getPosterUrl());
        assertThat(entity.getStatus()).isEqualTo("WATCHING");
        assertThat(entity.getRating()).isEqualTo(domain.getRating());
        assertThat(entity.getWatchedEpisodes()).isEqualTo(domain.getWatchedEpisodes());
        assertThat(entity.getTotalEpisodes()).isEqualTo(domain.getTotalEpisodes());
        assertThat(entity.getNetwork()).isEqualTo(domain.getNetwork());
        assertThat(entity.getNotes()).isEqualTo(domain.getNotes());
    }

    @Test
    @DisplayName("should map all SeriesStatus values correctly")
    void shouldMapAllStatuses_correctly() {
        for (SeriesStatus status : SeriesStatus.values()) {
            UserSeriesEntity entity = buildEntity();
            entity.setStatus(status.name());

            UserSeries domain = mapper.toDomain(entity);
            assertThat(domain.getStatus()).isEqualTo(status);

            UserSeriesEntity backToEntity = mapper.toEntity(domain);
            assertThat(backToEntity.getStatus()).isEqualTo(status.name());
        }
    }
}