package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
@ApplicationScoped
public class InventoryRepository implements PanacheRepository<Inventory>{
 

    
    
    public Inventory findByName(String name) {

        return find("name",name).firstResult();
    }

    @Transactional
    public Inventory updateStock(Inventory inv) {
        Inventory i = find("name",inv.getName()).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        this.persistAndFlush(i);
        return i;

    }
}