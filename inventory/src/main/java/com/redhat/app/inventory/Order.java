package com.redhat.app.inventory;

import com.redhat.app.inventory.status.InventoryStatus;
import com.redhat.app.inventory.status.OrderStatus;

import lombok.Data;

@Data
public class Order {
    String id;
    String customer;
    String product;
    String address;
    String email;
    Integer qty;
//    String status;
    OrderStatus status;
    String deliveryStatus;
    //String inventoryStatus;
    InventoryStatus inventoryStatus;
    String paymentStatus;    
    /**
    Product product;
    Customer customer;
    Integer qty;
    */
}