package com.example.vouchersystem.listener;

import com.example.vouchersystem.event.CacheSyncEvent;
import com.example.vouchersystem.exception.CacheOperationException;
import com.example.vouchersystem.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheSyncListener {
    private final RedisService redisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public void handleCacheSyncEvent(CacheSyncEvent event){
        log.info("DB Commit Successful for Campaign ID = {}. Syncing to Redis...",
                event.campaignId()
        );

        try{
            if(event.action() == CacheSyncEvent.Action.UPSERT){
                event.quotasToSync().forEach((key, quota) -> {
                    redisService.setValueForever(key, quota);
                    log.debug("Redis Synced -> Key: {}, Quota: {}", key, quota);
                });
            }else if(event.action() == CacheSyncEvent.Action.DELETE){
                event.quotasToSync().keySet().forEach(key -> {
                    redisService.deleteValue(key);
                    log.debug("Redis Evicted -> Key: {}", key);
                });
            }
        }catch (Exception e){
            log.error("Failed to sync cache for Campaign ID = {}",event.campaignId(), e);
            throw new CacheOperationException("Redis sync failed", e);
        }
    }
}
