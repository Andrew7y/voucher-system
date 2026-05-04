package com.example.vouchersystem.domain.dto;

import jakarta.validation.constraints.NotNull;

public record ClaimRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Rule ID is required")
        Long ruleId
){}
