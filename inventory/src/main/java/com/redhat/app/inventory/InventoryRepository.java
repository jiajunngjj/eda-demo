package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
@ApplicationScoped
public class InventoryRepository implements PanacheRepository<Inventory>{
 
    public Inventory findInventorybyId(String id) {
        return find("id",id).firstResult();
    }
}