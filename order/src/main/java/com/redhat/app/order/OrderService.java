package com.redhat.app.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gson.Gson;
import com.redhat.app.order.status.DeliveryStatus;
import com.redhat.app.order.status.InventoryStatus;
import com.redhat.app.order.status.OrderStatus;
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
@ActivateRequestContext
public class OrderService {

    private final ExecutorService scheduler = Executors.newFixedThreadPool(10);

    @Inject
    TransactionRepository repo;

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    @Channel("order-new")
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512) 
    @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)                    
    //@OnOverflow(OnOverflow.Strategy.LATEST) 
    @Broadcast(value = 0)                          
    Emitter<String> newOrderEmitter;


    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512) 
    //@Inject
    //@Channel("order-new-delivery")
    //@OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)                    
    //@OnOverflow(OnOverflow.Strategy.LATEST) 
    //@Broadcast(value = 0)                          
    //Emitter<String> newOrderDeliveryEmitter;

    
    @Inject
    @Channel("order-error-inv")
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
    //@OnOverflow(OnOverflow.Strategy.LATEST) 
    @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)                    
    Emitter<String> invErrorEmitter;
    
    @Inject
    @Channel("status-input")
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
    @OnOverflow(OnOverflow.Strategy.LATEST)                
    Emitter<String> statusEmitter;

    Gson gson = new Gson();
    Map<OrderStatus,String> returnStatus = new HashMap<OrderStatus,String>();
    
    
    List<Transaction> txList = new ArrayList<Transaction>();

    void onStart(@Observes StartupEvent event) {
        log.info("starting up order service");
        returnStatus.put(OrderStatus.NEW, OrderStatus.NEW.label);
        returnStatus.put(OrderStatus.CANCELLED, OrderStatus.CANCELLED.label);
        returnStatus.put(OrderStatus.CANCELLED_TIMEOUT, OrderStatus.CANCELLED_TIMEOUT.label);
        returnStatus.put(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED.label);
        returnStatus.put(OrderStatus.CANCELLED_NO_STOCK, OrderStatus.CANCELLED_NO_STOCK.label);
    }
    @Incoming("order-in-progress")//in-progress-order
    //Incoming: INVENTORY_UPDATED
    public String process(String json) {
        Order order = gson.fromJson(json, Order.class);
        boolean allclear = false;
        if (order.getInventoryStatus().equals(InventoryStatus.UPDATED)) { 
            try {
                log.info("****IN PROGRESS 1 "+json);
                

                 if (scheduler.submit(new TransactionDBService("INVENTORY", order, repo)).get().isOrderComplete()) {
                    allclear = true;
                 }

			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}  
            
        } 
        else if (order.getDeliveryStatus().equals(DeliveryStatus.CONFIRMED)) {
            try {
				if (scheduler.submit(new TransactionDBService("DELIVERY", order, repo)).get().isOrderComplete()) {
				    allclear = true;
				 }
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

        }
        else if (order.getInventoryStatus().equals(InventoryStatus.REVERTED)) {    
            log.info("****IN PROGRESS RECV Cancellation "+json);
            //order.setStatus("CANCELLED");
            order.setStatus(OrderStatus.CANCELLED);
            json = gson.toJson(order);
            updateStreamStatus(json);
            allclear = false;
        }
        log.info("****IN PROGRESS all clear "+allclear);

        if (allclear) {
            this.completeTransaction(order);
        }   

        return json;
    }

    @Incoming("order-error")//receive error from other services
    //for now only INVENTORY INSUFFICIENT_STOCK
    public String processError(String json) {
        Order order = gson.fromJson(json, Order.class);
        log.info("*****************ProcssError: "+order.getStatus());
        //call downstreams services to handle erros also
        json = gson.toJson(order);
        if (order.getInventoryStatus().equals(InventoryStatus.NO_STOCK)) {    
            log.info("*******ProcssError: Sending to inventory error queue "+order);
            invErrorEmitter.send(json);
        }
        //if other Service, call their respective logic
        this.cancelTransactions(order);
        
        return json;
    }

    //outgoing CANCELLING
    public void cancelTransactions(Order order) {
        
        if (order.getInventoryStatus().equals(InventoryStatus.NO_STOCK)) {
            order.setStatus(OrderStatus.CANCELLING);
            //no change to inventory status
            String error=gson.toJson(order);
            log.info("*****Cancel Tx: sending to inv error queue "+error);
            invErrorEmitter.send(error);
        }

        try {
            Transaction tx = scheduler.submit(new TransactionDBService("CANCEL", order, repo)).get();
            log.info("CANCELLED "+tx.getId());
            //update screen status
            String json = gson.toJson(tx.getOrder());
                updateStreamStatus(json);
        } catch (Exception ex) {
            log.info("***********Cancel Tx:  Error in cancel tx "+ex+" ....ignoring");

        }
    }

    //called by cron job
    //cron job will pick up Tx that are NEW
    //set as CANCELLING
    //we should revert the inv here
    public void cancelStaleTransactions(Order order) {
        log.info("INSIDE CANCELSTALETX "+order);
        boolean errorSent = false;
        String error = "";
        try {
            if (order.getInventoryStatus().equals(InventoryStatus.UPDATED )) {    
                order.setStatus(OrderStatus.CANCELLING);
                //no change to inventory status
                error=gson.toJson(order);
                invErrorEmitter.send(error);
                errorSent = true;
                log.info("*******Cancelling committed inventory transaction due to timeout: sending to inv error queue "+error);
            }    
            
            
                Transaction tx = scheduler.submit(new TransactionDBService("CANCEL", order, repo)).get();            
                String json = gson.toJson(tx.getOrder());
                log.info("cancelled stale order "+tx.getId());
                //log.info("calling streams update "+json);
                updateStreamStatus(json);
                } catch (Exception ex) {
                    log.info("*******Cancelling transactions due to timeout: error in cancellation stale tx "+ex+" ....retry once");
                    if (!errorSent) {
                        error = gson.toJson(order);
                        invErrorEmitter.send(error);
                    }
                }
    }

    public void sendCancelStatus(Order order) {
        String error=null;

            order.setStatus(OrderStatus.CANCELLED);
            //no change to inventory status
            error=gson.toJson(order);
            try {
                updateStreamStatus(error);
            log.info("*******Cancelling due to time out "+error);            
            } catch (Exception ex) {
                log.info("error "+ex);
            }
    }

    
    public void revertInventory (Order order) {
        String error=null;
        
        order.setStatus(OrderStatus.CANCELLING);
        //no change to inventory status
        error=gson.toJson(order);
        try {
            invErrorEmitter.send(error);
        log.info("*******Cancelling committed inventory transaction due to timeout: sending to inv error queue "+error);            
        } catch (Exception ex) {
            log.info("error "+ex);
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
        tx.getOrder().setInventoryStatus(order.getInventoryStatus());
        tx.getOrder().setDeliveryStatus(order.getDeliveryStatus());
        tx.getOrder().setPaymentStatus(order.getPaymentStatus());
    }
    public void completeTransaction(Order order) {
        //log.info("Inside Complete order "+order);
        //order.setStatus("COMPLETED");
        order.setStatus(OrderStatus.CONFIRMED);
        //if (!order.getStatus().equals("CANCELLED")) {
        if (!(order.getStatus().equals(OrderStatus.CANCELLED) || order.getStatus().equals(OrderStatus.CANCELLED_TIMEOUT) )) {     

            try 
            {
                Transaction tx = scheduler.submit(new TransactionDBService("COMPLETE", order, repo)).get();
                log.info("Completed "+tx.getOrder());
                String json = gson.toJson(tx.getOrder());
                    updateStreamStatus(json);
            } catch (Exception ex) {
                log.info("****CompleteTx: error in new order "+ex+" ....ignoring");
            }
        } else {
            log.info("****CompleteTx: trying to complete a cancelled order");
        }
                

    }


    public Order newOrder(Order order) {
        try {
            log.info("NEW Order "+order);
            //TODO
            //order.setStatus("NEW");
            order.setStatus(OrderStatus.NEW);
            //if no order id, generate here (only orders from web client has order id generated)
            if (order.getId()==null || order.getId().length() == 0) {
                //log.info("generate id");
                order.setId( Math.floor(Math.random() * 100000 )+"-"+order.getEmail() );
            }
            //create transaction
            Future<Transaction> future = scheduler.submit(new TransactionDBService( "",order, repo));
            Transaction tx = future.get();
            log.info("created new tx "+tx.getId());                       
            String json = gson.toJson(order);
            //newOrderDeliveryEmitter.send(json);//send to order-new-delivery queue
            newOrderEmitter.send(json);//send to order-new queue
            updateStreamStatus(json);
        } catch (Exception ex) {
            log.info("****New Order: error in new order "+ex+" ....ignoring");
            //todo handle error here
        }
        return order;
    }

    private void updateStreamStatus(String json) {
        log.info("Emitting raw "+json );

        Order order = gson.fromJson(json, Order.class);
        log.info("Emitting order  "+order );
        String status = returnStatus.get(order.getStatus())+":"+order.getId();
        try {
            //log.info("Emitting "+status );
            statusEmitter.send(status);
        
        } catch (Exception ex) {
            //log.info("****UpdateStatusStream:  exception sending stream.... "+ex);
            //log.info("close it ");
            statusEmitter.complete();
        }

    }

    @Incoming("status-input")
    @Outgoing("status-stream")                          
    @Broadcast                                           
    public String updateStatus(String json) {
        //log.info("****************status update called "+json);
        return json;
    }    
}
