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

    @Inject
    @Channel("new-payment")
    Emitter<String> paymentEmitter;
    
    @Incoming("inventory-completed")
    public String process(String json) {
        log.info("Inventory done for "+json);
        //add a record into database
        newPayment();
        return json;
    }

    public Order newPayment(Order order) {
        log.info("Order " + order);
        Gson gson = new Gson();
        String json = gson.toJson(order);
        paymentEmitter.send(json);
        return order;
    }

    public Order newOrder(Order order) {
        log.info("Order "+order);
        Gson gson = new Gson();
        String json = gson.toJson(order);
        emitter.send(json);
        return order;
    }
}