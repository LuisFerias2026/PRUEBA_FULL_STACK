package com.ias.flashreserve.domain.port.out;

import com.ias.flashreserve.domain.model.OutboxMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxStore {
    Mono<Void> enqueue(String type, String aggregateId, String payload);

    Flux<OutboxMessage> findPending();

    Mono<OutboxMessage> save(OutboxMessage message);

    Mono<Void> deleteAll();
}
