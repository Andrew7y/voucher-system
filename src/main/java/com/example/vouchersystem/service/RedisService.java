package com.example.vouchersystem.service;

import com.example.vouchersystem.exception.CacheOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

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
}
