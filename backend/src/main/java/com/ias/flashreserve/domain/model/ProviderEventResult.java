package com.ias.flashreserve.domain.model;

public record ProviderEventResult(
        String eventId,
        Outcome outcome,
        String reservationId,
        ReservationStatus status
) {
    public enum Outcome {
        APPLIED,
        IGNORED_DUPLICATE,
        IGNORED_STALE_SEQUENCE
    }

    public static ProviderEventResult applied(String eventId, String reservationId, ReservationStatus status) {
        return new ProviderEventResult(eventId, Outcome.APPLIED, reservationId, status);
    }

    public static ProviderEventResult ignored(String eventId, Outcome outcome) {
        return new ProviderEventResult(eventId, outcome, null, null);
    }
}
