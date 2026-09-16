package com.ias.flashreserve.adapter.out.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inventory")
public class InventoryDocument {

    @Id
    private String sku;
    private String name;
    private int available;

    public InventoryDocument() {
    }

    public InventoryDocument(String sku, String name, int available) {
        this.sku = sku;
        this.name = name;
        this.available = available;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }
}
