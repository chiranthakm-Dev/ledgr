package com.paystream.api;

import com.paystream.api.dto.PaymentRequest;
import com.paystream.api.dto.PaymentResponse;
import com.paystream.domain.Payment;
import com.paystream.repository.PaymentRepository;
import com.paystream.saga.PaymentSaga;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentSaga paymentSaga;

    public PaymentController(PaymentRepository paymentRepository, PaymentSaga paymentSaga) {
        this.paymentRepository = paymentRepository;
        this.paymentSaga = paymentSaga;
    }

    @PostMapping
    public Mono<ResponseEntity<PaymentResponse>> createPayment(
        @RequestBody PaymentRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestHeader("X-Merchant-ID") String merchantId
    ) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
            .map(PaymentResponse::from)
            .map(ResponseEntity::ok)
            .switchIfEmpty(createNewPayment(request, idempotencyKey, merchantId));
    }

    private Mono<ResponseEntity<PaymentResponse>> createNewPayment(
        PaymentRequest request, String idempotencyKey, String merchantId
    ) {
        Payment payment = new Payment(
            UUID.randomUUID(),
            UUID.fromString(merchantId),
            request.amount(),
            request.currency(),
            Payment.PaymentStatus.CREATED,
            Payment.SagaState.STARTED,
            idempotencyKey,
            request.source().token(),
            request.description(),
            null, // metadata
            null, // failureReason
            java.time.Instant.now(),
            java.time.Instant.now()
        );

        return paymentRepository.save(payment)
            .flatMap(saved -> paymentSaga.execute(saved))
            .map(PaymentResponse::from)
            .map(ResponseEntity::accepted);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentResponse>> getPayment(@PathVariable UUID id) {
        return paymentRepository.findById(id)
            .map(PaymentResponse::from)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}