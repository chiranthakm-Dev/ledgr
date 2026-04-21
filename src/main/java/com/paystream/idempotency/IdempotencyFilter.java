package com.paystream.idempotency;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class IdempotencyFilter implements WebFilter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public IdempotencyFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getMethod().equals(org.springframework.http.HttpMethod.POST)) {
            return chain.filter(exchange);
        }

        String idempotencyKey = exchange.getRequest().getHeaders().getFirst("Idempotency-Key");
        if (idempotencyKey == null) {
            return chain.filter(exchange);
        }

        return redisTemplate.opsForValue().get(idempotencyKey)
            .flatMap(existing -> {
                // If key exists, it's a duplicate
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.CONFLICT);
                return exchange.getResponse().setComplete();
            })
            .switchIfEmpty(
                redisTemplate.opsForValue().set(idempotencyKey, "processing")
                    .then(chain.filter(exchange))
            );
    }
}