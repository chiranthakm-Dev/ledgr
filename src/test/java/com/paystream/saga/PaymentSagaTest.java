package com.paystream.saga;

import com.paystream.domain.Payment;
import com.paystream.metrics.PaymentMetrics;
import com.paystream.saga.steps.SagaStep;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentSagaTest {

    @Test
    void execute_shouldRollbackOnFailure() {
        PaymentMetrics metrics = mock(PaymentMetrics.class);
        PaymentSaga saga = new PaymentSaga(metrics);

        SagaStep failingStep = mock(SagaStep.class);
        SagaStep compensatingStep = mock(SagaStep.class);

        when(failingStep.execute(any(Payment.class))).thenReturn(Mono.error(new RuntimeException("Step failed")));
        when(compensatingStep.compensate(any(Payment.class))).thenReturn(Mono.empty());

        PaymentSaga spySaga = Mockito.spy(saga);
        Mockito.doReturn(List.of(failingStep, compensatingStep)).when(spySaga).getSteps();

        Payment payment = new Payment(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1000L,
            "EUR",
            Payment.PaymentStatus.CREATED,
            Payment.SagaState.STARTED,
            "test",
            "token",
            "desc",
            null,
            null,
            java.time.Instant.now(),
            java.time.Instant.now()
        );

        StepVerifier.create(spySaga.execute(payment))
            .expectError(PaymentSaga.SagaRollbackException.class)
            .verify();
    }
}