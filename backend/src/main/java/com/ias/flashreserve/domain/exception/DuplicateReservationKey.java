package com.ias.flashreserve.domain.exception;

public class DuplicateReservationKey extends DomainException {
    public DuplicateReservationKey(String key) {
        super("DUPLICATE_IDEMPOTENCY_KEY", "Reservation already exists for Idempotency-Key " + key);
    }
}
