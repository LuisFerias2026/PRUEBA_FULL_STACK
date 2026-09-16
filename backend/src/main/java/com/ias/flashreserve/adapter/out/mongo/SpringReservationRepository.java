package com.ias.flashreserve.adapter.out.mongo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface SpringReservationRepository extends ReactiveMongoRepository<ReservationDocument, String> {
    Mono<ReservationDocument> findByIdempotencyKey(String idempotencyKey);
}
