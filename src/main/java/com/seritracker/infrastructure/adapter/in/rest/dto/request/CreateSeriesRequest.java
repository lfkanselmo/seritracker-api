package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import com.seritracker.domain.model.SeriesStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class CreateSeriesRequest {

    @NotNull(message = "tmdbId is required")
    Integer tmdbId;

    SeriesStatus status;
}
