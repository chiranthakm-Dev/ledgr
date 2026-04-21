package com.paystream.saga;

import com.paystream.domain.Payment;
import reactor.core.publisher.Mono;

public interface SagaStep {
    Mono<Void> execute(Payment payment);
    Mono<Void> compensate(Payment payment);
}