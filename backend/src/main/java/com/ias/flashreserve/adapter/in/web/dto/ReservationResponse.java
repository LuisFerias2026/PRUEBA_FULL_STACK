package com.ias.flashreserve.adapter.in.web.dto;

import com.ias.flashreserve.domain.model.Reservation;
import com.ias.flashreserve.domain.model.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        String id,
        String clientId,
        String sku,
        int quantity,
        ReservationStatus status,
        long lastSequence,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.id(),
                reservation.clientId(),
                reservation.sku(),
                reservation.quantity(),
                reservation.status(),
                reservation.lastSequence(),
                reservation.createdAt(),
                reservation.updatedAt()
        );
    }
}
