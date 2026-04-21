package com.paystream.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @Min(1) long amount,
    @NotBlank String currency,
    @NotNull Source source,
    String description,
    Object metadata
) {
    public record Source(
        @NotBlank String type,
        @NotBlank String token
    ) {}
}