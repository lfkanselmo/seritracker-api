package com.seritracker.domain.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class PageRequest {
    int page;
    int size;
}
