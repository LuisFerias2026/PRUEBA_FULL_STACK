package com.ias.flashreserve.adapter.in.web.dto;

import com.ias.flashreserve.domain.model.InventoryItem;

public record InventoryItemResponse(String sku, String name, int available) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(item.sku(), item.name(), item.available());
    }
}
