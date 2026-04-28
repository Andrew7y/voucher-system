package com.example.vouchersystem.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CampaignCreateRequest(
        @NotNull(message = "Campaign title cannot be blank")
        String title,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startAt,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        LocalDateTime endAt,

        @NotEmpty(message = "At least one voucher rule is required")
        @Valid
        List<VoucherRuleCreateRequest> voucherRules
) {}
