package com.ias.flashreserve.application;

import com.ias.flashreserve.domain.exception.DomainException;
import com.ias.flashreserve.domain.model.Reservation;
import com.ias.flashreserve.domain.port.in.GetReservationUseCase;
import com.ias.flashreserve.domain.port.out.ReservationStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GetReservationService implements GetReservationUseCase {

    private final ReservationStore reservationStore;

    public GetReservationService(ReservationStore reservationStore) {
        this.reservationStore = reservationStore;
    }

    @Override
    public Mono<Reservation> byId(String id) {
        return reservationStore.findById(id)
                .switchIfEmpty(Mono.error(new DomainException(
                        "RESERVATION_NOT_FOUND",
                        "Reservation " + id + " was not found."
                )));
    }
}
