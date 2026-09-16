package com.ias.flashreserve.domain.model;

import java.time.Instant;

public record ProviderCommand(
        String eventId,
        String reservationId,
        long sequence,
        String status,
        Instant occurredAt
) {
}
