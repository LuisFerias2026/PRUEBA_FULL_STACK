package com.ias.flashreserve.adapter.out.mongo;

import com.ias.flashreserve.domain.model.OutboxStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface SpringOutboxRepository extends ReactiveMongoRepository<OutboxDocument, String> {
    Flux<OutboxDocument> findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
