package com.ias.flashreserve.adapter.out.mongo;

import com.ias.flashreserve.domain.model.OutboxMessage;
import com.ias.flashreserve.domain.model.OutboxStatus;
import com.ias.flashreserve.domain.port.out.OutboxStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
public class MongoOutboxAdapter implements OutboxStore {

    private static final Logger log = LoggerFactory.getLogger(MongoOutboxAdapter.class);

    private final SpringOutboxRepository repository;

    public MongoOutboxAdapter(SpringOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Void> enqueue(String type, String aggregateId, String payload) {
        OutboxDocument document = new OutboxDocument();
        document.setId("obx_" + UUID.randomUUID());
        document.setType(type);
        document.setAggregateId(aggregateId);
        document.setPayload(payload);
        document.setStatus(OutboxStatus.PENDING);
        document.setAttempts(0);
        document.setCreatedAt(Instant.now());
        return repository.save(document)
                .doOnNext(saved -> log.info("outbox_enqueued id={} type={} aggregateId={}", saved.getId(), type, aggregateId))
                .then()
                .onErrorResume(error -> {
                    log.error("outbox_enqueue_failed type={} aggregateId={} error={}", type, aggregateId, error.toString());
                    return Mono.empty();
                });
    }

    @Override
    public Flux<OutboxMessage> findPending() {
        return repository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).map(this::toDomain);
    }

    @SuppressWarnings("null")
    @Override
    public Mono<OutboxMessage> save(OutboxMessage message) {
        return repository.save(toDocument(message)).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }

    private OutboxMessage toDomain(OutboxDocument document) {
        return new OutboxMessage(
                document.getId(),
                document.getType(),
                document.getAggregateId(),
                document.getPayload(),
                document.getStatus(),
                document.getAttempts(),
                document.getCreatedAt(),
                document.getProcessedAt(),
                document.getLastError()
        );
    }

    private OutboxDocument toDocument(OutboxMessage message) {
        OutboxDocument document = new OutboxDocument();
        document.setId(message.id());
        document.setType(message.type());
        document.setAggregateId(message.aggregateId());
        document.setPayload(message.payload());
        document.setStatus(message.status());
        document.setAttempts(message.attempts());
        document.setCreatedAt(message.createdAt());
        document.setProcessedAt(message.processedAt());
        document.setLastError(message.lastError());
        return document;
    }
}
