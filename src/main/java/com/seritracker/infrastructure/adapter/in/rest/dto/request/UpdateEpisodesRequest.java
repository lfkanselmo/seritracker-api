package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class UpdateEpisodesRequest {

    @NotNull(message = "watchedEpisodes is required")
    @Min(value = 0, message = "watchedEpisodes must be at least 0")
    Integer watchedEpisodes;
}