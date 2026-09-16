package com.ias.flashreserve.domain.port.out;

import reactor.core.publisher.Mono;

public interface ProcessedEventStore {
    /**
     * @return true if this eventId was recorded for the first time, false if it was a duplicate
     */
    Mono<Boolean> tryMarkProcessed(String eventId, String reservationId, long sequence);

    Mono<Void> deleteAll();
}
