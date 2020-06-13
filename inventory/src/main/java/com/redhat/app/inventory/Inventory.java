package com.redhat.app.inventory;



import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntity;


import lombok.Data;

@Data
@Entity
//public class Inventory extends PanacheEntity{
public class Inventory {
    @Id
    @GeneratedValue
    Long id;
    String productId;
    String name;
    Double price;
    Integer stock;



}