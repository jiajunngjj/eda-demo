package com.redhat.app.order;

import java.util.Date;

import lombok.Data;

@Data
public class Order {
    String id;
    Customer customer;
    Product product;
    Integer qty;
    Date date;
    String status;
}
