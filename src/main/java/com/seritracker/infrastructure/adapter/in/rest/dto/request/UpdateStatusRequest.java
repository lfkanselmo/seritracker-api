package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import com.seritracker.domain.model.SeriesStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class UpdateStatusRequest {

    @NotNull(message = "status is required")
    SeriesStatus status;
}
