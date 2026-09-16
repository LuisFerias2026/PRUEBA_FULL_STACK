package com.ias.flashreserve.domain.port.in;

import com.ias.flashreserve.domain.model.InventoryItem;
import reactor.core.publisher.Flux;

public interface ListInventoryUseCase {
    Flux<InventoryItem> list();
}
