package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.google.gson.Gson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            json = gson.toJson(order);
                Thread.sleep(3000);
                //simulate delay in processing
                emitter.send(json);
    
           
		} catch (InventoryException e) {
            //e.printStackTrace();
            log.info("Inventory Error captured "+e.getMessage());
            order.setStatus(e.getMessage());         
            json = gson.toJson(order);
            errorEmitter.send(json);
		} catch (InterruptedException e) {
            e.printStackTrace();
        } 

        return order;
    }

    @Incoming("order-error-inv")
    public Order processError(String json) {
        //ignore event sent out by myself
        Order order = gson.fromJson(json, Order.class);
        if (order.getStatus().equals("INVENTORY_INSUFFICIENT_STOCK")) {
            return order;
        }
        
        //dirty hack - simple scenario, add back the reduced qty
        log.info("detected Error in order txn: "+order.getId()+" , reverting inventory");
        //order.setQty(order.getQty()*(-1));
        Inventory i =Inventory.findById(order.getProduct());
        i.setStock(i.getStock()+order.getQty());
        i.update();
        order.setStatus("INVENTORY_REVERTED");
        return  order;
    }   
    private Order updateInventory(Order order) throws InventoryException{
        Inventory i =Inventory.findById(order.getProduct());
        log.info("find by id "+i);
        if (i !=null ) {
            if ( (i.getStock().intValue() < order.getQty().intValue())) {
                order.setStatus("INVENTORY_INSUFFICIENT_STOCK");

                String json = gson.toJson(order);
                //errorEmitter.send(json);
                throw new InventoryException("INVENTORY_INSUFFICIENT_STOCK");
            }
            i.setStock(Integer.valueOf(i.getStock().intValue() - order.getQty().intValue()));
            i.update();
        }
        order.setStatus("INVENTORY_UPDATED");
        return order;
    }
}