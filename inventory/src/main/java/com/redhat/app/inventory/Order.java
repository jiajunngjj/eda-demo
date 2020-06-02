package com.redhat.app.inventory;

import lombok.Data;

@Data
public class Order {
    String id;
    String customer;
    String product;
    String address;
    String email;
    Integer qty;
    String status;
    /**
    Product product;
    Customer customer;
    Integer qty;
    */
}