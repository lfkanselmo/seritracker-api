package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.Badge;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BadgeResponse {
    String code;
    boolean earned;
    int progressCurrent;
    int progressTarget;

    public static BadgeResponse from(Badge domain) {
        return BadgeResponse.builder()
                .code(domain.getCode().name())
                .earned(domain.isEarned())
                .progressCurrent(domain.getProgressCurrent())
                .progressTarget(domain.getProgressTarget())
                .build();
    }
}
