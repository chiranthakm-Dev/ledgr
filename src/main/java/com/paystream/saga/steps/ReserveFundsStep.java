package com.paystream.saga.steps;

import com.paystream.domain.Payment;
import com.paystream.saga.SagaStep;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ReserveFundsStep implements SagaStep {

    @Override
    public Mono<Void> execute(Payment payment) {
        // TODO: Debit merchant's settlement account
        // For now, simulate
        return Mono.empty();
    }

    @Override
    public Mono<Void> compensate(Payment payment) {
        // Credit back the merchant account
        return Mono.empty();
    }
}