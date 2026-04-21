package com.paystream.outbox;

import com.paystream.domain.OutboxEvent;
import com.paystream.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 100)
    public void relay() {
        outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING)
            .flatMap(event -> kafkaTemplate
                .send("payments.events", event.eventType(), event.payload())
                .then(outboxRepository.save(new OutboxEvent(
                    event.id(),
                    event.eventType(),
                    event.payload(),
                    OutboxEvent.OutboxStatus.PUBLISHED,
                    event.createdAt(),
                    java.time.Instant.now()
                )))
            )
            .subscribe();
    }
}