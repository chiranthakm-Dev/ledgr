package com.paystream.repository;

import com.paystream.domain.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, UUID> {
    Mono<Payment> findByIdempotencyKey(String idempotencyKey);
}