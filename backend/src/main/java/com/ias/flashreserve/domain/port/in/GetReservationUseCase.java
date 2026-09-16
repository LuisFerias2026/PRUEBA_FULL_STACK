package com.ias.flashreserve.domain.port.in;

import com.ias.flashreserve.domain.model.Reservation;
import reactor.core.publisher.Mono;

public interface GetReservationUseCase {
    Mono<Reservation> byId(String id);
}
