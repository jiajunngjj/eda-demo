package com.redhat.app.inventory;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;

@Data
public class Inventory extends PanacheMongoEntity{
    String id;
    String name;
    Double price;
    Integer stock;
    
}