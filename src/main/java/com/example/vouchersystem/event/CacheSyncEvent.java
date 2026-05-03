package com.example.vouchersystem.event;

import java.util.Map;

public record CacheSyncEvent(
        Action action,
        Map<String, Integer> quotasToSync,
        Long campaignId
) {
    public enum Action{
        UPSERT,
        DELETE
    }
}
