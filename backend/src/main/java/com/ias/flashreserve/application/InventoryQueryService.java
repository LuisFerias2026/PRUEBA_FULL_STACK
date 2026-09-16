package com.ias.flashreserve.application;

import com.ias.flashreserve.domain.model.InventoryItem;
import com.ias.flashreserve.domain.port.in.ListInventoryUseCase;
import com.ias.flashreserve.domain.port.out.InventoryStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class InventoryQueryService implements ListInventoryUseCase {

    private final InventoryStore inventoryStore;

    public InventoryQueryService(InventoryStore inventoryStore) {
        this.inventoryStore = inventoryStore;
    }

    @Override
    public Flux<InventoryItem> list() {
        return inventoryStore.findAll();
    }
}
