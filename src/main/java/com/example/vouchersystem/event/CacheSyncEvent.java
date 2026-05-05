package com.example.vouchersystem.event;

import com.example.vouchersystem.domain.dto.CampaignCacheDto;

import java.time.LocalDateTime;
import java.util.Map;

public record CacheSyncEvent(
        Action action,
        Map<String, Integer> quotasToSync,
        Map<String, CampaignCacheDto> infoToSync,
        Long campaignId,
        LocalDateTime campaignEndAt
) {
    public enum Action{
        UPSERT,
        DELETE
    }
}
