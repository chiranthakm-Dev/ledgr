package com.paystream.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paystream.domain.OutboxEvent;
import com.paystream.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Mono<Void> publish(String eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                eventType,
                jsonPayload,
                OutboxEvent.OutboxStatus.PENDING,
                java.time.Instant.now(),
                null
            );
            return outboxRepository.save(event).then();
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}