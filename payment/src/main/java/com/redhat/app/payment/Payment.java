package com.redhat.app.payment;

import lombok.Data;

@Data
public class Payment {

    String id;
    String customer;
    String product;
    String address;
    String email;
    Integer qty;

}