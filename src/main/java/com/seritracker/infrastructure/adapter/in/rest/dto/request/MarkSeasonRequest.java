package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Value;

import java.util.List;

@Value
public class MarkSeasonRequest {

    @NotEmpty(message = "episodeNumbers must not be empty")
    List<Integer> episodeNumbers;
}
