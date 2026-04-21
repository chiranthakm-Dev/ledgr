package com.paystream.saga;

import com.paystream.domain.Payment;
import com.paystream.saga.steps.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class PaymentSaga {

    private final List<SagaStep> steps = List.of(
        new ValidatePaymentStep(),
        new ReserveFundsStep(),
        new NotifyMerchantStep()
    );

    public Mono<Payment> execute(Payment payment) {
        return Flux.fromIterable(steps)
            .concatMap(step -> step.execute(payment)
                .onErrorResume(error ->
                    rollback(payment, steps.indexOf(step), error)
                )
            )
            .then(Mono.just(payment));
    }

    private Mono<Void> rollback(Payment payment, int failedAt, Throwable cause) {
        return Flux.range(0, failedAt)
            .map(i -> steps.get(failedAt - 1 - i))
            .concatMap(step -> step.compensate(payment))
            .then(Mono.error(new SagaRollbackException(cause)));
    }

    public static class SagaRollbackException extends RuntimeException {
        public SagaRollbackException(Throwable cause) {
            super(cause);
        }
    }
}