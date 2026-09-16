package com.ias.flashreserve.adapter.in.web;

import com.ias.flashreserve.adapter.in.web.dto.InventoryItemResponse;
import com.ias.flashreserve.domain.port.in.ListInventoryUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final ListInventoryUseCase listInventoryUseCase;

    public InventoryController(ListInventoryUseCase listInventoryUseCase) {
        this.listInventoryUseCase = listInventoryUseCase;
    }

    @GetMapping
    public Flux<InventoryItemResponse> list() {
        return listInventoryUseCase.list().map(InventoryItemResponse::from);
    }
}
