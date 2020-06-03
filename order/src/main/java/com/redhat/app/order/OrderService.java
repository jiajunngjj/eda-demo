package com.redhat.app.order;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gson.Gson;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.reactive.messaging.annotations.Broadcast;

@ApplicationScoped
public class OrderService {

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    @Channel("new-order")    
    Emitter<String> emitter;
    
    @Inject
    @Channel("order-error-inv")    
    Emitter<String> invErrorEmitter;
    
    @Inject
    @Channel("status-input")    
    Emitter<String> statusEmitter;

    Gson gson = new Gson();

    @Incoming("inventory-completed")
    public String process(String json) {
        
        log.info("Inventory done for "+json);
        Order order = gson.fromJson(json, Order.class);
        updateStreamStatus(order.getId()+" inventory updated");
        return json;
    }

    @Incoming("order-error")
    public String processError(String json) {
        Order order = gson.fromJson(json, Order.class);
        log.info("Error  "+order.getStatus());
        //call downstreams services to handle erros also
        order.setStatus("FROM_ORDER_ERROR");
        json = gson.toJson(order);
        //inventory Service
        invErrorEmitter.send(json);
        //other Service
        statusEmitter.send(order.getId()+" is cancelled");
        return json;
    }
    public Order newOrder(Order order) {
        log.info("Order "+order);
        //create transaction
        /*
        Transaction tx = new Transaction();
        //tx.setId(Math.floor(Math.random()*10000)+order.getId());
        tx.setId(order.getId());
        tx.setOrder(order);
        tx.setStatus("NEW");
        log.info("tx id: "+tx.getId());
        */
        String json = gson.toJson(order);
        updateStreamStatus("Order "+order.getId()+" is being processed");
        emitter.send(json);
        //statusEmitter.send("test" + order.getId());

        //updateStatus();
        return order;
    }

    private void updateStreamStatus(String status) {
        statusEmitter.send(status);

    }

    @Incoming("status-input")
    @Outgoing("status-stream")                          
    @Broadcast                                           
    public String updateStatus(String json) {
        log.info("status update called "+json);
        return json;
    }    
}