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
        Gson gson = new Gson();
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
        emitter.send(order.getId());
        

        return order;
    }

    private Order updateInventory(Order order) throws InventoryException{
        log.info("injected error:"+injectedError);
        if (order.product.equals(injectedError)) {
            throw new InventoryException("Error updating Inventory");
        }
        order.setStatus("INVENTORY_CHEAPER");
        return order;
    }
}