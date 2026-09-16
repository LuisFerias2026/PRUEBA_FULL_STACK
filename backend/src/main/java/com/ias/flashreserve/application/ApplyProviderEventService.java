package com.ias.flashreserve.application;

import com.ias.flashreserve.domain.exception.DomainException;
import com.ias.flashreserve.domain.model.ProviderCommand;
import com.ias.flashreserve.domain.model.ProviderEventResult;
import com.ias.flashreserve.domain.model.ReservationStatus;
import com.ias.flashreserve.domain.model.SequenceApplyResult;
import com.ias.flashreserve.domain.port.in.ApplyProviderEventUseCase;
import com.ias.flashreserve.domain.port.out.InventoryStore;
import com.ias.flashreserve.domain.port.out.OutboxStore;
import com.ias.flashreserve.domain.port.out.ProcessedEventStore;
import com.ias.flashreserve.domain.port.out.ReservationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ApplyProviderEventService implements ApplyProviderEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyProviderEventService.class);

    private final ReservationStore reservationStore;
    private final ProcessedEventStore processedEventStore;
    private final OutboxStore outboxStore;
    private final InventoryStore inventoryStore;

    public ApplyProviderEventService(
            ReservationStore reservationStore,
            ProcessedEventStore processedEventStore,
            OutboxStore outboxStore,
            InventoryStore inventoryStore
    ) {
        this.reservationStore = reservationStore;
        this.processedEventStore = processedEventStore;
        this.outboxStore = outboxStore;
        this.inventoryStore = inventoryStore;
    }

    @Override
    public Mono<ProviderEventResult> apply(ProviderCommand command) {
        return validate(command)
                .then(reservationStore.findById(command.reservationId())
                        .switchIfEmpty(Mono.error(new DomainException(
                                "RESERVATION_NOT_FOUND",
                                "Reservation " + command.reservationId() + " was not found."
                        )))
                        .then(ingest(command)));
    }

    private Mono<Void> validate(ProviderCommand command) {
        if (command.eventId() == null || command.eventId().isBlank()) {
            return Mono.error(new DomainException("INVALID_EVENT", "eventId is required."));
        }
        if (command.reservationId() == null || command.reservationId().isBlank()) {
            return Mono.error(new DomainException("INVALID_EVENT", "reservationId is required."));
        }
        if (command.sequence() < 1) {
            return Mono.error(new DomainException("INVALID_EVENT", "sequence must be a positive number."));
        }
        if (parseStatus(command.status()) == null) {
            return Mono.error(new DomainException("INVALID_EVENT", "status must be CONFIRMED or REJECTED."));
        }
        if (command.occurredAt() == null) {
            return Mono.error(new DomainException("INVALID_EVENT", "occurredAt is required."));
        }
        return Mono.empty();
    }

    private Mono<ProviderEventResult> ingest(ProviderCommand command) {
        return processedEventStore.tryMarkProcessed(command.eventId(), command.reservationId(), command.sequence())
                .flatMap(firstTime -> {
                    if (!firstTime) {
                        log.info("provider_event_duplicate eventId={} reservationId={}", command.eventId(), command.reservationId());
                        return Mono.just(ProviderEventResult.ignored(command.eventId(), ProviderEventResult.Outcome.IGNORED_DUPLICATE));
                    }
                    return applyIfNewer(command);
                });
    }

    private Mono<ProviderEventResult> applyIfNewer(ProviderCommand command) {
        ReservationStatus newStatus = parseStatus(command.status());
        return reservationStore.applyIfNewerSequence(command.reservationId(), command.sequence(), newStatus)
                .flatMap(result -> afterApplied(command, result))
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.info(
                            "provider_event_stale eventId={} reservationId={} sequence={}",
                            command.eventId(),
                            command.reservationId(),
                            command.sequence()
                    );
                    return ProviderEventResult.ignored(command.eventId(), ProviderEventResult.Outcome.IGNORED_STALE_SEQUENCE);
                }));
    }

    private Mono<ProviderEventResult> afterApplied(ProviderCommand command, SequenceApplyResult result) {
        log.info(
                "provider_event_applied eventId={} reservationId={} sequence={} status={}",
                command.eventId(),
                result.current().id(),
                result.current().lastSequence(),
                result.current().status()
        );
        return releaseStockIfRejected(result)
                .then(enqueueApplied(command, result))
                .thenReturn(ProviderEventResult.applied(
                        command.eventId(),
                        result.current().id(),
                        result.current().status()
                ));
    }

    private Mono<Void> releaseStockIfRejected(SequenceApplyResult result) {
        if (result.current().status() != ReservationStatus.REJECTED
                || result.previous().status() == ReservationStatus.REJECTED) {
            return Mono.empty();
        }
        log.info(
                "inventory_released reservationId={} sku={} quantity={} fromStatus={}",
                result.current().id(),
                result.current().sku(),
                result.current().quantity(),
                result.previous().status()
        );
        return inventoryStore.increment(result.current().sku(), result.current().quantity());
    }

    private Mono<Void> enqueueApplied(ProviderCommand command, SequenceApplyResult result) {
        String payload = "{\"eventId\":\"" + command.eventId()
                + "\",\"reservationId\":\"" + result.current().id()
                + "\",\"status\":\"" + result.current().status() + "\"}";
        return outboxStore.enqueue("PROVIDER_EVENT_APPLIED", result.current().id(), payload);
    }

    private ReservationStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            ReservationStatus parsed = ReservationStatus.valueOf(status.trim().toUpperCase());
            if (parsed == ReservationStatus.PENDING) {
                return null;
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
