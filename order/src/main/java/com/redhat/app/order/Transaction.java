package com.redhat.app.order;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;
@Data
public class Transaction extends PanacheMongoEntity{
    
    String id;
    Order order;
    String status;
    String paymentStatus;
    String inventoryStatus;
    String deliveryStatus;
}