package com.paystream.integration;

import com.paystream.api.dto.PaymentRequest;
import com.paystream.api.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("paystream")
        .withUsername("paystream")
        .withPassword("paystream");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @Test
    void submitPayment_idempotentOnRetry(WebTestClient webTestClient) {
        PaymentRequest request = new PaymentRequest(
            4999L,
            "EUR",
            new PaymentRequest.Source("card", "tok_test_visa"),
            "Order #1234",
            null
        );

        // First submission
        PaymentResponse first = webTestClient.post().uri("/v1/payments")
            .header("Idempotency-Key", "test-key-001")
            .header("X-Merchant-ID", "merchant_abc")
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(PaymentResponse.class)
            .returnResult().getResponseBody();

        // Identical retry — same response, no duplicate in DB
        PaymentResponse second = webTestClient.post().uri("/v1/payments")
            .header("Idempotency-Key", "test-key-001")
            .header("X-Merchant-ID", "merchant_abc")
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(PaymentResponse.class)
            .returnResult().getResponseBody();

        assert first.id().equals(second.id());
    }
}