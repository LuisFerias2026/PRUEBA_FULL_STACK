package com.ias.flashreserve.domain.port.out;

import com.ias.flashreserve.domain.model.InventoryItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InventoryStore {
    Mono<InventoryItem> findBySku(String sku);

    Flux<InventoryItem> findAll();

    Mono<Boolean> decrementIfAvailable(String sku, int quantity);

    Mono<Void> increment(String sku, int quantity);

    Mono<InventoryItem> saveIfAbsent(InventoryItem item);

    Mono<Void> deleteAll();

    Mono<InventoryItem> save(InventoryItem item);
}
