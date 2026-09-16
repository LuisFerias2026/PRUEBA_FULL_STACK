package com.ias.flashreserve.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Reservation(
        String id,
        String idempotencyKey,
        String clientId,
        String sku,
        int quantity,
        ReservationStatus status,
        long lastSequence,
        Instant createdAt,
        Instant updatedAt
) {
    public static Reservation pending(String idempotencyKey, String clientId, String sku, int quantity, Instant now) {
        return new Reservation(
                "res_" + UUID.randomUUID(),
                idempotencyKey,
                clientId,
                sku,
                quantity,
                ReservationStatus.PENDING,
                0,
                now,
                now
        );
    }
}
