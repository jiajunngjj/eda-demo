package com.redhat.app.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class StartUpBean {
    Logger log = LoggerFactory.getLogger(this.getClass());
    
    List<Inventory> list = new ArrayList<Inventory>();
    void onStart(@Observes StartupEvent ev) {               
        log.info("The application is starting...");
        //create new inventory
        Inventory inv1 = new Inventory();

        //inv1.setId("f0001");
        inv1.setId("Burger");
        inv1.setName("Burger");
        inv1.setStock(Integer.valueOf(100));
        inv1.setPrice(Double.valueOf(22.5));
        list.add(inv1);
        inv1.persist();

        Inventory inv2 = new Inventory();
        //inv2.setId("f0002");
        inv2.setId("Pizza");
        inv2.setName("Pizza");
        inv2.setStock(Integer.valueOf(50));
        inv2.setPrice(Double.valueOf(29.5));
        list.add(inv2);
        inv2.persist();

        Inventory inv3 = new Inventory();
        //inv2.setId("f0003");
        inv3.setId("Sushi");
        inv3.setName("Sushi");
        inv3.setStock(Integer.valueOf(250));
        inv3.setPrice(Double.valueOf(3.5));
        list.add(inv3);
        inv3.persist();

    }

    void onStop(@Observes ShutdownEvent ev) {               
        log.info("The application is stopping...");
        for (Inventory inventory : list) {
            inventory.delete();
        }
    }    
}