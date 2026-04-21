package com.paystream.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("payments")
public record Payment(
    @Id UUID id,
    UUID merchantId,
    long amount, // minor units
    String currency,
    PaymentStatus status,
    SagaState sagaState,
    String idempotencyKey,
    String sourceToken,
    String description,
    String metadata, // JSON
    String failureReason,
    Instant createdAt,
    Instant updatedAt
) {
    public enum PaymentStatus {
        CREATED, VALIDATING, RESERVING, COMPLETED, FAILED, REFUNDED
    }

    public enum SagaState {
        STARTED, VALIDATING, RESERVING, NOTIFYING, COMPLETED, ROLLBACK
    }
}