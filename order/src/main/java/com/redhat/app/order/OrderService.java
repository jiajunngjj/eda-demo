package com.redhat.app.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gson.Gson;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Broadcast;

@ApplicationScoped
public class OrderService {

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    @Channel("order-new")
    @OnOverflow(OnOverflow.Strategy.LATEST)        
    Emitter<String> newOrderEmitter;
    
    @Inject
    @Channel("order-error-inv")
    @OnOverflow(OnOverflow.Strategy.LATEST)    
    Emitter<String> invErrorEmitter;
    
    @Inject
    @Channel("status-input")
    @OnOverflow(OnOverflow.Strategy.LATEST)        
    Emitter<String> statusEmitter;

    Gson gson = new Gson();
    Map<String,String> returnStatus = new HashMap<String,String>();
    
    
    List<Transaction> txList = new ArrayList<Transaction>();

    void onStart(@Observes StartupEvent event) {
        log.info("starting up order service");
        returnStatus.put("NEW", "ORDER RECEIVED:");
        returnStatus.put("CANCELLED", "ORDER CANCELLED - Timeout:");
        returnStatus.put("COMPLETED", "ORDER COMPLETED:");
        returnStatus.put("INVENTORY_INSUFFICIENT_STOCK", "ORDER CANCELLED - Out of Stock:");

    }
    @Incoming("order-in-progress")//in-progress-order
    public String process(String json) {
        
        Order order = gson.fromJson(json, Order.class);
        log.info("Received event from in progress queue: "+order);
        //simplified
        //put logic to check if all the dependent services are completed.
        // for now, after inventory check is done, we will mark as complete
        log.info("Completing order "+order);
        this.completeTransaction(order);
        return json;
    }

    @Incoming("order-error")//receive error from other services
    public String processError(String json) {
        Order order = gson.fromJson(json, Order.class);
        log.info("**************************************************Error  "+order.getStatus());
        //call downstreams services to handle erros also
        //order.setStatus("FROM_ORDER_ERROR");
        json = gson.toJson(order);
        //consolidate all error messages, and send to individual queues, 
        // crude, can be refined
        //check that this is a inventory related error, send back to inv-error queue
        if (order.getStatus().equals("INVENTORY_INSUFFICIENT_STOCK")) {
            log.info("Sending to invoice error queue "+order);
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
        if (tx!=null) {
            tx.setStatus("CANCELLED");
            tx.update();
            //update screen status
            //updateStreamStatus("Order Cancelled: "+order.getId()+" , Reason: "+order.getStatus());
            String json = gson.toJson(order);
            updateStreamStatus(json);
        }
    }

    //called by cron job
    public void cancelStaleTransactions(Order order) {
        order.setStatus("CANCELLED");
                //update screen status
        //updateStreamStatus("Order Cancelled: "+order.getId()+" Reason: time out");
        String json = gson.toJson(order);
        updateStreamStatus(json);
    }

    public void completeTransaction(Order order) {
        log.info("Inside Complete order "+order);
        order.setStatus("COMPLETED");
        if (!order.getStatus().equals("CANCELLED")) {
            Transaction tx = Transaction.findById(order.getId());
            if (tx!=null) {
                tx.setStatus("COMPLETED");
                tx.setInventoryStatus("COMPLETED");
                tx.update();
                log.info("Updating stream "+order);
                //updateStreamStatus("Order Confirmed: "+order.getId());
                String json = gson.toJson(order);
                updateStreamStatus(json);
            }
        }
    }


    public Order newOrder(Order order) {
        log.info("NEW Order "+order);
        order.setStatus("NEW");
        //if no order id, generate here (only orders from web client has order id generated)
        if (order.getId()==null || order.getId().length() == 0) {
            log.info("generate id");
            order.setId( Math.floor(Math.random() * 100000 )+"-"+order.getEmail() );
        }
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
        //updateStreamStatus("Order Received:  "+order.getId()+" is being processed");
        updateStreamStatus(json);
        newOrderEmitter.send(json);//send to order-new queue
        //statusEmitter.send("test" + order.getId());

        //updateStatus();
        return order;
    }

    private void updateStreamStatus(String json) {
        Order order = gson.fromJson(json, Order.class);
        String status = returnStatus.get(order.getStatus())+" "+order.getId();
        try {
        statusEmitter.send(status);
        } catch (Exception ex) {
            log.info("caught exception.... resending");
            statusEmitter.send(status);
        }

    }

    @Incoming("status-input")
    @Outgoing("status-stream")                          
    @Broadcast                                           
    public String updateStatus(String json) {
        log.info("status update called "+json);
        return json;
    }    
}