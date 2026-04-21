# 002: Transactional Outbox Pattern

## Status
Accepted

## Context
Need to publish domain events to Kafka atomically with database writes. Naive approach (write DB then publish) risks data inconsistency if the app crashes between operations.

## Decision
Implement transactional outbox pattern:

1. Write business data + outbox event in single DB transaction
2. Separate relay process polls outbox table and publishes to Kafka
3. Mark events as published after successful delivery

## Benefits
- Guaranteed exactly-once delivery (at-least-once with idempotent consumers)
- No distributed transactions
- Reliable event sourcing

## Implementation
- OutboxService writes events transactionally
- OutboxRelay polls with scheduled task
- Optimistic locking prevents duplicate publishes