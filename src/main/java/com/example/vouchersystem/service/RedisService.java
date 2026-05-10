package com.example.vouchersystem.service;

import com.example.vouchersystem.exception.CacheOperationException;
import com.example.vouchersystem.service.alert.AlertService;
import io.lettuce.core.RedisException;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> claimVoucherScript;
    private final RedisScript<Long> rollbackClaimScript;
    private final AlertService alertService;
    private final ObservationRegistry observationRegistry;

    public RedisService(
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("claimVoucherScript") RedisScript<Long> claimVoucherScript,
            @Qualifier("rollbackClaimScript") RedisScript<Long> rollbackClaimScript,
            AlertService alertService,
            ObservationRegistry observationRegistry
    ){
        this.redisTemplate = redisTemplate;
        this.claimVoucherScript = claimVoucherScript;
        this.rollbackClaimScript = rollbackClaimScript;
        this.alertService = alertService;
        this.observationRegistry = observationRegistry;
    }

    public void setValue(
            String key,
            Object value,
            long timeout,
            TimeUnit unit
    ){
        try{
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Saved to Redis: Key = {}", key);
        } catch (Exception e){
            log.error("Error saving to Redis. Key = {}", key, e);
            throw new CacheOperationException("Failed to sync quota to Redis for key: " + key, e);
        }
    }

    public void setValueForever(String key, Object value){
        try{
            redisTemplate.opsForValue().set(key, value);
            log.debug("Saved to Redis Forever: Key = {}", key);
        } catch(Exception e){
            log.error("Error saving to Redis Forever. Key = {}", key, e);
            throw new CacheOperationException("Failed to sync quota to Redis Forever for key: " + key, e);
        }
    }

    public Object getValue(String key){
        try{
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Error getting from Redis. Key = {}", key, e);
            return null;
        }
    }

    public void deleteValue(String key){
        try{
            redisTemplate.delete(key);
            log.debug("Deleted from Redis: Key = {}", key);
        }catch (Exception e){
            log.error("Error deleting from Redis. Key = {}", key, e);
            throw new CacheOperationException("Failed to delete from Redis for key: " + key, e);
        }
    }

    @Observed(name = "redis.lua.claim", contextualName = "execute-claim-script")
    public long executeClaimScript(
            String quotaKey,
            String claimedUsersKey,
            String userId
    ){
        if(observationRegistry.getCurrentObservation() != null){
            observationRegistry.getCurrentObservation()
                    .lowCardinalityKeyValue("redis.quota_key", quotaKey)
                    .highCardinalityKeyValue("user_id", userId);
        }

        try{
            Long result = redisTemplate.execute(
                    claimVoucherScript,
                    List.of(quotaKey, claimedUsersKey),
                    userId
            );

            return result != null ? result : -1L;
        }catch (Exception e){
            log.error("Critical Error executing Claim Lua Script for User ID: {}", userId, e);
            throw new CacheOperationException("Redis Lua execution failed", e);
        }
    }

    @Retryable(
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    @Observed(name = "redis.lua.rollback", contextualName = "execute-rollback-script")
    public void rollbackClaimAtomically(
            String quotaKey,
            String claimedUserKey,
            String userId
    ){
        if (observationRegistry.getCurrentObservation() != null){
            observationRegistry.getCurrentObservation()
                    .lowCardinalityKeyValue("redis.quota_key", quotaKey)
                    .highCardinalityKeyValue("user_id", userId);
        }

        try{
            Long result = redisTemplate.execute(
                    rollbackClaimScript,
                    List.of(quotaKey, claimedUserKey),
                    userId
            );

            if(result != null && result == 1L){
                log.info("Successfully rolled back claim for User ID: {}", userId);
            }else{
                log.warn("Rollback bypassed. User ID: {} was not in the claimed set.", userId);
            }
        }catch (RedisException e){
            log.error("CRITICAL DATA LOSS: Failed to rollback Redis claim for User ID: {}. Retrying...",
                    userId, e);
            throw e;
        }
    }

    @Recover
    @Observed(name = "redis.lua.recover_rollback", contextualName = "recover-rollback-script")
    public void recoverRollbackFailure(
            RedisException e,
            String quotaKey,
            String claimedUserKey,
            String userId
    ){
        if (observationRegistry.getCurrentObservation() != null){
            observationRegistry.getCurrentObservation()
                    .lowCardinalityKeyValue("redis.quota_key", quotaKey)
                    .highCardinalityKeyValue("user_id", userId);
        }

        log.error("CRITICAL DATA LOSS ALERT: Failed to rollback Redis claim for User ID: {} after max retries!",
                userId, e);

        String subject = "Redis Rollback Failure";
        String details = String
                .format(
                        """
                                *Affected User ID:* *%s*
                                *Affected Quota Key:* *%s*
                                *Claimed Set Key (to remove user):* %s
                                *Error Message:* `%s`
                                _Action Required: Check Redis cluster status immediately and manually restore quota._
                                """, userId, quotaKey, claimedUserKey, e.getMessage());
        alertService.sendCriticalAlert(subject, details);
        throw new CacheOperationException("Critical system failure during rollback", e);
    }
}
