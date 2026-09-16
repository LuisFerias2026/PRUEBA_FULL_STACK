package com.ias.flashreserve.config;

import com.ias.flashreserve.domain.model.InventoryItem;
import com.ias.flashreserve.domain.port.out.InventoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class InventorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InventorySeeder.class);

    private final InventoryStore inventoryStore;

    public InventorySeeder(InventoryStore inventoryStore) {
        this.inventoryStore = inventoryStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        Flux.just(
                        new InventoryItem("SKU-FLASH-A", "Flash campaign A", 10),
                        new InventoryItem("SKU-FLASH-B", "Flash campaign B", 3)
                )
                .flatMap(item -> inventoryStore.saveIfAbsent(item)
                        .doOnNext(saved -> log.info("seed sku={} available={}", saved.sku(), saved.available())))
                .blockLast();
    }
}
