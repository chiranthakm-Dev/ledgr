package com.paystream.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("payment_audit_log")
public record AuditEntry(
    @Id UUID id,
    UUID paymentId,
    String fromState,
    String toState,
    String actor,
    String reason,
    String metadata, // JSON
    Instant recordedAt
) {
}