package com.paystream.ratelimit;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class MerchantRateLimiter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public MerchantRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isAllowed(String merchantId) {
        String key = "rate_limit:" + merchantId;
        return redisTemplate.opsForValue().get(key)
            .map(count -> Integer.parseInt(count) < 100) // 100 requests per minute
            .switchIfEmpty(Mono.just(true))
            .flatMap(allowed -> {
                if (allowed) {
                    return redisTemplate.opsForValue().increment(key)
                        .then(redisTemplate.expire(key, Duration.ofMinutes(1)))
                        .thenReturn(true);
                }
                return Mono.just(false);
            });
    }
}