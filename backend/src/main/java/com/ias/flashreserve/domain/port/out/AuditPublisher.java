package com.ias.flashreserve.domain.port.out;

import com.ias.flashreserve.domain.model.OutboxMessage;
import reactor.core.publisher.Mono;

public interface AuditPublisher {
    Mono<Void> publish(OutboxMessage message);
}
