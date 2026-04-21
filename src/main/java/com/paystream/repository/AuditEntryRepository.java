package com.paystream.repository;

import com.paystream.domain.AuditEntry;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface AuditEntryRepository extends ReactiveCrudRepository<AuditEntry, UUID> {
}