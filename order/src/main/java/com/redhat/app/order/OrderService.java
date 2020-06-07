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
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)     
    @OnOverflow(OnOverflow.Strategy.LATEST)                           
    Emitter<String> newOrderEmitter;
    
    @Inject
    @Channel("order-error-inv")
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
    @OnOverflow(OnOverflow.Strategy.LATEST)                
    Emitter<String> invErrorEmitter;
    
    @Inject
    @Channel("status-input")
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
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
        if (order.getStatus().equals("INVENTORY_UPDATED")) {
            Transaction tx = Transaction.findById(order.getId());
            if (tx!=null) {
                merge(tx,order);
                tx.setInventoryStatus("COMPLETED");
                tx.update();
            }
        }
        log.info("Completing order "+order);
        this.completeTransaction(order);
        return json;
    }

    @Incoming("order-error")//receive error from other services
    public String processError(String json) {
        Order order = gson.fromJson(json, Order.class);
        log.info("**************************************************Error  "+order.getStatus());
        //call downstreams services to handle erros also
        json = gson.toJson(order);
        //consolidate all error messages, and send to individual queues, 
        // crude, can be refined
        //check that this is a inventory related error, send back to inv-error queue
        if (order.getStatus().equals("INVENTORY_INSUFFICIENT_STOCK")) {
            log.info("Sending to inventory error queue "+order);
            invErrorEmitter.send(json);
        }
        //if other Service, call their respective logic
        this.cancelTransactions(order);
        
        return json;
    }
    public void cancelTransactions(Order order) {
        //order.setStatus("CANCELLED");
        Transaction tx = Transaction.findById(order.getId());
        if (tx!=null) {
            merge(tx,order);
            //if (
              //  tx.getOrder().getStatus().equals("INVENTORY_UPDATED") 
                //|| tx.getInventoryStatus().equals("COMPLETED") 
            //) {
                tx.getOrder().setStatus("INVENTORY_REVERT");
                tx.setInventoryStatus("INVENTORY_REVERT");
                String error=gson.toJson(tx.getOrder());
                log.info("sending to inv error queue "+error);
                invErrorEmitter.send(error);

            //}            
            tx.setStatus("CANCELLED");
            tx.update();
            //update screen status
            String json = gson.toJson(order);
            try {
                updateStreamStatus(json);
            } catch (Exception ex) {
                log.info("error in cancel tx "+ex+" ....ignoring");
    
            }
         }
    }

    //called by cron job
    public void cancelStaleTransactions(Order order) {
        log.info("INSIDE CANCELSTALETX "+order);
        try {
            Transaction tx = Transaction.findById(order.getId());
            merge(tx,order);
            //if order status is INV UPDATED, means inventory was committed , need to revert
            //if (tx.getOrder().getStatus().equals("INVENTORY_UPDATED") || tx.getInventoryStatus().equals("COMPLETED")) {
                //if (
                //    tx.getOrder().getStatus().equals("INVENTORY_UPDATED") 
                //    || tx.getInventoryStatus().equals("COMPLETED") 
                //) {
       
                tx.getOrder().setStatus("INVENTORY_REVERT");
                tx.setInventoryStatus("INVENTORY_REVERT");
                String error=gson.toJson(tx.getOrder());
                log.info("sending to inv error queue "+error);
                invErrorEmitter.send(error);
            //}
        //update screen status
            tx.setStatus("CANCELLED");
            tx.getOrder().setStatus("CANCELLED");
            tx.update();
        String json = gson.toJson(tx.getOrder());
        log.info("calling streams update "+json);     
        updateStreamStatus(json);
        } catch (Exception ex) {
            log.info("error in cancel stale tx "+ex+" ....ignoring");

        }
    }

    private void merge(Transaction tx, Order order) {
        tx.getOrder().setCustomer(order.getCustomer());
        tx.getOrder().setAddress(order.getAddress());
        tx.getOrder().setEmail(order.getEmail());
        tx.getOrder().setStatus(order.getStatus());
        tx.getOrder().setQty(order.getQty());
        tx.getOrder().setProduct(order.getProduct());
        tx.getOrder().setId(order.getId());
    }
    public void completeTransaction(Order order) {
        log.info("Inside Complete order "+order);
        order.setStatus("COMPLETED");
        if (!order.getStatus().equals("CANCELLED")) {
            Transaction tx = Transaction.findById(order.getId());
            if (tx!=null) {
                //transfer order status to tx
                merge(tx,order);
                tx.setStatus("COMPLETED");
                tx.setInventoryStatus("COMPLETED");
                tx.update();
                log.info("Updating stream "+order);
                String json = gson.toJson(order);
                try {
                    updateStreamStatus(json);
                } catch (Exception ex) {
                    log.info("error in new order "+ex+" ....ignoring");
                }
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
        tx.setId(order.getId());
        tx.setOrder(order);        
        log.info("tx id: "+tx.getId());
        //save a transaction record in mongodb, have a 10s job to check , 
        //if record is not completed, will the job will cancel it
        tx.persist();
        String json = gson.toJson(order);
        try {
         updateStreamStatus(json);
        } catch (Exception ex) {
            log.info("error in new order "+ex+" ....ignoring");

        }
        newOrderEmitter.send(json);//send to order-new queue
        return order;
    }

    private void updateStreamStatus(String json) {
        Order order = gson.fromJson(json, Order.class);
        String status = returnStatus.get(order.getStatus())+" "+order.getId();
        try {
            log.info("Emitting "+status );
            statusEmitter.send(status);
        
        } catch (Exception ex) {
            log.info("****caught exception sending stream.... "+ex);
            log.info("close it ");
            statusEmitter.complete();
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