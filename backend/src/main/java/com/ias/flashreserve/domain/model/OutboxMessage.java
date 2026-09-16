package com.ias.flashreserve.domain.model;

import java.time.Instant;

public record OutboxMessage(
        String id,
        String type,
        String aggregateId,
        String payload,
        OutboxStatus status,
        int attempts,
        Instant createdAt,
        Instant processedAt,
        String lastError
) {
}
