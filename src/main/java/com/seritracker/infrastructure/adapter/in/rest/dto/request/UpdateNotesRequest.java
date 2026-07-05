package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class UpdateNotesRequest {

    @Size(max = 2000, message = "notes must be at most 2000 characters")
    String notes;
}
