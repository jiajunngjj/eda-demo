package com.redhat.app.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;



@ApplicationScoped
public class StartUpBean {
    Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    InventoryRepository repo;

    @ConfigProperty(name = "db.remove.when.shutdown") 
    String dbRenove;
    @Transactional
    void onStart(@Observes StartupEvent ev) {    
        log.info("starting up....");
        Inventory inv1 = new Inventory();
        //inv1.setId("Burger");
        inv1.setName("Burger");
        inv1.setPrice(12.5D);
        inv1.setStock(1000);
        

        Inventory inv2 = new Inventory();
        //inv2.setId("Sushi");
        inv2.setName("Sushi");
        inv2.setPrice(2.5D);
        inv2.setStock(1000);
        Inventory inv3 = new Inventory();
        //inv2.setId("Sushi");
        inv3.setName("Pizza");
        inv3.setPrice(2.5D);
        inv3.setStock(1000);
        
        if (repo.findAll().list().size() == 0) {
            repo.persist(inv1);
            repo.persist(inv2);
            repo.persist(inv3);
        }
    }
    @Transactional
    void shutdown(@Observes ShutdownEvent ev) {
        log.info("shutdown");
        if (Boolean.parseBoolean(dbRenove)) {
            repo.deleteAll();
        }
        //Inventory.dropTable(client).onItem().apply(deleted -> deleted);
    }
}