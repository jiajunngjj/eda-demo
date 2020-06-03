package com.redhat.app.inventory;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gson.Gson;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.mongodb.panache.PanacheQuery;

@ApplicationScoped
public class InventoryService {
    Gson gson = new Gson();

    @Inject
    @Channel("inventory-completed")
    Emitter<String> emitter;

    @Inject
    @Channel("order-error")
    Emitter<String> errorEmitter;    

    @ConfigProperty(name = "app.error.inventory.id")
    String injectedError;

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Incoming("new-order")
    public Order process(String json) {
        //simulate processing inventory
        Order order = gson.fromJson(json, Order.class);   
        log.info("Order "+order.getId());
        try {
            
            this.updateInventory(order);
            
		} catch (InventoryException e) {
            //e.printStackTrace();
            log.info("Inventory Error captured "+e.getMessage());
            order.setStatus("INVENTORY_ERROR");         
            json = gson.toJson(order);
            errorEmitter.send(json);
		}
        json = gson.toJson(order);
        try {
            Thread.sleep(3000);
            //simulate delay in processing
            emitter.send(json);

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        

        return order;
    }

    @Incoming("order-error-inv")
    public Order processError(String json) {
        //ignore event sent out by myself
        Order order = gson.fromJson(json, Order.class);
        if (order.getStatus().equals("INVENTORY_ERROR")) {
            return order;
        }        
        //dirty hack - simple scenario, add back the reduced qty
        try {
            log.info("detected Error in order txn: "+order.getId()+" , reverting inventory");
            order.setQty(order.getQty()*(-1));
			this.updateInventory(order);
		} catch (InventoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        order.setStatus("revert inventory");
        return  order;
    }   
    private Order updateInventory(Order order) throws InventoryException{
        Inventory i =Inventory.findById(order.getProduct());
        log.info("find by id "+i);
        if (i !=null ) {
            if ( (i.getStock().intValue() < order.getQty().intValue())) {
                order.setStatus("INVENTORY_ERROR");
                String json = gson.toJson(order);
                errorEmitter.send(json);
                throw new InventoryException("Insufficient Stock");

            }
            i.setStock(Integer.valueOf(i.getStock().intValue() - order.getQty().intValue()));
            i.update();
        }
        //testing
        //PanacheQuery<Inventory> inventoryQuery = Inventory.find("name", order.getProduct());
        //List<Inventory> results = inventoryQuery.list();
        //log.info("results "+results.size());
        //Inventory inv = results.get(0);
        //inv.update("stock", inv.stock - order.getQty() );
        order.setStatus("INVENTORY_UPDATED");
        return order;
    }
}