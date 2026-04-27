package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class UpdateStatusRequest {

    @NotNull(message = "status is required")
    @Pattern(
            regexp = "WATCHING|WANT_TO_WATCH|COMPLETED|ABANDONED",
            message = "status must be WATCHING, WANT_TO_WATCH, COMPLETED or ABANDONED"
    )
    String status;
}