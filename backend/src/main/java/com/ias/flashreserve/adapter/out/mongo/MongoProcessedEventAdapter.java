package com.ias.flashreserve.adapter.out.mongo;

import com.ias.flashreserve.domain.port.out.ProcessedEventStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class MongoProcessedEventAdapter implements ProcessedEventStore {

    private final SpringProcessedEventRepository repository;

    public MongoProcessedEventAdapter(SpringProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Boolean> tryMarkProcessed(String eventId, String reservationId, long sequence) {
        ProcessedEventDocument document = new ProcessedEventDocument(eventId, reservationId, sequence, Instant.now());
        return repository.insert(document)
                .thenReturn(true)
                .onErrorResume(DuplicateKeyException.class, error -> Mono.just(false));
    }

    @Override
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }
}
