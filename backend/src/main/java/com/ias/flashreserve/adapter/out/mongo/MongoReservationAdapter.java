package com.ias.flashreserve.adapter.out.mongo;

import com.ias.flashreserve.domain.exception.DuplicateReservationKey;
import com.ias.flashreserve.domain.model.Reservation;
import com.ias.flashreserve.domain.model.ReservationStatus;
import com.ias.flashreserve.domain.model.SequenceApplyResult;
import com.ias.flashreserve.domain.port.out.ReservationStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class MongoReservationAdapter implements ReservationStore {

    private final SpringReservationRepository repository;
    private final ReactiveMongoTemplate mongoTemplate;

    public MongoReservationAdapter(SpringReservationRepository repository, ReactiveMongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Reservation> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Mono<Reservation> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public Mono<Reservation> insert(Reservation reservation) {
        return repository.insert(toDocument(reservation))
                .map(this::toDomain)
                .onErrorMap(DuplicateKeyException.class, error -> new DuplicateReservationKey(reservation.idempotencyKey()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }

    @Override
    public Mono<Long> count() {
        return repository.count();
    }

    @Override
    public Mono<SequenceApplyResult> applyIfNewerSequence(String reservationId, long sequence, ReservationStatus status) {
        Query query = Query.query(Criteria.where("_id").is(reservationId).and("lastSequence").lt(sequence));
        Instant now = Instant.now();
        Update update = new Update()
                .set("status", status)
                .set("lastSequence", sequence)
                .set("updatedAt", now);
        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(false),
                ReservationDocument.class
        ).map(previousDoc -> {
            Reservation previous = toDomain(previousDoc);
            Reservation current = new Reservation(
                    previous.id(),
                    previous.idempotencyKey(),
                    previous.clientId(),
                    previous.sku(),
                    previous.quantity(),
                    status,
                    sequence,
                    previous.createdAt(),
                    now
            );
            return new SequenceApplyResult(previous, current);
        });
    }

    private Reservation toDomain(ReservationDocument document) {
        return new Reservation(
                document.getId(),
                document.getIdempotencyKey(),
                document.getClientId(),
                document.getSku(),
                document.getQuantity(),
                document.getStatus(),
                document.getLastSequence(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private ReservationDocument toDocument(Reservation reservation) {
        ReservationDocument document = new ReservationDocument();
        document.setId(reservation.id());
        document.setIdempotencyKey(reservation.idempotencyKey());
        document.setClientId(reservation.clientId());
        document.setSku(reservation.sku());
        document.setQuantity(reservation.quantity());
        document.setStatus(reservation.status());
        document.setLastSequence(reservation.lastSequence());
        document.setCreatedAt(reservation.createdAt());
        document.setUpdatedAt(reservation.updatedAt());
        return document;
    }
}
