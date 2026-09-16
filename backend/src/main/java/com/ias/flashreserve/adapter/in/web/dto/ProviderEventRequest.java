package com.ias.flashreserve.adapter.in.web.dto;

import java.time.Instant;

public record ProviderEventRequest(
        String eventId,
        String reservationId,
        Long sequence,
        String status,
        Instant occurredAt
) {
}
