package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class MarkEpisodeRequest {

    @NotNull(message = "watched is required")
    Boolean watched;
}
