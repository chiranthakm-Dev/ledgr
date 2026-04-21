package com.paystream.api.dto;

import com.paystream.domain.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PaymentResponse(
    String id,
    String status,
    long amount,
    String currency,
    String idempotency_key,
    Instant created_at,
    Map<String, String> _links
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.id().toString(),
            payment.status().name().toLowerCase(),
            payment.amount(),
            payment.currency(),
            payment.idempotencyKey(),
            payment.createdAt(),
            Map.of(
                "self", "/v1/payments/" + payment.id(),
                "refund", "/v1/payments/" + payment.id() + "/refund"
            )
        );
    }
}