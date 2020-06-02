package com.redhat.app.order;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gson.Gson;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OrderService {

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    @Channel("new-order")    
    Emitter<String> emitter;
    
    Gson gson = new Gson();

    @Incoming("inventory-completed")
    public String process(String json) {
        log.info("Inventory done for "+json);
        return json;
    }

    @Incoming("order-error")
    public String processError(String json) {
        Order order = gson.fromJson(json, Order.class);
        log.info("Error  "+order.getStatus());
        return json;
    }
    public Order newOrder(Order order) {
        log.info("Order "+order);
        
        String json = gson.toJson(order);
        emitter.send(json);
        return order;
    }
}