package com.ias.flashreserve.domain.port.out;

import com.ias.flashreserve.domain.model.Reservation;
import com.ias.flashreserve.domain.model.ReservationStatus;
import com.ias.flashreserve.domain.model.SequenceApplyResult;
import reactor.core.publisher.Mono;

public interface ReservationStore {
    Mono<Reservation> findById(String id);

    Mono<Reservation> findByIdempotencyKey(String idempotencyKey);

    Mono<Reservation> insert(Reservation reservation);

    Mono<Void> deleteById(String id);

    Mono<Void> deleteAll();

    Mono<Long> count();

    Mono<SequenceApplyResult> applyIfNewerSequence(String reservationId, long sequence, ReservationStatus status);
}
