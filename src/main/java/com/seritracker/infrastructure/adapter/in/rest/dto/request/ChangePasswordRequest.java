package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class ChangePasswordRequest {

    @NotBlank(message = "current password is required")
    String currentPassword;

    @NotBlank(message = "new password is required")
    @Size(min = 8, message = "new password must be at least 8 characters")
    String newPassword;
}
