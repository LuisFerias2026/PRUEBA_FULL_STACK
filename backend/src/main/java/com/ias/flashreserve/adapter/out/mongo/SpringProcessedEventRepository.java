package com.ias.flashreserve.adapter.out.mongo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SpringProcessedEventRepository extends ReactiveMongoRepository<ProcessedEventDocument, String> {
}
