package com.redhat.app.order;

import java.util.ArrayList;
import java.util.List;

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

    List<Transaction> txList = new ArrayList<Transaction>();

    @Incoming("inventory-completed")//in-progress-order
    public String process(String json) {
        
        log.info("Inventory done for "+json);
        Order order = gson.fromJson(json, Order.class);
        //simplified
        //put logic to check if all the dependent services are completed.
        // for now, after inventory check is done, we will mark as complete
        this.completeTransaction(order);
        return json;
    }

    @Incoming("order-error")//receive error from services
    public String processError(String json) {
        Order order = gson.fromJson(json, Order.class);
        log.info("Error  "+order.getStatus());
        //call downstreams services to handle erros also
        //order.setStatus("FROM_ORDER_ERROR");
        json = gson.toJson(order);
        //consolidate all error messages, and send to individual queues, 
        // crude, can be refined
        //check that this is a inventory related error, send back to inv-error queue
        if (order.getStatus().equals("INVENTORY_INSUFFICIENT_STOCK")) {
            invErrorEmitter.send(json);
        }
        this.cancelTransactions(order);
        //other Service
        //updateStreamStatus(order.getId()+" is cancelled");
        return json;
    }
    public void cancelTransactions(Order order) {
        //order.setStatus("CANCELLED");
        Transaction tx = Transaction.findById(order.getId());
        tx.setStatus("CANCELLED");
        tx.update();
        //update screen status
        updateStreamStatus("Order Cancelled: "+order.getId()+" , Reason: "+order.getStatus());
    }

    //called by cron job
    public void cancelStaleTransactions(Order order) {
        order.setStatus("CANCELLED");
                //update screen status
        updateStreamStatus("Order Cancelled: "+order.getId()+" Reason: time out");
    }

    public void completeTransaction(Order order) {

        Transaction tx = Transaction.findById(order.getId());
        tx.setStatus("COMPLETED");
        tx.setInventoryStatus("COMPLETED");
        tx.update();
        updateStreamStatus("Order Confirmed: "+order.getId());

    }


    public Order newOrder(Order order) {
        log.info("Order "+order);
        //create transaction
        
        Transaction tx = new Transaction();
        //tx.setId(Math.floor(Math.random()*10000)+order.getId());
        tx.setId(order.getId());
        tx.setOrder(order);        
        log.info("tx id: "+tx.getId());
        //save a transaction record in mongodb, have a 10s job to check , 
        //if record is not completed, will the job will cancel it
        tx.persist();

        String json = gson.toJson(order);
        updateStreamStatus("Order Received:  "+order.getId()+" is being processed");
        emitter.send(json);//send to new-order queue
        //statusEmitter.send("test" + order.getId());

        //updateStatus();
        return order;
    }

    private void updateStreamStatus(String status) {
        statusEmitter.send(status);

    }

    @Incoming("status-input")
    @Outgoing("status-stream")                          
    //@Broadcast                                           
    public String updateStatus(String json) {
        log.info("status update called "+json);
        return json;
    }    
}