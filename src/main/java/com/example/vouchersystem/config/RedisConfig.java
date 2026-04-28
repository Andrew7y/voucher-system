package com.example.vouchersystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
public class RedisConfig {
    private JsonMapper redisMapper() {
        return JsonMapper.builder()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType("com.example.vouchersystem")
                                .allowIfBaseType("java.lang")
                                .allowIfBaseType("java.time")
                                .allowIfBaseType("java.util")
                                .allowIfBaseType("java.math")
                                .allowIfBaseType("java.util.concurrent")
                                .allowIfBaseType("java.util.Optional")
                                .allowIfBaseType("java.util.Record")
                                .build()
                )
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory
    ){
        GenericJacksonJsonRedisSerializer serializer =
                new GenericJacksonJsonRedisSerializer(redisMapper());

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
