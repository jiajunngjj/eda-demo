package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import lombok.Data;

@Data
@ApplicationScoped
@ActivateRequestContext
public class DBService implements Runnable {


    Logger log = LoggerFactory.getLogger(this.getClass());
    private Order order;
    private int type;
    private InventoryRepository repo;
    
    public DBService(Order order, int type, InventoryRepository repo) {
        this.order=order;
        this.type=type;
        this.repo = repo;
        log.info("Constructor "+this.order);
    }

    public DBService() {

    }

    void onStart(@Observes StartupEvent ev) {
        log.info("Onstart "+this.repo);
        //scheduler.submit(this);
        
        
    }
    @Override
    public void run() {
        try {
            log.info("DB service "+Thread.currentThread().getId());
            log.info(""+this.repo);
            this.repo.updateStock(this.order, this.type);
            
		} catch (InventoryException e) {
            //pointless to throw here, but then....
			e.printStackTrace();
		}  finally {
            log.info("Done "+Thread.currentThread().getId());
        }      
    }
}