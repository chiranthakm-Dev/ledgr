package com.paystream.saga.steps;

import com.paystream.domain.Payment;
import com.paystream.saga.SagaStep;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ValidatePaymentStep implements SagaStep {

    @Override
    public Mono<Void> execute(Payment payment) {
        // Validate amount, currency, merchant limits
        if (payment.amount() <= 0) {
            return Mono.error(new IllegalArgumentException("Invalid amount"));
        }
        if (!List.of("EUR", "USD", "GBP").contains(payment.currency())) {
            return Mono.error(new IllegalArgumentException("Unsupported currency"));
        }
        // TODO: Check merchant limits
        return Mono.empty();
    }

    @Override
    public Mono<Void> compensate(Payment payment) {
        // No compensation needed for validation
        return Mono.empty();
    }
}