package com.seritracker.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class LoginRequest {

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    String email;

    @NotBlank(message = "password is required")
    String password;
}