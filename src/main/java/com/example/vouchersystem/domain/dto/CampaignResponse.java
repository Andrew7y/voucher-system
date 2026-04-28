package com.example.vouchersystem.domain.dto;

import com.example.vouchersystem.domain.entity.CampaignStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CampaignResponse(
        Long id,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        CampaignStatus status,
        List<VoucherRuleResponse> voucherRules
) {}
