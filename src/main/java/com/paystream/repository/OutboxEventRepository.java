package com.paystream.repository;

import com.paystream.domain.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    Flux<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);
    Flux<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxEvent.OutboxStatus status, Instant before);
}