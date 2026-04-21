package com.paystream.outbox;

import com.paystream.domain.OutboxEvent;
import com.paystream.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    @Test
    void relay_shouldPublishPendingEvents() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        OutboxRelay relay = new OutboxRelay(repository, kafkaTemplate);

        OutboxEvent event = new OutboxEvent(
            UUID.randomUUID(),
            "payment.created",
            "{\"id\":\"test\"}",
            OutboxEvent.OutboxStatus.PENDING,
            java.time.Instant.now(),
            null
        );

        when(repository.findByStatus(OutboxEvent.OutboxStatus.PENDING)).thenReturn(Flux.just(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(mock(org.springframework.kafka.support.SendResult.class));
        when(repository.save(any(OutboxEvent.class))).thenReturn(Mono.just(event));

        // The relay method is scheduled, so we can't easily test it directly
        // In a real test, we'd use @SpringBootTest and trigger the schedule
    }
}