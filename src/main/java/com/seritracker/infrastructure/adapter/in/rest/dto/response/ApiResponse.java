package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ApiResponse<T> {

    boolean success;
    T data;
    String message;
    LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("Created")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiResponse<Void> noContent() {
        return ApiResponse.<Void>builder()
                .success(true)
                .data(null)
                .message("Deleted")
                .timestamp(LocalDateTime.now())
                .build();
    }
}