package com.redhat.app.order;

import lombok.Data;

@Data
public class Inventory  {
    
    String id;
    Product product;
    Integer stock;
    String uom;
    Double price;
}