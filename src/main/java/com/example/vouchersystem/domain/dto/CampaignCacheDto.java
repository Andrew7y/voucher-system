package com.example.vouchersystem.domain.dto;

import com.example.vouchersystem.domain.entity.CampaignStatus;

import java.time.LocalDateTime;

public record CampaignCacheDto(
        CampaignStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
