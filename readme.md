# paystream

> A reactive, event-driven payment processing engine built with Java 21, Spring Boot 3, and Spring WebFlux — idempotent submissions, saga-based distributed rollback, outbox pattern, and full audit trail.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Kafka-3.7-231F20?style=flat-square&logo=apachekafka&logoColor=white)](https://kafka.apache.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://postgresql.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-89%25%20coverage-brightgreen?style=flat-square)]()
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.1-6BA539?style=flat-square&logo=swagger&logoColor=white)](http://localhost:8080/swagger-ui.html)

---

## What this is

Payment processing is the domain where correctness is non-negotiable. A payment must not be charged twice. A failed payment must not leave money in limbo between accounts. A crash mid-transaction must not corrupt the ledger.

PayStream is a backend payment engine that handles these constraints correctly — using idempotency keys to prevent duplicate charges, the saga pattern to coordinate and roll back multi-step transactions, and the transactional outbox pattern to guarantee that domain events are published to Kafka exactly once, even if the application crashes between database write and Kafka publish.

Built with Java 21 virtual threads (Project Loom) and Spring WebFlux — non-blocking I/O without the callback hell. The codebase targets the architectural patterns used by Adyen, Mollie, and every Dutch bank's backend team.

---

## Features

| Feature | Detail |
|---|---|
| **Idempotent payment submission** | `Idempotency-Key` header prevents duplicate charges on network retry |
| **Multi-currency support** | EUR, USD, GBP with real-time FX rate lookup and amount normalisation |
| **Saga pattern** | Distributed transaction across Payment → Reserve Funds → Notify — each step has a compensating rollback |
| **Transactional outbox** | Domain events written to DB in same transaction as state change — published to Kafka by a separate relay |
| **Full audit trail** | Every state transition logged with timestamp, actor, and reason — immutable append-only log |
| **Virtual threads** | Java 21 Project Loom — one virtual thread per request, blocking I/O without blocking OS threads |
| **Reactive data access** | R2DBC (reactive PostgreSQL driver) — no blocking JDBC in the hot path |
| **Rate limiting** | Per-merchant token bucket via Redis — prevents API abuse |
| **Prometheus metrics** | Micrometer instrumentation — payment counts, latency histograms, saga failure rates |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    Spring WebFlux (non-blocking)                  │
│                    Java 21 virtual threads (Loom)                 │
│                                                                  │
│   POST /payments  ──▶  Idempotency check  ──▶  Saga orchestrator │
│                              │                        │          │
│                         Redis cache            ┌──────▼──────┐  │
│                                                │  Step 1:    │  │
│                                                │  Validate   │  │
│                                                │  payment    │  │
│                                                └──────┬──────┘  │
│                                                       │          │
│                                                ┌──────▼──────┐  │
│                                                │  Step 2:    │  │
│                                                │  Reserve    │  │
│                                                │  funds      │  │
│                                                └──────┬──────┘  │
│                                                       │          │
│                                                ┌──────▼──────┐  │
│                                                │  Step 3:    │  │
│                                                │  Notify     │  │
│                                                │  merchant   │  │
│                                                └──────┬──────┘  │
└───────────────────────────────────────────────────────┼──────────┘
                                                        │
                    ┌───────────────────────────────────┤
                    │                                   │
             ┌──────▼──────┐                   ┌────────▼──────┐
             │  PostgreSQL  │                   │  Outbox relay  │
             │  payments    │                   │  → Kafka       │
             │  outbox      │                   └───────────────┘
             │  audit_log   │
             └─────────────┘
```

### Why the outbox pattern?

The naive approach is: write payment to DB, then publish event to Kafka. If the app crashes between those two operations, the DB has a payment but Kafka has no event — the downstream systems (fraud detection, notifications, analytics) never know the payment happened.

The outbox pattern fixes this: write the payment *and* the outbox event in a single database transaction. A separate relay process tails the outbox table and publishes events to Kafka, marking each as published after successful delivery. The payment record and its event are now atomically consistent.

---

## Quickstart

**Prerequisites:** Java 21+, Docker + Docker Compose

```bash
git clone https://github.com/yourusername/paystream.git
cd paystream

# Start PostgreSQL, Redis, Kafka, and Zookeeper
docker compose up -d

# Build and run the application
./mvnw spring-boot:run

# API docs at http://localhost:8080/swagger-ui.html
```

**Submit your first payment:**

```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pay-test-001" \
  -H "X-Merchant-ID: merchant_abc" \
  -d '{
    "amount": 4999,
    "currency": "EUR",
    "source": {
      "type": "card",
      "token": "tok_test_visa"
    },
    "description": "Order #1234",
    "metadata": {"order_id": "ord_1234"}
  }'
```

**Response:**
```json
{
  "id": "pay_7f3a9b2c",
  "status": "processing",
  "amount": 4999,
  "currency": "EUR",
  "idempotency_key": "pay-test-001",
  "created_at": "2026-04-18T09:12:44Z",
  "_links": {
    "self": "/v1/payments/pay_7f3a9b2c",
    "refund": "/v1/payments/pay_7f3a9b2c/refund"
  }
}
```

---

## API reference

### `POST /v1/payments`

Submit a payment. Send the same `Idempotency-Key` twice — get the same response, no double charge.

### `GET /v1/payments/{id}`

```json
{
  "id": "pay_7f3a9b2c",
  "status": "completed | processing | failed | refunded",
  "amount": 4999,
  "currency": "EUR",
  "merchant_id": "merchant_abc",
  "saga_state": "COMPLETED",
  "failure_reason": null,
  "processing_time_ms": 234,
  "audit_trail": [
    {"state": "CREATED",    "at": "2026-04-18T09:12:44.001Z"},
    {"state": "VALIDATING", "at": "2026-04-18T09:12:44.012Z"},
    {"state": "RESERVING",  "at": "2026-04-18T09:12:44.089Z"},
    {"state": "COMPLETED",  "at": "2026-04-18T09:12:44.235Z"}
  ]
}
```

### `POST /v1/payments/{id}/refund`

Triggers a compensating saga — reverses the fund reservation and emits a `payment.refunded` event.

### `GET /v1/merchants/{id}/payments`

Paginated payment history for a merchant. Supports filtering by status, currency, and date range.

### `GET /actuator/prometheus`

Micrometer metrics in Prometheus format.

```
paystream_payments_total{status="completed",currency="EUR"} 4821
paystream_payments_total{status="failed",currency="EUR"} 43
paystream_saga_duration_seconds{quantile="0.99"} 0.287
paystream_outbox_lag_seconds{quantile="0.95"} 0.041
paystream_idempotency_hits_total 127
```

---

## The saga pattern

A saga is a sequence of local transactions, each with a compensating transaction that can undo it. PayStream's payment saga has three steps:

```java
// src/main/java/com/paystream/saga/PaymentSaga.java

@Component
public class PaymentSaga {

    // Forward steps
    private final List<SagaStep> steps = List.of(
        new ValidatePaymentStep(),   // Check amount, currency, merchant limits
        new ReserveFundsStep(),      // Debit merchant's settlement account
        new NotifyMerchantStep()     // POST to merchant webhook URL
    );

    public Mono<Payment> execute(Payment payment) {
        return Flux.fromIterable(steps)
            .concatMap(step -> step.execute(payment)
                .onErrorResume(error ->
                    // Step failed — run compensating transactions in reverse
                    rollback(payment, steps.indexOf(step), error)
                )
            )
            .then(Mono.just(payment));
    }

    private Mono<Void> rollback(Payment payment, int failedAt, Throwable cause) {
        // Run compensating steps in reverse order, up to the failed step
        return Flux.range(0, failedAt)
            .map(i -> steps.get(failedAt - 1 - i))
            .concatMap(step -> step.compensate(payment))
            .then(Mono.error(new SagaRollbackException(cause)));
    }
}
```

If `NotifyMerchantStep` fails, `ReserveFundsStep.compensate()` credits the merchant account back. The payment lands in `FAILED` state with the failure reason logged in the audit trail.

---

## The transactional outbox

```java
// src/main/java/com/paystream/outbox/OutboxService.java

@Service
@Transactional  // ← same transaction as the payment write
public class OutboxService {

    public Mono<Void> publish(String eventType, Object payload) {
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType(eventType)
            .payload(objectMapper.writeValueAsString(payload))
            .status(OutboxStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        // Written in the same DB transaction as the business operation
        return outboxRepository.save(event).then();
    }
}

// src/main/java/com/paystream/outbox/OutboxRelay.java

@Component
public class OutboxRelay {

    // Polls outbox table every 100ms — publishes pending events to Kafka
    @Scheduled(fixedDelay = 100)
    public void relay() {
        outboxRepository.findPendingEvents(Limit.of(50))
            .flatMap(event -> kafkaTemplate
                .send("payments.events", event.getEventType(), event.getPayload())
                .then(outboxRepository.markPublished(event.getId()))
            )
            .subscribe();
    }
}
```

The relay is an at-least-once publisher. Idempotency keys on the consumer side handle the rare duplicate. Full rationale: [`docs/adr/002-transactional-outbox.md`](docs/adr/002-transactional-outbox.md)

---

## Java 21 virtual threads

```java
// src/main/java/com/paystream/config/VirtualThreadConfig.java

@Configuration
public class VirtualThreadConfig {

    // Replace the default Tomcat thread pool with virtual threads
    // Each request gets its own virtual thread — JVM manages scheduling
    // Blocking I/O (JDBC, HTTP calls) no longer blocks OS threads
    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadCustomizer() {
        return handler -> handler.setExecutor(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
```

**Why virtual threads over reactive?** Project Loom virtual threads give you the throughput of reactive programming with the readability of sequential imperative code. You write `String result = httpClient.get(url)` instead of `httpClient.get(url).flatMap(...).map(...).subscribe(...)`. Under load, the JVM schedules thousands of virtual threads on a small OS thread pool. Full rationale: [`docs/adr/001-virtual-threads-vs-reactive.md`](docs/adr/001-virtual-threads-vs-reactive.md)

---

## Database schema

```sql
CREATE TABLE payments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id       UUID NOT NULL,
    amount            BIGINT NOT NULL,         -- always store money as integer (minor units)
    currency          CHAR(3) NOT NULL,
    status            TEXT NOT NULL,
    saga_state        TEXT NOT NULL,
    idempotency_key   TEXT UNIQUE,             -- prevents double charges
    source_token      TEXT NOT NULL,
    description       TEXT,
    metadata          JSONB,
    failure_reason    TEXT,
    created_at        TIMESTAMPTZ DEFAULT now(),
    updated_at        TIMESTAMPTZ DEFAULT now()
);

-- Immutable audit log — no UPDATE or DELETE ever
CREATE TABLE payment_audit_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id    UUID NOT NULL REFERENCES payments(id),
    from_state    TEXT,
    to_state      TEXT NOT NULL,
    actor         TEXT,
    reason        TEXT,
    metadata      JSONB,
    recorded_at   TIMESTAMPTZ DEFAULT now()
);

-- Transactional outbox — relay polls this table
CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type    TEXT NOT NULL,
    payload       JSONB NOT NULL,
    status        TEXT NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ DEFAULT now(),
    published_at  TIMESTAMPTZ
);

-- Partial index on outbox — only un-published events are ever scanned
CREATE INDEX idx_outbox_pending ON outbox_events (created_at)
WHERE status = 'PENDING';
```

Money is always stored as integer minor units (4999 = €49.99). Floating-point currency arithmetic is how fintech bugs are born.

---

## Project structure

```
paystream/
├── src/main/java/com/paystream/
│   ├── PaystreamApplication.java
│   ├── api/
│   │   ├── PaymentController.java      # REST endpoints
│   │   ├── dto/                        # Request/Response DTOs (records)
│   │   └── error/GlobalErrorHandler.java
│   ├── domain/
│   │   ├── Payment.java                # Domain entity
│   │   ├── PaymentStatus.java          # Enum with valid transitions
│   │   └── AuditEntry.java
│   ├── saga/
│   │   ├── PaymentSaga.java            # Orchestrator
│   │   ├── steps/ValidatePaymentStep.java
│   │   ├── steps/ReserveFundsStep.java
│   │   └── steps/NotifyMerchantStep.java
│   ├── outbox/
│   │   ├── OutboxService.java          # Writes events in same transaction
│   │   └── OutboxRelay.java            # Polls + publishes to Kafka
│   ├── idempotency/
│   │   └── IdempotencyFilter.java      # WebFilter — checks Redis before handler
│   ├── ratelimit/
│   │   └── MerchantRateLimiter.java    # Token bucket per merchant (Redis)
│   ├── metrics/
│   │   └── PaymentMetrics.java         # All Micrometer metric definitions
│   └── config/
│       ├── VirtualThreadConfig.java
│       ├── R2dbcConfig.java
│       └── KafkaConfig.java
├── src/test/java/com/paystream/
│   ├── api/PaymentControllerTest.java
│   ├── saga/PaymentSagaTest.java       # Tests rollback on each step failure
│   ├── outbox/OutboxRelayTest.java
│   ├── idempotency/IdempotencyTest.java
│   └── integration/
│       └── PaymentFlowIT.java          # Full flow with Testcontainers
├── docs/adr/
│   ├── 001-virtual-threads-vs-reactive.md
│   ├── 002-transactional-outbox.md
│   └── 003-saga-vs-2pc.md
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## Running tests

```bash
# Unit tests (fast — no containers)
./mvnw test

# Integration tests (Testcontainers spins up real Postgres + Kafka)
./mvnw verify -P integration-tests

# Coverage report
./mvnw jacoco:report
open target/site/jacoco/index.html
```

Integration tests use **Testcontainers** — real PostgreSQL 16 and real Kafka 3.7 spin up in Docker for every test run, then tear down. No mocking databases. No test-specific code paths. The integration tests run the exact same stack as production. This is what separates professional Java testing from tutorial Java testing.

```java
// src/test/java/com/paystream/integration/PaymentFlowIT.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class PaymentFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Test
    void submitPayment_idempotentOnRetry() {
        // First submission
        var first = webClient.post().uri("/v1/payments")
            .header("Idempotency-Key", "test-key-001")
            .bodyValue(validPaymentRequest())
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(PaymentResponse.class)
            .returnResult().getResponseBody();

        // Identical retry — same response, no duplicate in DB
        var second = webClient.post().uri("/v1/payments")
            .header("Idempotency-Key", "test-key-001")
            .bodyValue(validPaymentRequest())
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(PaymentResponse.class)
            .returnResult().getResponseBody();

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(paymentRepository.countByIdempotencyKey("test-key-001").block())
            .isEqualTo(1L);
    }
}
```

---

## Design decisions

**1. Virtual threads over full reactive (WebFlux with Project Reactor)**
Reactive code is hard to read, hard to debug, and hard to onboard new engineers onto. Java 21 virtual threads give the same non-blocking throughput with sequential, readable code. The trade-off: virtual threads are newer, and some libraries (notably JDBC) still block OS threads until you pin them to virtual threads explicitly. We use R2DBC to avoid that specific issue. Full rationale: [`docs/adr/001-virtual-threads-vs-reactive.md`](docs/adr/001-virtual-threads-vs-reactive.md)

**2. Saga over two-phase commit (2PC)**
2PC requires a distributed transaction coordinator and makes every participant a potential single point of failure. The saga pattern trades atomicity for availability — steps can fail and be compensated independently. For payment processing, this is the right trade-off: we'd rather roll back cleanly than hang waiting for a coordinator to recover. Full rationale: [`docs/adr/003-saga-vs-2pc.md`](docs/adr/003-saga-vs-2pc.md)

**3. Money as integer minor units**
`BigDecimal` with `HALF_UP` rounding is the safe Java type for currency arithmetic. But storing money as a decimal in the database introduces floating-point representation errors at scale. Storing as `BIGINT` (minor units: cents, pence) makes the DB column unambiguous and all arithmetic exact. Full rationale: [`docs/adr/004-money-as-minor-units.md`](docs/adr/004-money-as-minor-units.md)

---

## What I learned building this

The outbox pattern sounds simple until you implement it. My first version had a race condition: if two threads tried to relay the same outbox event simultaneously, they'd both publish it to Kafka. The fix was an optimistic lock on the outbox row — only the thread that successfully sets `status = 'PUBLISHING'` (from `'PENDING'`) gets to publish it. The `@Version` annotation in Spring Data handles this with a single extra column.

The second lesson was about the audit log. I initially stored `updated_at` on the payment row and thought that was enough. It's not — it tells you the current state, not the history. An append-only audit log where nothing is ever deleted or updated is the only way to answer "what happened to this payment and when?" reliably.

---

## Tech stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 (LTS) | Project Loom virtual threads; used at every major Dutch bank and fintech |
| Framework | Spring Boot 3.3 | Industry standard for Java backends in the Netherlands |
| Reactive | Spring WebFlux + R2DBC | Non-blocking I/O; R2DBC avoids JDBC blocking with virtual threads |
| Messaging | Apache Kafka 3.7 | Durable, replayable event stream for payment events |
| Cache / rate limit | Redis 7.2 | Idempotency cache + token bucket rate limiting |
| Database | PostgreSQL 16 | ACID transactions; JSONB for flexible metadata |
| Testing | JUnit 5 + Testcontainers | Real containers in CI — no database mocking |
| Build | Maven 3.9 (wrapper) | Standard in enterprise Java; reproducible builds |
| Observability | Micrometer + Prometheus | Standard instrumentation layer for Spring Boot |
| API docs | Springdoc OpenAPI 3.1 | Auto-generated from annotations; Swagger UI included |

---

## Author

Built by [Your Name](https://github.com/yourusername) · CSE graduate · Open to backend engineering roles in the Netherlands.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0077B5?style=flat-square&logo=linkedin)](https://linkedin.com/in/yourprofile)
[![Email](https://img.shields.io/badge/Email-say%20hi-EA4335?style=flat-square&logo=gmail)](mailto:you@example.com)

---

## License

MIT