package com.paystream.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final Counter paymentsTotal;
    private final Timer sagaDuration;
    private final Counter outboxLag;

    public PaymentMetrics(MeterRegistry registry) {
        this.paymentsTotal = Counter.builder("paystream_payments_total")
            .description("Total number of payments")
            .tags("status", "currency")
            .register(registry);

        this.sagaDuration = Timer.builder("paystream_saga_duration_seconds")
            .description("Saga execution duration")
            .register(registry);

        this.outboxLag = Counter.builder("paystream_outbox_lag_seconds")
            .description("Outbox processing lag")
            .register(registry);
    }

    public void incrementPayments(String status, String currency) {
        paymentsTotal.increment();
    }

    public Timer.Sample startSagaTimer() {
        return Timer.start();
    }

    public void recordSagaDuration(Timer.Sample sample) {
        sample.stop(sagaDuration);
    }

    public void incrementOutboxLag() {
        outboxLag.increment();
    }
}