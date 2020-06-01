package com.redhat.app.order;

import lombok.Data;

@Data
public class Order {
    String id;
    String customer;
    String product;
    String address;
    String email;
    Integer qty;
    /**
    Product product;
    Customer customer;
    Integer qty;
    */
}