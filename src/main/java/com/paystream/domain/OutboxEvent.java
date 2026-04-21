package com.paystream.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("outbox_events")
public record OutboxEvent(
    @Id UUID id,
    String eventType,
    String payload, // JSON
    OutboxStatus status,
    Instant createdAt,
    Instant publishedAt
) {
    public enum OutboxStatus {
        PENDING, PUBLISHING, PUBLISHED
    }
}