CREATE TABLE payments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id       UUID NOT NULL,
    amount            BIGINT NOT NULL,
    currency          CHAR(3) NOT NULL,
    status            TEXT NOT NULL,
    saga_state        TEXT NOT NULL,
    idempotency_key   TEXT UNIQUE,
    source_token      TEXT NOT NULL,
    description       TEXT,
    metadata          JSONB,
    failure_reason    TEXT,
    created_at        TIMESTAMPTZ DEFAULT now(),
    updated_at        TIMESTAMPTZ DEFAULT now()
);

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

CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type    TEXT NOT NULL,
    payload       JSONB NOT NULL,
    status        TEXT NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ DEFAULT now(),
    published_at  TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON outbox_events (created_at)
WHERE status = 'PENDING';