package com.seritracker.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Exceptions")
class ExceptionTest {

    @Test
    @DisplayName("SeriesNotFoundException should contain id in message")
    void seriesNotFoundException_shouldContainIdInMessage() {
        SeriesNotFoundException ex = new SeriesNotFoundException(42L);
        assertThat(ex.getMessage()).contains("42");
    }

    @Test
    @DisplayName("SeriesNotFoundException should contain tmdbId in message")
    void seriesNotFoundException_shouldContainTmdbIdInMessage() {
        SeriesNotFoundException ex = new SeriesNotFoundException(1396);
        assertThat(ex.getMessage()).contains("1396");
    }

    @Test
    @DisplayName("DuplicateSeriesException should contain tmdbId in message")
    void duplicateSeriesException_shouldContainTmdbIdInMessage() {
        DuplicateSeriesException ex = new DuplicateSeriesException(1396);
        assertThat(ex.getMessage()).contains("1396");
    }
}