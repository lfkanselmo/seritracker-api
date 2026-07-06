package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Badge {
    BadgeCode code;
    boolean earned;
    int progressCurrent;
    int progressTarget;
}
