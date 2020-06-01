package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gson.Gson;

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

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Incoming("new-order")
    public Order process(String json) {
        Gson gson = new Gson();
        Order order = gson.fromJson(json, Order.class);
        log.info("Order "+order.getId());
        //simulate processing inventory
        emitter.send(order.getId());
        return order;
    }
}