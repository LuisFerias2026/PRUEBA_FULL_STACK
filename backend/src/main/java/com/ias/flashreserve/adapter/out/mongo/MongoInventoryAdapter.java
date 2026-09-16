package com.ias.flashreserve.adapter.out.mongo;

import com.ias.flashreserve.domain.model.InventoryItem;
import com.ias.flashreserve.domain.port.out.InventoryStore;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MongoInventoryAdapter implements InventoryStore {

    private final SpringInventoryRepository repository;
    private final ReactiveMongoTemplate mongoTemplate;

    public MongoInventoryAdapter(SpringInventoryRepository repository, ReactiveMongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @SuppressWarnings("null")
    @Override
    public Mono<InventoryItem> findBySku(String sku) {
        return repository.findById(sku).map(this::toDomain);
    }

    @Override
    public Flux<InventoryItem> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Mono<Boolean> decrementIfAvailable(String sku, int quantity) {
        Query query = Query.query(Criteria.where("_id").is(sku).and("available").gte(quantity));
        Update update = new Update().inc("available", -quantity);
        return mongoTemplate.updateFirst(query, update, InventoryDocument.class)
                .map(result -> result.getMatchedCount() >= 1);
    }

    @Override
    public Mono<Void> increment(String sku, int quantity) {
        Query query = Query.query(Criteria.where("_id").is(sku));
        Update update = new Update().inc("available", quantity);
        return mongoTemplate.updateFirst(query, update, InventoryDocument.class).then();
    }

    @SuppressWarnings("null")
    @Override
    public Mono<InventoryItem> saveIfAbsent(InventoryItem item) {
        return repository.findById(item.sku())
                .map(this::toDomain)
                .switchIfEmpty(save(item));
    }

    @Override
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }

    @Override
    public Mono<InventoryItem> save(InventoryItem item) {
        return repository.save(new InventoryDocument(item.sku(), item.name(), item.available())).map(this::toDomain);
    }

    private InventoryItem toDomain(InventoryDocument document) {
        return new InventoryItem(document.getSku(), document.getName(), document.getAvailable());
    }
}
