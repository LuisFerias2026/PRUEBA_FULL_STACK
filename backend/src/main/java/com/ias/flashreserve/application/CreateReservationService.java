package com.ias.flashreserve.application;

import com.ias.flashreserve.domain.exception.DomainException;
import com.ias.flashreserve.domain.exception.DuplicateReservationKey;
import com.ias.flashreserve.domain.model.Reservation;
import com.ias.flashreserve.domain.port.in.CreateReservationUseCase;
import com.ias.flashreserve.domain.port.out.InventoryStore;
import com.ias.flashreserve.domain.port.out.OutboxStore;
import com.ias.flashreserve.domain.port.out.ReservationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class CreateReservationService implements CreateReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateReservationService.class);

    private final ReservationStore reservationStore;
    private final InventoryStore inventoryStore;
    private final OutboxStore outboxStore;

    public CreateReservationService(
            ReservationStore reservationStore,
            InventoryStore inventoryStore,
            OutboxStore outboxStore
    ) {
        this.reservationStore = reservationStore;
        this.inventoryStore = inventoryStore;
        this.outboxStore = outboxStore;
    }

    @Override
    public Mono<Reservation> create(String idempotencyKey, String clientId, String sku, int quantity) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new DomainException(
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "Header Idempotency-Key is required to avoid duplicate reservations."
            ));
        }
        String key = idempotencyKey.trim();
        return reservationStore.findByIdempotencyKey(key)
                .doOnNext(existing -> log.info(
                        "reservation_idempotent_hit reservationId={} idempotencyKey={}",
                        existing.id(),
                        key
                ))
                .switchIfEmpty(Mono.defer(() -> createNew(key, clientId, sku, quantity)));
    }

    private Mono<Reservation> createNew(String idempotencyKey, String clientId, String sku, int quantity) {
        Reservation reservation = Reservation.pending(idempotencyKey, clientId, sku, quantity, Instant.now());
        return inventoryStore.findBySku(sku)
                .switchIfEmpty(Mono.error(new DomainException("SKU_NOT_FOUND", "SKU " + sku + " does not exist.")))
                .flatMap(ignored -> reservationStore.insert(reservation)
                        .flatMap(this::commitStockOrRelease)
                        .onErrorResume(DuplicateReservationKey.class, error ->
                                reservationStore.findByIdempotencyKey(idempotencyKey)
                                        .switchIfEmpty(Mono.error(error))));
    }

    private Mono<Reservation> commitStockOrRelease(Reservation saved) {
        return inventoryStore.decrementIfAvailable(saved.sku(), saved.quantity())
                .flatMap(decremented -> {
                    if (!decremented) {
                        return releaseReservation(saved);
                    }
                    return enqueueCreated(saved).thenReturn(saved);
                });
    }

    private Mono<Reservation> releaseReservation(Reservation saved) {
        log.info("reservation_rejected_no_stock reservationId={} sku={} quantity={}", saved.id(), saved.sku(), saved.quantity());
        return reservationStore.deleteById(saved.id())
                .then(Mono.error(new DomainException(
                        "INSUFFICIENT_STOCK",
                        "Not enough inventory available for SKU " + saved.sku() + "."
                )));
    }

    private Mono<Void> enqueueCreated(Reservation saved) {
        String payload = "{\"reservationId\":\"" + saved.id()
                + "\",\"sku\":\"" + saved.sku()
                + "\",\"quantity\":" + saved.quantity()
                + ",\"status\":\"" + saved.status() + "\"}";
        log.info(
                "reservation_created reservationId={} sku={} quantity={} status={}",
                saved.id(),
                saved.sku(),
                saved.quantity(),
                saved.status()
        );
        return outboxStore.enqueue("RESERVATION_CREATED", saved.id(), payload);
    }
}
