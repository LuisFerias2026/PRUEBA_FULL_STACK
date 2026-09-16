package com.ias.flashreserve.domain.port.in;

import com.ias.flashreserve.domain.model.Reservation;
import reactor.core.publisher.Mono;

public interface CreateReservationUseCase {
    Mono<Reservation> create(String idempotencyKey, String clientId, String sku, int quantity);
}
