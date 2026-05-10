package com.example.vouchersystem.listener;

import com.example.vouchersystem.event.CacheSyncEvent;
import com.example.vouchersystem.exception.CacheOperationException;
import com.example.vouchersystem.service.RedisService;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheSyncListener {
    private final RedisService redisService;
    private final ObservationRegistry observationRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(maxAttempts = 4, backoff = @Backoff(delay = 1000))
    @Observed(name = "cache.sync", contextualName = "sync-cache-to-redis")
    public void handleCacheSyncEvent(CacheSyncEvent event){
        if (observationRegistry.getCurrentObservation() != null){
            observationRegistry.getCurrentObservation()
                    .highCardinalityKeyValue("campaign.id", String.valueOf(event.campaignId()))
                    .lowCardinalityKeyValue("sync.action", event.action().name());
        }

        log.info("DB Commit Successful for Campaign ID = {}. Syncing to Redis...",
                event.campaignId()
        );

        try{
            if(event.action() == CacheSyncEvent.Action.UPSERT){
                long ttlInSeconds = calculateTtlWithBuffer(event.campaignEndAt());

                if(event.quotasToSync() != null){
                    event.quotasToSync().forEach((key, quota) -> {
                        redisService.setValue(key, quota, ttlInSeconds, TimeUnit.SECONDS);
                        log.debug("Redis Synced -> Key: {}, Quota: {}", key, quota);
                    });
                }

                if(event.infoToSync() != null){
                    event.infoToSync().forEach((key, info) -> {
                        redisService.setValue(key, info, ttlInSeconds, TimeUnit.SECONDS);
                        log.debug("Redis Synced -> Key: {}, Info: {}", key, info);
                    });
                }
            }else if(event.action() == CacheSyncEvent.Action.DELETE){
                if(event.quotasToSync() != null){
                    event.quotasToSync().keySet().forEach(key -> {
                        redisService.deleteValue(key);
                        log.debug("Redis Evicted -> Quota Key: {}", key);
                    });
                }

                if(event.infoToSync() != null){
                    event.infoToSync().keySet().forEach(key -> {
                        redisService.deleteValue(key);
                        log.debug("Redis Evicted -> Info Key: {}", key);
                    });
                }
            }
        }catch (Exception e){
            log.error("Failed to sync cache for Campaign ID = {}",event.campaignId(), e);
            throw new CacheOperationException("Redis sync failed", e);
        }
    }

    // ================
    // Helper Function
    // ================
    private long calculateTtlWithBuffer(LocalDateTime endAt){
        if(endAt == null){
            return 30 * 24 * 60 * 60L;
        }

        LocalDateTime now = LocalDateTime.now();
        if(endAt.isBefore(now)){
            return 7 * 24 * 60 * 60L;
        }

        long secondsUnitEnd = Duration.between(now, endAt).getSeconds();
        long bufferSeconds = 7 * 24 * 60 * 60L;

        return secondsUnitEnd + bufferSeconds;
    }
}
