package com.paystream.saga.steps;

import com.paystream.domain.Payment;
import com.paystream.saga.SagaStep;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NotifyMerchantStep implements SagaStep {

    private final WebClient webClient = WebClient.create();

    @Override
    public Mono<Void> execute(Payment payment) {
        // TODO: POST to merchant webhook URL
        // For now, simulate
        return Mono.empty();
    }

    @Override
    public Mono<Void> compensate(Payment payment) {
        // TODO: Send failure notification
        return Mono.empty();
    }
}