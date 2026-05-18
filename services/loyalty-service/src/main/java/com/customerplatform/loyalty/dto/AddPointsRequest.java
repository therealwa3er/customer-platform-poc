package com.customerplatform.loyalty.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddPointsRequest(
        @Min(1)
        int points,

        @NotBlank
        String reason,

        @Email
        @NotBlank
        String email
) {}
