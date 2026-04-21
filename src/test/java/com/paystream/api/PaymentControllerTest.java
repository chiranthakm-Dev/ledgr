package com.paystream.api;

import com.paystream.api.dto.PaymentRequest;
import com.paystream.api.dto.PaymentResponse;
import com.paystream.domain.Payment;
import com.paystream.repository.PaymentRepository;
import com.paystream.saga.PaymentSaga;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private PaymentSaga paymentSaga;

    @Test
    void createPayment_shouldReturnAccepted() {
        PaymentRequest request = new PaymentRequest(
            4999L,
            "EUR",
            new PaymentRequest.Source("card", "tok_test_visa"),
            "Order #1234",
            null
        );

        Payment payment = new Payment(
            UUID.randomUUID(),
            UUID.randomUUID(),
            4999L,
            "EUR",
            Payment.PaymentStatus.PROCESSING,
            Payment.SagaState.COMPLETED,
            "test-key",
            "tok_test_visa",
            "Order #1234",
            null,
            null,
            java.time.Instant.now(),
            java.time.Instant.now()
        );

        when(paymentRepository.findByIdempotencyKey("test-key")).thenReturn(Mono.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(payment));
        when(paymentSaga.execute(payment)).thenReturn(Mono.just(payment));

        webTestClient.post()
            .uri("/v1/payments")
            .header("Idempotency-Key", "test-key")
            .header("X-Merchant-ID", UUID.randomUUID().toString())
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(PaymentResponse.class);
    }
}