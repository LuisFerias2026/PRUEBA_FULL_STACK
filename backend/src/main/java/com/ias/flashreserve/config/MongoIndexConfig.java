package com.ias.flashreserve.config;

import com.ias.flashreserve.adapter.out.mongo.OutboxDocument;
import com.ias.flashreserve.adapter.out.mongo.ProcessedEventDocument;
import com.ias.flashreserve.adapter.out.mongo.ReservationDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Order(0)
public class MongoIndexConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoIndexConfig(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensure(ReservationDocument.class, new Index().named("idx_reservations_sku").on("sku", Sort.Direction.ASC))
                .then(ensure(ProcessedEventDocument.class, new Index().named("idx_processed_reservation").on("reservationId", Sort.Direction.ASC)))
                .then(ensure(OutboxDocument.class, new Index().named("idx_outbox_status_created")
                        .on("status", Sort.Direction.ASC)
                        .on("createdAt", Sort.Direction.ASC)))
                .doOnSuccess(ignored -> log.info("mongo_indexes_ready"))
                .block();
    }

    @SuppressWarnings("null")
    private Mono<String> ensure(Class<?> type, Index index) {
        return mongoTemplate.indexOps(type)
                .createIndex(index)
                .onErrorResume(error -> {
                    log.warn("index_already_present type={} message={}", type.getSimpleName(), error.getMessage());
                    return Mono.just("present");
                });
    }
}
